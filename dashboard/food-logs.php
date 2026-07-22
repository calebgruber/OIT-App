<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';
requireLogin();

$patientId = (int)($_GET['patient_id'] ?? 0);
$date      = $_GET['date'] ?? '';
$patients  = dbQuery('SELECT id, first_name, last_name, mrn FROM patients ORDER BY last_name');

$where  = [];
$params = [];
if ($patientId > 0) { $where[] = 'f.patient_id = ?'; $params[] = $patientId; }
if ($date !== '')   { $where[] = 'DATE(f.logged_at) = ?'; $params[] = $date; }

$sql = "SELECT f.*, p.first_name, p.last_name FROM food_logs f
        JOIN patients p ON p.id = f.patient_id"
     . ($where ? ' WHERE ' . implode(' AND ', $where) : '')
     . " ORDER BY f.logged_at DESC LIMIT 200";
$logs = dbQuery($sql, $params);

$pageTitle  = 'Food Logs';
$activePage = 'food-logs';
require_once __DIR__ . '/includes/header.php';
?>
<div class="page-header d-print-none">
  <div class="container-xl"><h2 class="page-title">Food Logs</h2></div>
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
          <div class="col-md-3">
            <label class="form-label">Date</label>
            <input type="date" name="date" class="form-control" value="<?= e($date) ?>">
          </div>
          <div class="col-auto">
            <button class="btn btn-primary"><i class="fa-solid fa-filter me-1"></i>Filter</button>
            <a href="food-logs.php" class="btn btn-ghost-secondary ms-1">Reset</a>
          </div>
        </form>
      </div>
    </div>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Food Logs <span class="badge bg-blue-lt ms-1"><?= count($logs) ?></span></h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>Logged At</th><th>Patient</th><th>Food Description</th><th>Timing OK</th></tr></thead>
          <tbody>
          <?php foreach ($logs as $f): ?>
          <tr>
            <td><?= e($f['logged_at']) ?></td>
            <td><a href="patient-detail.php?id=<?= $f['patient_id'] ?>"><?= e($f['last_name'].', '.$f['first_name']) ?></a></td>
            <td><?= e($f['food_description']) ?></td>
            <td><?= $f['timing_ok'] ? '<span class="badge bg-success">Yes</span>' : '<span class="badge bg-warning text-dark">No</span>' ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($logs)): ?><tr><td colspan="4" class="text-muted text-center py-3">No food logs found.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
