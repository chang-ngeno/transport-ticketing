'use client';
import { useEffect, useState } from 'react';
import { useRequireAuth } from '@/lib/auth';
import AppShell from '@/components/layout/AppShell';
import { PageHeader, Button, Input, Select, Modal, Alert, RoleBadge, useToast, EmptyState, SkeletonRows, SectionDivider } from '@/components/ui';
import { userApi, stageApi, tenantApi } from '@/lib/api';
import { fmt, ROLE_LABEL } from '@/lib/utils';
import { PlusCircle, Eye, EyeOff, Users } from 'lucide-react';

const ROLES = [
  { value: 'TENANT_ADMIN',    label: 'Tenant Admin' },
  { value: 'STAGE_HEAD',      label: 'Stage Head' },
  { value: 'STAGE_ATTENDANT', label: 'Stage Attendant' },
];

const SUPER_ROLES = [
  { value: 'SUPER_ADMIN',     label: 'Super Admin' },
  ...ROLES,
];

const EMPTY = { username: '', password: '', role: '', tenantId: '', stageId: '' };

export default function UsersPage() {
  const { is } = useRequireAuth('SUPER_ADMIN', 'TENANT_ADMIN');
  const { show, ToastEl } = useToast();

  const [users,   setUsers]   = useState([]);
  const [stages,  setStages]  = useState([]);
  const [tenants, setTenants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal,   setModal]   = useState(false);
  const [form,    setForm]    = useState(EMPTY);
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState('');
  const [showPw,  setShowPw]  = useState(false);

  async function load() {
    try {
      const [s, t] = await Promise.all([stageApi.list(), is('SUPER_ADMIN') ? tenantApi.list() : Promise.resolve([])]);
      setStages(s);
      setTenants(t);
    } catch {}
    finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  function set(k, v) { setForm(f => ({ ...f, [k]: v })); }

  async function handleCreate(e) {
    e.preventDefault();
    setSaving(true); setError('');
    try {
      await userApi.create({
        username: form.username,
        password: form.password,
        role:     form.role,
        tenantId: form.tenantId ? Number(form.tenantId) : undefined,
        stageId:  form.stageId  ? Number(form.stageId)  : undefined,
      });
      show('success', `User "${form.username}" created`);
      setModal(false); setForm(EMPTY); load();
    } catch (err) {
      setError(err.message);
    } finally { setSaving(false); }
  }

  const needsStage  = ['STAGE_HEAD', 'STAGE_ATTENDANT'].includes(form.role);
  const needsTenant = is('SUPER_ADMIN');
  const roleOptions = is('SUPER_ADMIN') ? SUPER_ROLES : ROLES;

  return (
    <AppShell title="Users">
      {ToastEl}
      <PageHeader
        title="Users"
        sub="Admin Portal"
        actions={<Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>New User</Button>}
      />

      <div className="card">
        <div className="overflow-x-auto">
          <table className="data-table w-full">
            <thead>
              <tr>
                <th>Username</th>
                <th>Role</th>
                <th>Tenant ID</th>
                <th>Stage ID</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {loading ? <SkeletonRows rows={5} cols={5}/> : (
                <tr>
                  <td colSpan={5}>
                    <EmptyState
                      message="Users are created here but listed from the server — create one to see it"
                      action={<Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>Add User</Button>}
                    />
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <Modal
        open={modal}
        onClose={() => { setModal(false); setError(''); setForm(EMPTY); }}
        title="New User"
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal(false)}>Cancel</Button>
            <Button onClick={handleCreate} loading={saving}>Create User</Button>
          </>
        }
      >
        {error && <Alert type="error" message={error} onClose={() => setError('')}/>}
        <form onSubmit={handleCreate} className="space-y-4">
          <Input label="Username" value={form.username} onChange={e => set('username', e.target.value)} placeholder="jane.doe" required autoComplete="off"/>

          <div className="space-y-1">
            <label className="label">Password</label>
            <div className="relative">
              <input
                type={showPw ? 'text' : 'password'}
                value={form.password}
                onChange={e => set('password', e.target.value)}
                className="input pr-10"
                placeholder="••••••••"
                required
                autoComplete="new-password"
              />
              <button type="button" onClick={() => setShowPw(v => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-text transition-colors">
                {showPw ? <EyeOff size={14}/> : <Eye size={14}/>}
              </button>
            </div>
          </div>

          <Select
            label="Role"
            value={form.role}
            onChange={e => set('role', e.target.value)}
            options={roleOptions}
            placeholder="Select role…"
            required
          />

          {needsTenant && (
            <Select
              label="Tenant"
              value={form.tenantId}
              onChange={e => set('tenantId', e.target.value)}
              options={tenants.map(t => ({ value: t.id, label: t.name }))}
              placeholder="Select tenant…"
            />
          )}

          {needsStage && (
            <Select
              label="Stage"
              value={form.stageId}
              onChange={e => set('stageId', e.target.value)}
              options={stages.map(s => ({ value: s.id, label: s.name }))}
              placeholder="Select stage…"
            />
          )}
        </form>
      </Modal>
    </AppShell>
  );
}
