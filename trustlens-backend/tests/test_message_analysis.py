import pytest
from app.message_analyzer import analyze_message_content, analyze_suspicious_message, extract_urls
from app.schemas import RiskCategory, SeverityLevel, ThreatIntelStatus
from app.threat_intelligence import MockThreatIntelligenceProvider


def test_extract_urls_from_text():
    """Verify safe extraction of multiple URLs from unstructured text."""
    text = (
        "Hello! Check this link: https://legit-site.com/welcome and this one: "
        "http://another-site.org/portal?user=123. Also www.example.com."
    )
    urls = extract_urls(text)
    assert len(urls) == 3
    assert "https://legit-site.com/welcome" in urls
    assert "http://another-site.org/portal?user=123" in urls
    assert "https://www.example.com" in urls


def test_otp_theft_detection():
    """Detect unauthorized OTP credential solicitation."""
    text = "ALERT: Suspicious login detected. Please reply with your 6-digit verification code to keep your account safe."
    findings, urls = analyze_message_content(text)

    otp_findings = [f for f in findings if f.rule_id == "MSG_RULE_OTP_CREDENTIAL_SOLICITATION"]
    assert len(otp_findings) == 1
    assert otp_findings[0].severity == SeverityLevel.critical
    assert "verification code" in otp_findings[0].evidence.lower()


def test_parcel_delivery_smishing():
    """Detect parcel delivery fee traps commonly used in smishing."""
    text = "USPS: Your package is on hold at our courier sorting center due to an unpaid customs fee. Update info at https://usps-redelivery.test/parcel"
    findings, urls = analyze_message_content(text)

    parcel_findings = [f for f in findings if f.rule_id == "MSG_RULE_PARCEL_DELIVERY_SCAM"]
    assert len(parcel_findings) == 1
    assert parcel_findings[0].severity == SeverityLevel.high
    assert len(urls) == 1
    assert "https://usps-redelivery.test/parcel" in urls


def test_urgency_pressure_detection():
    """Detect high-pressure coercive deadlines."""
    text = "FINAL NOTICE: Your service will be terminated immediately within 24 hours if action is not taken."
    findings, urls = analyze_message_content(text)

    urgency_findings = [f for f in findings if f.rule_id == "MSG_RULE_URGENCY_PRESSURE"]
    assert len(urgency_findings) == 1
    assert urgency_findings[0].severity == SeverityLevel.medium


def test_authority_impersonation_detection():
    """Detect government and fraud department impersonation."""
    text = "IRS Notice: The tax department fraud investigation unit has flagged your profile. Immediate payment required."
    findings, urls = analyze_message_content(text)

    auth_findings = [f for f in findings if f.rule_id == "MSG_RULE_AUTHORITY_IMPERSONATION"]
    assert len(auth_findings) == 1
    assert auth_findings[0].severity == SeverityLevel.high


def test_shortened_url_detection():
    """Detect shortened URL services commonly used to obfuscate destinations."""
    text = "Check your refund status here: https://bit.ly/3xRefundNow"
    findings, urls = analyze_message_content(text)

    short_findings = [f for f in findings if f.rule_id == "MSG_RULE_SUSPICIOUS_LINK_SHORTENER"]
    assert len(short_findings) == 1
    assert short_findings[0].severity == SeverityLevel.medium


@pytest.mark.asyncio
async def test_analyze_suspicious_message_with_confirmed_url():
    """Message containing confirmed malicious link produces confirmed_threat."""
    mock_provider = MockThreatIntelligenceProvider(
        canned_status=ThreatIntelStatus.confirmed_threat,
        canned_threat_types=["SOCIAL_ENGINEERING"],
        canned_details="Phishing distribution domain.",
    )
    msg = "Security Alert: Verify your credentials at https://secure-bank.example.test/login immediately."
    assessment, findings, url_analyses, summary, actions = await analyze_suspicious_message(
        text=msg,
        threat_provider=mock_provider,
    )

    assert assessment == RiskCategory.confirmed_threat
    assert len(url_analyses) == 1
    assert url_analyses[0]["assessment"] == RiskCategory.confirmed_threat.value
    assert len(actions) > 0


@pytest.mark.asyncio
async def test_clean_message_yields_no_known_threat():
    """Benign communication without scam patterns yields no_known_threat."""
    msg = "Hey Sarah, are we still meeting for lunch at 12:30 PM today?"
    assessment, findings, url_analyses, summary, actions = await analyze_suspicious_message(text=msg)

    assert assessment == RiskCategory.no_known_threat
    assert len(findings) == 0
    assert len(url_analyses) == 0
