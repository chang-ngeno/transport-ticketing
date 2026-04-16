'use client';
import { useEffect, useState, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { useRequireAuth } from '@/lib/auth';
import AppShell from '@/components/layout/AppShell';
import { PageHeader, Button, Input, Select, Modal, Alert, SkeletonRows, EmptyState, useToast } from '@/components/ui';
import { fareApi, tripApi } from '@/lib/api';
import { fmt, fmtKES } from '@/lib/utils';
import { PlusCircle, TrendingUp, Clock, Info } from 'lucide-react';

const EMPTY = { effectiveFrom: '', effectiveTo: '', price: '' };

function FaresContent() {
  useRequireAuth('SUPER_ADMIN', 'TENANT_ADMIN', 'STAGE_HEAD');
  const params   = useSearchParams();
  const { show, ToastEl } = useToast();

  const [trips,   setTrips]   = useState([]);
  const [fares,   setFares]   = useState([]);
  const [tripId,  setTripId]  = useState(params.get('tripId') || '');
  const [loading, setLoading] = useState(false);
  const [modal,   setModal]   = useState(false);
  const [form,    setForm]    = useState(EMPTY);
  const [saving,  setSaving]  = useState(false);
  const [error,   setError]   = useState('');

  useEffect(() => {
    tripApi.list().then(setTrips).catch(() => {});
  }, []);

  useEffect(() => {
    if (!tripId) { setFares([]); return; }
    setLoading(true);
    fareApi.list(Number(tripId))
      .then(setFares)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [tripId]);

  function set(k, v) { setForm(f => ({ ...f, [k]: v })); }

  async function handleCreate(e) {
    e.preventDefault();
    setSaving(true); setError('');
    try {
      await fareApi.create(Number(tripId), {
        effectiveFrom: form.effectiveFrom,
        effectiveTo:   form.effectiveTo || undefined,
        price:         Number(form.price),
      });
      show('success', 'Fare window saved');
      setModal(false); setForm(EMPTY);
      setFares(await fareApi.list(Number(tripId)));
    } catch (err) {
      setError(err.message);
    } finally { setSaving(false); }
  }

  const selectedTrip  = trips.find(t => String(t.id) === String(tripId));
  const tripOpts = trips.map(t => ({
    value: t.id,
    label: `${fmt(t.departureTime, 'dd MMM HH:mm')} → ${t.toDestination}`,
  }));

  // Timeline: sort fares by effectiveFrom
  const sorted = [...fares].sort((a, b) => new Date(a.effectiveFrom) - new Date(b.effectiveFrom));

  return (
    <AppShell title="Fare Windows">
      {ToastEl}
      <PageHeader
        title="Fare Windows"
        sub="Admin Portal — Dynamic Pricing"
        actions={
          tripId && <Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>Add Window</Button>
        }
      />

      {/* Info banner */}
      <div className="flex items-start gap-3 px-4 py-3 bg-amber/6 border border-amber/20 rounded mb-6 text-sm">
        <Info size={15} className="text-amber mt-0.5 flex-shrink-0"/>
        <p className="text-muted">
          Fare windows override the trip's base price for a specific time range.
          The system picks the window whose <span className="font-mono text-amber text-xs">effectiveFrom ≤ departureTime &lt; effectiveTo</span>.
          Leave <em>Effective To</em> blank for an open-ended fare.
        </p>
      </div>

      {/* Trip selector */}
      <div className="card mb-5">
        <div className="card-body">
          <Select
            label="Select Trip"
            value={tripId}
            onChange={e => setTripId(e.target.value)}
            options={tripOpts}
            placeholder="Choose a trip to manage fares…"
          />
          {selectedTrip && (
            <div className="mt-3 flex flex-wrap gap-4 font-mono text-xs text-muted">
              <span>Base price: <span className="text-amber">{fmtKES(selectedTrip.pricePerSeat)}</span></span>
              <span>Departs: <span className="text-text">{fmt(selectedTrip.departureTime)}</span></span>
              <span>Seats: <span className="text-text">{selectedTrip.totalSeats - selectedTrip.bookedSeats} / {selectedTrip.totalSeats} free</span></span>
            </div>
          )}
        </div>
      </div>

      {tripId && (
        <>
          {/* Visual timeline */}
          {sorted.length > 0 && (
            <div className="card mb-5">
              <div className="card-header">
                <span className="card-title">Pricing Timeline</span>
                <TrendingUp size={14} className="text-muted"/>
              </div>
              <div className="card-body">
                <div className="relative">
                  <div className="absolute left-4 top-0 bottom-0 w-px bg-border"/>
                  <div className="space-y-4 pl-10">
                    {sorted.map((f, i) => (
                      <div key={f.id} className="relative">
                        <div className="absolute -left-[26px] top-1 w-3 h-3 rounded-full bg-amber border-2 border-navy"/>
                        <div className="panel py-3">
                          <div className="flex items-center justify-between flex-wrap gap-2">
                            <div>
                              <span className="font-display font-bold text-xl text-amber">{fmtKES(f.pricePerSeat)}</span>
                              <span className="text-xs text-muted ml-2">per seat</span>
                            </div>
                            <div className="flex items-center gap-1.5 font-mono text-xs text-muted">
                              <Clock size={11}/>
                              <span>{fmt(f.effectiveFrom, 'dd MMM HH:mm')}</span>
                              <span className="text-border">→</span>
                              <span>{f.effectiveTo ? fmt(f.effectiveTo, 'dd MMM HH:mm') : 'Open-ended'}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Table */}
          <div className="card">
            <div className="card-header">
              <span className="card-title">Fare Windows ({fares.length})</span>
              <Button size="sm" onClick={() => setModal(true)}><PlusCircle size={14}/>Add Window</Button>
            </div>
            <div className="overflow-x-auto">
              <table className="data-table w-full">
                <thead>
                  <tr>
                    <th>Effective From</th>
                    <th>Effective To</th>
                    <th>Price / Seat</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? <SkeletonRows rows={3} cols={4}/> :
                   fares.length === 0 ? (
                    <tr><td colSpan={4}><EmptyState message="No fare windows — base price applies"/></td></tr>
                  ) : sorted.map(f => (
                    <tr key={f.id}>
                      <td className="font-mono text-xs">{fmt(f.effectiveFrom)}</td>
                      <td className="font-mono text-xs text-muted">{f.effectiveTo ? fmt(f.effectiveTo) : <span className="badge text-amber bg-amber/10 border-amber/25">Open-ended</span>}</td>
                      <td className="font-mono text-sm text-amber font-semibold">{fmtKES(f.pricePerSeat)}</td>
                      <td className="font-mono text-xs text-muted">{fmt(f.createdAt, 'dd MMM HH:mm')}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {!tripId && (
        <EmptyState message="Select a trip above to view and manage its fare windows"/>
      )}

      {/* Create modal */}
      <Modal
        open={modal}
        onClose={() => { setModal(false); setError(''); setForm(EMPTY); }}
        title="Add Fare Window"
        footer={
          <>
            <Button variant="ghost" onClick={() => setModal(false)}>Cancel</Button>
            <Button onClick={handleCreate} loading={saving}>Save Window</Button>
          </>
        }
      >
        {error && <Alert type="error" message={error} onClose={() => setError('')}/>}
        <form onSubmit={handleCreate} className="space-y-4">
          <Input label="Effective From" type="datetime-local" value={form.effectiveFrom} onChange={e => set('effectiveFrom', e.target.value)} required/>
          <Input
            label="Effective To"
            type="datetime-local"
            value={form.effectiveTo}
            onChange={e => set('effectiveTo', e.target.value)}
            hint="Leave blank for open-ended (no expiry)"
          />
          <Input label="Price per Seat (KES)" type="number" step="0.01" min="0" value={form.price} onChange={e => set('price', e.target.value)} placeholder="200.00" required/>
        </form>
      </Modal>
    </AppShell>
  );
}

export default function FaresPage() {
  return <Suspense fallback={null}><FaresContent/></Suspense>;
}
