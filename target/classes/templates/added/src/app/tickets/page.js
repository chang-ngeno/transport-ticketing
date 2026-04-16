'use client';
import { useEffect, useState } from 'react';
import { useRequireAuth } from '@/lib/auth';
import AppShell from '@/components/layout/AppShell';
import { PageHeader, Button, StatusBadge, SkeletonRows, EmptyState, Input, Select } from '@/components/ui';
import { ticketApi } from '@/lib/api';
import { fmt, fmtKES } from '@/lib/utils';
import Link from 'next/link';
import { PlusCircle, Search, X } from 'lucide-react';

export default function TicketsPage() {
  useRequireAuth();
  const [all,      setAll]      = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [search,   setSearch]   = useState('');
  const [status,   setStatus]   = useState('');

  useEffect(() => {
    ticketApi.list()
      .then(setAll)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const filtered = all.filter(b => {
    const matchSearch = !search ||
      b.ticketId?.toLowerCase().includes(search.toLowerCase()) ||
      b.phoneNumber?.includes(search);
    const matchStatus = !status || b.status === status;
    return matchSearch && matchStatus;
  });

  const paid    = all.filter(b => b.status === 'PAID').length;
  const pending = all.filter(b => b.status === 'PENDING').length;
  const failed  = all.filter(b => b.status === 'FAILED').length;

  return (
    <AppShell title="Tickets">
      <PageHeader
        title="Tickets"
        sub="Bookings"
        actions={<Link href="/book"><Button size="sm"><PlusCircle size={14}/>New Booking</Button></Link>}
      />

      {/* Quick filters */}
      <div className="flex gap-2 mb-5 flex-wrap">
        {[
          { label: `All (${all.length})`,     value: '' },
          { label: `Paid (${paid})`,           value: 'PAID' },
          { label: `Pending (${pending})`,     value: 'PENDING' },
          { label: `Failed (${failed})`,       value: 'FAILED' },
        ].map(f => (
          <button
            key={f.value}
            onClick={() => setStatus(f.value)}
            className={`btn btn-sm ${status === f.value ? 'btn-primary' : 'btn-ghost'}`}
          >
            {f.label}
          </button>
        ))}
        <div className="ml-auto relative">
          <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted pointer-events-none"/>
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search ticket ID or phone…"
            className="input pl-8 pr-8 py-1.5 text-xs w-60"
          />
          {search && (
            <button onClick={() => setSearch('')} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-muted hover:text-text">
              <X size={12}/>
            </button>
          )}
        </div>
      </div>

      <div className="card">
        <div className="overflow-x-auto">
          <table className="data-table w-full">
            <thead>
              <tr>
                <th>Ticket ID</th>
                <th>Phone</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Booked At</th>
              </tr>
            </thead>
            <tbody>
              {loading ? <SkeletonRows rows={8} cols={5}/> :
               filtered.length === 0 ? (
                <tr><td colSpan={5}><EmptyState message="No tickets match your filters"/></td></tr>
              ) : filtered.map(b => (
                <tr key={b.id}>
                  <td><span className="font-mono text-xs text-amber">{b.ticketId}</span></td>
                  <td className="font-mono text-xs text-muted">{b.phoneNumber}</td>
                  <td className="font-mono text-xs">{fmtKES(b.pricePaid)}</td>
                  <td><StatusBadge status={b.status}/></td>
                  <td className="font-mono text-xs text-muted">{fmt(b.createdAt, 'dd MMM HH:mm')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!loading && filtered.length > 0 && (
          <div className="px-5 py-3 border-t border-border font-mono text-[10px] text-muted">
            Showing {filtered.length} of {all.length} records
          </div>
        )}
      </div>
    </AppShell>
  );
}
