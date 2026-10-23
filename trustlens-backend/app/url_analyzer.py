from __future__ import annotations

import re
from typing import List, Optional, Set
import urllib.parse

from app.schemas import Finding, SeverityLevel, UrlValidationDetails


# Set of prominent high-value consumer brands frequently targeted in mobile phishing
KNOWN_BRAND_DOMAINS = {
    "paypal": {"paypal.com", "paypal.me"},
    "apple": {"apple.com", "icloud.com"},
    "google": {"google.com", "gmail.com", "google.co.uk", "google.co.in", "google.ca"},
    "microsoft": {"microsoft.com", "live.com", "office.com", "outlook.com", "microsoftonline.com"},
    "netflix": {"netflix.com"},
    "amazon": {"amazon.com", "amazon.co.uk", "amazon.de", "amazon.in", "amazon.co.jp"},
    "facebook": {"facebook.com", "fb.com"},
    "instagram": {"instagram.com"},
    "whatsapp": {"whatsapp.com"},
    "chase": {"chase.com"},
    "wellsfargo": {"wellsfargo.com"},
    "bankofamerica": {"bankofamerica.com"},
    "binance": {"binance.com"},
    "coinbase": {"coinbase.com"},
    "usps": {"usps.com"},
    "dhl": {"dhl.com"},
    "fedex": {"fedex.com"},
}

# Credential and urgency lure keywords frequently seen in scam URLs
SUSPICIOUS_LURE_KEYWORDS = {
    "login", "signin", "sign-in", "log-in",
    "verify", "verification", "verify-account", "account-suspended",
    "billing", "update-billing", "security-alert", "password-reset",
    "wallet-connect", "claim-reward", "kyc-update", "confirm-identity",
    "secure-portal", "auth-check"
}

STANDARD_PORTS = {80, 443, 8080, 8443}


def _extract_apex_domain(hostname: str) -> str:
    """
    Extract the effective apex / registered domain (second-level domain + TLD).
    Handles standard two-part TLDs (e.g. .co.uk, .com.au) simply and reliably.
    """
    parts = hostname.lower().split(".")
    if len(parts) <= 2:
        return hostname

    # Handle common ccTLD two-part suffixes (e.g., co.uk, com.au, org.uk, gov.uk, net.au)
    two_part_tlds = {"co.uk", "com.au", "org.uk", "gov.uk", "net.au", "co.in", "com.sg"}
    if len(parts) >= 3:
        potential_two_part = f"{parts[-2]}.{parts[-1]}"
        if potential_two_part in two_part_tlds:
            return ".".join(parts[-3:])

    return ".".join(parts[-2:])


def _check_brand_impersonation(details: UrlValidationDetails) -> Optional[Finding]:
    """
    Rule: RULE_BRAND_IMPERSONATION
    Detects brand names appearing in subdomains or misleading hyphenated tokens
    when the destination apex domain does not belong to the authentic brand.

    Limitation:
    Corporate affiliate domains or CDN partnerships may legitimate use brand names.
    This rule flags structural incongruities for user awareness, not as definitive proof of compromise.
    """
    if details.is_ip_address:
        return None

    hostname = details.hostname.lower()
    apex = _extract_apex_domain(hostname)

    for brand, legit_domains in KNOWN_BRAND_DOMAINS.items():
        # Check if the authentic apex domain matches
        if any(apex == legit or apex.endswith("." + legit) for legit in legit_domains):
            continue

        # Check if brand appears in subdomains or hyphenated labels
        # E.g. paypal.com.account-update.xyz or login-paypal.verify.com
        hostname_labels = hostname.split(".")
        subdomain_labels = hostname_labels[:-2] if len(hostname_labels) > 2 else []
        subdomain_text = ".".join(subdomain_labels)

        # Brand in subdomain prefix
        if brand in subdomain_text or any(brand in label for label in subdomain_labels):
            return Finding(
                rule_id="RULE_BRAND_IMPERSONATION",
                title="Deceptive Brand Impersonation Indicator",
                severity=SeverityLevel.high,
                evidence=f"Brand '{brand}' was detected in subdomain '{subdomain_text}', while the registered base domain is '{apex}'.",
                explanation=(
                    f"Scammers frequently prefix well-known brand names (such as '{brand}') "
                    "onto unrelated domains to deceive mobile users whose address bars truncate long URLs."
                ),
                recommendation=(
                    f"Do not enter passwords, one-time codes, or payment details. If you intended to visit {brand}, "
                    "navigate directly to their official registered domain."
                )
            )

        # Brand hyphenated in host prefix (e.g. chase-security-update.com)
        first_label = hostname_labels[0]
        if f"{brand}-" in first_label or f"-{brand}" in first_label:
            return Finding(
                rule_id="RULE_BRAND_IMPERSONATION",
                title="Deceptive Brand Impersonation Indicator",
                severity=SeverityLevel.high,
                evidence=f"Brand '{brand}' appears in hyphenated host label '{first_label}', which is not an official domain of {brand}.",
                explanation=(
                    f"Phishing campaigns often register hyphenated domains referencing legitimate brands ('{brand}') "
                    "to fabricate a deceptive aura of authenticity."
                ),
                recommendation=(
                    f"Avoid interacting with this page. Navigate directly to {brand}'s official website or app."
                )
            )

    return None


