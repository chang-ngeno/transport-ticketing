'use client';
import { createContext, useContext, useEffect, useState } from 'react';
import { TokenStore } from '@/lib/api';

const AuthCtx = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser]   = useState(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const u = TokenStore.getUser();
    if (u) setUser(u);
    setReady(true);
  }, []);

  function login(u, persist = false) {
    TokenStore.set(u.token, persist);
    TokenStore.setUser(u, persist);
    setUser(u);
  }

  function logout() {
    TokenStore.clear();
    setUser(null);
    window.location.href = '/login';
  }

  /** Check if current user has any of the given roles */
  function is(...roles) {
    return !!user && roles.includes(user.role);
  }

  return (
    <AuthCtx.Provider value={{ user, login, logout, is, ready }}>
      {children}
    </AuthCtx.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthCtx);
  if (!ctx) throw new Error('useAuth must be inside AuthProvider');
  return ctx;
}

export function useRequireAuth(...roles) {
  const auth = useAuth();
  useEffect(() => {
    if (!auth.ready) return;
    if (!auth.user) { window.location.href = '/login'; return; }
    if (roles.length && !roles.includes(auth.user.role)) {
      window.location.href = '/dashboard';
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auth.ready, auth.user]);
  return auth;
}
