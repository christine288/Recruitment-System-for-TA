<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.bupt.ta.model.ApplicantProfile" %>
<%@ page import="com.bupt.ta.model.User" %>
<%@ page import="com.bupt.ta.model.EducationEntry" %>
<%@ page import="com.bupt.ta.service.ProfileService" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Applicant Profile — TA Recruitment</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css" />
</head>
<body>
<%
  ApplicantProfile p = (ApplicantProfile) request.getAttribute("profile");
  User u = (User) request.getAttribute("user");
  Boolean editableObj = (Boolean) request.getAttribute("editable");
  boolean editable = Boolean.TRUE.equals(editableObj);
  if (p == null) p = new ApplicantProfile();
  @SuppressWarnings("unchecked")
  List<EducationEntry> eduList = (List<EducationEntry>) request.getAttribute("educationList");
  if (eduList == null || eduList.isEmpty()) {
    eduList = new java.util.ArrayList<EducationEntry>();
    eduList.add(new EducationEntry());
  }
  String def = "";
  @SuppressWarnings("unchecked")
  Map<String, String> fieldErrors = (Map<String, String>) request.getAttribute("fieldErrors");
  if (fieldErrors == null) fieldErrors = new java.util.LinkedHashMap<String, String>();
  String degVal = p.degree == null ? "" : p.degree;
  String heroName = (p.fullName != null && !p.fullName.trim().isEmpty()) ? p.fullName.trim()
          : (u != null && u.name != null ? u.name : "Your profile");
  String heroMajor = p.major == null ? "" : p.major.trim();
  String heroStudentId = p.studentId == null ? "" : p.studentId.trim();
  String heroInitials = "?";
  if (!heroName.isEmpty()) {
    String[] nameParts = heroName.split("\\s+");
    if (nameParts.length >= 2) {
      heroInitials = ("" + nameParts[0].charAt(0) + nameParts[nameParts.length - 1].charAt(0)).toUpperCase();
    } else if (nameParts[0].length() >= 2) {
      heroInitials = nameParts[0].substring(0, 2).toUpperCase();
    } else if (nameParts[0].length() == 1) {
      heroInitials = nameParts[0].toUpperCase();
    }
  }