def _check_ip_host(details: UrlValidationDetails) -> Optional[Finding]:
    """
    Rule: RULE_RAW_IP_HOST or RULE_PRIVATE_IP_TARGET
    Detects URLs utilizing a raw IPv4 or IPv6 address instead of a domain name.

    Limitation:
    Development environments and local network hardware legitimately use IP addresses.
    """
    if not details.is_ip_address:
        return None

    if details.is_private_ip or details.is_loopback:
        return Finding(
            rule_id="RULE_PRIVATE_IP_TARGET",
            title="Internal / Loopback IP Target",
            severity=SeverityLevel.high,
            evidence=f"Destination '{details.hostname}' addresses an internal private, loopback, or reserved network space.",
            explanation=(
                "Scam messages occasionally link to private IP addresses or localhost to probe internal network hardware, "
                "exploit residential router admin interfaces, or confuse automated link security scanners."
            ),
            recommendation="Do not open links pointing to local or private IP addresses unless you intentionally set up a local network service."
        )

    return Finding(
        rule_id="RULE_RAW_IP_HOST",
        title="Bare IP Address Host",
        severity=SeverityLevel.high,
        evidence=f"Host '{details.hostname}' is a raw IP address rather than a registered domain name.",
        explanation=(
            "Legitimate consumer and commercial web services use registered domain names secured by domain-validated TLS certificates. "
            "Threat actors frequently deploy phishing lures on bare IP addresses to evade domain takedowns and blocklists."
        ),
        recommendation="Do not enter account credentials, personal identifiers, or financial information on unbranded bare IP hosts."
    )


def _check_excessive_subdomains(details: UrlValidationDetails) -> Optional[Finding]:
    """
    Rule: RULE_EXCESSIVE_SUBDOMAINS
    Detects unusually deep subdomain nesting (more than 3 subdomain labels).

    Limitation:
    Complex corporate multi-tenant infrastructures or CDNs may use multiple subdomain levels.
    """
    if details.is_ip_address:
        return None

    labels = details.hostname.split(".")
    # Discount the apex domain and TLD (typically last 2 labels, or 3 for ccTLDs)
    apex = _extract_apex_domain(details.hostname)
    apex_label_count = len(apex.split("."))
    subdomain_label_count = max(0, len(labels) - apex_label_count)

    if subdomain_label_count >= 3:
        subdomain_part = ".".join(labels[:subdomain_label_count])
        return Finding(
            rule_id="RULE_EXCESSIVE_SUBDOMAINS",
            title="Excessive Subdomain Nesting",
            severity=SeverityLevel.medium,
            evidence=f"Domain contains {subdomain_label_count} subdomain levels ('{subdomain_part}') preceding '{apex}'.",
            explanation=(
                "Deep subdomain nesting can be used to push the actual registered domain out of view on mobile screens, "
                "or to abuse free dynamic DNS wildcard providers."
            ),
            recommendation="Carefully examine the rightmost portion of the domain name to verify which organization actually owns the domain."
        )

    return None


def _check_unusual_port(details: UrlValidationDetails) -> Optional[Finding]:
    """
    Rule: RULE_UNUSUAL_PORT
    Detects non-standard ports outside conventional HTTP/HTTPS and standard web proxies.

    Limitation:
    Custom web servers, IoT devices, and dev staging sites commonly use alternate ports.
    """
    if details.port is not None and details.port not in STANDARD_PORTS:
        return Finding(
            rule_id="RULE_UNUSUAL_PORT",
            title="Non-Standard Port Number",
            severity=SeverityLevel.medium,
            evidence=f"URL specifies custom port ':{details.port}'.",
            explanation=(
                "Public web services almost exclusively utilize standard ports (80 for HTTP, 443 for HTTPS). "
                "Non-standard ports often indicate ephemeral hosting, unmanaged consumer hardware, or proxy relays."
            ),
            recommendation="Confirm why this link directs to an unusual network port before continuing."
        )
    return None


def _check_punycode(details: UrlValidationDetails) -> Optional[Finding]:
    """
    Rule: RULE_PUNYCODE_HOMOGLYPH
    Detects Internationalized Domain Names (IDN) encoded via Punycode.

    Limitation:
    Legitimate international websites written in non-Latin scripts legitimately use Punycode.
    """
    if details.is_punycode:
        display_unicode = details.unicode_hostname or details.hostname
        return Finding(
            rule_id="RULE_PUNYCODE_HOMOGLYPH",
            title="Internationalized Domain / Punycode Detected",
            severity=SeverityLevel.medium,
            evidence=f"Punycode encoded domain '{details.hostname}' decodes to '{display_unicode}'.",
            explanation=(
                "Internationalized Domain Names allow characters from other alphabets (such as Cyrillic or Greek) "
                "that look virtually identical to Latin letters. Scammers use homoglyphs to craft visually deceptive clone domains."
            ),
            recommendation="Inspect the decoded characters carefully to confirm the website belongs to the intended organization."
        )
    return None


