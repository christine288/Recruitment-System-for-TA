<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.bupt.ta.service.ai.AiFeatureOutput" %>
<%
  String ctx = request.getContextPath();
  String csrfToken = com.bupt.ta.security.CsrfFilter.csrfToken(request);
  Boolean lmEnabled = (Boolean) request.getAttribute("lmEnabled");
  String lmProvider = (String) request.getAttribute("lmProvider");
  String lmModel = (String) request.getAttribute("lmModel");
  AiFeatureOutput result = (AiFeatureOutput) request.getAttribute("result");
  String lastAction = (String) request.getAttribute("lastAction");
  if (lmEnabled == null) lmEnabled = Boolean.TRUE;
  if (lmProvider == null) lmProvider = "doubao";
  boolean isMockProvider = "mock".equalsIgnoreCase(lmProvider);
  boolean isDoubaoProvider = "doubao".equalsIgnoreCase(lmProvider);
  String pageTitle = isMockProvider ? "AI Demo (Mock) — Admin" : "AI Demo — Admin";
  String subtitle = isMockProvider ? "AI Demo — Offline Mock"
      : isDoubaoProvider ? "AI Demo — Doubao via Volcengine Ark" : "AI Demo — Configured Provider";
%>
<%!
  private static String esc(String s) {
    if (s == null) return "";
    StringBuilder sb = new StringBuilder(s.length() + 8);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '&': sb.append("&amp;"); break;
        case '<': sb.append("&lt;"); break;
        case '>': sb.append("&gt;"); break;
        case '"': sb.append("&quot;"); break;
        default: sb.append(c);
      }
    }
    return sb.toString();
  }
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title><%= pageTitle %></title>
  <link rel="stylesheet" href="<%= ctx %>/static/css/admin-dashboard.css" />
  <style>
    .ai-banner { font-size:0.75rem; color:#92400e; background:#fffbeb; border:1px solid #fcd34d; border-radius:0.5rem; padding:0.75rem 1rem; margin-bottom:1rem; }
    .ai-grid { display:grid; gap:1rem; }
    @media (min-width:900px) { .ai-grid { grid-template-columns: repeat(2, minmax(0,1fr)); } }
    .ai-pre { white-space:pre-wrap; font-family: ui-monospace, Consolas, monospace; font-size:0.8rem; background:#f8fafc; border:1px solid #e5e7eb; border-radius:0.5rem; padding:0.75rem; }
    .ai-error { color:#991b1b; font-size:0.875rem; }
    textarea.ai-input { width:100%; min-height:5rem; font-family: inherit; font-size:0.875rem; padding:0.5rem; border:1px solid #e5e7eb; border-radius:0.5rem; }
    .result-toolbar { display:flex; gap:0.5rem; margin:0.5rem 0 0.75rem; }
    .result-tab { border:1px solid #d1d5db; background:#fff; color:#374151; border-radius:9999px; padding:0.25rem 0.75rem; cursor:pointer; font-size:0.75rem; }
    .result-tab.active { background:#2563eb; color:#fff; border-color:#2563eb; }
    .result-panel { display:none; }
    .result-panel.active { display:block; }
    .md-box { font-size:0.875rem; color:#111827; background:#f8fafc; border:1px solid #e5e7eb; border-radius:0.5rem; padding:0.75rem; line-height:1.55; }
    .md-box h2, .md-box h3 { margin:0.8rem 0 0.4rem; font-size:1rem; }
    .md-box p { margin:0.45rem 0; }
    .md-box ul { margin:0.35rem 0 0.5rem 1.2rem; padding:0; }
    .md-box code { font-family: ui-monospace, Consolas, monospace; background:#eef2ff; padding:0.08rem 0.3rem; border-radius:0.25rem; }
    .md-box a { color:#2563eb; text-decoration:none; }
    .md-box a:hover { text-decoration:underline; }
  </style>
</head>
<body>
  <header class="header">
    <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:1rem;">
      <div>
        <h1 class="title">TA Recruitment System</h1>
        <p class="subtitle"><%= subtitle %></p>
        <div class="header-actions">
          <a href="<%= ctx %>/admin">Admin Dashboard</a>
          <span style="color:#d1d5db">|</span>
          <a href="<%= ctx %>/logout">Logout</a>
        </div>
      </div>
    </div>
  </header>

  <main class="container">
    <div class="ai-banner">
      <% if (isMockProvider) { %>
      <strong>Offline Mock</strong> — This mode returns deterministic demo content. Set <code>LM_PROVIDER=doubao</code> and configure Ark credentials to use the real model.
      <% } else if (isDoubaoProvider) { %>
      <strong>Doubao via Volcengine Ark</strong> — AI requests use the server-side Ark API. Configure <code>ARK_API_KEY</code> and <code>ARK_MODEL</code> (or their <code>LM_*</code> aliases) on the server. Missing configuration is reported as an error.
      <% } else { %>
      <strong>Configured Provider</strong> — AI requests use your configured HTTP provider. Ensure its URL, API key, and model are set correctly.
      <% } %>
      Configuration only: LM_ENABLED=<%= esc(String.valueOf(lmEnabled)) %>, LM_PROVIDER=<code><%= esc(lmProvider) %></code>, model=<code><%= esc(lmModel) %></code>.
      A successful request is needed to verify that this account can use the model.
    </div>

    <% if (result != null) { %>
    <section class="card">
      <div class="card-header"><h2 class="card-title">Last result <% if (lastAction != null) { %>(<%= esc(lastAction) %>)<% } %></h2></div>
      <div class="card-content">
        <% if (result.isSuccess()) { %>
          <p class="tooltip-small">Provider: <%= esc(result.getProvider()) %> · Model: <%= esc(result.getModel()) %></p>
          <div class="result-toolbar">
            <button type="button" class="result-tab active" data-target="md">Markdown View</button>
            <button type="button" class="result-tab" data-target="raw">Raw Text</button>
          </div>
          <div id="result-md" class="result-panel active">
            <div id="md-render" class="md-box"></div>
          </div>
          <div id="result-raw" class="result-panel">
            <div class="ai-pre"><%= esc(result.getText()) %></div>
          </div>
          <textarea id="result-source" style="display:none"><%= esc(result.getText()) %></textarea>
        <% } else { %>
          <p class="ai-error"><%= esc(result.getErrorMessage()) %></p>
        <% } %>
      </div>
    </section>
    <% } %>

    <section class="ai-grid">
      <section class="card">
        <div class="card-header"><h2 class="card-title">Skill match</h2></div>
        <div class="card-content">
          <form method="post" action="<%= ctx %>/admin/ai-demo">
            <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
            <input type="hidden" name="action" value="match" />
            <p class="tooltip-small">Applicant skills (comma-separated)</p>
            <textarea class="ai-input" name="applicantSkills" placeholder="Java, SQL, communication">Java, Python, SQL</textarea>
            <p class="tooltip-small" style="margin-top:0.75rem">Job requirements</p>
            <textarea class="ai-input" name="jobRequirements" placeholder="Java, Docker, teaching">Java, Docker, Kubernetes</textarea>
            <p style="margin-top:0.75rem"><button type="submit" class="button button-primary">Run</button></p>
          </form>
        </div>
      </section>

      <section class="card">
        <div class="card-header"><h2 class="card-title">Missing skills</h2></div>
        <div class="card-content">
          <form method="post" action="<%= ctx %>/admin/ai-demo">
            <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
            <input type="hidden" name="action" value="missing" />
            <p class="tooltip-small">Candidate skills</p>
            <textarea class="ai-input" name="candidateSkills">Java, office hours</textarea>
            <p class="tooltip-small" style="margin-top:0.75rem">Required job skills</p>
            <textarea class="ai-input" name="requiredSkills">Java, Docker, Kubernetes</textarea>
            <p style="margin-top:0.75rem"><button type="submit" class="button button-primary">Run</button></p>
          </form>
        </div>
      </section>

      <section class="card" style="grid-column: 1 / -1;">
        <div class="card-header"><h2 class="card-title">Job recommendation</h2></div>
        <div class="card-content">
          <form method="post" action="<%= ctx %>/admin/ai-demo">
            <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
            <input type="hidden" name="action" value="recommend" />
            <p class="tooltip-small">Candidate profile (short)</p>
            <textarea class="ai-input" name="candidateProfile">Final-year BSc; strong Java; prefers lab sessions.</textarea>
            <p class="tooltip-small" style="margin-top:0.75rem">Open positions (one per line)</p>
            <textarea class="ai-input" name="openPositions" style="min-height:7rem;">CS101 Lab TA — Java lab support
CS201 Grading Assistant — Python, SQL
CS301 Project Mentor — software project coaching</textarea>
            <p style="margin-top:0.75rem"><button type="submit" class="button button-primary">Run</button></p>
          </form>
        </div>
      </section>
    </section>
  </main>
  <script>
    (function () {
      var source = document.getElementById('result-source');
      if (!source) return;

      var mdTarget = document.getElementById('md-render');
      var md = source.value || '';

      function inlineFormat(text) {
        var s = text
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;');
        s = s.replace(/`([^`]+)`/g, '<code>$1</code>');
        s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
        s = s.replace(/\*([^*]+)\*/g, '<em>$1</em>');
        s = s.replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>');
        return s;
      }

      function renderMarkdown(input) {
        var lines = input.split(/\r?\n/);
        var html = [];
        var inList = false;

        function closeList() {
          if (inList) {
            html.push('</ul>');
            inList = false;
          }
        }

        for (var i = 0; i < lines.length; i++) {
          var line = lines[i];
          var t = line.trim();
          if (!t) {
            closeList();
            continue;
          }
          if (t.indexOf('### ') === 0) {
            closeList();
            html.push('<h3>' + inlineFormat(t.substring(4)) + '</h3>');
            continue;
          }
          if (t.indexOf('## ') === 0) {
            closeList();
            html.push('<h2>' + inlineFormat(t.substring(3)) + '</h2>');
            continue;
          }
          if (t.indexOf('- ') === 0 || t.indexOf('* ') === 0) {
            if (!inList) {
              html.push('<ul>');
              inList = true;
            }
            html.push('<li>' + inlineFormat(t.substring(2)) + '</li>');
            continue;
          }
          closeList();
          html.push('<p>' + inlineFormat(t) + '</p>');
        }
        closeList();
        return html.join('');
      }

      mdTarget.innerHTML = renderMarkdown(md);

      var tabs = document.querySelectorAll('.result-tab');
      tabs.forEach(function (btn) {
        btn.addEventListener('click', function () {
          tabs.forEach(function (b) { b.classList.remove('active'); });
          btn.classList.add('active');
          var target = btn.getAttribute('data-target');
          document.getElementById('result-md').classList.toggle('active', target === 'md');
          document.getElementById('result-raw').classList.toggle('active', target === 'raw');
        });
      });
    })();
  </script>
</body>
</html>
