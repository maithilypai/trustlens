import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app
from app.schemas import DemoScenario, RiskCategory


@pytest.fixture
def client():
    settings = Settings(
        SAFE_BROWSING_API_KEY=None,
        ENVIRONMENT="test",
        DEBUG=True,
    )
    app = create_app(settings)
    with TestClient(app) as c:
        yield c


@pytest.mark.parametrize("scenario", [
    DemoScenario.scam_link,
    DemoScenario.parcel_sms,
    DemoScenario.otp_theft,
    DemoScenario.fake_payment,
    DemoScenario.qr_scam,
    DemoScenario.fraud_call,
])
def test_six_educational_scam_scenarios(client, scenario):
    """Verify all six educational scam scenarios return simulated responses with rich findings."""
    response = client.post("/api/v1/demo", json={"scenario": scenario.value})
    assert response.status_code == 200
    data = response.json()
    assert data["is_simulated"] is True
    assert "DEMONSTRATION SIMULATION" in data["summary"]
    assert len(data["findings"]) > 0
    assert len(data["recommended_actions"]) > 0


def test_comparison_demo_endpoints(client):
    """Verify GET and POST /api/v1/demo/compare return side-by-side contrast."""
    # Test POST
    post_res = client.post("/api/v1/demo/compare")
    assert post_res.status_code == 200
    post_data = post_res.json()
    assert post_data["is_simulated"] is True
    assert post_data["simulated_threat"]["assessment"] == RiskCategory.confirmed_threat.value
    assert post_data["harmless_url"]["assessment"] == RiskCategory.no_known_threat.value
    assert "EDUCATIONAL COMPARISON" in post_data["comparison_summary"]
    assert len(post_data["key_differences"]) >= 3

    # Test GET
    get_res = client.get("/api/v1/demo/compare")
    assert get_res.status_code == 200
    get_data = get_res.json()
    assert get_data["is_simulated"] is True


def test_analyze_message_endpoint(client):
    """Verify POST /api/v1/analyze/message inspects SMS and extracts links."""
    payload = {
        "message": "URGENT: USPS shipment #US9821 has unpaid delivery fee. Pay immediately: https://usps-redelivery.test/parcel",
        "sender": "+18005550199"
    }
    response = client.post("/api/v1/analyze/message", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["assessment"] in (RiskCategory.suspicious_indicators.value, RiskCategory.confirmed_threat.value)
    assert len(data["detected_urls"]) == 1
    assert len(data["embedded_url_analyses"]) == 1
    assert data["message_length"] == len(payload["message"])
    assert len(data["recommended_actions"]) > 0


def test_analyze_qr_endpoint(client):
    """Verify POST /api/v1/analyze/qr detects direct payment schemes."""
    payload = {
        "payload": "upi://pay?pa=scam-billing@bank&pn=UtilityFee&am=50.00&cu=USD"
    }
    response = client.post("/api/v1/analyze/qr", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["payload_type"] == "payment"
    assert data["assessment"] == RiskCategory.suspicious_indicators.value
    rule_ids = [f["rule_id"] for f in data["findings"]]
    assert "QR_RULE_DIRECT_PAYMENT_TRANSFER" in rule_ids


def test_health_endpoint_capabilities(client):
    """Verify GET /health and GET / advertise backend capabilities."""
    health_res = client.get("/health")
    assert health_res.status_code == 200
    health_data = health_res.json()
    assert "capabilities" in health_data
    assert "message_sms_analysis" in health_data["capabilities"]
    assert "qr_payload_analysis" in health_data["capabilities"]
    assert "educational_scam_scenarios" in health_data["capabilities"]
    assert "comparison_demo" in health_data["capabilities"]

    root_res = client.get("/")
    assert root_res.status_code == 200
    assert "capabilities" in root_res.json()
