<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';
requireLogin();

$id = (int)($_GET['id'] ?? 0);
if ($id <= 0) { header('Location: patients.php'); exit; }

$patient = dbOne('SELECT * FROM patients WHERE id = ?', [$id]);
if (!$patient) { flash('error', 'Patient not found.'); header('Location: patients.php'); exit; }

$tab = $_GET['tab'] ?? 'overview';

// --- tab data ---
$syringes = $dosePlan = $cartons = $powder = $mar = $food = $symptoms = $missions = $badges = [];

if ($tab === 'overview' || $tab === 'syringes') {
    $syringes = dbQuery('SELECT * FROM syringes WHERE patient_id = ? ORDER BY final_expiry DESC LIMIT 50', [$id]);
}
if ($tab === 'overview' || $tab === 'dose-plan') {
    $dosePlan = dbQuery(
        'SELECT * FROM dose_plans WHERE patient_id = ? ORDER BY scheduled_date DESC LIMIT 60', [$id]);
}
if ($tab === 'cartons') {
    $cartons = dbQuery('SELECT * FROM cartons WHERE patient_id = ? ORDER BY expiration_date', [$id]);
    $powder  = dbQuery('SELECT * FROM powder_supplies WHERE patient_id = ? ORDER BY expiration_date', [$id]);
}
if ($tab === 'mar') {
    $mar = dbQuery(
        'SELECT m.*, u.username AS given_by
         FROM mar_entries m
         LEFT JOIN users u ON u.id = m.administered_by_user_id
         WHERE m.patient_id = ? ORDER BY m.administered_at DESC LIMIT 60', [$id]);
}
if ($tab === 'food') {
    $food = dbQuery('SELECT * FROM food_logs WHERE patient_id = ? ORDER BY logged_at DESC LIMIT 60', [$id]);
}
if ($tab === 'symptoms') {
    $symptoms = dbQuery('SELECT * FROM symptom_logs WHERE patient_id = ? ORDER BY logged_at DESC LIMIT 60', [$id]);
}
if ($tab === 'missions') {
    $missions = dbQuery('SELECT * FROM missions WHERE patient_id = ? ORDER BY mission_date DESC LIMIT 30', [$id]);
    $badges   = dbQuery('SELECT * FROM badges WHERE patient_id = ? ORDER BY earned_at DESC', [$id]);
}

$nextDose = dbOne(
    "SELECT * FROM dose_plans WHERE patient_id = ? AND status = 'scheduled' AND scheduled_date >= CURDATE() ORDER BY scheduled_date LIMIT 1",
    [$id]
);

$pageTitle  = $patient['first_name'] . ' ' . $patient['last_name'];
$activePage = 'patients';
require_once __DIR__ . '/includes/header.php';
?>
<div class="page-header d-print-none">
  <div class="container-xl">
    <div class="row g-2 align-items-center">
      <div class="col-auto">
        <a href="patients.php" class="btn btn-ghost-secondary btn-sm"><i class="fa-solid fa-arrow-left"></i></a>
      </div>
      <div class="col">
        <h2 class="page-title"><?= e($patient['first_name'] . ' ' . $patient['last_name']) ?></h2>
        <div class="text-muted mt-1">MRN: <code><?= e($patient['mrn']) ?></code></div>
      </div>
    </div>
  </div>
</div>
<div class="page-body">
  <div class="container-xl">
    <!-- Patient header card -->
    <div class="card mb-3">
      <div class="card-body">
        <div class="row g-3">
          <div class="col-sm-6 col-lg-3">
            <div class="text-muted small">Date of Birth</div>
            <div class="fw-bold"><?= e($patient['date_of_birth']) ?></div>
          </div>
          <div class="col-sm-6 col-lg-3">
            <div class="text-muted small">Allergen</div>
            <div class="fw-bold"><?= e($patient['allergen']) ?></div>
          </div>
          <div class="col-sm-6 col-lg-3">
            <div class="text-muted small">OIT Protocol</div>
            <div class="fw-bold"><?= e($patient['oit_protocol']) ?></div>
          </div>
          <div class="col-sm-6 col-lg-3">
            <div class="text-muted small">Current Dose</div>
            <div><span class="badge bg-blue"><?= e($patient['current_dose_mcg']) ?> mcg</span></div>
          </div>
          <?php if ($nextDose): ?>
          <div class="col-sm-6 col-lg-3">
            <div class="text-muted small">Next Scheduled Dose</div>
            <div class="fw-bold"><?= e($nextDose['scheduled_date']) ?>
              <span class="badge bg-azure-lt ms-1"><?= e($nextDose['dose_mcg']) ?> mcg</span></div>
          </div>
          <?php endif; ?>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <ul class="nav nav-tabs mb-3">
      <?php
      $tabs = [
        'overview'  => ['fa-house','Overview'],
        'dose-plan' => ['fa-calendar-days','Dose Plan'],
        'syringes'  => ['fa-syringe','Syringes'],
        'cartons'   => ['fa-box','Cartons'],
        'mar'       => ['fa-clipboard-list','MAR'],
        'food'      => ['fa-utensils','Food Logs'],
        'symptoms'  => ['fa-heart-pulse','Symptoms'],
        'missions'  => ['fa-star','Missions'],
      ];
      foreach ($tabs as $key => [$icon, $label]):
      ?>
      <li class="nav-item">
        <a class="nav-link <?= $tab===$key?'active':'' ?>"
           href="?id=<?= $id ?>&tab=<?= $key ?>">
          <i class="fa-solid <?= $icon ?> me-1"></i><?= $label ?>
        </a>
      </li>
      <?php endforeach; ?>
    </ul>

    <?php if ($tab === 'overview'): ?>
    <div class="row row-cards">
      <div class="col-md-6">
        <div class="card">
          <div class="card-header"><h3 class="card-title">Recent Syringes</h3></div>
          <div class="table-responsive">
            <table class="table table-sm card-table">
              <thead><tr><th>Dose</th><th>Form</th><th>Status</th><th>Expiry</th></tr></thead>
              <tbody>
              <?php foreach (array_slice($syringes,0,5) as $s): ?>
              <tr class="<?= expiryClass($s['final_expiry']) ?>">
                <td><?= e($s['dose_mcg']) ?> mcg</td>
                <td><?= e($s['dose_form']) ?></td>
                <td><?= statusBadge($s['status']) ?></td>
                <td><?= e($s['final_expiry']) ?></td>
              </tr>
              <?php endforeach; ?>
              <?php if (empty($syringes)): ?><tr><td colspan="4" class="text-muted">None.</td></tr><?php endif; ?>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="col-md-6">
        <div class="card">
          <div class="card-header"><h3 class="card-title">Upcoming Doses</h3></div>
          <div class="table-responsive">
            <table class="table table-sm card-table">
              <thead><tr><th>Date</th><th>Dose</th><th>Form</th><th>Status</th></tr></thead>
              <tbody>
              <?php foreach (array_slice($dosePlan,0,5) as $dp): ?>
              <tr>
                <td><?= e($dp['scheduled_date']) ?></td>
                <td><?= e($dp['dose_mcg']) ?> mcg</td>
                <td><?= e($dp['dose_form']) ?></td>
                <td><?= statusBadge($dp['status']) ?></td>
              </tr>
              <?php endforeach; ?>
              <?php if (empty($dosePlan)): ?><tr><td colspan="4" class="text-muted">None.</td></tr><?php endif; ?>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <?php elseif ($tab === 'dose-plan'): ?>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Dose Plan</h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>Scheduled Date</th><th>Dose (mcg)</th><th>Form</th><th>Status</th></tr></thead>
          <tbody>
          <?php foreach ($dosePlan as $dp): ?>
          <tr><td><?= e($dp['scheduled_date']) ?></td><td><?= e($dp['dose_mcg']) ?></td>
              <td><?= e($dp['dose_form']) ?></td><td><?= statusBadge($dp['status']) ?></td></tr>
          <?php endforeach; ?>
          <?php if (empty($dosePlan)): ?><tr><td colspan="4" class="text-muted text-center py-3">No dose plan entries.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>

    <?php elseif ($tab === 'syringes'): ?>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Syringes / Vials</h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>ID</th><th>Dose</th><th>Form</th><th>Drawn</th><th>Scheduled</th><th>Final Expiry</th><th>Seal</th><th>Status</th></tr></thead>
          <tbody>
          <?php foreach ($syringes as $s): ?>
          <tr class="<?= expiryClass($s['final_expiry']) ?>">
            <td><code><?= e($s['id']) ?></code></td>
            <td><?= e($s['dose_mcg']) ?> mcg</td>
            <td><?= e($s['dose_form']) ?></td>
            <td><?= e($s['date_drawn']) ?></td>
            <td><?= e($s['scheduled_dose_date']) ?></td>
            <td><?= e($s['final_expiry']) ?></td>
            <td><?= e($s['seal_type']) ?></td>
            <td><?= statusBadge($s['status']) ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($syringes)): ?><tr><td colspan="8" class="text-muted text-center py-3">No syringes.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>

    <?php elseif ($tab === 'cartons'): ?>
    <div class="card mb-3">
      <div class="card-header"><h3 class="card-title">Milk Cartons</h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>ID</th><th>Lot</th><th>Expiry</th><th>Opened</th><th>Status</th></tr></thead>
          <tbody>
          <?php foreach ($cartons as $c): ?>
          <tr class="<?= expiryClass($c['expiration_date']) ?>">
            <td><code><?= e($c['id']) ?></code></td>
            <td><?= e($c['lot_number']) ?></td>
            <td><?= e($c['expiration_date']) ?></td>
            <td><?= e($c['date_opened']) ?></td>
            <td><?= statusBadge($c['status']) ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($cartons)): ?><tr><td colspan="5" class="text-muted text-center py-3">No cartons.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Powder Supplies</h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>ID</th><th>Lot</th><th>Expiry</th><th>Opened</th><th>Status</th></tr></thead>
          <tbody>
          <?php foreach ($powder as $pw): ?>
          <tr class="<?= expiryClass($pw['expiration_date']) ?>">
            <td><code><?= e($pw['id']) ?></code></td>
            <td><?= e($pw['lot_number']) ?></td>
            <td><?= e($pw['expiration_date']) ?></td>
            <td><?= e($pw['date_opened']) ?></td>
            <td><?= statusBadge($pw['status']) ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($powder)): ?><tr><td colspan="5" class="text-muted text-center py-3">No powder supplies.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>

    <?php elseif ($tab === 'mar'): ?>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Medication Administration Records</h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>Date/Time</th><th>Dose</th><th>Form</th><th>Outcome</th><th>Given By</th><th>Notes</th></tr></thead>
          <tbody>
          <?php foreach ($mar as $m): ?>
          <tr>
            <td><?= e($m['administered_at']) ?></td>
            <td><?= e($m['dose_mcg']) ?> mcg</td>
            <td><?= e($m['dose_form']) ?></td>
            <td><?= statusBadge($m['outcome']) ?></td>
            <td><?= e($m['given_by'] ?? '—') ?></td>
            <td><?= e($m['notes'] ?? '—') ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($mar)): ?><tr><td colspan="6" class="text-muted text-center py-3">No MAR entries.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>

    <?php elseif ($tab === 'food'): ?>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Food Logs</h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>Logged At</th><th>Food</th><th>Timing OK</th></tr></thead>
          <tbody>
          <?php foreach ($food as $f): ?>
          <tr>
            <td><?= e($f['logged_at']) ?></td>
            <td><?= e($f['food_description']) ?></td>
            <td><?= $f['timing_ok'] ? '<span class="badge bg-success">Yes</span>' : '<span class="badge bg-warning">No</span>' ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($food)): ?><tr><td colspan="3" class="text-muted text-center py-3">No food logs.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>

    <?php elseif ($tab === 'symptoms'): ?>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Symptom Logs</h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table">
          <thead><tr><th>Logged At</th><th>Severity</th><th>Symptoms</th><th>Notes</th></tr></thead>
          <tbody>
          <?php foreach ($symptoms as $s): ?>
          <tr>
            <td><?= e($s['logged_at']) ?></td>
            <td><span class="badge bg-<?= (int)$s['severity'] >= 4 ? 'danger' : ((int)$s['severity'] >= 3 ? 'warning' : 'success') ?>"><?= e($s['severity']) ?>/5</span></td>
            <td><?= e($s['symptoms'] ?? '—') ?></td>
            <td><?= e($s['notes'] ?? '—') ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($symptoms)): ?><tr><td colspan="4" class="text-muted text-center py-3">No symptom logs.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>

    <?php elseif ($tab === 'missions'): ?>
    <div class="row row-cards">
      <div class="col-md-8">
        <div class="card">
          <div class="card-header"><h3 class="card-title">Missions</h3></div>
          <div class="table-responsive">
            <table class="table table-vcenter card-table">
              <thead><tr><th>Date</th><th>Type</th><th>Status</th><th>Completed At</th></tr></thead>
              <tbody>
              <?php foreach ($missions as $m): ?>
              <tr>
                <td><?= e($m['mission_date']) ?></td>
                <td><?= e(str_replace('_',' ', $m['mission_type'])) ?></td>
                <td><?= statusBadge($m['status']) ?></td>
                <td><?= e($m['completed_at'] ?? '—') ?></td>
              </tr>
              <?php endforeach; ?>
              <?php if (empty($missions)): ?><tr><td colspan="4" class="text-muted text-center py-3">No missions.</td></tr><?php endif; ?>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="card">
          <div class="card-header"><h3 class="card-title">Badges</h3></div>
          <div class="card-body">
          <?php if (empty($badges)): ?>
            <p class="text-muted">No badges yet.</p>
          <?php else: ?>
            <?php foreach ($badges as $b): ?>
            <span class="badge bg-yellow text-dark me-1 mb-1 p-2" title="<?= e($b['earned_at']) ?>">
              <i class="fa-solid fa-medal me-1"></i><?= e(str_replace('_',' ',$b['badge_type'])) ?>
            </span>
            <?php endforeach; ?>
          <?php endif; ?>
          </div>
        </div>
      </div>
    </div>
    <?php endif; ?>
  </div>
</div>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
