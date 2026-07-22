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
  <script>
    (() => {
      try {
        const saved = localStorage.getItem('chop-theme');
        const theme = (saved === 'light' || saved === 'dark')
          ? saved
          : (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
        document.documentElement.setAttribute('data-bs-theme', theme);
      } catch (e) {}
    })();
  </script>
  <link rel="preconnect" href="https://cdn.jsdelivr.net">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@tabler/core@latest/dist/css/tabler.min.css">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.6.0/css/all.min.css">
  <link rel="stylesheet" href="assets/chop/chop-theme.css">
</head>
<body class="chop-login-page">
<div class="login-container p-3">
  <div class="login-card">
    <div class="d-flex justify-content-end mb-3">
      <button type="button" class="btn btn-outline-secondary btn-sm" id="chop-theme-toggle">
        <i class="fa-solid fa-moon me-1" data-chop-theme-icon></i>
        <span data-chop-theme-label>Dark mode</span>
      </button>
    </div>
    <div class="card shadow-lg border-0">
      <div class="card-body p-4 p-md-5">
        <div class="text-center mb-4">
          <div class="mb-3">
            <img
              src="assets/chop/images/chop-logo-full-light.png"
              data-chop-logo
              data-light-src="assets/chop/images/chop-logo-full-light.png"
              data-dark-src="assets/chop/images/chop-logo-full-dark.png"
              alt="CHOP Logo"
              class="chop-login-logo"
            >
          </div>
          <h1 class="h3 mb-1">CHOP OIT Management</h1>
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
<script src="assets/chop/chop-theme.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@tabler/core@latest/dist/js/tabler.min.js"></script>
</body>
</html>
