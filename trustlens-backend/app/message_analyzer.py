from __future__ import annotations

import re
from typing import List, Optional, Tuple

from app.risk_assessment import evaluate_risk
from app.schemas import (
    Finding,
    RiskCategory,
    SeverityLevel,
    ThreatIntelStatus,
    ThreatIntelligenceResult,
    UrlValidationDetails,
)
from app.threat_intelligence import ThreatIntelligenceProvider
from app.url_analyzer import analyze_url_structure
from app.url_validator import UrlValidationError, validate_and_normalize_url

# Regex to safely detect URLs embedded in text messages without downloading
URL_REGEX = re.compile(
    r"(?:https?://|www\.)[a-zA-Z0-9.\-_~:/?#\[\]@!$&'()*+,;=%]+",
    re.IGNORECASE,
)

# Known URL shorteners often abused in SMS phishing
KNOWN_SHORTENERS = {
    "bit.ly", "tinyurl.com", "t.co", "is.gd", "ow.ly", "cutt.ly",
    "rb.gy", "shorturl.at", "tiny.cc", "goo.gl", "rebrand.ly",
}

# Lure patterns with regex matching for explainable detection
OTP_PATTERNS = [
    r"\b(?:otp|one-time\s+password|verification\s+code|security\s+code|2fa\s+code|6-digit\s+code)\b",
    r"\b(?:enter\s+your\s+code|share\s+(?:your\s+)?code|do\s+not\s+share\s+this\s+code)\b",
    r"\b(?:temporary\s+pin|auth(?:entication)?\s+code)\b",
]

PARCEL_PATTERNS = [
    r"\b(?:package|parcel|shipment|delivery|postal|courier)\b",
    r"\b(?:customs\s+fee|unpaid\s+fee|redelivery|reschedule\s+delivery|address\s+confirmation)\b",
    r"\b(?:usps|dhl|fedex|ups|royal\s+mail|evri|dpd|post\s+office)\b",
]

URGENCY_PATTERNS = [
    r"\b(?:urgent|immediately|act\s+now|immediate\s+action|within\s+(?:24|12|48)\s+hours)\b",
    r"\b(?:suspended|locked|deactivated|terminated|disabled|freeze|frozen)\b",
    r"\b(?:final\s+notice|last\s+chance|critical\s+alert|security\s+alert)\b",
]

PAYMENT_PATTERNS = [
    r"\b(?:unauthorized\s+transaction|wire\s+transfer|charge\s+of\s+\$|deducted\s+from\s+your)\b",
    r"\b(?:refund|claim\s+your\s+prize|won\s+\$|lottery|crypto\s+wallet|bonus)\b",
    r"\b(?:overdue\s+invoice|payment\s+declined|billing\s+update|tax\s+rebate)\b",
]

AUTHORITY_PATTERNS = [
    r"\b(?:internal\s+revenue|irs|hmrc|tax\s+department|social\s+security)\b",
    r"\b(?:fraud\s+department|investigation\s+unit|law\s+enforcement|court\s+notice|police)\b",
    r"\b(?:bank\s+security|fraud\s+prevention\s+team)\b",
]


def extract_urls(text: str) -> List[str]:
    """Safely extracts all unique HTTP(S) and www URLs from message text."""
    matches = URL_REGEX.findall(text)
    urls: List[str] = []
    seen = set()
    for m in matches:
        # Strip trailing punctuation that often attaches to URLs at the end of sentences
        clean = m.rstrip(".,;:!?)'\"")
        if clean.lower().startswith("www."):
            clean = "https://" + clean
        if clean and clean not in seen:
            seen.add(clean)
            urls.append(clean)
    return urls


def _check_shortener(url_str: str) -> Optional[Finding]:
    """Detects shortened URL services commonly used to conceal destination hosts in SMS."""
    try:
        val = validate_and_normalize_url(url_str)
        host = val.hostname.lower()
        if host in KNOWN_SHORTENERS:
            return Finding(
                rule_id="MSG_RULE_SUSPICIOUS_LINK_SHORTENER",
                title="URL Shortening Service Detected",
                severity=SeverityLevel.medium,
                evidence=f"Message contains shortened link using '{host}'.",
                explanation=(
                    "Scammers frequently use URL shorteners in SMS messages to disguise the actual destination "
                    "domain and evade domain reputation blocklists on mobile devices."
                ),
                recommendation="Do not tap shortened links from unknown senders. Inspect the unshortened destination first.",
            )
    except Exception:
        pass
    return None


