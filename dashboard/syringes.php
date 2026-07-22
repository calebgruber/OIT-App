<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';
requireLogin();

$patientId = (int)($_GET['patient_id'] ?? 0);
$statusF   = $_GET['status'] ?? '';
$patients  = dbQuery('SELECT id, first_name, last_name, mrn FROM patients ORDER BY last_name');

$where  = [];
$params = [];
if ($patientId > 0) { $where[] = 's.patient_id = ?'; $params[] = $patientId; }
if ($statusF !== '') { $where[] = 's.status = ?';    $params[] = $statusF; }
$sql = "SELECT s.*, p.first_name, p.last_name, p.mrn
        FROM syringes s JOIN patients p ON p.id = s.patient_id"
     . ($where ? ' WHERE ' . implode(' AND ', $where) : '')
     . " ORDER BY s.final_expiry ASC LIMIT 200";
$syringes = dbQuery($sql, $params);

$pageTitle  = 'Syringes & Vials';
$activePage = 'syringes';
require_once __DIR__ . '/includes/header.php';
?>
<div class="page-header d-print-none">
  <div class="container-xl"><h2 class="page-title">Syringes &amp; Vials</h2></div>
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
              <?php foreach (['ready','used','expired','discarded'] as $s): ?>
              <option value="<?= $s ?>" <?= $statusF===$s?'selected':'' ?>><?= ucfirst($s) ?></option>
              <?php endforeach; ?>
            </select>
          </div>
          <div class="col-auto">
            <button class="btn btn-primary"><i class="fa-solid fa-filter me-1"></i>Filter</button>
            <a href="syringes.php" class="btn btn-ghost-secondary ms-1">Reset</a>
          </div>
        </form>
      </div>
    </div>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Syringes / Vials <span class="badge bg-blue-lt ms-1"><?= count($syringes) ?></span></h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead>
            <tr><th>ID</th><th>Patient</th><th>Dose</th><th>Form</th><th>Drawn</th><th>Scheduled</th><th>Final Expiry</th><th>Seal</th><th>Status</th></tr>
          </thead>
          <tbody>
          <?php foreach ($syringes as $s): ?>
          <tr class="<?= expiryClass($s['final_expiry']) ?>">
            <td><code><?= e($s['id']) ?></code></td>
            <td><a href="patient-detail.php?id=<?= $s['patient_id'] ?>"><?= e($s['last_name'].', '.$s['first_name']) ?></a></td>
            <td><?= e($s['dose_mcg']) ?> mcg</td>
            <td><?= e($s['dose_form']) ?></td>
            <td><?= e($s['date_drawn']) ?></td>
            <td><?= e($s['scheduled_dose_date']) ?></td>
            <td><?= e($s['final_expiry']) ?></td>
            <td><?= e($s['seal_type']) ?></td>
            <td><?= statusBadge($s['status']) ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($syringes)): ?><tr><td colspan="9" class="text-muted text-center py-3">No syringes found.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
