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
  const token = Auth.getToken();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch('/api' + path, {
    method,
    headers,
    credentials: 'include',   // send AUTH_TOKEN cookie when no Bearer token in storage
    body: body ? JSON.stringify(body) : undefined
  });
  if (res.status === 401) { Auth.clear(); window.location = '/login'; return; }
  const text = await res.text();
  try { return JSON.parse(text); } catch { return text; }
}

// ── Service Worker registration ───────────────────
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
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
  e.preventDefault(); deferredPrompt = e;
  document.getElementById('install-banner')?.classList.add('show');
});
function installPWA() {
  if (!deferredPrompt) return;
  deferredPrompt.prompt();
  deferredPrompt.userChoice.then(() => {
    deferredPrompt = null;
    document.getElementById('install-banner')?.classList.remove('show');
  });
}
function dismissInstall() { document.getElementById('install-banner')?.classList.remove('show'); }

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
  if (e.target.classList.contains('modal-backdrop')) e.target.classList.remove('open');
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

// ── Table helpers ─────────────────────────────────
function setTableBody(tbodyId, html) {
  const el = document.getElementById(tbodyId);
  if (el) el.innerHTML = html;
}

// ── Login form ────────────────────────────────────
const loginForm = document.getElementById('login-form');
if (loginForm) {
  loginForm.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = loginForm.querySelector('[type=submit]');
    btn.disabled = true; btn.textContent = 'Signing in…';
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: loginForm.username.value, password: loginForm.password.value })
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

// ── Stages page ───────────────────────────────────
async function loadStages() {
  const data = await api('GET', '/tenant/stages');
  if (!Array.isArray(data)) { setTableBody('stages-tbody', '<tr><td colspan="3" style="text-align:center;padding:2rem;color:var(--text-muted);">Failed to load stages</td></tr>'); return; }
  if (!data.length) { setTableBody('stages-tbody', '<tr><td colspan="3" style="text-align:center;padding:2rem;color:var(--text-muted);">No stages found</td></tr>'); return; }
  setTableBody('stages-tbody', data.map(s => `
    <tr>
      <td>${s.name}</td>
      <td style="color:var(--text-muted);font-size:0.85rem;">${s.location || '–'}</td>
      <td class="mono" style="font-size:0.75rem;">${s.id}</td>
    </tr>`).join(''));
}

const stageForm = document.getElementById('create-stage-form');
if (stageForm) {
  stageForm.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = stageForm.querySelector('[type=submit]');
    btn.disabled = true; btn.textContent = 'Creating…';
    try {
      const body = { name: stageForm.name.value, location: stageForm.location.value };
      const tenantId = stageForm.tenantId?.value;
      if (tenantId) body.tenantId = Number(tenantId);
      const result = await api('POST', '/tenant/stages', body);
      if (result?.id) {
        closeModal('create-stage-modal');
        stageForm.reset();
        showFlash(`Stage '${result.name}' created.`, 'success');
        loadStages();
      } else {
        throw new Error(result?.message || 'Failed to create stage');
      }
    } catch (err) { showFlash(err.message, 'error'); }
    finally { btn.disabled = false; btn.textContent = 'Create Stage'; }
  });
  loadStages();
}

// ── Trips page ────────────────────────────────────
async function loadTrips() {
  const data = await api('GET', '/tenant/trips');
  if (!Array.isArray(data)) { setTableBody('trips-tbody', '<tr><td colspan="6" style="text-align:center;padding:2rem;color:var(--text-muted);">Failed to load trips</td></tr>'); return; }
  if (!data.length) { setTableBody('trips-tbody', '<tr><td colspan="6" style="text-align:center;padding:2rem;color:var(--text-muted);">No trips found</td></tr>'); return; }
  setTableBody('trips-tbody', data.map(t => {
    const dep = t.departureTime ? new Date(t.departureTime).toLocaleString('en-KE', {day:'2-digit',month:'short',year:'numeric',hour:'2-digit',minute:'2-digit'}) : '–';
    const seats = `${t.totalSeats - t.bookedSeats}<span style="color:var(--text-muted);font-size:0.75rem;">/${t.totalSeats}</span>`;
    const price = `KES ${Number(t.pricePerSeat).toLocaleString('en-KE', {minimumFractionDigits:2})}`;
    return `<tr>
      <td class="mono" style="font-size:0.85rem;">${dep}</td>
      <td>${t.toDestination || '–'}</td>
      <td style="color:var(--text-muted);">${t.route || '–'}</td>
      <td>${seats}</td>
      <td class="mono">${price}</td>
      <td style="display:flex;gap:0.5rem;">
        <a href="/tickets/book?tripId=${t.id}" class="btn btn-outline btn-sm">Book</a>
        <a href="/tenant/trips/${t.id}/fares" class="btn btn-ghost btn-sm">Fares</a>
      </td>
    </tr>`;
  }).join(''));
}

