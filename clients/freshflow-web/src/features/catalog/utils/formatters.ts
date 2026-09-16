import { AvailabilityStatus } from '@/types/api.types';

/**
 * Format currency number into Vietnamese Dong (VND)
 * Example: 35000 -> 35.000đ
 */
export function formatVND(amount: number): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  })
    .format(amount)
    .replace('₫', 'đ');
}

/**
 * Maps backend AvailabilityStatus to human-readable Vietnamese label and badge variant
 */
export function getCapacityStatusInfo(status: AvailabilityStatus): {
  label: string;
  variant: 'success' | 'warning' | 'danger' | 'default';
  description: string;
} {
  switch (status) {
    case 'AVAILABLE':
      return {
        label: 'Đang mở bán',
        variant: 'success',
        description: 'Món ăn có sẵn công suất phục vụ hôm nay',
      };
    case 'CAPACITY_EXHAUSTED':
      return {
        label: 'Hết công suất',
        variant: 'warning',
        description: 'Định mức chế biến trong ngày đã đạt giới hạn tối đa (BR-08)',
      };
    case 'MARKED_UNAVAILABLE':
      return {
        label: 'Tạm ngưng bán',
        variant: 'danger',
        description: 'Chủ cửa hàng tạm tắt món này',
      };
    case 'CAPACITY_NOT_CONFIGURED':
    default:
      return {
        label: 'Chưa cấu hình',
        variant: 'default',
        description: 'Chưa thiết lập định mức công suất ngày',
      };
  }
}

/**
 * Normalizes Vietnamese text by stripping accents/diacritics for flexible search
 * Example: "Trà Đào Cam Sả" -> "tra dao cam sa"
 */
export function normalizeVietnamese(text: string): string {
  return text
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'd')
    .toLowerCase();
}
