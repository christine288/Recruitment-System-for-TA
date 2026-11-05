<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.bupt.ta.model.Roles" %>
<%@ page import="com.bupt.ta.model.User" %>
<%@ page import="com.bupt.ta.model.Application" %>
<%@ page import="com.bupt.ta.model.Job" %>
<%@ page import="com.bupt.ta.model.TaWorkloadStats" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.LinkedHashMap" %>
<%
  @SuppressWarnings("unchecked")
  List<User> users = (List<User>) request.getAttribute("users");
  if (users == null) users = java.util.Collections.emptyList();
  @SuppressWarnings("unchecked")
  List<Application> applications = (List<Application>) request.getAttribute("applications");
  if (applications == null) applications = java.util.Collections.emptyList();
  @SuppressWarnings("unchecked")
  List<Job> jobList = (List<Job>) request.getAttribute("jobs");
  if (jobList == null) jobList = java.util.Collections.emptyList();
  @SuppressWarnings("unchecked")
  Map<String, TaWorkloadStats> taWorkload = (Map<String, TaWorkloadStats>) request.getAttribute("taWorkload");
  if (taWorkload == null) taWorkload = java.util.Collections.emptyMap();
  @SuppressWarnings("unchecked")
  List<User> taUsersWorkloadOrder = (List<User>) request.getAttribute("taUsersWorkloadOrder");
  if (taUsersWorkloadOrder == null) taUsersWorkloadOrder = java.util.Collections.emptyList();

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

  int totalTa = 0;
  for (User u : users) {
    if (Roles.TA.equals(u.role)) totalTa++;
  }
  int totalApplications = applications.size();
  Map<String, Integer> statusMap = new LinkedHashMap<String, Integer>();
  statusMap.put("Pending", 0);
  statusMap.put("Accepted", 0);
  statusMap.put("Rejected", 0);
  for (Application a : applications) {
    String rawSt = a.status == null ? "" : a.status.trim();
    String bucket;
    if ("Accepted".equalsIgnoreCase(rawSt)) bucket = "Accepted";
    else if ("Rejected".equalsIgnoreCase(rawSt)) bucket = "Rejected";
    else bucket = "Pending";
    statusMap.put(bucket, statusMap.getOrDefault(bucket, 0) + 1);
  }
  String chartDataJson = (String) request.getAttribute("chartDataJson");
  if (chartDataJson == null) chartDataJson = "{}";
  Boolean aiEnabledObj = (Boolean) request.getAttribute("aiEnabled");
  boolean aiEnabled = aiEnabledObj != null && aiEnabledObj.booleanValue();
  Integer highWorkloadObj = (Integer) request.getAttribute("highWorkloadTaCount");
  int highWorkloadTaCount = highWorkloadObj != null ? highWorkloadObj.intValue() : 0;

  String ctx = request.getContextPath();
  String csrfToken = com.bupt.ta.security.CsrfFilter.csrfToken(request);
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>TA Recruitment System - Admin Dashboard</title>
  <link rel="stylesheet" href="<%= ctx %>/static/css/admin-dashboard.css" />
