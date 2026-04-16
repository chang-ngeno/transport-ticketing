'use client';
import { useEffect, useState } from 'react';
import { useRequireAuth } from '@/lib/auth';
import AppShell from '@/components/layout/AppShell';
import { PageHeader, Button, Input, Select, Modal, Alert, SkeletonRows, EmptyState, useToast, ConfirmDialog } from '@/components/ui';
import { vehicleApi, stageApi } from '@/lib/api';
import { fmt, cn } from '@/lib/utils';
import { PlusCircle, CarFront, CheckCircle, XCircle, ToggleLeft, ToggleRight } from 'lucide-react';

const EMPTY = { registrationNumber: '', capacity: '', stageId: '' };

export default function VehiclesPage() {
  const { is } = useRequireAuth('SUPER_ADMIN', 'TENANT_ADMIN', 'STAGE_HEAD');
  const { show, ToastEl } = useToast();

  const [vehicles,  setVehicles]  = useState([]);
  const [stages,    setStages]    = useState([]);
  const [loading,   setLoading]   = useState(true);
  const [modal,     setModal]     = useState(false);
  const [form,      setForm]      = useState(EMPTY);
  const [saving,    setSaving]    = useState(false);
  const [error,     setError]     = useState('');
  const [toggling,  setToggling]  = useState(null);
  const [filterActive, setFilterActive] = useState('all');

  async function load() {
    try {
      const [v, s] = await Promise.all([vehicleApi.list(), stageApi.list()]);
      setVehicles(v); setStages(s);
    } catch {}
    finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  function set(k, v) { setForm(f => ({ ...f, [k]: v })); }

  async function handleCreate(e) {
    e.preventDefault();
    setSaving(true); setError('');
    try {
      await vehicleApi.create({
        registrationNumber: form.registrationNumber,
        capacity:           Number(form.capacity),
        stageId:            form.stageId ? Number(form.stageId) : undefined,
      });
      show('success', `Vehicle ${form.registrationNumber} added`);
      setModal(false); setForm(EMPTY); load();
    } catch (err) {
      setError(err.message);
    } finally { setSaving(false); }
  }

  async function handleToggle(vehicle) {
    setToggling(vehicle.id);
    try {
      await vehicleApi.toggle(vehicle.id, !vehicle.isActive);
      show('success', `${vehicle.registrationNumber} ${vehicle.isActive ? 'deactivated' : 'activated'}`);
      load();
    } catch (err) {
      show('error', err.message);
    } finally { setToggling(null); }
  }

  const filtered = vehicles.filter(v => {
    if (filterActive === 'active')   return v.isActive;
    if (filterActive === 'inactive') return !v.isActive;
    return true;
  });

  const active   = vehicles.filter(v => v.isActive).length;
  const inactive = vehicles.filter(v => !v.isActive).length;

  return (
    <AppShell title="Vehicles">
      {ToastEl}
      <PageHeader
        title="Vehicles"
        sub="Admin Portal"
        actions={<Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>Add Vehicle</Button>}
      />

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="card p-4 relative stat-amber overflow-hidden">
          <p className="label">Total</p>
          <p className="font-display font-black text-2xl">{vehicles.length}</p>
        </div>
        <div className="card p-4 relative stat-green overflow-hidden">
          <p className="label">Active</p>
          <p className="font-display font-black text-2xl text-green-400">{active}</p>
        </div>
        <div className="card p-4 relative stat-red overflow-hidden">
          <p className="label">Inactive</p>
          <p className="font-display font-black text-2xl text-red-400">{inactive}</p>
        </div>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-1 mb-4">
        {[['all','All'], ['active','Active'], ['inactive','Inactive']].map(([v, l]) => (
          <button key={v} onClick={() => setFilterActive(v)}
            className={`btn btn-sm ${filterActive === v ? 'btn-primary' : 'btn-ghost'}`}>
            {l}
          </button>
        ))}
      </div>

      <div className="card">
        <div className="overflow-x-auto">
          <table className="data-table w-full">
            <thead>
              <tr>
                <th>Vehicle</th>
                <th>Reg. Number</th>
                <th>Capacity</th>
                <th>Stage ID</th>
                <th>Status</th>
                <th>Added</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {loading ? <SkeletonRows rows={5} cols={7}/> :
               filtered.length === 0 ? (
                <tr><td colSpan={7}><EmptyState message="No vehicles found" action={
                  <Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>Add Vehicle</Button>
                }/></td></tr>
              ) : filtered.map(v => (
                <tr key={v.id}>
                  <td>
                    <div className={cn(
                      'w-8 h-8 rounded flex items-center justify-center',
                      v.isActive ? 'bg-green-500/10 border border-green-500/20' : 'bg-border/50 border border-border'
                    )}>
                      <CarFront size={14} className={v.isActive ? 'text-green-400' : 'text-muted'}/>
                    </div>
                  </td>
                  <td className="font-mono font-semibold text-sm">{v.registrationNumber}</td>
                  <td className="font-mono text-xs">{v.capacity} <span className="text-muted">seats</span></td>
                  <td className="font-mono text-xs text-muted">#{v.stageId}</td>
                  <td>
                    {v.isActive
                      ? <span className="badge text-green-400 bg-green-400/10 border-green-400/25">Active</span>
                      : <span className="badge text-muted bg-panel border-border">Inactive</span>
                    }
                  </td>
                  <td className="font-mono text-xs text-muted">{fmt(v.createdAt, 'dd MMM yyyy')}</td>
                  <td>
                    <button
                      onClick={() => handleToggle(v)}
                      disabled={toggling === v.id}
                      className={cn(
                        'btn btn-sm gap-1.5',
                        v.isActive ? 'btn-ghost text-red-400 hover:bg-red-400/10 border-red-400/20' : 'btn-ghost text-green-400 hover:bg-green-400/10 border-green-400/20'
                      )}
                    >
                      {v.isActive ? <ToggleRight size={13}/> : <ToggleLeft size={13}/>}
                      {toggling === v.id ? '…' : v.isActive ? 'Deactivate' : 'Activate'}
                    </button>
                  </td>
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
        title="Add Vehicle"
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal(false)}>Cancel</Button>
            <Button onClick={handleCreate} loading={saving}>Add Vehicle</Button>
          </>
        }
      >
        {error && <Alert type="error" message={error} onClose={() => setError('')}/>}
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Registration Number"
            value={form.registrationNumber}
            onChange={e => set('registrationNumber', e.target.value.toUpperCase())}
            placeholder="KCA 123A"
            required
          />
          <Input
            label="Capacity (seats)"
            type="number"
            min="1"
            max="100"
            value={form.capacity}
            onChange={e => set('capacity', e.target.value)}
            placeholder="14"
            required
          />
          {stages.length > 0 && (
            <Select
              label="Stage (optional — defaults to your stage)"
              value={form.stageId}
              onChange={e => set('stageId', e.target.value)}
              options={stages.map(s => ({ value: s.id, label: s.name }))}
              placeholder="Use my stage"
            />
          )}
        </form>
      </Modal>
    </AppShell>
  );
}
