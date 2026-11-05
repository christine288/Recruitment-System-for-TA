# Recruitment System for TA

This repository contains the group project for **EBU6304 Software Engineering**.
The project aims to develop a **Teaching Assistant Recruitment System** for BUPT International School to improve the efficiency of the TA recruitment process.

---

## Project Overview

The current TA recruitment process mainly relies on email and Excel files, which may lead to fragmented information, repeated manual work, and difficulty in tracking applications. This project proposes a lightweight recruitment system that allows:

- **TA Applicants** to register, create profiles, upload CVs, browse job postings, submit applications, and track application status online
- **Module Organisers** to post TA jobs and review incoming applications
- **Administrators** to monitor overall recruitment information and workload

The system is developed following **Agile software development methods**.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 11 |
| Web | Java Servlet 4.0, JSP 2.3, JSTL 1.2 |
| Server | Apache Tomcat 9.x |
| Persistence | Plain JSON files + CV files on disk (`FileStore` in `com.bupt.ta.service`) |
| Security | CSRF filter on POST and role-based access; demo passwords are stored in plain text |
| Email | JavaMail — optional SMTP (MailHog in Docker Compose) |
| AI | Doubao via Volcengine Ark Chat Completions; explicit offline mock for tests |
| Build / test | Maven 3.9+, JUnit 5, Mockito |
| CI | GitHub Actions — `mvn clean verify` |

---

## Repository Structure

```text
Recruitment-System-for-TA/          # Repository root / Maven root
├── docs/                           # Project documents
│   ├── ProductBacklog_group51.xlsx
│   ├── Prototype_group51.pdf
│   └── Report_group51.pdf
├── pom.xml                         # Maven build (compile, test, WAR, run)
├── src/
│   ├── main/
│   │   ├── java/com/bupt/ta/
│   │   │   ├── ai/                 # LM client interfaces, mock + Ark HTTP client
│   │   │   ├── model/              # Data models (User, Job, Application, …)
│   │   │   ├── service/            # Business logic & file persistence
│   │   │   │   └── ai/             # AiFeatureService + feature builders
│   │   │   ├── util/               # AppConfig, HttpJsonClient, Strings
│   │   │   └── servlet/            # HTTP handlers (@WebServlet + web.xml)
│   │   └── webapp/
│   │       ├── static/css/         # app.css, admin-dashboard.css
│   │       ├── static/js/          # ai-stream.js
│   │       └── WEB-INF/
│   │           ├── web.xml
│   │           ├── lm.properties.example
│   │           ├── data/           # JSON seed data + runtime uploads
│   │           └── jsp/            # JSP view templates
│   └── test/java/                  # JUnit 5 tests
└── README.md

# Local build output (gitignored)
└── target/
```

---

## Prerequisites

Before building or running the project, ensure the following are installed:


| Tool          | Version     | Notes                                |
| ------------- | ----------- | ------------------------------------ |
| JDK           | 11 or above | Set `JAVA_HOME`                      |
| Apache Maven  | 3.9+        | `mvn` must be on your `PATH`         |


Install Maven on Windows (pick one):

```powershell
winget install Apache.Maven
```

Or download from [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi), unzip, and add `bin` to `PATH`. Verify:

```powershell
mvn -version
```

---

## Build & Run Instructions

All commands run from the **repository root**.

### Build WAR

```powershell
mvn package
```

Output: `target/ta-recruitment.war`

### Run tests

```powershell
mvn test
```

### Run locally (embedded Tomcat, port 18080)

For a local Doubao run using `ARK_API_KEY` and `ARK_MODEL` in the root `.env`, use:

```bash
python3 tools/run-local.py
```

The plain Maven command does not read `.env`, and `run-war` excludes the local
`WEB-INF/lm.properties` file. This helper passes the two Ark settings to Tomcat
as environment variables and skips tests that would inherit live credentials.
Press `Ctrl+C` to stop the server.

Without `.env` credentials, Maven can also run the WAR directly:

```powershell
mvn tomcat7:run-war
```

