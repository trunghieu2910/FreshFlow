import React, { useState } from 'react';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { Breadcrumbs, BreadcrumbItem } from './Breadcrumbs';

export interface MerchantLayoutProps {
  children: React.ReactNode;
  breadcrumbs?: BreadcrumbItem[];
  activeNavId?: string;
  onSelectNav?: (id: string) => void;
  title?: string;
  description?: string;
  actions?: React.ReactNode;
  onLogout?: () => void;
}

export const MerchantLayout: React.FC<MerchantLayoutProps> = ({
  children,
  breadcrumbs = [
    { label: 'Quản lý cửa hàng', href: '#merchant' },
    { label: 'Thực đơn & Món ăn' },
  ],
  activeNavId = 'catalog',
  onSelectNav,
  title,
  description,
  actions,
  onLogout,
}) => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col antialiased">
      {/* Skip to main content link for keyboard accessibility */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 z-50 px-4 py-2 bg-emerald-600 text-white font-medium rounded-lg shadow-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-emerald-500 transition"
      >
        Chuyển đến nội dung chính
      </a>

      {/* Main Flex Layout: Sidebar + Main Area */}
      <div className="flex flex-1 min-h-screen">
        {/* Persistent Sidebar on Desktop / Off-canvas Drawer on Tablet/Mobile */}
        <Sidebar
          isOpen={isMobileMenuOpen}
          onClose={() => setIsMobileMenuOpen(false)}
          activeId={activeNavId}
          onSelect={onSelectNav}
        />

        {/* Content Wrapper */}
        <div className="flex flex-1 flex-col min-w-0">
          {/* Header Topbar */}
          <Header
            onOpenMobileMenu={() => setIsMobileMenuOpen(true)}
            onLogout={onLogout}
          />

          {/* Sub-header with Breadcrumbs & Title bar */}
          {(breadcrumbs || title || actions) && (
            <div className="bg-white border-b border-slate-200 px-4 sm:px-6 lg:px-8 py-4">
              {breadcrumbs && <Breadcrumbs items={breadcrumbs} className="mb-2" />}

              {(title || actions) && (
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 pt-1">
                  <div className="min-w-0">
                    {title && (
                      <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900 truncate">
                        {title}
                      </h1>
                    )}
                    {description && (
                      <p className="text-xs sm:text-sm text-slate-500 mt-0.5 max-w-2xl text-balance">
                        {description}
                      </p>
                    )}
                  </div>

                  {actions && <div className="flex items-center gap-2.5 shrink-0">{actions}</div>}
                </div>
              )}
            </div>
          )}

          {/* Main Content Area */}
          <main
            id="main-content"
            tabIndex={-1}
            className="flex-1 p-4 sm:p-6 lg:p-8 outline-none"
          >
            <div className="max-w-7xl mx-auto w-full">{children}</div>
          </main>
        </div>
      </div>
    </div>
  );
};
