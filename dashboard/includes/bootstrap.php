<?php
// includes/bootstrap.php — loaded on every page before any output
if (session_name() !== SESSION_NAME) {
    session_name(SESSION_NAME);
}
session_start();

// Session timeout check
if (isset($_SESSION['last_activity']) && (time() - $_SESSION['last_activity']) > SESSION_TIMEOUT) {
    session_unset();
    session_destroy();
    header('Location: /dashboard/index.php?timeout=1');
    exit;
}
$_SESSION['last_activity'] = time();

function requireLogin(): void {
    if (empty($_SESSION['user_id'])) {
        header('Location: /dashboard/index.php');
        exit;
    }
}

function currentUser(): array {
    return [
        'id'       => $_SESSION['user_id']   ?? 0,
        'username' => $_SESSION['username']  ?? '',
        'role'     => $_SESSION['role']      ?? '',
    ];
}

function flash(string $type, string $msg): void {
    $_SESSION['flash'] = ['type' => $type, 'msg' => $msg];
}

function getFlash(): ?array {
    $f = $_SESSION['flash'] ?? null;
    unset($_SESSION['flash']);
    return $f;
}

function e(mixed $v): string {
    return htmlspecialchars((string)$v, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}

function statusBadge(string $status): string {
    $map = [
        'ready'     => 'bg-success',
        'active'    => 'bg-success',
        'used'      => 'bg-secondary',
        'expired'   => 'bg-danger',
        'discarded' => 'bg-warning',
        'scheduled' => 'bg-azure',
        'completed' => 'bg-success',
        'skipped'   => 'bg-secondary',
        'pending'   => 'bg-yellow',
        'failed'    => 'bg-danger',
        'success'   => 'bg-success',
    ];
    $cls = $map[strtolower($status)] ?? 'bg-secondary';
    return '<span class="badge ' . $cls . '">' . e($status) . '</span>';
}

function expiryClass(string $expiry): string {
    $ts = strtotime($expiry);
    if ($ts === false) return '';
    if ($ts < time()) return 'table-danger';
    if ($ts < time() + 86400) return 'table-warning';
    return '';
}