%>
<div class="page--top fade-in">
  <div class="layout-xl profile-page">

    <div class="page-header-row">
      <div class="header-left">
        <div class="logo">
          <svg viewBox="0 0 24 24"><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><rect x="8" y="2" width="8" height="4" rx="1"/><path d="M9 12h6"/><path d="M9 16h4"/><path d="M9 8h6"/></svg>
        </div>
        <div>
          <h1 class="page-title">Applicant Profile</h1>
          <p class="page-subtitle">Your details for TA applications and AI matching</p>
        </div>
      </div>
      <div style="display:flex;gap:.5rem;align-items:center;flex-wrap:wrap;">
        <a class="link-pill" href="${pageContext.request.contextPath}/applications">
          <svg viewBox="0 0 24 24"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>
          My applications
        </a>
        <a class="link-pill" href="${pageContext.request.contextPath}/job">
          <svg viewBox="0 0 24 24"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
          Job List
        </a>
        <% if (!editable) { %>
        <a class="link-pill" href="${pageContext.request.contextPath}/profile?edit=1">
          <svg viewBox="0 0 24 24"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          Edit
        </a>
        <% } %>
        <a class="link-pill" href="${pageContext.request.contextPath}/logout">
          <svg viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          Logout
        </a>
      </div>
    </div>

    <div class="profile-form-shell">

      <div class="profile-hero">
        <div class="profile-hero-avatar profile-hero-initials" aria-hidden="true"><%= heroInitials %></div>
        <div class="profile-hero-meta">
          <div class="profile-hero-name"><%= heroName %></div>
          <p class="profile-hero-sub">Keep this profile complete to apply for jobs and get better AI recommendations.</p>
          <div class="profile-hero-chips">
            <% if (!degVal.isEmpty()) { %>
            <span class="profile-chip profile-chip--degree">
              <svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="6"/><path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11"/></svg>
              <%= degVal %>
            </span>
            <% } %>
            <% if (!heroMajor.isEmpty()) { %>
            <span class="profile-chip profile-chip--major">
              <svg viewBox="0 0 24 24"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
              <%= heroMajor %>
            </span>
            <% } %>
            <% if (!heroStudentId.isEmpty()) { %>
            <span class="profile-chip">
              <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M7 8h10"/><path d="M7 12h6"/></svg>
              <%= heroStudentId %>
            </span>
            <% } %>
          </div>
        </div>
      </div>

      <% if (!editable) { %>
        <div class="alert alert-info">Profile is in view mode. Click <strong>Edit</strong> to update your information.</div>
      <% } %>
      <% String infoMessage = (String) request.getAttribute("infoMessage"); if (infoMessage != null) { %>
        <div class="alert alert-info"><%= infoMessage %></div>
      <% } %>

      <form class="form" method="post"
            action="${pageContext.request.contextPath}/profile"
            enctype="multipart/form-data"
            novalidate
            data-editable="<%= editable ? "1" : "0" %>">
        <input type="hidden" name="csrfToken" value="<%= com.bupt.ta.security.CsrfFilter.csrfToken(request) %>" />

        <div class="profile-form-grid">
          <div class="profile-panel profile-panel--personal">
            <div class="profile-panel-head">
              <div class="profile-panel-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
              </div>
              <div>
                <h2 class="profile-panel-title">Personal Information</h2>
                <p class="profile-panel-desc">Identity and contact details used across your applications.</p>
              </div>
            </div>
            <div class="profile-panel-body">

        <div class="field">
          <label for="fullName">Full Name</label>
          <input id="fullName" name="fullName" type="text"
                 value="<%= p.fullName != null ? p.fullName : def %>" required />
          <% if (fieldErrors.get("fullName") != null) { %><div class="field-error"><%= fieldErrors.get("fullName") %></div><% } %>
        </div>

        <div class="grid-2col" style="gap:.75rem 1rem;">
          <div class="field">
            <label for="gender">Gender</label>
            <select id="gender" name="gender" required>
              <option value="">Select</option>
              <option value="Male"   <%= "Male".equals(p.gender)   ? "selected" : "" %>>Male</option>
              <option value="Female" <%= "Female".equals(p.gender) ? "selected" : "" %>>Female</option>
              <option value="Other"  <%= "Other".equals(p.gender)  ? "selected" : "" %>>Other</option>
            </select>
            <% if (fieldErrors.get("gender") != null) { %><div class="field-error"><%= fieldErrors.get("gender") %></div><% } %>
          </div>
          <div class="field">
            <label for="degree">Degree</label>
            <select id="degree" name="degree" required>
              <option value="">Select</option>
              <option value="Master" <%= "Master".equals(degVal) ? "selected" : "" %>>Master (graduate student)</option>
              <option value="Doctoral" <%= "Doctoral".equals(degVal) ? "selected" : "" %>>Doctoral (PhD student)</option>
            </select>
            <% if (fieldErrors.get("degree") != null) { %><div class="field-error"><%= fieldErrors.get("degree") %></div><% } %>
          </div>
        </div>

        <div class="grid-2col" style="gap:.75rem 1rem;">
          <div class="field">
            <label for="major">Major</label>
            <input id="major" name="major" type="text"
                   value="<%= p.major != null ? p.major : def %>" required />
            <% if (fieldErrors.get("major") != null) { %><div class="field-error"><%= fieldErrors.get("major") %></div><% } %>
          </div>
          <div class="field">
            <label for="studentId">Student ID</label>
            <input id="studentId" name="studentId" type="text"
                   value="<%= p.studentId != null ? p.studentId : def %>" required
                   maxlength="10" inputmode="numeric" autocomplete="off" />
            <% if (fieldErrors.get("studentId") != null) { %><div class="field-error"><%= fieldErrors.get("studentId") %></div><% } %>
          </div>
        </div>

        <div class="grid-2col" style="gap:.75rem 1rem;">
          <div class="field">
            <label for="phone">Phone</label>
            <input id="phone" name="phone" type="text"
                   value="<%= p.phone != null ? p.phone : def %>" required />
            <% if (fieldErrors.get("phone") != null) { %><div class="field-error"><%= fieldErrors.get("phone") %></div><% } %>
          </div>
          <div class="field">
            <label for="email">Email</label>
            <input id="email" name="email" type="email"
                   value="<%= p.email != null ? p.email : def %>" required />
            <% if (fieldErrors.get("email") != null) { %><div class="field-error"><%= fieldErrors.get("email") %></div><% } %>
          </div>
        </div>
        <div class="field">
          <label for="idCard">National ID</label>
          <input id="idCard" name="idCard" type="text"
                 value="<%= p.idCard != null ? p.idCard : def %>" required
                 maxlength="18" inputmode="text" autocomplete="off" />
          <% if (fieldErrors.get("idCard") != null) { %><div class="field-error"><%= fieldErrors.get("idCard") %></div><% } %>
        </div>

        <div class="profile-subsection">
          <div class="profile-subsection-head">
            <svg viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            Curriculum Vitae (CV) <span style="font-weight:400;color:#94a3b8;">(optional)</span>
          </div>
        <%
          boolean hasCv = p.cvFileName != null && !p.cvFileName.isEmpty();
          String cvEsc = hasCv ? p.cvFileName.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") : "";
          String ctx = request.getContextPath();
        %>
        <div class="cv-compact" data-context="<%= ctx %>" data-server-cv="<%= hasCv ? "1" : "0" %>"
             data-placeholder="Choose file..."
             data-server-cv-display="<%= hasCv ? cvEsc : "" %>">
          <% if (editable) { %>
          <input id="cv" name="cv" type="file" accept=".pdf,.doc,.docx" class="cv-file-input" title="" />
          <div class="cv-file-block">
            <label for="cv" class="cv-file-card-label">
              <div class="upload-area cv-file-card" id="cv-file-card">
                <svg class="cv-file-card-icon" viewBox="0 0 24 24" aria-hidden="true">
                  <path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                  <polyline fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" points="14 2 14 8 20 8"/>
                </svg>
                <span id="cv-display-label" class="cv-display-label"><%= hasCv ? cvEsc : "Choose file..." %></span>
              </div>
            </label>
            <div class="cv-icon-row">
              <a id="cv-view-btn" class="cv-icon-btn" target="_blank" rel="noopener noreferrer" title="View" aria-label="View CV"
                 href="<%= hasCv ? ctx + "/cv" : "#" %>"
                 style="<%= hasCv ? "" : "display:none;" %>">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle fill="none" stroke="currentColor" stroke-width="2" cx="12" cy="12" r="3"/></svg>
              </a>
              <button type="button" id="cv-replace-btn" class="cv-icon-btn" title="Replace CV" aria-label="Replace CV">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="M23 4v6h-6M1 20v-6h6M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/></svg>
              </button>
              <% if (hasCv) { %>
              <button type="button" id="cv-delete-btn" class="cv-icon-btn cv-icon-btn--danger" title="Delete CV" aria-label="Delete CV">
                <svg viewBox="0 0 24 24" aria-hidden="true"><polyline fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" points="3 6 5 6 21 6"/><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/><line fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" x1="10" y1="11" x2="10" y2="17"/><line fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" x1="14" y1="11" x2="14" y2="17"/></svg>
              </button>
              <% } %>
            </div>
          </div>
          <% } else if (hasCv) { %>
          <div class="cv-file-block">
            <div class="upload-area cv-file-card cv-file-card--readonly">
              <svg class="cv-file-card-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
                <polyline fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" points="14 2 14 8 20 8"/>
              </svg>
              <span class="cv-display-label"><%= cvEsc %></span>
            </div>
            <div class="cv-icon-row">
              <a class="cv-icon-btn" href="<%= ctx %>/cv" target="_blank" rel="noopener noreferrer" title="View" aria-label="View CV">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle fill="none" stroke="currentColor" stroke-width="2" cx="12" cy="12" r="3"/></svg>
              </a>
            </div>
          </div>
          <% } else { %>
          <p class="cv-display-label" style="margin:0;color:#64748b;font-size:.9rem;">No CV uploaded.</p>
          <% } %>
          <% if (fieldErrors.get("cv") != null) { %><div class="field-error" style="margin-top:.35rem;"><%= fieldErrors.get("cv") %></div><% } %>
        </div>
        </div><!-- /profile-subsection CV -->
            </div><!-- /profile-panel-body -->
          </div><!-- /profile-panel personal -->

          <div class="profile-panel profile-panel--quals">
            <div class="profile-panel-head">
              <div class="profile-panel-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
              </div>
              <div>
                <h2 class="profile-panel-title">Qualifications &amp; Availability</h2>
                <p class="profile-panel-desc">Education, courses, schedule, and skills for matching TA roles.</p>
              </div>
            </div>
            <div class="profile-panel-body">
        <div class="profile-subsection" style="border-top:none;padding-top:0;margin-top:0;">
          <div class="profile-subsection-head">
            <svg viewBox="0 0 24 24"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
            Education background
          </div>
        <table class="edu-table" id="edu-table">
          <thead>
            <tr>
              <th>School</th>
              <th>Degree</th>
              <th>Major</th>
              <th>Period</th>
              <th></th>
            </tr>
          </thead>
          <tbody id="edu-body">
            <% for (int i = 0; i < eduList.size(); i++) { EducationEntry e = eduList.get(i); %>
            <tr>
              <td>
                <input name="edu_school" type="text"
                       value="<%= e.school != null ? e.school : def %>" required />
                <% if (fieldErrors.get("edu_school_" + i) != null) { %><div class="field-error"><%= fieldErrors.get("edu_school_" + i) %></div><% } %>
              </td>
              <td>
                <input name="edu_degree" type="text"
                       value="<%= e.degree != null ? e.degree : def %>" required />
                <% if (fieldErrors.get("edu_degree_" + i) != null) { %><div class="field-error"><%= fieldErrors.get("edu_degree_" + i) %></div><% } %>
              </td>
              <td>
                <input name="edu_major"  type="text"
                       value="<%= e.major  != null ? e.major  : def %>" required />
                <% if (fieldErrors.get("edu_major_" + i) != null) { %><div class="field-error"><%= fieldErrors.get("edu_major_" + i) %></div><% } %>
              </td>
              <td>
                <input name="edu_period" type="text"
                       value="<%= e.period != null ? e.period : def %>" required />
                <% if (fieldErrors.get("edu_period_" + i) != null) { %><div class="field-error"><%= fieldErrors.get("edu_period_" + i) %></div><% } %>
              </td>
              <td style="text-align:center;vertical-align:middle;">
                <button type="button" onclick="removeEduRow(this)"
                        class="btn-withdraw"
                        title="Remove row">&times;</button>
              </td>
            </tr>
            <% } %>
          </tbody>
        </table>
        <% if (fieldErrors.get("education") != null) { %><div class="field-error"><%= fieldErrors.get("education") %></div><% } %>
        <button id="add-edu-btn" type="button" onclick="addEduRow()" class="profile-add-row">
          <svg viewBox="0 0 24 24"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          Add education row
        </button>
        </div>

        <div class="profile-subsection profile-subsection--courses">
          <div class="profile-subsection-head">
            <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4"/><path d="M8 2v4"/><path d="M3 10h18"/></svg>
            Courses &amp; Availability
          </div>

        <div class="field">
          <label for="courses">Courses Completed <span style="font-weight:400;color:#94a3b8;">(one per line)</span></label>
          <textarea id="courses" name="courses" placeholder="e.g. Data Structures"
                    required><%= p.courses != null ? p.courses : def %></textarea>
          <% if (fieldErrors.get("courses") != null) { %><div class="field-error"><%= fieldErrors.get("courses") %></div><% } %>
        </div>

        <div class="field field--availability">
          <label for="freeTime">Availability <span style="font-weight:400;color:#94a3b8;">(when you can work)</span></label>
          <textarea id="freeTime" name="freeTime"
                    placeholder="e.g. Mon 14:00-17:00"
                    required><%= p.freeTime != null ? p.freeTime : (p.availability != null ? p.availability : def) %></textarea>
          <% if (fieldErrors.get("freeTime") != null) { %><div class="field-error"><%= fieldErrors.get("freeTime") %></div><% } %>
        </div>
        </div>

        <div class="profile-subsection">
          <div class="profile-subsection-head">
            <svg viewBox="0 0 24 24"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            Skills
          </div>
        <div class="field">
          <label for="skills">Skills <span style="font-weight:400;color:#94a3b8;">(one per line or comma-separated)</span></label>
          <textarea id="skills" name="skills" placeholder="e.g. Python, public speaking"
                    required><%= p.skills != null ? p.skills : def %></textarea>
          <% if (fieldErrors.get("skills") != null) { %><div class="field-error"><%= fieldErrors.get("skills") %></div><% } %>
        </div>
        </div>

        <% if (editable) { %>
        <div class="save-bar">
          <button type="submit" class="btn btn-primary profile-save-btn">
            <svg viewBox="0 0 24 24"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
            Save Profile
          </button>
        </div>
        <% } %>

            </div><!-- /profile-panel-body -->
          </div><!-- /profile-panel quals -->
        </div><!-- /profile-form-grid -->
      </form>
      <form id="form-delete-cv" method="post" action="${pageContext.request.contextPath}/profile" class="profile-hidden-form" aria-hidden="true">
        <input type="hidden" name="csrfToken" value="<%= com.bupt.ta.security.CsrfFilter.csrfToken(request) %>" />
        <input type="hidden" name="action" value="deleteCvOnly" />
      </form>
      <div id="cv-delete-dialog" class="app-dialog-backdrop" role="presentation" aria-hidden="true">
        <div class="app-dialog" role="dialog" aria-modal="true" aria-labelledby="cv-delete-title" aria-describedby="cv-delete-copy">
          <h2 id="cv-delete-title">Delete CV?</h2>
          <p id="cv-delete-copy">This will remove your saved CV from the system. This action cannot be undone.</p>
          <div class="app-dialog-actions">
            <button type="button" class="btn-ghost" id="cv-delete-cancel">Cancel</button>
            <button type="button" class="btn btn-primary btn-danger" id="cv-delete-confirm">Delete CV</button>
          </div>
        </div>
      </div>
    </div><!-- /profile-form-shell -->
  </div><!-- /layout-xl -->
