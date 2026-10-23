import pytest
from app.url_validator import UrlValidationError, validate_and_normalize_url


def test_valid_ordinary_https_url():
    """Verify standard HTTPS URLs parse and normalize properly."""
    url = "https://example.com/welcome"
    details = validate_and_normalize_url(url)
    assert details.valid is True
    assert details.scheme == "https"
    assert details.hostname == "example.com"
    assert details.path == "/welcome"
    assert details.normalized_url == "https://example.com/welcome"
    assert details.is_ip_address is False
    assert details.is_private_ip is False
    assert details.is_loopback is False


def test_valid_http_url():
    """Verify standard HTTP URLs parse and normalize properly."""
    url = "http://example.org/portal"
    details = validate_and_normalize_url(url)
    assert details.valid is True
    assert details.scheme == "http"
    assert details.hostname == "example.org"
    assert details.normalized_url == "http://example.org/portal"


def test_url_normalization_defaults():
    """Ensure empty path becomes '/' and default ports are stripped."""
    details_http = validate_and_normalize_url("http://example.com:80")
    assert details_http.port is None
    assert details_http.path == "/"
    assert details_http.normalized_url == "http://example.com/"

    details_https = validate_and_normalize_url("https://EXAMPLE.COM:443")
    assert details_https.port is None
    assert details_https.hostname == "example.com"
    assert details_https.normalized_url == "https://example.com/"


def test_custom_port_preserved():
    """Ensure non-standard port numbers are preserved in normalized output."""
    details = validate_and_normalize_url("https://example.com:8443/api")
    assert details.port == 8443
    assert details.normalized_url == "https://example.com:8443/api"


def test_reject_missing_scheme():
    """Reject URLs lacking http:// or https:// scheme."""
    with pytest.raises(UrlValidationError) as exc_info:
        validate_and_normalize_url("example.com/path")
    assert exc_info.value.code == "MISSING_SCHEME"


@pytest.mark.parametrize("scheme_url", [
    "ftp://files.example.com/data.zip",
    "file:///etc/shadow",
    "javascript:alert(document.cookie)",
    "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==",
    "ws://streaming.example.com",
    "ssh://root@example.com",
])
def test_reject_unsupported_schemes(scheme_url):
    """Reject non-HTTP(S) schemes."""
    with pytest.raises(UrlValidationError) as exc_info:
        validate_and_normalize_url(scheme_url)
    assert exc_info.value.code == "UNSUPPORTED_SCHEME"


@pytest.mark.parametrize("cred_url", [
    "https://admin:secret123@example.com/dashboard",
    "http://user@target.com",
    "https://bank.com:login@phishing-attacker.com/session",
])
def test_reject_embedded_credentials(cred_url):
    """Reject URLs with embedded credentials to prevent obfuscation and SSRF abuse."""
    with pytest.raises(UrlValidationError) as exc_info:
        validate_and_normalize_url(cred_url)
    assert exc_info.value.code == "EMBEDDED_CREDENTIALS"


@pytest.mark.parametrize("empty_input", ["", "   ", None])
def test_reject_empty_or_whitespace_urls(empty_input):
    """Reject empty or whitespace-only input."""
    with pytest.raises(UrlValidationError) as exc_info:
        validate_and_normalize_url(empty_input)
    assert exc_info.value.code == "EMPTY_URL"


def test_reject_url_exceeding_max_length():
    """Reject URLs that exceed configured maximum length."""
    long_url = "https://example.com/" + ("a" * 2100)
    with pytest.raises(UrlValidationError) as exc_info:
        validate_and_normalize_url(long_url, max_length=2048)
    assert exc_info.value.code == "URL_TOO_LONG"


def test_reject_control_characters():
    """Reject URLs containing ASCII control codes."""
    with pytest.raises(UrlValidationError) as exc_info:
        validate_and_normalize_url("https://example.com/\x00evil")
    assert exc_info.value.code == "CONTROL_CHARS"


@pytest.mark.parametrize("ip_url, is_private, is_loopback", [
    ("http://127.0.0.1/", True, True),
    ("http://127.0.0.1:8080/", True, True),
    ("http://localhost/", True, True),
    ("http://192.168.1.1/admin", True, False),
    ("http://10.0.0.1/", True, False),
    ("http://172.16.0.1/", True, False),
    ("http://[::1]/", True, True),
    ("http://8.8.8.8/", False, False),
    ("https://1.1.1.1/", False, False),
])
def test_ip_address_classification(ip_url, is_private, is_loopback):
    """Accurately identify and categorize IPv4, IPv6, private, and loopback hosts."""
    details = validate_and_normalize_url(ip_url)
    if "localhost" in ip_url:
        assert details.is_loopback is True
        assert details.is_private_ip is True
    else:
        assert details.is_ip_address is True
        assert details.is_private_ip == is_private
        assert details.is_loopback == is_loopback


def test_punycode_and_idn_handling():
    """Correctly process Internationalized Domain Names."""
    # Example: münchen.de -> xn--mnchen-3ya.de
    details = validate_and_normalize_url("https://münchen.de/info")
    assert details.is_punycode is True
    assert details.hostname == "xn--mnchen-3ya.de"
    assert "münchen.de" in details.unicode_hostname
