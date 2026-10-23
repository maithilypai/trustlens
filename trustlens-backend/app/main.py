from __future__ import annotations

from contextlib import asynccontextmanager
from datetime import datetime, timezone
import logging
import uuid
from typing import Any, AsyncGenerator

from fastapi import FastAPI, HTTPException, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import Settings, get_settings
from app.demo_service import generate_comparison_demo, generate_demo_analysis
from app.message_analyzer import analyze_suspicious_message
from app.qr_analyzer import analyze_qr_code_payload
from app.risk_assessment import evaluate_risk
from app.schemas import (
    AnalyzeResponse,
    ComparisonDemoResponse,
    DemoAnalyzeRequest,
    DemoScenario,
    HealthResponse,
    MessageAnalyzeRequest,
    MessageAnalyzeResponse,
    QrAnalyzeRequest,
    QrAnalyzeResponse,
    RootResponse,
    UrlAnalyzeRequest,
)
from app.threat_intelligence import (
    GoogleSafeBrowsingProvider,
    ThreatIntelligenceProvider,
)
from app.url_analyzer import analyze_url_structure
from app.url_validator import UrlValidationError, validate_and_normalize_url

# Configure structured logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("trustlens")


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """Lifespan context manager for application startup and shutdown events."""
    settings = get_settings()
    logger.info("Initializing %s v%s in %s mode", settings.APP_NAME, settings.APP_VERSION, settings.ENVIRONMENT)
    # Initialize the threat intelligence provider if not already pre-configured (e.g. in tests)
    if not hasattr(app.state, "threat_provider") or app.state.threat_provider is None:
        app.state.threat_provider = GoogleSafeBrowsingProvider(settings)
    yield
    logger.info("Shutting down %s", settings.APP_NAME)