def analyze_message_content(
    text: str,
    sender: Optional[str] = None,
) -> Tuple[List[Finding], List[str]]:
    """
    Analyzes message body for transparent social engineering and scam tactics.

    Guarantees:
    - Never stores or persists message text.
    - Completely in-memory and rule-based.
    - Returns explainable findings with evidence and user recommendations.
    """
    findings: List[Finding] = []
    lower_text = text.lower()

    # 1. OTP / Credential Theft Solicitation
    for pat in OTP_PATTERNS:
        match = re.search(pat, lower_text)
        if match:
            findings.append(
                Finding(
                    rule_id="MSG_RULE_OTP_CREDENTIAL_SOLICITATION",
                    title="Credential / One-Time Passcode (OTP) Solicitation",
                    severity=SeverityLevel.critical,
                    evidence=f"Text references credential solicitation: '{match.group(0)}'.",
                    explanation=(
                        "Legitimate banks, payment services, and tech companies NEVER ask customers to share an OTP, "
                        "temporary PIN, or verification code via SMS. Attackers use this tactic to bypass 2-Factor Authentication."
                    ),
                    recommendation="NEVER share OTP codes or passwords with anyone. Banks and official services never ask for them.",
                )
            )
            break

    # 2. Parcel / Package Delivery Lure
    parcel_hits = [re.search(pat, lower_text) for pat in PARCEL_PATTERNS]
    valid_parcel_hits = [h.group(0) for h in parcel_hits if h]
    if len(valid_parcel_hits) >= 2:
        findings.append(
            Finding(
                rule_id="MSG_RULE_PARCEL_DELIVERY_SCAM",
                title="Impersonated Delivery / Postal Fee Scam Lure",
                severity=SeverityLevel.high,
                evidence=f"Delivery lure terms identified: {', '.join(valid_parcel_hits)}.",
                explanation=(
                    "Smishing (SMS phishing) attacks frequently pose as postal delivery services (USPS, DHL, FedEx) "
                    "claiming a parcel requires address verification or an unpaid fee to harvest credit card numbers."
                ),
                recommendation="Do not follow links in delivery notification messages. Verify tracking numbers directly on official postal carrier apps.",
            )
        )

    # 3. Urgent Pressure / Intimidation
    urgency_hits = [h.group(0) for pat in URGENCY_PATTERNS if (h := re.search(pat, lower_text))]
    if urgency_hits:
        findings.append(
            Finding(
                rule_id="MSG_RULE_URGENCY_PRESSURE",
                title="Artificial Urgency & Threat Pressure",
                severity=SeverityLevel.medium,
                evidence=f"Urgency phrases detected: {', '.join(urgency_hits)}.",
                explanation=(
                    "Scammers create artificial deadlines and panic (e.g. 'account suspended within 24 hours') "
                    "to prevent victims from pausing to verify the request."
                ),
                recommendation="Pause before responding. Urgent threats of account suspension in unsolicited messages are standard social engineering tactics.",
            )
        )

    # 4. Financial / Payment / Prize Lures
    payment_hits = [h.group(0) for pat in PAYMENT_PATTERNS if (h := re.search(pat, lower_text))]
    if payment_hits:
        findings.append(
            Finding(
                rule_id="MSG_RULE_FINANCIAL_PAYMENT_PRESSURE",
                title="Unsolicited Financial Transaction / Prize Lure",
                severity=SeverityLevel.high,
                evidence=f"Financial lure keywords identified: {', '.join(payment_hits)}.",
                explanation=(
                    "Fraudulent messages often claim an unauthorized charge occurred or that you are owed an unexpected refund/prize, "
                    "prompting you to click a link to 'cancel' or 'claim' funds."
                ),
                recommendation="Verify bank transactions directly in your authentic banking app. Never use links or phone numbers provided in unsolicited messages.",
            )
        )

    # 5. Authority Impersonation
    auth_hits = [h.group(0) for pat in AUTHORITY_PATTERNS if (h := re.search(pat, lower_text))]
    if auth_hits:
        findings.append(
            Finding(
                rule_id="MSG_RULE_AUTHORITY_IMPERSONATION",
                title="Government / Financial Authority Impersonation",
                severity=SeverityLevel.high,
                evidence=f"Authority terms detected: {', '.join(auth_hits)}.",
                explanation=(
                    "Scammers impersonate government tax bodies, fraud departments, or law enforcement to intimidate recipients into compliance."
                ),
                recommendation="Government agencies and law enforcement do not contact citizens over SMS to demand payments or account verifications.",
            )
        )

    # Extract all embedded URLs
    extracted_urls = extract_urls(text)
    for url in extracted_urls:
        shortener_finding = _check_shortener(url)
        if shortener_finding:
            findings.append(shortener_finding)

    return findings, extracted_urls


