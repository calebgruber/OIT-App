function getCurrentUser() {
  try {
    return JSON.parse(localStorage.getItem('current_user') || 'null');
  } catch (error) {
    console.warn('Unable to parse current user:', error);
    return null;
  }
}

function isAuthenticated() {
  return Boolean(localStorage.getItem('auth_token'));
}

function checkAuth() {
  const path = window.location.pathname;
  const loginPage = path.endsWith('/index.html') || path.endsWith('/dashboard/') || path.endsWith('/dashboard');
  if (!isAuthenticated()) {
    if (!loginPage) {
      window.location.href = 'index.html';
    }
    return false;
  }
  return true;
}

async function logout() {
  try {
    if (window.api?.logout) {
      await window.api.logout();
    }
  } finally {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('current_user');
    window.location.href = 'index.html';
  }
}

window.getCurrentUser = getCurrentUser;
window.checkAuth = checkAuth;
window.logout = logout;
