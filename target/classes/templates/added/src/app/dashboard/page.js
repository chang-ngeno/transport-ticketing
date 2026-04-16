'use client';
import { useEffect, useState } from 'react';
import { useRequireAuth } from '@/lib/auth';
import AppShell from '@/components/layout/AppShell';
import { StatCard, PageHeader, StatusBadge, Button, SkeletonRows, EmptyState } from '@/components/ui';
import { ticketApi, tripApi } from '@/lib/api';
import { fmtKES, fmt, fmtSeats } from '@/lib/utils';
import Link from 'next/link';
import {
  AreaChart, Area, BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, Cell,
} from 'recharts';
import { Ticket, Bus, DollarSign, Clock, PlusCircle, ArrowRight, TrendingUp } from 'lucide-react';

const CHART_COLORS = { PAID: '#22c55e', PENDING: '#f0a500', FAILED: '#e03b3b' };

export default function DashboardPage() {
  const { user } = useRequireAuth();
  const [bookings, setBookings] = useState([]);
  const [trips,    setTrips]    = useState([]);
  const [loading,  setLoading]  = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const [b, t] = await Promise.all([ticketApi.list(), tripApi.list()]);
        setBookings(b);
        setTrips(t);
      } catch {}
      finally { setLoading(false); }
    }
    load();
  }, []);

  // Stats
  const paid    = bookings.filter(b => b.status === 'PAID').length;
  const pending = bookings.filter(b => b.status === 'PENDING').length;
  const failed  = bookings.filter(b => b.status === 'FAILED').length;
  const revenue = bookings.filter(b => b.status === 'PAID').reduce((s, b) => s + Number(b.pricePaid), 0);

  // Chart data — bookings by status
  const statusData = [
    { name: 'Paid',    value: paid,    fill: '#22c55e' },
    { name: 'Pending', value: pending, fill: '#f0a500' },
    { name: 'Failed',  value: failed,  fill: '#e03b3b' },
  ];

  // Revenue chart — last 7 bookings
  const recentRevenue = bookings
    .filter(b => b.status === 'PAID')
    .slice(-10)
    .map((b, i) => ({ i: i + 1, amount: Number(b.pricePaid) }));

  const recentBookings = [...bookings].reverse().slice(0, 6);
  const upcomingTrips  = trips.slice(0, 5);

  return (
    <AppShell title="Dashboard" actions={
      <Link href="/book"><Button size="sm"><PlusCircle size={14}/>Book Ticket</Button></Link>
    }>
      <PageHeader title={`Good day${user?.username ? ', ' + user.username : ''}`} sub="Overview" />

      {/* Stats row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <StatCard label="Total Bookings" value={bookings.length} sub="All time" accent="amber" icon={<Ticket size={28}/>} />
        <StatCard label="Revenue"        value={loading ? '…' : `KES ${(revenue/1000).toFixed(1)}k`} sub="From paid tickets" accent="green" icon={<DollarSign size={28}/>} />
        <StatCard label="Active Trips"   value={trips.length}    sub="Scheduled" accent="sky"   icon={<Bus size={28}/>} />
        <StatCard label="Pending"        value={pending}         sub="Awaiting payment" accent="muted" icon={<Clock size={28}/>} />
      </div>

      <div className="grid lg:grid-cols-3 gap-5 mb-5">
        {/* Revenue trend */}
        <div className="lg:col-span-2 card">
          <div className="card-header">
            <span className="card-title">Revenue Trend</span>
            <TrendingUp size={14} className="text-muted" />
          </div>
          <div className="card-body">
            {recentRevenue.length > 1 ? (
              <ResponsiveContainer width="100%" height={180}>
                <AreaChart data={recentRevenue} margin={{ top: 5, right: 5, bottom: 0, left: -20 }}>
                  <defs>
                    <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%"  stopColor="#f0a500" stopOpacity={0.25} />
                      <stop offset="95%" stopColor="#f0a500" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#1e2d47" />
                  <XAxis dataKey="i" tick={{ fill: '#8895aa', fontSize: 10 }} />
                  <YAxis tick={{ fill: '#8895aa', fontSize: 10 }} />
                  <Tooltip
                    contentStyle={{ background: '#111827', border: '1px solid #1e2d47', borderRadius: 4, fontSize: 12 }}
                    labelStyle={{ color: '#8895aa' }}
                    formatter={v => [`KES ${v}`, 'Amount']}
                  />
                  <Area type="monotone" dataKey="amount" stroke="#f0a500" strokeWidth={2} fill="url(#rev)" />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <EmptyState message="Not enough data for chart" />
            )}
          </div>
        </div>

        {/* Status breakdown */}
        <div className="card">
          <div className="card-header"><span className="card-title">Booking Status</span></div>
          <div className="card-body">
            <ResponsiveContainer width="100%" height={180}>
              <BarChart data={statusData} margin={{ top: 5, right: 0, bottom: 0, left: -25 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e2d47" />
                <XAxis dataKey="name" tick={{ fill: '#8895aa', fontSize: 10 }} />
                <YAxis tick={{ fill: '#8895aa', fontSize: 10 }} />
                <Tooltip
                  contentStyle={{ background: '#111827', border: '1px solid #1e2d47', borderRadius: 4, fontSize: 12 }}
                  cursor={{ fill: 'rgba(240,165,0,0.06)' }}
                />
                <Bar dataKey="value" radius={[2, 2, 0, 0]}>
                  {statusData.map((d, i) => <Cell key={i} fill={d.fill} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="grid lg:grid-cols-2 gap-5">
        {/* Recent bookings */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">Recent Bookings</span>
            <Link href="/tickets" className="btn btn-ghost btn-sm">View all <ArrowRight size={12}/></Link>
          </div>
          <div className="overflow-x-auto">
            <table className="data-table w-full">
              <thead>
                <tr><th>Ticket</th><th>Phone</th><th>Amount</th><th>Status</th></tr>
              </thead>
              <tbody>
                {loading ? <SkeletonRows rows={4} cols={4}/> :
                  recentBookings.length === 0 ? (
                    <tr><td colSpan={4}><EmptyState message="No bookings yet"/></td></tr>
                  ) :
                  recentBookings.map(b => (
                    <tr key={b.id}>
                      <td><span className="font-mono text-amber text-xs">{b.ticketId}</span></td>
                      <td className="font-mono text-xs text-muted">{b.phoneNumber}</td>
                      <td className="font-mono text-xs">{fmtKES(b.pricePaid)}</td>
                      <td><StatusBadge status={b.status}/></td>
                    </tr>
                  ))
                }
              </tbody>
            </table>
          </div>
        </div>

        {/* Upcoming trips */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">Upcoming Trips</span>
            <Link href="/trips" className="btn btn-ghost btn-sm">View all <ArrowRight size={12}/></Link>
          </div>
          <div className="divide-y divide-border">
            {loading ? (
              <div className="p-5 space-y-3">
                {[1,2,3].map(i => <div key={i} className="skeleton h-12 rounded"/>)}
              </div>
            ) : upcomingTrips.length === 0 ? (
              <EmptyState message="No trips scheduled"/>
            ) : upcomingTrips.map(t => (
              <div key={t.id} className="flex items-center gap-3 px-5 py-3.5 hover:bg-panel/50 transition-colors">
                <div className="flex-shrink-0 text-center min-w-[52px]">
                  <p className="font-mono text-amber font-semibold text-sm leading-none">
                    {fmt(t.departureTime, 'HH:mm')}
                  </p>
                  <p className="font-mono text-[10px] text-muted mt-0.5">
                    {fmt(t.departureTime, 'dd MMM')}
                  </p>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium truncate">{t.toDestination}</p>
                  <p className="text-xs text-muted truncate">{t.route || 'Direct route'}</p>
                </div>
                <div className="text-right flex-shrink-0">
                  <p className="font-display font-bold text-sm">{fmtKES(t.pricePerSeat)}</p>
                  <p className="font-mono text-[10px] text-muted">{fmtSeats(t.bookedSeats, t.totalSeats)}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </AppShell>
  );
}
