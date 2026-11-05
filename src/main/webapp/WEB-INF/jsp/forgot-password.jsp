<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Forgot Password — TA Recruitment</title>
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
      <h1 class="page-title">Forgot Password</h1>
      <p class="page-subtitle">Enter your email to receive a reset link</p>
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
          <h2>Check Your Email</h2>
          <p><%= message %></p>
        </div>
        <a href="${pageContext.request.contextPath}/forgot-password" class="btn-outline" style="margin-bottom:1rem;">Try a different email</a>
        <div class="card-footer" style="text-align:center;">
          <a class="back-link" href="${pageContext.request.contextPath}/login" style="justify-content:center;">
            <svg viewBox="0 0 24 24"><polyline points="15 18 9 12 15 6"/></svg>
            Back to Login
          </a>
        </div>
      <% } else { %>
        <!-- Form state -->
        <div class="icon-circle icon-circle--blue">
          <svg viewBox="0 0 24 24"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
        </div>
        <p style="text-align:center;margin-bottom:1rem;line-height:1.625;font-size:.875rem;color:#64748b;background:#fef3c7;padding:.75rem;border-radius:8px;">
          Password reset sends an email when SMTP is configured (see Docker MailHog in deployment docs). Without SMTP, check server logs for the reset link token.
        </p>
        <p style="text-align:center;margin-bottom:1.5rem;line-height:1.625;font-size:.9375rem;color:#475569;">
          We'll send a password reset link to your registered university email address.
        </p>
        <form class="form" method="post" action="${pageContext.request.contextPath}/forgot-password">
          <input type="hidden" name="csrfToken" value="<%= com.bupt.ta.security.CsrfFilter.csrfToken(request) %>" />
          <div class="field">
            <label for="email">Email Address</label>
            <input id="email" name="email" type="email" placeholder="student@university.edu" required />
          </div>
          <button type="submit" class="btn btn-primary">Send Reset Link</button>
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
