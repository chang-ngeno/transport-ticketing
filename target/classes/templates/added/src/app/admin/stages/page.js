'use client';
import { useEffect, useState } from 'react';
import { useRequireAuth } from '@/lib/auth';
import AppShell from '@/components/layout/AppShell';
import { PageHeader, Button, Input, Modal, Alert, SkeletonRows, EmptyState, useToast } from '@/components/ui';
import { stageApi, tenantApi } from '@/lib/api';
import { fmt } from '@/lib/utils';
import { MapPin, PlusCircle } from 'lucide-react';

const EMPTY = { name: '', location: '', tenantId: '' };

export default function StagesPage() {
  const { is } = useRequireAuth('SUPER_ADMIN', 'TENANT_ADMIN');
  const { show, ToastEl } = useToast();

  const [stages,  setStages]  = useState([]);
  const [tenants, setTenants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal,   setModal]   = useState(false);
  const [form,    setForm]    = useState(EMPTY);
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState('');

  async function load() {
    try {
      const [s, t] = await Promise.all([
        stageApi.list(),
        is('SUPER_ADMIN') ? tenantApi.list() : Promise.resolve([]),
      ]);
      setStages(s); setTenants(t);
    } catch {}
    finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  function set(k, v) { setForm(f => ({ ...f, [k]: v })); }

  async function handleCreate(e) {
    e.preventDefault();
    setSaving(true); setError('');
    try {
      await stageApi.create({
        name:     form.name,
        location: form.location || undefined,
        tenantId: form.tenantId ? Number(form.tenantId) : undefined,
      });
      show('success', `Stage "${form.name}" created`);
      setModal(false); setForm(EMPTY); load();
    } catch (err) {
      setError(err.message);
    } finally { setSaving(false); }
  }

  return (
    <AppShell title="Stages">
      {ToastEl}
      <PageHeader
        title="Stages"
        sub="Admin Portal"
        actions={<Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>New Stage</Button>}
      />

      <div className="grid sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 mb-5">
        {loading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="card p-5 space-y-3">
              <div className="skeleton h-4 w-1/2"/>
              <div className="skeleton h-3 w-3/4"/>
            </div>
          ))
        ) : stages.length === 0 ? (
          <div className="col-span-full">
            <EmptyState message="No stages yet" action={
              <Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>Add Stage</Button>
            }/>
          </div>
        ) : stages.map(s => (
          <div key={s.id} className="card p-5 hover:border-amber/40 transition-colors cursor-default">
            <div className="flex items-start gap-3">
              <div className="w-9 h-9 rounded bg-amber/10 border border-amber/20 flex items-center justify-center flex-shrink-0 mt-0.5">
                <MapPin size={15} className="text-amber"/>
              </div>
              <div className="min-w-0">
                <p className="font-semibold text-sm truncate">{s.name}</p>
                <p className="text-xs text-muted truncate mt-0.5">{s.location || 'No location set'}</p>
                <p className="font-mono text-[10px] text-border mt-2">ID #{s.id}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Table view */}
      {stages.length > 0 && (
        <div className="card">
          <div className="card-header"><span className="card-title">All Stages</span></div>
          <div className="overflow-x-auto">
            <table className="data-table w-full">
              <thead>
                <tr><th>ID</th><th>Name</th><th>Location</th><th>Tenant ID</th><th>Created</th></tr>
              </thead>
              <tbody>
                {stages.map(s => (
                  <tr key={s.id}>
                    <td className="font-mono text-xs text-muted">{s.id}</td>
                    <td className="font-medium">{s.name}</td>
                    <td className="text-muted text-xs">{s.location || '—'}</td>
                    <td className="font-mono text-xs text-muted">{s.tenantId}</td>
                    <td className="font-mono text-xs text-muted">{fmt(s.createdAt, 'dd MMM yyyy')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal
        open={modal}
        onClose={() => { setModal(false); setError(''); setForm(EMPTY); }}
        title="New Stage"
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal(false)}>Cancel</Button>
            <Button onClick={handleCreate} loading={saving}>Create Stage</Button>
          </>
        }
      >
        {error && <Alert type="error" message={error} onClose={() => setError('')}/>}
        <form onSubmit={handleCreate} className="space-y-4">
          <Input label="Stage Name" value={form.name} onChange={e => set('name', e.target.value)} placeholder="Nairobi Stage A" required/>
          <Input label="Location (optional)" value={form.location} onChange={e => set('location', e.target.value)} placeholder="e.g. Tom Mboya St, CBD"/>
          {is('SUPER_ADMIN') && tenants.length > 0 && (
            <div className="space-y-1">
              <label className="label">Tenant</label>
              <select className="input cursor-pointer" value={form.tenantId} onChange={e => set('tenantId', e.target.value)}>
                <option value="">Current tenant</option>
                {tenants.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            </div>
          )}
        </form>
      </Modal>
    </AppShell>
  );
}
