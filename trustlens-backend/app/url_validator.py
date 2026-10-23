from __future__ import annotations

import codecs
import ipaddress
import re
from typing import Optional, Tuple
import urllib.parse

from app.schemas import UrlValidationDetails


class UrlValidationError(ValueError):
    """Exception raised when a submitted URL violates structural or security constraints."""
    def __init__(self, message: str, code: str = "INVALID_URL"):
        super().__init__(message)
        self.message = message
        self.code = code


ALLOWED_SCHEMES = {"http", "https"}


def _is_loopback_host(hostname: str) -> bool:
    """Check if hostname refers to localhost or loopback name."""
    lower_host = hostname.lower()
    return lower_host == "localhost" or lower_host.endswith(".localhost")


def _safe_parse_ip(hostname: str) -> Tuple[bool, bool, bool]:
    """
    Check if the hostname is a valid IPv4 or IPv6 address.
    Returns: (is_ip, is_private, is_loopback)
    """
    # Clean bracket notation from IPv6 if present
    clean_host = hostname.strip("[]")

    try:
        ip = ipaddress.ip_address(clean_host)
        is_private = (
            ip.is_private
            or ip.is_reserved
            or ip.is_link_local
            or ip.is_multicast
            or ip.is_unspecified
        )
        is_loopback = ip.is_loopback
        return True, is_private, is_loopback
    except ValueError:
        return False, False, False


def _safe_idna_conversion(hostname: str) -> Tuple[str, Optional[str], bool]:
    """
    Safely process internationalized domain names (IDN/Punycode).
    Returns: (ascii_hostname, unicode_hostname, is_punycode)
    """
    is_punycode = False
    unicode_hostname = None

    # Check for punycode prefix in any label
    labels = hostname.split(".")
    for label in labels:
        if label.lower().startswith("xn--"):
            is_punycode = True
            break

    try:
        # Convert to ASCII IDNA form
        ascii_hostname = codecs.encode(hostname, "idna").decode("ascii").lower()
        # Decode back to Unicode representation for inspection
        unicode_hostname = codecs.decode(ascii_hostname.encode("ascii"), "idna")
        if unicode_hostname != ascii_hostname:
            is_punycode = True
    except (UnicodeError, LookupError) as e:
        raise UrlValidationError(
            f"Domain name '{hostname}' cannot be converted to valid IDNA/Punycode: {str(e)}",
            code="INVALID_IDNA"
        ) from e

    return ascii_hostname, unicode_hostname, is_punycode


def validate_and_normalize_url(raw_url: str, max_length: int = 2048) -> UrlValidationDetails:
    """
    Performs strict structural validation and normalization of a submitted URL.

    Security Guarantees:
    - Never issues network requests or DNS queries to the target host.
    - Rejects non-HTTP(S) schemes (such as file, javascript, data, ftp).
    - Rejects embedded user credentials (user:pass@host).
    - Normalizes scheme, port, hostname, and path consistently.
    - Safely parses and categorizes raw IP addresses (IPv4 and IPv6).
    - Carefully decodes and normalizes Internationalized Domain Names.
    """
    if not raw_url or not isinstance(raw_url, str) or not raw_url.strip():
        raise UrlValidationError("URL must be a non-empty string.", code="EMPTY_URL")

    stripped_url = raw_url.strip()
    if len(stripped_url) > max_length:
        raise UrlValidationError(
            f"URL length ({len(stripped_url)} chars) exceeds maximum allowed limit of {max_length} characters.",
            code="URL_TOO_LONG"
        )

    # Disallow control characters and whitespace within the URL body
    if any(ord(c) < 32 or ord(c) == 127 for c in stripped_url):
        raise UrlValidationError("URL contains invalid control characters or ASCII escape sequences.", code="CONTROL_CHARS")

    try:
        parsed = urllib.parse.urlsplit(stripped_url)
    except Exception as e:
        raise UrlValidationError(f"Malformed URL structure could not be parsed: {str(e)}", code="MALFORMED_URL") from e

    # Scheme verification
    scheme = parsed.scheme.lower() if parsed.scheme else ""
    if not scheme:
        raise UrlValidationError(
            "URL is missing a protocol scheme. Only 'http://' and 'https://' are supported.",
            code="MISSING_SCHEME"
        )

    if scheme not in ALLOWED_SCHEMES:
        raise UrlValidationError(
            f"Unsupported protocol scheme '{scheme}'. TRUSTLENS only analyzes 'http://' and 'https://' URLs.",
            code="UNSUPPORTED_SCHEME"
        )

    # Embedded credentials verification (SSRF / Phishing vector: e.g. http://bank.com:login@attacker.com)
    if parsed.username or parsed.password or ("@" in parsed.netloc):
        raise UrlValidationError(
            "URL contains embedded user credentials (user:password@host). "
            "This deprecated format is frequently used in phishing scams to obfuscate destination hosts.",
            code="EMBEDDED_CREDENTIALS"
        )

    # Hostname validation
    hostname = parsed.hostname
    if not hostname:
        raise UrlValidationError(
            "URL does not contain a valid destination hostname or IP address.",
            code="MISSING_HOST"
        )

    # Strip trailing dot from FQDN representation (e.g., example.com. -> example.com)
    hostname = hostname.rstrip(".")
    if not hostname:
        raise UrlValidationError("URL hostname is empty after stripping root domain dots.", code="EMPTY_HOST")

    # Safe IP parsing
    is_ip, is_private, is_loopback = _safe_parse_ip(hostname)

    # Handle localhost name
    if not is_ip and _is_loopback_host(hostname):
        is_loopback = True
        is_private = True

    # Process IDN / Punycode if not an IP
    unicode_hostname: Optional[str] = None
    is_punycode = False
    normalized_host = hostname.lower()

    if not is_ip:
        normalized_host, unicode_hostname, is_punycode = _safe_idna_conversion(hostname)
        # Check for invalid hostname characters after IDNA conversion
        # Hostname labels can only contain a-z, 0-9, hyphen and dots
        if not re.match(r"^[a-z0-9\-\._]+$", normalized_host):
            raise UrlValidationError(
                f"Hostname '{hostname}' contains characters not permitted in standard internet hostnames.",
                code="INVALID_HOST_CHARACTERS"
            )

    # Port normalization
    port = parsed.port
    is_default_port = (scheme == "http" and port == 80) or (scheme == "https" and port == 443)
    display_port = None if is_default_port else port

    # Path normalization
    path = parsed.path
    if not path or path == "":
        path = "/"

    # Netloc construction for normalized URL
    if is_ip and ":" in hostname and not normalized_host.startswith("["):
        # Wrap IPv6 in brackets if needed
        host_netloc = f"[{normalized_host}]"
    else:
        host_netloc = normalized_host

    if display_port is not None:
        host_netloc = f"{host_netloc}:{display_port}"

    # Reconstruct normalized URL
    query_part = f"?{parsed.query}" if parsed.query else ""
    fragment_part = f"#{parsed.fragment}" if parsed.fragment else ""
    normalized_url = f"{scheme}://{host_netloc}{path}{query_part}{fragment_part}"

    return UrlValidationDetails(
        valid=True,
        scheme=scheme,
        hostname=normalized_host,
        normalized_url=normalized_url,
        port=display_port,
        path=path,
        is_ip_address=is_ip,
        is_private_ip=is_private,
        is_loopback=is_loopback,
        is_punycode=is_punycode,
        unicode_hostname=unicode_hostname,
    )
