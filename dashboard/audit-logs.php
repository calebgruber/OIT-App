<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';
requireLogin();

$patientId  = (int)($_GET['patient_id'] ?? 0);
$entityType = $_GET['entity_type'] ?? '';
$action     = $_GET['action']      ?? '';
$dateFrom   = $_GET['date_from']   ?? '';
$dateTo     = $_GET['date_to']     ?? '';
$patients   = dbQuery('SELECT id, first_name, last_name, mrn FROM patients ORDER BY last_name');

// Distinct entity types for filter
$entityTypes = dbQuery('SELECT DISTINCT entity_type FROM audit_logs ORDER BY entity_type');
$actions     = dbQuery('SELECT DISTINCT action FROM audit_logs ORDER BY action');

$where  = [];
$params = [];
if ($patientId > 0)    { $where[] = 'al.patient_id = ?';              $params[] = $patientId; }
if ($entityType !== '') { $where[] = 'al.entity_type = ?';             $params[] = $entityType; }
if ($action !== '')    { $where[] = 'al.action = ?';                   $params[] = $action; }
if ($dateFrom !== '')  { $where[] = 'DATE(al.created_at) >= ?';        $params[] = $dateFrom; }
if ($dateTo !== '')    { $where[] = 'DATE(al.created_at) <= ?';        $params[] = $dateTo; }

$sql = "SELECT al.*, u.username, p.first_name, p.last_name
        FROM audit_logs al
        LEFT JOIN users u ON u.id = al.user_id
        LEFT JOIN patients p ON p.id = al.patient_id"
     . ($where ? ' WHERE ' . implode(' AND ', $where) : '')
     . " ORDER BY al.created_at DESC LIMIT 500";
$logs = dbQuery($sql, $params);

$pageTitle  = 'Audit Logs';
$activePage = 'audit-logs';
require_once __DIR__ . '/includes/header.php';
?>
<div class="page-header d-print-none">
  <div class="container-xl"><h2 class="page-title">Audit Logs</h2></div>
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
            <label class="form-label">Entity Type</label>
            <select name="entity_type" class="form-select">
              <option value="">All</option>
              <?php foreach ($entityTypes as $et): ?>
              <option value="<?= e($et['entity_type']) ?>" <?= $entityType===$et['entity_type']?'selected':'' ?>>
                <?= e($et['entity_type']) ?>
              </option>
              <?php endforeach; ?>
            </select>
          </div>
          <div class="col-md-2">
            <label class="form-label">Action</label>
            <select name="action" class="form-select">
              <option value="">All</option>
              <?php foreach ($actions as $a): ?>
              <option value="<?= e($a['action']) ?>" <?= $action===$a['action']?'selected':'' ?>>
                <?= e($a['action']) ?>
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
          <div class="col-auto">
            <button class="btn btn-primary"><i class="fa-solid fa-filter me-1"></i>Filter</button>
            <a href="audit-logs.php" class="btn btn-ghost-secondary ms-1">Reset</a>
          </div>
        </form>
      </div>
    </div>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Audit Trail <span class="badge bg-blue-lt ms-1"><?= count($logs) ?></span></h3></div>
      <div class="table-responsive">
        <table class="table table-vcenter card-table table-sm">
          <thead><tr><th>Timestamp</th><th>User</th><th>Patient</th><th>Action</th><th>Entity</th><th>Entity ID</th><th>IP</th></tr></thead>
          <tbody>
          <?php foreach ($logs as $l): ?>
          <tr>
            <td class="text-nowrap"><?= e($l['created_at']) ?></td>
            <td><?= e($l['username'] ?? 'system') ?></td>
            <td>
              <?php if ($l['patient_id']): ?>
              <a href="patient-detail.php?id=<?= $l['patient_id'] ?>"><?= e(($l['last_name'] ?? '').', '.($l['first_name'] ?? '')) ?></a>
              <?php else: ?>—<?php endif; ?>
            </td>
            <td><span class="badge bg-azure-lt"><?= e($l['action']) ?></span></td>
            <td><?= e($l['entity_type'] ?? '—') ?></td>
            <td><?= e($l['entity_id'] ?? '—') ?></td>
            <td class="text-muted small"><?= e($l['ip_address'] ?? '—') ?></td>
          </tr>
          <?php endforeach; ?>
          <?php if (empty($logs)): ?><tr><td colspan="7" class="text-muted text-center py-3">No audit log entries found.</td></tr><?php endif; ?>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
