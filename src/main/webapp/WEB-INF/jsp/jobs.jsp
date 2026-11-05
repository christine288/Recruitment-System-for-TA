<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.bupt.ta.model.Job" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Job Portal - TA Recruitment</title>
  <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css" />
</head>
<body>
<%
  List<Job> jobs = (List<Job>) request.getAttribute("jobs");
  if (jobs == null) jobs = new java.util.ArrayList<Job>();
  String q = (String) request.getAttribute("q");
  if (q == null) q = "";
  String sortBy = (String) request.getAttribute("sortBy");
  if (sortBy == null || sortBy.isEmpty()) sortBy = "postingDate";
  String escQ = q.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
  String ctx = request.getContextPath();
  boolean aiEnabled = Boolean.TRUE.equals(request.getAttribute("aiEnabled"));
  boolean aiProfileExists = Boolean.TRUE.equals(request.getAttribute("aiProfileExists"));
  boolean aiProfileComplete = Boolean.TRUE.equals(request.getAttribute("aiProfileComplete"));
  boolean aiProfileHasSkills = Boolean.TRUE.equals(request.getAttribute("aiProfileHasSkills"));
  boolean hasJobs = !jobs.isEmpty();
  boolean aiInteractive = aiEnabled && aiProfileExists && aiProfileHasSkills;
  Set recentViewedJobIds = (Set) request.getAttribute("recentViewedJobIds");
  if (recentViewedJobIds == null) recentViewedJobIds = java.util.Collections.emptySet();
