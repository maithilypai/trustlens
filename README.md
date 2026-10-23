# TRUSTLENS — Real-Time Scam Link Detection & Protection

[![Backend Tests](https://img.shields.io/badge/backend%20tests-80%20passed-brightgreen.svg)]()
[![Platform](https://img.shields.io/badge/platform-Android%20%7C%20FastAPI-blue.svg)]()
[![License](https://img.shields.io/badge/license-MIT-green.svg)]()

> **“Don't just trust the message. Verify the link.”**

TRUSTLENS is an AI-assisted cybersecurity platform engineered to protect users from modern digital scams, including deceptive phishing links, smishing (SMS scams), QR code quishing, credential harvesting, and urgent payment fraud.

---

## Repository Structure

```text
trustlens/
├── trustlens-backend/         # Python FastAPI threat analysis engine
│   ├── app/                   # Heuristics, threat intel, validators, analyzers
│   ├── tests/                 # Comprehensive test suite (80 automated tests)
│   ├── requirements.txt       # Python dependencies
│   ├── .env.example           # Configuration template
│   └── README.md              # Backend documentation & API spec
│
├── trustlens-android/         # Native Android application
│   ├── app/                   # Kotlin + Jetpack Compose + Material 3
│   │   ├── src/main/java/     # MVVM architecture: UI, ViewModel, Repository, Network
│   │   └── src/test/java/     # Unit & deserialization tests
│   ├── gradle/                # Gradle wrapper & version catalogs
│   ├── build.gradle.kts       # Root build configuration
│   └── README.md              # Android setup & architecture guide
│
├── .gitignore                 # Monorepo git ignore (secrets, caches, build outputs)
└── README.md                  # This unified documentation
```

---

## Key Capabilities

1. **Zero-Navigation URL Inspection**:
   - The backend **never** navigates to, downloads, or follows redirects from arbitrary target URLs, eliminating Server-Side Request Forgery (SSRF) and malware infection vectors.
   - Evaluates brand impersonation, deceptive Punycode/homographs, IP hostnames, credential embedding, risky TLDs, and path entropy.

2. **Multi-Vector Threat Analysis**:
   - **URL Analysis**: Structural and reputation assessment.
   - **SMS & Message Analysis**: Transparent detection of urgency triggers, OTP theft requests, bank impersonation, and parcel delivery scams.
   - **QR Code Payloads**: Quishing detection, deceptive deep-links, and rogue Wi-Fi/payment payloads.

3. **Explainable AI & Actionable Guidance**:
   - Every finding includes rule identifiers, observed evidence, layman explanations, and concrete recommendations.
   - Never returns a naive "Safe" verdict when threat intelligence is inconclusive or unavailable.

4. **Educational Demonstration Suite**:
   - Built-in scenarios simulating scam links, parcel fraud, OTP theft, fake payment requests, and QR traps with explicit simulation banners (`is_simulated: true`).

5. **Modern Android Native Frontend**:
   - Built with Jetpack Compose and Material 3 Dark Cybersecurity theme.
   - Intercepts URLs from system Share Sheet (`ACTION_SEND`) directly from SMS, WhatsApp, and browsers.
   - Clear visual indicators distinguishing confirmed threats, suspicious indicators, inconclusive results, and safe demonstration fixtures.

---

## Quick Start Guide

### 1. Backend Setup

```bash
# Navigate to the backend directory
cd trustlens-backend

# Create and activate a Python virtual environment
python -m venv .venv
# On Windows:
.venv\Scripts\activate
# On Linux/macOS:
source .venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Run the test suite
pytest tests

# Launch the FastAPI development server
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Interactive API documentation (Swagger UI) is available at:
`http://127.0.0.1:8000/docs`

### 2. Android App Setup

1. Open **Android Studio** (Koala / Ladybug or newer recommended).
2. Select **Open** and choose the `trustlens-android` directory (`C:\trustlens\trustlens-android`).
3. Allow Gradle to sync the project dependencies.
4. Select an Android Emulator (API 34 recommended) or a physical device with USB debugging enabled.
5. **API Connection Settings**:
   - **Android Emulator**: Uses `http://10.0.2.2:8000/` automatically (the loopback alias to your host computer).
   - **Physical Device**: Tap the Settings icon in the app header and set the server URL to your host machine's local Wi-Fi IP (e.g., `http://192.168.1.50:8000/`). Ensure the backend host is `0.0.0.0`.
6. Click **Run** (`Shift + F10`) or build the APK via **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

---

## API Endpoints Overview

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Application info and health summary |
| `GET` | `/health` | Detailed health status and threat intel availability |
| `POST` | `/api/v1/analyze` | Deep URL security & heuristic analysis |
| `POST` | `/api/v1/analyze/message` | SMS / Smishing social engineering message analysis |
| `POST` | `/api/v1/analyze/qr` | QR code payload / quishing analysis |
| `POST` | `/api/v1/demo` | Deterministic demo scenarios (`threat_detected`, `no_known_threat`, `inconclusive`) |
| `GET/POST`| `/api/v1/demo/compare` | Side-by-side threat vs safe comparison fixture |

---

## Security & Privacy Guarantee

- **No Data Retention**: URLs, text messages, and QR payloads are processed in-memory and never logged or stored.
- **Zero Credential Handling**: The app never prompts for passwords, bank credentials, or OTP codes.
- **No Direct Payments**: The application does not execute financial transactions.
- **Secrets Management**: API keys and environment variables are strictly managed through `.env` files and excluded from version control.
# trustlens
AI-assisted scam link detection and digital safety Android application
