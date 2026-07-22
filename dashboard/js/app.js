(function () {
  const sharedStyleId = 'oit-dashboard-shared-style';
  if (!document.getElementById(sharedStyleId)) {
    const style = document.createElement('style');
    style.id = sharedStyleId;
    style.textContent = `
      body { background: var(--tblr-bg-surface-secondary); }
      .navbar-vertical.navbar-dark { background: linear-gradient(180deg, #0f172a 0%, #111827 48%, #1f2937 100%); }
      .navbar-brand-title { font-weight: 700; letter-spacing: .02em; }
      .brand-mark { width: 2rem; height: 2rem; border-radius: .75rem; display: inline-flex; align-items: center; justify-content: center; background: rgba(255,255,255,.12); margin-right: .75rem; }
      .page-wrapper { min-height: 100vh; }
      .card { border-radius: 1rem; }
      .table > :not(caption) > * > * { vertical-align: middle; }
      .metric-card .card-body { min-height: 128px; }
      .icon-tile { width: 3rem; height: 3rem; border-radius: 1rem; display: inline-flex; align-items: center; justify-content: center; font-size: 1.25rem; }
      .page-shell .page-header { margin-bottom: 1rem; }
      .badge-soft { background: rgba(32, 107, 196, 0.12); color: var(--tblr-blue); }
      .table-clickable tbody tr { cursor: pointer; }
      .timeline-mini { border-left: 2px solid rgba(98, 105, 118, .18); margin-left: .75rem; padding-left: 1rem; }
      .timeline-mini-item { position: relative; margin-bottom: 1rem; }
      .timeline-mini-item::before { content: ''; position: absolute; left: -1.45rem; top: .35rem; width: .65rem; height: .65rem; border-radius: 999px; background: var(--tblr-blue); }
      .badge-gallery .card { transition: transform .15s ease, box-shadow .15s ease; }
      .badge-gallery .card:hover { transform: translateY(-2px); box-shadow: 0 0.75rem 2rem rgba(15, 23, 42, .08); }
      .login-shell { min-height: 100vh; background: radial-gradient(circle at top right, rgba(32,107,196,.2), transparent 28%), linear-gradient(135deg, #0f172a 0%, #1e293b 35%, #f8fafc 35%, #f8fafc 100%); }
      .login-card { width: min(100%, 440px); border-radius: 1.25rem; }
      .list-group-item.unread { border-left: 4px solid var(--tblr-blue); background: rgba(32,107,196,.03); }
      .data-source-note { position: sticky; top: .5rem; z-index: 5; }
      @media (max-width: 991.98px) {
        .login-shell { background: #f8fafc; }
      }
    `;
    document.head.appendChild(style);
  }

  const demoData = {
    patients: [
      {
        id: 'pt-10045',
        name: 'Alice Johnson',
        mrn: 'P10045',
        dob: '2014-03-11',
        allergen: 'Peanut',
        protocol: 'Peanut Escalation v3',
        currentDose: '1,200 mcg',
        currentDoseValue: 1200,
        nextUpdoseDate: '2026-07-25',
        status: 'Active',
        created_at: '2026-01-12T09:15:00Z'
      },
      {
        id: 'pt-10082',
        name: 'Noah Ramirez',
        mrn: 'P10082',
        dob: '2012-11-22',
        allergen: 'Milk',
        protocol: 'Milk Maintenance Pathway',
        currentDose: '800 mcg',
        currentDoseValue: 800,
        nextUpdoseDate: '2026-07-24',
        status: 'Observation',
        created_at: '2026-02-03T13:40:00Z'
      },
      {
        id: 'pt-10117',
        name: 'Emma Patel',
        mrn: 'P10117',
        dob: '2016-06-04',
        allergen: 'Egg',
        protocol: 'Egg Low-Slow Protocol',
        currentDose: '300 mcg',
        currentDoseValue: 300,
        nextUpdoseDate: '2026-07-30',
        status: 'Stable',
        created_at: '2026-03-21T11:05:00Z'
      }
    ],
    dosePlans: [
      { patientId: 'pt-10045', date: '2026-07-15', dose: 600, form: 'Suspension', status: 'Completed' },
      { patientId: 'pt-10045', date: '2026-07-18', dose: 900, form: 'Suspension', status: 'Completed' },
      { patientId: 'pt-10045', date: '2026-07-25', dose: 1200, form: 'Syringe', status: 'Scheduled' },
      { patientId: 'pt-10082', date: '2026-07-16', dose: 600, form: 'Syringe', status: 'Completed' },
      { patientId: 'pt-10082', date: '2026-07-24', dose: 800, form: 'Syringe', status: 'Observation' },
      { patientId: 'pt-10117', date: '2026-07-14', dose: 150, form: 'Powder Capsule', status: 'Completed' },
      { patientId: 'pt-10117', date: '2026-07-30', dose: 300, form: 'Powder Capsule', status: 'Scheduled' }
    ],
    updoseDecisions: [
      { patientId: 'pt-10045', decided_at: '2026-07-18T09:10:00Z', current_dose: '900 mcg', next_dose: '1,200 mcg', outcome: 'Approved', clinician: 'Dr. Chen', notes: 'No adverse reactions during observation.' },
      { patientId: 'pt-10082', decided_at: '2026-07-17T10:30:00Z', current_dose: '600 mcg', next_dose: '800 mcg', outcome: 'Caution', clinician: 'Dr. Alvarez', notes: 'Proceed with extended 2-hour post-dose observation.' },
      { patientId: 'pt-10117', decided_at: '2026-07-14T08:55:00Z', current_dose: '150 mcg', next_dose: '300 mcg', outcome: 'Approved', clinician: 'Dr. Morgan', notes: 'Continue food co-ingestion guidance.' }
    ],
    syringes: [
      { id: 'SYR-2041', patientId: 'pt-10045', dose: '1,200 mcg', form: 'Suspension', date_drawn: '2026-07-22T08:00:00Z', scheduled_date: '2026-07-25T09:00:00Z', final_expiry: '2026-07-25T10:00:00Z', status: 'Ready' },
      { id: 'SYR-2029', patientId: 'pt-10082', dose: '800 mcg', form: 'Suspension', date_drawn: '2026-07-22T06:30:00Z', scheduled_date: '2026-07-24T09:00:00Z', final_expiry: '2026-07-23T05:45:00Z', status: 'Expiring Soon' },
      { id: 'SYR-1988', patientId: 'pt-10117', dose: '300 mcg', form: 'Powder Slurry', date_drawn: '2026-07-20T07:10:00Z', scheduled_date: '2026-07-21T08:00:00Z', final_expiry: '2026-07-21T09:00:00Z', status: 'Expired' },
      { id: 'SYR-1974', patientId: 'pt-10045', dose: '900 mcg', form: 'Suspension', date_drawn: '2026-07-18T08:00:00Z', scheduled_date: '2026-07-18T09:00:00Z', final_expiry: '2026-07-18T11:00:00Z', status: 'Used' }
    ],
    cartons: [
      { id: 'CRT-904', patientId: 'pt-10045', lot: 'LOT-PN-1187', expiry: '2026-08-12', date_opened: '2026-07-10', status: 'Open' },
      { id: 'CRT-887', patientId: 'pt-10082', lot: 'LOT-ML-4421', expiry: '2026-07-24', date_opened: '2026-07-05', status: 'Expiring Soon' },
      { id: 'CRT-873', patientId: 'pt-10117', lot: 'LOT-EG-7720', expiry: '2026-07-18', date_opened: '2026-06-30', status: 'Expired' }
    ],
    powderSupplies: [
      { id: 'PWD-31', patientId: 'pt-10045', powder_type: 'Peanut Protein', lot: 'PP-774', remaining_grams: 48, expiry: '2026-09-01', status: 'In Stock' },
      { id: 'PWD-22', patientId: 'pt-10082', powder_type: 'Milk Powder', lot: 'MP-512', remaining_grams: 9, expiry: '2026-07-27', status: 'Low Stock' },
      { id: 'PWD-17', patientId: 'pt-10117', powder_type: 'Egg White Powder', lot: 'EW-645', remaining_grams: 0, expiry: '2026-07-12', status: 'Out' }
    ],
    mar: [
      { date_time: '2026-07-18T09:00:00Z', patientId: 'pt-10045', dose: '900 mcg', form: 'Suspension', outcome: 'Administered', administered_by: 'Nurse Lewis' },
      { date_time: '2026-07-17T09:15:00Z', patientId: 'pt-10082', dose: '600 mcg', form: 'Suspension', outcome: 'Observed', administered_by: 'Nurse Patel' },
      { date_time: '2026-07-14T08:45:00Z', patientId: 'pt-10117', dose: '150 mcg', form: 'Powder Capsule', outcome: 'Administered', administered_by: 'Nurse Lewis' },
      { date_time: '2026-07-12T08:55:00Z', patientId: 'pt-10045', dose: '600 mcg', form: 'Suspension', outcome: 'Held', administered_by: 'Dr. Chen' }
    ],
    foodLogs: [
      { id: 'FL-1', patientId: 'pt-10045', date_time: '2026-07-18T11:30:00Z', meal: 'Peanut dose with yogurt', tolerated: 'Yes', notes: 'No delayed symptoms.', created_at: '2026-07-18T12:15:00Z' },
      { id: 'FL-2', patientId: 'pt-10082', date_time: '2026-07-17T12:10:00Z', meal: 'Milk dose with oatmeal', tolerated: 'Partial', notes: 'Mild nausea resolved in 15 minutes.', created_at: '2026-07-17T12:45:00Z' },
      { id: 'FL-3', patientId: 'pt-10117', date_time: '2026-07-14T10:05:00Z', meal: 'Egg dose with applesauce', tolerated: 'Yes', notes: 'Completed full meal.', created_at: '2026-07-14T10:50:00Z' }
    ],
    symptomLogs: [
      { id: 'SL-1', patientId: 'pt-10045', date_time: '2026-07-18T13:20:00Z', symptom: 'None reported', severity: 'Info', timing: '2h post-dose', notes: 'Routine follow-up call.', created_at: '2026-07-18T13:25:00Z' },
      { id: 'SL-2', patientId: 'pt-10082', date_time: '2026-07-17T12:30:00Z', symptom: 'Nausea', severity: 'Mild', timing: '45m post-dose', notes: 'Self-resolved, no intervention.', created_at: '2026-07-17T12:36:00Z' },
      { id: 'SL-3', patientId: 'pt-10117', date_time: '2026-07-14T11:10:00Z', symptom: 'Perioral itch', severity: 'Mild', timing: '20m post-dose', notes: 'Resolved with observation.', created_at: '2026-07-14T11:17:00Z' }
    ],
    notifications: [
      { id: 'NT-1', patientId: 'pt-10045', type: 'updose', priority: 'Info', message: 'Alice Johnson is due for updose review on 25 Jul 2026.', read: false, created_at: '2026-07-22T08:15:00Z' },
      { id: 'NT-2', patientId: 'pt-10082', type: 'expiry', priority: 'Warning', message: 'Syringe SYR-2029 will expire within 24 hours.', read: false, created_at: '2026-07-22T07:40:00Z' },
      { id: 'NT-3', patientId: 'pt-10117', type: 'inventory', priority: 'Critical', message: 'Egg powder supply PWD-17 is depleted.', read: true, created_at: '2026-07-21T16:05:00Z' }
    ],
    auditLogs: [
      { timestamp: '2026-07-22T08:20:00Z', user: 'nurse.lewis', patientId: 'pt-10045', action: 'Viewed', entity: 'Patient', details: 'Opened patient detail dashboard.' },
      { timestamp: '2026-07-22T07:42:00Z', user: 'dr.alvarez', patientId: 'pt-10082', action: 'Reviewed', entity: 'Syringe', details: 'Reviewed expiry warning for SYR-2029.' },
      { timestamp: '2026-07-21T16:10:00Z', user: 'system', patientId: 'pt-10117', action: 'Generated', entity: 'Notification', details: 'Created low inventory alert for egg powder.' }
    ],
    missions: [
      { id: 'MS-1', patientId: 'pt-10045', mission: 'Complete three consecutive observation visits', due_date: '2026-07-28', status: 'Complete', points: 40 },
      { id: 'MS-2', patientId: 'pt-10082', mission: 'Log post-dose meal for seven days', due_date: '2026-07-26', status: 'In Progress', points: 25 },
      { id: 'MS-3', patientId: 'pt-10117', mission: 'Symptom-free week', due_date: '2026-07-29', status: 'Scheduled', points: 30 }
    ],
    badges: [
      { id: 'BD-1', patientId: 'pt-10045', name: 'Dose Climber', description: 'Advanced through 5 dose steps.', icon: 'fa-mountain-sun', color: 'green', earned_at: '2026-07-18T09:30:00Z' },
      { id: 'BD-2', patientId: 'pt-10082', name: 'Meal Logger', description: 'Completed 14 food log entries.', icon: 'fa-utensils', color: 'blue', earned_at: '2026-07-10T12:00:00Z' },
      { id: 'BD-3', patientId: 'pt-10117', name: 'Observation Star', description: 'Perfect clinic observation attendance.', icon: 'fa-star', color: 'yellow', earned_at: '2026-07-14T11:30:00Z' }
    ]
  };

  const navItems = [
    { key: 'patients', label: 'Patients', icon: 'fa-user-group', href: 'patients.html' },
    { key: 'dose-plan', label: 'Dose Plan', icon: 'fa-syringe', href: 'dose-plan.html' },
    { key: 'syringes', label: 'Syringes', icon: 'fa-prescription-bottle-medical', href: 'syringes.html' },
    { key: 'cartons', label: 'Cartons', icon: 'fa-box-open', href: 'cartons.html' },
    { key: 'mar', label: 'MAR', icon: 'fa-notes-medical', href: 'mar.html' },
    { key: 'food-logs', label: 'Food Logs', icon: 'fa-apple-whole', href: 'food-logs.html' },
    { key: 'symptom-logs', label: 'Symptom Logs', icon: 'fa-heart-pulse', href: 'symptom-logs.html' },
    { key: 'notifications', label: 'Notifications', icon: 'fa-bell', href: 'notifications.html' },
    { key: 'audit-logs', label: 'Audit Trail', icon: 'fa-shield-halved', href: 'audit-logs.html' },
    { key: 'missions', label: 'Missions', icon: 'fa-award', href: 'missions.html' }
  ];

  const OITApp = {
    navItems,
    demoData,
    normalizeApiUrl(url) {
      return window.api ? window.api.normalizeBaseUrl(url) : ((url || '').trim() || 'http://localhost:3000/api/v1');
    },
    saveApiUrl(url) {
      const normalized = this.normalizeApiUrl(url);
      localStorage.setItem('api_url', normalized);
      if (window.api) {
        window.api.baseUrl = normalized;
      }
      return normalized;
    },
    asArray(value) {
      if (Array.isArray(value)) return value;
      if (value == null) return [];
      return [value];
    },
    escapeHtml(value) {
      return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    },
    formatDate(value) {
      if (!value) return '—';
      const date = new Date(value);
      return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('en-US', { month: 'short', day: '2-digit', year: 'numeric' }).format(date);
    },
    formatDateTime(value) {
      if (!value) return '—';
      const date = new Date(value);
      return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('en-US', { month: 'short', day: '2-digit', year: 'numeric', hour: 'numeric', minute: '2-digit' }).format(date);
    },
    parseQuery() {
      return new URLSearchParams(window.location.search);
    },
    getCurrentApiUrl() {
      return localStorage.getItem('api_url') || 'http://localhost:3000/api/v1';
    },
    getInitials(name) {
      return String(name || 'OIT').split(' ').map(part => part[0]).join('').slice(0, 2).toUpperCase();
    },
    getPatient(patientId) {
      return this.demoData.patients.find(patient => patient.id === patientId) || null;
    },
    getPatientName(patientId) {
      return this.getPatient(patientId)?.name || 'Unknown Patient';
    },
    patientOptions(selected = '') {
      return ['<option value="">All patients</option>']
        .concat(this.demoData.patients.map(patient => `<option value="${patient.id}" ${patient.id === selected ? 'selected' : ''}>${this.escapeHtml(patient.name)}</option>`))
        .join('');
    },
    statusTone(status = '') {
      const value = status.toLowerCase();
      if (['active', 'stable', 'ready', 'administered', 'complete', 'completed', 'used', 'approved', 'open', 'in stock', 'yes'].some(token => value.includes(token))) return 'green';
      if (['warning', 'expiring', 'observation', 'caution', 'partial', 'scheduled', 'in progress', 'held', 'low'].some(token => value.includes(token))) return 'yellow';
      if (['critical', 'expired', 'discarded', 'adverse', 'out', 'failed'].some(token => value.includes(token))) return 'red';
      return 'blue';
    },
    badge(status) {
      const tone = this.statusTone(status);
      return `<span class="badge bg-${tone}-lt text-${tone}">${this.escapeHtml(status || 'Unknown')}</span>`;
    },
    priorityBadge(status) {
      return this.badge(status);
    },
    expiryBadge(dateValue, statusOverride = '') {
      const now = new Date();
      const date = new Date(dateValue);
      let label = statusOverride || 'OK';
      let tone = 'green';
      if (!Number.isNaN(date.getTime())) {
        const hours = (date - now) / 36e5;
        if (hours < 0) {
          tone = 'red';
          label = statusOverride || 'Expired';
        } else if (hours <= 24) {
          tone = 'yellow';
          label = statusOverride || 'Expiring Soon';
        }
      }
      if (statusOverride) {
        tone = this.statusTone(statusOverride);
      }
      return `<span class="badge bg-${tone}-lt text-${tone}">${this.escapeHtml(label)}</span>`;
    },
    renderMetricCard({ label, value, icon, color = 'blue', description = '' }) {
      return `
        <div class="col-sm-6 col-lg-3">
          <div class="card metric-card">
            <div class="card-body">
              <div class="d-flex align-items-start justify-content-between mb-3">
                <div>
                  <div class="text-secondary text-uppercase small fw-medium">${this.escapeHtml(label)}</div>
                  <div class="display-6 fw-bold mt-1">${this.escapeHtml(value)}</div>
                </div>
                <span class="icon-tile bg-${color}-lt text-${color}"><i class="fa-solid ${icon}"></i></span>
              </div>
              <div class="text-secondary small">${this.escapeHtml(description)}</div>
            </div>
          </div>
        </div>`;
    },
    renderDataSourceNote({ demo = false, error = null } = {}) {
      if (!demo) return '';
      return `
        <div class="alert alert-info data-source-note" role="alert">
          <div class="d-flex gap-2 align-items-start">
            <i class="fa-solid fa-circle-info mt-1"></i>
            <div>
              <strong>Demo dataset loaded.</strong>
              ${this.escapeHtml(error?.message || 'The configured API is unavailable, so sample clinical data is being shown.')}
              <a href="#" class="alert-link ms-1" id="open-settings-inline">Update API URL</a>
            </div>
          </div>
        </div>`;
    },
    renderEmptyState(message, icon = 'fa-box-open') {
      return `
        <div class="card">
          <div class="card-body py-5 text-center">
            <div class="icon-tile bg-blue-lt text-blue mx-auto mb-3"><i class="fa-solid ${icon}"></i></div>
            <h3 class="mb-1">Nothing to display</h3>
            <p class="text-secondary mb-0">${this.escapeHtml(message)}</p>
          </div>
        </div>`;
    },
    async resolveData(fetcher, fallback) {
      try {
        const data = await fetcher();
        return { data, demo: false, error: null };
      } catch (error) {
        console.warn('API unavailable, using demo data:', error);
        const data = typeof fallback === 'function' ? fallback() : fallback;
        return { data, demo: true, error };
      }
    },
    attachSharedEvents() {
      document.getElementById('logout-link')?.addEventListener('click', (event) => {
        event.preventDefault();
        logout();
      });
      const openSettings = () => {
        document.getElementById('settings-api-url').value = this.getCurrentApiUrl();
        const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('settings-modal'));
        modal.show();
      };
      document.getElementById('open-settings')?.addEventListener('click', (event) => {
        event.preventDefault();
        openSettings();
      });
      if (!document.body.dataset.oitSettingsBound) {
        document.body.dataset.oitSettingsBound = 'true';
        document.body.addEventListener('click', (event) => {
          if (event.target.closest('#open-settings-inline')) {
            event.preventDefault();
            openSettings();
          }
        });
      }
      document.getElementById('settings-form')?.addEventListener('submit', (event) => {
        event.preventDefault();
        const value = document.getElementById('settings-api-url').value;
        const normalized = this.saveApiUrl(value);
        document.getElementById('current-api-url').textContent = normalized;
        bootstrap.Modal.getInstance(document.getElementById('settings-modal'))?.hide();
        window.location.reload();
      });
    },
    renderLayout({ title, subtitle = '', activeNav = '', content = '', headerActions = '' }) {
      const user = getCurrentUser() || { name: 'Clinical User', role: 'Clinical Staff' };
      const navHtml = navItems.map(item => `
        <li class="nav-item ${activeNav === item.key ? 'active' : ''}">
          <a class="nav-link" href="${item.href}">
            <span class="nav-link-icon d-md-none d-lg-inline-block"><i class="fa-solid ${item.icon}"></i></span>
            <span class="nav-link-title">${item.label}</span>
          </a>
        </li>`).join('');

      document.getElementById('app-shell').innerHTML = `
        <div class="page page-shell">
          <aside class="navbar navbar-vertical navbar-expand-lg navbar-dark">
            <div class="container-fluid">
              <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#sidebar-menu" aria-controls="sidebar-menu" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
              </button>
              <h1 class="navbar-brand navbar-brand-autodark mb-0">
                <a href="patients.html" class="text-reset text-decoration-none d-flex align-items-center">
                  <span class="brand-mark"><i class="fa-solid fa-shield-heart"></i></span>
                  <span class="navbar-brand-title">OIT Management</span>
                </a>
              </h1>
              <div class="collapse navbar-collapse" id="sidebar-menu">
                <ul class="navbar-nav pt-lg-3">${navHtml}</ul>
              </div>
            </div>
          </aside>
          <div class="page-wrapper">
            <header class="navbar navbar-expand-md d-print-none">
              <div class="container-fluid">
                <div>
                  <div class="page-pretitle">Clinical dashboard</div>
                  <h2 class="page-title mb-0">${this.escapeHtml(title)}</h2>
                  ${subtitle ? `<div class="text-secondary">${this.escapeHtml(subtitle)}</div>` : ''}
                </div>
                <div class="navbar-nav flex-row order-md-last align-items-center gap-2">
                  <span class="badge badge-soft d-none d-lg-inline-flex">API: <span class="ms-1" id="current-api-url">${this.escapeHtml(this.getCurrentApiUrl())}</span></span>
                  <a href="#" class="nav-link px-2" id="open-settings" title="API settings"><i class="fa-solid fa-gear"></i></a>
                  <div class="nav-item dropdown">
                    <a href="#" class="nav-link d-flex lh-1 text-reset p-0" data-bs-toggle="dropdown" aria-label="Open user menu">
                      <span class="avatar avatar-sm bg-blue-lt text-blue">${this.getInitials(user.name)}</span>
                      <div class="d-none d-xl-block ps-2">
                        <div>${this.escapeHtml(user.name)}</div>
                        <div class="mt-1 small text-secondary">${this.escapeHtml(user.role || 'Clinical Staff')}</div>
                      </div>
                    </a>
                    <div class="dropdown-menu dropdown-menu-end dropdown-menu-arrow">
                      <a href="#" class="dropdown-item" id="logout-link">Sign out</a>
                    </div>
                  </div>
                </div>
              </div>
            </header>
            <div class="page-header d-print-none">
              <div class="container-fluid">${headerActions}</div>
            </div>
            <div class="page-body">
              <div class="container-fluid">${content}</div>
            </div>
          </div>
        </div>
        <div class="modal modal-blur fade" id="settings-modal" tabindex="-1" aria-hidden="true">
          <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
              <form id="settings-form">
                <div class="modal-header">
                  <h5 class="modal-title">Dashboard settings</h5>
                  <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                  <div class="mb-3">
                    <label class="form-label">API base URL</label>
                    <input id="settings-api-url" type="url" class="form-control" value="${this.escapeHtml(this.getCurrentApiUrl())}" placeholder="http://localhost:3000/api/v1" required>
                    <div class="form-hint">Enter the platform API base URL. Host-only values are normalized to /api/v1 automatically.</div>
                  </div>
                </div>
                <div class="modal-footer">
                  <button type="button" class="btn me-auto" data-bs-dismiss="modal">Cancel</button>
                  <button type="submit" class="btn btn-primary">Save settings</button>
                </div>
              </form>
            </div>
          </div>
        </div>`;
      this.attachSharedEvents();
    },
    filterPatients(search = '') {
      const term = search.trim().toLowerCase();
      if (!term) return [...demoData.patients];
      return demoData.patients.filter(patient => [patient.name, patient.mrn, patient.dob].some(value => String(value).toLowerCase().includes(term)));
    }
  };

  window.OITApp = OITApp;
})();
