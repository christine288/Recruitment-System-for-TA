# TA Recruitment System — User Manual

**Project:** EBU6304 Software Engineering — Group 51  
**Application:** BUPT International School Teaching Assistant Recruitment System  
**Version:** 1.0.0  
**Last updated:** May 2026

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Before You Start](#2-before-you-start)
3. [Demo Accounts](#3-demo-accounts)
4. [TA Applicant Guide](#4-ta-applicant-guide)
5. [Module Organiser Guide](#5-module-organiser-guide)
6. [Administrator Guide](#6-administrator-guide)
7. [Profile Validation Rules](#7-profile-validation-rules)
8. [Troubleshooting](#8-troubleshooting)
9. [Screenshot Index](#9-screenshot-index)

---

## 1. Introduction

The **TA Recruitment System** replaces email-and-spreadsheet workflows with a lightweight web application. Three roles use the system:

| Role | Purpose |
|------|---------|
| **TA Applicant** | Register, maintain profile and CV, browse jobs, apply, and track status |
| **Module Organiser (MO)** | Publish TA vacancies, review applications, approve or reject |
| **Administrator** | Monitor users, jobs, applications, profiles, workload, and exports |

The user interface is **English**. Data is stored in JSON files under `WEB-INF/data/` (no database).

---

## 2. Before You Start

### Run locally

From the project root:

```powershell
mvn tomcat7:run-war
```

Open: **http://localhost:18080/ta-recruitment/login**

Default context path: `/ta-recruitment`

### Requirements

| Item | Version |
|------|---------|
| JDK | 11+ |
| Maven | 3.9+ |
| Browser | Chrome, Edge, or Firefox (modern) |

---

## 3. Demo Accounts

Seed accounts in `users.json` (coursework prototype — plain-text passwords):

| Role | Email | Password |
|------|-------|----------|
| Administrator | `admin@bupt.local` | `admin123` |
| Module Organiser | `mo@bupt.local` | `mo123` |
| TA Applicant | `alice.chen@bupt.local` | `ta123` |
| TA Applicant | `brian.li@bupt.local` | `ta123` |

> **Security note:** Passwords are stored in plain text for coursework only. Do not use in production.

The login page also lists demo accounts in a collapsible **Demo accounts (coursework)** panel.

![Login page](manual-screenshots/01-login.png)

*Figure 1 — Login page*

---

## 4. TA Applicant Guide

### 4.1 Register a new account

1. From the login page, click **Register here**.
2. Fill in name, student ID, email, and password.
3. Submit to create a **TA** account.
4. Sign in with the new credentials.

![Register page](manual-screenshots/02-register.png)

*Figure 2 — Registration*

---

### 4.2 Complete your profile

After first login, open **Profile** from the navigation or go to `/profile`.

#### View mode vs Edit mode

| Mode | When | What you see |
|------|------|--------------|
| **Edit** | Profile is incomplete, or you clicked **Edit** | Fields are editable; **Save Profile** appears at the bottom |
| **View** | Profile is complete and saved | Fields are read-only; **Edit** button appears top-right (left of Logout) |

**Profile fields (required unless noted):**

- Personal: full name, gender, degree (Master or Doctoral only), major, student ID, national ID, phone, email
- Education: at least one row (school, degree, major, period)
- Courses completed, availability, skills
- **CV (optional):** PDF, DOC, or DOCX, max 10 MB

![Profile — view mode](manual-screenshots/03-profile-view.png)

*Figure 3 — Applicant profile (view mode)*

![Profile — edit mode](manual-screenshots/04-profile-edit.png)

*Figure 4 — Applicant profile (edit mode)*

**Tips**

- Click **Edit** (top-right) to change a completed profile.
- After **Save Profile**, the page returns to view mode.
- CV can be uploaded, viewed (`/cv`), replaced, or deleted in edit mode.
- You can apply for jobs **without** uploading a CV, but other profile fields must be complete.

---

### 4.3 Browse jobs

Go to **Job List** (`/job`) to see published vacancies.

- Use search and filters on the list page.
- Click a job card to open details.

![Job list](manual-screenshots/05-job-list.png)

*Figure 5 — TA job list*

![Job detail](manual-screenshots/06-job-detail.png)

*Figure 6 — Job detail with Apply action*

On the detail page you may see **AI-assisted** hints (job recommendations, skill match, missing skills) when LM integration is enabled.

---

### 4.4 Submit an application

1. Open a published job detail page.
2. Click **Apply** (your profile must be complete).
3. Confirm module name, module code, and role if prompted.
4. You cannot submit duplicate active applications for the same module.

If your profile is incomplete, you are redirected to **Profile** with a message.

---

### 4.5 Track application status

Open **My applications** (`/applications`).

Filter by: **All**, **Pending**, **Accepted**, **Rejected**, or **Withdrawn**.

You may **withdraw** a pending application from this page.

![Application status](manual-screenshots/07-applications.png)

*Figure 7 — My applications*

| Status | Meaning |
|--------|---------|
| Pending | Awaiting MO review |
| Accepted | Approved |
| Rejected | Not selected |
| Withdrawn | Cancelled by the applicant |

---

## 5. Module Organiser Guide

Sign in as `mo@bupt.local` / `mo123`, then open **MO Dashboard** (`/mo`).

### 5.1 Manage job postings

From the dashboard you can:

- **Create** a new job (module name, code, description, skills, deadline, TA count, schedule)
- **Publish** draft jobs
- **Edit** open jobs
- **Close** jobs that no longer accept applications

Each job is linked to the MO via `createdByMoId`.

![MO dashboard](manual-screenshots/08-mo-dashboard.png)

*Figure 8 — Module Organiser dashboard*

---

### 5.2 Review applications

The dashboard lists applications for **your** jobs only (matched by `moduleName` + `moduleCode`).

For each pending application you can:

| Action | Description |
|--------|-------------|
| **View Profile** | Open a read-only applicant profile |
| **Open CV** | Download or view the applicant CV (if uploaded) |
| **Approve / Reject** | Update status with optional feedback |
| **AI review aids** | Skill match and decision hints (when enabled) |

![MO applicant profile](manual-screenshots/09-mo-applicant-profile.png)

*Figure 9 — MO view of applicant profile*

**How applicants link to jobs**

- `applications.json` stores `userId` (who applied) plus `moduleName` and `moduleCode` (which job).
- The MO dashboard shows applications whose module pair matches one of the MO’s published jobs.
- Profile details are loaded separately via `userId`.

---

## 6. Administrator Guide

Sign in as `admin@bupt.local` / `admin123`.

### 6.1 Admin dashboard

`/admin` shows system-wide metrics: users, jobs, applications, workload overview, analytics charts, and management tabs.

![Admin dashboard](manual-screenshots/10-admin-dashboard.png)

*Figure 10 — Administrator dashboard (overview)*

---

### 6.2 TA profiles

`/admin/ta-profiles` lists all applicant profiles with search and detail view. Admins can open CVs via `/admin/cv`.

![Admin TA profiles](manual-screenshots/11-admin-ta-profiles.png)

*Figure 11 — Admin TA profiles listing*

---

### 6.3 User management

User management is on the **Admin dashboard** under the **Management** section → **User Management** tab.

> **Note:** `/admin/users` accepts **POST** requests only (create, activate, deactivate, delete). Opening that URL directly in the browser returns HTTP 405. Use `/admin` and the User Management tab instead.

From this tab you can:

- **Create** TA or MO accounts (name, email, password, role)
- **Activate** or **Deactivate** existing users
- **Delete** users (removes related profile, application, and CV data per servlet rules)

![Admin user management](manual-screenshots/12-admin-users.png)

*Figure 12 — Admin user management (Management tab on `/admin`)*

---

### 6.4 Other admin functions

| URL | Function |
|-----|----------|
| `/admin/job-view?id=` | View job details |
| `/admin/applications/by-status?status=` | Filter applications by status |
| `/admin/applications` | Override application status (POST) |
| `/admin/export` | Export data |
| `/admin/ai-demo` | AI feature demo (Doubao via Volcengine Ark when configured) |

Job deletion is handled via POST to `/admin/jobs` from the **Job Management** tab on the admin dashboard.

---

## 7. Profile Validation Rules

| Field | Required | Rule |
|-------|----------|------|
| Full name | Yes | 2–60 letters |
| Gender | Yes | Male, Female, or Other |
| Degree | Yes | Master or Doctoral only |
| Student ID | Yes | 10 digits; first 4 digits = admission year |
| National ID | Yes | 18-digit PRC ID with valid checksum |
| Phone | Yes | China mobile number (+86) |
| Email | Yes | Valid domain; must be unique |
| Education | Yes | At least one complete row |
| Courses, availability, skills | Yes | Non-empty |
| CV | **No** | PDF, DOC, or DOCX if uploaded |

Validation is implemented in `ApplicantFieldValidation.java` and `ProfileServlet.validateProfileInput()`.

---

## 8. Troubleshooting

| Problem | Solution |
|---------|----------|
| Cannot log in | Check email and password; account must be **active** |
| Redirected to Profile when applying | Complete all required profile fields |
| No Edit button on Profile | Profile is still incomplete — fill required fields and Save |
| CV upload fails | Use PDF, DOC, or DOCX; maximum 10 MB |
| MO cannot see an application | Job module name/code must match the application record |
| HTTP 405 on `/admin/users` | Expected — use `/admin` → User Management tab (POST-only endpoint) |
| HTTP 403 on form submit | Session expired — refresh the page and retry (CSRF token) |

---

## 9. Screenshot Index

All screenshots are stored in `docs/manual-screenshots/`.

| File | Screen |
|------|--------|
| `01-login.png` | Login |
| `02-register.png` | Registration |
| `03-profile-view.png` | Profile (view mode) |
| `04-profile-edit.png` | Profile (edit mode) |
| `05-job-list.png` | Job list |
| `06-job-detail.png` | Job detail |
| `07-applications.png` | My applications |
| `08-mo-dashboard.png` | MO dashboard |
| `09-mo-applicant-profile.png` | MO applicant profile |
| `10-admin-dashboard.png` | Admin dashboard (overview) |
| `11-admin-ta-profiles.png` | Admin TA profiles |
| `12-admin-users.png` | Admin user management tab |

### Regenerate screenshots

1. Start the app: `mvn tomcat7:run-war`
2. Run: `python tools/capture_manual_screenshots.py`
3. Requires: `pip install playwright` and Chrome or Edge installed

---

## Document History

| Date | Change |
|------|--------|
| May 2026 | Initial user manual with 12 main-frame screenshots |
| May 2026 | English-only revision; fixed Figure 12 (admin user management tab) |

---

*End of User Manual*
