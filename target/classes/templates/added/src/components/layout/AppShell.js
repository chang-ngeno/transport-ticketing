'use client';
import { useState } from 'react';
import Sidebar from './Sidebar';
import { Menu, Bell, Wifi, WifiOff, Download } from 'lucide-react';
import { useAuth } from '@/lib/auth';
import { cn, ROLE_LABEL } from '@/lib/utils';

export default function AppShell({ children, title, actions }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { user } = useAuth();

  return (
    <div className="flex h-screen overflow-hidden bg-navy">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        {/* Topbar */}
        <header className="flex items-center gap-3 px-5 py-3 border-b border-border bg-navy/95 backdrop-blur flex-shrink-0 z-30">
          <button
            onClick={() => setSidebarOpen(true)}
            className="text-muted hover:text-text transition-colors lg:hidden p-1"
          >
            <Menu size={20} />
          </button>

          {title && (
            <h1 className="font-display font-bold text-lg tracking-tight truncate">{title}</h1>
          )}

          <div className="ml-auto flex items-center gap-2.5">
            {actions}
            <button className="relative text-muted hover:text-amber transition-colors p-1">
              <Bell size={17} />
              <span className="absolute top-1 right-1 w-1.5 h-1.5 rounded-full bg-amber" />
            </button>
            {user && (
              <div className="hidden sm:flex px-2.5 py-1 bg-panel border border-border rounded-full items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-amber inline-block animate-pulse" />
                <span className="font-mono text-[10px] uppercase tracking-widest text-muted">
                  {ROLE_LABEL[user.role] ?? user.role}
                </span>
              </div>
            )}
          </div>
        </header>

        {/* Page scroll area */}
        <main className="flex-1 overflow-y-auto bg-grid">
          <div className="p-5 lg:p-7 animate-fadeUp">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
