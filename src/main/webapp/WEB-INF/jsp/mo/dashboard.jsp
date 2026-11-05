<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="com.bupt.ta.model.User" %>
<%@ page import="com.bupt.ta.model.Job" %>
<%@ page import="com.bupt.ta.model.Application" %>
<%@ page import="com.bupt.ta.model.MoWorkloadSnapshot" %>
<%@ page import="com.bupt.ta.model.TaWorkloadStats" %>
<%
    String ctx = request.getContextPath();
    if (ctx == null) ctx = "";
    String csrfToken = com.bupt.ta.security.CsrfFilter.csrfToken(request);
    User currentUser = (User) session.getAttribute("user");
    @SuppressWarnings("unchecked")
    List<Job> jobs = (List<Job>) request.getAttribute("jobs");
    @SuppressWarnings("unchecked")
    List<Application> applications = (List<Application>) request.getAttribute("applications");
    if (jobs == null) jobs = java.util.Collections.emptyList();
    if (applications == null) applications = java.util.Collections.emptyList();

    @SuppressWarnings("unchecked")
    Map<String, String> cvByUserId = (Map<String, String>) request.getAttribute("cvByUserId");
    if (cvByUserId == null) cvByUserId = java.util.Collections.emptyMap();

    @SuppressWarnings("unchecked")
    Map<String, MoWorkloadSnapshot> workloadSnapshots =
            (Map<String, MoWorkloadSnapshot>) request.getAttribute("workloadSnapshots");
    if (workloadSnapshots == null) workloadSnapshots = java.util.Collections.emptyMap();

    @SuppressWarnings("unchecked")
    Map<String, String> applicantNamesByUserId =
            (Map<String, String>) request.getAttribute("applicantNamesByUserId");
    if (applicantNamesByUserId == null) applicantNamesByUserId = java.util.Collections.emptyMap();

    Boolean aiEnabledObj = (Boolean) request.getAttribute("aiEnabled");
    boolean aiEnabled = aiEnabledObj != null && aiEnabledObj.booleanValue();

    String moMessage = (String) request.getAttribute("moMessage");

    int pendingCount = 0;
    for (Application a : applications) {
        if ("Pending".equalsIgnoreCase(a.status)) pendingCount++;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>MO Dashboard - TA Recruitment</title>
    <link rel="stylesheet" href="<%= ctx %>/static/css/app.css" />
</head>
<body>
<div class="page--top fade-in mo-page">
  <div class="layout-xl">

    <div class="page-header-row">
      <div class="header-left">
        <div class="logo">
          <svg viewBox="0 0 24 24"><rect x="2" y="7" width="20" height="14" rx="2" ry="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
        </div>
        <div>
          <h1 class="page-title">Module Organiser</h1>
          <p class="page-subtitle">
            Welcome, <strong><%= currentUser != null ? currentUser.name : "MO User" %></strong>
            &mdash; manage vacancies and review applications
          </p>
        </div>
      </div>
      <a class="link-pill" href="<%= ctx %>/logout">
        <svg viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        Logout
      </a>
    </div>

    <% if (moMessage != null && !moMessage.trim().isEmpty()) { %>
      <div class="alert alert-success" style="margin-bottom:2rem;"><%= moMessage %></div>
    <% } %>

    <div class="mo-stat-row">
      <div class="stat-card stat-card--info">
        <div class="stat-top">
          <div class="stat-label">Total Jobs</div>
          <div class="stat-icon" style="border-color:rgba(191,219,254,.60);">
            <svg viewBox="0 0 24 24"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>
          </div>
        </div>
        <div class="stat-number"><%= jobs.size() %></div>
      </div>
      <div class="stat-card stat-card--neutral">
        <div class="stat-top">
          <div class="stat-label">Applications</div>
          <div class="stat-icon" style="border-color:rgba(203,213,225,.75);">
            <svg viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          </div>
        </div>
        <div class="stat-number"><%= applications.size() %></div>
      </div>
      <div class="stat-card stat-card--pending">
        <div class="stat-top">
          <div class="stat-label">Pending Review</div>
          <div class="stat-icon" style="border-color:rgba(253,230,138,.60);">
            <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          </div>
        </div>
        <div class="stat-number"><%= pendingCount %></div>
      </div>
    </div>

    <div class="mo-stack">

      <!-- Create / Publish Job -->
      <section class="card mo-panel">
        <div class="mo-section-head">
          <div>
            <h2 class="section-heading">Create / Publish Job</h2>
            <p class="mo-section-desc">Post a new TA vacancy or save as draft for later.</p>
          </div>
        </div>
        <form method="post" action="<%= ctx %>/mo">
          <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
          <input type="hidden" name="action" value="createJob" />

          <div class="mo-form-panels">
            <div class="mo-inner-card">
              <h3 class="mo-inner-card__title">Basic information</h3>
              <div class="mo-form-grid">
                <div class="field">
                  <label for="moduleName">Module Name *</label>
                  <input type="text" id="moduleName" name="moduleName" required />
                </div>
                <div class="field">
                  <label for="moduleCode">Module Code *</label>
                  <input type="text" id="moduleCode" name="moduleCode" required />
                </div>
                <div class="field">
                  <label for="activityType">Activity Type</label>
                  <input type="text" id="activityType" name="activityType" placeholder="Lab Assistant / Tutorial Support" />
                </div>
                <div class="field">
                  <label for="requiredSkills">Required Skills</label>
                  <input type="text" id="requiredSkills" name="requiredSkills" placeholder="Java, SQL, Communication" />
                </div>
              </div>
            </div>

            <div class="mo-inner-card">
              <h3 class="mo-inner-card__title">Job description</h3>
              <div class="mo-form-grid">
                <div class="field field--full">
                  <label for="description">Description *</label>
                  <textarea id="description" name="description" required rows="5"></textarea>
                </div>
              </div>
            </div>

            <div class="mo-inner-card">
              <h3 class="mo-inner-card__title">Schedule &amp; capacity</h3>
              <div class="mo-form-grid">
                <div class="field">
                  <label for="applicationDeadline">Application Deadline</label>
                  <input type="text" id="applicationDeadline" name="applicationDeadline" placeholder="yyyy-mm-dd" pattern="\d{4}-\d{2}-\d{2}" title="Use format yyyy-mm-dd, e.g. 2026-06-30" />
                </div>
                <div class="field">
                  <label for="numberOfTAs">Number of TAs</label>
                  <input type="number" id="numberOfTAs" name="numberOfTAs" min="1" value="2" />
                </div>
                <div class="field">
                  <label for="duration">Duration</label>
                  <input type="text" id="duration" name="duration" value="One semester" />
                </div>
                <div class="field">
                  <label for="workloadHours">Workload</label>
                  <input type="text" id="workloadHours" name="workloadHours" placeholder="e.g. 4h/week" />
                </div>
              </div>
            </div>
          </div>

          <div class="mo-form-actions">
            <button class="btn btn-outline" type="submit" name="publishNow" value="0">Save Draft</button>
            <button class="btn btn-success" type="submit" name="publishNow" value="1">Publish Now</button>
          </div>
        </form>
      </section>

      <!-- Job Management -->
      <section class="card mo-panel">
        <div class="mo-section-head">
          <div>
            <h2 class="section-heading">Job Management</h2>
            <p class="mo-section-desc">Edit, publish drafts, or close open positions.</p>
          </div>
        </div>
        <% if (jobs.isEmpty()) { %>
          <div class="mo-empty">No jobs posted yet. Create your first vacancy above.</div>
        <% } else { %>
          <div class="mo-job-grid">
          <% for (Job job : jobs) {
              String st = (job.getStatus() == null || job.getStatus().trim().isEmpty()) ? "Published" : job.getStatus().trim();
              String statusClass = "job-status--published";
              if ("Draft".equalsIgnoreCase(st)) statusClass = "job-status--draft";
              else if ("Closed".equalsIgnoreCase(st)) statusClass = "job-status--closed";
          %>
            <article class="mo-job-card">
              <div class="mo-job-card__top">
                <div>
                  <h3 class="mo-job-card__title"><%= job.getModuleName() %></h3>
                  <span class="module-code" style="margin-top:.5rem;display:inline-block;"><%= job.getModuleCode() %></span>
                </div>
                <span class="job-status <%= statusClass %>"><%= st %></span>
              </div>
              <p class="mo-job-card__desc"><%= job.getDescription() == null || job.getDescription().isEmpty() ? "No description provided." : job.getDescription() %></p>
              <div class="mo-job-card__meta">
                <div class="mo-job-card__meta-item">
                  <span class="mo-job-card__meta-label">Activity</span>
                  <span class="mo-job-card__meta-value"><%= job.getActivityType() == null || job.getActivityType().isEmpty() ? "—" : job.getActivityType() %></span>
                </div>
                <div class="mo-job-card__meta-item">
                  <span class="mo-job-card__meta-label">Posted</span>
                  <span class="mo-job-card__meta-value"><%= job.getPostDate() == null ? "—" : job.getPostDate() %></span>
                </div>
                <div class="mo-job-card__meta-item">
                  <span class="mo-job-card__meta-label">Deadline</span>
                  <span class="mo-job-card__meta-value"><%= job.getApplicationDeadline() == null ? "—" : job.getApplicationDeadline() %></span>
                </div>
                <div class="mo-job-card__meta-item">
                  <span class="mo-job-card__meta-label">TAs needed</span>
                  <span class="mo-job-card__meta-value"><%= job.getNumberOfTAs() == null || job.getNumberOfTAs().trim().isEmpty() ? "—" : job.getNumberOfTAs() %></span>
                </div>
              </div>
              <div class="mo-job-card__skills">
                <%
                  List<String> skills = job.getRequiredSkills();
                  if (skills != null && !skills.isEmpty()) {
                    for (String s : skills) {
                %>
                  <span class="skill-tag"><%= s %></span>
                <%
                    }
                  } else {
                %>
                  <span class="muted">No skills listed</span>
                <% } %>
              </div>
              <div class="mo-job-card__footer">
                <% if ("Draft".equalsIgnoreCase(st)) { %>
                  <form method="post" action="<%= ctx %>/mo" style="display:inline;">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="publishJob" />
                    <input type="hidden" name="jobId" value="<%= job.getId() %>" />
                    <button class="btn btn-primary" type="submit">Publish</button>
                  </form>
                <% } %>
                <% if (!"Closed".equalsIgnoreCase(st)) { %>
                  <details class="mo-edit-panel">
                    <summary>Edit job</summary>
                    <form method="post" action="<%= ctx %>/mo">
                      <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                      <input type="hidden" name="action" value="editJob" />
                      <input type="hidden" name="jobId" value="<%= job.getId() %>" />
                      <div class="field">
                        <label>Module Name</label>
                        <input type="text" name="moduleName" value="<%= job.getModuleName() == null ? "" : job.getModuleName() %>" required />
                      </div>
                      <div class="field">
                        <label>Module Code</label>
                        <input type="text" name="moduleCode" value="<%= job.getModuleCode() == null ? "" : job.getModuleCode() %>" required />
                      </div>
                      <div class="field">
                        <label>Description</label>
                        <textarea name="description" required rows="3"><%= job.getDescription() == null ? "" : job.getDescription() %></textarea>
                      </div>
                      <div class="field">
                        <label>Deadline (yyyy-mm-dd)</label>
                        <input type="text" name="applicationDeadline" value="<%= job.getApplicationDeadline() == null ? "" : job.getApplicationDeadline() %>" />
                      </div>
                      <div class="field">
                        <label>Skills (comma-separated)</label>
                        <input type="text" name="requiredSkills" value="<%
                          List<String> sk = job.getRequiredSkills();
                          if (sk != null && !sk.isEmpty()) {
                            out.print(String.join(", ", sk).replace("&","&amp;").replace("\"","&quot;"));
                          }
                        %>" />
                      </div>
                      <button class="btn btn-primary" type="submit">Save changes</button>
                    </form>
                  </details>
                  <form method="post" action="<%= ctx %>/mo" style="display:inline;"
                        onsubmit="return confirm('Close this job? It will no longer accept applications.');">
                    <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                    <input type="hidden" name="action" value="closeJob" />
                    <input type="hidden" name="jobId" value="<%= job.getId() %>" />
                    <button class="btn btn-destructive" type="submit">Close</button>
                  </form>
                <% } else { %>
                  <span class="muted">This job is closed</span>
                <% } %>
              </div>
            </article>
          <% } %>
          </div>
        <% } %>
      </section>

      <% if (!applications.isEmpty() && pendingCount > 0) { %>
      <!-- Pending Applications -->
      <section class="card mo-panel">
        <div class="mo-section-head">
          <div>
            <h2 class="section-heading">Pending Review</h2>
            <p class="mo-section-desc"><%= pendingCount %> application<%= pendingCount == 1 ? "" : "s" %> waiting for your decision.</p>
          </div>
        </div>
        <div class="mo-pending-list">
        <% for (Application app : applications) {
            if (!"Pending".equalsIgnoreCase(app.status)) continue;
            MoWorkloadSnapshot ws = workloadSnapshots.get(app.id);
            int acceptedCount = ws != null ? ws.acceptedCount : 0;
            int pendingAppCount = ws != null ? ws.pendingCount : 0;
            int potentialLoad = ws != null ? ws.potentialLoadIfApprove : 0;
            boolean highLoad = potentialLoad >= TaWorkloadStats.ASSIGNED_JOBS_WARNING_THRESHOLD;
            String applicantQuery = URLEncoder.encode(app.userId, StandardCharsets.UTF_8);
            String cvName = cvByUserId.get(app.userId);
            String displayName = ws != null && ws.applicantName != null ? ws.applicantName : app.userId;
        %>
          <article class="mo-pending-card">
            <div class="mo-pending-card__head">
              <div>
                <h3 class="mo-pending-card__title"><%= app.moduleName %></h3>
                <p class="mo-pending-card__sub">
                  <%= displayName %> &middot; <%= app.moduleCode %> &middot; <%= app.role %>
                  &middot; Applied <%= app.applicationDate %>
                </p>
              </div>
              <span class="status-badge b-Pending">Pending</span>
            </div>
            <div class="mo-pending-card__body">
              <div class="mo-pending-side mo-pending-side--actions">
                <div class="mo-pending-id">Application <%= app.id %></div>
                <div class="mo-pending-links">
                  <% if (cvName != null) { %>
                    <a class="btn btn-primary" href="<%= ctx %>/cv?userId=<%= applicantQuery %>" target="_blank" rel="noopener">Open CV</a>
                  <% } else { %>
                    <span class="muted">No CV uploaded</span>
                  <% } %>
                  <a class="btn btn-outline" href="<%= ctx %>/mo/applicant-profile?userId=<%= applicantQuery %>">View Profile</a>
                </div>
                <% if (ws != null) { %>
                <div class="workload-chips">
                  <span class="workload-chip">Accepted <strong><%= acceptedCount %></strong></span>
                  <span class="workload-chip">Pending <strong><%= pendingAppCount %></strong></span>
                  <span class="workload-chip<%= highLoad ? " is-warn" : "" %>">Potential load <strong><%= potentialLoad %></strong></span>
                </div>
                <% } %>
                <form class="mo-decision-form" method="post" action="<%= ctx %>/mo">
                  <input type="hidden" name="csrfToken" value="<%= csrfToken %>" />
                  <input type="hidden" name="appId" value="<%= app.id %>" />
                  <input type="text" name="feedback" placeholder="Optional feedback for applicant" />
                  <div class="mo-decision-actions">
                    <button class="btn btn-success" type="submit" name="action" value="approveApp">Approve</button>
                    <button class="btn btn-destructive" type="submit" name="action" value="rejectApp">Reject</button>
                  </div>
                </form>
              </div>
              <div class="mo-pending-side mo-pending-side--ai">
                <% if (aiEnabled) { %>
                <div class="mo-ai-advice" id="mo-ai-<%= app.id %>" data-app-id="<%= app.id %>" data-feature="moWorkloadAdvice">
                  <div class="mo-ai-advice-head">
                    <p class="mo-ai-advice-title">AI workload advice</p>
                    <span class="rec-badge-slot"></span>
                  </div>
                  <div class="mo-ai-advice-body">
                    <div class="mo-ai-advice-loading">Analyzing workload…</div>
                    <div class="mo-ai-advice-md" style="display:none;"></div>
                  </div>
                </div>
                <% } else { %>
                <div class="mo-ai-disabled">AI advice is disabled (LM_ENABLED=false).</div>
                <% } %>
              </div>
            </div>
          </article>
        <% } %>
        </div>
      </section>
      <% } else if (!applications.isEmpty()) { %>
      <section class="card mo-panel">
        <div class="mo-section-head">
          <div>
            <h2 class="section-heading">Pending Review</h2>
            <p class="mo-section-desc">All caught up — no applications waiting.</p>
          </div>
        </div>
        <div class="mo-empty">No pending applications at the moment.</div>
      </section>
      <% } %>

      <% if (!applications.isEmpty()) {
           int processedCount = applications.size() - pendingCount;
      %>
      <!-- Processed Applications -->
      <section class="card mo-panel">
        <div class="mo-section-head">
          <div>
            <h2 class="section-heading">Processed Applications</h2>
            <p class="mo-section-desc"><%= processedCount %> completed decision<%= processedCount == 1 ? "" : "s" %> on record.</p>
          </div>
        </div>
        <% boolean anyProcessed = false;
           for (Application app : applications) {
            if ("Pending".equalsIgnoreCase(app.status)) continue;
            anyProcessed = true;
            break;
           }
           if (!anyProcessed) { %>
          <div class="mo-empty">No processed applications yet.</div>
        <% } else { %>
          <div class="mo-processed-list">
          <% for (Application app : applications) {
              if ("Pending".equalsIgnoreCase(app.status)) continue;
              String st = app.status == null ? "" : app.status.trim();
              String badgeClass = "b-Pending";
              if ("Accepted".equalsIgnoreCase(st)) badgeClass = "b-Accepted";
              else if ("Rejected".equalsIgnoreCase(st)) badgeClass = "b-Rejected";
              else if ("Withdrawn".equalsIgnoreCase(st)) badgeClass = "b-Withdrawn";
              String applicantQuery = URLEncoder.encode(app.userId, StandardCharsets.UTF_8);
              String cvName = cvByUserId.get(app.userId);
              String displayName = applicantNamesByUserId.getOrDefault(
                      app.userId != null ? app.userId.trim() : "", app.userId);
          %>
            <article class="mo-processed-card">
              <div class="mo-processed-card__head">
                <div>
                  <h3 class="mo-processed-card__title"><%= app.moduleName %></h3>
                  <p class="mo-processed-card__sub">
                    <%= displayName %> &middot; <%= app.moduleCode %> &middot; <%= app.role %>
                    &middot; <%= app.applicationDate %>
                  </p>
                </div>
                <span class="status-badge <%= badgeClass %>"><%= app.status %></span>
              </div>
              <div class="mo-processed-card__body">
                <div class="mo-pending-side mo-pending-side--actions">
                  <div class="mo-pending-id">Application <%= app.id %></div>
                  <div class="mo-pending-links">
                    <% if (cvName != null) { %>
                      <a class="btn btn-outline" href="<%= ctx %>/cv?userId=<%= applicantQuery %>" target="_blank" rel="noopener">Open CV</a>
                    <% } %>
                    <a class="btn btn-primary" href="<%= ctx %>/mo/applicant-profile?userId=<%= applicantQuery %>">View Profile</a>
                  </div>
                  <% if (app.feedback != null && !app.feedback.isEmpty()) { %>
                  <div class="mo-processed-card__feedback">
                    <span class="mo-processed-card__feedback-label">Your feedback</span>
                    <p><%= app.feedback %></p>
                  </div>
                  <% } else { %>
                  <div class="mo-processed-card__feedback mo-processed-card__feedback--empty">
                    <span class="muted">No feedback recorded for this decision.</span>
                  </div>
                  <% } %>
                </div>
                <div class="mo-pending-side mo-pending-side--ai">
                  <% if (aiEnabled) { %>
                  <div class="mo-ai-advice mo-ai-advice--review" id="mo-ai-review-<%= app.id %>"
                       data-app-id="<%= app.id %>" data-feature="moDecisionReview">
                    <div class="mo-ai-advice-head">
                      <p class="mo-ai-advice-title">AI decision insight</p>
                      <span class="rec-badge-slot"></span>
                    </div>
                    <div class="mo-ai-advice-body">
                      <div class="mo-ai-advice-loading">Reviewing decision…</div>
                      <div class="mo-ai-advice-md" style="display:none;"></div>
                    </div>
                  </div>
                  <% } else { %>
                  <div class="mo-ai-disabled">AI insight is disabled (LM_ENABLED=false).</div>
                  <% } %>
                </div>
              </div>
            </article>
          <% } %>
          </div>
        <% } %>
      </section>
      <% } else { %>
      <section class="card mo-panel">
        <div class="mo-section-head">
          <div>
            <h2 class="section-heading">Applications</h2>
            <p class="mo-section-desc">Review submissions once TAs apply to your jobs.</p>
          </div>
        </div>
        <div class="mo-empty">No applications yet. They will appear here once TAs apply to your jobs.</div>
      </section>
      <% } %>

    </div>
  </div>
</div>
<% if (aiEnabled) { %>
<script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
<script src="<%= ctx %>/static/js/ai-stream.js"></script>
<script>
(function () {
  'use strict';
  var ctx = '<%= ctx %>';
  var maxConcurrent = 2;
  var queue = [];
  var active = 0;

  document.querySelectorAll('.mo-ai-advice[data-app-id]').forEach(function (panel) {
    queue.push({
      appId: panel.getAttribute('data-app-id'),
      feature: panel.getAttribute('data-feature') || 'moWorkloadAdvice',
      panelId: panel.id
    });
  });

  function pumpQueue() {
    while (active < maxConcurrent && queue.length > 0) {
      var item = queue.shift();
      active++;
      startAdviceStream(item, function () {
        active--;
        pumpQueue();
      });
    }
  }

  function applyRecommendationBadge(panel, text, feature) {
    if (!panel || !text) return;
    var slot = panel.querySelector('.rec-badge-slot');
    if (!slot) return;
    var m;
    if (feature === 'moDecisionReview') {
      m = text.match(/\*\*(Aligned|Questionable|Withdrawn|Review)\*\*/i);
      if (!m) {
        m = text.match(/Decision Assessment[^\n]*\n+\s*(Aligned|Questionable|Withdrawn|Review)/i);
      }
    } else {
      m = text.match(/\*\*(Approve|Reject|Caution)\*\*/i);
      if (!m) {
        m = text.match(/Recommendation[^\n]*\n+\s*(Approve|Reject|Caution)/i);
      }
    }
    if (!m) return;
    var label = m[1];
    var badge = document.createElement('span');
    var cls = label.toLowerCase();
    if (feature === 'moDecisionReview') {
      badge.className = 'rec-badge rec-' + cls;
    } else {
      badge.className = 'rec-badge rec-' + cls;
    }
    badge.textContent = label;
    slot.innerHTML = '';
    slot.appendChild(badge);
  }

  function startAdviceStream(item, done) {
    var panel = document.getElementById(item.panelId);
    if (!panel) {
      done();
      return;
    }
    var loadingEl = panel.querySelector('.mo-ai-advice-loading');
    var mdEl = panel.querySelector('.mo-ai-advice-md');
    var text = '';
    var url = ctx + '/api/ai/stream?feature=' + encodeURIComponent(item.feature)
        + '&applicationId=' + encodeURIComponent(item.appId);

    TaAiStream.consume(url, {
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
          loadingEl.textContent = item.feature === 'moDecisionReview'
              ? 'No insight returned.' : 'No advice returned.';
          loadingEl.style.display = 'flex';
        } else {
          applyRecommendationBadge(panel, text, item.feature);
        }
        done();
      },
      onError: function (err) {
        if (loadingEl) {
          loadingEl.className = 'mo-ai-advice-error';
          loadingEl.textContent = err || 'AI request failed.';
          loadingEl.style.display = 'block';
        }
        done();
      }
    });
  }

  if (queue.length > 0) {
    pumpQueue();
  }
})();
</script>
<% } %>
</body>
</html>
