<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';
requireLogin();

$patientId  = (int)($_GET['patient_id'] ?? 0);
$statusF    = $_GET['status'] ?? '';
$patients   = dbQuery('SELECT id, first_name, last_name, mrn FROM patients ORDER BY last_name');

$params = [];
$where  = [];
if ($patientId > 0)  { $where[] = 'dp.patient_id = ?'; $params[] = $patientId; }
if ($statusF !== '')  { $where[] = 'dp.status = ?';     $params[] = $statusF; }
$sql = "SELECT dp.*, p.first_name, p.last_name, p.mrn
        FROM dose_plans dp JOIN patients p ON p.id = dp.patient_id"
    . ($where ? ' WHERE ' . implode(' AND ', $where) : '')
    . " ORDER BY dp.scheduled_date DESC LIMIT 200";
$plans = dbQuery($sql, $params);

// Updose decisions
$decisions = [];
if ($patientId > 0) {
    $decisions = dbQuery(
        'SELECT ud.*, u.username FROM updose_decisions ud
         LEFT JOIN users u ON u.id = ud.decided_by_user_id
         WHERE ud.patient_id = ? ORDER BY ud.decision_date DESC',
        [$patientId]
    );
}

$pageTitle  = 'Dose Plans';
$activePage = 'dose-plan';
require_once __DIR__ . '/includes/header.php';
?>
<div class="page-header d-print-none">
  <div class="container-xl"><h2 class="page-title">Dose Plans</h2></div>
</div>
<div class="page-body">
  <div class="container-xl">
    <div class="card mb-3">
      <div class="card-body">
        <form method="GET" class="row g-2 align-items-end">
          <div class="col-md-4">
            <label class="form-label">Patient</label>
            <select name="patient_id" class="form-select">
              <option value="">All patients</option>
              <?php foreach ($patients as $p): ?>
              <option value="<?= $p['id'] ?>" <?= $patientId===$p['id']?'selected':'' ?>>
                <?= e($p['last_name'].', '.$p['first_name'].' ('.$p['mrn'].')') ?>
              </option>
              <?php endforeach; ?>
            </select>
          </div>
          <div class="col-md-3">
            <label class="form-label">Status</label>
            <select name="status" class="form-select">
              <option value="">All</option>
              <?php foreach (['scheduled','completed','skipped'] as $s): ?>
              <option value="<?= $s ?>" <?= $statusF===$s?'selected':'' ?>><?= ucfirst($s) ?></option>
              <?php endforeach; ?>
            </select>
          </div>
          <div class="col-auto">
            <button class="btn btn-primary"><i class="fa-solid fa-filter me-1"></i>Filter</button>
            <a href="dose-plan.php" class="btn btn-ghost-secondary ms-1">Reset</a>
          </div>
        </form>
      </div>
    </div>

    <div class="card mb-3">
      <div class="card-header"><h3 class="card-title">Dose Schedule <span class="badge bg-blue-lt ms-1"><?= count($plans) ?></span></h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>Patient</th><th>Scheduled Date</th><th>Dose (mcg)</th><th>Form</th><th>Status</th></tr></thead>
          <tbody>
          <?php foreach ($plans as $dp): ?>
          <tr>
            <td><a href="patient-detail.php?id=<?= $dp['patient_id'] ?>"><?= e($dp['last_name'].', '.$dp['first_name']) ?></a></td>
            <td><?= e($dp['scheduled_date']) ?></td>
            <td><?= e($dp['dose_mcg']) ?></td>
            <td><?= e($dp['dose_form']) ?></td>
            <td><?= statusBadge($dp['status']) ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($plans)): ?><tr><td colspan="5" class="text-muted text-center py-3">No entries found.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>

    <?php if ($patientId > 0 && !empty($decisions)): ?>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Updose Decisions</h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>Date</th><th>Type</th><th>Previous</th><th>New</th><th>By</th><th>Notes</th></tr></thead>
          <tbody>
          <?php foreach ($decisions as $d): ?>
          <tr>
            <td><?= e($d['decision_date']) ?></td>
            <td><span class="badge bg-blue-lt"><?= e($d['decision_type']) ?></span></td>
            <td><?= e($d['previous_dose_mcg']) ?> mcg</td>
            <td><?= e($d['new_dose_mcg']) ?> mcg</td>
            <td><?= e($d['username'] ?? '—') ?></td>
            <td><?= e($d['notes'] ?? '—') ?></td>
          </tr>
          <?php endforeach; ?>
          </tbody>
        </table>
      </div>
    </div>
    <?php endif; ?>
  </div>
</div>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
