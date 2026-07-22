<?php
// includes/header.php
// Expects: $pageTitle (string), $activePage (string)
$flash = getFlash();
$user  = currentUser();
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
  <title><?= e($pageTitle ?? 'OIT Management') ?> — OIT Dashboard</title>
  <link rel="preconnect" href="https://cdn.jsdelivr.net">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@tabler/core@latest/dist/css/tabler.min.css">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.6.0/css/all.min.css">
  <style>
    .navbar-brand-image { height: 32px; }
    .expiry-warn  { background: #fff3cd !important; }
    .expiry-crit  { background: #f8d7da !important; }
  </style>
</head>
<body class="antialiased">
<div class="wrapper">
  <!-- Sidebar -->
  <aside class="navbar navbar-vertical navbar-expand-lg" data-bs-theme="dark">
    <div class="container-fluid">
      <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#sidebar-menu">
        <span class="navbar-toggler-icon"></span>
      </button>
      <h1 class="navbar-brand navbar-brand-autodark">
        <a href="patients.php" class="text-white text-decoration-none">
          <i class="fa-solid fa-syringe me-2"></i>OIT System
        </a>
      </h1>
      <div class="collapse navbar-collapse" id="sidebar-menu">
        <ul class="navbar-nav pt-lg-3">
          <li class="nav-item">
            <a class="nav-link <?= $activePage==='patients'?'active':'' ?>" href="patients.php">
              <span class="nav-link-icon"><i class="fa-solid fa-users"></i></span>
              <span class="nav-link-title">Patients</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link <?= $activePage==='dose-plan'?'active':'' ?>" href="dose-plan.php">
              <span class="nav-link-icon"><i class="fa-solid fa-calendar-days"></i></span>
              <span class="nav-link-title">Dose Plans</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link <?= $activePage==='syringes'?'active':'' ?>" href="syringes.php">
              <span class="nav-link-icon"><i class="fa-solid fa-syringe"></i></span>
              <span class="nav-link-title">Syringes / Vials</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link <?= $activePage==='cartons'?'active':'' ?>" href="cartons.php">
              <span class="nav-link-icon"><i class="fa-solid fa-box"></i></span>
              <span class="nav-link-title">Cartons &amp; Powder</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link <?= $activePage==='mar'?'active':'' ?>" href="mar.php">
              <span class="nav-link-icon"><i class="fa-solid fa-clipboard-list"></i></span>
              <span class="nav-link-title">MAR</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link <?= $activePage==='food-logs'?'active':'' ?>" href="food-logs.php">
              <span class="nav-link-icon"><i class="fa-solid fa-utensils"></i></span>
              <span class="nav-link-title">Food Logs</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link <?= $activePage==='symptom-logs'?'active':'' ?>" href="symptom-logs.php">
              <span class="nav-link-icon"><i class="fa-solid fa-heart-pulse"></i></span>
              <span class="nav-link-title">Symptom Logs</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link <?= $activePage==='notifications'?'active':'' ?>" href="notifications.php">
              <span class="nav-link-icon"><i class="fa-solid fa-bell"></i></span>
              <span class="nav-link-title">Notifications</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link <?= $activePage==='missions'?'active':'' ?>" href="missions.php">
              <span class="nav-link-icon"><i class="fa-solid fa-star"></i></span>
              <span class="nav-link-title">Missions</span>
            </a>
          </li>
          <li class="nav-item">
            <a class="nav-link <?= $activePage==='audit-logs'?'active':'' ?>" href="audit-logs.php">
              <span class="nav-link-icon"><i class="fa-solid fa-shield-halved"></i></span>
              <span class="nav-link-title">Audit Logs</span>
            </a>
          </li>
        </ul>
        <div class="mt-auto pt-3 border-top border-secondary">
          <div class="d-flex align-items-center px-3 py-2 text-white small">
            <i class="fa-solid fa-user-circle me-2"></i>
            <span><?= e($user['username']) ?></span>
            <span class="badge bg-azure ms-2"><?= e($user['role']) ?></span>
            <a href="logout.php" class="ms-auto text-white-50" title="Logout"><i class="fa-solid fa-right-from-bracket"></i></a>
          </div>
        </div>
      </div>
    </div>
  </aside>
  <!-- Main content -->
  <div class="page-wrapper">
    <?php if ($flash): ?>
    <div class="container-xl pt-3">
      <div class="alert alert-<?= $flash['type']==='error'?'danger':e($flash['type']) ?> alert-dismissible" role="alert">
        <?= e($flash['msg']) ?>
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
      </div>
    </div>
    <?php endif; ?>