async function loadStagesIntoSelect(selectId) {
  const stages = await api('GET', '/tenant/stages');
  const sel = document.getElementById(selectId);
  if (!sel || !Array.isArray(stages)) return;
  sel.innerHTML = '<option value="">Select stage…</option>' +
    stages.map(s => `<option value="${s.id}">${s.name}</option>`).join('');
}

const tripForm = document.getElementById('create-trip-form');
if (tripForm) {
  loadStagesIntoSelect('trip-from-stage');
  tripForm.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = tripForm.querySelector('[type=submit]');
    btn.disabled = true; btn.textContent = 'Creating…';
    try {
      const body = {
        fromStageId:   Number(tripForm.fromStageId.value),
        toDestination: tripForm.toDestination.value,
        route:         tripForm.route.value || null,
        departureTime: tripForm.departureTime.value,
        totalSeats:    Number(tripForm.totalSeats.value),
        basePrice:     Number(tripForm.basePrice.value)
      };
      const tenantId = tripForm.tenantId?.value;
      if (tenantId) body.tenantId = Number(tenantId);
      const result = await api('POST', '/tenant/trips', body);
      if (result?.id) {
        closeModal('create-trip-modal');
        tripForm.reset();
        showFlash(`Trip to '${result.toDestination}' created.`, 'success');
        loadTrips();
      } else {
        throw new Error(result?.message || 'Failed to create trip');
      }
    } catch (err) { showFlash(err.message, 'error'); }
    finally { btn.disabled = false; btn.textContent = 'Create Trip'; }
  });
  loadTrips();
}

// ── Vehicles page ─────────────────────────────────
async function loadVehicles() {
  const data = await api('GET', '/stage/vehicles');
  if (!Array.isArray(data)) { setTableBody('vehicles-tbody', '<tr><td colspan="4" style="text-align:center;padding:2rem;color:var(--text-muted);">Failed to load vehicles</td></tr>'); return; }
  if (!data.length) { setTableBody('vehicles-tbody', '<tr><td colspan="4" style="text-align:center;padding:2rem;color:var(--text-muted);">No vehicles found</td></tr>'); return; }
  setTableBody('vehicles-tbody', data.map(v => `
    <tr>
      <td class="mono">${v.registrationNumber}</td>
      <td>${v.capacity} seats</td>
      <td><span class="badge ${v.isActive ? 'badge-paid' : 'badge-failed'}">${v.isActive ? 'Active' : 'Inactive'}</span></td>
      <td><button class="btn btn-ghost btn-sm" onclick="toggleVehicle(${v.id}, ${v.isActive})">${v.isActive ? 'Deactivate' : 'Activate'}</button></td>
    </tr>`).join(''));
}

async function toggleVehicle(id, currentActive) {
  await api('PATCH', `/stage/vehicles/${id}/toggle`, { active: !currentActive });
  loadVehicles();
}

const vehicleForm = document.getElementById('create-vehicle-form');
if (vehicleForm) {
  vehicleForm.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = vehicleForm.querySelector('[type=submit]');
    btn.disabled = true; btn.textContent = 'Registering…';
    try {
      const body = {
        registrationNumber: vehicleForm.registrationNumber.value.toUpperCase(),
        capacity: Number(vehicleForm.capacity.value)
      };
      const stageId = vehicleForm.stageId?.value;
      if (stageId) body.stageId = Number(stageId);
      const result = await api('POST', '/stage/vehicles', body);
      if (result?.id) {
        closeModal('create-vehicle-modal');
        vehicleForm.reset();
        showFlash(`Vehicle ${result.registrationNumber} registered.`, 'success');
        loadVehicles();
      } else {
        throw new Error(result?.message || 'Failed to register vehicle');
      }
    } catch (err) { showFlash(err.message, 'error'); }
    finally { btn.disabled = false; btn.textContent = 'Register'; }
  });
  loadVehicles();
}