</div>

<script>
function addEduRow() {
  var tbody = document.getElementById('edu-body');
  var tr = document.createElement('tr');
  tr.innerHTML =
    '<td><input name="edu_school" type="text" required /></td>' +
    '<td><input name="edu_degree" type="text" required /></td>' +
    '<td><input name="edu_major"  type="text" required /></td>' +
    '<td><input name="edu_period" type="text" required /></td>' +
    '<td style="text-align:center;vertical-align:middle;">' +
    '<button type="button" onclick="removeEduRow(this)" ' +
    'class="btn-withdraw" ' +
    'title="Remove row">&times;</button></td>';
  tbody.appendChild(tr);
}
function removeEduRow(btn) {
  var tbody = document.getElementById('edu-body');
  if (tbody.rows.length > 1) {
    btn.closest('tr').remove();
  }
}

(function () {
  var form = document.querySelector('form[data-editable]');
  if (!form || form.getAttribute('data-editable') === '1') return;
  form.querySelectorAll('input, textarea, select, button').forEach(function (el) {
    var isAllowedButton = el.id === 'add-edu-btn';
    if (isAllowedButton) {
      el.style.display = 'none';
      return;
    }
    if (el.type === 'button') {
      if (el.classList.contains('cv-icon-btn--danger')) {
        return;
      }
      el.style.display = 'none';
      return;
    }
    if (el.type === 'file') {
      el.disabled = true;
      return;
    }
    if (el.type === 'checkbox') {
      el.disabled = true;
      return;
    }
    el.setAttribute('disabled', 'disabled');
  });
})();

