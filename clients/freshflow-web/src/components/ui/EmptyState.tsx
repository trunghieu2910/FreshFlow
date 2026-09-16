import React from 'react';
import { cn } from '@/lib/utils';
import { Button } from './Button';
import { UtensilsCrossed } from 'lucide-react';

export interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  className,
}) => {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center p-8 sm:p-12 text-center rounded-2xl border border-dashed border-slate-300 bg-white/80',
        className
      )}
    >
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600 mb-4 border border-emerald-100 shadow-xs">
        {icon || <UtensilsCrossed className="h-7 w-7" aria-hidden="true" />}
      </div>

      <h3 className="text-base font-semibold text-slate-900 tracking-tight mb-1">{title}</h3>

      {description && (
        <p className="text-sm text-slate-500 max-w-sm mb-6 text-balance leading-relaxed">
          {description}
        </p>
      )}

      {actionLabel && onAction && (
        <Button variant="primary" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
};
