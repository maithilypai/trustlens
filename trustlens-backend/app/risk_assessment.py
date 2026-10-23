from __future__ import annotations

from typing import List, Tuple
from app.schemas import (
    Finding,
    RiskCategory,
    SeverityLevel,
    ThreatIntelStatus,
    ThreatIntelligenceResult,
)


def evaluate_risk(
    findings: List[Finding],
    threat_intel: ThreatIntelligenceResult,
) -> Tuple[RiskCategory, str, List[str]]:
    """
    Synthesizes local heuristic findings and threat intelligence into a final risk assessment.

    Decision Rules:
    1. Confirmed Threat: External threat intelligence match takes absolute precedence.
    2. Suspicious Indicators: Presence of high or medium severity local heuristic indicators.
    3. No Known Threat: Heuristics are clean AND external threat intelligence confirmed no match.
    4. Inconclusive: Heuristics are clean BUT threat intelligence was unavailable, unconfigured, or timed out.
       Never returns a clean verdict without positive corroboration.

    Returns:
        (assessment_category, summary_explanation, recommended_actions)
    """
    # 1. Rule 1: Confirmed threat from external intelligence
    if threat_intel.status == ThreatIntelStatus.confirmed_threat:
        threat_types_str = ", ".join(threat_intel.threat_types) if threat_intel.threat_types else "unspecified threat"
        summary = (
            f"External threat intelligence confirmed this URL as an active malicious threat ({threat_types_str}). "
            "Interaction with this link poses immediate risk of credential theft, scam deception, or malware infection."
        )
        recommended_actions = [
            "Do not click this link or submit any information.",
            "If received via SMS, messaging app, or email, report the message as spam/phishing.",
            "If you previously visited this URL and entered credentials, change your passwords immediately.",
        ]
        return RiskCategory.confirmed_threat, summary, recommended_actions

    # Evaluate local heuristic severity levels
    has_high_or_critical = any(
        f.severity in (SeverityLevel.high, SeverityLevel.critical) for f in findings
    )
    has_medium = any(f.severity == SeverityLevel.medium for f in findings)
    has_low = any(f.severity == SeverityLevel.low for f in findings)

    # 2. Rule 2: High or critical local indicators
    if has_high_or_critical:
        high_rule_titles = [
            f.title for f in findings if f.severity in (SeverityLevel.high, SeverityLevel.critical)
        ]
        summary = (
            f"Local structural analysis detected high-severity suspicious indicators: {', '.join(high_rule_titles)}. "
            "This structural pattern strongly correlates with mobile scam, phishing, or deceptive redirection campaigns."
        )
        recommended_actions = [
            "Do not enter passwords, payment card information, or personal identifiers.",
            "Access the intended service directly by typing the official domain into your browser.",
            "Verify the sender's identity through an independent, trusted communication channel.",
        ]
        return RiskCategory.suspicious_indicators, summary, recommended_actions

    # 3. Rule 3: Medium severity local indicators
    if has_medium:
        medium_rule_titles = [f.title for f in findings if f.severity == SeverityLevel.medium]
        summary = (
            f"Local analysis identified atypical URL characteristics that warrant caution: {', '.join(medium_rule_titles)}. "
            "While not definitively confirmed as malicious, these indicators deviate from standard safe practices."
        )
        recommended_actions = [
            "Inspect the destination domain carefully before proceeding.",
            "Avoid completing urgent financial or security requests from unsolicited links.",
            "Look for official communications within your service's account portal instead of following links.",
        ]
        return RiskCategory.suspicious_indicators, summary, recommended_actions

    # 4. Heuristics are clean (only low/info or no findings)
    # Check threat intelligence corroboration status
    if threat_intel.status == ThreatIntelStatus.no_threat_found:
        low_note = " (Note: plain HTTP was detected without TLS encryption)." if has_low else ""
        summary = (
            f"Neither external threat intelligence nor local structural heuristics identified known malicious indicators{low_note}. "
            "This indicates absence of known threats in consulted databases, but does not guarantee absolute safety."
        )
        recommended_actions = [
            "Verify that your browser displays a secure connection before entering information.",
            "Ensure the domain name corresponds exactly with the intended organization.",
            "Remain cautious if prompted to download files or grant unusual permissions.",
        ]
        return RiskCategory.no_known_threat, summary, recommended_actions

    # 5. Rule 5: Inconclusive (Threat intelligence unavailable / unconfigured / timed out)
    intel_reason = threat_intel.details or threat_intel.status.value
    summary = (
        "Local structural analysis did not identify high-risk indicators, but external threat intelligence "
        f"could not be consulted ({intel_reason}). Because external verification was unavailable, "
        "a definitive safe verdict cannot be confirmed."
    )
    recommended_actions = [
        "Exercise heightened caution before opening this link.",
        "Do not provide sensitive or financial information if this link originated from an unsolicited message.",
        "Verify the domain independently using an established search engine or bookmark.",
    ]
    return RiskCategory.inconclusive, summary, recommended_actions
