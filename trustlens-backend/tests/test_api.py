from datetime import datetime, timezone
import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.schemas import (
    RiskCategory,
    SeverityLevel,
    ThreatIntelStatus,
)
from app.threat_intelligence import MockThreatIntelligenceProvider


@pytest.fixture
def app_instance():
    """Create test application instance."""
    settings = Settings(
        SAFE_BROWSING_API_KEY=None,
        ENVIRONMENT="test",
        DEBUG=True,
    )
    return create_app(settings)


@pytest.fixture
def client(app_instance):
    """Create test client."""
    with TestClient(app_instance) as c:
        yield c


def test_root_endpoint(client):
    """GET / should return application metadata and status."""
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert data["app"] == "TRUSTLENS API"
    assert data["status"] == "running"
    assert "version" in data
    assert "docs_url" in data


def test_health_endpoint(client):
    """GET /health should return system health status."""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "threat_intel_configured" in data
    assert "timestamp" in data


def test_analyze_endpoint_unconfigured_intel(client):
    """When threat intel is unconfigured, benign URL returns inconclusive without false safe claims."""
    response = client.post("/api/v1/analyze", json={"url": "https://example.com/portal"})
    assert response.status_code == 200
    data = response.json()
    assert data["is_simulated"] is False
    assert data["assessment"] == RiskCategory.inconclusive.value
    assert data["threat_intelligence"]["status"] == ThreatIntelStatus.not_configured.value
    assert data["url_validation"]["valid"] is True
    assert data["url_validation"]["hostname"] == "example.com"


def test_analyze_endpoint_mock_confirmed_threat(app_instance):
    """Mocked threat intelligence confirmed threat takes precedence."""
    mock_provider = MockThreatIntelligenceProvider(
        canned_status=ThreatIntelStatus.confirmed_threat,
        canned_threat_types=["SOCIAL_ENGINEERING"],
        canned_details="Simulated active phishing URL match.",
    )
    app_instance.state.threat_provider = mock_provider

    with TestClient(app_instance) as client:
        response = client.post("/api/v1/analyze", json={"url": "https://example.com/benign-looking"})
        assert response.status_code == 200
        data = response.json()
        assert data["assessment"] == RiskCategory.confirmed_threat.value
        assert "SOCIAL_ENGINEERING" in data["threat_intelligence"]["threat_types"]
        assert len(data["recommended_actions"]) > 0


def test_analyze_endpoint_mock_clean_threat(app_instance):
    """Clean heuristics combined with clean threat intelligence returns no_known_threat."""
    mock_provider = MockThreatIntelligenceProvider(
        canned_status=ThreatIntelStatus.no_threat_found,
        canned_details="No threats found in test database.",
    )
    app_instance.state.threat_provider = mock_provider

    with TestClient(app_instance) as client:
        response = client.post("/api/v1/analyze", json={"url": "https://example.com/docs"})
        assert response.status_code == 200
        data = response.json()
        assert data["assessment"] == RiskCategory.no_known_threat.value
        assert data["threat_intelligence"]["status"] == ThreatIntelStatus.no_threat_found.value


def test_analyze_endpoint_mock_timeout(app_instance):
    """When external threat intelligence times out, result is marked inconclusive."""
    mock_provider = MockThreatIntelligenceProvider(
        canned_status=ThreatIntelStatus.timeout,
        canned_details="Network request timed out.",
    )
    app_instance.state.threat_provider = mock_provider

    with TestClient(app_instance) as client:
        response = client.post("/api/v1/analyze", json={"url": "https://example.com/search"})
        assert response.status_code == 200
        data = response.json()
        assert data["assessment"] == RiskCategory.inconclusive.value
        assert data["threat_intelligence"]["status"] == ThreatIntelStatus.timeout.value


def test_analyze_endpoint_suspicious_brand_indicators(client):
    """Deceptive brand in subdomain triggers suspicious_indicators."""
    response = client.post(
        "/api/v1/analyze",
        json={"url": "https://paypal.com.verify-account.test/login"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["assessment"] == RiskCategory.suspicious_indicators.value
    rule_ids = [f["rule_id"] for f in data["findings"]]
    assert "RULE_BRAND_IMPERSONATION" in rule_ids


def test_analyze_validation_errors(client):
    """Validation errors return 400 with structured error codes."""
    # Unsupported scheme
    res1 = client.post("/api/v1/analyze", json={"url": "ftp://files.example.com/test"})
    assert res1.status_code == 400
    assert res1.json()["code"] == "UNSUPPORTED_SCHEME"

    # Embedded credentials
    res2 = client.post("/api/v1/analyze", json={"url": "https://admin:pass@example.com/"})
    assert res2.status_code == 400
    assert res2.json()["code"] == "EMBEDDED_CREDENTIALS"

    # Missing scheme
    res3 = client.post("/api/v1/analyze", json={"url": "example.com"})
    assert res3.status_code == 400
    assert res3.json()["code"] == "MISSING_SCHEME"


def test_analyze_schema_validation_error(client):
    """Malformed request payload returns 422 Unprocessable Entity."""
    # Missing required 'url' field
    res = client.post("/api/v1/analyze", json={})
    assert res.status_code == 422
    assert res.json()["error"] == "REQUEST_VALIDATION_ERROR"


# Demo Endpoint Tests

def test_demo_threat_detected_scenario(client):
    """POST /api/v1/demo returns threat_detected scenario marked is_simulated: true."""
    response = client.post("/api/v1/demo", json={"scenario": "threat_detected"})
    assert response.status_code == 200
    data = response.json()
    assert data["is_simulated"] is True
    assert data["assessment"] == RiskCategory.confirmed_threat.value
    assert data["threat_intelligence"]["status"] == ThreatIntelStatus.confirmed_threat.value
    assert "DEMONSTRATION SIMULATION" in data["summary"]


def test_demo_no_known_threat_scenario(client):
    """POST /api/v1/demo returns no_known_threat scenario marked is_simulated: true."""
    response = client.post("/api/v1/demo", json={"scenario": "no_known_threat"})
    assert response.status_code == 200
    data = response.json()
    assert data["is_simulated"] is True
    assert data["assessment"] == RiskCategory.no_known_threat.value
    assert data["threat_intelligence"]["status"] == ThreatIntelStatus.no_threat_found.value


def test_demo_inconclusive_scenario(client):
    """POST /api/v1/demo returns inconclusive scenario marked is_simulated: true."""
    response = client.post("/api/v1/demo", json={"scenario": "inconclusive"})
    assert response.status_code == 200
    data = response.json()
    assert data["is_simulated"] is True
    assert data["assessment"] == RiskCategory.inconclusive.value
    assert data["threat_intelligence"]["status"] == ThreatIntelStatus.service_unavailable.value


def test_demo_invalid_scenario(client):
    """POST /api/v1/demo rejects invalid scenarios with 422."""
    response = client.post("/api/v1/demo", json={"scenario": "invalid_scenario"})
    assert response.status_code == 422


def test_cors_preflight(client):
    """Verify CORS preflight headers respond properly for permitted origins."""
    headers = {
        "Origin": "http://localhost:3000",
        "Access-Control-Request-Method": "POST",
    }
    response = client.options("/api/v1/analyze", headers=headers)
    assert response.status_code == 200
    assert response.headers.get("access-control-allow-origin") == "http://localhost:3000"
