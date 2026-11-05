<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.bupt.ta.model.ApplicantProfile" %>
<%@ page import="com.bupt.ta.model.EducationEntry" %>
<%@ page import="com.bupt.ta.model.User" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.List" %>
<%!
  private static String h(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
  }

  private static String htmlBlock(String s) {
    String safe = h(s).trim();
    if (safe.isEmpty()) return "<span class=\"muted\">Not provided</span>";
    return safe.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "<br/>");
  }

  private static String valueOrMuted(String s) {
    String safe = h(s).trim();
    if (safe.isEmpty()) return "<span class=\"muted\">Not provided</span>";
    return safe;
  }
%>
<%
  ApplicantProfile p = (ApplicantProfile) request.getAttribute("profile");
  User applicant = (User) request.getAttribute("applicantUser");
  @SuppressWarnings("unchecked")
  List<EducationEntry> educationList = (List<EducationEntry>) request.getAttribute("educationList");
  if (p == null) p = new ApplicantProfile();
  if (educationList == null) educationList = java.util.Collections.emptyList();
  String ctx = request.getContextPath();
  String applicantName = applicant != null && applicant.name != null && !applicant.name.trim().isEmpty()
          ? applicant.name : p.fullName;
  boolean hasCv = p.cvFileName != null && !p.cvFileName.trim().isEmpty();
  String cvUserId = applicant != null ? applicant.id : p.userId;
  String encodedUserId = URLEncoder.encode(cvUserId == null ? "" : cvUserId, StandardCharsets.UTF_8);
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Applicant Profile - MO View</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css" />
</head>
<body>
<div class="page--top fade-in">
  <div class="layout-wide">
    <div class="page-header-row">
      <div class="header-left">
        <div class="logo">
          <svg viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </div>
        <div>
          <h1 class="page-title">Applicant Profile</h1>
          <p class="page-subtitle">
            Read-only view for <strong><%= h(applicantName == null ? "Applicant" : applicantName) %></strong>
          </p>
        </div>
      </div>
      <div class="mo-pending-links">
        <a class="link-pill" href="${pageContext.request.contextPath}/mo">
          <svg viewBox="0 0 24 24"><path d="M15 18l-6-6 6-6"/></svg>
          Back to Dashboard
        </a>
        <% if (hasCv) { %>
        <a class="link-pill" href="<%= ctx %>/cv?userId=<%= encodedUserId %>" target="_blank" rel="noopener noreferrer">
          <svg viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
          Open CV
        </a>
        <% } %>
        <a class="link-pill" href="${pageContext.request.contextPath}/logout">
          <svg viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          Logout
        </a>
      </div>
    </div>

    <div class="card mo-section">
      <div class="alert alert-info" style="margin-bottom:1.5rem;">
        This page is view-only for module organisers. Use the CV link when you need the original document.
      </div>

      <div class="mo-profile-grid">
        <div style="display:flex;flex-direction:column;gap:1rem;">
          <h3 class="section-heading">Personal Information</h3>

          <div class="mo-readonly-list">
            <div class="mo-readonly-item">
              <label>Full Name</label>
              <div class="mo-readonly-value"><%= valueOrMuted(p.fullName) %></div>
            </div>

            <div class="grid-2col" style="gap:.75rem 1rem;">
              <div class="mo-readonly-item">
                <label>Gender</label>
                <div class="mo-readonly-value"><%= valueOrMuted(p.gender) %></div>
              </div>
              <div class="mo-readonly-item">
                <label>Degree</label>
                <div class="mo-readonly-value"><%= valueOrMuted(p.degree) %></div>
              </div>
            </div>

            <div class="grid-2col" style="gap:.75rem 1rem;">
              <div class="mo-readonly-item">
                <label>Major</label>
                <div class="mo-readonly-value"><%= valueOrMuted(p.major) %></div>
              </div>
              <div class="mo-readonly-item">
                <label>Student ID</label>
                <div class="mo-readonly-value"><%= valueOrMuted(p.studentId) %></div>
              </div>
            </div>

            <div class="grid-2col" style="gap:.75rem 1rem;">
              <div class="mo-readonly-item">
                <label>Phone</label>
                <div class="mo-readonly-value"><%= valueOrMuted(p.phone) %></div>
              </div>
              <div class="mo-readonly-item">
                <label>Email</label>
                <div class="mo-readonly-value"><%= valueOrMuted(p.email) %></div>
              </div>
            </div>

            <div class="mo-readonly-item">
              <label>National ID</label>
              <div class="mo-readonly-value"><%= valueOrMuted(p.idCard) %></div>
            </div>

            <div class="mo-readonly-item">
              <label>CV</label>
              <div class="mo-readonly-value">
                <% if (hasCv) { %>
                  <a href="<%= ctx %>/cv?userId=<%= encodedUserId %>" target="_blank" rel="noopener noreferrer"><%= h(p.cvFileName) %></a>
                <% } else { %>
                  <span class="muted">No CV on file</span>
                <% } %>
              </div>
            </div>
          </div>
        </div>

        <div style="display:flex;flex-direction:column;gap:1rem;">
          <h3 class="section-heading">Qualifications &amp; Availability</h3>

          <div class="mo-readonly-item">
            <label>Education</label>
            <div class="mo-readonly-value" style="padding:0;">
              <% if (educationList.isEmpty()) { %>
                <div style="padding:.75rem .875rem;"><span class="muted">Not provided</span></div>
              <% } else { %>
                <table class="edu-table">
                  <thead>
                    <tr>
                      <th>School</th>
                      <th>Degree</th>
                      <th>Major</th>
                      <th>Period</th>
                    </tr>
                  </thead>
                  <tbody>
                    <% for (EducationEntry e : educationList) { %>
                    <tr>
                      <td><%= valueOrMuted(e.school) %></td>
                      <td><%= valueOrMuted(e.degree) %></td>
                      <td><%= valueOrMuted(e.major) %></td>
                      <td><%= valueOrMuted(e.period) %></td>
                    </tr>
                    <% } %>
                  </tbody>
                </table>
              <% } %>
            </div>
          </div>

          <div class="mo-readonly-item">
            <label>Courses Completed</label>
            <div class="mo-readonly-value mo-readonly-value--multiline"><%= htmlBlock(p.courses) %></div>
          </div>

          <div class="mo-readonly-item">
            <label>Availability</label>
            <div class="mo-readonly-value mo-readonly-value--multiline"><%= htmlBlock(p.freeTime != null && !p.freeTime.trim().isEmpty() ? p.freeTime : p.availability) %></div>
          </div>

          <div class="mo-readonly-item">
            <label>Skills</label>
            <div class="mo-readonly-value mo-readonly-value--multiline"><%= htmlBlock(p.skills) %></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
</body>
</html>
