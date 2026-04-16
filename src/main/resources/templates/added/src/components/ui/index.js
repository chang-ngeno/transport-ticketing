'use client';
import { useState, useEffect } from 'react';
import { X, AlertCircle, CheckCircle, Info, Loader2, ChevronRight } from 'lucide-react';
import { cn, STATUS_COLOR, ROLE_COLOR, ROLE_LABEL } from '@/lib/utils';

/* ── StatCard ─────────────────────────────────── */
export function StatCard({ label, value, sub, accent = 'amber', icon }) {
  return (
    <div className={cn('relative card p-5 overflow-hidden', `stat-${accent}`)}>
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="font-mono text-[10px] tracking-widest uppercase text-muted mb-2">{label}</p>
          <p className="font-display font-black text-3xl text-text leading-none">{value ?? '—'}</p>
          {sub && <p className="text-xs text-muted mt-1.5">{sub}</p>}
        </div>
        {icon && <span className="text-muted/30 flex-shrink-0">{icon}</span>}
      </div>
    </div>
  );
}

/* ── Badge ────────────────────────────────────── */
export function StatusBadge({ status }) {
  return (
    <span className={cn('badge', STATUS_COLOR[status] ?? 'text-muted bg-panel border-border')}>
      {status}
    </span>
  );
}

export function RoleBadge({ role }) {
  return (
    <span className={cn('badge', ROLE_COLOR[role] ?? 'text-muted bg-panel border-border')}>
      {ROLE_LABEL[role] ?? role}
    </span>
  );
}

/* ── Button ───────────────────────────────────── */
export function Button({ variant = 'primary', size, loading, children, className, disabled, ...props }) {
  return (
    <button
      className={cn(
        'btn',
        {
          'btn-primary': variant === 'primary',
          'btn-outline': variant === 'outline',
          'btn-ghost':   variant === 'ghost',
          'btn-danger':  variant === 'danger',
          'btn-success': variant === 'success',
          'btn-sm':      size === 'sm',
          'btn-lg':      size === 'lg',
        },
        className
      )}
      disabled={disabled || loading}
      {...props}
    >
      {loading && <Loader2 size={14} className="animate-spin" />}
      {children}
    </button>
  );
}

/* ── Input ────────────────────────────────────── */
export function Input({ label, error, hint, className, id, ...props }) {
  const _id = id ?? label?.toLowerCase().replace(/\s+/g, '-');
  return (
    <div className="space-y-1">
      {label && <label htmlFor={_id} className="label">{label}</label>}
      <input
        id={_id}
        className={cn('input', error && 'input-error', className)}
        {...props}
      />
      {hint  && <p className="text-[11px] text-muted">{hint}</p>}
      {error && <p className="text-[11px] text-red-400">{error}</p>}
    </div>
  );
}

