<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.Map" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Register — TA Recruitment</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css" />
</head>
<body>
<%
  @SuppressWarnings("unchecked")
  Map<String, String> fieldErrors = (Map<String, String>) request.getAttribute("fieldErrors");
  if (fieldErrors == null) fieldErrors = new java.util.LinkedHashMap<String, String>();
  String vName = request.getAttribute("vName") != null ? (String) request.getAttribute("vName") : "";
  String vStudentId = request.getAttribute("vStudentId") != null ? (String) request.getAttribute("vStudentId") : "";
  String vEmail = request.getAttribute("vEmail") != null ? (String) request.getAttribute("vEmail") : "";
  String vPhone = request.getAttribute("vPhone") != null ? (String) request.getAttribute("vPhone") : "";
%>
<div class="page fade-in">
  <div class="container">

    <!-- Header -->
    <div class="page-header">
      <div class="logo-wrap">
        <div class="logo">
          <svg viewBox="0 0 24 24"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
        </div>
      </div>
      <h1 class="page-title">Register as TA</h1>
      <p class="page-subtitle">Complete degree and other details in your profile after sign-up.</p>
    </div>

    <!-- Card -->
    <div class="card">
      <% String error = (String) request.getAttribute("error"); if (error != null) { %>
        <div class="alert alert-error"><%= error %></div>
      <% } %>

      <form class="form" method="post" action="${pageContext.request.contextPath}/register" novalidate>
        <input type="hidden" name="csrfToken" value="<%= com.bupt.ta.security.CsrfFilter.csrfToken(request) %>" />
        <div class="field">
          <label for="name">Full Name</label>
          <input id="name" name="name" type="text" required
                 value="<%= vName.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;") %>" />
          <% if (fieldErrors.get("name") != null) { %><div class="field-error"><%= fieldErrors.get("name") %></div><% } %>
        </div>

        <div class="field">
          <label for="studentId">Student ID</label>
          <input id="studentId" name="studentId" type="text" required
                 maxlength="10" inputmode="numeric" autocomplete="off"
                 value="<%= vStudentId.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;") %>" />
          <% if (fieldErrors.get("studentId") != null) { %><div class="field-error"><%= fieldErrors.get("studentId") %></div><% } %>
        </div>

        <div class="field">
          <label for="phone">Phone</label>
          <input id="phone" name="phone" type="text" required
                 value="<%= vPhone.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;") %>" />
          <% if (fieldErrors.get("phone") != null) { %><div class="field-error"><%= fieldErrors.get("phone") %></div><% } %>
        </div>

        <div class="field">
          <label for="email">Email</label>
          <input id="email" name="email" type="email" required
                 value="<%= vEmail.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;") %>" />
          <% if (fieldErrors.get("email") != null) { %><div class="field-error"><%= fieldErrors.get("email") %></div><% } %>
        </div>

        <div class="field">
          <label for="password">Password</label>
          <input id="password" name="password" type="password" required />
          <% if (fieldErrors.get("password") != null) { %><div class="field-error"><%= fieldErrors.get("password") %></div><% } %>
        </div>

        <div class="field">
          <label for="confirm">Confirm Password</label>
          <input id="confirm" name="confirm" type="password" required />
          <% if (fieldErrors.get("confirm") != null) { %><div class="field-error"><%= fieldErrors.get("confirm") %></div><% } %>
        </div>

        <button type="submit" class="btn btn-primary" style="margin-top:.5rem;">Register</button>
      </form>

      <div class="card-footer" style="text-align:center;">
        <a class="back-link" href="${pageContext.request.contextPath}/login" style="justify-content:center;">
          <svg viewBox="0 0 24 24"><polyline points="15 18 9 12 15 6"/></svg>
          Back to login
        </a>
      </div>
    </div>

  </div>
</div>
</body>
</html>
