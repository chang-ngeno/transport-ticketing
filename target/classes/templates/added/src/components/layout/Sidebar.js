'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/lib/auth';
import { cn, ROLE_LABEL } from '@/lib/utils';
import {
  LayoutDashboard, Bus, Ticket, PlusCircle, Building2,
  Users, MapPin, CarFront, TrendingUp, LogOut, X,
  Shield, Layers, ChevronRight,
} from 'lucide-react';

const NAV = [
  // ── Main ──
  { href: '/dashboard', label: 'Dashboard',     icon: LayoutDashboard },
  { href: '/trips',     label: 'Trips',          icon: Bus },
  { href: '/tickets',   label: 'Tickets',         icon: Ticket },
  { href: '/book',      label: 'Book Ticket',     icon: PlusCircle },
  // ── Admin Portal ──
  { href: '/admin/tenants',  label: 'Tenants',      icon: Building2,   roles: ['SUPER_ADMIN'],                                        section: 'admin' },
  { href: '/admin/users',    label: 'Users',         icon: Users,        roles: ['SUPER_ADMIN', 'TENANT_ADMIN'],                        section: 'admin' },
  { href: '/admin/stages',   label: 'Stages',        icon: MapPin,       roles: ['SUPER_ADMIN', 'TENANT_ADMIN'],                        section: 'admin' },
  { href: '/admin/trips',    label: 'Manage Trips',  icon: Layers,       roles: ['SUPER_ADMIN', 'TENANT_ADMIN'],                        section: 'admin' },
  { href: '/admin/fares',    label: 'Fare Windows',  icon: TrendingUp,   roles: ['SUPER_ADMIN', 'TENANT_ADMIN', 'STAGE_HEAD'],          section: 'admin' },
  { href: '/admin/vehicles', label: 'Vehicles',      icon: CarFront,     roles: ['SUPER_ADMIN', 'TENANT_ADMIN', 'STAGE_HEAD'],          section: 'admin' },
];

function NavLink({ item, onClose }) {
  const path = usePathname();
  const active = path === item.href || (item.href !== '/' && path.startsWith(item.href));
  const Icon = item.icon;
  return (
    <Link
      href={item.href}
      onClick={onClose}
      className={cn(
        'flex items-center gap-3 px-3 py-2.5 rounded text-sm font-medium transition-all duration-150 group',
        active
          ? 'bg-amber/10 text-amber border border-amber/20'
          : 'text-muted hover:text-text hover:bg-panel border border-transparent'
      )}
    >
      <Icon size={15} className={cn('flex-shrink-0', active ? 'text-amber' : 'text-muted group-hover:text-text')} />
      <span className="flex-1 truncate">{item.label}</span>
      {active && <ChevronRight size={11} className="text-amber/50" />}
    </Link>
  );
}

export default function Sidebar({ open, onClose }) {
  const { user, logout, is } = useAuth();

  const visible = NAV.filter(n => !n.roles || n.roles.some(r => is(r)));
  const mainNav  = visible.filter(n => !n.section);
  const adminNav = visible.filter(n => n.section === 'admin');

  return (
    <>
      {/* Overlay */}
      <div
        className={cn(
          'fixed inset-0 bg-black/60 backdrop-blur-sm z-40 lg:hidden transition-opacity duration-200',
          open ? 'opacity-100' : 'opacity-0 pointer-events-none'
        )}
        onClick={onClose}
      />

      {/* Sidebar */}
      <aside className={cn(
        'fixed top-0 left-0 bottom-0 w-64 bg-navy border-r border-border z-50',
        'flex flex-col transition-transform duration-[280ms] ease-out',
        'lg:translate-x-0 lg:relative lg:z-auto lg:flex-shrink-0',
        open ? 'translate-x-0' : '-translate-x-full'
      )}>
        {/* Brand */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-border flex-shrink-0">
          <span className="font-display font-black text-xl tracking-tight">
            Transit<span className="text-amber">Pass</span>
          </span>
          <button onClick={onClose} className="text-muted hover:text-text lg:hidden">
            <X size={18} />
          </button>
        </div>

        {/* User chip */}
        {user && (
          <div className="mx-4 mt-4 px-3 py-2.5 bg-panel border border-border rounded flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-full bg-amber/15 border border-amber/25 flex items-center justify-center flex-shrink-0">
              <Shield size={12} className="text-amber" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-semibold text-text truncate">{user.username || 'User'}</p>
              <p className="font-mono text-[10px] text-amber/70 uppercase tracking-wider truncate">
                {ROLE_LABEL[user.role] ?? user.role}
              </p>
            </div>
          </div>
        )}

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-0.5">
          {mainNav.map(item => <NavLink key={item.href} item={item} onClose={onClose} />)}

          {adminNav.length > 0 && (
            <>
              <div className="pt-5 pb-2 px-3">
                <span className="font-mono text-[10px] tracking-widest uppercase text-border">
                  Admin Portal
                </span>
              </div>
              {adminNav.map(item => <NavLink key={item.href} item={item} onClose={onClose} />)}
            </>
          )}
        </nav>

        {/* Sign out */}
        <div className="p-3 border-t border-border flex-shrink-0">
          <button
            onClick={logout}
            className="flex items-center gap-3 w-full px-3 py-2.5 rounded text-sm text-muted hover:text-red-400 hover:bg-red-400/8 transition-all duration-150"
          >
            <LogOut size={15} />
            Sign Out
          </button>
        </div>
      </aside>
    </>
  );
}
