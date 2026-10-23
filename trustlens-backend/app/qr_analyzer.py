from __future__ import annotations

import re
from typing import List, Optional, Tuple
import urllib.parse

from app.risk_assessment import evaluate_risk
from app.schemas import (
    Finding,
    RiskCategory,
    SeverityLevel,
    ThreatIntelStatus,
    ThreatIntelligenceResult,
)
from app.threat_intelligence import ThreatIntelligenceProvider
from app.url_analyzer import analyze_url_structure
from app.url_validator import UrlValidationError, validate_and_normalize_url


def classify_qr_payload(payload: str) -> Tuple[str, dict]:
    """
    Classifies the raw string payload decoded from a QR code.

    Returns:
        (payload_type, metadata_dict)
        where payload_type is one of: 'url', 'payment', 'wifi', 'contact', 'telephony', 'intent', 'plain_text'
    """
    clean = payload.strip()
    lower = clean.lower()

    # 1. HTTP / HTTPS URL
    if lower.startswith("http://") or lower.startswith("https://"):
        return "url", {"url": clean}

    # 2. UPI / Cryptocurrency Payment Schemes
    if lower.startswith("upi://pay"):
        parsed = urllib.parse.urlsplit(clean)
        query = urllib.parse.parse_qs(parsed.query)
        pa = query.get("pa", [None])[0]  # Payee VPA
        pn = query.get("pn", [None])[0]  # Payee Name
        am = query.get("am", [None])[0]  # Amount
        cu = query.get("cu", ["INR"])[0]  # Currency
        return "payment", {"scheme": "upi", "payee_address": pa, "payee_name": pn, "amount": am, "currency": cu}

    if lower.startswith("bitcoin:") or lower.startswith("ethereum:") or lower.startswith("crypto:"):
        scheme = lower.split(":")[0]
        return "payment", {"scheme": scheme, "raw": clean}

    # 3. Wi-Fi Configuration URI (WIFI:S:ssid;T:WPA;P:password;;)
    if lower.startswith("wifi:"):
        ssid_match = re.search(r"S:([^;]+)", clean, re.IGNORECASE)
        type_match = re.search(r"T:([^;]+)", clean, re.IGNORECASE)
        ssid = ssid_match.group(1) if ssid_match else "Hidden/Unknown"
        sec_type = type_match.group(1) if type_match else "Unknown"
        return "wifi", {"ssid": ssid, "security_type": sec_type}

    # 4. Telephony / SMS / Mailto
    if lower.startswith("tel:"):
        number = clean[4:]
        return "telephony", {"type": "phone_call", "number": number}

    if lower.startswith("smsto:") or lower.startswith("sms:"):
        parts = clean.split(":")
        number = parts[1] if len(parts) > 1 else ""
        return "telephony", {"type": "sms", "recipient": number}

    if lower.startswith("mailto:"):
        return "contact", {"type": "email", "recipient": clean[7:]}

    # 5. Application Intent
    if lower.startswith("intent://") or lower.startswith("market://"):
        return "intent", {"raw": clean}

    # 6. Plain text that might contain an embedded URL
    url_match = re.search(r"https?://[^\s<>]+", clean)
    if url_match:
        return "url", {"url": url_match.group(0), "wrapped_in_text": True}

    return "plain_text", {"length": len(clean)}


