'use client';
import { useEffect, useState } from 'react';
import { useRequireAuth } from '@/lib/auth';
import AppShell from '@/components/layout/AppShell';
import { PageHeader, Button, Modal, Input, Select, Alert, SkeletonRows, EmptyState, useToast } from '@/components/ui';
import { tripApi, stageApi } from '@/lib/api';
import { fmt, fmtKES, fmtSeats } from '@/lib/utils';
import Link from 'next/link';
import { PlusCircle, Bus, ArrowRight, TrendingUp } from 'lucide-react';

const EMPTY_FORM = {
  fromStageId: '', toDestination: '', route: '',
  departureTime: '', totalSeats: '', basePrice: '',
};

export default function TripsPage() {
  const { is } = useRequireAuth();
  const { show, ToastEl } = useToast();

  const [trips,   setTrips]   = useState([]);
  const [stages,  setStages]  = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal,   setModal]   = useState(false);
  const [form,    setForm]    = useState(EMPTY_FORM);
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState('');

  async function load() {
    try {
      const [t, s] = await Promise.all([tripApi.list(), stageApi.list()]);
      setTrips(t);
      setStages(s);
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
      show('success', 'Trip created successfully');
      setModal(false);
      setForm(EMPTY_FORM);
      load();
    } catch (err) {
      setError(err.message);
    } finally { setSaving(false); }
  }

  const canCreate = is('SUPER_ADMIN', 'TENANT_ADMIN');

  return (
    <AppShell title="Trips" actions={
      canCreate && <Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>New Trip</Button>
    }>
      {ToastEl}
      <PageHeader title="Trips" sub="Schedule" actions={
        canCreate && <Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>New Trip</Button>
      }/>

      <div className="card">
        <div className="overflow-x-auto">
          <table className="data-table w-full">
            <thead>
              <tr>
                <th>Departure</th>
                <th>From</th>
                <th>Destination</th>
                <th>Route</th>
                <th>Seats</th>
                <th>Price</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? <SkeletonRows rows={5} cols={7}/> :
               trips.length === 0 ? (
                <tr><td colSpan={7}><EmptyState message="No trips scheduled yet"/></td></tr>
              ) : trips.map(t => (
                <tr key={t.id}>
                  <td className="font-mono text-xs">{fmt(t.departureTime)}</td>
                  <td className="text-muted text-xs">{t.fromStageName || '—'}</td>
                  <td className="font-medium">{t.toDestination}</td>
                  <td className="text-muted text-xs">{t.route || '—'}</td>
                  <td className="font-mono text-xs">{fmtSeats(t.bookedSeats, t.totalSeats)}</td>
                  <td className="font-mono text-amber text-xs">{fmtKES(t.pricePerSeat)}</td>
                  <td>
                    <div className="flex gap-2">
                      <Link href={`/book?tripId=${t.id}`} className="btn btn-outline btn-sm">
                        Book
                      </Link>
                      {is('SUPER_ADMIN', 'TENANT_ADMIN', 'STAGE_HEAD') && (
                        <Link href={`/admin/fares?tripId=${t.id}`} className="btn btn-ghost btn-sm">
                          <TrendingUp size={12}/>Fares
                        </Link>
                      )}
                    </div>
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
        onClose={() => { setModal(false); setError(''); setForm(EMPTY_FORM); }}
        title="New Trip"
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal(false)}>Cancel</Button>
            <Button onClick={handleCreate} loading={saving}>Create Trip</Button>
          </>
        }
      >
        {error && <Alert type="error" message={error} onClose={() => setError('')}/>}
        <form id="trip-form" onSubmit={handleCreate} className="space-y-4">
          <Select
            label="From Stage"
            value={form.fromStageId}
            onChange={e => set('fromStageId', e.target.value)}
            options={stages.map(s => ({ value: s.id, label: s.name }))}
            placeholder="Select stage…"
            required
          />
          <Input label="Destination" value={form.toDestination} onChange={e => set('toDestination', e.target.value)} placeholder="e.g. Nairobi CBD" required/>
          <Input label="Route (optional)" value={form.route} onChange={e => set('route', e.target.value)} placeholder="e.g. via Thika Rd"/>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Departure Time" type="datetime-local" value={form.departureTime} onChange={e => set('departureTime', e.target.value)} required/>
            <Input label="Total Seats" type="number" min="1" value={form.totalSeats} onChange={e => set('totalSeats', e.target.value)} placeholder="14" required/>
          </div>
          <Input label="Base Price (KES)" type="number" step="0.01" min="0" value={form.basePrice} onChange={e => set('basePrice', e.target.value)} placeholder="150.00" required/>
        </form>
      </Modal>
    </AppShell>
  );
}