def _check_credential_lures(details: UrlValidationDetails) -> Optional[Finding]:
    """
    Rule: RULE_CREDENTIAL_LURE_PATH
    Detects urgent credential, verification, or banking lure keywords in the URL path.

    Limitation:
    Legitimate web applications also have login and account pages.
    """
    lower_path = details.path.lower()
    found_keywords = [kw for kw in SUSPICIOUS_LURE_KEYWORDS if kw in lower_path]

    if found_keywords:
        # Flag with medium severity if combined with raw IP or unverified domain
        severity = SeverityLevel.high if details.is_ip_address else SeverityLevel.medium
        return Finding(
            rule_id="RULE_CREDENTIAL_LURE_PATH",
            title="Credential / Account Verification Lure",
            severity=severity,
            evidence=f"URL path contains sensitive account action terms: {', '.join(sorted(found_keywords))}.",
            explanation=(
                "Phishing messages routinely employ urgent language regarding suspended accounts, login verifications, "
                "or security resets to trick victims into surrendering confidential credentials."
            ),
            recommendation="Never click direct links to resolve urgent security or account alerts. Open your provider's official app or portal manually."
        )
    return None


def _check_insecure_protocol(details: UrlValidationDetails) -> Optional[Finding]:
    """
    Rule: RULE_INSECURE_HTTP_SCHEME
    Identifies plain HTTP connections lacking TLS encryption.

    Limitation:
    Using HTTP does NOT mean a website is inherently malicious; it indicates a lack of encryption.
    """
    if details.scheme == "http":
        return Finding(
            rule_id="RULE_INSECURE_HTTP_SCHEME",
            title="Unencrypted HTTP Protocol",
            severity=SeverityLevel.low,
            evidence="URL uses 'http://' scheme without TLS/SSL transport layer security.",
            explanation=(
                "Communications over plain HTTP are transmitted in cleartext and vulnerable to interception "
                "or tampering on public or untrusted networks. Note: HTTP alone does not prove malicious intent, but lacks standard modern security."
            ),
            recommendation="Do not submit passwords, payment card numbers, or private data on unencrypted HTTP pages."
        )
    return None


def _check_userinfo_auth_trick(details: UrlValidationDetails) -> Optional[Finding]:
    """
    Rule: RULE_CONFUSING_AUTH_CHARACTERS
    Detects '@' symbols placed inside the path or query string to mislead mobile users.

    Limitation:
    Certain legitimate URL parameters (like email addresses or OAuth redirects) may contain '@'.
    """
    # Embedded credentials in netloc are rejected during validation; check path and query here
    parsed = urllib.parse.urlsplit(details.normalized_url)
    if "@" in parsed.path or "@" in parsed.query:
        return Finding(
            rule_id="RULE_CONFUSING_AUTH_CHARACTERS",
            title="Confusing Authentication Syntax in URL",
            severity=SeverityLevel.medium,
            evidence="The '@' character appears within the URL path or query string.",
            explanation=(
                "Attackers sometimes insert '@' characters inside URL paths or query strings to mimic user-authentication "
                "syntax and deceive mobile users about the actual destination domain."
            ),
            recommendation="Verify the destination address carefully before opening this URL."
        )
    return None


def analyze_url_structure(details: UrlValidationDetails) -> List[Finding]:
    """
    Runs the full battery of local heuristic rules against the validated URL.

    Guarantees:
    - Entirely offline and local: never reaches out to the target host.
    - Transparent and explainable: each rule provides stable ID, title, severity, evidence, explanation, and recommendation.
    - Nuanced: avoids naive false-positive classifications.
    """
    findings: List[Finding] = []

    # 1. IP-address hosts (raw or private/loopback)
    ip_finding = _check_ip_host(details)
    if ip_finding:
        findings.append(ip_finding)

    # 2. Deceptive brand impersonation
    brand_finding = _check_brand_impersonation(details)
    if brand_finding:
        findings.append(brand_finding)

    # 3. Excessive subdomain nesting
    subdomain_finding = _check_excessive_subdomains(details)
    if subdomain_finding:
        findings.append(subdomain_finding)

    # 4. Unusual port usage
    port_finding = _check_unusual_port(details)
    if port_finding:
        findings.append(port_finding)

    # 5. Punycode / IDN homoglyphs
    punycode_finding = _check_punycode(details)
    if punycode_finding:
        findings.append(punycode_finding)

    # 6. Credential lure keywords
    lure_finding = _check_credential_lures(details)
    if lure_finding:
        findings.append(lure_finding)

    # 7. Unencrypted HTTP transport
    http_finding = _check_insecure_protocol(details)
    if http_finding:
        findings.append(http_finding)

    # 8. User-info authentication confusion in path/query
    userinfo_finding = _check_userinfo_auth_trick(details)
    if userinfo_finding:
        findings.append(userinfo_finding)

    return findings
