<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.bupt.ta.model.Job" %>
<%@ page import="java.util.List" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Job Details - TA Recruitment</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css" />
</head>
<body>
<%
  Job job = (Job) request.getAttribute("job");
  Boolean jobFavorited = (Boolean) request.getAttribute("jobFavorited");
  if (jobFavorited == null) jobFavorited = Boolean.FALSE;
  String ctx = request.getContextPath();
  String qErrRaw = request.getParameter("err");
  String qMsgRaw = request.getParameter("msg");
  String qErr = qErrRaw == null ? "" : qErrRaw.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
  String qMsg = qMsgRaw == null ? "" : qMsgRaw.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
%>

<div class="page--top fade-in">
  <div class="layout-xl">

    <% if (job == null) { %>
      <div class="page-header-row">
        <div class="header-left">
          <div class="logo">
            <svg viewBox="0 0 24 24"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
          </div>
          <div>
            <h1 class="page-title">Job details</h1>
            <p class="page-subtitle">This listing could not be loaded</p>
          </div>
        </div>
        <a class="link-pill" href="<%= ctx %>/job">
          <svg viewBox="0 0 24 24"><polyline points="15 18 9 12 15 6"/><path d="M20 12H9"/></svg>
          Back to Job List
        </a>
      </div>
      <div class="card job-detail-card">
        <div class="alert alert-error">Job Not Found</div>
      </div>
    <% } else { %>
      <input type="hidden" id="ta-job-id" value="<%= job.getId() == null ? "" : job.getId().replace("&","&amp;").replace("\"","&quot;") %>" />
      <div class="page-header-row">
        <div class="header-left">
          <div class="logo">
            <svg viewBox="0 0 24 24"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
          </div>
          <div>
            <h1 class="page-title"><%= job.getModuleName() == null ? "" : job.getModuleName().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;") %></h1>
            <p class="page-subtitle job-detail-header-meta">
              <span class="chip"><%= job.getActivityType() == null ? "" : job.getActivityType().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;") %></span>
              <% if (job.getModuleCode() != null && !job.getModuleCode().isEmpty()) { %>
                <span class="job-detail-code"><%= job.getModuleCode().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;") %></span>
              <% } %>
            </p>
          </div>
        </div>
        <div style="display:flex;gap:.5rem;flex-wrap:wrap;align-items:center;">
        <a class="link-pill" href="<%= ctx %>/applications">
          <svg viewBox="0 0 24 24"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>
          My applications
        </a>
        <a class="link-pill" href="<%= ctx %>/job">
          <svg viewBox="0 0 24 24"><polyline points="15 18 9 12 15 6"/><path d="M20 12H9"/></svg>
          Back to Job List
        </a>
        </div>
      </div>

      <% if (!qErr.isEmpty()) { %>
      <div class="alert alert-error" style="margin-bottom:1rem;"><%= qErr %></div>
      <% } %>
      <% if (!qMsg.isEmpty()) { %>
      <div class="alert alert-success" style="margin-bottom:1rem;"><%= qMsg %></div>
      <% } %>

      <%
        com.bupt.ta.model.JobApplicationStats jobAppStats = (com.bupt.ta.model.JobApplicationStats) request.getAttribute("jobAppStats");
        Integer jobTaCap = (Integer) request.getAttribute("jobTaCapacity");
        if (jobTaCap == null) jobTaCap = Integer.valueOf(1);
        boolean jobSlotsFull = Boolean.TRUE.equals(request.getAttribute("jobSlotsFull"));
        boolean userActiveApplication = Boolean.TRUE.equals(request.getAttribute("userActiveApplicationForJob"));
        boolean taProfileComplete = Boolean.TRUE.equals(request.getAttribute("taProfileComplete"));
        boolean statsLoadError = Boolean.TRUE.equals(request.getAttribute("jobAppStatsError"));
        boolean applyDisabled = !taProfileComplete || jobSlotsFull || userActiveApplication;
        int pnd = jobAppStats != null ? jobAppStats.pending : 0;
        int acc = jobAppStats != null ? jobAppStats.accepted : 0;
        int rej = jobAppStats != null ? jobAppStats.rejected : 0;
        int wdr = jobAppStats != null ? jobAppStats.withdrawn : 0;
      %>
      <% if (jobSlotsFull && qErr.isEmpty()) { %>
      <div class="alert alert-error" style="margin-bottom:1rem;">All TA slots for this job are filled. New applications are not accepted.</div>
      <% } %>

      <div class="job-detail-shell">
        <div class="job-detail-main card job-detail-card">
          <div class="detail-section detail-section--first">
            <h2>Job Description</h2>
            <p><%= job.getDescription() == null ? "" : job.getDescription().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;") %></p>
          </div>

          <div class="detail-section">
            <h2>Required Skills</h2>
            <%
              List<String> skills = job.getRequiredSkills();
              boolean hasSkills = skills != null && !skills.isEmpty();
            %>
            <% if (hasSkills) { %>
              <ul class="skill-list">
                <%
                  for (String skill : skills) {
                    if (skill == null || skill.isEmpty()) continue;
                    String esc = skill
                      .replace("&", "&amp;")
                      .replace("\"", "&quot;")
                      .replace("<", "&lt;")
                      .replace(">", "&gt;")
                      .replace("'", "&#39;");
                %>
                  <li>
                    <span class="bullet">&bull;</span>
                    <span><%= esc %></span>
                  </li>
                <%
                  }
                %>
              </ul>
            <% } else { %>
              <span class="page-subtitle">No required skills listed.</span>
            <% } %>
          </div>

          <div class="detail-section">
            <h2>Schedule</h2>
            <div class="info-box">
              <%
                List<String> schedule = job.getSchedule();
                if (schedule == null) schedule = new java.util.ArrayList<String>();
                if (schedule.isEmpty()) {
              %>
                <div class="page-subtitle">Schedule to be confirmed</div>
              <%
                } else {
                  for (String slot : schedule) {
                    if (slot == null || slot.isEmpty()) continue;
              %>
                <div class="app-meta" style="margin-bottom:.375rem;">
                  <span>&#128337;</span>
                  <span><%= slot.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;") %></span>
                </div>
              <%
                  }
                }
              %>
              <div style="margin-top:.625rem;padding-top:.5rem;border-top:1px solid #d1d5db;" class="job-meta">
                <strong>Duration:</strong> <%= (job.getDuration() == null || job.getDuration().isEmpty()) ? "One semester" : job.getDuration().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;") %>
              </div>
            </div>
          </div>

          <div class="detail-section detail-section--flush">
            <h2>AI Skill Analysis</h2>
            <p class="page-subtitle" style="margin-bottom:.75rem;">Streaming Markdown output. Complete your profile first.</p>
            <div style="display:flex;flex-wrap:wrap;gap:.5rem;align-items:center;margin-bottom:.75rem;">
              <button type="button" class="btn-ai" id="aiAnalyzeBtn">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="14" r="4"/><path d="M12 2a4 4 0 0 1 4 4c0 1.95-1.4 3.58-3.25 3.93"/><path d="M8.24 9.93A4 4 0 0 1 12 2"/><path d="M12 18v4"/><path d="M8 22h8"/>
                </svg>
                Analyze My Skill Match
              </button>
              <button type="button" class="btn-ai btn-ai--sm" id="aiReanalyzeBtn" style="display:none;">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 4v6h6"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>
                Re-analyze
              </button>
            </div>
            <div id="aiSkillPanels" style="display:none;">
              <div id="aiMatchPanel" class="ai-panel ai-panel--ok" style="margin-bottom:1rem;">
                <div class="ai-panel-header">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                  <span>Skill Match</span>
                  <span class="ai-model-tag" id="aiMatchModel"></span>
                </div>
                <div class="ai-panel-body">
                  <p class="ai-stream-hint" id="aiMatchHint" style="display:none;margin:0 0 .5rem;font-size:.8125rem;color:#64748b;">Streaming…</p>
                  <div id="aiMatchMd" class="ai-md"></div>
                  <div id="aiMatchErr" class="ai-error" style="display:none;"></div>
                </div>
              </div>
              <div id="aiGapPanel" class="ai-panel ai-panel--warn">
                <div class="ai-panel-header">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                  <span>Skill Gaps</span>
                  <span class="ai-model-tag" id="aiGapModel"></span>
                </div>
                <div class="ai-panel-body">
                  <p class="ai-stream-hint" id="aiGapHint" style="display:none;margin:0 0 .5rem;font-size:.8125rem;color:#64748b;">Streaming…</p>
                  <div id="aiGapMd" class="ai-md"></div>
                  <div id="aiGapErr" class="ai-error" style="display:none;"></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <aside class="job-detail-aside card job-detail-card">
          <div class="job-aside-block">
            <% if (statsLoadError) { %>
            <div class="alert alert-error" style="margin-bottom:.75rem;">Could not load live application counts.</div>
            <% } %>
            <div class="info-box info-box--blue">
              <div class="info-label">Positions Available</div>
              <div class="info-value"><%= jobTaCap %> TA<%= jobTaCap == 1 ? "" : "s" %> funded</div>
              <div class="page-subtitle" style="margin-top:.5rem;margin-bottom:0;">Accepted hires: <strong><%= acc %></strong> / <%= jobTaCap %>. New applications <%= jobSlotsFull ? "are closed (slots full)." : "are open while slots remain." %></div>
            </div>
            <div class="info-box" style="margin-top:.75rem;border:1px solid #e2e8f0;background:rgba(248,250,252,.85);">
              <div class="info-label">Applicants on this listing</div>
              <div class="page-subtitle" style="margin:.35rem 0;">Visible to all signed-in TAs (same module + code).</div>
              <ul style="margin:.5rem 0 0;padding-left:1.1rem;line-height:1.6;font-size:.9375rem;color:#334155;">
                <li><strong><%= pnd %></strong> pending</li>
                <li><strong><%= acc %></strong> accepted</li>
                <li><strong><%= rej %></strong> rejected</li>
                <li><strong><%= wdr %></strong> withdrawn</li>
              </ul>
            </div>
            <div class="info-box info-box--amber" style="margin-top:.75rem;">
              <div class="info-label">Application Deadline</div>
              <div class="info-value"><%= (job.getApplicationDeadline() == null || job.getApplicationDeadline().isEmpty()) ? "TBC" : job.getApplicationDeadline().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;") %></div>
            </div>
          </div>
          <div class="detail-actions detail-actions--aside">
            <form method="post" action="<%= ctx %>/applications" id="applyForm">
              <input type="hidden" name="csrfToken" value="<%= com.bupt.ta.security.CsrfFilter.csrfToken(request) %>" />
              <input type="hidden" name="jobId"      value="<%= job.getId() == null ? "" : job.getId() %>" />
              <input type="hidden" name="moduleName" value="<%= job.getModuleName() == null ? "" : job.getModuleName().replace("\"","&quot;") %>" />
              <input type="hidden" name="moduleCode" value="<%= job.getModuleCode() == null ? "" : job.getModuleCode().replace("\"","&quot;") %>" />
              <input type="hidden" name="role"       value="<%= job.getActivityType() == null ? "Teaching Assistant" : job.getActivityType().replace("\"","&quot;") %>" />
              <% if (applyDisabled) { %>
              <button type="button" class="btn btn-primary apply-blocked-btn" aria-describedby="applyBlockFeedback"
                <% if (jobSlotsFull) { %>data-block-reason="slots-full"<% } else if (!taProfileComplete) { %>data-block-reason="profile"<% } else { %>data-block-reason="duplicate"<% } %>>
                Apply for Job
              </button>
              <% } else { %>
              <button type="submit" class="btn btn-primary">Apply for Job</button>
              <% } %>
            </form>
            <div id="applyBlockFeedback" class="alert alert-error" style="display:none;margin-top:.75rem;" role="alert"></div>
            <% if (applyDisabled) { %>
            <div class="alert <%= jobSlotsFull ? "alert-error" : "alert-info" %>" style="margin-top:.75rem;">
              <% if (!taProfileComplete) { %><div>Complete your <a href="<%= ctx %>/profile">profile</a> before applying.</div><% } %>
              <% if (jobSlotsFull) { %><div>TA slots are full for this listing; applications are closed.</div><% } %>
              <% if (userActiveApplication) { %><div>You already have an active application for this job. Track it under <a href="<%= ctx %>/applications">My applications</a>.</div><% } %>
            </div>
            <% } %>
            <form method="post" action="<%= ctx %>/job">
              <input type="hidden" name="csrfToken" value="<%= com.bupt.ta.security.CsrfFilter.csrfToken(request) %>" />
              <input type="hidden" name="action" value="toggleFavorite" />
              <input type="hidden" name="jobId" value="<%= job.getId() == null ? "" : job.getId() %>" />
              <button type="submit" class="btn btn-favorite"><%= jobFavorited.booleanValue() ? "Remove from Favorites" : "Save Job" %></button>
            </form>
          </div>
        </aside>
      </div>
    <% } %>

  </div>