// ── Tickets page ──────────────────────────────────
async function loadTickets() {
  const data = await api('GET', '/tickets');
  if (!Array.isArray(data)) { setTableBody('tickets-tbody', '<tr><td colspan="6" style="text-align:center;padding:2rem;color:var(--text-muted);">Failed to load tickets</td></tr>'); return; }
  if (!data.length) { setTableBody('tickets-tbody', '<tr><td colspan="6" style="text-align:center;padding:2rem;color:var(--text-muted);">No bookings found</td></tr>'); return; }

  const search = document.getElementById('ticket-search')?.value.toLowerCase() || '';
  const status = document.getElementById('ticket-status')?.value || '';
  const filtered = data.filter(b =>
    (!search || b.ticketId.toLowerCase().includes(search) || b.phoneNumber.includes(search)) &&
    (!status || b.status === status)
  );

  setTableBody('tickets-tbody', filtered.map(b => {
    const date = b.createdAt ? new Date(b.createdAt).toLocaleString('en-KE', {day:'2-digit',month:'short',hour:'2-digit',minute:'2-digit'}) : '–';
    const price = `KES ${Number(b.pricePaid).toLocaleString('en-KE', {minimumFractionDigits:2})}`;
    return `<tr>
      <td><span class="mono" style="color:var(--accent);">${b.ticketId}</span></td>
      <td class="mono" style="font-size:0.8rem;">${b.phoneNumber}</td>
      <td class="mono" style="font-size:0.8rem;">${b.tripId}</td>
      <td class="mono">${price}</td>
      <td><span class="badge badge-${b.status.toLowerCase()}">${b.status}</span></td>
      <td class="mono" style="font-size:0.75rem;">${date}</td>
    </tr>`;
  }).join(''));
}

if (document.getElementById('tickets-tbody')) {
  loadTickets();
  document.getElementById('ticket-search')?.addEventListener('input', loadTickets);
  document.getElementById('ticket-status')?.addEventListener('change', loadTickets);
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
        showFlash(`✅ Ticket ${result.ticketId} booked! Check your phone for STK push.`, 'success');
        setTimeout(() => window.location.reload(), 2500);
      } else {
        throw new Error(result?.message || 'Booking failed');
      }
    } catch (err) { showFlash(err.message, 'error'); }
    finally { btn.disabled = false; btn.textContent = 'Confirm & Pay'; }
  });
}

// ── Tenants page (admin) ──────────────────────────
async function loadTenants() {
  const data = await api('GET', '/admin/tenants');
  if (!Array.isArray(data)) { setTableBody('tenants-tbody', '<tr><td colspan="4" style="text-align:center;padding:2rem;color:var(--text-muted);">Failed to load tenants</td></tr>'); return; }
  if (!data.length) { setTableBody('tenants-tbody', '<tr><td colspan="4" style="text-align:center;padding:2rem;color:var(--text-muted);">No tenants found</td></tr>'); return; }
  setTableBody('tenants-tbody', data.map(t => {
    const date = t.createdAt ? new Date(t.createdAt).toLocaleDateString('en-KE', {day:'2-digit',month:'short',year:'numeric'}) : '–';
    return `<tr>
      <td class="mono">${t.id}</td>
      <td>${t.name}</td>
      <td class="mono">${t.mpesaShortcode}</td>
      <td class="mono" style="font-size:0.75rem;">${date}</td>
    </tr>`;
  }).join(''));
}

const tenantForm = document.getElementById('create-tenant-form');
if (tenantForm) {
  tenantForm.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = tenantForm.querySelector('[type=submit]');
    btn.disabled = true; btn.textContent = 'Creating…';
    try {
      const result = await api('POST', '/admin/tenants', {
        name:           tenantForm.name.value,
        mpesaShortcode: tenantForm.mpesaShortcode.value,
        consumerKey:    tenantForm.consumerKey.value,
        consumerSecret: tenantForm.consumerSecret.value,
        passkey:        tenantForm.passkey.value
      });
      if (result?.id) {
        closeModal('create-tenant-modal');
        tenantForm.reset();
        showFlash(`Tenant '${result.name}' created.`, 'success');
        loadTenants();
      } else {
        throw new Error(result?.message || 'Failed to create tenant');
      }
    } catch (err) { showFlash(err.message, 'error'); }
    finally { btn.disabled = false; btn.textContent = 'Create Tenant'; }
  });
  loadTenants();
}

// ── Users pages (admin + tenant) ──────────────────
async function loadUsers(apiPath) {
  const data = await api('GET', apiPath);
  if (!Array.isArray(data)) { setTableBody('users-tbody', '<tr><td colspan="5" style="text-align:center;padding:2rem;color:var(--text-muted);">Failed to load users</td></tr>'); return; }
  if (!data.length) { setTableBody('users-tbody', '<tr><td colspan="5" style="text-align:center;padding:2rem;color:var(--text-muted);">No users found</td></tr>'); return; }
  setTableBody('users-tbody', data.map(u => {
    const date = u.createdAt ? new Date(u.createdAt).toLocaleDateString('en-KE', {day:'2-digit',month:'short',year:'numeric'}) : '–';
    return `<tr>
      <td>${u.username}</td>
      <td class="mono" style="font-size:0.75rem;">${u.role}</td>
      <td class="mono" style="font-size:0.75rem;">${u.tenantId || '–'}</td>
      <td class="mono" style="font-size:0.75rem;">${u.stageId || '–'}</td>
      <td class="mono" style="font-size:0.75rem;">${date}</td>
    </tr>`;
  }).join(''));
}

