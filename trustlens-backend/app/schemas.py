from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Any, List, Optional
from pydantic import BaseModel, Field


class RiskCategory(str, Enum):
    """Categorized assessment outcome combining local heuristics and threat intelligence."""
    confirmed_threat = "confirmed_threat"
    suspicious_indicators = "suspicious_indicators"
    no_known_threat = "no_known_threat"
    inconclusive = "inconclusive"


class SeverityLevel(str, Enum):
    """Severity classification for specific heuristic findings."""
    info = "info"
    low = "low"
    medium = "medium"
    high = "high"
    critical = "critical"


class ThreatIntelStatus(str, Enum):
    """Granular operational status of external threat intelligence lookups."""
    confirmed_threat = "confirmed_threat"
    no_threat_found = "no_threat_found"
    service_unavailable = "service_unavailable"
    not_configured = "not_configured"
    timeout = "timeout"
    failed = "failed"


class DemoScenario(str, Enum):
    """
    Supported controlled demonstration scenarios for educational evaluation.

    Includes classic scenarios:
    - threat_detected
    - no_known_threat
    - inconclusive

    And dedicated educational hackathon scenarios:
    - scam_link: Deceptive lookalike brand URL
    - parcel_sms: Impersonated postal customs fee smishing
    - otp_theft: Bank wire transfer OTP solicitation
    - fake_payment: Urgent invoice / payment redirection
    - qr_scam: Direct payment transfer / quishing QR sticker
    - fraud_call: Vishing caller follow-up alert
    - comparison: Side-by-side deceptive vs harmless URL
    """
    # Original core scenarios (backward compatibility)
    threat_detected = "threat_detected"
    no_known_threat = "no_known_threat"
    inconclusive = "inconclusive"

    # Six educational scam scenarios
    scam_link = "scam_link"
    parcel_sms = "parcel_sms"
    otp_theft = "otp_theft"
    fake_payment = "fake_payment"
    qr_scam = "qr_scam"
    fraud_call = "fraud_call"

    # Comparison scenario
    comparison = "comparison"


class UrlValidationDetails(BaseModel):
    """Detailed structural attributes extracted during URL validation."""
    valid: bool = Field(..., description="Whether the URL passed structural syntactic validation")
    scheme: str = Field(..., description="URL protocol scheme (http or https)")
    hostname: str = Field(..., description="Parsed and normalized domain name or IP address")
    normalized_url: str = Field(..., description="Consistently normalized URL representation")
    port: Optional[int] = Field(None, description="Explicit port number if present")
    path: str = Field("/", description="Normalized URL path")
    is_ip_address: bool = Field(False, description="Indicates if the host is a raw IP address")
    is_private_ip: bool = Field(False, description="Indicates if the IP is within RFC 1918 or private range")
    is_loopback: bool = Field(False, description="Indicates if the host targets loopback (e.g. 127.0.0.1 or localhost)")
    is_punycode: bool = Field(False, description="Indicates if hostname contains internationalized punycode labels")
    unicode_hostname: Optional[str] = Field(None, description="Decoded Unicode representation of internationalized host")


class Finding(BaseModel):
    """An explainable indicator detected by local heuristic analysis."""
    rule_id: str = Field(..., description="Stable, unique machine-readable rule identifier")
    title: str = Field(..., description="Concise, human-readable indicator title")
    severity: SeverityLevel = Field(..., description="Assigned severity level")
    evidence: str = Field(..., description="Concrete observed string or structural token that triggered the rule")
    explanation: str = Field(..., description="Clear explanation of the scam risk associated with this characteristic")
    recommendation: str = Field(..., description="Recommended defensive action for the user")


class ThreatIntelligenceResult(BaseModel):
    """Result of querying the configured threat intelligence provider."""
    status: ThreatIntelStatus = Field(..., description="Execution status of the threat intelligence check")
    source: Optional[str] = Field(None, description="Source provider name (e.g. 'Google Safe Browsing v4')")
    threat_types: List[str] = Field(default_factory=list, description="Categorized threat types if confirmed (e.g. SOCIAL_ENGINEERING)")
    checked_at: Optional[datetime] = Field(None, description="UTC timestamp when the query was executed")
    details: Optional[str] = Field(None, description="Contextual message or provider disclosure")


# URL Analysis Schemas
class UrlAnalyzeRequest(BaseModel):
    """Payload for submitting a URL for security analysis."""
    url: str = Field(
        ...,
        min_length=1,
        max_length=2048,
        description="The full URL to analyze (must begin with http:// or https://)",
        examples=["https://example.com"]
    )


class AnalyzeResponse(BaseModel):
    """Comprehensive analysis report returned to the Android client."""
    request_id: str = Field(..., description="Unique UUID tracking this analysis operation")
    assessment: RiskCategory = Field(..., description="Overall risk category synthesis")
    is_simulated: bool = Field(False, description="Flag indicating whether this result is simulated/demonstration")
    url_validation: UrlValidationDetails = Field(..., description="Syntactic URL breakdown and properties")
    findings: List[Finding] = Field(default_factory=list, description="List of explainable heuristic findings")
    threat_intelligence: ThreatIntelligenceResult = Field(..., description="External threat intelligence findings")
    summary: str = Field(..., description="Plain-language executive summary of the safety evaluation")
    recommended_actions: List[str] = Field(default_factory=list, description="Specific, prioritized user recommendations")


