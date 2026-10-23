# TRUSTLENS Android Frontend

**TRUSTLENS** is an AI-assisted scam link analysis and protection mobile client for Android, engineered with **Kotlin**, **Jetpack Compose**, and **Material 3**.

The application connects to the local **TRUSTLENS Python FastAPI Backend** to evaluate suspicious links, inspect structural indicators, query threat intelligence records, and guide users with transparent, explainable findings.

---

## Architecture & Technology Stack

- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose with Material 3 Dark Theme
- **Architecture**: Clean MVVM (Model-View-ViewModel) with unidirectional StateFlow (`TrustlensUiState`)
- **Networking**: Retrofit 2.11 + OkHttp 4.12 + Gson
- **Concurrency**: Kotlin Coroutines (`Dispatchers.IO`, `viewModelScope`)
- **System Integration**: Android Share Sheet (`ACTION_SEND`) for sharing links from browsers, SMS, and WhatsApp directly into TRUSTLENS
- **Testing**: JUnit 4 + Kotlinx Coroutines Test + Gson deserialization matrix tests

---

## Visual Design & Theme

TRUSTLENS utilizes a modern, dark navy cybersecurity aesthetic:
- **Background**: Deep Navy (`#0A0E1A`)
- **Surfaces & Cards**: Slate Navy (`#121829`, `#1C2438`, `#222C44`)
- **Accents**: Neon Cyan (`#00D2FF`) and Electric Blue (`#3B82F6`)
- **Risk Indicator Palette**:
  - `confirmed_threat`: Crimson Red (`#EF4444`) with pulsing warning emblem
  - `suspicious_indicators`: Caution Amber (`#F59E0B`)
  - `no_known_threat`: Verified Emerald (`#10B981`)
  - `inconclusive`: Insight Cyan (`#06B6D4`)
- **Simulation Disclaimer Badge**: Royal Violet (`#8B5CF6`)

---

## App Screens & Workflows

### 1. Home Dashboard (`HomeScreen.kt`)
- **Header**: Shield emblem with gradient glow, title **TRUSTLENS**, and core tagline:
  > *“Don't just trust the message. Verify the link.”*
- **Target URL Input**:
  - Outlined URI field with clear button.
  - Quick action **"Paste"** button pulling from the system clipboard.
  - Keyboard search action triggers analysis.
- **Analyze Link Button**:
  - Primary Cyan action with embedded loading spinner.
- **Demo Mode Button**:
  - Triggers the **Demo Scenario Picker** to test controlled backend scenarios (`threat_detected`, `no_known_threat`, `inconclusive`) against the real backend without testing real scam websites.
- **Safety Disclaimer**:
  - Prominent alert clarifying that automated analysis evaluates heuristics and threat intelligence, but no automated system can guarantee 100% safety against novel attacks.

### 2. Analysis Results (`ResultScreen.kt`)
- **Synthesis Card**:
  - Displays the overall assessment category (`confirmed_threat`, `suspicious_indicators`, `no_known_threat`, or `inconclusive`).
  - If simulated, displays: `SIMULATED DEMONSTRATION RESULT — NOT A LIVE THREAT DETECTION`.
  - Executive summary explaining the evaluation.
  - Actionable user recommendations list.
- **URL Structure Breakdown**:
  - Normalized URL (with one-tap copy button).
  - Protocol scheme (`HTTPS` / `HTTP`).
  - Destination hostname.
  - Security badges for raw IP addresses, private subnets, Punycode/IDN homoglyphs, and non-standard ports.
- **Threat Intelligence Repository Card**:
  - Status (`CONFIRMED MATCH`, `NO MATCHES IN DATABASE`, `SERVICE NOT CONFIGURED`, `SERVICE UNAVAILABLE`, or `TIMEOUT`).
  - Source provider (e.g., *Google Safe Browsing v4* or *Demonstration Fixture*).
  - Categorized threats (e.g. `SOCIAL_ENGINEERING`, `MALWARE`).
