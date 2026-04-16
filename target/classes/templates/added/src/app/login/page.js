'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { authApi } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Alert, Button, Input } from '@/components/ui';
import { Eye, EyeOff, Zap } from 'lucide-react';

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [form, setForm]       = useState({ username: '', password: '' });
  const [remember, setRemember] = useState(false);
  const [showPw, setShowPw]   = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const user = await authApi.login(form.username, form.password);
      login(user, remember);
      router.push('/dashboard');
    } catch (err) {
      setError(err.message || 'Invalid credentials');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-[1fr_440px]">

      {/* Left — visual panel */}
      <div className="relative hidden lg:flex flex-col justify-center px-16 bg-ink overflow-hidden">
        {/* Grid texture */}
        <div className="absolute inset-0 bg-grid opacity-60 pointer-events-none" />
        {/* Amber corner accent */}
        <div className="absolute top-0 left-0 w-1 h-full bg-amber" />
        <div className="absolute top-0 left-0 right-0 h-0.5 bg-amber/30" />

        <div className="relative z-10 max-w-lg">
          <div className="font-mono text-[10px] tracking-[0.25em] uppercase text-amber mb-6">
            Transport Management System
          </div>
          <h1 className="font-display font-black text-5xl text-text leading-[1.1] mb-6">
            Move people.<br/>
            <span className="text-amber">Track everything.</span>
          </h1>
          <p className="text-muted text-lg leading-relaxed mb-12">
            Multi-tenant ticketing with dynamic pricing, M-PESA payments,
            stage management, and real-time seat tracking.
          </p>

          <div className="space-y-4">
            {[
              'Per-tenant M-PESA STK push integration',
              'Dynamic fare windows by date & time',
              'Virtual thread concurrency (Java 21)',
              'Role-based: Super Admin → Stage Attendant',
              'Offline-capable PWA',
            ].map(f => (
              <div key={f} className="flex items-center gap-3 text-sm text-muted">
                <div className="w-1.5 h-1.5 rounded-full bg-amber flex-shrink-0" />
                {f}
              </div>
            ))}
          </div>

          {/* Decorative ticker */}
          <div className="absolute bottom-8 left-16 right-16 flex items-center gap-4 font-mono text-[10px] text-border">
            <Zap size={11} className="text-amber" />
            <span>TRANSITPASS v4.0 · JAVA 21 · SPRING BOOT 3.3 · TIMESCALEDB</span>
          </div>
        </div>
      </div>

      {/* Right — form */}
      <div className="flex flex-col justify-center bg-slate border-l border-border px-8 py-12">
        <div className="mb-10 lg:hidden">
          <span className="font-display font-black text-2xl">Transit<span className="text-amber">Pass</span></span>
        </div>
        <div className="mb-8">
          <h2 className="font-display font-black text-2xl mb-1">Welcome back</h2>
          <p className="text-sm text-muted">Sign in to your account</p>
        </div>

        {error && (
          <div className="mb-4">
            <Alert type="error" message={error} onClose={() => setError('')} />
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Username"
            value={form.username}
            onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
            placeholder="your-username"
            autoComplete="username"
            required
          />

          <div className="space-y-1">
            <label className="label">Password</label>
            <div className="relative">
              <input
                type={showPw ? 'text' : 'password'}
                value={form.password}
                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                className="input pr-10"
                placeholder="••••••••"
                autoComplete="current-password"
                required
              />
              <button
                type="button"
                onClick={() => setShowPw(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-text transition-colors"
              >
                {showPw ? <EyeOff size={15}/> : <Eye size={15}/>}
              </button>
            </div>
          </div>

          <label className="flex items-center gap-2.5 cursor-pointer group">
            <input
              type="checkbox"
              checked={remember}
              onChange={e => setRemember(e.target.checked)}
              className="accent-amber w-3.5 h-3.5"
            />
            <span className="text-sm text-muted group-hover:text-text transition-colors">
              Keep me signed in
            </span>
          </label>

          <Button type="submit" size="lg" loading={loading} className="w-full justify-center mt-2">
            Sign In
          </Button>
        </form>

        <p className="mt-8 text-[11px] text-muted text-center font-mono">
          TRANSITPASS © {new Date().getFullYear()} · All rights reserved
        </p>
      </div>
    </div>
  );
}