%>
<div class="page--top fade-in">
  <div class="layout-xl" id="job-portal-root"
       data-ctx="<%= ctx %>"
       data-can-use-ai="<%= aiInteractive %>"
       data-has-jobs="<%= hasJobs %>">

    <div class="page-header-row">
      <div class="header-left">
        <div class="logo">
          <svg viewBox="0 0 24 24"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
        </div>
        <div>
          <h1 class="page-title">TA Job Portal</h1>
          <p class="page-subtitle">Browse available TA positions and view role details</p>
        </div>
      </div>
      <div style="display:flex;gap:.5rem;flex-wrap:wrap;align-items:center;">
        <a class="link-pill" href="<%= ctx %>/applications">
          <svg viewBox="0 0 24 24"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>
          My applications
        </a>
        <a class="link-pill" href="<%= ctx %>/profile">
          <svg viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          Profile
        </a>
        <a class="link-pill" href="<%= ctx %>/logout">
          <svg viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          Logout
        </a>
      </div>
    </div>

    <form method="get" action="<%= ctx %>/job" class="app-summary-row">
      <div class="job-search-wrap">
        <div class="search-panel">
          <div class="search-row">
            <input type="text" name="q" value="<%= escQ %>" placeholder="Search jobs..." />
            <button type="submit" class="btn-ghost">Search</button>
          </div>
        </div>
      </div>
      <div class="filter-panel filter-panel--sort">
        <div class="filter-panel-label">Sort by</div>
        <div class="sort-row sort-row--panel">
          <select id="sortBy" name="sortBy" onchange="this.form.submit()" aria-label="Sort jobs by">
            <option value="moduleName" <%= "moduleName".equals(sortBy) ? "selected" : "" %>>Module Name</option>
            <option value="postingDate" <%= "postingDate".equals(sortBy) ? "selected" : "" %>>Posting Date</option>
            <option value="activityType" <%= "activityType".equals(sortBy) ? "selected" : "" %>>Activity Type</option>
            <option value="favorited" <%= "favorited".equals(sortBy) ? "selected" : "" %>>Favorited</option>
          </select>
        </div>
      </div>
    </form>

    <section class="job-ai-shell">
      <div class="job-ai-toolbar">
        <div>
          <h2 class="job-ai-title">AI Job Guidance</h2>
          <p class="page-subtitle">Get role recommendations for this filtered list, then inspect missing skills for one selected job.</p>
        </div>
        <div class="job-ai-actions">
          <button type="button" class="btn-ai" id="aiRecBtn" <%= (!aiInteractive || !hasJobs) ? "disabled" : "" %>>
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="14" r="4"/><path d="M12 2a4 4 0 0 1 4 4c0 1.95-1.4 3.58-3.25 3.93"/><path d="M8.24 9.93A4 4 0 0 1 12 2"/><path d="M12 18v4"/><path d="M8 22h8"/>
            </svg>
            AI Job Recommendations
          </button>
          <button type="button" class="btn-ai btn-ai--secondary" id="aiGapBtn" <%= (!aiInteractive || !hasJobs) ? "disabled" : "" %>>
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            Missing Skills for Selected Job
          </button>
        </div>
      </div>

      <% if (!aiEnabled) { %>
        <div class="ai-panel ai-panel--warn job-ai-status">
          <div class="ai-panel-header">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <span>AI temporarily unavailable</span>
          </div>
          <div class="ai-panel-body">
            <p class="job-ai-status-text">You can still browse jobs normally. AI recommendations and skill guidance will appear here when the language model is enabled again.</p>
          </div>
        </div>
      <% } else if (!aiProfileExists) { %>
        <div class="ai-panel ai-panel--warn job-ai-status">
          <div class="ai-panel-header">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <span>Complete your profile to unlock AI</span>
          </div>
          <div class="ai-panel-body">
            <p class="job-ai-status-text">Add your background and skills in your <a href="<%= ctx %>/profile">profile</a> first. Then this page can recommend suitable jobs and explain skill gaps for a selected role.</p>
          </div>
        </div>
      <% } else if (!aiProfileHasSkills) { %>
        <div class="ai-panel ai-panel--warn job-ai-status">
          <div class="ai-panel-header">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <span>Add your skills for better guidance</span>
          </div>
          <div class="ai-panel-body">
            <p class="job-ai-status-text">Add at least one of <strong>Skills</strong> or <strong>Courses completed</strong> in your <a href="<%= ctx %>/profile">profile</a>. AI uses both to recommend jobs and explain missing skills for a selected role.</p>
          </div>
        </div>
      <% } else if (!aiProfileComplete) { %>
        <div class="ai-panel ai-panel--warn job-ai-status">
          <div class="ai-panel-header">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <span>AI is available, but your profile is still partial</span>
          </div>
          <div class="ai-panel-body">
            <p class="job-ai-status-text">You can use the AI tools now. For stronger recommendations, consider filling in courses, availability, and other missing profile details in <a href="<%= ctx %>/profile">your profile</a>.</p>
          </div>
        </div>
      <% } else if (!hasJobs) { %>
        <div class="ai-panel ai-panel--warn job-ai-status">
          <div class="ai-panel-header">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <span>No jobs in the current list</span>
          </div>
          <div class="ai-panel-body">
            <p class="job-ai-status-text">Adjust your search or sorting to load some jobs, and the AI workspace will analyze the visible list.</p>
          </div>
        </div>
      <% } %>

      <div class="job-ai-panels">
        <% if (aiInteractive && hasJobs) { %>
        <div class="ai-panel ai-panel--ok job-ai-selection-panel">
          <div class="ai-panel-header">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
            <span>Selected Job (for Missing-Skills Analysis)</span>
          </div>
          <div class="ai-panel-body">
            <p id="aiSelectedEmpty" class="job-ai-selection-empty">Click <strong>Use for AI Guidance</strong> on a job below to analyze skill gaps for that specific role.</p>
            <div id="aiSelectedSummary" class="job-ai-selected-summary" style="display:none;">
              <div class="job-ai-selected-top">
                <div>
                  <div id="aiSelectedTitle" class="job-ai-selected-title"></div>
                  <div class="job-ai-selected-meta">
                    <span id="aiSelectedType" class="chip" style="display:none;"></span>
                    <span id="aiSelectedCode" class="job-detail-code" style="display:none;"></span>
                  </div>
                </div>
              </div>
              <p id="aiSelectedDesc" class="job-desc" style="margin-bottom:.75rem;"></p>
              <div>
                <p class="job-meta" style="margin-bottom:.375rem;">Required Skills:</p>
                <div id="aiSelectedSkills" class="job-ai-selected-skills"></div>
              </div>
            </div>
          </div>
        </div>
        <% } %>

        <div id="aiRecPanel" class="ai-panel ai-panel--ok" style="display:none;">
          <div class="ai-panel-header">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="14" r="4"/><path d="M12 2a4 4 0 0 1 4 4c0 1.95-1.4 3.58-3.25 3.93"/><path d="M8.24 9.93A4 4 0 0 1 12 2"/>
            </svg>
            <span>AI Recommendations for This List</span>
            <span class="ai-model-tag" id="aiRecModel"></span>
          </div>
          <div class="ai-panel-body">
            <p class="ai-stream-hint" id="aiRecHint" style="display:none;margin:0 0 .5rem;font-size:.8125rem;color:#64748b;">Streaming…</p>
            <div id="aiRecMd" class="ai-md"></div>
            <div id="aiRecErr" class="ai-error" style="display:none;"></div>
          </div>
        </div>

        <div id="aiGapPanel" class="ai-panel ai-panel--warn" style="display:none;">
          <div class="ai-panel-header">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <span>Missing Skills Guidance</span>
            <span id="aiGapTarget" class="ai-model-tag" style="margin-left:.5rem;"></span>
            <span class="ai-model-tag" id="aiGapModel"></span>
          </div>
          <div class="ai-panel-body">
            <p class="ai-stream-hint" id="aiGapHint" style="display:none;margin:0 0 .5rem;font-size:.8125rem;color:#64748b;">Streaming…</p>
            <div id="aiGapMd" class="ai-md"></div>
            <div id="aiGapErr" class="ai-error" style="display:none;"></div>
          </div>
        </div>
      </div>
    </section>

    <div class="app-grid job-portal-grid">
        <% if (jobs.isEmpty()) { %>
          <div class="empty-state">
            <p class="app-name"><%= "favorited".equals(sortBy) ? "No favorited jobs yet" : "No matching jobs found" %></p>
            <p class="page-subtitle"><%= "favorited".equals(sortBy) ? "Save jobs from the detail page to see them here" : "Try adjusting your search criteria" %></p>
          </div>
        <% } else { %>
          <%
            for (Job job : jobs) {
              List<String> skills = job.getRequiredSkills();
              if (skills == null) skills = new java.util.ArrayList<String>();
              String jobId = job.getId() == null ? "" : job.getId();
              String module = job.getModuleName() == null ? "" : job.getModuleName();
              String moduleCode = job.getModuleCode() == null ? "" : job.getModuleCode();
              String act = job.getActivityType() == null ? "" : job.getActivityType();
              String desc = job.getDescription() == null ? "" : job.getDescription();
              String deadline = job.getApplicationDeadline() == null ? "" : job.getApplicationDeadline();
              String numTa = job.getNumberOfTAs() == null ? "" : job.getNumberOfTAs();
              String moduleEsc = module.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
              String moduleAttrEsc = moduleEsc.replace("\"", "&quot;").replace("'", "&#39;");
              String moduleCodeEsc = moduleCode.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
              String moduleCodeAttrEsc = moduleCodeEsc.replace("\"", "&quot;").replace("'", "&#39;");
              String actEsc = act.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
              String actAttrEsc = actEsc.replace("\"", "&quot;").replace("'", "&#39;");
              String descEsc = desc.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
              String descAttrEsc = descEsc.replace("\"", "&quot;").replace("'", "&#39;");
              String deadlineEsc = deadline.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
              String numTaEsc = numTa.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
              String jobIdEsc = jobId.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;");
              java.util.List<String> skillTexts = new java.util.ArrayList<String>();
              for (String skill : skills) {
                if (skill == null || skill.isEmpty()) continue;
                skillTexts.add(skill);
              }
              String skillsJoined = String.join(", ", skillTexts);
              String skillsAttrEsc = skillsJoined.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;");
              boolean recentlyViewed = !jobId.isEmpty() && recentViewedJobIds.contains(jobId);
          %>
            <div class="job-card<%= recentlyViewed ? " job-card--recent-viewed" : "" %>" data-job-card="true" data-job-id="<%= jobIdEsc %>" data-job-title="<%= moduleAttrEsc %>" data-job-code="<%= moduleCodeAttrEsc %>" data-job-type="<%= actAttrEsc %>" data-job-desc="<%= descAttrEsc %>" data-job-skills="<%= skillsAttrEsc %>">
              <% if (recentlyViewed) { %>
                <span class="job-recent-badge">Recently viewed</span>
              <% } %>
              <div class="job-card-header">
                <h3 class="job-card-title"><%= moduleEsc %></h3>
                <span class="chip"><%= actEsc %></span>
              </div>

              <div class="skill-wrap">
                <p class="job-meta" style="margin-bottom:.375rem;">Required Skills:</p>
                <div style="display:flex;flex-wrap:wrap;gap:.375rem;">
                  <%
                    if (skills.isEmpty()) {
                  %>
                    <span class="page-subtitle">No required skills</span>
                  <%
                    } else {
                      for (String s : skills) {
                        if (s == null || s.isEmpty()) continue;
                        String esc = s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
                  %>
                    <span class="chip--skill"><%= esc %></span>
                  <%
                      }
                    }
                  %>
                </div>
              </div>

              <p class="job-desc"><%= descEsc %></p>

              <div class="job-footer">
                <div class="job-meta">
                  <% if (!moduleCodeEsc.isEmpty()) { %>
                    <div>Code: <%= moduleCodeEsc %></div>
                  <% } %>
                  <div>Deadline: <%= deadlineEsc.isEmpty() ? "TBC" : deadlineEsc %></div>
                  <div><%= numTaEsc.isEmpty() ? "2" : numTaEsc %> positions</div>
                </div>
                <div class="job-card-actions">
                  <button type="button" class="btn-ghost btn-sm job-ai-select" <%= (jobId.isEmpty() || !aiInteractive) ? "disabled" : "" %>>
                    Use for AI Guidance
                  </button>
                  <a href="${pageContext.request.contextPath}/job?id=<%= jobId %>" class="btn btn-primary btn-sm">
                    View Details
                  </a>
                </div>
              </div>
            </div>
          <%
            }
          %>
        <% } %>
    </div>

  </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/marked@12.0.2/marked.min.js"></script>
