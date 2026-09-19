import React from 'react';
import { cn } from '@/lib/utils';
import { Button } from './Button';
import { UtensilsCrossed, SearchX, ShoppingBag } from 'lucide-react';

export type EmptyStateVariant = 'default' | 'search' | 'cart';

export interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  secondaryActionLabel?: string;
  onSecondaryAction?: () => void;
  variant?: EmptyStateVariant;
  className?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  secondaryActionLabel,
  onSecondaryAction,
  variant = 'default',
  className,
}) => {
  const defaultIcons = {
    default: <UtensilsCrossed className="h-7 w-7" aria-hidden="true" />,
    search: <SearchX className="h-7 w-7" aria-hidden="true" />,
    cart: <ShoppingBag className="h-7 w-7" aria-hidden="true" />,
  };

  const variantColors = {
    default: 'bg-emerald-50 text-emerald-600 border-emerald-100',
    search: 'bg-amber-50 text-amber-600 border-amber-100',
    cart: 'bg-sky-50 text-sky-600 border-sky-100',
  };

  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        'flex flex-col items-center justify-center p-8 sm:p-12 text-center rounded-2xl border border-dashed border-slate-300 bg-white/80',
        className
      )}
    >
      <div
        className={cn(
          'flex h-14 w-14 items-center justify-center rounded-2xl mb-4 border shadow-xs transition-transform hover:scale-105',
          variantColors[variant]
        )}
      >
        {icon || defaultIcons[variant]}
      </div>

      <h3 className="text-base font-semibold text-slate-900 tracking-tight mb-1">{title}</h3>

      {description && (
        <p className="text-sm text-slate-500 max-w-sm mb-6 text-balance leading-relaxed">
          {description}
        </p>
      )}

      {(actionLabel || secondaryActionLabel) && (
        <div className="flex items-center gap-3 flex-wrap justify-center">
          {actionLabel && onAction && (
            <Button variant="primary" size="sm" onClick={onAction}>
              {actionLabel}
            </Button>
          )}

          {secondaryActionLabel && onSecondaryAction && (
            <Button variant="outline" size="sm" onClick={onSecondaryAction}>
              {secondaryActionLabel}
            </Button>
          )}
        </div>
      )}
    </div>
  );
};
