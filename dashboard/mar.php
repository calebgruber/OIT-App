<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';
requireLogin();

$patientId = (int)($_GET['patient_id'] ?? 0);
$dateFrom  = $_GET['date_from'] ?? '';
$dateTo    = $_GET['date_to']   ?? '';
$outcome   = $_GET['outcome']   ?? '';
$patients  = dbQuery('SELECT id, first_name, last_name, mrn FROM patients ORDER BY last_name');

$where  = [];
$params = [];
if ($patientId > 0)  { $where[] = 'm.patient_id = ?';            $params[] = $patientId; }
if ($dateFrom !== '') { $where[] = 'DATE(m.administered_at) >= ?'; $params[] = $dateFrom; }
if ($dateTo !== '')   { $where[] = 'DATE(m.administered_at) <= ?'; $params[] = $dateTo; }
if ($outcome !== '')  { $where[] = 'm.outcome = ?';               $params[] = $outcome; }

$sql = "SELECT m.*, p.first_name, p.last_name, u.username AS given_by
        FROM mar_entries m
        JOIN patients p ON p.id = m.patient_id
        LEFT JOIN users u ON u.id = m.administered_by_user_id"
     . ($where ? ' WHERE ' . implode(' AND ', $where) : '')
     . " ORDER BY m.administered_at DESC LIMIT 300";
$mar = dbQuery($sql, $params);

$pageTitle  = 'MAR — Medication Administration Records';
$activePage = 'mar';
require_once __DIR__ . '/includes/header.php';
?>
<div class="page-header d-print-none">
  <div class="container-xl"><h2 class="page-title">Medication Administration Records (MAR)</h2></div>
</div>
<div class="page-body">
  <div class="container-xl">
    <div class="card mb-3">
      <div class="card-body">
        <form method="GET" class="row g-2 align-items-end">
          <div class="col-md-3">
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
          <div class="col-md-2">
            <label class="form-label">From</label>
            <input type="date" name="date_from" class="form-control" value="<?= e($dateFrom) ?>">
          </div>
          <div class="col-md-2">
            <label class="form-label">To</label>
            <input type="date" name="date_to" class="form-control" value="<?= e($dateTo) ?>">
          </div>
          <div class="col-md-2">
            <label class="form-label">Outcome</label>
            <select name="outcome" class="form-select">
              <option value="">All</option>
              <?php foreach (['completed','reaction','missed'] as $o): ?>
              <option value="<?= $o ?>" <?= $outcome===$o?'selected':'' ?>><?= ucfirst($o) ?></option>
              <?php endforeach; ?>
            </select>
          </div>
          <div class="col-auto">
            <button class="btn btn-primary"><i class="fa-solid fa-filter me-1"></i>Filter</button>
            <a href="mar.php" class="btn btn-ghost-secondary ms-1">Reset</a>
          </div>
        </form>
      </div>
    </div>
    <div class="card">
      <div class="card-header"><h3 class="card-title">MAR Entries <span class="badge bg-blue-lt ms-1"><?= count($mar) ?></span></h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>Date/Time</th><th>Patient</th><th>Dose</th><th>Form</th><th>Outcome</th><th>Given By</th><th>Notes</th></tr></thead>
          <tbody>
          <?php foreach ($mar as $m): ?>
          <tr>
            <td><?= e($m['administered_at']) ?></td>
            <td><a href="patient-detail.php?id=<?= $m['patient_id'] ?>"><?= e($m['last_name'].', '.$m['first_name']) ?></a></td>
            <td><?= e($m['dose_mcg']) ?> mcg</td>
            <td><?= e($m['dose_form']) ?></td>
            <td><?= statusBadge($m['outcome']) ?></td>
            <td><?= e($m['given_by'] ?? '—') ?></td>
            <td><?= e($m['notes'] ?? '—') ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($mar)): ?><tr><td colspan="7" class="text-muted text-center py-3">No records found.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
