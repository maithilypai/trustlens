import pytest
from app.qr_analyzer import analyze_qr_code_payload, classify_qr_payload
from app.schemas import RiskCategory, SeverityLevel, ThreatIntelStatus
from app.threat_intelligence import MockThreatIntelligenceProvider


def test_classify_qr_payload_types():
    """Verify accurate classification of diverse QR URI schemes."""
    # URL
    t1, m1 = classify_qr_payload("https://example.com/menu")
    assert t1 == "url"
    assert m1["url"] == "https://example.com/menu"

    # UPI Payment
    t2, m2 = classify_qr_payload("upi://pay?pa=merchant@bank&pn=CityCafe&am=15.50&cu=USD")
    assert t2 == "payment"
    assert m2["scheme"] == "upi"
    assert m2["payee_address"] == "merchant@bank"
    assert m2["amount"] == "15.50"

    # Bitcoin Payment
    t3, m3 = classify_qr_payload("bitcoin:1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa?amount=0.05")
    assert t3 == "payment"
    assert m3["scheme"] == "bitcoin"

    # Wi-Fi Configuration
    t4, m4 = classify_qr_payload("WIFI:S:CoffeeShop_Free;T:WPA;P:coffee123;;")
    assert t4 == "wifi"
    assert m4["ssid"] == "CoffeeShop_Free"
    assert m4["security_type"] == "WPA"

    # Telephony
    t5, m5 = classify_qr_payload("tel:+18005550199")
    assert t5 == "telephony"
    assert m5["number"] == "+18005550199"

    # Plain Text
    t6, m6 = classify_qr_payload("Hello world, this is a plain message.")
    assert t6 == "plain_text"


@pytest.mark.asyncio
async def test_qr_payment_transfer_analysis():
    """Detect direct payment transfer QR codes and alert user that scanning sends money."""
    payload = "upi://pay?pa=utility-bills@scambank&pn=PublicParking&am=35.00&cu=USD"
    payload_type, assessment, findings, url_analysis, summary, actions = await analyze_qr_code_payload(payload)

    assert payload_type == "payment"
    assert assessment == RiskCategory.suspicious_indicators
    payment_findings = [f for f in findings if f.rule_id == "QR_RULE_DIRECT_PAYMENT_TRANSFER"]
    assert len(payment_findings) == 1
    assert payment_findings[0].severity == SeverityLevel.high
    assert "OUTGOING" in actions[0]


@pytest.mark.asyncio
async def test_qr_quishing_confirmed_threat():
    """QR code encoding a confirmed malicious URL is flagged as confirmed_threat."""
    mock_provider = MockThreatIntelligenceProvider(
        canned_status=ThreatIntelStatus.confirmed_threat,
        canned_threat_types=["SOCIAL_ENGINEERING"],
        canned_details="Phishing distribution link.",
    )
    payload = "https://parking-meter.verify-session.test/pay"
    payload_type, assessment, findings, url_analysis, summary, actions = await analyze_qr_code_payload(
        payload=payload,
        threat_provider=mock_provider,
    )

    assert payload_type == "url"
    assert assessment == RiskCategory.confirmed_threat
    assert url_analysis is not None
    assert url_analysis["assessment"] == RiskCategory.confirmed_threat.value
    assert "CRITICAL QR THREAT" in summary


@pytest.mark.asyncio
async def test_qr_wifi_provisioning_warning():
    """QR code auto-configuring Wi-Fi includes security awareness finding."""
    payload = "WIFI:S:PublicMall_Free;T:WPA;P:password123;;"
    payload_type, assessment, findings, url_analysis, summary, actions = await analyze_qr_code_payload(payload)

    assert payload_type == "wifi"
    assert assessment == RiskCategory.suspicious_indicators
    wifi_findings = [f for f in findings if f.rule_id == "QR_RULE_WIFI_PROVISIONING"]
    assert len(wifi_findings) == 1
    assert "PublicMall_Free" in wifi_findings[0].evidence
