<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';
requireLogin();

$patientId  = (int)($_GET['patient_id'] ?? 0);
$dateF      = $_GET['date'] ?? '';
$patients   = dbQuery('SELECT id, first_name, last_name, mrn FROM patients ORDER BY last_name');

$mWhere  = [];
$mParams = [];
if ($patientId > 0) { $mWhere[] = 'm.patient_id = ?'; $mParams[] = $patientId; }
if ($dateF !== '')  { $mWhere[] = 'm.mission_date = ?'; $mParams[] = $dateF; }

$mSql = "SELECT m.*, p.first_name, p.last_name FROM missions m
         JOIN patients p ON p.id = m.patient_id"
      . ($mWhere ? ' WHERE ' . implode(' AND ', $mWhere) : '')
      . " ORDER BY m.mission_date DESC, m.id DESC LIMIT 200";
$missions = dbQuery($mSql, $mParams);

$bWhere  = $patientId > 0 ? 'WHERE patient_id = ?' : '';
$bParams = $patientId > 0 ? [$patientId] : [];
$badges  = dbQuery("SELECT b.*, p.first_name, p.last_name FROM badges b
                    JOIN patients p ON p.id = b.patient_id
                    $bWhere ORDER BY b.earned_at DESC LIMIT 100", $bParams);

$pageTitle  = 'Missions & Badges';
$activePage = 'missions';
require_once __DIR__ . '/includes/header.php';
?>
<div class="page-header d-print-none">
  <div class="container-xl"><h2 class="page-title">Missions &amp; Badges</h2></div>
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
            <input type="date" name="date" class="form-control" value="<?= e($dateF) ?>">
          </div>
          <div class="col-auto">
            <button class="btn btn-primary"><i class="fa-solid fa-filter me-1"></i>Filter</button>
            <a href="missions.php" class="btn btn-ghost-secondary ms-1">Reset</a>
          </div>
        </form>
      </div>
    </div>

    <div class="row row-cards">
      <div class="col-lg-8">
        <div class="card">
          <div class="card-header"><h3 class="card-title">Missions <span class="badge bg-blue-lt ms-1"><?= count($missions) ?></span></h3></div>
          <div class="table-responsive">
            <table class="table table-vcenter card-table">
              <thead><tr><th>Date</th><th>Patient</th><th>Mission</th><th>Status</th><th>Completed At</th></tr></thead>
              <tbody>
              <?php foreach ($missions as $m): ?>
              <tr>
                <td><?= e($m['mission_date']) ?></td>
                <td><a href="patient-detail.php?id=<?= $m['patient_id'] ?>"><?= e($m['last_name'].', '.$m['first_name']) ?></a></td>
                <td><?= e(ucwords(str_replace('_',' ',$m['mission_type']))) ?></td>
                <td><?= statusBadge($m['status']) ?></td>
                <td><?= e($m['completed_at'] ?? '—') ?></td>
              </tr>
              <?php endforeach; ?>
              <?php if (empty($missions)): ?><tr><td colspan="5" class="text-muted text-center py-3">No missions found.</td></tr><?php endif; ?>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="col-lg-4">
        <div class="card">
          <div class="card-header"><h3 class="card-title"><i class="fa-solid fa-medal me-1 text-yellow"></i>Badges <span class="badge bg-yellow-lt ms-1"><?= count($badges) ?></span></h3></div>
          <div class="card-body">
          <?php if (empty($badges)): ?>
            <p class="text-muted">No badges earned yet.</p>
          <?php else: ?>
            <?php foreach ($badges as $b): ?>
            <div class="mb-2">
              <span class="badge bg-yellow text-dark p-2">
                <i class="fa-solid fa-medal me-1"></i><?= e(ucwords(str_replace('_',' ',$b['badge_type']))) ?>
              </span>
              <small class="text-muted ms-2"><?= e($b['last_name'].', '.$b['first_name']) ?> · <?= e(substr($b['earned_at'],0,10)) ?></small>
            </div>
            <?php endforeach; ?>
          <?php endif; ?>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
