import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface PaginationProps {
  currentPage: number;
  totalPages: number;
  totalElements?: number;
  pageSize?: number;
  onPageChange: (page: number) => void;
  className?: string;
  disabled?: boolean;
}

export const Pagination: React.FC<PaginationProps> = ({
  currentPage,
  totalPages,
  totalElements,
  pageSize = 20,
  onPageChange,
  className,
  disabled = false,
}) => {
  if (totalPages <= 0) {
    return null;
  }

  // Calculate start and end item index for summary text
  const startItem = totalElements !== undefined && totalElements > 0
    ? (currentPage - 1) * pageSize + 1
    : 0;
  const endItem = totalElements !== undefined
    ? Math.min(currentPage * pageSize, totalElements)
    : 0;

  // Generate pagination items with ellipses for large page numbers
  const getVisiblePages = () => {
    const pages: (number | string)[] = [];
    const maxVisible = 5;

    if (totalPages <= maxVisible + 2) {
      for (let i = 1; i <= totalPages; i++) {
        pages.push(i);
      }
    } else {
      pages.push(1);
      const start = Math.max(2, currentPage - 1);
      const end = Math.min(totalPages - 1, currentPage + 1);

      if (start > 2) {
        pages.push('…');
      }

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      if (end < totalPages - 1) {
        pages.push('…');
      }

      pages.push(totalPages);
    }

    return pages;
  };

  const visiblePages = getVisiblePages();

  return (
    <nav
      aria-label="Phân trang danh sách"
      className={cn(
        'flex flex-col sm:flex-row items-center justify-between gap-4 px-4 py-3 bg-white border border-slate-200 rounded-xl select-none',
        className
      )}
    >
      {/* Summary Info */}
      <div className="text-xs text-slate-500">
        {totalElements !== undefined ? (
          <span>
            Hiển thị{' '}
            <span className="font-semibold text-slate-800 tabular-nums">
              {startItem} - {endItem}
            </span>{' '}
            trong tổng số{' '}
            <span className="font-semibold text-slate-800 tabular-nums">
              {totalElements}
            </span>{' '}
            mục
          </span>
        ) : (
          <span>
            Trang{' '}
            <span className="font-semibold text-slate-800 tabular-nums">
              {currentPage}
            </span>{' '}
            / <span className="font-semibold text-slate-800 tabular-nums">{totalPages}</span>
          </span>
        )}
      </div>

      {/* Navigation Buttons */}
      <div className="flex items-center gap-1">
        {/* Previous button */}
        <button
          type="button"
          aria-label="Trang trước"
          disabled={disabled || currentPage <= 1}
          onClick={() => onPageChange(currentPage - 1)}
          className={cn(
            'inline-flex items-center justify-center h-8 w-8 rounded-lg text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500',
            'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-slate-600'
          )}
        >
          <ChevronLeft className="h-4 w-4" aria-hidden="true" />
        </button>

        {/* Page numbers */}
        {visiblePages.map((page, index) => {
          if (page === '…') {
            return (
              <span
                key={`ellipsis-${index}`}
                className="h-8 w-8 inline-flex items-center justify-center text-xs text-slate-400 font-medium"
              >
                …
              </span>
            );
          }

          const pageNumber = page as number;
          const isActive = pageNumber === currentPage;

          return (
            <button
              key={pageNumber}
              type="button"
              aria-label={`Trang ${pageNumber}`}
              aria-current={isActive ? 'page' : undefined}
              disabled={disabled}
              onClick={() => onPageChange(pageNumber)}
              className={cn(
                'inline-flex items-center justify-center h-8 min-w-8 px-2 rounded-lg text-xs font-semibold tabular-nums transition-colors',
                'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500',
                isActive
                  ? 'bg-emerald-600 text-white shadow-xs pointer-events-none'
                  : 'text-slate-700 hover:bg-slate-100 hover:text-slate-900 border border-transparent'
              )}
            >
              {pageNumber}
            </button>
          );
        })}

        {/* Next button */}
        <button
          type="button"
          aria-label="Trang sau"
          disabled={disabled || currentPage >= totalPages}
          onClick={() => onPageChange(currentPage + 1)}
          className={cn(
            'inline-flex items-center justify-center h-8 w-8 rounded-lg text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500',
            'disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-slate-600'
          )}
        >
          <ChevronRight className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>
    </nav>
  );
};
