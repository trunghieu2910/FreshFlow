import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  useRef,
  useEffect,
} from 'react';
import { cn } from '@/lib/utils';
import {
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  Info,
  X,
} from 'lucide-react';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastAction {
  label: string;
  onClick: () => void;
}

export interface ToastItem {
  id: string;
  type: ToastType;
  title?: string;
  message: string;
  duration?: number; // Duration in ms; 0 or Infinity for persistent
  action?: ToastAction;
}

export interface ToastOptions {
  title?: string;
  duration?: number;
  action?: ToastAction;
}

export interface ToastContextValue {
  toasts: ToastItem[];
  showToast: (item: Omit<ToastItem, 'id'>) => string;
  dismissToast: (id: string) => void;
  success: (message: string, options?: ToastOptions) => string;
  error: (message: string, options?: ToastOptions) => string;
  warning: (message: string, options?: ToastOptions) => string;
  info: (message: string, options?: ToastOptions) => string;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const DEFAULT_DURATIONS: Record<ToastType, number> = {
  success: 4000,
  info: 4000,
  warning: 5000,
  error: 6000,
};

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (item: Omit<ToastItem, 'id'>): string => {
      const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
      const duration = item.duration !== undefined ? item.duration : DEFAULT_DURATIONS[item.type];
      const newToast: ToastItem = { ...item, id, duration };

      setToasts((prev) => [...prev, newToast]);
      return id;
    },
    []
  );

  const success = useCallback(
    (message: string, options?: ToastOptions) =>
      showToast({ type: 'success', message, ...options }),
    [showToast]
  );

  const error = useCallback(
    (message: string, options?: ToastOptions) =>
      showToast({ type: 'error', message, ...options }),
    [showToast]
  );

  const warning = useCallback(
    (message: string, options?: ToastOptions) =>
      showToast({ type: 'warning', message, ...options }),
    [showToast]
  );

  const info = useCallback(
    (message: string, options?: ToastOptions) =>
      showToast({ type: 'info', message, ...options }),
    [showToast]
  );

  const contextValue: ToastContextValue = {
    toasts,
    showToast,
    dismissToast,
    success,
    error,
    warning,
    info,
  };

  return (
    <ToastContext.Provider value={contextValue}>
      {children}
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
    </ToastContext.Provider>
  );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useToast = (): ToastContextValue => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
};

// Internal Toast Container
interface ToastContainerProps {
  toasts: ToastItem[];
  onDismiss: (id: string) => void;
}

const ToastContainer: React.FC<ToastContainerProps> = ({ toasts, onDismiss }) => {
  if (toasts.length === 0) return null;

  return (
    <div
      aria-live="polite"
      aria-relevant="additions text"
      className="fixed top-4 right-4 z-50 flex flex-col gap-2.5 max-w-sm w-full pointer-events-none px-4 sm:px-0"
    >
      {toasts.map((toast) => (
        <ToastCard key={toast.id} toast={toast} onDismiss={onDismiss} />
      ))}
    </div>
  );
};

// Individual Toast Card
interface ToastCardProps {
  toast: ToastItem;
  onDismiss: (id: string) => void;
}

const ToastCard: React.FC<ToastCardProps> = ({ toast, onDismiss }) => {
  const [isHovered, setIsHovered] = useState(false);
  const remainingTimeRef = useRef<number>(toast.duration || 4000);
  const startTimeRef = useRef<number>(Date.now());
  const timerRef = useRef<number | null>(null);

  const isPersistent = toast.duration === 0 || toast.duration === Infinity;

  useEffect(() => {
    if (isPersistent || isHovered) {
      if (timerRef.current) {
        window.clearTimeout(timerRef.current);
        timerRef.current = null;
      }
      return;
    }

    startTimeRef.current = Date.now();
    timerRef.current = window.setTimeout(() => {
      onDismiss(toast.id);
    }, remainingTimeRef.current);

    return () => {
      if (timerRef.current) {
        window.clearTimeout(timerRef.current);
      }
      const elapsed = Date.now() - startTimeRef.current;
      remainingTimeRef.current = Math.max(0, remainingTimeRef.current - elapsed);
    };
  }, [isHovered, isPersistent, onDismiss, toast.id]);

  const typeConfig = {
    success: {
      icon: CheckCircle2,
      containerClass: 'bg-white border-emerald-200 text-slate-800 shadow-emerald-500/10',
      iconClass: 'text-emerald-600 bg-emerald-50 border-emerald-200',
      badgeClass: 'bg-emerald-100 text-emerald-800',
      defaultTitle: 'Thành công',
    },
    error: {
      icon: AlertCircle,
      containerClass: 'bg-white border-rose-200 text-slate-800 shadow-rose-500/10',
      iconClass: 'text-rose-600 bg-rose-50 border-rose-200',
      badgeClass: 'bg-rose-100 text-rose-800',
      defaultTitle: 'Lỗi',
    },
    warning: {
      icon: AlertTriangle,
      containerClass: 'bg-white border-amber-200 text-slate-800 shadow-amber-500/10',
      iconClass: 'text-amber-600 bg-amber-50 border-amber-200',
      badgeClass: 'bg-amber-100 text-amber-800',
      defaultTitle: 'Cảnh báo',
    },
    info: {
      icon: Info,
      containerClass: 'bg-white border-sky-200 text-slate-800 shadow-sky-500/10',
      iconClass: 'text-sky-600 bg-sky-50 border-sky-200',
      badgeClass: 'bg-sky-100 text-sky-800',
      defaultTitle: 'Thông tin',
    },
  }[toast.type];

  const Icon = typeConfig.icon;

  return (
    <div
      role={toast.type === 'error' ? 'alert' : 'status'}
      aria-atomic="true"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      className={cn(
        'pointer-events-auto flex items-start gap-3 p-4 rounded-2xl border shadow-lg transition-all duration-300 transform translate-y-0 opacity-100 animate-slideInRight',
        typeConfig.containerClass
      )}
    >
      <div
        className={cn(
          'flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border',
          typeConfig.iconClass
        )}
      >
        <Icon className="h-5 w-5" aria-hidden="true" />
      </div>

      <div className="flex-1 min-w-0 pt-0.5">
        <h4 className="text-xs font-bold text-slate-900 tracking-tight">
          {toast.title || typeConfig.defaultTitle}
        </h4>
        <p className="text-xs text-slate-600 mt-0.5 leading-relaxed break-words">
          {toast.message}
        </p>

        {toast.action && (
          <button
            type="button"
            onClick={() => {
              toast.action?.onClick();
              onDismiss(toast.id);
            }}
            className="mt-2 inline-flex items-center text-xs font-semibold text-emerald-600 hover:text-emerald-700 underline focus:outline-none"
          >
            {toast.action.label}
          </button>
        )}
      </div>

      <button
        type="button"
        onClick={() => onDismiss(toast.id)}
        className="shrink-0 rounded-lg p-1 text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition focus:outline-none focus:ring-2 focus:ring-slate-400"
        aria-label="Đóng thông báo"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
};
