import React from 'react';
import { cn } from '@/lib/utils';

export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  className?: string;
}

export const Skeleton: React.FC<SkeletonProps> = ({ className, ...props }) => {
  return (
    <div
      aria-hidden="true"
      className={cn(
        'animate-pulse rounded-md bg-slate-200/80 dark:bg-slate-700/60',
        className
      )}
      {...props}
    />
  );
};

export const TableSkeleton: React.FC<{ rows?: number }> = ({ rows = 4 }) => {
  return (
    <div
      role="status"
      aria-label="Đang tải danh sách dữ liệu…"
      className="w-full rounded-xl border border-slate-200 bg-white shadow-xs overflow-hidden"
    >
      {/* Table Header Skeleton */}
      <div className="h-11 bg-slate-50 border-b border-slate-200 px-4 flex items-center justify-between gap-4">
        <Skeleton className="h-4 w-12" />
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-4 w-24 hidden sm:block" />
        <Skeleton className="h-4 w-28 hidden md:block" />
        <Skeleton className="h-4 w-20" />
        <Skeleton className="h-4 w-16" />
      </div>

      {/* Table Rows Skeleton */}
      <div className="divide-y divide-slate-100">
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="p-4 flex items-center justify-between gap-4">
            <Skeleton className="h-4 w-10" />
            <div className="space-y-1.5 flex-1 min-w-0">
              <Skeleton className="h-4 w-3/4 max-w-xs" />
              <Skeleton className="h-3 w-1/2 max-w-[160px]" />
            </div>
            <Skeleton className="h-6 w-20 rounded-full hidden sm:block" />
            <Skeleton className="h-4 w-24 hidden md:block" />
            <Skeleton className="h-6 w-24 rounded-full" />
            <div className="flex gap-2 shrink-0">
              <Skeleton className="h-8 w-12 rounded-lg" />
              <Skeleton className="h-8 w-12 rounded-lg" />
            </div>
          </div>
        ))}
      </div>
      <span className="sr-only">Đang tải dữ liệu thực đơn…</span>
    </div>
  );
};
