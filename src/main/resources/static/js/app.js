/* TransitPass – PWA App JS */
'use strict';

// ── JWT token storage ────────────────────────────
const Auth = {
  getToken: () => sessionStorage.getItem('token') || localStorage.getItem('token'),
  setToken: (t, remember) => {
    if (remember) localStorage.setItem('token', t);
    else sessionStorage.setItem('token', t);
  },
  clear: () => { sessionStorage.removeItem('token'); localStorage.removeItem('token'); },
  headers: () => ({ 'Content-Type': 'application/json', 'Authorization': `Bearer ${Auth.getToken()}` })
};

// ── API helper ────────────────────────────────────
async function api(method, path, body) {
  const res = await fetch('/api' + path, {
    method,
    headers: Auth.headers(),
    body: body ? JSON.stringify(body) : undefined
  });
  if (res.status === 401) { Auth.clear(); window.location = '/login'; return; }
  return res.json();
}

// ── Service Worker registration ───────────────────
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').then(reg => {
      console.log('[PWA] SW registered:', reg.scope);
    }).catch(err => console.warn('[PWA] SW failed:', err));
  });
}

// ── Online / Offline indicator ────────────────────
function updateOnlineStatus() {
  const bar = document.querySelector('.offline-bar');
  if (!bar) return;
  if (!navigator.onLine) { bar.classList.add('show'); document.body.classList.add('offline'); }
  else { bar.classList.remove('show'); document.body.classList.remove('offline'); }
}
window.addEventListener('online', updateOnlineStatus);
window.addEventListener('offline', updateOnlineStatus);
document.addEventListener('DOMContentLoaded', updateOnlineStatus);

// ── PWA Install prompt ────────────────────────────
let deferredPrompt = null;
window.addEventListener('beforeinstallprompt', e => {
  e.preventDefault();
  deferredPrompt = e;
  const banner = document.getElementById('install-banner');
  if (banner) banner.classList.add('show');
});
function installPWA() {
  if (!deferredPrompt) return;
  deferredPrompt.prompt();
  deferredPrompt.userChoice.then(() => {
    deferredPrompt = null;
    const banner = document.getElementById('install-banner');
    if (banner) banner.classList.remove('show');
  });
}
function dismissInstall() {
  const banner = document.getElementById('install-banner');
  if (banner) banner.classList.remove('show');
}

// ── Mobile nav ────────────────────────────────────
function openSidebar() {
  document.getElementById('sidebar')?.classList.add('open');
  document.getElementById('sidebar-overlay')?.classList.add('open');
}
function closeSidebar() {
  document.getElementById('sidebar')?.classList.remove('open');
  document.getElementById('sidebar-overlay')?.classList.remove('open');
}

// ── Modal helpers ─────────────────────────────────
function openModal(id) { document.getElementById(id)?.classList.add('open'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('open'); }
document.addEventListener('click', e => {
  if (e.target.classList.contains('modal-backdrop')) {
    e.target.classList.remove('open');
  }
});

// ── Flash messages ────────────────────────────────
function showFlash(msg, type = 'info') {
  const el = document.getElementById('flash');
  if (!el) return;
  el.className = `alert alert-${type}`;
  el.textContent = msg;
  el.style.display = 'block';
  setTimeout(() => { el.style.display = 'none'; }, 4000);
}

// ── Login form ────────────────────────────────────
const loginForm = document.getElementById('login-form');
if (loginForm) {
  loginForm.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = loginForm.querySelector('[type=submit]');
    btn.disabled = true; btn.textContent = 'Signing in…';
    const data = {
      username: loginForm.username.value,
      password: loginForm.password.value
    };
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });
      const json = await res.json();
      if (!res.ok) throw new Error(json.error || 'Login failed');
      Auth.setToken(json.token, loginForm.remember?.checked);
      window.location = '/dashboard';
    } catch (err) {
      showFlash(err.message, 'error');
      btn.disabled = false; btn.textContent = 'Sign In';
    }
  });
}

// ── Book ticket form ──────────────────────────────
const bookForm = document.getElementById('book-form');
if (bookForm) {
  bookForm.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = bookForm.querySelector('[type=submit]');
    btn.disabled = true; btn.textContent = 'Processing…';
    try {
      const result = await api('POST', '/tickets/book', {
        tripId: Number(bookForm.tripId.value),
        phoneNumber: bookForm.phoneNumber.value
      });
      if (result?.ticketId) {
        closeModal('book-modal');
        showFlash(`✅ Ticket ${result.ticketId} booked! Check your phone for STK push.`, 'success');
        setTimeout(() => window.location.reload(), 2500);
      } else {
        throw new Error(result?.message || 'Booking failed');
      }
    } catch (err) {
      showFlash(err.message, 'error');
    } finally {
      btn.disabled = false; btn.textContent = 'Confirm & Pay';
    }
  });
}

// ── Page load animation ───────────────────────────
document.querySelectorAll('.page').forEach(p => p.classList.add('page-enter'));

// ── Active nav link ───────────────────────────────
document.querySelectorAll('.topnav-links a, .sidebar a').forEach(a => {
  if (a.href && window.location.pathname.startsWith(new URL(a.href).pathname)) {
    a.classList.add('active');
  }
});