def create_app(settings: Settings | None = None) -> FastAPI:
    """FastAPI application factory."""
    cfg = settings or get_settings()

    app = FastAPI(
        title="TRUSTLENS Backend API",
        description=(
            "AI-assisted scam link analysis, SMS/message inspection, QR quishing detection, "
            "and threat intelligence protection engine designed for mobile and web integration."
        ),
        version=cfg.APP_VERSION,
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
        lifespan=lifespan,
    )

    # Configure deliberate CORS permissions
    # In production, wildcard origins are avoided to protect mobile-backend communication integrity
    app.add_middleware(
        CORSMiddleware,
        allow_origins=cfg.CORS_ORIGINS,
        allow_credentials=True,
        allow_methods=["GET", "POST", "OPTIONS"],
        allow_headers=["*"],
    )

    # Global Exception Handlers for Security
    @app.exception_handler(UrlValidationError)
    async def url_validation_exception_handler(request: Request, exc: UrlValidationError) -> JSONResponse:
        """Handle domain and URL validation errors safely without internal leakages."""
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={
                "error": "URL_VALIDATION_ERROR",
                "code": exc.code,
                "message": exc.message,
            },
        )

    @app.exception_handler(RequestValidationError)
    async def pydantic_validation_exception_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
        """Return clean validation errors for malformed JSON or invalid schema types."""
        errors = []
        for err in exc.errors():
            loc = " -> ".join(str(item) for item in err.get("loc", []))
            errors.append({"field": loc, "message": err.get("msg", "Invalid value")})
        return JSONResponse(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            content={
                "error": "REQUEST_VALIDATION_ERROR",
                "details": errors,
            },
        )

    @app.exception_handler(Exception)
    async def generic_unhandled_exception_handler(request: Request, exc: Exception) -> JSONResponse:
        """
        Safety Exception Handler:
        Logs the failure internally while preventing stack traces or sensitive environment
        details from leaking to API clients.
        """
        logger.error("Unhandled server exception: %s", type(exc).__name__, exc_info=cfg.DEBUG)
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "error": "INTERNAL_SERVER_ERROR",
                "message": "An internal error occurred while processing the security request. Please try again later.",
            },
        )

    # 1. Root endpoint
    @app.get(
        "/",
        response_model=RootResponse,
        summary="Service Metadata",
        tags=["System"],
    )
    async def root() -> RootResponse:
        return RootResponse(
            app=cfg.APP_NAME,
            version=cfg.APP_VERSION,
            status="running",
            description="TRUSTLENS AI-assisted scam link, SMS, and QR protection backend.",
            capabilities=[
                "url_analysis",
                "message_sms_analysis",
                "qr_payload_analysis",
                "educational_scam_scenarios",
                "comparison_demo",
            ],
            docs_url="/docs",
        )

    # 2. Health check endpoint
    @app.get(
        "/health",
        response_model=HealthResponse,
        summary="Service Health Check",
        tags=["System"],
    )
    async def health() -> HealthResponse:
        has_key = bool(cfg.SAFE_BROWSING_API_KEY and cfg.SAFE_BROWSING_API_KEY.strip())
        return HealthResponse(
            status="healthy",
            app=cfg.APP_NAME,
            version=cfg.APP_VERSION,
            threat_intel_configured=has_key,
            capabilities=[
                "url_analysis",
                "message_sms_analysis",
                "qr_payload_analysis",
                "educational_scam_scenarios",
                "comparison_demo",
            ],
            timestamp=datetime.now(timezone.utc),
        )

    # 3. Live URL analysis endpoint
    @app.post(
        f"{cfg.API_PREFIX}/analyze",
        response_model=AnalyzeResponse,
        summary="Analyze URL Security",
        tags=["Analysis"],
    )
    async def analyze_url(payload: UrlAnalyzeRequest, request: Request) -> AnalyzeResponse:
        """
        Analyze a submitted URL using local explainable heuristics and threat intelligence.

        Security Safeguards:
        - Never opens, fetches, navigates to, or downloads the submitted URL.
        - Rejects embedded credentials and non-HTTP(S) schemes.
        - Protects against SSRF vulnerabilities.
        """
        # URL Validation & Normalization
        val_details = validate_and_normalize_url(payload.url, max_length=cfg.MAX_URL_LENGTH)

        # Local Heuristic Analysis
        findings = analyze_url_structure(val_details)

        # Threat Intelligence Lookup
        provider: ThreatIntelligenceProvider = getattr(
            request.app.state,
            "threat_provider",
            GoogleSafeBrowsingProvider(cfg),
        )
        threat_result = await provider.check_url(val_details.normalized_url)

        # Risk Assessment Synthesis
        assessment, summary, recommendations = evaluate_risk(findings, threat_result)

        request_id = str(uuid.uuid4())

        return AnalyzeResponse(
            request_id=request_id,
            assessment=assessment,
            is_simulated=False,
            url_validation=val_details,
            findings=findings,
            threat_intelligence=threat_result,
            summary=summary,
            recommended_actions=recommendations,
        )

    # 4. SMS and Suspicious Message Analysis endpoint
    @app.post(
        f"{cfg.API_PREFIX}/analyze/message",
        response_model=MessageAnalyzeResponse,
        summary="Analyze SMS / Suspicious Message",
        tags=["Analysis"],
    )
    async def analyze_message_endpoint(payload: MessageAnalyzeRequest, request: Request) -> MessageAnalyzeResponse:
        """
        Analyze an incoming SMS or suspicious message text for social engineering lures
        (OTP theft, delivery fees, urgency pressure, authority impersonation) and inspect embedded links.

        Security Safeguards:
        - Submitted messages are analyzed in-memory and NEVER stored or logged.
        - Never collects or stores real credentials, OTPs, or passwords.
        """
        provider: ThreatIntelligenceProvider = getattr(
            request.app.state,
            "threat_provider",
            GoogleSafeBrowsingProvider(cfg),
        )

        assessment, findings, embedded_url_analyses, summary, recommendations = await analyze_suspicious_message(
            text=payload.message,
            sender=payload.sender,
            threat_provider=provider,
        )

        request_id = str(uuid.uuid4())

        return MessageAnalyzeResponse(
            request_id=request_id,
            assessment=assessment,
            is_simulated=False,
            message_length=len(payload.message),
            detected_urls=[u.get("url", "") for u in embedded_url_analyses if "url" in u],
            findings=findings,
            embedded_url_analyses=embedded_url_analyses,
            summary=summary,
            recommended_actions=recommendations,
        )

    # 5. QR Code Payload Analysis endpoint
    @app.post(
        f"{cfg.API_PREFIX}/analyze/qr",
        response_model=QrAnalyzeResponse,
        summary="Analyze Decoded QR Code Payload",
        tags=["Analysis"],
    )
    async def analyze_qr_endpoint(payload: QrAnalyzeRequest, request: Request) -> QrAnalyzeResponse:
        """
        Analyze a decoded QR code payload string for quishing (QR phishing), direct payment transfers,
        rogue Wi-Fi provisioning, or telephony actions.

        Security Safeguards:
        - Never initiates payments or communicates with arbitrary target URIs.
        - Memory-only inspection without persisting payloads.
        """
        provider: ThreatIntelligenceProvider = getattr(
            request.app.state,
            "threat_provider",
            GoogleSafeBrowsingProvider(cfg),
        )

        payload_type, assessment, findings, url_analysis, summary, recommendations = await analyze_qr_code_payload(
            payload=payload.payload,
            threat_provider=provider,
        )

        request_id = str(uuid.uuid4())

        return QrAnalyzeResponse(
            request_id=request_id,
            payload_type=payload_type,
            assessment=assessment,
            is_simulated=False,
            raw_payload=payload.payload,
            findings=findings,
            url_analysis=url_analysis,
            summary=summary,
            recommended_actions=recommendations,
        )

    # 6. Controlled demonstration endpoint (supports all 6 educational scenarios + legacy)
    @app.post(
        f"{cfg.API_PREFIX}/demo",
        response_model=AnalyzeResponse,
        summary="Demonstration Scenario",
        tags=["Demonstration"],
    )
    async def demo_scenario(payload: DemoAnalyzeRequest) -> AnalyzeResponse:
        """
        Produce a controlled simulation scenario for educational evaluation.

        Supported Scenarios:
        - threat_detected / scam_link: Deceptive lookalike brand phishing
        - parcel_sms: Postal customs fee smishing
        - otp_theft: Bank wire transfer OTP solicitation
        - fake_payment: Urgent supplier invoice redirection
        - qr_scam: Quishing / payment QR sticker overlay
        - fraud_call: Vishing caller follow-up alert
        - no_known_threat: Clean domain reputation check
        - inconclusive: Threat intelligence service unreachable

        Guarantees:
        - Response explicitly flags `is_simulated: true`.
        - Does not connect to live scam servers or test real URLs.
        """
        if payload.scenario == DemoScenario.comparison:
            # If called via /demo, return the simulated threat portion of the comparison
            comp = generate_comparison_demo()
            return comp.simulated_threat

        return generate_demo_analysis(payload.scenario, custom_url=payload.url)

    # 7. Comparison demonstration endpoint
    @app.post(
        f"{cfg.API_PREFIX}/demo/compare",
        response_model=ComparisonDemoResponse,
        summary="Educational Comparison Demo (POST)",
        tags=["Demonstration"],
    )
    @app.get(
        f"{cfg.API_PREFIX}/demo/compare",
        response_model=ComparisonDemoResponse,
        summary="Educational Comparison Demo (GET)",
        tags=["Demonstration"],
    )
    async def demo_compare() -> ComparisonDemoResponse:
        """
        Produce a side-by-side contrast between a simulated deceptive phishing URL
        and a harmless RFC 2606 reserved test URL (example.com).
        """
        return generate_comparison_demo()

    return app


app = create_app()
