import React from 'react';
import { ChevronRight, Home } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface BreadcrumbItem {
  label: string;
  href?: string;
}

export interface BreadcrumbsProps {
  items: BreadcrumbItem[];
  className?: string;
}

export const Breadcrumbs: React.FC<BreadcrumbsProps> = ({ items, className }) => {
  return (
    <nav aria-label="Đường dẫn phân cấp" className={cn('flex items-center text-xs', className)}>
      <ol className="flex items-center space-x-1.5 min-w-0">
        <li>
          <a
            href="#home"
            className="flex items-center text-slate-400 hover:text-slate-700 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 rounded p-0.5"
            aria-label="Trang chủ"
          >
            <Home className="h-3.5 w-3.5" aria-hidden="true" />
          </a>
        </li>

        {items.map((item, index) => {
          const isLast = index === items.length - 1;

          return (
            <li key={index} className="flex items-center space-x-1.5 min-w-0">
              <ChevronRight className="h-3 w-3 text-slate-400 shrink-0" aria-hidden="true" />
              {isLast ? (
                <span
                  aria-current="page"
                  className="font-semibold text-slate-900 truncate max-w-[150px] sm:max-w-xs"
                >
                  {item.label}
                </span>
              ) : item.href ? (
                <a
                  href={item.href}
                  className="text-slate-500 hover:text-slate-800 transition truncate max-w-[120px] sm:max-w-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 rounded p-0.5"
                >
                  {item.label}
                </a>
              ) : (
                <span className="text-slate-500 truncate max-w-[120px] sm:max-w-xs">
                  {item.label}
                </span>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
};
