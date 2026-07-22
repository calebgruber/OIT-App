(function () {
  function getPreferredTheme() {
    const saved = localStorage.getItem('chop-theme');
    if (saved === 'light' || saved === 'dark') return saved;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-bs-theme', theme);
    localStorage.setItem('chop-theme', theme);

    document.querySelectorAll('[data-chop-logo]').forEach((img) => {
      const nextSrc = theme === 'dark' ? img.dataset.darkSrc : img.dataset.lightSrc;
      if (nextSrc) img.src = nextSrc;
    });

    document.querySelectorAll('[data-chop-theme-icon]').forEach((icon) => {
      icon.classList.toggle('fa-moon', theme === 'light');
      icon.classList.toggle('fa-sun', theme === 'dark');
    });

    document.querySelectorAll('[data-chop-theme-label]').forEach((label) => {
      label.textContent = theme === 'dark' ? 'Light mode' : 'Dark mode';
    });
  }

  function initThemeToggle() {
    const toggle = document.getElementById('chop-theme-toggle');
    if (!toggle) return;
    toggle.addEventListener('click', function () {
      const current = document.documentElement.getAttribute('data-bs-theme') || getPreferredTheme();
      applyTheme(current === 'dark' ? 'light' : 'dark');
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    applyTheme(document.documentElement.getAttribute('data-bs-theme') || getPreferredTheme());
    initThemeToggle();
  });
})();
