<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';
requireLogin();

$patientId = (int)($_GET['patient_id'] ?? 0);
$patients  = dbQuery('SELECT id, first_name, last_name, mrn FROM patients ORDER BY last_name');

$where  = $patientId > 0 ? 'WHERE sl.patient_id = ?' : '';
$params = $patientId > 0 ? [$patientId] : [];

$sql = "SELECT sl.*, p.first_name, p.last_name FROM symptom_logs sl
        JOIN patients p ON p.id = sl.patient_id
        $where ORDER BY sl.logged_at DESC LIMIT 200";
$logs = dbQuery($sql, $params);

$pageTitle  = 'Symptom Logs';
$activePage = 'symptom-logs';
require_once __DIR__ . '/includes/header.php';
?>
<div class="page-header d-print-none">
  <div class="container-xl"><h2 class="page-title">Symptom Logs</h2></div>
</div>
<div class="page-body">
  <div class="container-xl">
    <div class="card mb-3">
      <div class="card-body">
        <form method="GET" class="row g-2 align-items-end">
          <div class="col-md-4">
            <label class="form-label">Patient</label>
            <select name="patient_id" class="form-select">
              <option value="">All</option>
              <?php foreach ($patients as $p): ?>
              <option value="<?= $p['id'] ?>" <?= $patientId===$p['id']?'selected':'' ?>>
                <?= e($p['last_name'].', '.$p['first_name']) ?>
              </option>
              <?php endforeach; ?>
            </select>
          </div>
          <div class="col-auto">
            <button class="btn btn-primary"><i class="fa-solid fa-filter me-1"></i>Filter</button>
            <a href="symptom-logs.php" class="btn btn-ghost-secondary ms-1">Reset</a>
          </div>
        </form>
      </div>
    </div>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Symptom Logs <span class="badge bg-blue-lt ms-1"><?= count($logs) ?></span></h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>Logged At</th><th>Patient</th><th>Severity</th><th>Symptoms</th><th>Notes</th></tr></thead>
          <tbody>
          <?php foreach ($logs as $sl): ?>
          <?php $sev = (int)$sl['severity']; ?>
          <tr>
            <td><?= e($sl['logged_at']) ?></td>
            <td><a href="patient-detail.php?id=<?= $sl['patient_id'] ?>"><?= e($sl['last_name'].', '.$sl['first_name']) ?></a></td>
            <td>
              <span class="badge bg-<?= $sev >= 4 ? 'danger' : ($sev >= 3 ? 'warning text-dark' : 'success') ?>">
                <?= $sev ?>/5
              </span>
            </td>
            <td><?= e($sl['symptoms'] ?? '—') ?></td>
            <td><?= e($sl['notes'] ?? '—') ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($logs)): ?><tr><td colspan="5" class="text-muted text-center py-3">No symptom logs found.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
