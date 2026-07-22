<?php
require_once __DIR__ . '/config/db.php';
require_once __DIR__ . '/includes/bootstrap.php';
require_once __DIR__ . '/includes/db.php';

// Already logged in
if (!empty($_SESSION['user_id'])) {
    header('Location: patients.php');
    exit;
}

$error = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';

    if ($username === '' || $password === '') {
        $error = 'Username and password are required.';
    } else {
        $user = dbOne('SELECT id, username, password_hash, role FROM users WHERE username = ?', [$username]);
        if ($user && password_verify($password, $user['password_hash'])) {
            session_regenerate_id(true);
            $_SESSION['user_id']  = $user['id'];
            $_SESSION['username'] = $user['username'];
            $_SESSION['role']     = $user['role'];
            header('Location: patients.php');
            exit;
        } else {
            $error = 'Invalid username or password.';
        }
    }
}

$timeout = isset($_GET['timeout']);
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
  <title>Login — OIT Management System</title>
  <link rel="preconnect" href="https://cdn.jsdelivr.net">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@tabler/core@latest/dist/css/tabler.min.css">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.6.0/css/all.min.css">
  <style>
    body { background: #f0f4f8; }
    .login-container { min-height: 100vh; display: flex; align-items: center; justify-content: center; }
    .login-card { width: 100%; max-width: 420px; }
  </style>
</head>
<body>
<div class="login-container p-3">
  <div class="login-card">
    <div class="card shadow-lg border-0">
      <div class="card-body p-4 p-md-5">
        <div class="text-center mb-4">
          <div class="mb-3">
            <span class="display-6 text-primary"><i class="fa-solid fa-syringe"></i></span>
          </div>
          <h1 class="h3 mb-1">OIT Management System</h1>
          <p class="text-muted small">Pediatric Oral Immunotherapy Platform</p>
        </div>
        <?php if ($timeout): ?>
        <div class="alert alert-warning">Your session expired. Please log in again.</div>
        <?php endif; ?>
        <?php if ($error): ?>
        <div class="alert alert-danger"><?= e($error) ?></div>
        <?php endif; ?>
        <form method="POST" autocomplete="off" novalidate>
          <div class="mb-3">
            <label class="form-label" for="username">Username</label>
            <div class="input-group">
              <span class="input-group-text"><i class="fa-solid fa-user"></i></span>
              <input type="text" id="username" name="username" class="form-control"
                     value="<?= e($_POST['username'] ?? '') ?>"
                     placeholder="Enter username" required autofocus>
            </div>
          </div>
          <div class="mb-4">
            <label class="form-label" for="password">Password</label>
            <div class="input-group">
              <span class="input-group-text"><i class="fa-solid fa-lock"></i></span>
              <input type="password" id="password" name="password" class="form-control"
                     placeholder="Enter password" required>
            </div>
          </div>
          <button type="submit" class="btn btn-primary w-100">
            <i class="fa-solid fa-right-to-bracket me-2"></i>Sign In
          </button>
        </form>
      </div>
    </div>
  </div>
</div>
<script src="https://cdn.jsdelivr.net/npm/@tabler/core@latest/dist/js/tabler.min.js"></script>
</body>
</html>
