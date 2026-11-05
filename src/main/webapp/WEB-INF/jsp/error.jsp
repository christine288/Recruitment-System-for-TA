<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Error — TA Recruitment</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css" />
</head>
<body>
<div class="page fade-in">
  <div class="container">
    <div class="card" role="alert">
      <h1 class="page-title" style="font-size:1.5rem;margin-bottom:.5rem;">
        <% Integer code = (Integer) request.getAttribute("javax.servlet.error.status_code");
           if (code == null) code = 500; %>
        Error <%= code %>
      </h1>
      <p style="color:#475569;margin-bottom:1.5rem;">
        <% if (code == 403) { %>
          You do not have permission to view this page, or your session may have expired.
        <% } else if (code == 404) { %>
          The page you requested was not found.
        <% } else { %>
          Something went wrong. Please try again or contact support.
        <% } %>
      </p>
      <a class="btn btn-primary" href="${pageContext.request.contextPath}/login">Back to Login</a>
    </div>
  </div>
</div>
</body>
</html>
