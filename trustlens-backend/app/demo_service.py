from __future__ import annotations

from datetime import datetime, timezone
from typing import Optional
import uuid

from app.schemas import (
    AnalyzeResponse,
    ComparisonDemoResponse,
    DemoScenario,
    Finding,
    RiskCategory,
    SeverityLevel,
    ThreatIntelStatus,
    ThreatIntelligenceResult,
    UrlValidationDetails,
)
from app.url_validator import validate_and_normalize_url


def generate_demo_analysis(
    scenario: DemoScenario,
    custom_url: Optional[str] = None,
) -> AnalyzeResponse:
    """
    Produces deterministic, controlled demonstration analysis responses for educational evaluation.

    Guarantees:
    - Every response explicitly sets `is_simulated = True`.
    - Never communicates with real malicious websites.
    - Does not alter or falsely classify real URLs in production analysis.
    - Covers six distinct real-world scam modalities + comparison.
    """
    request_id = f"demo-{uuid.uuid4().hex[:12]}"
    now_utc = datetime.now(timezone.utc)

    # 1. SCAM LINK / THREAT DETECTED (Classic Deceptive Brand Phishing)
    if scenario in (DemoScenario.threat_detected, DemoScenario.scam_link):
        target_url = custom_url or "https://secure-login.chase-verification.example.test/auth-portal"
        try:
            val_details = validate_and_normalize_url(target_url)
        except Exception:
            val_details = UrlValidationDetails(
                valid=True,
                scheme="https",
                hostname="secure-login.chase-verification.example.test",
                normalized_url=target_url,
                path="/auth-portal",
            )

        findings = [
            Finding(
                rule_id="RULE_BRAND_IMPERSONATION",
                title="Deceptive Brand Impersonation Indicator",
                severity=SeverityLevel.high,
                evidence="Brand 'chase' detected in host prefix, while registered apex domain is 'example.test'.",
                explanation=(
                    "Simulated Scenario: Threat actors frequently use brand names in subdomains or prefix labels "
                    "to fabricate a deceptive aura of authenticity."
                ),
                recommendation="Do not enter passwords or financial information. Visit the official banking portal directly.",
            ),
            Finding(
                rule_id="RULE_CREDENTIAL_LURE_PATH",
                title="Credential / Account Verification Lure",
                severity=SeverityLevel.medium,
                evidence="URL path contains credential lure keywords: 'auth-portal'.",
                explanation="Simulated Scenario: Deceptive security verification portals manipulate users into logging in on illegitimate servers.",
                recommendation="Navigate to the provider directly via the official mobile app or bookmark.",
            ),
        ]

        threat_intel = ThreatIntelligenceResult(
            status=ThreatIntelStatus.confirmed_threat,
            source="TRUSTLENS Educational Demonstration Provider",
            threat_types=["SOCIAL_ENGINEERING", "MALWARE"],
            checked_at=now_utc,
            details="DEMONSTRATION SIMULATION: Known phishing simulation feed matched target URL.",
        )

        return AnalyzeResponse(
            request_id=request_id,
            assessment=RiskCategory.confirmed_threat,
            is_simulated=True,
            url_validation=val_details,
            findings=findings,
            threat_intelligence=threat_intel,
            summary=(
                "DEMONSTRATION SIMULATION: External threat intelligence verified this target as a confirmed threat (SOCIAL_ENGINEERING). "
                "Interaction presents immediate risk of credential theft or malware download."
            ),
            recommended_actions=[
                "Do not click this link or submit any credentials.",
                "Report the message as phishing or scam to your messaging carrier.",
                "If you entered credentials previously, change your passwords immediately.",
            ],
        )

    # 2. PARCEL / SMS SCAM (Postal Smishing & Customs Fee Trap)
    elif scenario == DemoScenario.parcel_sms:
        target_url = custom_url or "https://usps-redelivery-fee.example.test/parcel"
        try:
            val_details = validate_and_normalize_url(target_url)
        except Exception:
            val_details = UrlValidationDetails(
                valid=True,
                scheme="https",
                hostname="usps-redelivery-fee.example.test",
                normalized_url=target_url,
                path="/parcel",
            )

        findings = [
            Finding(
                rule_id="RULE_BRAND_IMPERSONATION",
                title="Postal Service Brand Impersonation (USPS Lure)",
                severity=SeverityLevel.high,
                evidence="Postal brand 'usps' identified in host prefix, while base domain is 'example.test'.",
                explanation=(
                    "Parcel delivery smishing attacks impersonate postal operators (USPS, DHL, FedEx) "
                    "demanding small fees (e.g. $1.85) to trick victims into entering complete credit card credentials."
                ),
                recommendation="Do not pay fees via SMS links. Track packages directly on official postal carrier apps.",
            ),
            Finding(
                rule_id="RULE_CREDENTIAL_LURE_PATH",
                title="Delivery Redirection / Billing Lure",
                severity=SeverityLevel.medium,
                evidence="URL path contains delivery action keywords: 'parcel'.",
                explanation="Scammers claim delivery has failed to induce urgency and prompt hasty payment.",
                recommendation="Official carriers will leave a physical card or update the official tracking number.",
            ),
        ]

        threat_intel = ThreatIntelligenceResult(
            status=ThreatIntelStatus.confirmed_threat,
            source="TRUSTLENS Educational Demonstration Provider",
            threat_types=["SOCIAL_ENGINEERING"],
            checked_at=now_utc,
            details="DEMONSTRATION SIMULATION: Known delivery smishing campaign template.",
        )

        return AnalyzeResponse(
            request_id=request_id,
            assessment=RiskCategory.confirmed_threat,
            is_simulated=True,
            url_validation=val_details,
            findings=findings,
            threat_intelligence=threat_intel,
            summary=(
                "DEMONSTRATION SIMULATION: Parcel delivery smishing lure detected. Scammers solicit small customs "
                "or redelivery fees to harvest credit card numbers and personal information."
            ),
            recommended_actions=[
                "Do not submit credit card details or billing addresses on unsolicited postal links.",
                "Verify delivery status using your authentic tracking number at usps.com.",
                "Forward the scam SMS to 7726 (SPAM) to alert mobile carriers.",
            ],
        )

    # 3. OTP THEFT / ACCOUNT TAKEOVER
    elif scenario == DemoScenario.otp_theft:
        target_url = custom_url or "https://bank-fraud-department.example.test/cancel"
        try:
            val_details = validate_and_normalize_url(target_url)
        except Exception:
            val_details = UrlValidationDetails(
                valid=True,
                scheme="https",
                hostname="bank-fraud-department.example.test",
                normalized_url=target_url,
                path="/cancel",
            )

        findings = [
            Finding(
                rule_id="RULE_CREDENTIAL_LURE_PATH",
                title="Fake Fraud Cancellation / Security Lure",
                severity=SeverityLevel.high,
                evidence="URL path contains fraud cancellation lure: 'cancel'.",
                explanation=(
                    "Attackers manufacture an alarming fake transfer (e.g. '$850 pending') and instruct "
                    "the victim to 'cancel' it by entering their login credentials and one-time passcode (OTP)."
                ),
                recommendation="Never enter OTP codes to 'cancel' transactions. Banks never cancel transfers via external web forms.",
            ),
            Finding(
                rule_id="RULE_UNVERIFIED_FINANCIAL_PORTAL",
                title="Unverified Banking Portal Structure",
                severity=SeverityLevel.high,
                evidence="Domain 'bank-fraud-department.example.test' is not an authorized banking domain.",
                explanation="Financial fraud defense pages are only hosted on authentic verified corporate banking domains.",
                recommendation="Contact your bank using the phone number printed on the back of your debit/credit card.",
            ),
        ]

        threat_intel = ThreatIntelligenceResult(
            status=ThreatIntelStatus.confirmed_threat,
            source="TRUSTLENS Educational Demonstration Provider",
            threat_types=["SOCIAL_ENGINEERING"],
            checked_at=now_utc,
            details="DEMONSTRATION SIMULATION: Known bank takeover & OTP harvesting portal.",
        )

        return AnalyzeResponse(
            request_id=request_id,
            assessment=RiskCategory.confirmed_threat,
            is_simulated=True,
            url_validation=val_details,
            findings=findings,
            threat_intelligence=threat_intel,
            summary=(
                "DEMONSTRATION SIMULATION: OTP and banking credential theft attack. Scammers manufacture a fake wire "
                "alert to manipulate victims into surrendering 2-factor authentication codes."
            ),
            recommended_actions=[
                "NEVER share or enter an OTP code on an unfamiliar link.",
                "Banks will NEVER call, text, or email you asking for a one-time passcode.",
                "Log in to your banking app directly to check your real account balance.",
            ],
        )

    # 4. FAKE PAYMENT REQUEST (Invoice / BEC Redirection)
    elif scenario == DemoScenario.fake_payment:
        target_url = custom_url or "https://supplier-billing-escalation.example.test/invoice"
        try:
            val_details = validate_and_normalize_url(target_url)
        except Exception:
            val_details = UrlValidationDetails(
                valid=True,
                scheme="https",
                hostname="supplier-billing-escalation.example.test",
                normalized_url=target_url,
                path="/invoice",
            )

        findings = [
            Finding(
                rule_id="RULE_CREDENTIAL_LURE_PATH",
                title="Urgent Invoice Payment Lure",
                severity=SeverityLevel.medium,
                evidence="URL path contains billing action keywords: 'invoice'.",
                explanation=(
                    "Business email compromise and fake invoice scams create false urgency by threatening "
                    "account escalation or legal action unless immediate payment is submitted."
                ),
                recommendation="Verify invoice authenticity through a verified phone number of the vendor.",
            ),
            Finding(
                rule_id="RULE_UNFAMILIAR_PAYMENT_HOST",
                title="Unregistered Payment Gateway Domain",
                severity=SeverityLevel.medium,
                evidence="Base domain 'example.test' has no known institutional merchant accreditation.",
                explanation="Scammers register disposable domains with keywords like 'billing', 'escalation', or 'accounting'.",
                recommendation="Check the original contract or invoice document before processing payments.",
            ),
        ]

        threat_intel = ThreatIntelligenceResult(
            status=ThreatIntelStatus.no_threat_found,
            source="TRUSTLENS Educational Demonstration Provider",
            threat_types=[],
            checked_at=now_utc,
            details="DEMONSTRATION SIMULATION: Newly registered domain not yet indexed in threat blocklists.",
        )

        return AnalyzeResponse(
            request_id=request_id,
            assessment=RiskCategory.suspicious_indicators,
            is_simulated=True,
            url_validation=val_details,
            findings=findings,
            threat_intelligence=threat_intel,
            summary=(
                "DEMONSTRATION SIMULATION: Fake payment & invoice escalation lure. Threat actors deploy "
                "unregistered billing portals to intercept corporate and individual payments."
            ),
            recommended_actions=[
                "Perform out-of-band verification via voice call with known vendor contacts.",
                "Do not process payment requests originating from unexpected sender addresses.",
            ],
        )

    # 5. QR-CODE SCAM (Quishing / Payment Overlay Sticker)
    elif scenario == DemoScenario.qr_scam:
        target_url = custom_url or "https://quick-parking-meter.example.test/pay"
        try:
            val_details = validate_and_normalize_url(target_url)
        except Exception:
            val_details = UrlValidationDetails(
                valid=True,
                scheme="https",
                hostname="quick-parking-meter.example.test",
                normalized_url=target_url,
                path="/pay",
            )

        findings = [
            Finding(
                rule_id="QR_RULE_QUISHING_RISK",
                title="Quishing (QR Phishing) Kiosk / Meter Lure",
                severity=SeverityLevel.high,
                evidence="Decoded QR payload targets payment collection domain 'quick-parking-meter.example.test'.",
                explanation=(
                    "Physical quishing involves placing fraudulent QR code stickers over authentic parking meters, "
                    "restaurant tables, or public kiosks to siphon mobile payment transactions."
                ),
                recommendation="Inspect physical QR stickers to verify they have not been placed over authentic hardware.",
            ),
            Finding(
                rule_id="RULE_CREDENTIAL_LURE_PATH",
                title="Direct Checkout / Payment Path",
                severity=SeverityLevel.medium,
                evidence="URL path contains payment lure: 'pay'.",
                explanation="Malicious QR codes take users directly to credential or payment collection screens.",
                recommendation="Use official city parking apps or pay directly at physical terminal card slots.",
            ),
        ]

        threat_intel = ThreatIntelligenceResult(
            status=ThreatIntelStatus.confirmed_threat,
            source="TRUSTLENS Educational Demonstration Provider",
            threat_types=["SOCIAL_ENGINEERING"],
            checked_at=now_utc,
            details="DEMONSTRATION SIMULATION: Known quishing sticker distribution campaign.",
        )

        return AnalyzeResponse(
            request_id=request_id,
            assessment=RiskCategory.confirmed_threat,
            is_simulated=True,
            url_validation=val_details,
            findings=findings,
            threat_intelligence=threat_intel,
            summary=(
                "DEMONSTRATION SIMULATION: Quishing (QR code scam) detected. Attackers deploy deceptive QR codes "
                "that mimic municipal utilities or merchants to steal credit card payments."
            ),
            recommended_actions=[
                "Do not scan QR codes stuck onto parking meters or shared devices if they look like stickers.",
                "Never enter credit card details on websites reached via public QR code stickers.",
                "Download the official municipal or merchant app from the Google Play Store directly.",
            ],
        )

    # 6. FRAUDULENT CALL / VISHING FOLLOW-UP
    elif scenario == DemoScenario.fraud_call:
        target_url = custom_url or "https://fraud-support-call.example.test/verify"
        try:
            val_details = validate_and_normalize_url(target_url)
        except Exception:
            val_details = UrlValidationDetails(
                valid=True,
                scheme="https",
                hostname="fraud-support-call.example.test",
                normalized_url=target_url,
                path="/verify",
            )

        findings = [
            Finding(
                rule_id="RULE_BRAND_IMPERSONATION",
                title="Vishing (Voice Phishing) Multi-Channel Lure",
                severity=SeverityLevel.high,
                evidence="Domain 'fraud-support-call.example.test' mimics an official fraud response center.",
                explanation=(
                    "Vishing scammers initiate a spoofed phone call and send a coordinated SMS with a link "
                    "claiming to 'verify identity' or 'cancel an unauthorized charge' while keeping the victim on the line."
                ),
                recommendation="Hang up immediately. Scammers use live phone pressure to prevent independent verification.",
            ),
            Finding(
                rule_id="RULE_CREDENTIAL_LURE_PATH",
                title="Immediate Identity Verification Lure",
                severity=SeverityLevel.medium,
                evidence="URL path contains verification lure: 'verify'.",
                explanation="Victims are pressured to click while the scammer guides them through entering security details.",
                recommendation="Never click links while on the phone with an unsolicited caller.",
            ),
        ]

        threat_intel = ThreatIntelligenceResult(
            status=ThreatIntelStatus.confirmed_threat,
            source="TRUSTLENS Educational Demonstration Provider",
            threat_types=["SOCIAL_ENGINEERING"],
            checked_at=now_utc,
            details="DEMONSTRATION SIMULATION: Vishing call center verification infrastructure.",
        )

        return AnalyzeResponse(
            request_id=request_id,
            assessment=RiskCategory.confirmed_threat,
            is_simulated=True,
            url_validation=val_details,
            findings=findings,
            threat_intelligence=threat_intel,
            summary=(
                "DEMONSTRATION SIMULATION: Multi-channel vishing (voice phishing) attack. Scammers pair an automated "
                "or live phone call with an urgent verification link to compromise accounts."
            ),
            recommended_actions=[
                "Hang up immediately. Legitimate fraud departments never pressure you to open links during a call.",
                "Call your bank or institution using the official number from your card or bill.",
                "Never share 2-factor codes or screen sharing permissions with incoming callers.",
            ],
        )

    # 7. NO KNOWN THREAT (Authentic Domain Simulation)
    elif scenario == DemoScenario.no_known_threat:
        target_url = custom_url or "https://legitimate-sample-service.example.org/about"
        try:
            val_details = validate_and_normalize_url(target_url)
        except Exception:
            val_details = UrlValidationDetails(
                valid=True,
                scheme="https",
                hostname="legitimate-sample-service.example.org",
                normalized_url=target_url,
                path="/about",
            )

        threat_intel = ThreatIntelligenceResult(
            status=ThreatIntelStatus.no_threat_found,
            source="TRUSTLENS Educational Demonstration Provider",
            threat_types=[],
            checked_at=now_utc,
            details="DEMONSTRATION SIMULATION: No active matches in threat intelligence feed.",
        )

        return AnalyzeResponse(
            request_id=request_id,
            assessment=RiskCategory.no_known_threat,
            is_simulated=True,
            url_validation=val_details,
            findings=[],
            threat_intelligence=threat_intel,
            summary=(
                "DEMONSTRATION SIMULATION: Neither external threat intelligence nor local heuristics detected known malicious indicators. "
                "Standard cybersecurity vigilance is still recommended."
            ),
            recommended_actions=[
                "Verify the connection is secure (HTTPS) before entering confidential information.",
                "Confirm that the domain corresponds with the organization you intend to interact with.",
            ],
        )

    # 8. INCONCLUSIVE (Threat Intel Unreachable Simulation)
    elif scenario == DemoScenario.inconclusive:
        target_url = custom_url or "https://unverified-community-showcase.example.org/"
        try:
            val_details = validate_and_normalize_url(target_url)
        except Exception:
            val_details = UrlValidationDetails(
                valid=True,
                scheme="https",
                hostname="unverified-community-showcase.example.org",
                normalized_url=target_url,
                path="/",
            )

        threat_intel = ThreatIntelligenceResult(
            status=ThreatIntelStatus.service_unavailable,
            source="TRUSTLENS Educational Demonstration Provider",
            threat_types=[],
            checked_at=now_utc,
            details="DEMONSTRATION SIMULATION: Threat intelligence lookup simulated upstream network unavailability.",
        )

        return AnalyzeResponse(
            request_id=request_id,
            assessment=RiskCategory.inconclusive,
            is_simulated=True,
            url_validation=val_details,
            findings=[],
            threat_intelligence=threat_intel,
            summary=(
                "DEMONSTRATION SIMULATION: Local structural heuristics detected no high-risk indicators, but external threat intelligence "
                "could not be consulted. A definitive safe verdict cannot be confirmed."
            ),
            recommended_actions=[
                "Exercise heightened caution before opening links from unknown senders.",
                "Never share passwords, OTP codes, or banking information on unfamiliar portals.",
                "Verify the domain independently using an established search engine.",
            ],
        )

    raise ValueError(f"Unsupported demo scenario: {scenario}")


