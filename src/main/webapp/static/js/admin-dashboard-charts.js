/**
 * Admin dashboard Chart.js bindings (expects #admin-chart-data JSON).
 */
(function (global) {
  'use strict';

  var CHART_DEFAULTS = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { labels: { font: { size: 12 }, boxWidth: 14 } }
    }
  };

  function readData() {
    var el = document.getElementById('admin-chart-data');
    if (!el || !el.textContent) return null;
    try {
      return JSON.parse(el.textContent.trim());
    } catch (e) {
      return null;
    }
  }

  function seriesLabels(series) {
    if (!series || !series.length) return ['No data'];
    return series.map(function (p) { return p.label; });
  }

  function seriesValues(series) {
    if (!series || !series.length) return [0];
    return series.map(function (p) { return p.value; });
  }

  function mapEntries(obj) {
    var labels = [];
    var values = [];
    if (!obj) return { labels: ['No data'], values: [0] };
    Object.keys(obj).forEach(function (k) {
      labels.push(k);
      values.push(obj[k]);
    });
    if (!labels.length) return { labels: ['No data'], values: [0] };
    return { labels: labels, values: values };
  }

  function lineChart(ctx, labels, data, label, color) {
    return new global.Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: label,
          data: data,
          borderColor: color,
          backgroundColor: color.indexOf('rgba') === 0 ? color : color.replace('rgb(', 'rgba(').replace(')', ', 0.12)'),
          fill: true,
          tension: 0.35,
          pointRadius: 4,
          pointHoverRadius: 6
        }]
      },
      options: Object.assign({}, CHART_DEFAULTS, {
        scales: {
          y: { beginAtZero: true, ticks: { precision: 0 } },
          x: { grid: { display: false } }
        }
      })
    });
  }

  function doughnutChart(ctx, labels, data, colors) {
    return new global.Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: colors,
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: Object.assign({}, CHART_DEFAULTS, {
        cutout: '62%',
        plugins: Object.assign({}, CHART_DEFAULTS.plugins, {
          legend: { position: 'bottom' }
        })
      })
    });
  }

  function barChart(ctx, labels, data, color) {
    return new global.Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Applications',
          data: data,
          backgroundColor: color,
          borderRadius: 8,
          maxBarThickness: 36
        }]
      },
      options: Object.assign({}, CHART_DEFAULTS, {
        indexAxis: 'y',
        scales: {
          x: { beginAtZero: true, ticks: { precision: 0 } },
          y: { grid: { display: false } }
        },
        plugins: Object.assign({}, CHART_DEFAULTS.plugins, {
          legend: { display: false }
        })
      })
    });
  }

  function init() {
    if (typeof global.Chart === 'undefined') return;
    var data = readData();
    if (!data) return;

    var poolCtx = document.getElementById('chartApplicantPool');
    if (poolCtx) {
      lineChart(poolCtx, seriesLabels(data.monthlyApplicantPool),
          seriesValues(data.monthlyApplicantPool),
          'Cumulative applicants', 'rgb(37, 99, 235)');
    }

    var appsCtx = document.getElementById('chartApplications');
    if (appsCtx) {
      lineChart(appsCtx, seriesLabels(data.monthlyApplications),
          seriesValues(data.monthlyApplications),
          'Applications per month', 'rgb(139, 92, 246)');
    }

    var statusCtx = document.getElementById('chartStatus');
    if (statusCtx) {
      var st = mapEntries(data.appsByStatus);
      doughnutChart(statusCtx, st.labels, st.values,
          ['#fbbf24', '#22c55e', '#ef4444', '#94a3b8']);
    }

    var rolesCtx = document.getElementById('chartRoles');
    if (rolesCtx) {
      var roles = mapEntries(data.usersByRole);
      doughnutChart(rolesCtx, roles.labels, roles.values,
          ['#2563eb', '#8b5cf6', '#64748b']);
    }

    var modCtx = document.getElementById('chartModules');
    if (modCtx) {
      barChart(modCtx, seriesLabels(data.topModules), seriesValues(data.topModules),
          'rgba(37, 99, 235, 0.75)');
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})(window);
