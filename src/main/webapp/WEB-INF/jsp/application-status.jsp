<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.bupt.ta.model.Application" %>
<%@ page import="java.util.List" %>
<%
  @SuppressWarnings("unchecked")
  List<Application> apps = (List<Application>) request.getAttribute("applications");
  if (apps == null) apps = java.util.Collections.emptyList();
  String filter      = (String) request.getAttribute("filter");       if (filter == null) filter = "All";
  Long countPending  = (Long)   request.getAttribute("countPending");  if (countPending  == null) countPending  = 0L;
  Long countAccepted = (Long)   request.getAttribute("countAccepted"); if (countAccepted == null) countAccepted = 0L;
  Long countRejected = (Long)   request.getAttribute("countRejected"); if (countRejected == null) countRejected = 0L;
  Long countWithdrawn = (Long)  request.getAttribute("countWithdrawn"); if (countWithdrawn == null) countWithdrawn = 0L;
  Long countAll = (Long)        request.getAttribute("countAll");      if (countAll == null) countAll = countPending + countAccepted + countRejected + countWithdrawn;
  String infoMessage = (String) request.getAttribute("infoMessage");
  String ctx = request.getContextPath();
%>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>My Applications - TA Recruitment</title>
  <link rel="stylesheet" href="<%= ctx %>/static/css/app.css" />
</head>
<body>
<div class="page--top fade-in">
  <div class="layout-xl">

    <!-- Horizontal header row -->
    <div class="page-header-row">
      <div class="header-left">
        <div class="logo">
          <!-- Layers icon (matches ApplicationStatusPage.tsx) -->
          <svg viewBox="0 0 24 24" style="width:32px;height:32px;fill:none;stroke:#fff;stroke-width:2;stroke-linecap:round;stroke-linejoin:round;"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>
        </div>
        <div>
          <h1 class="page-title">My Applications</h1>
          <p class="page-subtitle">Track your TA application statuses</p>
        </div>
      </div>
      <div style="display:flex;gap:.5rem;flex-wrap:wrap;align-items:center;">
        <a class="link-pill" href="<%= ctx %>/job">
          <svg viewBox="0 0 24 24"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
          Browse jobs
        </a>
        <a class="link-pill" href="<%= ctx %>/profile">
          <svg viewBox="0 0 24 24"><polyline points="15 18 9 12 15 6"/></svg>
          Back to profile
        </a>
      </div>
    </div>

    <% if (infoMessage != null && !infoMessage.isEmpty()) { %>
    <div class="alert alert-success" style="margin-bottom:1rem;"><%= infoMessage %></div>
    <% } %>

    <!-- Summary cards + Filter panel (side by side on lg) -->
    <div class="app-summary-row">
      <!-- Summary cards -->
      <div class="stat-grid">
        <div class="stat-card stat-card--pending">
          <div class="stat-top">
            <div class="stat-label">Pending</div>
            <div class="stat-icon" style="border-color:rgba(253,230,138,.60);">
              <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            </div>
          </div>
          <div class="stat-number"><%= countPending %></div>
        </div>
        <div class="stat-card stat-card--accepted">
          <div class="stat-top">
            <div class="stat-label">Accepted</div>
            <div class="stat-icon" style="border-color:rgba(187,247,208,.60);">
              <svg viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            </div>
          </div>
          <div class="stat-number"><%= countAccepted %></div>
        </div>
        <div class="stat-card stat-card--rejected">
          <div class="stat-top">
            <div class="stat-label">Rejected</div>
            <div class="stat-icon" style="border-color:rgba(254,202,202,.60);">
              <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
            </div>
          </div>
          <div class="stat-number"><%= countRejected %></div>
        </div>
        <div class="stat-card stat-card--withdrawn">
          <div class="stat-top">
            <div class="stat-label">Withdrawn</div>
            <div class="stat-icon" style="border-color:rgba(203,213,225,.75);">
              <svg viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
            </div>
          </div>
          <div class="stat-number"><%= countWithdrawn %></div>
        </div>
      </div>

      <!-- Filter panel -->
      <div class="filter-panel">
        <div class="filter-panel-label">Filter by Status</div>
        <div class="filter-grid">
          <% String[] filters = {"All","Pending","Accepted","Rejected","Withdrawn"};
             long[] fCounts = {countAll, countPending, countAccepted, countRejected, countWithdrawn};
             for (int fi = 0; fi < filters.length; fi++) { String f = filters[fi]; %>
            <a href="<%= ctx %>/applications?filter=<%= f %>"
               class="seg-btn <%= f.equals(filter) ? "active" : "" %>"><%= f %><span class="count"><%= fCounts[fi] %></span></a>
          <% } %>
        </div>
      </div>
    </div>

    <!-- Application cards (3-column grid) -->
    <div class="app-grid">
    <% if (apps.isEmpty()) { %>
      <div class="empty-state">
        <div class="empty-icon">
          <svg viewBox="0 0 24 24"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
        </div>
        <p style="font-size:1rem;font-weight:600;color:#0f172a;margin-bottom:.25rem;">No applications found</p>
        <p class="page-subtitle">Try adjusting your filters or <a href="<%= ctx %>/job">browse open jobs</a> to apply.</p>
      </div>
    <% } else {
         for (Application a : apps) {
           String st = a.status != null ? a.status : "Pending";
           String dot = st.equals("Accepted") ? "&#10003;" : st.equals("Rejected") ? "&#10007;" : st.equals("Withdrawn") ? "&#8680;" : "&#8987;";
    %>
      <div class="app-card">
        <div class="app-accent acc-<%= st %>"></div>
        <div class="app-body">
          <div class="app-top">
            <span class="module-code"><%= a.moduleCode != null ? a.moduleCode : "" %></span>
            <span class="status-badge b-<%= st %>"><%= dot %>&nbsp;<%= st %></span>
          </div>
          <div class="app-name"><%= a.moduleName != null ? a.moduleName : "" %></div>
          <div class="app-meta">
            <svg viewBox="0 0 24 24"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>
            <%= a.role != null ? a.role : "" %>
          </div>
          <div class="app-meta">
            <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            Applied <%= a.applicationDate != null ? a.applicationDate : "" %>
          </div>
          <% if (a.feedback != null && !a.feedback.isEmpty()) { %>
          <div class="fb-toggle">
            <button type="button" onclick="toggleFb(this)">&#9660; View feedback</button>
            <div class="fb-box fb-<%= st %>"><%= a.feedback.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;") %></div>
          </div>
          <% } %>
          <% if ("Pending".equals(st)) { %>
          <div class="withdraw-btn">
            <form method="post" action="<%= ctx %>/applications"
                  onsubmit="return confirm('Withdraw this application?');">
              <input type="hidden" name="csrfToken" value="<%= com.bupt.ta.security.CsrfFilter.csrfToken(request) %>" />
              <input type="hidden" name="action" value="withdraw" />
              <input type="hidden" name="appId"  value="<%= a.id != null ? a.id : "" %>" />
              <button type="submit" class="btn-withdraw">Withdraw</button>
            </form>
          </div>
          <% } %>
        </div>
      </div>
    <% } } %>
    </div><!-- /app-grid -->

  </div>
</div>
<script>
function toggleFb(btn) {
  var box = btn.nextElementSibling;
  var open = box.style.display === 'block';
  box.style.display = open ? 'none' : 'block';
  btn.innerHTML = open ? '&#9660; View feedback' : '&#9650; Hide feedback';
}
</script>
</body>
</html>