// admin/users page
const adminUserForm = document.getElementById('create-admin-user-form');
if (adminUserForm) {
  loadUsers('/admin/users');
  adminUserForm.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = adminUserForm.querySelector('[type=submit]');
    btn.disabled = true; btn.textContent = 'Creating…';
    try {
      const body = {
        username: adminUserForm.username.value,
        password: adminUserForm.password.value,
        role:     adminUserForm.role.value
      };
      const tenantId = adminUserForm.tenantId?.value;
      const stageId  = adminUserForm.stageId?.value;
      if (tenantId) body.tenantId = Number(tenantId);
      if (stageId)  body.stageId  = Number(stageId);
      const result = await api('POST', '/tenant/users', body);
      if (result?.id) {
        closeModal('create-user-modal');
        adminUserForm.reset();
        showFlash(`User '${result.username}' created.`, 'success');
        loadUsers('/admin/users');
      } else {
        throw new Error(result?.message || 'Failed to create user');
      }
    } catch (err) { showFlash(err.message, 'error'); }
    finally { btn.disabled = false; btn.textContent = 'Create User'; }
  });
}

// tenant/users page
const tenantUserForm = document.getElementById('create-tenant-user-form');
if (tenantUserForm) {
  loadUsers('/tenant/stages'); // load stages into select
  api('GET', '/tenant/stages').then(stages => {
    const sel = document.getElementById('user-stage-select');
    if (sel && Array.isArray(stages)) {
      sel.innerHTML = '<option value="">None</option>' +
        stages.map(s => `<option value="${s.id}">${s.name}</option>`).join('');
    }
  });
  // also load users using the page's data attribute
  const usersPath = tenantUserForm.dataset.usersPath || '/tenant/users';
  loadTenantUsers();

  tenantUserForm.addEventListener('submit', async e => {
    e.preventDefault();
    const btn = tenantUserForm.querySelector('[type=submit]');
    btn.disabled = true; btn.textContent = 'Creating…';
    try {
      const body = {
        username: tenantUserForm.username.value,
        password: tenantUserForm.password.value,
        role:     tenantUserForm.role.value
      };
      const stageId = tenantUserForm.stageId?.value;
      if (stageId) body.stageId = Number(stageId);
      const result = await api('POST', '/tenant/users', body);
      if (result?.id) {
        closeModal('invite-user-modal');
        tenantUserForm.reset();
        showFlash(`User '${result.username}' created.`, 'success');
        loadTenantUsers();
      } else {
        throw new Error(result?.message || 'Failed to create user');
      }
    } catch (err) { showFlash(err.message, 'error'); }
    finally { btn.disabled = false; btn.textContent = 'Create User'; }
  });
}

async function loadTenantUsers() {
  const data = await api('GET', '/tenant/stages'); // stages to confirm auth, then load users via admin
  // tenant users aren't exposed via REST — use the admin endpoint filtered client-side
  const users = await api('GET', '/admin/users');
  if (!Array.isArray(users)) { setTableBody('users-tbody', '<tr><td colspan="4" style="text-align:center;padding:2rem;color:var(--text-muted);">Failed to load users</td></tr>'); return; }
  if (!users.length) { setTableBody('users-tbody', '<tr><td colspan="4" style="text-align:center;padding:2rem;color:var(--text-muted);">No users found</td></tr>'); return; }
  setTableBody('users-tbody', users.map(u => {
    const date = u.createdAt ? new Date(u.createdAt).toLocaleDateString('en-KE', {day:'2-digit',month:'short',year:'numeric'}) : '–';
    return `<tr>
      <td>${u.username}</td>
      <td class="mono" style="font-size:0.75rem;">${u.role}</td>
      <td class="mono" style="font-size:0.75rem;">${u.stageId || '–'}</td>
      <td class="mono" style="font-size:0.75rem;">${date}</td>
    </tr>`;
  }).join(''));
}

// ── Page load animation ───────────────────────────
document.querySelectorAll('.page').forEach(p => p.classList.add('page-enter'));

// ── Active nav link ───────────────────────────────
document.querySelectorAll('.topnav-links a, .sidebar a').forEach(a => {
  if (a.href && window.location.pathname.startsWith(new URL(a.href).pathname)) {
    a.classList.add('active');
  }
});
