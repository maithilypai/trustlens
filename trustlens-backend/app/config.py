from __future__ import annotations

import os
from functools import lru_cache
from typing import List, Optional, Union
from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """TRUSTLENS application configuration loaded from environment or defaults."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )

    APP_NAME: str = "TRUSTLENS API"
    APP_VERSION: str = "1.0.0"
    API_PREFIX: str = "/api/v1"
    ENVIRONMENT: str = "development"
    DEBUG: bool = False

    # CORS configuration - Note: 10.0.2.2 is the Android emulator's alias to host 127.0.0.1
    CORS_ORIGINS: Union[List[str], str] = [
        "http://localhost:3000",
        "http://localhost:8000",
        "http://127.0.0.1:8000",
        "http://10.0.2.2:8000",
    ]

    @field_validator("CORS_ORIGINS", mode="after")
    @classmethod
    def assemble_cors_origins(cls, v: Union[str, List[str]]) -> List[str]:
        if isinstance(v, str):
            clean = v.strip()
            if clean.startswith("[") and clean.endswith("]"):
                import json
                try:
                    parsed = json.loads(clean)
                    if isinstance(parsed, list):
                        return [str(item) for item in parsed]
                except Exception:
                    pass
            return [i.strip() for i in clean.split(",") if i.strip()]
        return v

    # Analysis limits
    MAX_URL_LENGTH: int = 2048

    # Google Safe Browsing integration
    SAFE_BROWSING_API_KEY: Optional[str] = None
    SAFE_BROWSING_CLIENT_ID: str = "trustlens-backend"
    SAFE_BROWSING_CLIENT_VERSION: str = "1.0.0"
    THREAT_INTEL_TIMEOUT_SECONDS: float = 4.0


@lru_cache()
def get_settings() -> Settings:
    """Return a cached instance of application settings."""
    return Settings()
