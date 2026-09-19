import { Component, ErrorInfo, ReactNode } from 'react';
import { cn } from '@/lib/utils';
import { Button } from './Button';
import { AlertTriangle, RefreshCw, RotateCcw, ChevronDown, ChevronUp } from 'lucide-react';

export interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode | ((props: { error: Error; resetErrorBoundary: () => void }) => ReactNode);
  title?: string;
  description?: string;
  onReset?: () => void;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
  className?: string;
}

export interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
  showDetails: boolean;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      showDetails: false,
    };
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ errorInfo });
    this.props.onError?.(error, errorInfo);
    console.error('[ErrorBoundary caught error]:', error, errorInfo);
  }

  resetErrorBoundary = () => {
    const nextState = {
      hasError: false,
      error: null,
      errorInfo: null,
      showDetails: false,
    };
    this.state = nextState;
    try {
      this.setState(nextState);
    } catch {
      // no-op if called outside React mount lifecycle
    }
    this.props.onReset?.();
  };

  toggleDetails = () => {
    this.setState((prev) => ({ showDetails: !prev.showDetails }));
  };

  render() {
    const { hasError, error, errorInfo, showDetails } = this.state;
    const {
      children,
      fallback,
      title = 'Đã có lỗi xảy ra khi hiển thị nội dung',
      description = 'Rất tiếc, một sự cố không mong muốn đã xảy ra trong quá trình kết xuất giao diện. Hệ thống đã ngăn chặn hiện tượng trắng trang để bảo vệ dữ liệu của bạn.',
      className,
    } = this.props;

    if (hasError && error) {
      if (typeof fallback === 'function') {
        return fallback({ error, resetErrorBoundary: this.resetErrorBoundary });
      }

      if (fallback) {
        return fallback;
      }

      return (
        <div
          role="alert"
          aria-live="assertive"
          className={cn(
            'flex flex-col items-center justify-center p-6 sm:p-10 text-center rounded-2xl border border-rose-200 bg-rose-50/40 my-4 max-w-2xl mx-auto shadow-sm',
            className
          )}
        >
          {/* Error Icon */}
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100 text-rose-600 mb-4 border border-rose-200 shadow-xs">
            <AlertTriangle className="h-7 w-7" aria-hidden="true" />
          </div>

          {/* Heading and Description */}
          <h3 className="text-base sm:text-lg font-bold text-rose-900 tracking-tight mb-2">
            {title}
          </h3>

          <p className="text-xs sm:text-sm text-rose-700/90 max-w-lg mb-6 leading-relaxed text-balance">
            {description}
          </p>

          {/* Action CTAs */}
          <div className="flex items-center gap-3 flex-wrap justify-center mb-6">
            <Button
              variant="danger"
              size="sm"
              leftIcon={<RefreshCw className="h-4 w-4" aria-hidden="true" />}
              onClick={this.resetErrorBoundary}
            >
              Thử lại ngay
            </Button>

            <Button
              variant="outline"
              size="sm"
              leftIcon={<RotateCcw className="h-4 w-4 text-slate-500" aria-hidden="true" />}
              onClick={() => window.location.reload()}
            >
              Tải lại trang
            </Button>
          </div>

          {/* Collapsible Technical Debug Details */}
          <div className="w-full text-left">
            <button
              type="button"
              onClick={this.toggleDetails}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-rose-800 hover:text-rose-950 focus:outline-none transition py-1"
            >
              {showDetails ? (
                <ChevronUp className="h-3.5 w-3.5" />
              ) : (
                <ChevronDown className="h-3.5 w-3.5" />
              )}
              <span>{showDetails ? 'Ẩn chi tiết kỹ thuật' : 'Xem chi tiết kỹ thuật (Debug)'}</span>
            </button>

            {showDetails && (
              <div className="mt-2 p-3 bg-slate-900 text-rose-200 rounded-xl text-xs font-mono overflow-x-auto max-h-56 leading-relaxed border border-slate-800">
                <div className="font-bold text-rose-400 mb-1">
                  {error.name}: {error.message}
                </div>
                {error.stack && (
                  <pre className="text-[11px] text-slate-400 whitespace-pre-wrap">
                    {error.stack}
                  </pre>
                )}
                {errorInfo?.componentStack && (
                  <div className="mt-2 pt-2 border-t border-slate-800 text-[10px] text-slate-500">
                    <span className="font-semibold text-slate-400">Component Stack:</span>
                    <pre className="whitespace-pre-wrap">{errorInfo.componentStack}</pre>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      );
    }

    return children;
  }
}
