import React, { forwardRef, useId } from 'react';
import { cn } from '@/lib/utils';
import { Spinner } from './Spinner';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  isLoading?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      id: customId,
      label,
      error,
      helperText,
      leftIcon,
      rightIcon,
      isLoading = false,
      disabled = false,
      className,
      ...props
    },
    ref
  ) => {
    const generatedId = useId();
    const inputId = customId || generatedId;
    const errorId = `${inputId}-error`;
    const helperId = `${inputId}-helper`;

    const isDisabled = disabled || isLoading;

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label
            htmlFor={inputId}
            className="block text-xs font-semibold text-slate-700 select-none"
          >
            {label}
            {props.required && <span className="text-rose-500 ml-0.5">*</span>}
          </label>
        )}

        <div className="relative rounded-lg shadow-sm">
          {leftIcon && (
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
              {leftIcon}
            </div>
          )}

          <input
            ref={ref}
            id={inputId}
            disabled={isDisabled}
            aria-invalid={!!error}
            aria-describedby={
              error ? errorId : helperText ? helperId : undefined
            }
            className={cn(
              'block w-full rounded-lg border bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 transition-colors',
              'focus:outline-none focus:ring-2 focus:ring-offset-0',
              leftIcon ? 'pl-9' : 'pl-3',
              rightIcon || isLoading ? 'pr-9' : 'pr-3',
              error
                ? 'border-rose-300 text-rose-900 focus:border-rose-500 focus:ring-rose-500/20'
                : 'border-slate-300 focus:border-emerald-500 focus:ring-emerald-500/20',
              isDisabled &&
                'bg-slate-100 text-slate-500 cursor-not-allowed border-slate-200 select-none opacity-80',
              className
            )}
            {...props}
          />

          <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
            {isLoading ? (
              <Spinner size="sm" color="muted" />
            ) : (
              rightIcon && <span className="text-slate-400">{rightIcon}</span>
            )}
          </div>
        </div>

        {error && (
          <p id={errorId} className="text-xs font-medium text-rose-600 animate-fadeIn" role="alert">
            {error}
          </p>
        )}

        {!error && helperText && (
          <p id={helperId} className="text-xs text-slate-500">
            {helperText}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
