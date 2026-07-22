<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';
requireLogin();

$patientId = (int)($_GET['patient_id'] ?? 0);
$patients  = dbQuery('SELECT id, first_name, last_name, mrn FROM patients ORDER BY last_name');

$cpWhere = $patientId > 0 ? 'WHERE patient_id = ?' : '';
$cpParams = $patientId > 0 ? [$patientId] : [];

$cartons = dbQuery("SELECT c.*, p.first_name, p.last_name FROM cartons c
                    JOIN patients p ON p.id = c.patient_id
                    $cpWhere ORDER BY c.expiration_date", $cpParams);
$powder  = dbQuery("SELECT ps.*, p.first_name, p.last_name FROM powder_supplies ps
                    JOIN patients p ON p.id = ps.patient_id
                    $cpWhere ORDER BY ps.expiration_date", $cpParams);

$pageTitle  = 'Cartons & Powder Supplies';
$activePage = 'cartons';
require_once __DIR__ . '/includes/header.php';
?>
<div class="page-header d-print-none">
  <div class="container-xl"><h2 class="page-title">Cartons &amp; Powder Supplies</h2></div>
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
          <div class="col-auto">
            <button class="btn btn-primary"><i class="fa-solid fa-filter me-1"></i>Filter</button>
            <a href="cartons.php" class="btn btn-ghost-secondary ms-1">Reset</a>
          </div>
        </form>
      </div>
    </div>

    <div class="card mb-3">
      <div class="card-header"><h3 class="card-title"><i class="fa-solid fa-droplet me-1 text-blue"></i>Milk Cartons <span class="badge bg-blue-lt ms-1"><?= count($cartons) ?></span></h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>ID</th><th>Patient</th><th>Lot #</th><th>Expiry</th><th>Opened</th><th>Barcode</th><th>Status</th></tr></thead>
          <tbody>
          <?php foreach ($cartons as $c): ?>
          <tr class="<?= expiryClass($c['expiration_date']) ?>">
            <td><code><?= e($c['id']) ?></code></td>
            <td><a href="patient-detail.php?id=<?= $c['patient_id'] ?>"><?= e($c['last_name'].', '.$c['first_name']) ?></a></td>
            <td><?= e($c['lot_number']) ?></td>
            <td><?= e($c['expiration_date']) ?></td>
            <td><?= e($c['date_opened'] ?? '—') ?></td>
            <td><code><?= e($c['barcode'] ?? '—') ?></code></td>
            <td><?= statusBadge($c['status']) ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($cartons)): ?><tr><td colspan="7" class="text-muted text-center py-3">No cartons.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <div class="card-header"><h3 class="card-title"><i class="fa-solid fa-jar me-1 text-orange"></i>Powder Supplies <span class="badge bg-orange-lt ms-1"><?= count($powder) ?></span></h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>ID</th><th>Patient</th><th>Lot #</th><th>Expiry</th><th>Opened</th><th>Barcode</th><th>Status</th></tr></thead>
          <tbody>
          <?php foreach ($powder as $pw): ?>
          <tr class="<?= expiryClass($pw['expiration_date']) ?>">
            <td><code><?= e($pw['id']) ?></code></td>
            <td><a href="patient-detail.php?id=<?= $pw['patient_id'] ?>"><?= e($pw['last_name'].', '.$pw['first_name']) ?></a></td>
            <td><?= e($pw['lot_number']) ?></td>
            <td><?= e($pw['expiration_date']) ?></td>
            <td><?= e($pw['date_opened'] ?? '—') ?></td>
            <td><code><?= e($pw['barcode'] ?? '—') ?></code></td>
            <td><?= statusBadge($pw['status']) ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($powder)): ?><tr><td colspan="7" class="text-muted text-center py-3">No powder supplies.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
