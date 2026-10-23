from datetime import datetime, timezone
import pytest

from app.risk_assessment import evaluate_risk
from app.schemas import (
    Finding,
    RiskCategory,
    SeverityLevel,
    ThreatIntelStatus,
    ThreatIntelligenceResult,
)
from app.url_analyzer import analyze_url_structure
from app.url_validator import validate_and_normalize_url


def test_brand_impersonation_subdomain():
    """Detect brand name injected into subdomains of an unrelated domain."""
    details = validate_and_normalize_url("https://paypal.com.account-update.xyz/login")
    findings = analyze_url_structure(details)

    brand_findings = [f for f in findings if f.rule_id == "RULE_BRAND_IMPERSONATION"]
    assert len(brand_findings) == 1
    assert brand_findings[0].severity == SeverityLevel.high
    assert "paypal" in brand_findings[0].evidence.lower()
    assert "account-update.xyz" in brand_findings[0].evidence


def test_brand_impersonation_hyphenated():
    """Detect brand names in hyphenated domain prefixes."""
    details = validate_and_normalize_url("https://apple-id-verify-session.net/secure")
    findings = analyze_url_structure(details)

    brand_findings = [f for f in findings if f.rule_id == "RULE_BRAND_IMPERSONATION"]
    assert len(brand_findings) == 1
    assert brand_findings[0].severity == SeverityLevel.high
    assert "apple" in brand_findings[0].evidence.lower()


def test_authentic_brand_not_flagged():
    """Ensure legitimate brand domains are not falsely flagged as impersonation."""
    details = validate_and_normalize_url("https://www.paypal.com/signin")
    findings = analyze_url_structure(details)

    brand_findings = [f for f in findings if f.rule_id == "RULE_BRAND_IMPERSONATION"]
    assert len(brand_findings) == 0


def test_raw_public_ip_host():
    """Detect bare public IP addresses used as hostnames."""
    details = validate_and_normalize_url("https://93.184.216.34/login")
    findings = analyze_url_structure(details)

    ip_findings = [f for f in findings if f.rule_id == "RULE_RAW_IP_HOST"]
    assert len(ip_findings) == 1
    assert ip_findings[0].severity == SeverityLevel.high


def test_private_ip_host():
    """Detect private / loopback IP address hosts."""
    details = validate_and_normalize_url("http://192.168.1.1/setup")
    findings = analyze_url_structure(details)

    private_findings = [f for f in findings if f.rule_id == "RULE_PRIVATE_IP_TARGET"]
    assert len(private_findings) == 1
    assert private_findings[0].severity == SeverityLevel.high


def test_excessive_subdomain_nesting():
    """Detect excessive subdomain levels used to truncate visible domain names."""
    details = validate_and_normalize_url("https://auth.gateway.portal.secure.example.com/verify")
    findings = analyze_url_structure(details)

    subdomain_findings = [f for f in findings if f.rule_id == "RULE_EXCESSIVE_SUBDOMAINS"]
    assert len(subdomain_findings) == 1
    assert subdomain_findings[0].severity == SeverityLevel.medium


def test_non_standard_port():
    """Detect non-standard network ports."""
    details = validate_and_normalize_url("https://example.com:9443/login")
    findings = analyze_url_structure(details)

    port_findings = [f for f in findings if f.rule_id == "RULE_UNUSUAL_PORT"]
    assert len(port_findings) == 1
    assert port_findings[0].severity == SeverityLevel.medium


def test_credential_lure_keywords():
    """Detect sensitive credential lures in path."""
    details = validate_and_normalize_url("https://suspicious-portal.org/verify-account/password-reset")
    findings = analyze_url_structure(details)

    lure_findings = [f for f in findings if f.rule_id == "RULE_CREDENTIAL_LURE_PATH"]
    assert len(lure_findings) == 1
    assert lure_findings[0].severity == SeverityLevel.medium


def test_insecure_http_transport():
    """Detect plain HTTP without asserting definite malice."""
    details = validate_and_normalize_url("http://example.com/public-doc")
    findings = analyze_url_structure(details)

    http_findings = [f for f in findings if f.rule_id == "RULE_INSECURE_HTTP_SCHEME"]
    assert len(http_findings) == 1
    assert http_findings[0].severity == SeverityLevel.low


def test_clean_benign_url():
    """Verify an ordinary HTTPS website generates no alarmist findings."""
    details = validate_and_normalize_url("https://example.com/about")
    findings = analyze_url_structure(details)
    assert len(findings) == 0


# Risk Assessment Matrix Tests

def test_risk_assessment_threat_intel_precedence():
    """Confirmed threat intelligence overrides clean local heuristics."""
    threat_result = ThreatIntelligenceResult(
        status=ThreatIntelStatus.confirmed_threat,
        source="Mock Threat Provider",
        threat_types=["MALWARE"],
        checked_at=datetime.now(timezone.utc),
        details="Known malware distributor.",
    )
    # Zero local findings
    category, summary, actions = evaluate_risk([], threat_result)
    assert category == RiskCategory.confirmed_threat
    assert "MALWARE" in summary
    assert len(actions) > 0


def test_risk_assessment_suspicious_indicators_precedence():
    """High-severity local indicators trigger suspicious_indicators."""
    finding = Finding(
        rule_id="RULE_BRAND_IMPERSONATION",
        title="Deceptive Brand Impersonation Indicator",
        severity=SeverityLevel.high,
        evidence="Brand 'paypal' in subdomain",
        explanation="Phishing lure",
        recommendation="Do not enter credentials",
    )
    threat_result = ThreatIntelligenceResult(
        status=ThreatIntelStatus.no_threat_found,
        source="Mock Threat Provider",
        threat_types=[],
        checked_at=datetime.now(timezone.utc),
    )
    category, summary, actions = evaluate_risk([finding], threat_result)
    assert category == RiskCategory.suspicious_indicators
    assert "Deceptive Brand Impersonation" in summary


def test_risk_assessment_no_known_threat():
    """Clean heuristics corroborated by clean threat intelligence yields no_known_threat."""
    threat_result = ThreatIntelligenceResult(
        status=ThreatIntelStatus.no_threat_found,
        source="Google Safe Browsing v4",
        threat_types=[],
        checked_at=datetime.now(timezone.utc),
    )
    category, summary, actions = evaluate_risk([], threat_result)
    assert category == RiskCategory.no_known_threat
    assert "Neither external threat intelligence nor local structural heuristics" in summary


def test_risk_assessment_inconclusive_when_intel_unconfigured():
    """When threat intel is not configured, clean heuristics return inconclusive (not safe!)."""
    threat_result = ThreatIntelligenceResult(
        status=ThreatIntelStatus.not_configured,
        source=None,
        threat_types=[],
        details="Threat intelligence is not configured.",
    )
    category, summary, actions = evaluate_risk([], threat_result)
    assert category == RiskCategory.inconclusive
    assert "definitive safe verdict cannot be confirmed" in summary


def test_risk_assessment_inconclusive_when_intel_times_out():
    """When threat intel times out, clean heuristics return inconclusive."""
    threat_result = ThreatIntelligenceResult(
        status=ThreatIntelStatus.timeout,
        source="Google Safe Browsing v4",
        threat_types=[],
        details="Query timed out.",
    )
    category, summary, actions = evaluate_risk([], threat_result)
    assert category == RiskCategory.inconclusive
