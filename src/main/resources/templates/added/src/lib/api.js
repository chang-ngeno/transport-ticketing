// ── Token storage ────────────────────────────────
export const TokenStore = {
  get() {
    if (typeof window === 'undefined') return null;
    return sessionStorage.getItem('tp_token') || localStorage.getItem('tp_token');
  },
  set(token, persist = false) {
    if (persist) localStorage.setItem('tp_token', token);
    else sessionStorage.setItem('tp_token', token);
  },
  clear() {
    ['tp_token', 'tp_user'].forEach(k => {
      sessionStorage.removeItem(k);
      localStorage.removeItem(k);
    });
  },
  getUser() {
    try {
      const raw = sessionStorage.getItem('tp_user') || localStorage.getItem('tp_user');
      return raw ? JSON.parse(raw) : null;
    } catch { return null; }
  },
  setUser(user, persist = false) {
    const s = JSON.stringify(user);
    if (persist) localStorage.setItem('tp_user', s);
    else sessionStorage.setItem('tp_user', s);
  },
};

// ── Core fetcher ─────────────────────────────────
async function http(method, path, body) {
  const token = TokenStore.get();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch('/api' + path, {
    method,
    headers,
    body: body != null ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401) {
    TokenStore.clear();
    if (typeof window !== 'undefined') window.location.href = '/login';
    throw new Error('Unauthorised');
  }

  let data = {};
  try { data = await res.json(); } catch {}

  if (!res.ok) {
    const err = new Error(data.message || `Error ${res.status}`);
    err.status = res.status;
    err.details = data.details;
    throw err;
  }
  return data;
}

const get   = (path)        => http('GET',   path);
const post  = (path, body)  => http('POST',  path, body);
const patch = (path, body)  => http('PATCH', path, body);

// ── Auth ─────────────────────────────────────────
export const authApi = {
  login: (username, password) => post('/auth/login', { username, password }),
};

// ── Tenants ───────────────────────────────────────
export const tenantApi = {
  list:   ()     => get('/admin/tenants'),
  create: (body) => post('/admin/tenants', body),
};

// ── Users ─────────────────────────────────────────
export const userApi = {
  create: (body) => post('/tenant/users', body),
};

// ── Stages ────────────────────────────────────────
export const stageApi = {
  list:   ()     => get('/tenant/stages'),
  create: (body) => post('/tenant/stages', body),
};

// ── Trips ─────────────────────────────────────────
export const tripApi = {
  list:   ()     => get('/tenant/trips'),
  create: (body) => post('/tenant/trips', body),
};

// ── Fares ─────────────────────────────────────────
export const fareApi = {
  list:   (tripId)        => get(`/tenant/trips/${tripId}/fares`),
  create: (tripId, body)  => post(`/tenant/trips/${tripId}/fares`, body),
};

// ── Vehicles ──────────────────────────────────────
export const vehicleApi = {
  list:   ()              => get('/stage/vehicles'),
  create: (body)          => post('/stage/vehicles', body),
  toggle: (id, active)    => patch(`/stage/vehicles/${id}/toggle`, { active }),
};

// ── Tickets ───────────────────────────────────────
export const ticketApi = {
  list:      ()           => get('/tickets'),
  getById:   (id)         => get(`/tickets/${id}`),
  book:      (body)       => post('/tickets/book', body),
  bookBatch: (body)       => post('/tickets/book/batch', body),
};
