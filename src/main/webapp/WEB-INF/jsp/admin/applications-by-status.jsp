<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.bupt.ta.model.User" %>
<%@ page import="com.bupt.ta.model.Application" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%
  @SuppressWarnings("unchecked")
  List<Application> apps = (List<Application>) request.getAttribute("applications");
  if (apps == null) apps = java.util.Collections.emptyList();
  @SuppressWarnings("unchecked")
  Map<String, User> userById = (Map<String, User>) request.getAttribute("userById");
  if (userById == null) userById = java.util.Collections.emptyMap();
  String pageTitle = (String) request.getAttribute("pageTitle");
  if (pageTitle == null || pageTitle.isEmpty()) pageTitle = "Applications";
  String statusBucket = (String) request.getAttribute("statusBucket");
  if (statusBucket == null) statusBucket = "";
  String listMessage = (String) request.getAttribute("listMessage");

  User admin = (User) session.getAttribute("user");
  String adminName = admin != null && admin.name != null && !admin.name.isEmpty() ? admin.name : "Admin";
  String adminEmail = admin != null && admin.email != null ? admin.email : "";
  String avatarLetter = "?";
  if (admin != null) {
    if (admin.name != null && !admin.name.trim().isEmpty()) {
      avatarLetter = admin.name.trim().substring(0, 1).toUpperCase();
    } else if (admin.email != null && !admin.email.isEmpty()) {
      avatarLetter = admin.email.substring(0, 1).toUpperCase();
    }
  }

  String ctx = request.getContextPath();
  String csrfToken = com.bupt.ta.security.CsrfFilter.csrfToken(request);
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title><%= pageTitle %> - Admin</title>
  <link rel="stylesheet" href="<%= ctx %>/static/css/admin-dashboard.css" />
