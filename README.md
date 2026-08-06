# Intelligent Leave Request Application

A Spring Boot application that accepts employee leave request documents (`.txt` and `.pdf`), automatically categorizes the leave type, stores the uploaded files in OCI Object Storage, and authenticates users using OAuth 2.0 (Google).

## Manual Testing

A minimal HTML upload form is included at `src/main/resources/static/test.html`. After signing in with Google in the same browser session, open:

```
http://localhost:8080/test.html
```

Sample leave request files for testing are provided in the `sample-files/` folder.

## Features

- **Secure REST API** — protected by Spring Security with OAuth 2.0 (Google Login)
- **File upload** — accepts `.txt` and `.pdf` leave request documents
- **Automatic categorization**
  - `.txt` files → keyword-based rule matching
  - `.pdf` files → text extraction (Apache PDFBox) + classification via Gemini Flash LLM (bonus feature)
- **Cloud storage** — every uploaded file is stored in OCI Object Storage; the response includes the generated object name and object ID
- **Fully externalized configuration** — all environment-specific values (credentials, region, bucket, port) are read from environment variables, with no secrets committed to source control

## Tech Stack

| Component | Technology |
|---|---|
| Language / Runtime | Java 17 |
| Framework | Spring Boot 4.1.0 (Spring Framework 7) |
| Build tool | Gradle (wrapper included) |
| Security | Spring Security + OAuth 2.0 (Google) |
| Cloud Storage | Oracle Cloud Infrastructure (OCI) Object Storage |
| PDF parsing | Apache PDFBox 3 |
| LLM categorization | Google Gemini Flash API |

## Project Structure

```
leave-portal-app/
├── src/main/java/com/agileoracles/leave_portal_app/
│   ├── config/          → SecurityConfig (OAuth2 setup)
│   ├── controller/      → LeaveRequestController (REST endpoint)
│   ├── dto/              → Response/result objects
│   ├── service/          → Business logic (categorization, PDF extraction, OCI upload, LLM)
│   └── LeavePortalAppApplication.java
└── src/main/resources/
    ├── application.properties
    └── static/test.html  → simple browser-based upload form for manual testing
```

## Prerequisites

- Java 17 (JDK)
- An OCI account with:
  - An Object Storage bucket
  - An API signing key pair and a `config` file (see [OCI SDK docs](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdkconfig.htm))
- A Google Cloud project with an OAuth 2.0 Web Client (Client ID + Secret)
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey) (free tier, no billing required) — only needed for the PDF bonus feature

## Configuration

All configuration is externalized via environment variables. Set these before running the application:

| Variable | Description |
|---|---|
| `GOOGLE_CLIENT_ID` | OAuth 2.0 Client ID from Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | OAuth 2.0 Client Secret from Google Cloud Console |
| `OCI_REGION` | OCI region identifier (e.g. `us-ashburn-1`) |
| `OCI_NAMESPACE` | OCI Object Storage namespace |
| `OCI_BUCKET_NAME` | Target bucket name |
| `OCI_CONFIG_FILE` | Absolute path to the OCI SDK `config` file |
| `OCI_PROFILE` | Profile name inside the OCI config file (defaults to `DEFAULT`) |
| `GEMINI_API_KEY` | Gemini API key, only needed for PDF uploads |
| `SERVER_PORT` | Port the application listens on (defaults to `8080`) |

See `application.properties.example` in this repository for the exact property mapping.

## Running Locally

```bash
cd leave-portal-app
./gradlew bootRun
```

The application starts on `http://localhost:8080` (or the port set via `SERVER_PORT`).

## API

### `POST /api/leave-requests/upload`

Requires an authenticated session (Google login via `oauth2Login`). Accepts a `multipart/form-data` request with a single field:

| Field | Type | Description |
|---|---|---|
| `file` | file | The leave request document (`.txt` or `.pdf`) |

**Sample response:**

```json
{
  "authenticatedUser": "user@example.com",
  "fileName": "sample_sick_leave.txt",
  "category": "Sick Leave",
  "matchedReason": "Matched keywords: fever, doctor",
  "uploadTimestamp": "2026-08-06T18:27:46.192",
  "ociObjectName": "a645f752-...-sample_sick_leave.txt",
  "ociObjectId": "541b8977-2190-4767-8fc5-4e9dd25bf6c0"
}
```

### Leave Categories

| Category | Keywords (for `.txt` files) |
|---|---|
| Sick Leave | doctor, hospital, fever, surgery |
| Annual Leave | vacation, holiday, family |
| Emergency Leave | emergency, urgent |
| Maternity Leave | maternity, childbirth |
| Unpaid Leave | unpaid |
| Other | (no keywords matched) |

For `.pdf` files, the category is instead determined by the Gemini Flash LLM based on the full extracted document text.
