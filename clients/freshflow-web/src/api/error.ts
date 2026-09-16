import axios, { AxiosError } from 'axios';
import { ApiErrorResponse, FieldError } from '@/types/api.types';

/**
 * Normalized application error class for all network and HTTP failures
 */
export class ApiError extends Error {
  readonly code: string;
  readonly status?: number;
  readonly path?: string;
  readonly timestamp: string;
  readonly fieldErrors: FieldError[];

  constructor({
    code,
    message,
    status,
    path,
    timestamp,
    fieldErrors = [],
  }: {
    code: string;
    message: string;
    status?: number;
    path?: string;
    timestamp?: string;
    fieldErrors?: FieldError[];
  }) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
    this.path = path;
    this.timestamp = timestamp || new Date().toISOString();
    this.fieldErrors = fieldErrors;

    // Maintain proper prototype chain in TypeScript
    Object.setPrototypeOf(this, ApiError.prototype);
  }

  /**
   * Helper to retrieve a specific field's error message
   */
  getFieldError(fieldName: string): string | undefined {
    return this.fieldErrors.find((fe) => fe.field === fieldName)?.message;
  }
}

/**
 * Normalizes any caught error (AxiosError, Error, string, etc.) into a strongly-typed ApiError
 */
export function normalizeApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }

  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiErrorResponse>;
    const status = axiosError.response?.status;
    const data = axiosError.response?.data;

    // 1. Check if backend returned a standard ApiErrorResponse payload
    if (data && typeof data === 'object' && 'code' in data && 'message' in data) {
      return new ApiError({
        code: typeof data.code === 'string' ? data.code : 'API_ERROR',
        message:
          typeof data.message === 'string' ? data.message : 'Đã có lỗi từ máy chủ API FreshFlow',
        status,
        path: typeof data.path === 'string' ? data.path : axiosError.config?.url,
        timestamp: typeof data.timestamp === 'string' ? data.timestamp : undefined,
        fieldErrors: Array.isArray(data.fieldErrors) ? data.fieldErrors : [],
      });
    }

    // 2. Timeout Error
    if (axiosError.code === 'ECONNABORTED' || axiosError.message.includes('timeout')) {
      return new ApiError({
        code: 'TIMEOUT_ERROR',
        message: 'Yêu cầu kết nối máy chủ quá thời gian chờ (10s). Vui lòng thử lại sau.',
        status: 408,
        path: axiosError.config?.url,
      });
    }

    // 3. Network Connection Error (no response received)
    if (!axiosError.response) {
      return new ApiError({
        code: 'NETWORK_ERROR',
        message:
          'Không thể kết nối đến máy chủ FreshFlow API. Vui lòng kiểm tra lại đường truyền mạng hoặc cấu hình VITE_API_URL.',
        status: 0,
        path: axiosError.config?.url,
      });
    }

    // 4. HTTP Status based fallback
    let defaultCode = 'HTTP_ERROR';
    let defaultMessage = `Máy chủ phản hồi với mã lỗi HTTP ${status}`;

    if (status === 400) {
      defaultCode = 'VALIDATION_ERROR';
      defaultMessage = 'Dữ liệu gửi lên không hợp lệ';
    } else if (status === 401) {
      defaultCode = 'UNAUTHORIZED';
      defaultMessage = 'Phiên đăng nhập đã hết hạn hoặc không hợp lệ';
    } else if (status === 403) {
      defaultCode = 'ACCESS_DENIED';
      defaultMessage = 'Bạn không có quyền thực hiện thao tác này';
    } else if (status === 404) {
      defaultCode = 'NOT_FOUND';
      defaultMessage = 'Không tìm thấy tài nguyên yêu cầu trên hệ thống';
    } else if (status && status >= 500) {
      defaultCode = 'INTERNAL_SERVER_ERROR';
      defaultMessage = 'Lỗi xử lý nội bộ tại máy chủ FreshFlow';
    }

    return new ApiError({
      code: defaultCode,
      message: defaultMessage,
      status,
      path: axiosError.config?.url,
    });
  }

  // 5. Standard JavaScript Error
  if (error instanceof Error) {
    return new ApiError({
      code: 'UNEXPECTED_ERROR',
      message: error.message || 'Đã có lỗi không mong muốn xảy ra',
    });
  }

  // 6. Unknown / Primitive Error
  return new ApiError({
    code: 'UNKNOWN_ERROR',
    message: typeof error === 'string' ? error : 'Đã có lỗi không xác định xảy ra',
  });
}