</head>
<body>
  <header class="header">
    <div class="header-row">
      <div>
        <h1 class="title">TA Recruitment System</h1>
        <p class="subtitle"><%= pageTitle %></p>
        <div class="header-actions">
          <a href="<%= ctx %>/admin">Back to dashboard</a>
          <span class="header-sep">|</span>
          <a href="<%= ctx %>/logout">Logout</a>
        </div>
      </div>
      <div class="profile">
        <div class="header-user">
          <p class="header-user-name"><%= adminName %></p>
          <p class="header-user-email"><%= adminEmail %></p>
        </div>
        <div class="avatar" aria-hidden="true"><%= avatarLetter %></div>
      </div>
    </div>
  </header>

  <main class="container">
    <section class="card">
      <div class="card-header">
        <h2 class="card-title"><%= pageTitle %></h2>
        <a class="button button-outline" href="<%= ctx %>/admin">Dashboard</a>
      </div>
      <div class="card-content">
        <% if (listMessage != null && !listMessage.trim().isEmpty()) { %>
        <p class="admin-notice" style="margin-top:0"><%= listMessage.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") %></p>
        <% } %>
        <p class="tooltip-small" style="margin-top:0">Status filter: <strong><%= statusBucket %></strong>. Total: <%= apps.size() %>. Admin actions override MO decisions (same as updating <code>applications.json</code>).</p>
        <% if (apps.isEmpty()) { %>
          <p class="tooltip-small">No applications in this category.</p>
        <% } else { %>
        <table class="table">
          <thead>
            <tr>
              <th>Application ID</th>
              <th>Position</th>
              <th>Applicant</th>
              <th>Date</th>
              <th>Role</th>
              <th>Feedback</th>
              <th>Admin actions</th>
            </tr>
          </thead>
          <tbody>
            <% for (Application a : apps) {
                 String aid = a.id == null ? "" : a.id;
                 String mn = a.moduleName == null ? "" : a.moduleName;
                 String mc = a.moduleCode == null ? "" : a.moduleCode;
                 String pos = mn.isEmpty() && mc.isEmpty() ? "—" : (mc.isEmpty() ? mn : (mn.isEmpty() ? mc : mn + " (" + mc + ")"));
                 String uid = a.userId == null ? "" : a.userId.trim();
                 User ap = userById.get(uid);
                 String applicant = "—";
                 if (ap != null) {
                   if (ap.name != null && !ap.name.trim().isEmpty()) applicant = ap.name.trim();
                   else if (ap.email != null && !ap.email.trim().isEmpty()) applicant = ap.email.trim();
                   else applicant = uid;
                 } else if (!uid.isEmpty()) {
                   applicant = uid;
                 }
                 String dt = a.applicationDate == null || a.applicationDate.isEmpty() ? "—" : a.applicationDate;
                 String role = a.role == null || a.role.isEmpty() ? "—" : a.role;
                 String fb = a.feedback == null || a.feedback.trim().isEmpty() ? "—" : a.feedback;
                 String safePos = pos.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
                 String safeApp = applicant.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
                 String safeFb = fb.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
            %>
            <tr>
              <td class="tooltip-small"><%= aid %></td>
              <td><%= safePos %></td>
              <td><%= safeApp %></td>
              <td><%= dt %></td>
              <td><%= role.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;") %></td>
              <td class="tooltip-small"><%= safeFb %></td>
              <td class="admin-app-actions-cell">
                <div class="admin-app-actions">
                  <% if ("pending".equals(statusBucket)) { %>
                  <form method="post" action="<%= ctx %>/admin/applications" class="inline-form admin-app-action-form" onsubmit="return confirm('Force accept this application (override MO)?');">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="forceAccept" />
                    <input type="hidden" name="appId" value="<%= aid %>" />
                    <input type="hidden" name="returnStatus" value="<%= statusBucket %>" />
                    <input type="text" name="feedback" class="admin-app-feedback" placeholder="Optional note" aria-label="Feedback for accept" />
                    <button type="submit" class="button button-success">Force accept</button>
                  </form>
                  <form method="post" action="<%= ctx %>/admin/applications" class="inline-form admin-app-action-form" onsubmit="return confirm('Force reject this application (override MO)?');">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="forceReject" />
                    <input type="hidden" name="appId" value="<%= aid %>" />
                    <input type="hidden" name="returnStatus" value="<%= statusBucket %>" />
                    <input type="text" name="feedback" class="admin-app-feedback" placeholder="Optional note" aria-label="Feedback for reject" />
                    <button type="submit" class="button button-danger">Force reject</button>
                  </form>
                  <% } else if ("accepted".equals(statusBucket)) { %>
                  <form method="post" action="<%= ctx %>/admin/applications" class="inline-form admin-app-action-form" onsubmit="return confirm('Force reject and move this application back from accepted?');">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="forceReject" />
                    <input type="hidden" name="appId" value="<%= aid %>" />
                    <input type="hidden" name="returnStatus" value="<%= statusBucket %>" />
                    <input type="text" name="feedback" class="admin-app-feedback" placeholder="Optional note" aria-label="Feedback for reject" />
                    <button type="submit" class="button button-danger">Force reject</button>
                  </form>
                  <form method="post" action="<%= ctx %>/admin/applications" class="inline-form admin-app-action-form" onsubmit="return confirm('Move this application back to the pending queue (MO can review again)?');">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="forcePend" />
                    <input type="hidden" name="appId" value="<%= aid %>" />
                    <input type="hidden" name="returnStatus" value="<%= statusBucket %>" />
                    <input type="text" name="feedback" class="admin-app-feedback" placeholder="Optional note" aria-label="Feedback for pending" />
                    <button type="submit" class="button button-outline">Force pend</button>
                  </form>
                  <% } else if ("rejected".equals(statusBucket)) { %>
                  <form method="post" action="<%= ctx %>/admin/applications" class="inline-form admin-app-action-form" onsubmit="return confirm('Force accept this application again (from rejected)?');">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="forceAccept" />
                    <input type="hidden" name="appId" value="<%= aid %>" />
                    <input type="hidden" name="returnStatus" value="<%= statusBucket %>" />
                    <input type="text" name="feedback" class="admin-app-feedback" placeholder="Optional note" aria-label="Feedback for accept" />
                    <button type="submit" class="button button-success">Force accept</button>
                  </form>
                  <form method="post" action="<%= ctx %>/admin/applications" class="inline-form admin-app-action-form" onsubmit="return confirm('Move this application back to the pending queue (MO can review again)?');">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="forcePend" />
                    <input type="hidden" name="appId" value="<%= aid %>" />
                    <input type="hidden" name="returnStatus" value="<%= statusBucket %>" />
                    <input type="text" name="feedback" class="admin-app-feedback" placeholder="Optional note" aria-label="Feedback for pending" />
                    <button type="submit" class="button button-outline">Force pend</button>
                  </form>
                  <% } %>
                </div>
              </td>
            </tr>
            <% } %>
          </tbody>
        </table>
        <% } %>
      </div>
    </section>
  </main>
</body>
</html>
