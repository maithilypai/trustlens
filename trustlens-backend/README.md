# TRUSTLENS Backend API

TRUSTLENS is an AI-assisted scam link analysis and threat intelligence backend designed for mobile and web environments, tailored specifically for consumption by an Android application built with Kotlin and Jetpack Compose.

TRUSTLENS empowers users to inspect suspicious links, analyze deceptive SMS/smishing messages, evaluate decoded QR code payloads, and explore educational scam simulations without compromising user privacy.

---

## Core Security & Architecture Principles

1. **Zero URL Navigation / SSRF Protection**: The backend performs purely structural, syntactic, and offline heuristic analysis locally. The backend **never** fetches, opens, downloads, or follows redirects for submitted target URLs.
2. **Explainable Findings**: Heuristics produce transparent, human-readable findings containing stable rule IDs, observed evidence, scam explanations, and actionable defensive recommendations.
3. **No False Guarantees**: Clean local heuristics with an unavailable or unconfigured threat intelligence provider return `inconclusive`, **never** an unverified "safe" verdict.
4. **Controlled Demonstration**: Dedicated demonstration endpoints provide deterministic fixtures for UI testing and Android application development with `is_simulated: true`.
5. **Zero Data Retention MVP**: Submitted URLs, SMS messages, and QR payloads are processed entirely in-memory and never written to a database or persistent disk log.
6. **Mobile-First Network Design**: Standardized CORS origin support including `http://10.0.2.2:8000` (the Android emulator loopback alias).

---

## Project Structure

```text
trustlens-backend/
├── app/
│   ├── __init__.py               # Package metadata
│   ├── config.py                 # Pydantic Settings with env parsing
│   ├── demo_service.py           # Controlled demonstration scenarios & comparison demo
│   ├── main.py                   # FastAPI app factory, routes, and middleware
│   ├── message_analyzer.py       # SMS / suspicious message social engineering analyzer
│   ├── qr_analyzer.py            # QR code payload & quishing analyzer
│   ├── risk_assessment.py        # Assessment synthesis matrix & rules
│   ├── schemas.py                # Pydantic v2 request/response schemas & enums
│   ├── threat_intelligence.py    # Modular provider (Google Safe Browsing v4 & Mock)
│   ├── url_analyzer.py           # Local explainable heuristic rules engine
│   └── url_validator.py          # Strict URL validation, normalization & safety checks
├── tests/
│   ├── __init__.py               # Test package
│   ├── test_analysis.py          # Heuristic rules & risk assessment matrix tests
│   ├── test_api.py               # Core FastAPI integration & demo tests
│   ├── test_educational_demos.py # Six educational scam scenarios & comparison demo tests
│   ├── test_message_analysis.py  # SMS / smishing / OTP solicitation tests
│   ├── test_qr_analysis.py       # QR quishing, payment URI, and Wi-Fi tests
│   └── test_validation.py        # Scheme, credentials, IP, and IDNA validation tests
├── .env.example                  # Environment configuration template
├── .env                          # Local environment settings
├── .gitignore                    # Git ignore file
├── README.md                     # Complete architectural & operational documentation
└── requirements.txt              # Production and development dependencies
```

---

## Prerequisites

- **Python**: 3.10 or 3.11+
- **Pip**: Latest version recommended
- **Operating System**: Linux, macOS, or Windows

---

## Installation & Setup

### 1. Create and Activate Virtual Environment

**Windows (PowerShell):**
```powershell
python -m venv .venv
.venv\Scripts\Activate.ps1
```

**Linux / macOS:**
```bash
python3 -m venv .venv
source .venv/bin/activate
```

### 2. Install Dependencies

```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### 3. Configure Environment Variables

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

Edit `.env` to configure your settings:

```ini
# Application Mode
ENVIRONMENT=development
DEBUG=false

# Allowed CORS Origins (10.0.2.2 is Android emulator host loopback)
CORS_ORIGINS=http://localhost:3000,http://localhost:8000,http://127.0.0.1:8000,http://10.0.2.2:8000

