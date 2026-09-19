import React from 'react';
import { cn } from '@/lib/utils';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'outline';
  size?: 'sm' | 'md';
  dot?: boolean;
}

const variantClasses = {
  default: 'bg-slate-100 text-slate-800 border-slate-200',
  success: 'bg-emerald-50 text-emerald-800 border-emerald-300',
  warning: 'bg-amber-50 text-amber-800 border-amber-300',
  danger: 'bg-rose-50 text-rose-800 border-rose-300',
  info: 'bg-blue-50 text-blue-800 border-blue-300',
  outline: 'bg-transparent text-slate-700 border-slate-300',
};

const dotClasses = {
  default: 'bg-slate-500',
  success: 'bg-emerald-500',
  warning: 'bg-amber-500',
  danger: 'bg-rose-500',
  info: 'bg-blue-500',
  outline: 'bg-slate-500',
};

const sizeClasses = {
  sm: 'text-[11px] px-2 py-0.5 gap-1 font-medium',
  md: 'text-xs px-2.5 py-1 gap-1.5 font-medium',
};

export const Badge: React.FC<BadgeProps> = ({
  children,
  className,
  variant = 'default',
  size = 'md',
  dot = false,
  ...props
}) => {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border transition-colors select-none',
        variantClasses[variant],
        sizeClasses[size],
        className
      )}
      {...props}
    >
      {dot && (
        <span
          className={cn('h-1.5 w-1.5 rounded-full shrink-0 animate-pulse', dotClasses[variant])}
          aria-hidden="true"
        />
      )}
      {children}
    </span>
  );
};
