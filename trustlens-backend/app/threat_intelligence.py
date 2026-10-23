from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import datetime, timezone
import logging
from typing import List, Optional
import httpx

from app.config import Settings, get_settings
from app.schemas import ThreatIntelStatus, ThreatIntelligenceResult

logger = logging.getLogger("trustlens.threat_intel")


class ThreatIntelligenceProvider(ABC):
    """Abstract base class for modular threat intelligence providers."""

    @abstractmethod
    async def check_url(self, url: str) -> ThreatIntelligenceResult:
        """
        Check whether the target URL is a known malicious threat.

        Must return a structured ThreatIntelligenceResult without throwing unhandled exceptions.
        """
        pass


class GoogleSafeBrowsingProvider(ThreatIntelligenceProvider):
    """
    Threat intelligence provider for Google Safe Browsing Lookup API (v4).

    Security and Privacy Safeguards:
    - Never hardcodes or logs the API key.
    - Never logs complete sensitive URLs.
    - Uses bounded timeouts to prevent hanging client requests.
    - Never interprets an API failure or timeout as a clean/safe verdict.
    - A clean lookup is explicitly documented as 'no threat found', not proof of safety.
    """

    SAFE_BROWSING_ENDPOINT = "https://safebrowsing.googleapis.com/v4/threatMatches:find"

    def __init__(self, settings: Optional[Settings] = None):
        self.settings = settings or get_settings()

    async def check_url(self, url: str) -> ThreatIntelligenceResult:
        api_key = self.settings.SAFE_BROWSING_API_KEY
        if not api_key or not api_key.strip():
            return ThreatIntelligenceResult(
                status=ThreatIntelStatus.not_configured,
                source=None,
                threat_types=[],
                checked_at=None,
                details=(
                    "External threat intelligence is not configured. "
                    "Set SAFE_BROWSING_API_KEY to enable Google Safe Browsing lookup."
                ),
            )

        payload = {
            "client": {
                "clientId": self.settings.SAFE_BROWSING_CLIENT_ID,
                "clientVersion": self.settings.SAFE_BROWSING_CLIENT_VERSION,
            },
            "threatInfo": {
                "threatTypes": [
                    "MALWARE",
                    "SOCIAL_ENGINEERING",
                    "UNWANTED_SOFTWARE",
                    "POTENTIALLY_HARMFUL_APPLICATION",
                ],
                "platformTypes": ["ANY_PLATFORM"],
                "threatEntryTypes": ["URL"],
                "threatEntries": [{"url": url}],
            },
        }

        # Bounded network timeout
        timeout = httpx.Timeout(self.settings.THREAT_INTEL_TIMEOUT_SECONDS)
        now_utc = datetime.now(timezone.utc)

        try:
            async with httpx.AsyncClient(timeout=timeout) as client:
                response = await client.post(
                    self.SAFE_BROWSING_ENDPOINT,
                    params={"key": api_key},
                    json=payload,
                )

                if response.status_code == 200:
                    data = response.json()
                    matches = data.get("matches", [])
                    if matches:
                        threat_types = list({m.get("threatType", "UNKNOWN") for m in matches})
                        return ThreatIntelligenceResult(
                            status=ThreatIntelStatus.confirmed_threat,
                            source="Google Safe Browsing v4",
                            threat_types=threat_types,
                            checked_at=now_utc,
                            details=(
                                f"Confirmed match found in threat intelligence database. "
                                f"Identified threat categories: {', '.join(threat_types)}."
                            ),
                        )
                    else:
                        return ThreatIntelligenceResult(
                            status=ThreatIntelStatus.no_threat_found,
                            source="Google Safe Browsing v4",
                            threat_types=[],
                            checked_at=now_utc,
                            details=(
                                "No matches found in Google Safe Browsing database. "
                                "This indicates absence of known indexed threats, not proof of absolute safety."
                            ),
                        )

                elif response.status_code >= 500:
                    logger.warning("Threat intelligence upstream returned HTTP %d", response.status_code)
                    return ThreatIntelligenceResult(
                        status=ThreatIntelStatus.service_unavailable,
                        source="Google Safe Browsing v4",
                        threat_types=[],
                        checked_at=now_utc,
                        details="Upstream threat intelligence service is currently unavailable.",
                    )

                else:
                    logger.warning("Threat intelligence API request failed with HTTP %d", response.status_code)
                    return ThreatIntelligenceResult(
                        status=ThreatIntelStatus.failed,
                        source="Google Safe Browsing v4",
                        threat_types=[],
                        checked_at=now_utc,
                        details="Threat intelligence lookup request failed or was rejected by upstream.",
                    )

        except httpx.TimeoutException:
            logger.warning("Threat intelligence query timed out after %s seconds", self.settings.THREAT_INTEL_TIMEOUT_SECONDS)
            return ThreatIntelligenceResult(
                status=ThreatIntelStatus.timeout,
                source="Google Safe Browsing v4",
                threat_types=[],
                checked_at=now_utc,
                details="Threat intelligence query timed out before upstream responded.",
            )

        except Exception as exc:
            # Safe exception handling: do not leak raw exception that might contain query params
            logger.error("Threat intelligence lookup encountered an unexpected error: %s", type(exc).__name__)
            return ThreatIntelligenceResult(
                status=ThreatIntelStatus.failed,
                source="Google Safe Browsing v4",
                threat_types=[],
                checked_at=now_utc,
                details="Threat intelligence lookup encountered an internal network error.",
            )


class MockThreatIntelligenceProvider(ThreatIntelligenceProvider):
    """
    Deterministic mock provider for automated unit testing and offline development.
    """

    def __init__(
        self,
        canned_status: ThreatIntelStatus = ThreatIntelStatus.no_threat_found,
        canned_threat_types: Optional[List[str]] = None,
        canned_details: Optional[str] = None,
    ):
        self.canned_status = canned_status
        self.canned_threat_types = canned_threat_types or []
        self.canned_details = canned_details or f"Mock result with status: {canned_status.value}"

    async def check_url(self, url: str) -> ThreatIntelligenceResult:
        now_utc = datetime.now(timezone.utc)
        return ThreatIntelligenceResult(
            status=self.canned_status,
            source="Mock Threat Intelligence Provider",
            threat_types=self.canned_threat_types,
            checked_at=now_utc,
            details=self.canned_details,
        )
