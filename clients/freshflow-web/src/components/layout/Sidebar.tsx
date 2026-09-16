import React, { useEffect } from 'react';
import { cn } from '@/lib/utils';
import {
  LayoutDashboard,
  UtensilsCrossed,
  ShoppingBag,
  BarChart3,
  Settings,
  X,
  Store,
  ChevronRight,
} from 'lucide-react';

export interface NavItem {
  id: string;
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string; 'aria-hidden'?: boolean | 'true' | 'false' }>;
  badge?: string | number;
}

const navItems: NavItem[] = [
  {
    id: 'dashboard',
    label: 'Tổng quan vận hành',
    href: '#dashboard',
    icon: LayoutDashboard,
  },
  {
    id: 'catalog',
    label: 'Thực đơn số & Món ăn',
    href: '#catalog',
    icon: UtensilsCrossed,
  },
  {
    id: 'orders',
    label: 'Quản lý đơn hàng',
    href: '#orders',
    icon: ShoppingBag,
    badge: 3,
  },
  {
    id: 'analytics',
    label: 'Báo cáo doanh thu',
    href: '#analytics',
    icon: BarChart3,
  },
  {
    id: 'settings',
    label: 'Cài đặt cửa hàng',
    href: '#settings',
    icon: Settings,
  },
];

export interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  activeId?: string;
  onSelect?: (id: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  isOpen,
  onClose,
  activeId = 'catalog',
  onSelect,
}) => {
  // Close drawer on Escape key press (Tablet/Mobile)
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  return (
    <>
      {/* Backdrop for Tablet/Mobile Drawer */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-slate-900/50 backdrop-blur-xs lg:hidden transition-opacity motion-reduce:transition-none animate-fadeIn"
          aria-hidden="true"
          onClick={onClose}
        />
      )}

      {/* Sidebar Container */}
      <aside
        id="sidebar-navigation"
        aria-label="Thanh điều hướng chính"
        className={cn(
          'fixed inset-y-0 left-0 z-50 flex w-64 flex-col bg-white border-r border-slate-200 shadow-sm transition-transform duration-200 ease-in-out motion-reduce:transition-none',
          'lg:translate-x-0 lg:static lg:z-auto',
          isOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {/* Brand Header */}
        <div className="flex h-16 items-center justify-between px-5 border-b border-slate-100">
          <div className="flex items-center gap-3 min-w-0">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-emerald-600 text-white shadow-sm">
              <Store className="h-5 w-5" aria-hidden="true" />
            </div>
            <div className="min-w-0">
              <span className="block font-bold text-sm text-slate-900 truncate">
                FreshFlow Portal
              </span>
              <span className="block text-xs text-emerald-700 font-medium truncate">
                Merchant Admin
              </span>
            </div>
          </div>

          {/* Close button for Tablet/Mobile */}
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng menu điều hướng"
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 lg:hidden"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        {/* Navigation Links */}
        <nav aria-label="Menu chức năng" className="flex-1 overflow-y-auto p-4 space-y-1.5">
          <div className="px-3 pb-2 text-[11px] font-bold uppercase tracking-wider text-slate-400 select-none">
            Chức năng chính
          </div>

          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeId === item.id;

            return (
              <a
                key={item.id}
                href={item.href}
                onClick={(e) => {
                  e.preventDefault();
                  if (onSelect) onSelect(item.id);
                  if (window.innerWidth < 1024) onClose();
                }}
                aria-current={isActive ? 'page' : undefined}
                className={cn(
                  'group flex items-center justify-between rounded-xl px-3.5 py-2.5 text-xs font-semibold transition-colors duration-150',
                  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500',
                  isActive
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200/80 shadow-xs'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900 border border-transparent'
                )}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <Icon
                    aria-hidden="true"
                    className={cn(
                      'h-4 w-4 shrink-0 transition-colors',
                      isActive
                        ? 'text-emerald-600'
                        : 'text-slate-400 group-hover:text-slate-600'
                    )}
                  />
                  <span className="truncate">{item.label}</span>
                </div>

                {item.badge !== undefined && (
                  <span
                    className={cn(
                      'rounded-full px-2 py-0.5 text-[10px] font-bold tabular-nums shrink-0',
                      isActive
                        ? 'bg-emerald-600 text-white'
                        : 'bg-slate-100 text-slate-600 group-hover:bg-slate-200'
                    )}
                  >
                    {item.badge}
                  </span>
                )}
              </a>
            );
          })}
        </nav>

        {/* Store Branch Footer Card */}
        <div className="p-4 border-t border-slate-100 bg-slate-50/50">
          <div className="flex items-center justify-between p-2.5 rounded-xl bg-white border border-slate-200 shadow-xs">
            <div className="min-w-0 pr-2">
              <span className="block text-[11px] font-bold text-slate-800 truncate">
                CN Nguyễn Huệ (Q.1)
              </span>
              <span className="block text-[10px] text-slate-500 truncate">
                Mở cửa: 07:00 – 22:00
              </span>
            </div>
            <ChevronRight className="h-4 w-4 text-slate-400 shrink-0" aria-hidden="true" />
          </div>
        </div>
      </aside>
    </>
  );
};