</head>
<body>
  <header class="header">
    <div class="header-row">
      <div>
        <h1 class="title">TA Recruitment System</h1>
        <p class="subtitle">Admin Dashboard</p>
        <div class="header-actions">
          <a href="<%= ctx %>/admin/ai-demo">AI Demo</a>
          <span class="header-sep">|</span>
          <a href="<%= ctx %>/admin/ta-profiles">Profile</a>
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

  <%
    String adminMessage = (String) request.getAttribute("adminMessage");
  %>
  <% if (adminMessage != null && !adminMessage.trim().isEmpty()) { %>
  <div class="container" style="padding-bottom:0">
    <p class="admin-notice"><%= adminMessage.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") %></p>
  </div>
  <% } %>

  <main class="container container--wide">
    <section class="grid-3 stat-row">
      <article class="card stat-card">
        <div class="card-header">
          <div>
            <h2 class="card-title">Total TAs</h2>
            <p class="stat-hint">Registered applicant accounts</p>
          </div>
          <div class="icon-wrap bg-blue" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          </div>
        </div>
        <div class="card-content"><div class="stat-value"><%= totalTa %></div></div>
      </article>
      <article class="card stat-card">
        <div class="card-header">
          <div>
            <h2 class="card-title">Open Jobs</h2>
            <p class="stat-hint">Published vacancies in system</p>
          </div>
          <div class="icon-wrap bg-green" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
          </div>
        </div>
        <div class="card-content"><div class="stat-value"><%= jobList.size() %></div></div>
      </article>
      <article class="card stat-card">
        <div class="card-header">
          <div>
            <h2 class="card-title">Applications</h2>
            <p class="stat-hint">All submission records</p>
          </div>
          <div class="icon-wrap bg-purple" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          </div>
        </div>
        <div class="card-content"><div class="stat-value"><%= totalApplications %></div></div>
      </article>
    </section>

    <!-- AI Insights & Charts -->
    <section class="analytics-section" aria-labelledby="analytics-heading">
      <div class="section-head">
        <div>
          <h2 id="analytics-heading" class="section-head__title">AI Insights &amp; Analytics</h2>
          <p class="section-head__subtitle">Platform trends with AI-generated executive summary</p>
        </div>
        <span class="ai-chip">AI-powered</span>
      </div>

      <div class="analytics-kpi-row">
        <article class="card analytics-kpi-card">
          <p class="analytics-kpi-card__label">Total users</p>
          <p class="analytics-kpi-card__value"><%= users.size() %></p>
        </article>
        <article class="card analytics-kpi-card analytics-kpi-card--pending">
          <p class="analytics-kpi-card__label">Pending reviews</p>
          <p class="analytics-kpi-card__value"><%= statusMap.get("Pending") %></p>
        </article>
        <article class="card analytics-kpi-card analytics-kpi-card--accepted">
          <p class="analytics-kpi-card__label">Accepted</p>
          <p class="analytics-kpi-card__value"><%= statusMap.get("Accepted") %></p>
        </article>
        <article class="card analytics-kpi-card<%= highWorkloadTaCount > 0 ? " analytics-kpi-card--alert" : "" %>">
          <p class="analytics-kpi-card__label">Workload alerts</p>
          <p class="analytics-kpi-card__value"><%= highWorkloadTaCount %></p>
        </article>
      </div>

      <div class="analytics-layout">
        <div class="analytics-main">
          <div class="grid-2 chart-row">
            <article class="card chart-card">
              <div class="card-header chart-card__header">
                <div>
                  <h3 class="card-title">Applicant pool growth</h3>
                  <p class="chart-card__desc">Cumulative unique applicants by first application month</p>
                </div>
              </div>
              <div class="card-content chart-card__body">
                <div class="chart-canvas-wrap"><canvas id="chartApplicantPool" aria-label="Applicant pool growth chart"></canvas></div>
              </div>
            </article>
            <article class="card chart-card">
              <div class="card-header chart-card__header">
                <div>
                  <h3 class="card-title">Monthly applications</h3>
                  <p class="chart-card__desc">Submission volume over time</p>
                </div>
              </div>
              <div class="card-content chart-card__body">
                <div class="chart-canvas-wrap"><canvas id="chartApplications" aria-label="Monthly applications chart"></canvas></div>
              </div>
            </article>
          </div>

          <div class="grid-2 chart-row">
            <article class="card chart-card">
              <div class="card-header chart-card__header">
                <h3 class="card-title">Pipeline status</h3>
              </div>
              <div class="card-content chart-card__body">
                <div class="chart-canvas-wrap chart-canvas-wrap--donut"><canvas id="chartStatus"></canvas></div>
                <div class="status-quick-links">
                  <a class="status-link status-link--pending" href="<%= ctx %>/admin/applications/by-status?status=pending">Pending <%= statusMap.get("Pending") %></a>
                  <a class="status-link status-link--accepted" href="<%= ctx %>/admin/applications/by-status?status=accepted">Accepted <%= statusMap.get("Accepted") %></a>
                  <a class="status-link status-link--rejected" href="<%= ctx %>/admin/applications/by-status?status=rejected">Rejected <%= statusMap.get("Rejected") %></a>
                </div>
              </div>
            </article>
            <article class="card chart-card">
              <div class="card-header chart-card__header">
                <h3 class="card-title">Users by role</h3>
              </div>
              <div class="card-content chart-card__body">
                <div class="chart-canvas-wrap chart-canvas-wrap--donut"><canvas id="chartRoles"></canvas></div>
              </div>
            </article>
          </div>

          <article class="card chart-card">
            <div class="card-header chart-card__header">
              <div>
                <h3 class="card-title">Top modules by applications</h3>
                <p class="chart-card__desc">Most popular teaching modules in the pipeline</p>
              </div>
            </div>
            <div class="card-content chart-card__body">
              <div class="chart-canvas-wrap chart-canvas-wrap--bar"><canvas id="chartModules"></canvas></div>
            </div>
          </article>
        </div>

        <aside class="analytics-sidebar">
          <article class="card analytics-ai-card">
            <div class="card-header analytics-ai-card__header">
              <div>
                <h3 class="card-title">AI platform briefing</h3>
                <p class="chart-card__desc">Executive summary &amp; recommended actions</p>
              </div>
              <% if (highWorkloadTaCount > 0) { %>
              <span class="alert-chip"><%= highWorkloadTaCount %> alert<%= highWorkloadTaCount == 1 ? "" : "s" %></span>
              <% } %>
            </div>
            <div class="card-content analytics-ai-card__body">
              <% if (aiEnabled) { %>
              <div class="analytics-ai__panel" id="admin-ai-panel">
                <div class="analytics-ai__loading">Analyzing platform metrics…</div>
                <div class="analytics-ai__md" style="display:none;"></div>
              </div>
              <% } else { %>
              <div class="analytics-ai__disabled">AI analytics disabled (LM_ENABLED=false).</div>
              <% } %>
            </div>
          </article>
        </aside>
      </div>
    </section>

    <script type="application/json" id="admin-chart-data"><%= chartDataJson %></script>

    <section class="card card--workload">
      <div class="card-header">
        <div>
          <h2 class="card-title">TA Workload Overview</h2>
          <p class="card-subtitle">Accepted assignments per TA · warning at &ge; <%= TaWorkloadStats.ASSIGNED_JOBS_WARNING_THRESHOLD %></p>
        </div>
      </div>
      <div class="card-content">
        <div class="table-scroll">
        <table class="table table-workload">
          <thead>
            <tr>
              <th>TA Name</th>
              <th>Email</th>
              <th>Assigned jobs</th>
              <th>Accepted work</th>
              <th>Rejected work</th>
            </tr>
          </thead>
          <tbody>
            <% if (taUsersWorkloadOrder.isEmpty()) { %>
            <tr><td colspan="5" class="tooltip-small">No TA accounts yet.</td></tr>
            <% } else { %>
            <% for (User u : taUsersWorkloadOrder) {
                 String tid = u.id == null ? "" : u.id.trim();
                 TaWorkloadStats ws = taWorkload.get(tid);
                 if (ws == null) ws = new TaWorkloadStats();
            %>
            <tr>
              <td><%= u.name == null ? "-" : u.name %></td>
              <td><%= u.email == null ? "-" : u.email %></td>
              <td class="workload-assigned-cell"><strong><%= ws.accepted %></strong></td>
              <td class="workload-accepted-cell">
                <% if (ws.accepted >= TaWorkloadStats.ASSIGNED_JOBS_WARNING_THRESHOLD) { %>
                <div class="workload-warning" role="alert">
                  <strong>Warning:</strong> workload limit reached or exceeded (<%= ws.accepted %> assigned jobs, limit <%= TaWorkloadStats.ASSIGNED_JOBS_WARNING_THRESHOLD %>).
                </div>
                <% } %>
                <% if (ws.acceptedPositions.isEmpty()) { %>
                  <span class="tooltip-small">—</span>
                <% } else { %>
                  <ul class="workload-accepted-list">
                  <% for (String line : ws.acceptedPositions) {
                       String safe = line == null ? "" : line.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
                  %>
                    <li><%= safe %></li>
                  <% } %>
                  </ul>
                <% } %>
              </td>
              <td class="workload-rejected-cell">
                <% if (ws.rejectedPositions.isEmpty()) { %>
                  <span class="tooltip-small">—</span>
                <% } else { %>
                  <ul class="workload-rejected-list">
                  <% for (String line : ws.rejectedPositions) {
                       String safeR = line == null ? "" : line.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
                  %>
                    <li><%= safeR %></li>
                  <% } %>
                  </ul>
                <% } %>
              </td>
            </tr>
            <% } %>
            <% } %>
          </tbody>
        </table>
        </div>
      </div>
    </section>

    <section class="card">
      <div class="card-header"><h2 class="card-title">Management</h2></div>
      <div class="card-content">
        <div class="tab-list" role="tablist">
          <button class="tab-btn active" type="button" data-tab="users">User Management</button>
          <button class="tab-btn" type="button" data-tab="jobs">Job Management</button>
          <button class="tab-btn" type="button" data-tab="export">Data Export</button>
        </div>

        <div id="users" class="tab-content active">
          <form method="post" action="<%= ctx %>/admin/users" class="create-user-form">
            <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
            <input type="hidden" name="action" value="createUser" />
            <p class="tooltip-small" style="margin-top:0"><strong>Create user</strong> — TA or MO account with a unique email.</p>
            <div class="create-user-grid">
              <label>Name<br/><input type="text" name="name" required /></label>
              <label>Email<br/><input type="email" name="email" required /></label>
              <label>Password<br/><input type="password" name="password" required minlength="6" /></label>
              <label>Role<br/>
                <select name="role" required>
                  <option value="TA">TA</option>
                  <option value="MO">MO</option>
                </select>
              </label>
              <button type="submit" class="button button-primary">Create user</button>
            </div>
          </form>
          <% if (users.isEmpty()) { %>
            <p class="tooltip-small">No registered users yet.</p>
          <% } else { %>
          <table class="table">
            <thead><tr><th>Name</th><th>Role</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              <% for (User u : users) {
                   String r = u.role != null ? u.role : "";
              %>
              <tr>
                <td><%= u.name == null ? "" : u.name %></td>
                <td><%= r %></td>
                <td>
                  <% if (u.active) { %>
                    <span class="badge badge-active">Active</span>
                  <% } else { %>
                    <span class="badge badge-inactive">Inactive</span>
                  <% } %>
                </td>
                <td>
                  <div class="user-mgmt-actions">
                  <form method="post" action="<%= ctx %>/admin/users" class="inline-form" onsubmit="return confirm('Deactivate this user? They will not be able to log in.');">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="deactivate" />
                    <input type="hidden" name="userId" value="<%= u.id %>" />
                    <button type="submit" class="button" <%= u.active ? "" : "disabled" %>>Deactivate</button>
                  </form>
                  <form method="post" action="<%= ctx %>/admin/users" class="inline-form" onsubmit="return confirm('Activate this user? They will be able to log in again.');">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="activate" />
                    <input type="hidden" name="userId" value="<%= u.id %>" />
                    <button type="submit" class="button button-success" <%= u.active ? "disabled" : "" %>>Activate</button>
                  </form>
                  <form method="post" action="<%= ctx %>/admin/users" class="inline-form" onsubmit="return confirm('Permanently delete this user and their applications, profile, and CV files?');">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="delete" />
                    <input type="hidden" name="userId" value="<%= u.id %>" />
                    <button type="submit" class="button button-danger">Delete</button>
                  </form>
                  </div>
                </td>
              </tr>
              <% } %>
            </tbody>
          </table>
          <% } %>
        </div>

        <div id="jobs" class="tab-content">
          <p class="tooltip-small">Jobs from <code>WEB-INF/data/jobs.json</code> (same list as applicant job browser).</p>
          <% if (jobList.isEmpty()) { %>
            <p class="tooltip-small">No jobs defined.</p>
          <% } else { %>
          <table class="table">
            <thead><tr><th>Module</th><th>Activity</th><th>Posted</th><th>Actions</th></tr></thead>
            <tbody>
              <% for (Job j : jobList) {
                   String mid = j.getId() == null ? "" : j.getId();
              %>
              <tr>
                <td><%= j.getModuleName() == null ? "-" : j.getModuleName() %></td>
                <td><%= j.getActivityType() == null ? "-" : j.getActivityType() %></td>
                <td><%= j.getPostDate() == null ? "-" : j.getPostDate() %></td>
                <td>
                  <a class="button button-outline" href="<%= ctx %>/admin/job-view?id=<%= mid %>">View</a>
                  <form method="post" action="<%= ctx %>/admin/jobs" style="display:inline" onsubmit="return confirm('Delete this job from jobs.json?');">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="delete" />
                    <input type="hidden" name="jobId" value="<%= mid %>" />
                    <button type="submit" class="button button-danger">Delete</button>
                  </form>
                </td>
              </tr>
              <% } %>
            </tbody>
          </table>
          <% } %>
        </div>

        <div id="export" class="tab-content">
          <p class="tooltip-small">Download CSV (UTF-8). Open in Excel if needed.</p>
          <div class="export-bar">
            <a class="button button-primary" href="<%= ctx %>/admin/export?type=users">Export users (CSV)</a>
            <a class="button button-success" href="<%= ctx %>/admin/export?type=applications">Export applications (CSV)</a>
          </div>
        </div>
      </div>
    </section>
  </main>

  <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
  <script src="<%= ctx %>/static/js/admin-dashboard-charts.js"></script>
  <% if (aiEnabled) { %>
  <script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
  <script src="<%= ctx %>/static/js/ai-stream.js"></script>
  <script>
  (function () {
    'use strict';
    var ctx = '<%= ctx %>';
    var panel = document.getElementById('admin-ai-panel');
    if (!panel || typeof TaAiStream === 'undefined') return;
    var loadingEl = panel.querySelector('.analytics-ai__loading');
    var mdEl = panel.querySelector('.analytics-ai__md');
    var text = '';
    TaAiStream.consume(ctx + '/api/ai/stream?feature=adminAnalytics', {
      onDelta: function (delta) {
        text += delta;
        if (loadingEl) loadingEl.style.display = 'none';
        if (mdEl) {
          mdEl.style.display = 'block';
          TaAiStream.renderMarkdown(mdEl, text, typeof marked !== 'undefined' ? marked : null);
        }
      },
      onDone: function () {
        if (!text && loadingEl) {
          loadingEl.textContent = 'No analysis returned.';
          loadingEl.style.display = 'block';
        }
      },
      onError: function (err) {
        if (loadingEl) {
          loadingEl.className = 'analytics-ai__error';
          loadingEl.textContent = err || 'AI analysis failed.';
        }
      }
    });
  })();
  </script>
  <% } %>
  <script>
    function showTab(tabName) {
      document.querySelectorAll('.tab-btn').forEach(function (btn) {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tabName);
      });
      document.querySelectorAll('.tab-content').forEach(function (c) {
        c.classList.toggle('active', c.id === tabName);
      });
    }
    document.querySelectorAll('.tab-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        showTab(btn.getAttribute('data-tab'));
      });
    });
  </script>
</body>
</html>