# Demo Schemas
class DemoAnalyzeRequest(BaseModel):
    """Payload for requesting a controlled demonstration scenario."""
    scenario: DemoScenario = Field(
        ...,
        description="The demonstration scenario to simulate",
        examples=["threat_detected", "scam_link", "parcel_sms", "otp_theft", "fake_payment", "qr_scam", "fraud_call", "comparison"]
    )
    url: Optional[str] = Field(
        None,
        max_length=2048,
        description="Optional custom URL for scenario context; if omitted, scenario default is used"
    )


class ComparisonDemoResponse(BaseModel):
    """Side-by-side contrast of a simulated deceptive threat URL vs a harmless reserved test URL."""
    is_simulated: bool = Field(True, description="Always true for educational demonstration")
    simulated_threat: AnalyzeResponse = Field(..., description="Analysis breakdown of the simulated deceptive link")
    harmless_url: AnalyzeResponse = Field(..., description="Analysis breakdown of the harmless RFC reserved domain")
    comparison_summary: str = Field(..., description="Educational summary of why the threat triggered alerts while the benign domain passed")
    key_differences: List[str] = Field(default_factory=list, description="Core technical differentiators between the two URLs")


# SMS / Suspicious Message Analysis Schemas
class MessageAnalyzeRequest(BaseModel):
    """Payload for submitting an SMS or suspicious message text for security analysis."""
    message: str = Field(
        ...,
        min_length=1,
        max_length=5000,
        description="The full body text of the SMS or message to inspect",
        examples=["USPS: Your parcel is on hold due to an unpaid customs fee of $2.15. Confirm at https://usps-redelivery.xyz/pay"]
    )
    sender: Optional[str] = Field(
        None,
        max_length=100,
        description="Optional sender phone number, shortcode, or display name",
        examples=["+18005550199"]
    )


class MessageAnalyzeResponse(BaseModel):
    """Comprehensive analysis report for an SMS or text message."""
    request_id: str = Field(..., description="Unique UUID tracking this analysis operation")
    assessment: RiskCategory = Field(..., description="Overall risk category synthesis for the message")
    is_simulated: bool = Field(False, description="Flag indicating whether this result is simulated")
    message_length: int = Field(..., description="Length of inspected message in characters")
    detected_urls: List[str] = Field(default_factory=list, description="List of URLs safely extracted from message")
    findings: List[Finding] = Field(default_factory=list, description="List of explainable social engineering and technical findings")
    embedded_url_analyses: List[dict] = Field(default_factory=list, description="Security analysis breakdowns for each extracted link")
    summary: str = Field(..., description="Plain-language executive summary of the safety evaluation")
    recommended_actions: List[str] = Field(default_factory=list, description="Specific defensive steps for the user")


# QR Code Analysis Schemas
class QrAnalyzeRequest(BaseModel):
    """Payload for submitting a decoded QR code payload string."""
    payload: str = Field(
        ...,
        min_length=1,
        max_length=4096,
        description="Raw decoded text or URI scheme from a scanned QR code",
        examples=["https://restaurant-menu.xyz/pay", "upi://pay?pa=merchant@bank&am=25.00"]
    )


class QrAnalyzeResponse(BaseModel):
    """Comprehensive security analysis of a decoded QR code payload."""
    request_id: str = Field(..., description="Unique UUID tracking this analysis operation")
    payload_type: str = Field(..., description="Classification of the QR payload ('url', 'payment', 'wifi', 'telephony', 'intent', 'plain_text')")
    assessment: RiskCategory = Field(..., description="Overall risk category synthesis")
    is_simulated: bool = Field(False, description="Flag indicating whether this result is simulated")
    raw_payload: str = Field(..., description="The raw decoded QR string inspected")
    findings: List[Finding] = Field(default_factory=list, description="List of explainable security findings")
    url_analysis: Optional[dict] = Field(None, description="Full URL security analysis if payload contained a web link")
    summary: str = Field(..., description="Plain-language executive summary of the evaluation")
    recommended_actions: List[str] = Field(default_factory=list, description="Recommended user actions before interacting with this QR payload")


# Health and Root Metadata Schemas
class HealthResponse(BaseModel):
    """Operational health status response."""
    status: str = Field("healthy", description="Operational status of backend service")
    app: str = Field(..., description="Application name")
    version: str = Field(..., description="Application version")
    threat_intel_configured: bool = Field(..., description="Indicates if external threat intelligence provider is active")
    capabilities: List[str] = Field(
        default_factory=lambda: [
            "url_analysis",
            "message_sms_analysis",
            "qr_payload_analysis",
            "educational_scam_scenarios",
            "comparison_demo"
        ],
        description="Supported backend analysis capabilities"
    )
    timestamp: datetime = Field(..., description="Current server time (UTC)")


class RootResponse(BaseModel):
    """Root metadata response."""
    app: str = Field(..., description="Application name")
    version: str = Field(..., description="Application semantic version")
    status: str = Field("running", description="Status message")
    description: str = Field(..., description="Application purpose overview")
    capabilities: List[str] = Field(
        default_factory=lambda: [
            "url_analysis",
            "message_sms_analysis",
            "qr_payload_analysis",
            "educational_scam_scenarios",
            "comparison_demo"
        ],
        description="Supported backend analysis capabilities"
    )
    docs_url: str = Field("/docs", description="Interactive OpenAPI documentation endpoint")