<script src="<%= ctx %>/static/js/ai-stream.js"></script>
<script>
(function () {
  var root = document.getElementById('job-portal-root');
  var ctx = root ? root.getAttribute('data-ctx') || '' : '';
  var canUseAi = root && root.getAttribute('data-can-use-ai') === 'true';
  var hasJobs = root && root.getAttribute('data-has-jobs') === 'true';
  var recBtn = document.getElementById('aiRecBtn');
  var gapBtn = document.getElementById('aiGapBtn');
  var cards = Array.prototype.slice.call(document.querySelectorAll('[data-job-card="true"]'));
  var selectedCard = null;
  var selectedJobId = '';
  var selectedTitle = '';
  var selectedEmptyEl = document.getElementById('aiSelectedEmpty');
  var selectedSummaryEl = document.getElementById('aiSelectedSummary');
  var selectedTitleEl = document.getElementById('aiSelectedTitle');
  var selectedTypeEl = document.getElementById('aiSelectedType');
  var selectedCodeEl = document.getElementById('aiSelectedCode');
  var selectedDescEl = document.getElementById('aiSelectedDesc');
  var selectedSkillsEl = document.getElementById('aiSelectedSkills');
  var recPanel = document.getElementById('aiRecPanel');
  var recMdEl = document.getElementById('aiRecMd');
  var recErrEl = document.getElementById('aiRecErr');
  var recModelEl = document.getElementById('aiRecModel');
  var recHintEl = document.getElementById('aiRecHint');
  var gapPanel = document.getElementById('aiGapPanel');
  var gapMdEl = document.getElementById('aiGapMd');
  var gapErrEl = document.getElementById('aiGapErr');
  var gapModelEl = document.getElementById('aiGapModel');
  var gapHintEl = document.getElementById('aiGapHint');
  var gapTargetEl = document.getElementById('aiGapTarget');

  if (!window.TaAiStream) return;

  function friendlyAiError(message, fallback) {
    var text = message || '';
    var lower = text.toLowerCase();
    if (lower.indexOf('lm_enabled=false') >= 0 || lower.indexOf('disabled') >= 0) {
      return 'AI guidance is currently unavailable. You can still browse jobs and try again later.';
    }
    if (lower.indexOf('complete your profile') >= 0) {
      return 'Please complete your profile, especially your skills, before using this AI guidance.';
    }
    if (lower.indexOf('job not found') >= 0) {
      return 'The selected job is no longer available. Please choose another job from the list.';
    }
    return text || fallback || 'AI guidance could not be loaded.';
  }

  function setBusy(isBusy) {
    if (recBtn) recBtn.disabled = isBusy || !canUseAi || !hasJobs;
    if (gapBtn) gapBtn.disabled = isBusy || !canUseAi || !selectedJobId;
  }

  function clearSkillChips() {
    while (selectedSkillsEl && selectedSkillsEl.firstChild) {
      selectedSkillsEl.removeChild(selectedSkillsEl.firstChild);
    }
  }

  function renderSkillChips(rawSkills) {
    clearSkillChips();
    if (!selectedSkillsEl) return;
    var parts = (rawSkills || '').split(',');
    var hasAny = false;
    for (var i = 0; i < parts.length; i++) {
      var skill = parts[i].trim();
      if (!skill) continue;
      hasAny = true;
      var chip = document.createElement('span');
      chip.className = 'chip--skill';
      chip.textContent = skill;
      selectedSkillsEl.appendChild(chip);
    }
    if (!hasAny) {
      var empty = document.createElement('span');
      empty.className = 'page-subtitle';
      empty.textContent = 'No required skills listed for this job.';
      selectedSkillsEl.appendChild(empty);
    }
  }

  function updateSelectedJob(card) {
    if (selectedCard) {
      selectedCard.classList.remove('job-card--selected');
    }
    selectedCard = card && card.getAttribute('data-job-id') ? card : null;
    selectedJobId = selectedCard ? (selectedCard.getAttribute('data-job-id') || '') : '';
    selectedTitle = selectedCard ? (selectedCard.getAttribute('data-job-title') || '') : '';

    if (!selectedCard || !selectedJobId) {
      if (selectedEmptyEl) selectedEmptyEl.style.display = 'block';
      if (selectedSummaryEl) selectedSummaryEl.style.display = 'none';
      if (gapTargetEl) gapTargetEl.textContent = '';
      setBusy(false);
      return;
    }

    selectedCard.classList.add('job-card--selected');
    if (selectedEmptyEl) selectedEmptyEl.style.display = 'none';
    if (selectedSummaryEl) selectedSummaryEl.style.display = 'block';
    if (selectedTitleEl) selectedTitleEl.textContent = selectedTitle || 'Selected job';

    var type = selectedCard.getAttribute('data-job-type') || '';
    if (selectedTypeEl) {
      selectedTypeEl.textContent = type;
      selectedTypeEl.style.display = type ? 'inline-flex' : 'none';
    }

    var code = selectedCard.getAttribute('data-job-code') || '';
    if (selectedCodeEl) {
      selectedCodeEl.textContent = code;
      selectedCodeEl.style.display = code ? 'inline-block' : 'none';
    }

    if (selectedDescEl) {
      var desc = selectedCard.getAttribute('data-job-desc') || '';
      selectedDescEl.textContent = desc || 'No description available for this job.';
    }

    renderSkillChips(selectedCard.getAttribute('data-job-skills') || '');
    if (gapTargetEl) {
      gapTargetEl.textContent = selectedTitle ? 'For ' + selectedTitle : '';
    }
    setBusy(false);
  }

  cards.forEach(function (card) {
    var btn = card.querySelector('.job-ai-select');
    if (!btn) return;
    btn.addEventListener('click', function () {
      updateSelectedJob(card);
    });
  });

  if (cards.length && canUseAi) {
    updateSelectedJob(cards[0]);
  } else {
    updateSelectedJob(null);
  }

  if (recBtn) {
    recBtn.addEventListener('click', function () {
      if (!canUseAi || !hasJobs) return;
      var qInput = document.querySelector('.app-summary-row input[name="q"]');
      var sortSel = document.getElementById('sortBy');
      var q = qInput ? qInput.value : '';
      var sortBy = sortSel ? sortSel.value : 'postingDate';

      recPanel.style.display = 'block';
      recPanel.className = 'ai-panel ai-panel--ok';
      recErrEl.style.display = 'none';
      recErrEl.textContent = '';
      recMdEl.innerHTML = '';
      recModelEl.textContent = '';
      recHintEl.style.display = 'block';
      setBusy(true);

      var text = '';
      var params = new URLSearchParams();
      params.set('feature', 'recommendation');
      params.set('q', q);
      params.set('sortBy', sortBy);

      TaAiStream.consume(ctx + '/api/ai/stream?' + params.toString(), {
        onMeta: function (m) { recModelEl.textContent = m || ''; },
        onDelta: function (d) {
          text += d;
          TaAiStream.renderMarkdown(recMdEl, text, typeof marked !== 'undefined' ? marked : null);
        },
        onDone: function () {
          recHintEl.style.display = 'none';
          setBusy(false);
        },
        onError: function (e) {
          recHintEl.style.display = 'none';
          recErrEl.style.display = 'block';
          recErrEl.textContent = friendlyAiError(e, 'AI recommendations could not be loaded.');
          recPanel.className = 'ai-panel ai-panel--err';
          setBusy(false);
        }
      });
    });
  }

  if (gapBtn) {
    gapBtn.addEventListener('click', function () {
      if (!canUseAi) return;
      if (!selectedJobId) {
        gapPanel.style.display = 'block';
        gapPanel.className = 'ai-panel ai-panel--warn';
        gapMdEl.innerHTML = '';
        gapErrEl.style.display = 'block';
        gapErrEl.textContent = 'Choose a job from the list first, then run missing-skills guidance for that role.';
        gapHintEl.style.display = 'none';
        return;
      }

      gapPanel.style.display = 'block';
      gapPanel.className = 'ai-panel ai-panel--warn';
      gapErrEl.style.display = 'none';
      gapErrEl.textContent = '';
      gapMdEl.innerHTML = '';
      gapModelEl.textContent = '';
      gapHintEl.style.display = 'block';
      if (gapTargetEl) {
        gapTargetEl.textContent = selectedTitle ? 'For ' + selectedTitle : '';
      }
      setBusy(true);

      var text = '';
      var params = new URLSearchParams();
      params.set('feature', 'missingSkills');
      params.set('jobId', selectedJobId);

      TaAiStream.consume(ctx + '/api/ai/stream?' + params.toString(), {
        onMeta: function (m) { gapModelEl.textContent = m || ''; },
        onDelta: function (d) {
          text += d;
          TaAiStream.renderMarkdown(gapMdEl, text, typeof marked !== 'undefined' ? marked : null);
        },
        onDone: function () {
          gapHintEl.style.display = 'none';
          setBusy(false);
        },
        onError: function (e) {
          gapHintEl.style.display = 'none';
          gapErrEl.style.display = 'block';
          gapErrEl.textContent = friendlyAiError(e, 'Missing-skills guidance could not be loaded.');
          gapPanel.className = 'ai-panel ai-panel--err';
          setBusy(false);
        }
      });
    });
  }
})();
</script>
</body>
</html>