Open: [http://localhost:18080/ta-recruitment/login](http://localhost:18080/ta-recruitment/login)

### Deploy to external Tomcat 9

1. `mvn clean package`
2. Copy `target/ta-recruitment.war` to `{TOMCAT}/webapps/`
3. Start Tomcat (`startup.bat` / IDE)
4. Open `http://localhost:8080/ta-recruitment/login` (port depends on your Tomcat)

---

The login page lists demo accounts in a collapsible panel.

| Walkthrough | Link |
|-------------|------|
| Step-by-step user guide (with screenshots) | [docs/USER_MANUAL.md](docs/USER_MANUAL.md) |

---

## Features by Role

### TA applicant

1. Register or log in → 2. Complete profile (CV optional) → 3. Browse/filter/favorite jobs on `/job` → 4. Apply from job detail → 5. Track or withdraw applications on `/applications`

AI hints (when Doubao is configured): job recommendations, skill match, missing skills.

### Module organiser

1. Log in → 2. Open `/mo` dashboard → 3. Create/publish/edit/close vacancies → 4. Review applicant profile & CV → 5. Approve or reject with feedback

MO users only see applications whose `moduleName` + `moduleCode` match **their own** job postings. AI aids: workload advice, processed-decision review.

### Administrator

1. Log in → 2. Open `/admin` (KPIs, charts, AI analytics briefing) → 3. Manage users/jobs/export from dashboard tabs → 4. Browse TA profiles → 5. Override application status or export CSV

User management UI is on `/admin` → **User Management** tab. `/admin/users` is **POST-only** (direct GET returns HTTP 405).

---

## Applicant Profile & CV

The `/profile` page (`profile.jsp`) lets applicants:

- Enter personal information: full name, gender, degree, major, student ID, national ID, phone, email
- Add multiple education entries (stored as JSON in `profiles.json` → `educationJson`)
- List courses completed, availability, and skills
- **Upload a CV (optional):** PDF, DOC, or DOCX, max ~10 MB → `WEB-INF/data/cv/{userId}/`

**View vs edit:** incomplete profiles open in edit mode; complete profiles show read-only view with an **Edit** button. Saving returns to view mode.

On save, `AuthService.updateUserBasics` syncs name, student ID, and email to `users.json`. `CvDownloadServlet` at `/cv` serves the logged-in TA’s CV; MO may pass `?userId=` to download an applicant’s file.

Profile completeness is enforced by `ProfileService.isApplicantProfileComplete()` — **CV is not required** to apply.

---

## URL Routes


| URL                | Method     | Description                                                                               |
| ------------------ | ---------- | ----------------------------------------------------------------------------------------- |
| `/login`           | GET / POST | Login page                                                                                |
| `/register`        | GET / POST | New account registration                                                                  |
| `/logout`          | GET        | Invalidate session and redirect to login                                                  |
| `/profile`         | GET / POST | View and save applicant profile (multipart; CV upload)                                    |
| `/cv`              | GET        | Download / open uploaded CV (logged-in user only)                                         |
| `/forgot-password` | GET / POST | Forgot password (demo placeholder)                                                        |
| `/reset-password`  | GET / POST | Reset password (demo placeholder)                                                         |
| `/applications`    | GET        | View and filter own application statuses                                                  |
| `/job`             | GET        | TA job list + detail (`?id=`); login required; list page now includes TA-facing AI recommendation and missing-skills guidance |
| `/mo`              | GET / POST | Module organiser dashboard (`MoServlet`; mapping in `web.xml`)                            |
| `/mo/applicant-profile` | GET   | MO-only read-only applicant profile view (`?userId=`)                                     |
| `/admin`           | GET        | Administrator dashboard (requires `role` = `ADMIN`)                                       |
| `/admin/users`     | GET / POST | User management (ADMIN)                                                                   |
| `/admin/jobs`      | GET / POST | Job management (ADMIN)                                                                    |
| `/admin/job-view`  | GET        | Admin job view                                                                            |
| `/admin/ta-profiles` | GET      | TA profiles listing (ADMIN)                                                               |
| `/admin/cv`        | GET        | Admin CV download                                                                         |
| `/admin/export`    | GET        | Data export (ADMIN)                                                                       |
| `/admin/ai-demo`   | GET / POST | **Mock / demo** AI scaffold (admin only): skill match, missing skills, job recommendation |
| `/api/ai/stream`   | GET        | SSE endpoint for AI recommendation, skill-match, and missing-skills streaming responses   |


Seed accounts in `src/main/webapp/WEB-INF/data/users.json` (coursework; plain-text passwords): **admin** `admin@bupt.local` / `admin123` (`ADMIN`); **module organiser** `mo@bupt.local` / `mo123` (`MO`); demo TA applicants `alice.chen@bupt.local`, `brian.li@bupt.local`, `clara.wang@bupt.local`, and `daniel.zhang@bupt.local` all use password `ta123` and have profiles in `profiles.json`. New registrations get `role` = `TA`. Remove or change in production.

---

## Data Storage

All data is stored as JSON arrays in plain text files under `WEB-INF/data/`:


| File / location     | Contents                                                                                                                                                                                                                                                                                                                                                       |
| ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `users.json`        | User accounts (`id`, `name`, `studentId`, `email`, `passwordHash`, `role`, `active`)                                                                                                                                                                                                                                                                           |
| `profiles.json`     | Applicant profiles per `userId`: personal fields (`fullName`, `gender`, `degree`, `major`, `studentId`, `idCard`, `phone`, `email`), `educationJson` (array of education objects), `courses` (multiline text), `freeTime`, `skills`, `cvFileName`. Legacy keys (`degreeProgramme`, `yearOfStudy`, `availability`, `selfIntro`) may still exist for older rows. |
| `applications.json` | Application records per user: `id`, `userId`, `moduleName`, `moduleCode`, `role`, `applicationDate`, `status` (Pending/Accepted/Rejected), `feedback`                                                                                                                                                                                                          |
| `jobs.json`         | Job postings (`Job` / `JobService`)                                                                                                                                                                                                                                                                                                                             |
| `cv/{userId}/…`     | Uploaded CV files (created at runtime; not checked into Git)                                                                                                                                                                                                                                                                                                   |


There is **no database**. The `FileStore` class reads and writes these files directly using a hand-rolled JSON parser (no external library). `FileStore.toJsonArrayOfObjects` serialises nested education data inside `educationJson`.

> **Security note (coursework prototype):** Passwords are currently stored in plain text. In a production system they must be hashed (e.g. with BCrypt).

---

## AI Integration

The application uses **Doubao through Volcengine Ark** for AI features. It calls Ark's Chat Completions API from the Java server, so the API key is never sent to the browser. If credentials or the model are missing, the application reports a configuration error instead of generating a mock answer. AI output is **advisory** — hiring decisions remain human-reviewed. Ark's [Chat API documentation](https://docs.volcengine.com/docs/ark/chat-api?lang=zh) describes the request format and streaming responses.

### Configuration

| Key | Meaning | Default |
|-----|---------|---------|
| `LM_ENABLED` | Master switch | `true` |
| `LM_PROVIDER` | `doubao` \| `mock` \| `openai` \| `custom` | `doubao` |
| `ARK_API_KEY` | Volcengine Ark API key; `LM_API_KEY` is an alias | *(required for Doubao)* |
| `ARK_MODEL` | Available Doubao Model ID or Endpoint ID; `LM_MODEL` is an alias | *(required for Doubao)* |
| `LM_BASE_URL` | Ark API base URL; override for another region | `https://ark.cn-beijing.volces.com/api/v3` for Doubao |
| `LM_TIMEOUT_MS` | HTTP connect and non-streaming timeout; SSE requests have a 180-second minimum | `30000` |
| `LM_HTTP_CHAT_PATH` | Chat completions path | `/chat/completions` |

Get an API key from Volcengine Ark and select a Doubao Model ID or Endpoint ID available to your account. Set `ARK_API_KEY` and `ARK_MODEL` as server environment variables before starting a deployed WAR. For source-based `tomcat7:run`, you can copy `src/main/webapp/WEB-INF/lm.properties.example` to `src/main/webapp/WEB-INF/lm.properties` and fill in its two blank values. Maven excludes the local properties file from packaged WARs so the key is not distributed with the app. Both the properties file and root `.env` files are Git-ignored; **never commit API keys**. See Ark's [authentication guide](https://docs.volcengine.com/docs/ark/base-url-and-authentication?lang=en) for API key setup.

For the text-only recommendation and skill analysis features, a suitable model ID is
`doubao-seed-2-1-lite-260915` (subject to account access). Use the exact
[Model ID](https://docs.volcengine.com/docs/ark/model-list?lang=zh), not the
display name in a model picker. This app uses the regular Ark `/api/v3` endpoint;
[Agent/Coding Plan](https://docs.volcengine.com/docs/ark/integrate-third-party-tools?lang=zh)
uses a separate endpoint and key.

For Docker Compose, create a Git-ignored `.env` file in the repository root:

```dotenv
ARK_API_KEY=your-ark-api-key
ARK_MODEL=your-available-doubao-model-or-endpoint-id
```

Then run `mvn clean package` and `docker compose up --build`. Compose passes those values into the app container. When testing without network access, set `LM_PROVIDER=mock` explicitly; mock mode produces deterministic demo content. The `openai` and `custom` providers remain available for other compatible APIs.

LM-related tests live under `src/test/java/com/bupt/ta/ai/` and `…/service/ai/`. Unset global `ARK_*` and `LM_*` environment variables for deterministic local test runs.

Configuration priority order:

1. **Environment variables** (recommended for secrets)
2. **JVM system properties** (e.g. `-DLM_PROVIDER=mock`)
3. **Optional file**: `src/main/webapp/WEB-INF/lm.properties` for source-based runs
4. **Built-in defaults**

### Main packages


| Location                 | Role                                                                            |
| ------------------------ | ------------------------------------------------------------------------------- |
| `com.bupt.ta.ai`         | `LmClient`, DTOs, `LmConfig`, `MockLmClient`, `HttpLmClient`, `LmClientFactory` |
| `com.bupt.ta.service.ai` | `AiFeatureService`, feature-specific prompt builders, `AiFeatureOutput`         |
| `com.bupt.ta.util`       | `AppConfig` (config merge), `HttpJsonClient` (JDK 11 `HttpClient`)              |


Networking uses the JDK's `java.net.http` client. JUnit 5 and Mockito are test-only dependencies.

### Unit tests (LM workflow)

Tests live under `src/test/java/**` and mirror the LM integration layers:

| Test class | What it verifies |
|---|---|
| `LmProviderTypeTest` | `LmProviderType.fromString` |
| `AppConfigTest` | `AppConfig.resolve` (system property vs file vs default) |
| `LmConfigTest` | `LmConfig.load` + `mock.properties` / system overrides |
| `LmRequestTest` | `LmRequest` builder and immutable message list |
| `MockLmClientTest` | `MockLmClient` per `AiFeatureNames` |
| `LmClientFactoryTest` | `LmClientFactory.create` (mock, HTTP, fallback) |
| `HttpLmClientTest` | `HttpLmClient` fails fast without credentials |
| `SkillMatchServiceTest` / `MissingSkillServiceTest` / `RecommendationServiceTest` | Each feature service + `LmClient` |
| `AiFeatureServiceTest` | Full pipeline: config → factory → `AiFeatureService` |
| `AiFeatureOutputTest` | `AiFeatureOutput.fromResponse` mapping |

`com.bupt.ta.testsupport.LmTestSupport` provides a mocked `ServletContext` with optional `WEB-INF/lm.properties` content.

**Run JUnit tests:**

```powershell
mvn test
```

**Note:** `AppConfig.resolve` checks **environment variables first**. If a machine has `LM_PROVIDER` set globally, it may override a test file — unset it for deterministic tests, or rely on CI without those variables.

### Security and academic integrity

- **Never commit API keys**; use environment variables or local `lm.properties` excluded from Git.
- **AI output is advisory** only; hiring decisions must remain explainable and human-reviewed.
- For coursework, emphasise **transparent rules** (mock logic) and **where** a real model would plug in (`HttpLmClient` + factory).

---

## Team Members


| GitHub Alias  | Branch          | QMUL ID   | Name                  |
| ------------- | --------------- | --------- | --------------------- |
| Markrivera683 | Ruiyang_Sun     | 231226783 | Sun Ruiyang (孙瑞阳)     |
| christine288  | Qixin_Li        | 231225373 | Li Qixin (李其馨)        |
| Hzwnt         | Tianjing_Zhuang | 231225351 | Zhuang Tianjing (庄天婧) |
| S01ZZ         | Qinchun_Chen    | 231225410 | Chen Qinchun (陈沁纯)    |
| g726unknown   | Yifeng_Zhang    | 231226174 | Zhang Yifeng (张毅峰)    |
| negan525      | WeiJia_Xiao     | 231226233 | Xiao Weijia (肖炜佳)     |


**Note:**
Commits under the name "Chen Qinchun" correspond to the GitHub account **@S01ZZ**, due to local Git configuration (e.g., GitHub Desktop). All such contributions are made by the same contributor.

---

## Agile Development

The team follows an Agile workflow:

- **Product Backlog** maintained in `docs/ProductBacklog_group51.xlsx`
- Each team member works on a dedicated feature branch (named after the member)
- Pull Requests are used to merge completed features into `main`
- Iterations are tracked via GitHub Issues and the backlog spreadsheet