</div>
<% if (job != null) { %>
<script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
<script src="<%= ctx %>/static/js/ai-stream.js"></script>
<script>
(function () {
  var ctx = '<%= ctx %>';
  var jobIdEl = document.getElementById('ta-job-id');
  var jobId = jobIdEl ? jobIdEl.value : '';
  var analyzeBtn = document.getElementById('aiAnalyzeBtn');
  var reBtn = document.getElementById('aiReanalyzeBtn');
  var panels = document.getElementById('aiSkillPanels');
  if (!analyzeBtn || !window.TaAiStream || !jobId) return;

  function resetPanels() {
    document.getElementById('aiMatchMd').innerHTML = '';
    document.getElementById('aiGapMd').innerHTML = '';
    document.getElementById('aiMatchErr').style.display = 'none';
    document.getElementById('aiMatchErr').textContent = '';
    document.getElementById('aiGapErr').style.display = 'none';
    document.getElementById('aiGapErr').textContent = '';
    document.getElementById('aiMatchModel').textContent = '';
    document.getElementById('aiGapModel').textContent = '';
    document.getElementById('aiMatchPanel').className = 'ai-panel ai-panel--ok';
    document.getElementById('aiGapPanel').className = 'ai-panel ai-panel--warn';
  }

  function runStreams() {
    panels.style.display = 'block';
    reBtn.style.display = 'inline-flex';
    resetPanels();
    var matchHint = document.getElementById('aiMatchHint');
    var gapHint = document.getElementById('aiGapHint');
    var matchErr = document.getElementById('aiMatchErr');
    var gapErr = document.getElementById('aiGapErr');
    var matchPanel = document.getElementById('aiMatchPanel');
    var gapPanel = document.getElementById('aiGapPanel');
    matchHint.style.display = 'block';
    gapHint.textContent = 'Waiting for Skill Match…';
    gapHint.style.display = 'block';
    analyzeBtn.disabled = true;
    reBtn.disabled = true;

    var matchText = '';
    var gapText = '';
    function finishAnalysis() {
      analyzeBtn.disabled = false;
      reBtn.disabled = false;
    }

    function startGapStream() {
      gapHint.textContent = 'Streaming…';
      var paramsG = new URLSearchParams();
      paramsG.set('feature', 'missingSkills');
      paramsG.set('jobId', jobId);
      TaAiStream.consume(ctx + '/api/ai/stream?' + paramsG.toString(), {
        onMeta: function (m) { document.getElementById('aiGapModel').textContent = m || ''; },
        onDelta: function (d) {
          gapText += d;
          TaAiStream.renderMarkdown(document.getElementById('aiGapMd'), gapText, typeof marked !== 'undefined' ? marked : null);
        },
        onDone: function () {
          gapHint.style.display = 'none';
          if (!gapText.trim()) {
            gapErr.style.display = 'block';
            gapErr.textContent = 'No skill-gap guidance was returned. Please re-analyze.';
          }
          finishAnalysis();
        },
        onError: function (e) {
          gapHint.style.display = 'none';
          gapErr.style.display = 'block';
          gapErr.textContent = e || 'Skill-gap guidance failed. Please re-analyze.';
          gapPanel.className = 'ai-panel ai-panel--err';
          finishAnalysis();
        }
      });
    }

    var paramsM = new URLSearchParams();
    paramsM.set('feature', 'skillMatch');
    paramsM.set('jobId', jobId);

    TaAiStream.consume(ctx + '/api/ai/stream?' + paramsM.toString(), {
      onMeta: function (m) { document.getElementById('aiMatchModel').textContent = m || ''; },
      onDelta: function (d) {
        matchText += d;
        TaAiStream.renderMarkdown(document.getElementById('aiMatchMd'), matchText, typeof marked !== 'undefined' ? marked : null);
      },
      onDone: function () {
        matchHint.style.display = 'none';
        if (!matchText.trim()) {
          matchErr.style.display = 'block';
          matchErr.textContent = 'No skill-match analysis was returned. Please re-analyze.';
          matchPanel.className = 'ai-panel ai-panel--warn';
        }
        startGapStream();
      },
      onError: function (e) {
        matchHint.style.display = 'none';
        matchErr.style.display = 'block';
        matchErr.textContent = e || 'Skill-match analysis failed. Please re-analyze.';
        matchPanel.className = 'ai-panel ai-panel--err';
        startGapStream();
      }
    });
  }

  analyzeBtn.addEventListener('click', function () {
    runStreams();
  });
  reBtn.addEventListener('click', function () {
    runStreams();
  });
})();
</script>
<script>
(function () {
  var feedback = document.getElementById('applyBlockFeedback');
  var blockedBtns = document.querySelectorAll('.apply-blocked-btn');
  if (!feedback || !blockedBtns.length) return;

  var messages = {
    'slots-full': 'All TA slots for this job are filled. New applications are not accepted.',
    'profile': 'Please complete your profile before applying for a job.',
    'duplicate': 'You already have an active application for this job.'
  };

  blockedBtns.forEach(function (btn) {
    btn.addEventListener('click', function () {
      var reason = btn.getAttribute('data-block-reason') || 'slots-full';
      var msg = messages[reason] || messages['slots-full'];
      feedback.textContent = msg;
      feedback.style.display = 'block';
      feedback.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    });
  });
})();
</script>
<% } %>
</body>
</html>
