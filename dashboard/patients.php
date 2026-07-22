<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';
requireLogin();

$search = trim($_GET['search'] ?? '');
$params = [];
$where  = '';
if ($search !== '') {
    $where  = 'WHERE p.first_name LIKE ? OR p.last_name LIKE ? OR p.mrn LIKE ? OR p.date_of_birth LIKE ?';
    $like   = "%$search%";
    $params = [$like, $like, $like, $like];
}

$patients = dbQuery(
    "SELECT p.*,
            (SELECT dp.scheduled_date FROM dose_plans dp
             WHERE dp.patient_id = p.id AND dp.status = 'scheduled' AND dp.scheduled_date >= CURDATE()
             ORDER BY dp.scheduled_date LIMIT 1) AS next_dose_date
     FROM patients p $where
     ORDER BY p.last_name, p.first_name",
    $params
);

$pageTitle  = 'Patients';
$activePage = 'patients';
require_once __DIR__ . '/includes/header.php';
?>
<div class="page-header d-print-none">
  <div class="container-xl">
    <div class="row g-2 align-items-center">
      <div class="col"><h2 class="page-title">Patients</h2></div>
    </div>
  </div>
</div>
<div class="page-body">
  <div class="container-xl">
    <!-- Search -->
    <div class="card mb-3">
      <div class="card-body">
        <form method="GET" class="row g-2 align-items-end">
          <div class="col-md-6">
            <label class="form-label">Search (name, MRN, date of birth)</label>
            <input type="text" name="search" class="form-control" value="<?= e($search) ?>" placeholder="e.g. Smith, 001234, 2015-04-01">
          </div>
          <div class="col-auto">
            <button class="btn btn-primary"><i class="fa-solid fa-magnifying-glass me-1"></i>Search</button>
            <?php if ($search): ?>
            <a href="patients.php" class="btn btn-ghost-secondary ms-1">Clear</a>
            <?php endif; ?>
          </div>
        </form>
      </div>
    </div>
    <!-- Table -->
    <div class="card">
      <div class="card-header">
        <h3 class="card-title">All Patients <span class="badge bg-blue-lt ms-1"><?= count($patients) ?></span></h3>
      </div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table table-hover">
          <thead>
            <tr>
              <th>MRN</th><th>Name</th><th>Date of Birth</th><th>Allergen</th>
              <th>Protocol</th><th>Current Dose</th><th>Next Dose Date</th><th></th>
            </tr>
          </thead>
          <tbody>
          <?php if (empty($patients)): ?>
            <tr><td colspan="8" class="text-center text-muted py-4">No patients found.</td></tr>
          <?php else: ?>
            <?php foreach ($patients as $p): ?>
            <tr>
              <td><code><?= e($p['mrn']) ?></code></td>
              <td><a href="patient-detail.php?id=<?= (int)$p['id'] ?>" class="fw-bold"><?= e($p['last_name'] . ', ' . $p['first_name']) ?></a></td>
              <td><?= e($p['date_of_birth']) ?></td>
              <td><?= e($p['allergen']) ?></td>
              <td><?= e($p['oit_protocol']) ?></td>
              <td><?= e($p['current_dose_mcg']) ?> mcg</td>
              <td><?= e($p['next_dose_date'] ?? '—') ?></td>
              <td><a href="patient-detail.php?id=<?= (int)$p['id'] ?>" class="btn btn-sm btn-outline-primary">View</a></td>
            </tr>
            <?php endforeach; ?>
          <?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
