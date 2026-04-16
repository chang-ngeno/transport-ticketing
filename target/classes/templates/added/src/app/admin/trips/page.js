'use client';
import { useEffect, useState } from 'react';
import { useRequireAuth } from '@/lib/auth';
import AppShell from '@/components/layout/AppShell';
import { PageHeader, Button, Input, Select, Modal, Alert, SkeletonRows, EmptyState, useToast } from '@/components/ui';
import { tripApi, stageApi } from '@/lib/api';
import { fmt, fmtKES, fmtSeats } from '@/lib/utils';
import Link from 'next/link';
import { PlusCircle, TrendingUp, Layers } from 'lucide-react';

const EMPTY = { fromStageId: '', toDestination: '', route: '', departureTime: '', totalSeats: '', basePrice: '' };

export default function AdminTripsPage() {
  const { is } = useRequireAuth('SUPER_ADMIN', 'TENANT_ADMIN');
  const { show, ToastEl } = useToast();

  const [trips,   setTrips]   = useState([]);
  const [stages,  setStages]  = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal,   setModal]   = useState(false);
  const [form,    setForm]    = useState(EMPTY);
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState('');
  const [filter,  setFilter]  = useState('');

  async function load() {
    try {
      const [t, s] = await Promise.all([tripApi.list(), stageApi.list()]);
      setTrips(t); setStages(s);
    } catch {}
    finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  function set(k, v) { setForm(f => ({ ...f, [k]: v })); }

  async function handleCreate(e) {
    e.preventDefault();
    setSaving(true); setError('');
    try {
      await tripApi.create({
        ...form,
        fromStageId: Number(form.fromStageId),
        totalSeats:  Number(form.totalSeats),
        basePrice:   Number(form.basePrice),
      });
      show('success', 'Trip created');
      setModal(false); setForm(EMPTY); load();
    } catch (err) {
      setError(err.message);
    } finally { setSaving(false); }
  }

  const filtered = trips.filter(t =>
    !filter ||
    t.toDestination?.toLowerCase().includes(filter.toLowerCase()) ||
    t.route?.toLowerCase().includes(filter.toLowerCase())
  );

  const stageOpts = stages.map(s => ({ value: s.id, label: s.name }));

  return (
    <AppShell title="Manage Trips">
      {ToastEl}
      <PageHeader
        title="Manage Trips"
        sub="Admin Portal"
        actions={<Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>New Trip</Button>}
      />

      {/* Summary row */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="card p-4 relative stat-amber overflow-hidden">
          <p className="label">Total Trips</p>
          <p className="font-display font-black text-2xl">{trips.length}</p>
        </div>
        <div className="card p-4 relative stat-green overflow-hidden">
          <p className="label">Total Seats</p>
          <p className="font-display font-black text-2xl">{trips.reduce((s, t) => s + t.totalSeats, 0)}</p>
        </div>
        <div className="card p-4 relative stat-sky overflow-hidden">
          <p className="label">Booked Seats</p>
          <p className="font-display font-black text-2xl">{trips.reduce((s, t) => s + t.bookedSeats, 0)}</p>
        </div>
      </div>

      {/* Search */}
      <div className="mb-4">
        <input
          value={filter}
          onChange={e => setFilter(e.target.value)}
          placeholder="Filter by destination or route…"
          className="input max-w-xs text-sm"
        />
      </div>

      <div className="card">
        <div className="overflow-x-auto">
          <table className="data-table w-full">
            <thead>
              <tr>
                <th>ID</th>
                <th>Departure</th>
                <th>From Stage</th>
                <th>To</th>
                <th>Route</th>
                <th>Seats</th>
                <th>Base Price</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? <SkeletonRows rows={5} cols={8}/> :
               filtered.length === 0 ? (
                <tr><td colSpan={8}><EmptyState message="No trips found"/></td></tr>
              ) : filtered.map(t => (
                <tr key={t.id}>
                  <td className="font-mono text-xs text-muted">{t.id}</td>
                  <td className="font-mono text-xs">{fmt(t.departureTime)}</td>
                  <td className="text-xs text-muted">{t.fromStageName || `Stage #${t.fromStageId}`}</td>
                  <td className="font-medium text-sm">{t.toDestination}</td>
                  <td className="text-xs text-muted">{t.route || '—'}</td>
                  <td>
                    <div className="flex items-center gap-1.5">
                      <div className="h-1.5 bg-border rounded-full w-16 overflow-hidden">
                        <div
                          className="h-full bg-amber rounded-full transition-all"
                          style={{ width: `${Math.round((t.bookedSeats / t.totalSeats) * 100)}%` }}
                        />
                      </div>
                      <span className="font-mono text-[10px] text-muted">{fmtSeats(t.bookedSeats, t.totalSeats)}</span>
                    </div>
                  </td>
                  <td className="font-mono text-xs text-amber">{fmtKES(t.pricePerSeat)}</td>
                  <td>
                    <Link href={`/admin/fares?tripId=${t.id}`} className="btn btn-ghost btn-sm">
                      <TrendingUp size={12}/>Fares
                    </Link>
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
        title="New Trip"
        size="lg"
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal(false)}>Cancel</Button>
            <Button onClick={handleCreate} loading={saving}>Create Trip</Button>
          </>
        }
      >
        {error && <Alert type="error" message={error} onClose={() => setError('')}/>}
        <form onSubmit={handleCreate} className="space-y-4">
          <Select label="From Stage" value={form.fromStageId} onChange={e => set('fromStageId', e.target.value)} options={stageOpts} placeholder="Select stage…" required/>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Destination" value={form.toDestination} onChange={e => set('toDestination', e.target.value)} placeholder="Nairobi CBD" required className="col-span-2"/>
            <Input label="Route (optional)" value={form.route} onChange={e => set('route', e.target.value)} placeholder="via Thika Rd" className="col-span-2"/>
            <Input label="Departure Time" type="datetime-local" value={form.departureTime} onChange={e => set('departureTime', e.target.value)} required/>
            <Input label="Total Seats" type="number" min="1" value={form.totalSeats} onChange={e => set('totalSeats', e.target.value)} placeholder="14" required/>
          </div>
          <Input label="Base Price (KES)" type="number" step="0.01" min="0" value={form.basePrice} onChange={e => set('basePrice', e.target.value)} placeholder="150.00" required/>
        </form>
      </Modal>
    </AppShell>
  );
}
