import React from 'react';
import { cn } from '@/lib/utils';
import { Button } from './Button';
import { AlertTriangle, RefreshCw } from 'lucide-react';

export interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
  isRetrying?: boolean;
  className?: string;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Không thể tải dữ liệu',
  message = 'Đã có lỗi xảy ra trong quá trình kết nối với máy chủ. Vui lòng kiểm tra lại đường truyền mạng hoặc thử lại sau.',
  onRetry,
  isRetrying = false,
  className,
}) => {
  return (
    <div
      role="alert"
      aria-live="polite"
      className={cn(
        'flex flex-col items-center justify-center p-8 sm:p-12 text-center rounded-2xl border border-rose-200 bg-rose-50/50',
        className
      )}
    >
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100 text-rose-600 mb-4 border border-rose-200 shadow-xs">
        <AlertTriangle className="h-7 w-7" aria-hidden="true" />
      </div>

      <h3 className="text-base font-semibold text-rose-900 tracking-tight mb-1">{title}</h3>

      <p className="text-sm text-rose-700/90 max-w-md mb-6 text-balance leading-relaxed">
        {message}
      </p>

      {onRetry && (
        <Button
          variant="danger"
          isLoading={isRetrying}
          leftIcon={<RefreshCw className="h-4 w-4" aria-hidden="true" />}
          onClick={onRetry}
        >
          Thử lại ngay
        </Button>
      )}
    </div>
  );
};