# URL constraint limits
MAX_URL_LENGTH=2048

# Google Safe Browsing API Configuration (Optional)
# If left blank, TRUSTLENS runs with local structural heuristics only
SAFE_BROWSING_API_KEY=
SAFE_BROWSING_CLIENT_ID=trustlens-backend
SAFE_BROWSING_CLIENT_VERSION=1.0.0
THREAT_INTEL_TIMEOUT_SECONDS=4.0
```

---

## Running the Backend

From the `trustlens-backend` directory:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Or from the project root:

```powershell
$env:PYTHONPATH = "trustlens-backend"
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The server will be available at:
- **Base URL**: `http://127.0.0.1:8000`
- **Interactive OpenAPI Documentation (Swagger UI)**: `http://127.0.0.1:8000/docs`
- **ReDoc Alternative Documentation**: `http://127.0.0.1:8000/redoc`

---

## API Capabilities & Endpoints

### 1. Root Service Information
- **Method**: `GET /`
- **Description**: Returns service identification, semantic version, and advertised capabilities.

### 2. System Health Check
- **Method**: `GET /health`
- **Description**: Reports service operational readiness, capabilities, and whether threat intelligence is active.

### 3. Live URL Analysis
- **Method**: `POST /api/v1/analyze`
- **Request Body**:
```json
{
  "url": "https://paypal.com.verify-account.security-alert.xyz/signin"
}
```
- **Returns**: Structured findings, syntactic breakdown, threat intelligence result, executive summary, and recommended defensive actions.

### 4. SMS & Suspicious Message Analysis
- **Method**: `POST /api/v1/analyze/message`
- **Description**: Evaluates text message bodies for social engineering lures (OTP theft, postal delivery smishing, urgent pressure, financial bait, authority impersonation) and safely extracts and inspects all embedded URLs without fetching them.
- **Request Body**:
```json
{
  "message": "USPS Notice: Package #US91823 has unpaid customs fee ($1.85). Pay within 24h: https://usps-redelivery.test/parcel",
  "sender": "+18005550199"
}
```
- **Response**: Extracted URLs, message-level findings, embedded URL analysis breakdowns, and action checklist.

### 5. QR Code Payload Analysis
- **Method**: `POST /api/v1/analyze/qr`
- **Description**: Analyzes decoded QR code strings across multiple payload formats:
  - **URLs (Quishing)**: Evaluates the web target using URL heuristics and threat intelligence.
  - **Payment URIs (`upi://pay`, `bitcoin:`)**: Flags direct financial transfers and educates users that QR codes only send funds, never receive them.
  - **Wi-Fi Provisioning (`WIFI:S:...`)**: Identifies rogue access point setup attempts.
  - **Telephony Actions (`tel:`, `smsto:`)**: Flags auto-dialing or premium-rate SMS prompts.
- **Request Body**:
```json
{
  "payload": "upi://pay?pa=scam-billing@bank&pn=ParkingFee&am=25.00&cu=USD"
}
```

### 6. Six Controlled Educational Scam Scenarios
- **Method**: `POST /api/v1/demo`
- **Description**: Deterministic, offline simulations illustrating real scam mechanics. Every response sets `"is_simulated": true`.
- **Supported Scenarios**:
  1. `scam_link`: Brand lookalike deceptive URL (`https://secure-login.chase-verification.example.test/auth-portal`).
  2. `parcel_sms`: Postal delivery fee smishing (`https://usps-redelivery-fee.example.test/parcel`).
  3. `otp_theft`: Account takeover via fake wire cancellation OTP theft (`https://bank-fraud-department.example.test/cancel`).
  4. `fake_payment`: Urgent supplier invoice / billing escalation redirection (`https://supplier-billing-escalation.example.test/invoice`).
  5. `qr_scam`: Quishing parking meter / payment sticker fraud (`https://quick-parking-meter.example.test/pay`).
  6. `fraud_call`: Coordinated vishing phone + SMS verification attack (`https://fraud-support-call.example.test/verify`).
  7. `no_known_threat`: Authentic domain simulation (`https://legitimate-sample-service.example.org/about`).
  8. `inconclusive`: Unreachable threat intelligence simulation (`https://unverified-community-showcase.example.org/`).