async def analyze_qr_code_payload(
    payload: str,
    threat_provider: Optional[ThreatIntelligenceProvider] = None,
) -> Tuple[str, RiskCategory, List[Finding], Optional[dict], str, List[str]]:
    """
    Analyzes a QR code payload for quishing (QR phishing), payment scams, and rogue redirection.

    Guarantees:
    - Zero network execution to arbitrary targets.
    - Strict local classification.
    - Returns structured explainable findings.

    Returns:
        (payload_type, assessment, findings, url_analysis_dict, summary, recommended_actions)
    """
    clean_payload = payload.strip()
    payload_type, metadata = classify_qr_payload(clean_payload)
    findings: List[Finding] = []
    url_analysis_dict: Optional[dict] = None

    # Handle URL payload (Quishing / Phishing Risk)
    if payload_type == "url":
        target_url = metadata.get("url", clean_payload)
        try:
            val_details = validate_and_normalize_url(target_url)
            url_findings = analyze_url_structure(val_details)
            threat_result = None

            if threat_provider:
                threat_result = await threat_provider.check_url(val_details.normalized_url)
            else:
                threat_result = ThreatIntelligenceResult(
                    status=ThreatIntelStatus.not_configured,
                    details="Threat intelligence check not configured.",
                )

            url_assessment, url_summary, url_actions = evaluate_risk(url_findings, threat_result)

            # Add quishing educational finding
            findings.append(
                Finding(
                    rule_id="QR_RULE_QUISHING_RISK",
                    title="QR Code Web Destination (Quishing Consideration)",
                    severity=SeverityLevel.info,
                    evidence=f"Decoded QR target: {val_details.normalized_url}",
                    explanation=(
                        "QR codes in public spaces (such as parking meters, restaurant tables, or shared scooters) "
                        "can be covered with malicious sticker overlays to redirect mobile users to phishing portals."
                    ),
                    recommendation="Ensure the destination domain exactly corresponds to the physical venue before entering credentials or making payments.",
                )
            )

            # Inherit URL structural findings
            findings.extend(url_findings)

            url_analysis_dict = {
                "validation": val_details.model_dump(),
                "assessment": url_assessment.value,
                "findings": [f.model_dump() for f in url_findings],
                "threat_intelligence": threat_result.model_dump(),
                "summary": url_summary,
            }

            if url_assessment == RiskCategory.confirmed_threat:
                assessment = RiskCategory.confirmed_threat
                summary = (
                    "CRITICAL QR THREAT: The web address encoded in this QR code is confirmed malicious by threat intelligence. "
                    "This is an active quishing (QR phishing) or malware distribution attack."
                )
                recommended_actions = [
                    "Do NOT open this website or proceed in your browser.",
                    "If this was a physical sticker on a kiosk or meter, report it to the venue operator immediately.",
                    url_actions[0] if url_actions else "Close your camera/scanner application.",
                ]
                return payload_type, assessment, findings, url_analysis_dict, summary, recommended_actions

            elif url_assessment == RiskCategory.suspicious_indicators:
                assessment = RiskCategory.suspicious_indicators
                summary = (
                    f"SUSPICIOUS QR DESTINATION: Local analysis detected suspicious indicators in the QR link ({url_summary}). "
                    "Exercise extreme caution before submitting any payment or personal info."
                )
                recommended_actions = [
                    "Inspect the domain name carefully. Do not enter passwords or card details.",
                    "Verify physical stickers on kiosks to ensure they have not been placed over authentic codes.",
                ]
                return payload_type, assessment, findings, url_analysis_dict, summary, recommended_actions

            elif url_assessment == RiskCategory.no_known_threat:
                assessment = RiskCategory.no_known_threat
                summary = (
                    "No known threats or heuristic anomalies detected in the decoded QR URL. "
                    "Confirm the domain corresponds to your intended physical or digital service."
                )
                recommended_actions = [
                    "Verify that the web connection is secure (HTTPS).",
                    "Confirm you are interacting with the authentic provider.",
                ]
                return payload_type, assessment, findings, url_analysis_dict, summary, recommended_actions

            else:
                assessment = RiskCategory.inconclusive
                summary = (
                    "Decoded QR URL shows no local heuristic red flags, but external threat intelligence could not be corroborated."
                )
                recommended_actions = [
                    "Verify the domain independently before logging in or making payments.",
                ]
                return payload_type, assessment, findings, url_analysis_dict, summary, recommended_actions

        except UrlValidationError as exc:
            findings.append(
                Finding(
                    rule_id="QR_RULE_MALFORMED_URL",
                    title="Malformed URL in QR Code",
                    severity=SeverityLevel.high,
                    evidence=f"URL parsing failed: {exc.message}",
                    explanation="QR codes containing malformed or unparseable URLs often attempt parser evasion.",
                    recommendation="Do not proceed with this QR code.",
                )
            )
            return payload_type, RiskCategory.suspicious_indicators, findings, None, "Malformed QR code payload.", ["Do not open this QR code."]

    # Handle Direct Payment Transfer URIs (UPI / Crypto)
    elif payload_type == "payment":
        scheme = metadata.get("scheme", "payment").upper()
        payee = metadata.get("payee_address") or metadata.get("raw") or "Unknown"
        amount = metadata.get("amount")
        amount_note = f" (Amount: {amount} {metadata.get('currency', '')})" if amount else ""

        findings.append(
            Finding(
                rule_id="QR_RULE_DIRECT_PAYMENT_TRANSFER",
                title=f"Direct Payment / Transfer URI ({scheme})",
                severity=SeverityLevel.high,
                evidence=f"Payment recipient: '{payee}'{amount_note}.",
                explanation=(
                    f"This QR code triggers a direct financial payment transaction via {scheme}. "
                    "Scammers frequently use payment QR codes falsely claiming you are 'receiving' a refund, "
                    "lottery payout, or deposit. In reality, scanning a payment QR code can only SEND money, never receive it."
                ),
                recommendation=(
                    "NEVER scan a QR code to 'receive' money. "
                    "Confirm the recipient name and amount carefully in your payment app before confirming any PIN."
                ),
            )
        )
        assessment = RiskCategory.suspicious_indicators
        summary = (
            f"FINANCIAL PAYMENT QR DETECTED: This QR initiates an outbound funds transfer to '{payee}'. "
            "Beware of scammers claiming this code will deposit or refund money to your account."
        )
        recommended_actions = [
            "Never scan a QR code expecting to receive funds. Scanning only authorizes OUTGOING payments.",
            "Verify the recipient name matches the merchant or person you intend to pay.",
            "Never enter your UPI PIN, banking password, or OTP when scanning a QR code.",
        ]
        return payload_type, assessment, findings, None, summary, recommended_actions

    # Handle Wi-Fi Provisioning
    elif payload_type == "wifi":
        ssid = metadata.get("ssid", "Unknown")
        findings.append(
            Finding(
                rule_id="QR_RULE_WIFI_PROVISIONING",
                title="Wi-Fi Network Configuration QR Code",
                severity=SeverityLevel.medium,
                evidence=f"SSID: '{ssid}', Security: '{metadata.get('security_type', 'Unknown')}'.",
                explanation=(
                    "This QR code provisions an automatic Wi-Fi network connection on your device. "
                    "Connecting to rogue or unverified Wi-Fi access points allows attackers to inspect "
                    "or intercept unencrypted network traffic (man-in-the-middle)."
                ),
                recommendation="Only connect if you are in a trusted physical location and recognize the network name.",
            )
        )
        assessment = RiskCategory.suspicious_indicators
        summary = f"Wi-Fi configuration payload for network '{ssid}'. Ensure you trust the network operator."
        recommended_actions = [
            "Confirm this Wi-Fi network belongs to the venue or host you are visiting.",
            "Avoid conducting online banking or entering sensitive passwords on unfamiliar Wi-Fi networks.",
        ]
        return payload_type, assessment, findings, None, summary, recommended_actions

    # Handle Telephony / Auto-Dial
    elif payload_type == "telephony":
        target = metadata.get("number") or metadata.get("recipient") or "Unknown"
        findings.append(
            Finding(
                rule_id="QR_RULE_TELEPHONY_ACTION",
                title="Telephony / SMS Action Payload",
                severity=SeverityLevel.medium,
                evidence=f"Target recipient: '{target}'.",
                explanation=(
                    "This QR code automatically initiates a phone call or SMS message. "
                    "Malicious actors use telephony QR codes to connect victims to fraudulent call centers "
                    "or send premium-rate subscription messages."
                ),
                recommendation="Verify the recipient number before calling or sending messages.",
            )
        )
        assessment = RiskCategory.suspicious_indicators
        summary = f"QR code requests outbound telephony action to '{target}'."
        recommended_actions = [
            "Do not call or send SMS if you do not recognize the number.",
            "Beware of automated prompts asking you to press digits or enter verification codes.",
        ]
        return payload_type, assessment, findings, None, summary, recommended_actions

    # Plain text / other
    else:
        assessment = RiskCategory.no_known_threat
        summary = "QR code contains plain text without active executable schemes, payment URIs, or web links."
        recommended_actions = ["Verify content matches your expectations."]
        return payload_type, assessment, findings, None, summary, recommended_actions
