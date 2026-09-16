import React from 'react';
import { Menu, Bell, ChevronDown, Store, Search, LogOut } from 'lucide-react';

export interface HeaderProps {
  onOpenMobileMenu: () => void;
  storeName?: string;
  userName?: string;
  userRole?: string;
  onLogout?: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  onOpenMobileMenu,
  storeName = 'FreshFlow Coffee & Tea (Chi nhánh Q.1)',
  userName = 'Trần Minh Anh',
  userRole = 'Chủ cửa hàng (Owner)',
  onLogout,
}) => {
  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-slate-200 bg-white/95 backdrop-blur-xs px-4 sm:px-6 lg:px-8">
      {/* Left section: Hamburger (Mobile/Tablet) + Store Selector */}
      <div className="flex items-center gap-3 min-w-0">
        <button
          type="button"
          onClick={onOpenMobileMenu}
          aria-label="Mở menu điều hướng"
          className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 hover:text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 lg:hidden"
        >
          <Menu className="h-5 w-5" aria-hidden="true" />
        </button>

        {/* Store Selector Dropdown Trigger */}
        <div className="flex items-center gap-2 rounded-xl border border-slate-200 bg-slate-50/80 px-3 py-1.5 text-xs font-semibold text-slate-800 hover:bg-slate-100 transition cursor-pointer select-none min-w-0 shadow-2xs">
          <Store className="h-4 w-4 text-emerald-600 shrink-0" aria-hidden="true" />
          <span className="truncate max-w-[200px] sm:max-w-xs">{storeName}</span>
          <ChevronDown className="h-3.5 w-3.5 text-slate-400 shrink-0" aria-hidden="true" />
        </div>
      </div>

      {/* Center / Right section */}
      <div className="flex items-center gap-3 sm:gap-4 shrink-0">
        {/* Quick Search trigger */}
        <div className="hidden md:flex items-center gap-2 rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1.5 text-xs text-slate-400 hover:border-slate-300 transition cursor-pointer select-none">
          <Search className="h-3.5 w-3.5" aria-hidden="true" />
          <span>Tìm kiếm món ăn, đơn hàng…</span>
          <kbd className="rounded border border-slate-300 bg-white px-1.5 py-0.5 text-[10px] font-mono font-medium text-slate-500 shadow-3xs">
            ⌘&nbsp;K
          </kbd>
        </div>

        {/* Notifications Button */}
        <button
          type="button"
          aria-label="Thông báo đơn hàng mới"
          className="relative rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 transition-colors"
        >
          <Bell className="h-5 w-5" aria-hidden="true" />
          <span className="absolute top-1.5 right-1.5 flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-600" />
          </span>
        </button>

        {/* User Identity Profile & Logout */}
        <div className="flex items-center gap-2.5 pl-2 border-l border-slate-200">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-600 text-white font-bold text-xs shadow-xs">
            M
          </div>
          <div className="hidden sm:block text-left min-w-0">
            <span className="block text-xs font-semibold text-slate-900 truncate">
              {userName}
            </span>
            <span className="block text-[10px] text-slate-500 truncate">
              {userRole}
            </span>
          </div>

          {onLogout && (
            <button
              type="button"
              onClick={onLogout}
              aria-label="Đăng xuất khỏi hệ thống"
              title="Đăng xuất"
              className="ml-1 rounded-lg p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500 transition-colors"
            >
              <LogOut className="h-4 w-4" aria-hidden="true" />
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