def generate_comparison_demo() -> ComparisonDemoResponse:
    """
    Generates an educational side-by-side comparison between:
    1. A simulated deceptive phishing link (brand masquerading, credential lure, deep subdomains).
    2. A harmless reserved test URL (RFC 2606 / RFC 6761 example.com).
    """
    # Deceptive threat simulation
    deceptive_url = "https://paypal.com.verify-account.security-alert.xyz/signin"
    val_deceptive = validate_and_normalize_url(deceptive_url)
    findings_deceptive = [
        Finding(
            rule_id="RULE_BRAND_IMPERSONATION",
            title="Deceptive Brand Impersonation Indicator",
            severity=SeverityLevel.high,
            evidence="Brand 'paypal' detected in subdomain prefix, while registered apex domain is 'security-alert.xyz'.",
            explanation="Scammers prefix trusted brand names to mislead mobile users whose browser address bar cuts off long URLs.",
            recommendation="Do not enter passwords or card details. Navigate directly to paypal.com.",
        ),
        Finding(
            rule_id="RULE_EXCESSIVE_SUBDOMAINS",
            title="Excessive Subdomain Nesting",
            severity=SeverityLevel.medium,
            evidence="Domain contains 3 subdomain levels preceding 'security-alert.xyz'.",
            explanation="Deep subdomains are used to obscure the true domain on smaller mobile screens.",
            recommendation="Inspect the rightmost labels of the domain to identify the actual operator.",
        ),
        Finding(
            rule_id="RULE_CREDENTIAL_LURE_PATH",
            title="Credential / Account Verification Lure",
            severity=SeverityLevel.medium,
            evidence="URL path contains sensitive account action terms: 'signin'.",
            explanation="Phishing links routinely use urgency and login prompts to harvest confidential passwords.",
            recommendation="Never use unsolicited links to resolve login alerts.",
        ),
    ]
    threat_intel_deceptive = ThreatIntelligenceResult(
        status=ThreatIntelStatus.confirmed_threat,
        source="TRUSTLENS Educational Demonstration Provider",
        threat_types=["SOCIAL_ENGINEERING"],
        checked_at=datetime.now(timezone.utc),
        details="DEMONSTRATION SIMULATION: Known phishing simulation feed matched target URL.",
    )
    simulated_threat = AnalyzeResponse(
        request_id=f"demo-threat-{uuid.uuid4().hex[:8]}",
        assessment=RiskCategory.confirmed_threat,
        is_simulated=True,
        url_validation=val_deceptive,
        findings=findings_deceptive,
        threat_intelligence=threat_intel_deceptive,
        summary="DEMONSTRATION SIMULATION: External threat intelligence verified this target as a confirmed threat (SOCIAL_ENGINEERING).",
        recommended_actions=[
            "Do not click this link or submit any information.",
            "Report message as phishing to your carrier.",
        ],
    )

    # Harmless test domain (RFC 2606 / IANA)
    harmless_url_str = "https://example.com/about"
    val_harmless = validate_and_normalize_url(harmless_url_str)
    threat_intel_harmless = ThreatIntelligenceResult(
        status=ThreatIntelStatus.no_threat_found,
        source="TRUSTLENS Educational Demonstration Provider",
        threat_types=[],
        checked_at=datetime.now(timezone.utc),
        details="DEMONSTRATION SIMULATION: No active threat records for reserved documentation domain.",
    )
    harmless_url = AnalyzeResponse(
        request_id=f"demo-harmless-{uuid.uuid4().hex[:8]}",
        assessment=RiskCategory.no_known_threat,
        is_simulated=True,
        url_validation=val_harmless,
        findings=[],
        threat_intelligence=threat_intel_harmless,
        summary="DEMONSTRATION SIMULATION: Neither external threat intelligence nor local heuristics detected known malicious indicators.",
        recommended_actions=[
            "Verify standard HTTPS connection.",
            "Confirm domain matches expected service.",
        ],
    )

    comparison_summary = (
        "EDUCATIONAL COMPARISON: This side-by-side contrast illustrates why structural analysis flags deceptive URLs "
        "even when the user cannot see the full domain on a mobile device. The deceptive URL abuses brand prepending, "
        "excessive subdomains, and credential lure paths on an unrelated '.xyz' domain. In contrast, the harmless URL "
        "uses standard RFC-reserved domain structure without misleading prefix masquerading or urgency triggers."
    )

    key_differences = [
        "Brand Masquerading: Threat URL injects 'paypal.com' into a subdomain of 'security-alert.xyz', whereas harmless URL uses standard apex hierarchy.",
        "Mobile Screen Obfuscation: Threat URL uses 3 subdomain levels to push the actual domain out of view on smartphones.",
        "Path Intent: Threat URL specifies '/signin' to steal credentials, whereas harmless URL uses neutral path '/about'.",
        "Threat Intelligence: Threat URL matches indexed social engineering signatures; harmless URL has clean reputation status.",
    ]

    return ComparisonDemoResponse(
        is_simulated=True,
        simulated_threat=simulated_threat,
        harmless_url=harmless_url,
        comparison_summary=comparison_summary,
        key_differences=key_differences,
    )