(function () {
  var cv = document.getElementById('cv');
  var label = document.getElementById('cv-display-label');
  var viewBtn = document.getElementById('cv-view-btn');
  var replaceBtn = document.getElementById('cv-replace-btn');
  var compact = document.querySelector('.cv-compact');
  if (!cv || !label || !compact) return;
  var contextPath = compact.getAttribute('data-context') || '';
  var hasServerCv = compact.getAttribute('data-server-cv') === '1';
  var placeholder = compact.getAttribute('data-placeholder') || 'Choose file...';
  var serverDisplay = compact.getAttribute('data-server-cv-display') || '';
  var blobUrl = null;

  function revokeBlob() {
    if (blobUrl) {
      URL.revokeObjectURL(blobUrl);
      blobUrl = null;
    }
  }

  function syncViewLink(file) {
    if (!viewBtn) return;
    revokeBlob();
    if (file) {
      blobUrl = URL.createObjectURL(file);
      viewBtn.setAttribute('href', blobUrl);
      viewBtn.style.display = 'inline-flex';
      return;
    }
    if (hasServerCv && contextPath) {
      viewBtn.setAttribute('href', contextPath + '/cv');
      viewBtn.style.display = 'inline-flex';
    } else {
      viewBtn.setAttribute('href', '#');
      viewBtn.style.display = 'none';
    }
  }

  /** Keep label + preview in sync with input; if picker ends with no file, fall back to server CV name / placeholder. */
  function refreshCvUiFromInput() {
    var f = cv.files && cv.files[0];
    if (f) {
      label.textContent = f.name;
      syncViewLink(f);
      return;
    }
    label.textContent = serverDisplay ? serverDisplay : placeholder;
    syncViewLink(null);
  }

  cv.addEventListener('change', refreshCvUiFromInput);
  cv.addEventListener('cancel', refreshCvUiFromInput);
  if (replaceBtn) {
    replaceBtn.addEventListener('click', function () {
      cv.value = '';
      refreshCvUiFromInput();
      cv.click();
    });
  }

  refreshCvUiFromInput();
})();

(function () {
  var trigger = document.getElementById('cv-delete-btn');
  var dialog = document.getElementById('cv-delete-dialog');
  var cancel = document.getElementById('cv-delete-cancel');
  var confirmBtn = document.getElementById('cv-delete-confirm');
  var form = document.getElementById('form-delete-cv');
  if (!trigger || !dialog || !cancel || !confirmBtn || !form) return;

  function openDialog() {
    dialog.classList.add('is-open');
    dialog.setAttribute('aria-hidden', 'false');
    cancel.focus();
  }

  function closeDialog() {
    dialog.classList.remove('is-open');
    dialog.setAttribute('aria-hidden', 'true');
    trigger.focus();
  }

  trigger.addEventListener('click', openDialog);
  cancel.addEventListener('click', closeDialog);
  confirmBtn.addEventListener('click', function () {
    form.submit();
  });
  dialog.addEventListener('click', function (event) {
    if (event.target === dialog) {
      closeDialog();
    }
  });
  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape' && dialog.classList.contains('is-open')) {
      closeDialog();
    }
  });
})();
</script>
</body>
</html>
