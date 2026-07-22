<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';
requireLogin();

$patientId = (int)($_GET['patient_id'] ?? 0);
$unreadOnly = isset($_GET['unread']);
$patients  = dbQuery('SELECT id, first_name, last_name, mrn FROM patients ORDER BY last_name');

// Mark as read
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['mark_read'])) {
    $nid = (int)$_POST['mark_read'];
    db()->prepare('UPDATE notifications SET is_read = 1 WHERE id = ?')->execute([$nid]);
    header('Location: notifications.php?' . http_build_query(array_filter(['patient_id' => $patientId])));
    exit;
}

$where  = [];
$params = [];
if ($patientId > 0) { $where[] = 'n.patient_id = ?'; $params[] = $patientId; }
if ($unreadOnly)    { $where[] = 'n.is_read = 0'; }

$sql = "SELECT n.*, p.first_name, p.last_name FROM notifications n
        JOIN patients p ON p.id = n.patient_id"
     . ($where ? ' WHERE ' . implode(' AND ', $where) : '')
     . " ORDER BY n.created_at DESC LIMIT 200";
$notes = dbQuery($sql, $params);
$unreadCount = dbScalar('SELECT COUNT(*) FROM notifications WHERE is_read = 0');

$pageTitle  = 'Notifications';
$activePage = 'notifications';
require_once __DIR__ . '/includes/header.php';
?>
<div class="page-header d-print-none">
  <div class="container-xl">
    <div class="row align-items-center">
      <div class="col"><h2 class="page-title">Notifications
        <?php if ($unreadCount > 0): ?>
        <span class="badge bg-danger ms-1"><?= (int)$unreadCount ?> unread</span>
        <?php endif; ?>
      </h2></div>
    </div>
  </div>
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
          <div class="col-auto d-flex align-items-end">
            <label class="form-check me-3">
              <input type="checkbox" name="unread" class="form-check-input" <?= $unreadOnly?'checked':'' ?>>
              <span class="form-check-label">Unread only</span>
            </label>
            <button class="btn btn-primary"><i class="fa-solid fa-filter me-1"></i>Filter</button>
            <a href="notifications.php" class="btn btn-ghost-secondary ms-1">Reset</a>
          </div>
        </form>
      </div>
    </div>
    <div class="card">
      <div class="card-header"><h3 class="card-title">Notifications <span class="badge bg-blue-lt ms-1"><?= count($notes) ?></span></h3></div>
      <div class="list-group list-group-flush">
      <?php if (empty($notes)): ?>
        <div class="list-group-item text-muted text-center py-4">No notifications found.</div>
      <?php else: ?>
        <?php foreach ($notes as $n): ?>
        <div class="list-group-item <?= !$n['is_read'] ? 'list-group-item-primary' : '' ?>">
          <div class="row align-items-center">
            <div class="col">
              <div class="fw-bold"><?= e($n['title']) ?></div>
              <div class="text-muted small"><?= e($n['message']) ?></div>
              <div class="text-muted small mt-1">
                <i class="fa-solid fa-user me-1"></i><?= e($n['last_name'].', '.$n['first_name']) ?>
                &nbsp;·&nbsp;
                <span class="badge bg-secondary"><?= e($n['type']) ?></span>
                &nbsp;·&nbsp;
                <?= e($n['created_at']) ?>
              </div>
            </div>
            <?php if (!$n['is_read']): ?>
            <div class="col-auto">
              <form method="POST">
                <input type="hidden" name="mark_read" value="<?= $n['id'] ?>">
                <?php if ($patientId): ?><input type="hidden" name="patient_id" value="<?= $patientId ?>"><?php endif; ?>
                <button class="btn btn-sm btn-outline-success">Mark read</button>
              </form>
            </div>
            <?php endif; ?>
          </div>
        </div>
        <?php endforeach; ?>
      <?php endif; ?>
      </div>
    </div>
  </div>
</div>
<?php require_once __DIR__ . '/includes/footer.php'; ?>