- **Local Heuristic Findings**:
  - Expandable finding cards.
  - Severity badge (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`).
  - Monospace rule ID (e.g., `RULE_BRAND_IMPERSONATION`).
  - Specific observed evidence string.
  - Explanation of why scammers use this structural pattern.
  - Recommended defensive action.
- **Verify Another Link**:
  - Button to reset state and return safely to the home dashboard.

### 3. Critical Threat Modal (`WarningDialog.kt`)
- When the assessment is `confirmed_threat`, an alert dialog is automatically surfaced.
- Features a danger shield icon and explicit warning message.
- Actions:
  - **"Go Back to Safety"**: Dismisses the result and clears the input.
  - **"View Detailed Findings"**: Allows the user to inspect the evidence safely inside the app without opening the URL.
- **Security Rule**: The app **never** automatically opens, fetches, or navigates to the submitted link.

### 4. Share Sheet Flow (`ACTION_SEND`)
- Users can highlight or share a link in Chrome, Google Messages, WhatsApp, or Email and choose **TRUSTLENS**.
- `MainActivity` intercepts the shared text, extracts the link, populates the input, and initiates analysis automatically.

---

## Backend Connectivity & Setup

### 1. Ensure Backend is Running
The Android frontend communicates with the TRUSTLENS FastAPI backend. Ensure the backend is active:

```powershell
# From the trustlens project root:
$env:PYTHONPATH = "trustlens-backend"
.venv\Scripts\uvicorn app.main:app --host 0.0.0.0 --port 8000
```
Verify by opening [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs) in your browser.

### 2. Android Emulator (Preconfigured Default)
The standard Android Studio emulator accesses your host computer's `localhost` via the special gateway address:
```
http://10.0.2.2:8000/
```
The Android app is preconfigured to use this address by default. **Do not use `http://127.0.0.1:8000/` inside the emulator**, as that refers to the virtual device itself!

### 3. Physical Android Device (Over Wi-Fi)
If running on a physical phone:
1. Connect your computer and your phone to the same Wi-Fi network.
2. Find your computer's local Wi-Fi IP address:
   - On Windows: run `ipconfig` (e.g. `192.168.1.150`).
3. Ensure your Windows Firewall permits inbound connections on port 8000.
4. Launch the TRUSTLENS app, tap the **Settings** gear icon in the top right, and set the Base URL to:
   ```
   http://192.168.1.150:8000/
   ```

### 4. Cleartext Network Security Configuration
Android 9+ (API 28+) blocks unencrypted HTTP traffic by default. TRUSTLENS includes `res/xml/network_security_config.xml`:
- Cleartext HTTP is permitted **strictly** for local development domains (`10.0.2.2`, `127.0.0.1`, `localhost`, and local private IP ranges).
- Production environments enforce strict HTTPS (`cleartextTrafficPermitted="false"`).

---

## Build and Run Instructions

### Prerequisites
- **Android Studio**: Iguana (2023.2), Jellyfish (2023.3), Koala (2024.1), or Ladybug (2024.2)+
- **JDK**: Java 17 or Java 21 (bundled with modern Android Studio)
- **Android SDK**: API 34 (Android 14) or API 35 (Android 15)
- **Minimum Supported Android Version**: Android 8.0 Oreo (API 26)

### Opening in Android Studio
1. Open Android Studio.
2. Select **File -> Open...** and select the `c:\trustlens\trustlens-android` directory.
3. Allow Gradle to sync dependencies.
4. Start an Android Virtual Device (AVD) running API 30+ (e.g. Pixel 8 with API 34).
5. Ensure the Python backend is running on port 8000.
6. Click the **Run 'app'** button (Shift + F10).

### Command-Line Build (via Gradle Wrapper)
When JDK 17+ and the Android SDK are installed in your PATH:

```powershell
cd c:\trustlens\trustlens-android

# Build Debug APK:
.\gradlew.bat assembleDebug

# Run Unit Tests:
.\gradlew.bat testDebugUnitTest
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Unit Testing Suite

The Android test suite validates response mapping, enum fidelity, and ViewModel state transitions:

1. **`AnalyzeResponseMappingTest.kt`**:
   - Tests JSON deserialization of live `POST /api/v1/analyze` responses.
   - Tests deserialization of simulated `POST /api/v1/demo` responses.
   - Verifies mapping of `RiskCategory`, `SeverityLevel`, and `ThreatIntelStatus` enums.
   - Tests structured error responses (`ApiErrorResponse`).

2. **`TrustlensViewModelTest.kt`**:
   - Tests URL input updates, clearing, and clipboard paste actions.
   - Tests client-side validation when empty URLs are submitted.
   - Tests successful analysis emission and state transitions.
   - Tests automatic triggering of `WarningDialog` when `CONFIRMED_THREAT` is received.
   - Tests network failure and timeout handling.
   - Tests demonstration scenario invocation.

---

## Honest Scope & Limitations for MVP

1. **No System-Wide Browser Hijacking**:
   This MVP does **not** silently intercept every link clicked across third-party apps. In standard Android security architecture, transparently intercepting all external links without user initiation requires an Accessibility Service or a local VPN packet tunnel.
2. **Explicit Verification & Share Sheet Support**:
   Users can verify links via:
   - Direct copy-pasting into the TRUSTLENS app.
   - Tapping **Share** on any link in Chrome, Google Messages, WhatsApp, or Slack and selecting **TRUSTLENS**.
3. **No Automatic URL Navigation**:
   TRUSTLENS intentionally does not render web views or download target payloads, ensuring the device remains insulated from malicious drive-by downloads.
