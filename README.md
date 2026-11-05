# Recruitment System for TA

**EBU6304 Software Engineering · Group 51**

Web platform for Teaching Assistant recruitment at BUPT International School.

| | |
|---|---|
| **Version** | 1.0.0 |
| **License** | [MIT](LICENSE) |
| **Context path** | `/ta-recruitment` |
| **Storage** | JSON files under `WEB-INF/data/` — no database |

**Try it locally with Doubao:** `python3 tools/run-local.py` → [http://localhost:18080/ta-recruitment/login](http://localhost:18080/ta-recruitment/login)
**User guide:** [docs/USER_MANUAL.md](docs/USER_MANUAL.md)

---

## Project Overview

The current TA recruitment process mainly relies on email and Excel files, which leads to fragmented information, repeated manual work, and difficulty tracking applications. This project provides a lightweight recruitment system where:

- **TA Applicants** register, maintain profiles, optionally upload CVs, browse jobs, apply, and track status
- **Module Organisers (MO)** publish vacancies and review applications for their own jobs
- **Administrators** monitor users, jobs, applications, TA workload, analytics, and exports

The team follows **Agile** development (feature branches, pull requests, product backlog).

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 11 |
| Web | Java Servlet 4.0, JSP 2.3, JSTL 1.2 |
| Server | Apache Tomcat 9.x |
| Persistence | Plain JSON files + CV files on disk (`FileStore` in `com.bupt.ta.service`) |
| Security | BCrypt (`jbcrypt`), CSRF filter on POST, role-based access |
| Email | JavaMail — optional SMTP (MailHog in Docker Compose) |
| AI | Volcengine Ark Chat Completions with Doubao by default; explicit offline mock for tests |
| Build / test | Maven 3.9+, JUnit 5, Mockito |
| CI | GitHub Actions — `mvn clean verify` |

---

## Repository Structure

```text
Recruitment-System-for-TA/
├── docs/
│   ├── USER_MANUAL.md              # Step-by-step user guide (English)
│   ├── manual-screenshots/         # Screenshots for USER_MANUAL.md
│   ├── javadoc/                    # Generated API docs (mvn verify)
│   └── ProductBacklog_group51.xlsx
├── tools/
│   ├── capture_manual_screenshots.py
│   └── run-local.py                # Load root .env for local Doubao runs
├── src/main/java/com/bupt/ta/
│   ├── ai/                         # LmClient, MockLmClient, HttpLmClient, LmConfig
│   ├── model/                      # User, Job, Application, ApplicantProfile, …
│   ├── persistence/                # AppInitListener, ServiceFactory
│   ├── security/                   # PasswordHasher, CsrfFilter
│   ├── service/                    # Business logic + FileStore JSON I/O
│   │   ├── ai/                     # AiFeatureService + feature services
│   │   └── admin/                  # Admin dashboard metrics
│   ├── servlet/                    # HTTP controllers (@WebServlet + web.xml)
│   └── util/                       # AppConfig, HttpJsonClient, validation helpers
├── src/main/webapp/
│   ├── static/css/                 # app.css, admin-dashboard.css
│   ├── static/js/                  # ai-stream.js, admin-dashboard-charts.js
│   └── WEB-INF/
│       ├── web.xml                 # CsrfFilter; explicit /mo, /cv, /api/ai/stream
│       ├── lm.properties.example
│       ├── data/                   # Seed JSON + runtime uploads
│       └── jsp/                    # View templates
├── src/test/java/                  # Unit + integration tests
├── docker-compose.yml              # App + MailHog (optional)
├── Dockerfile
├── pom.xml
└── README.md
```

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 11+ | Set `JAVA_HOME` |
| Maven | 3.9+ | `mvn` on `PATH` |

```powershell
winget install Apache.Maven
mvn -version
```

---

## Build & Run

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

Full verify (tests + Javadoc copied to `docs/javadoc/`):

```powershell
mvn clean verify
```

### Run locally (embedded Tomcat, port 18080)

Create a Git-ignored `.env` file in the repository root with `ARK_API_KEY` and
`ARK_MODEL`, then run:

```bash
python3 tools/run-local.py
```

The helper loads those settings and starts `tomcat7:run-war`. A plain
`mvn tomcat7:run-war` does not load `.env`. Open
[http://localhost:18080/ta-recruitment/login](http://localhost:18080/ta-recruitment/login).
Stop with `Ctrl+C`.

### Deploy to external Tomcat 9

1. `mvn clean package`
2. Copy `target/ta-recruitment.war` to `{TOMCAT}/webapps/`
3. Start Tomcat
4. Open `http://localhost:8080/ta-recruitment/login` (port depends on your Tomcat)

Runtime data is written under the exploded WAR at `WEB-INF/data/`.

### Optional — Docker (app + MailHog)

Useful for password-reset email demos:

```powershell
mvn clean package
docker compose up --build
```

| Service | URL |
|---------|-----|
| Application | http://localhost:8080/ta-recruitment/login |
| MailHog (SMTP UI) | http://localhost:8025 |

Without SMTP, forgot-password tokens are still created; reset links are logged server-side for development.

---

## Demo Accounts

Seed accounts in `src/main/webapp/WEB-INF/data/users.json` use **plain-text passwords** for coursework. **New registrations** are stored with **BCrypt** hashes.

| Role | Email | Password |
|------|-------|----------|
| Administrator | `admin@bupt.local` | `admin123` |
| Module Organiser | `mo@bupt.local` | `mo123` |
| TA Applicant | `alice.chen@bupt.local` | `ta123` |
| TA Applicant | `brian.li@bupt.local` | `ta123` |
| TA Applicant | `clara.wang@bupt.local` | `ta123` |
| TA Applicant | `daniel.zhang@bupt.local` | `ta123` |

The login page lists demo accounts in a collapsible panel.

| Walkthrough | Link |
|-------------|------|
| Step-by-step user guide (with screenshots) | [docs/USER_MANUAL.md](docs/USER_MANUAL.md) |

---

## Features by Role

### TA applicant

1. Register or log in → 2. Complete profile (CV optional) → 3. Browse/filter/favorite jobs on `/job` → 4. Apply from job detail → 5. Track or withdraw applications on `/applications`

AI hints (Doubao when configured): job recommendations, skill match, missing skills.

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

Base URL: `{host}/ta-recruitment`

All state-changing **POST** requests require a valid CSRF token (`CsrfFilter`).

| URL | Method | Access | Description |
|-----|--------|--------|-------------|
| `/login` | GET / POST | Public | Login |
| `/register` | GET / POST | Public | TA registration |
| `/logout` | GET | Authenticated | End session |
| `/forgot-password` | GET / POST | Public | Request password reset (email if SMTP configured) |
| `/reset-password` | GET / POST | Public | Reset password with token |
| `/profile` | GET / POST | TA | View/save profile; multipart CV upload |
| `/cv` | GET | TA / MO | Download CV (MO: `?userId=`) |
| `/job` | GET / POST | TA | Job list & detail (`?id=`); POST `toggleFavorite` |
| `/applications` | GET / POST | TA | List/filter applications; POST apply or withdraw |
| `/mo` | GET / POST | MO | Dashboard, jobs, approve/reject |
| `/mo/applicant-profile` | GET | MO | Read-only applicant profile (`?userId=`) |
| `/admin` | GET | ADMIN | Dashboard, analytics, management tabs |
| `/admin/users` | POST | ADMIN | Create / activate / deactivate / delete users |
| `/admin/jobs` | POST | ADMIN | Delete job (from dashboard tab) |
| `/admin/job-view` | GET | ADMIN | Job detail (`?id=`) |
| `/admin/ta-profiles` | GET | ADMIN | TA profile listing |
| `/admin/cv` | GET | ADMIN | Download applicant CV (`?userId=`) |
| `/admin/applications/by-status` | GET | ADMIN | Applications filtered by status |
| `/admin/applications` | POST | ADMIN | Override application status |
| `/admin/export` | GET | ADMIN | CSV export (`?type=users` or `applications`) |
| `/admin/ai-demo` | GET / POST | ADMIN | AI feature playground |
| `/api/ai/stream` | GET | TA / MO / ADMIN | SSE streaming AI responses (`?feature=…`) |

---

## Data Storage

All persistent state lives under `WEB-INF/data/`:

| File / folder | Contents |
|---------------|----------|
| `users.json` | Accounts (`id`, `name`, `studentId`, `email`, `passwordHash`, `role`, `active`) |
| `profiles.json` | Applicant profiles per `userId` |
| `applications.json` | Applications (`status`: Pending / Accepted / Rejected / Withdrawn) |
| `jobs.json` | Job postings |
| `favorites.json` | TA job favorites |
| `recently-viewed.json` | Recently viewed jobs per user |
| `audit-logs.json` | Admin audit trail |
| `password-reset-tokens.json` | Password reset tokens |
| `cv/{userId}/…` | Uploaded CV files (runtime; not in Git) |

There is **no database**. `FileStore` reads/writes JSON with a hand-rolled parser; the AI HTTP client uses Gson for provider responses.

> **Security (coursework prototype):** Seed passwords in `users.json` are plaintext. New accounts use BCrypt. Change seed credentials before any public deployment.

---

## AI Integration

The application calls Volcengine Ark Chat Completions from the Java server for
AI features. The default provider is `doubao`; missing credentials produce a
visible configuration error. Set `LM_PROVIDER=mock` explicitly for offline,
deterministic demos and tests. AI output is **advisory** — hiring decisions
remain human-reviewed.

| Feature | Audience | Where |
|---------|----------|-------|
| Job recommendation | TA | `/job` → `?feature=recommendation` |
| Skill match | TA | Job detail → `?feature=skillMatch&jobId=` |
| Missing skills | TA | `/job` / job detail → `?feature=missingSkills&jobId=` |
| Workload advice | MO | `/mo` → `?feature=moWorkloadAdvice` |
| Decision review | MO | `/mo` → `?feature=moDecisionReview` |
| Platform analytics | ADMIN | `/admin` → `?feature=adminAnalytics` |
| Feature demo | ADMIN | `/admin/ai-demo` |

### Configuration (priority: env → JVM property → `WEB-INF/lm.properties` → defaults)

| Key | Meaning | Default |
|-----|---------|---------|
| `LM_ENABLED` | Master switch | `true` |
| `LM_PROVIDER` | `doubao` \| `mock` \| `openai` \| `custom` | `doubao` |
| `ARK_API_KEY` | Ark API key (`LM_API_KEY` alias) | *(required for Doubao)* |
| `ARK_MODEL` | Ark Model ID or Endpoint ID (`LM_MODEL` alias) | *(required for Doubao)* |
| `LM_BASE_URL` | Ark API base URL | `https://ark.cn-beijing.volces.com/api/v3` for Doubao |
| `LM_TIMEOUT_MS` | Connect and non-streaming timeout; SSE has a 180-second minimum | `30000` |
| `LM_HTTP_CHAT_PATH` | Chat completions path | `/chat/completions` |

For a local run, put the key and an enabled model ID in the Git-ignored root
`.env`, then run `python3 tools/run-local.py`. For this project's text tasks,
`doubao-seed-2-1-lite-260915` is a suitable ID if available to your account.
Use an exact [Model ID](https://docs.volcengine.com/docs/ark/model-list?lang=zh),
not the model picker's display name. This app uses the regular Ark `/api/v3`
endpoint; [Agent/Coding Plan](https://docs.volcengine.com/docs/ark/integrate-third-party-tools?lang=zh)
uses a different endpoint and key. **Never commit API keys.**

For Docker Compose, the root `.env` supplies `ARK_API_KEY` and `ARK_MODEL` to
the container. For source-based `tomcat7:run`, an optional
`src/main/webapp/WEB-INF/lm.properties` can provide local settings. The WAR
excludes this file.

### Key packages

| Package | Role |
|---------|------|
| `com.bupt.ta.ai` | `LmClient`, DTOs, `MockLmClient`, `HttpLmClient`, `LmClientFactory` |
| `com.bupt.ta.service.ai` | `AiFeatureService` + feature-specific prompt builders |
| `com.bupt.ta.util` | `AppConfig`, `HttpJsonClient` (JDK 11 `HttpClient`) |

LM-related tests live under `src/test/java/com/bupt/ta/ai/` and `…/service/ai/`.
Unset global `ARK_*` and `LM_*` environment variables for deterministic test runs.

---

## Testing & CI

```powershell
mvn test                 # Unit + integration tests
mvn clean verify         # Tests + Javadoc → docs/javadoc/
```

GitHub Actions (`.github/workflows/maven.yml`) runs `mvn clean verify` on push/PR to `main`.

Test utilities: `FileTestSupport`, `ServletTestSupport`, `TestFixtures`, `LmTestSupport`.

---

## Documentation

| Document | Description |
|----------|-------------|
| [docs/USER_MANUAL.md](docs/USER_MANUAL.md) | TA, MO, and Admin workflows with screenshots |
| [docs/ProductBacklog_group51.xlsx](docs/ProductBacklog_group51.xlsx) | Product backlog |
| `docs/javadoc/` | Generated Java API reference (`mvn verify`) |

Regenerate manual screenshots: start the app, then run `python tools/capture_manual_screenshots.py` (requires Playwright + Chrome/Edge).

**Coursework PDFs:** [docs/Prototype_group51.pdf](docs/Prototype_group51.pdf), [docs/Report_group51.pdf](docs/Report_group51.pdf)

---

## Team Members

| GitHub Alias | Branch | QMUL ID | Name |
|--------------|--------|---------|------|
| Markrivera683 | Ruiyang_Sun | 231226783 | Sun Ruiyang (孙瑞阳) |
| christine288 | Qixin_Li | 231225373 | Li Qixin (李其馨) |
| Hzwnt | Tianjing_Zhuang | 231225351 | Zhuang Tianjing (庄天婧) |
| S01ZZ | Qinchun_Chen | 231225410 | Chen Qinchun (陈沁纯) |
| g726unknown | Yifeng_Zhang | 231226174 | Zhang Yifeng (张毅峰) |
| negan525 | WeiJia_Xiao | 231226233 | Xiao Weijia (肖炜佳) |

Commits under the name "Chen Qinchun" correspond to GitHub **@S01ZZ** (local Git configuration).

---

## Agile Development

- **Product Backlog:** [docs/ProductBacklog_group51.xlsx](docs/ProductBacklog_group51.xlsx)
- Feature branches per team member → Pull Requests → `main`
- Iterations tracked via GitHub Issues and the backlog spreadsheet
- CI: GitHub Actions runs `mvn clean verify` on push/PR to `main`