### 7. Side-by-Side Comparison Demo
- **Method**: `POST /api/v1/demo/compare` or `GET /api/v1/demo/compare`
- **Description**: Returns a side-by-side contrast between a simulated deceptive phishing URL (`paypal.com.verify-account.security-alert.xyz/signin`) and a harmless reserved test URL (`example.com/about` from RFC 2606), explaining the exact structural and reputational differences.

---

## Risk Assessment Matrix

| Threat Intelligence Status | Local Heuristics | Assessment Category | Rational Summary |
|---|---|---|---|
| `confirmed_threat` | Any | `confirmed_threat` | Threat intelligence match overrides local heuristics. Immediate scam/malware danger. |
| Any | High/Critical Severity (e.g. Brand Impersonation, Bare IP, OTP Theft) | `suspicious_indicators` | Heuristics detected high-risk deceptive structures or credential solicitation. |
| Any | Medium Severity (e.g. Non-Standard Port, Homoglyphs, Urgency) | `suspicious_indicators` | Unusual patterns warrant user caution. |
| `no_threat_found` | Clean (No findings or low HTTP finding only) | `no_known_threat` | No known threat records in database; local structure is clean. |
| `not_configured`, `service_unavailable`, `timeout`, or `failed` | Clean | `inconclusive` | Clean local structure, but absence of threat intelligence corroboration prevents a definitive safe verdict. |

---

## Running Automated Tests

Run the full pytest suite:

```bash
pytest -v trustlens-backend/tests
```

Or from project root in Windows PowerShell:
```powershell
$env:PYTHONPATH = "trustlens-backend"
.venv\Scripts\pytest -v trustlens-backend\tests
```

### Test Suite Breakdown (80 tests total, 100% passing):
- **`test_validation.py`** (28 tests): Ordinary HTTPS/HTTP, normalization, port stripping, missing scheme, unsupported schemes (`ftp`, `javascript`, `file`, `data`, `ssh`), embedded credentials rejection, maximum length limits, private/loopback/multicast IPv4/IPv6 classification, and Punycode/IDN parsing.
- **`test_analysis.py`** (15 tests): Deceptive brand masquerading in subdomains and hyphenated labels, authentic brand safety protection, bare public IP host detection, RFC 1918 private IP detection, excessive subdomains, non-standard ports, credential lures, HTTP transport warning, and the full risk assessment matrix.
- **`test_api.py`** (15 tests): Root endpoint, health check, live analysis with mocked threat intel, timeouts, unconfigured status, validation error responses (HTTP 400/422), demonstration scenarios, and CORS preflight.
- **`test_message_analysis.py`** (8 tests): Safe URL extraction, OTP theft solicitation, parcel smishing, urgency pressure, authority impersonation, URL shorteners, embedded link evaluation, and clean messages.
- **`test_qr_analysis.py`** (5 tests): Payload classification (URL, UPI payment, Bitcoin, Wi-Fi, telephony, plain text), direct payment transfer warning, quishing URL threat detection, and Wi-Fi provisioning.
- **`test_educational_demos.py`** (9 tests): All six educational scam scenarios, side-by-side comparison endpoints (GET & POST), message analysis endpoint, QR analysis endpoint, and health capabilities advertisement.

---

## Security Safeguards

1. **Zero Credential Collection**: TRUSTLENS never collects or prompts for real passwords, OTPs, PINs, or banking credentials.
2. **Zero Payment Initiation**: TRUSTLENS never processes or routes financial transactions.
3. **No Target URL Navigation**: The backend strictly inspects strings and tokens locally; it never issues HTTP requests to arbitrary user-submitted URLs.
4. **No Premature "Safe" Declarations**: Absences of blocklist records are clearly described as lack of indexed matches, not proof of complete safety.
5. **Clear Simulation Labeling**: All demonstration and simulation responses set `is_simulated: true`.