async def analyze_suspicious_message(
    text: str,
    sender: Optional[str] = None,
    threat_provider: Optional[ThreatIntelligenceProvider] = None,
) -> Tuple[RiskCategory, List[Finding], List[dict], str, List[str]]:
    """
    Comprehensive multi-factor analysis of a message text and any embedded URLs.

    Returns:
        (risk_category, findings, embedded_url_analyses, summary, recommended_actions)
    """
    findings, urls = analyze_message_content(text, sender)
    embedded_url_analyses = []
    has_confirmed_threat_url = False
    has_suspicious_url = False

    for url_str in urls:
        try:
            val_details = validate_and_normalize_url(url_str)
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

            if url_assessment == RiskCategory.confirmed_threat:
                has_confirmed_threat_url = True
            elif url_assessment == RiskCategory.suspicious_indicators:
                has_suspicious_url = True

            embedded_url_analyses.append({
                "url": url_str,
                "validation": val_details.model_dump(),
                "assessment": url_assessment.value,
                "findings": [f.model_dump() for f in url_findings],
                "threat_intelligence": threat_result.model_dump(),
                "summary": url_summary,
            })
        except UrlValidationError as exc:
            findings.append(
                Finding(
                    rule_id="MSG_RULE_MALFORMED_OR_TRICK_URL",
                    title="Malformed / Obfuscated Link in Message",
                    severity=SeverityLevel.high,
                    evidence=f"Embedded link '{url_str}' failed validation: {exc.message}",
                    explanation="Scam messages frequently embed malformed or non-standard URLs to evade filters.",
                    recommendation="Do not tap malformed links.",
                )
            )

    # Risk synthesis for the message
    if has_confirmed_threat_url:
        assessment = RiskCategory.confirmed_threat
        summary = (
            "CRITICAL THREAT: An embedded link in this message is confirmed malicious by threat intelligence. "
            "This message is an active phishing or malware delivery campaign."
        )
        recommended_actions = [
            "Do not click any links or reply to this message.",
            "Report the message as spam/phishing to your mobile carrier (forward to 7726).",
            "Block the sender number immediately.",
        ]
    elif any(f.severity in (SeverityLevel.critical, SeverityLevel.high) for f in findings) or has_suspicious_url:
        assessment = RiskCategory.suspicious_indicators
        high_rule_titles = [f.title for f in findings if f.severity in (SeverityLevel.critical, SeverityLevel.high)]
        summary = (
            f"SUSPICIOUS SCAM INDICATORS DETECTED: {', '.join(high_rule_titles) if high_rule_titles else 'Deceptive link detected'}. "
            "This message exhibits social engineering patterns typical of smishing and account takeover attacks."
        )
        recommended_actions = [
            "Do not share passwords, OTPs, or financial information.",
            "Never use the contact numbers or links provided in the message.",
            "Verify the claimed status by contacting the entity through their official website or phone number.",
        ]
    elif any(f.severity == SeverityLevel.medium for f in findings):
        assessment = RiskCategory.suspicious_indicators
        summary = (
            "CAUTION: This message contains pressure tactics or indicators that warrant heightened vigilance."
        )
        recommended_actions = [
            "Pause and verify independently before responding.",
            "Confirm identity through an established secondary channel.",
        ]
    else:
        assessment = RiskCategory.no_known_threat
        summary = (
            "No known scam patterns, high-urgency lures, or malicious links were detected in this message. "
            "Continue exercising standard digital caution."
        )
        recommended_actions = [
            "Always verify unexpected requests independently.",
            "Never disclose private security codes to unsolicited callers or texters.",
        ]

    return assessment, findings, embedded_url_analyses, summary, recommended_actions
