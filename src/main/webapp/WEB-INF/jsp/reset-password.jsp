<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Reset Password — TA Recruitment</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css" />
</head>
<body>
<div class="page fade-in">
  <div class="container">

    <!-- Header -->
    <div class="page-header">
      <div class="logo-wrap">
        <div class="logo">
          <svg viewBox="0 0 24 24"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
        </div>
      </div>
      <h1 class="page-title">Reset Password</h1>
      <p class="page-subtitle">Create a new password for your account</p>
    </div>

    <!-- Card -->
    <div class="card">
      <% String message = (String) request.getAttribute("message"); %>
      <% if (message != null) { %>
        <!-- Success state -->
        <div class="icon-circle icon-circle--green">
          <svg viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        </div>
        <div class="success-state">
          <h2>Password Updated!</h2>
          <p><%= message %></p>
          <a href="${pageContext.request.contextPath}/login" class="btn btn-primary">Go to Login</a>
        </div>
      <% } else { %>
        <!-- Form state -->
        <div class="icon-circle icon-circle--blue">
          <svg viewBox="0 0 24 24"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
        </div>
        <% String error = (String) request.getAttribute("error"); if (error != null) { %>
          <div class="alert alert-error"><%= error %></div>
        <% } %>
        <form class="form" method="post" action="${pageContext.request.contextPath}/reset-password">
          <input type="hidden" name="csrfToken" value="<%= com.bupt.ta.security.CsrfFilter.csrfToken(request) %>" />
          <% String tokenVal = (String) request.getAttribute("token");
             if (tokenVal != null && !tokenVal.trim().isEmpty()) { %>
          <input type="hidden" name="token" value="<%= tokenVal.replace("&","&amp;").replace("\"","&quot;") %>" />
          <% } %>
          <div class="field">
            <label for="password">New Password</label>
            <input id="password" name="password" type="password" placeholder="Enter new password" required />
          </div>
          <div class="field">
            <label for="confirm">Confirm Password</label>
            <input id="confirm" name="confirm" type="password" placeholder="Re-enter new password" required />
          </div>
          <button type="submit" class="btn btn-primary">Reset Password</button>
        </form>
        <div class="card-footer" style="text-align:center;">
          <a class="back-link" href="${pageContext.request.contextPath}/login" style="justify-content:center;">
            <svg viewBox="0 0 24 24"><polyline points="15 18 9 12 15 6"/></svg>
            Back to Login
          </a>
        </div>
      <% } %>
    </div>

  </div>
</div>
</body>
</html>
