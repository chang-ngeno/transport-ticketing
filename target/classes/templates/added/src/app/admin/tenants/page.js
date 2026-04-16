'use client';
import { useEffect, useState } from 'react';
import { useRequireAuth } from '@/lib/auth';
import AppShell from '@/components/layout/AppShell';
import { PageHeader, Button, Input, Modal, Alert, SkeletonRows, EmptyState, useToast } from '@/components/ui';
import { tenantApi } from '@/lib/api';
import { fmt } from '@/lib/utils';
import { Building2, PlusCircle, Eye, EyeOff, ShieldCheck } from 'lucide-react';

const EMPTY = { name: '', mpesaShortcode: '', consumerKey: '', consumerSecret: '', passkey: '' };

export default function TenantsPage() {
  useRequireAuth('SUPER_ADMIN');
  const { show, ToastEl } = useToast();

  const [tenants, setTenants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal,   setModal]   = useState(false);
  const [form,    setForm]    = useState(EMPTY);
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState('');
  const [reveal,  setReveal]  = useState({});

  async function load() {
    try { setTenants(await tenantApi.list()); }
    catch {} finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  function set(k, v) { setForm(f => ({ ...f, [k]: v })); }
  function toggleReveal(k) { setReveal(r => ({ ...r, [k]: !r[k] })); }

  async function handleCreate(e) {
    e.preventDefault();
    setSaving(true); setError('');
    try {
      await tenantApi.create(form);
      show('success', `Tenant "${form.name}" created`);
      setModal(false); setForm(EMPTY); load();
    } catch (err) {
      setError(err.message);
    } finally { setSaving(false); }
  }

  return (
    <AppShell title="Tenants">
      {ToastEl}
      <PageHeader
        title="Tenants"
        sub="Admin Portal"
        actions={<Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>New Tenant</Button>}
      />

      {/* Super admin badge */}
      <div className="flex items-center gap-2 px-4 py-2.5 bg-amber/8 border border-amber/25 rounded mb-6 w-fit">
        <ShieldCheck size={14} className="text-amber"/>
        <span className="font-mono text-xs text-amber">Super Admin — Full tenant management access</span>
      </div>

      <div className="card">
        <div className="overflow-x-auto">
          <table className="data-table w-full">
            <thead>
              <tr>
                <th>#</th>
                <th>Tenant Name</th>
                <th>M-PESA Shortcode</th>
                <th>Credentials</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {loading ? <SkeletonRows rows={4} cols={5}/> :
               tenants.length === 0 ? (
                <tr><td colSpan={5}><EmptyState message="No tenants yet — create the first one" action={
                  <Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>Add Tenant</Button>
                }/></td></tr>
              ) : tenants.map(t => (
                <tr key={t.id}>
                  <td className="font-mono text-xs text-muted">{t.id}</td>
                  <td>
                    <div className="flex items-center gap-2">
                      <div className="w-7 h-7 rounded bg-amber/10 border border-amber/20 flex items-center justify-center flex-shrink-0">
                        <Building2 size={13} className="text-amber"/>
                      </div>
                      <span className="font-medium">{t.name}</span>
                    </div>
                  </td>
                  <td className="font-mono text-xs text-amber">{t.mpesaShortcode}</td>
                  <td>
                    <span className="badge text-green-400 bg-green-400/10 border-green-400/25">
                      Encrypted
                    </span>
                  </td>
                  <td className="font-mono text-xs text-muted">{fmt(t.createdAt, 'dd MMM yyyy')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create modal */}
      <Modal
        open={modal}
        onClose={() => { setModal(false); setError(''); setForm(EMPTY); }}
        title="New Tenant"
        size="lg"
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal(false)}>Cancel</Button>
            <Button onClick={handleCreate} loading={saving}>Create Tenant</Button>
          </>
        }
      >
        {error && <Alert type="error" message={error} onClose={() => setError('')}/>}
        <form onSubmit={handleCreate} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Input label="Tenant Name" value={form.name} onChange={e => set('name', e.target.value)} placeholder="Nairobi Express" required className="col-span-2"/>
            <Input label="M-PESA Shortcode" value={form.mpesaShortcode} onChange={e => set('mpesaShortcode', e.target.value)} placeholder="174379" required/>
          </div>

          <div className="pt-2 pb-1">
            <div className="flex items-center gap-2 mb-3">
              <div className="flex-1 h-px bg-border"/>
              <span className="font-mono text-[10px] tracking-widest uppercase text-muted">M-PESA Credentials (Encrypted at rest)</span>
              <div className="flex-1 h-px bg-border"/>
            </div>
          </div>

          {[
            { key: 'consumerKey',    label: 'Consumer Key' },
            { key: 'consumerSecret', label: 'Consumer Secret' },
            { key: 'passkey',        label: 'Passkey' },
          ].map(({ key, label }) => (
            <div key={key} className="space-y-1">
              <label className="label">{label}</label>
              <div className="relative">
                <input
                  type={reveal[key] ? 'text' : 'password'}
                  value={form[key]}
                  onChange={e => set(key, e.target.value)}
                  className="input pr-10"
                  placeholder="••••••••••••"
                  required
                />
                <button type="button" onClick={() => toggleReveal(key)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-text transition-colors">
                  {reveal[key] ? <EyeOff size={14}/> : <Eye size={14}/>}
                </button>
              </div>
            </div>
          ))}

          <p className="text-[11px] text-muted bg-panel rounded px-3 py-2 border border-border">
            Credentials are AES-256 encrypted using a per-tenant salt before storage.
            The master encryption password never leaves the server.
          </p>
        </form>
      </Modal>
    </AppShell>
  );
}
