'use client';
import { useEffect, useState, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { useRequireAuth } from '@/lib/auth';
import AppShell from '@/components/layout/AppShell';
import { PageHeader, Button, Input, Select, Alert, StatusBadge, Modal } from '@/components/ui';
import { ticketApi, tripApi } from '@/lib/api';
import { fmt, fmtKES, fmtSeats } from '@/lib/utils';
import { CheckCircle, PlusCircle, Trash2, Phone } from 'lucide-react';

function BookForm() {
  useRequireAuth();
  const params = useSearchParams();

  const [trips,    setTrips]    = useState([]);
  const [mode,     setMode]     = useState('single'); // 'single' | 'batch'
  const [tripId,   setTripId]   = useState(params.get('tripId') || '');
  const [phone,    setPhone]    = useState('');
  const [phones,   setPhones]   = useState(['']);
  const [loading,  setLoading]  = useState(false);
  const [tripsLoading, setTripsLoading] = useState(true);
  const [result,   setResult]   = useState(null); // booking or bookings[]
  const [error,    setError]    = useState('');

  useEffect(() => {
    tripApi.list()
      .then(setTrips)
      .catch(() => {})
      .finally(() => setTripsLoading(false));
  }, []);

  const selectedTrip = trips.find(t => String(t.id) === String(tripId));

  async function handleSingle(e) {
    e.preventDefault();
    setLoading(true); setError(''); setResult(null);
    try {
      const booking = await ticketApi.book({ tripId: Number(tripId), phoneNumber: phone });
      setResult(booking);
    } catch (err) {
      setError(err.message || 'Booking failed');
    } finally { setLoading(false); }
  }

  async function handleBatch(e) {
    e.preventDefault();
    const validPhones = phones.filter(p => p.trim());
    if (!validPhones.length) { setError('Add at least one phone number'); return; }
    setLoading(true); setError(''); setResult(null);
    try {
      const bookings = await ticketApi.bookBatch({ tripId: Number(tripId), phoneNumbers: validPhones });
      setResult(bookings);
    } catch (err) {
      setError(err.message || 'Batch booking failed');
    } finally { setLoading(false); }
  }

  function addPhone() { setPhones(p => [...p, '']); }
  function removePhone(i) { setPhones(p => p.filter((_, j) => j !== i)); }
  function setPhoneAt(i, v) { setPhones(p => p.map((x, j) => j === i ? v : x)); }

  const tripOptions = trips.map(t => ({
    value: t.id,
    label: `${fmt(t.departureTime, 'dd MMM HH:mm')} → ${t.toDestination} | ${fmtKES(t.pricePerSeat)} (${t.totalSeats - t.bookedSeats} seats)`,
  }));

  return (
    <AppShell title="Book Ticket">
      <PageHeader title="Book a Ticket" sub="New Booking"/>

      <div className="max-w-2xl">
        {/* Mode toggle */}
        <div className="flex gap-1 p-1 bg-panel border border-border rounded mb-6 w-fit">
          {['single', 'batch'].map(m => (
            <button
              key={m}
              onClick={() => { setMode(m); setResult(null); setError(''); }}
              className={`px-4 py-1.5 rounded text-sm font-mono transition-all ${
                mode === m ? 'bg-amber text-ink font-semibold' : 'text-muted hover:text-text'
              }`}
            >
              {m === 'single' ? 'Single' : 'Batch'}
            </button>
          ))}
        </div>

        {/* Success result */}
        {result && !Array.isArray(result) && (
          <div className="card mb-6 border-green-500/40">
            <div className="card-body">
              <div className="flex items-center gap-2 text-green-400 font-semibold mb-4">
                <CheckCircle size={18}/>
                Ticket Booked — STK Push Sent
              </div>
              <div className="bg-panel rounded p-4 space-y-2.5 relative overflow-hidden">
                <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                  <span className="font-display font-black text-6xl text-amber/4 rotate-[-15deg] select-none">
                    TRANSITPASS
                  </span>
                </div>
                {[
                  ['Ticket ID',   <span className="font-mono text-amber">{result.ticketId}</span>],
                  ['Phone',       <span className="font-mono text-xs">{result.phoneNumber}</span>],
                  ['Amount',      <span className="font-mono">{fmtKES(result.pricePaid)}</span>],
                  ['Status',      <StatusBadge status={result.status}/>],
                ].map(([l, v]) => (
                  <div key={l} className="flex justify-between items-center text-sm">
                    <span className="text-muted font-mono text-xs uppercase tracking-wider">{l}</span>
                    <span>{v}</span>
                  </div>
                ))}
              </div>
              <p className="text-xs text-muted mt-3 text-center">
                Check phone for M-PESA STK push to complete payment.
              </p>
              <Button variant="ghost" onClick={() => { setResult(null); setPhone(''); }} className="w-full justify-center mt-2">
                Book Another
              </Button>
            </div>
          </div>
        )}

        {/* Batch results */}
        {result && Array.isArray(result) && (
          <div className="card mb-6 border-green-500/40">
            <div className="card-header">
              <span className="card-title">Batch Results — {result.length} tickets</span>
              <CheckCircle size={14} className="text-green-400"/>
            </div>
            <div className="overflow-x-auto">
              <table className="data-table w-full">
                <thead><tr><th>Ticket ID</th><th>Phone</th><th>Amount</th><th>Status</th></tr></thead>
                <tbody>
                  {result.map(b => (
                    <tr key={b.id}>
                      <td className="font-mono text-xs text-amber">{b.ticketId}</td>
                      <td className="font-mono text-xs text-muted">{b.phoneNumber}</td>
                      <td className="font-mono text-xs">{fmtKES(b.pricePaid)}</td>
                      <td><StatusBadge status={b.status}/></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Form */}
        {!result && (
          <div className="card">
            <div className="card-header"><span className="card-title">Booking Details</span></div>
            <div className="card-body">
              {error && <Alert type="error" message={error} onClose={() => setError('')}/>}

              <form onSubmit={mode === 'single' ? handleSingle : handleBatch} className="space-y-4">
                <Select
                  label="Select Trip"
                  value={tripId}
                  onChange={e => setTripId(e.target.value)}
                  options={tripOptions}
                  placeholder={tripsLoading ? 'Loading trips…' : 'Choose a trip…'}
                  required
                />

                {selectedTrip && (
                  <div className="flex items-center justify-between bg-panel rounded px-4 py-3 text-xs font-mono border border-border">
                    <span className="text-muted">Effective Price</span>
                    <span className="text-amber font-semibold text-sm">{fmtKES(selectedTrip.pricePerSeat)}</span>
                  </div>
                )}

                {mode === 'single' ? (
                  <Input
                    label="Passenger Phone Number"
                    type="tel"
                    value={phone}
                    onChange={e => setPhone(e.target.value)}
                    placeholder="+254712345678"
                    hint="M-PESA STK push will be sent to this number"
                    required
                  />
                ) : (
                  <div className="space-y-2">
                    <label className="label">Phone Numbers</label>
                    {phones.map((p, i) => (
                      <div key={i} className="flex gap-2">
                        <div className="relative flex-1">
                          <Phone size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted pointer-events-none"/>
                          <input
                            value={p}
                            onChange={e => setPhoneAt(i, e.target.value)}
                            placeholder={`+254712345${String(i).padStart(3,'0')}`}
                            className="input pl-8"
                            required
                          />
                        </div>
                        {phones.length > 1 && (
                          <button type="button" onClick={() => removePhone(i)}
                            className="btn btn-ghost btn-sm text-red-400 hover:bg-red-400/10 border-red-400/20">
                            <Trash2 size={13}/>
                          </button>
                        )}
                      </div>
                    ))}
                    <button type="button" onClick={addPhone}
                      className="btn btn-ghost btn-sm w-full justify-center border-dashed">
                      <PlusCircle size={13}/>Add Phone
                    </button>
                    <p className="text-[11px] text-muted">
                      Each phone number will receive an individual STK push.
                      Uses StructuredTaskScope for atomic batch processing.
                    </p>
                  </div>
                )}

                <div className="flex justify-end gap-3 pt-2">
                  <Button type="submit" size="lg" loading={loading} className="px-8">
                    {mode === 'single' ? 'Confirm & Pay' : `Book ${phones.filter(p=>p.trim()).length} Tickets`}
                  </Button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </AppShell>
  );
}

export default function BookPage() {
  return (
    <Suspense fallback={null}>
      <BookForm/>
    </Suspense>
  );
}
