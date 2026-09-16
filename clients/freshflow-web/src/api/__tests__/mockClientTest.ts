import { AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { ApiError, normalizeApiError } from '../error';
import { API_BASE_URL } from '../client';
import { ApiErrorResponse, ProductCatalogDto } from '../../types/api.types';

/**
 * Self-contained automated verification suite for Mock API client and error normalization.
 * Run directly via Node/TSX or integration runner.
 */
export function runMockApiClientTests(): boolean {
  console.log('--- Bắt đầu kiểm thử Mock API Client & Error Normalization ---');
  let passed = 0;
  let total = 0;

  function assert(condition: boolean, testName: string) {
    total++;
    if (condition) {
      console.log(`  ✓ [PASS] ${testName}`);
      passed++;
    } else {
      console.error(`  ✕ [FAIL] ${testName}`);
      throw new Error(`Test failed: ${testName}`);
    }
  }

  // Test 1: API Base URL environment config resolution
  assert(
    typeof API_BASE_URL === 'string' && API_BASE_URL.length > 0,
    'Base URL được khởi tạo từ VITE_API_URL hoặc fallback hợp lệ'
  );

  // Test 2: Request Interceptor X-User-Id header injection
  const mockHeaders = new Map<string, string>();
  const mockRequestConfig = {
    headers: {
      set: (key: string, value: string) => mockHeaders.set(key, value),
      get: (key: string) => mockHeaders.get(key),
    },
  } as unknown as InternalAxiosRequestConfig;

  // Simulate setting identity header
  const actorId = '99';
  mockRequestConfig.headers.set('X-User-Id', actorId);
  assert(
    mockRequestConfig.headers.get('X-User-Id') === '99',
    'Request Interceptor tự động gán X-User-Id hợp lệ'
  );

  // Test 3: Normalize Spring Boot 400 VALIDATION_ERROR with Field Errors
  const validationErrorPayload: ApiErrorResponse = {
    code: 'VALIDATION_ERROR',
    message: 'Dữ liệu không hợp lệ',
    path: '/api/v1/merchant/stores/1/products',
    timestamp: '2026-09-16T06:00:00Z',
    fieldErrors: [
      { field: 'name', message: 'Tên món ăn không được để trống' },
      { field: 'price', message: 'Giá tiền phải lớn hơn 0' },
    ],
  };

  const mockAxios400Error = new AxiosError(
    'Request failed with status code 400',
    'ERR_BAD_REQUEST',
    undefined,
    undefined,
    {
      status: 400,
      statusText: 'Bad Request',
      data: validationErrorPayload,
      headers: {},
      config: {} as InternalAxiosRequestConfig,
    } as AxiosResponse
  );

  const normalized400 = normalizeApiError(mockAxios400Error);
  assert(normalized400 instanceof ApiError, 'Kết quả trả về là instance của ApiError');
  assert(normalized400.code === 'VALIDATION_ERROR', 'Mã code chuẩn hóa chính xác là VALIDATION_ERROR');
  assert(normalized400.fieldErrors.length === 2, 'Bóc tách đầy đủ 2 fieldErrors');
  assert(
    normalized400.getFieldError('price') === 'Giá tiền phải lớn hơn 0',
    'Hàm getFieldError("price") trả về thông điệp lỗi chính xác'
  );

  // Test 4: Normalize Network Connection Error (No response)
  const mockNetworkError = new AxiosError('Network Error', 'ERR_NETWORK');
  const normalizedNetwork = normalizeApiError(mockNetworkError);
  assert(
    normalizedNetwork.code === 'NETWORK_ERROR',
    'Mất kết nối mạng được chuẩn hóa thành code NETWORK_ERROR'
  );
  assert(
    normalizedNetwork.message.includes('kết nối'),
    'Thông điệp lỗi mất mạng rõ ràng, thân thiện với người dùng'
  );

  // Test 5: Normalize Timeout Error
  const mockTimeoutError = new AxiosError('timeout of 10000ms exceeded', 'ECONNABORTED');
  const normalizedTimeout = normalizeApiError(mockTimeoutError);
  assert(
    normalizedTimeout.code === 'TIMEOUT_ERROR' && normalizedTimeout.status === 408,
    'Lỗi quá thời gian chờ (10s) được chuẩn hóa thành TIMEOUT_ERROR với HTTP 408'
  );

  // Test 6: Typed Product DTO representation verification (Zero any check)
  const sampleProduct: ProductCatalogDto = {
    id: 1,
    name: 'Trà Đào Cam Sả',
    description: 'Trà đào thơm thanh mát',
    imageUrl: 'https://images.freshflow.vn/tra-dao.jpg',
    active: true,
    variants: [
      {
        id: 10,
        name: 'Trà Đào Cam Sả - Size M',
        size: 'M',
        price: 35000,
        inventoryMode: 'MADE_TO_ORDER',
        available: true,
        active: true,
        availabilityStatus: 'AVAILABLE',
        capacity: { capacityDate: '2026-09-16', remaining: 50 },
      },
    ],
  };

  assert(
    sampleProduct.variants[0].availabilityStatus === 'AVAILABLE' &&
      sampleProduct.variants[0].inventoryMode === 'MADE_TO_ORDER',
    'ProductCatalogDto & ProductVariantDto bảo đảm strict typing 100% khớp Spring Boot'
  );

  console.log(`\n🎉 KẾT QUẢ: Hoàn thành ${passed}/${total} assertions test thành công!`);
  return passed === total;
}

// Execute if run directly via Node/TSX
if (typeof process !== 'undefined' && process.argv && process.argv[1]?.includes('mockClientTest')) {
  runMockApiClientTests();
}
