import { format, parseISO } from 'date-fns';
import clsx from 'clsx';

export function cn(...inputs) { return clsx(inputs); }

export function fmt(date, pattern = 'dd MMM yyyy HH:mm') {
  if (!date) return '—';
  try { return format(typeof date === 'string' ? parseISO(date) : date, pattern); }
  catch { return String(date); }
}

export function fmtKES(amount) {
  if (amount == null) return '—';
  return `KES ${Number(amount).toLocaleString('en-KE', { minimumFractionDigits: 2 })}`;
}

export function fmtSeats(booked, total) {
  return `${total - booked} / ${total} free`;
}

export const STATUS_COLOR = {
  PAID:    'text-green-400  bg-green-400/10  border-green-400/30',
  PENDING: 'text-amber      bg-amber/10      border-amber/30',
  FAILED:  'text-red-400    bg-red-400/10    border-red-400/30',
};

export const ROLE_LABEL = {
  SUPER_ADMIN:     'Super Admin',
  TENANT_ADMIN:    'Tenant Admin',
  STAGE_HEAD:      'Stage Head',
  STAGE_ATTENDANT: 'Stage Attendant',
};

export const ROLE_COLOR = {
  SUPER_ADMIN:     'text-amber      bg-amber/10      border-amber/30',
  TENANT_ADMIN:    'text-sky-400    bg-sky-400/10    border-sky-400/30',
  STAGE_HEAD:      'text-purple-400 bg-purple-400/10 border-purple-400/30',
  STAGE_ATTENDANT: 'text-muted      bg-panel         border-border',
};