/* ── Select ───────────────────────────────────── */
export function Select({ label, error, options = [], placeholder, className, id, ...props }) {
  const _id = id ?? label?.toLowerCase().replace(/\s+/g, '-');
  return (
    <div className="space-y-1">
      {label && <label htmlFor={_id} className="label">{label}</label>}
      <select id={_id} className={cn('input cursor-pointer', error && 'input-error', className)} {...props}>
        {placeholder && <option value="">{placeholder}</option>}
        {options.map(o => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
      {error && <p className="text-[11px] text-red-400">{error}</p>}
    </div>
  );
}

/* ── Modal ────────────────────────────────────── */
export function Modal({ open, onClose, title, children, footer, size = 'md' }) {
  useEffect(() => {
    const h = e => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', h);
    return () => window.removeEventListener('keydown', h);
  }, [onClose]);

  if (!open) return null;
  return (
    <div className="modal-backdrop" onClick={e => e.target === e.currentTarget && onClose()}>
      <div className={cn('modal', {
        'max-w-sm':  size === 'sm',
        'max-w-lg':  size === 'md',
        'max-w-2xl': size === 'lg',
        'max-w-4xl': size === 'xl',
      })}>
        <div className="modal-header">
          <h2 className="font-display font-bold text-xl">{title}</h2>
          <button onClick={onClose} className="text-muted hover:text-text transition-colors ml-4">
            <X size={18} />
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  );
}

/* ── Alert ────────────────────────────────────── */
export function Alert({ type = 'info', message, onClose }) {
  const s = {
    success: { cls: 'border-green-500 bg-green-500/8 text-green-400', icon: <CheckCircle size={15}/> },
    error:   { cls: 'border-red-500   bg-red-500/8   text-red-400',   icon: <AlertCircle size={15}/> },
    info:    { cls: 'border-amber     bg-amber/8     text-amber',      icon: <Info        size={15}/> },
  }[type];
  return (
    <div className={cn('flex items-start gap-2.5 p-3.5 rounded border-l-2 text-sm', s.cls)}>
      <span className="flex-shrink-0 mt-0.5">{s.icon}</span>
      <span className="flex-1">{message}</span>
      {onClose && (
        <button onClick={onClose} className="flex-shrink-0 opacity-60 hover:opacity-100">
          <X size={13} />
        </button>
      )}
    </div>
  );
}

/* ── Toast hook ───────────────────────────────── */
export function useToast() {
  const [toast, setToast] = useState(null);

  function show(type, message) {
    setToast({ type, message });
    setTimeout(() => setToast(null), 4000);
  }

  const ToastEl = toast ? (
    <div className={cn(
      'fixed bottom-5 right-5 z-[200] flex items-center gap-2.5 px-4 py-3 rounded border shadow-panel text-sm font-medium animate-fadeUp',
      toast.type === 'success'
        ? 'bg-slate border-green-500/40 text-green-400'
        : 'bg-slate border-red-500/40 text-red-400'
    )}>
      {toast.type === 'success' ? <CheckCircle size={15}/> : <AlertCircle size={15}/>}
      {toast.message}
    </div>
  ) : null;

  return { show, ToastEl };
}

/* ── EmptyState ───────────────────────────────── */
export function EmptyState({ message = 'No records found', action }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 gap-3 text-center">
      <div className="w-12 h-12 rounded-full bg-panel border border-border flex items-center justify-center opacity-40">
        <AlertCircle size={20} className="text-muted" />
      </div>
      <p className="text-sm text-muted">{message}</p>
      {action}
    </div>
  );
}

/* ── Skeleton rows ────────────────────────────── */
export function SkeletonRows({ rows = 5, cols = 5 }) {
  return Array.from({ length: rows }).map((_, i) => (
    <tr key={i}>
      {Array.from({ length: cols }).map((_, j) => (
        <td key={j} className="px-4 py-3.5 border-b border-border/50">
          <div className="skeleton h-4" style={{ width: `${55 + (i * j * 7) % 35}%` }} />
        </td>
      ))}
    </tr>
  ));
}

/* ── PageHeader ───────────────────────────────── */
export function PageHeader({ title, sub, actions }) {
  return (
    <div className="flex items-end justify-between flex-wrap gap-3 mb-6">
      <div>
        <p className="font-mono text-[10px] tracking-widest uppercase text-muted mb-1">{sub || 'Section'}</p>
        <h1 className="font-display font-black text-2xl lg:text-3xl tracking-tight">{title}</h1>
      </div>
      {actions && <div className="flex items-center gap-2 flex-wrap">{actions}</div>}
    </div>
  );
}

/* ── Confirm dialog ───────────────────────────── */
export function ConfirmDialog({ open, title, message, onConfirm, onCancel, loading }) {
  return (
    <Modal
      open={open}
      onClose={onCancel}
      title={title}
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onCancel} disabled={loading}>Cancel</Button>
          <Button variant="danger" onClick={onConfirm} loading={loading}>Confirm</Button>
        </>
      }
    >
      <p className="text-sm text-muted">{message}</p>
    </Modal>
  );
}

/* ── FormGrid ─────────────────────────────────── */
export function FormGrid({ cols = 2, children }) {
  return (
    <div className={cn('grid gap-4', cols === 2 ? 'grid-cols-1 sm:grid-cols-2' : 'grid-cols-1')}>
      {children}
    </div>
  );
}

/* ── SectionDivider ───────────────────────────── */
export function SectionDivider({ label }) {
  return (
    <div className="flex items-center gap-3 my-1">
      <div className="flex-1 h-px bg-border" />
      <span className="font-mono text-[10px] tracking-widest uppercase text-muted flex-shrink-0">{label}</span>
      <div className="flex-1 h-px bg-border" />
    </div>
  );
}
