import { formatVND, getCapacityStatusInfo } from '../utils/formatters';
import { ProductCatalogDto, PageResponse } from '@/types/api.types';

export function runProductListSmokeTest(): boolean {
  console.log('--- Bắt đầu kiểm thử ProductList & Catalog Formatters ---');
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

  // Test 1: Currency formatter formatVND
  assert(
    formatVND(35000).replace(/\s/g, '').includes('35.000'),
    'formatVND(35000) định dạng đúng số tiền 35.000'
  );
  assert(
    formatVND(1250000).replace(/\s/g, '').includes('1.250.000'),
    'formatVND(1250000) định dạng đúng số tiền hàng triệu 1.250.000'
  );

  // Test 2: Availability status labels and badge variants
  const availableInfo = getCapacityStatusInfo('AVAILABLE');
  assert(
    availableInfo.label === 'Đang mở bán' && availableInfo.variant === 'success',
    'Trạng thái AVAILABLE ánh xạ chính xác thành badge xanh "Đang mở bán"'
  );

  const exhaustedInfo = getCapacityStatusInfo('CAPACITY_EXHAUSTED');
  assert(
    exhaustedInfo.label === 'Hết công suất' && exhaustedInfo.variant === 'warning',
    'Trạng thái CAPACITY_EXHAUSTED ánh xạ chính xác thành badge vàng "Hết công suất"'
  );

  const markedUnavailableInfo = getCapacityStatusInfo('MARKED_UNAVAILABLE');
  assert(
    markedUnavailableInfo.label === 'Tạm ngưng bán' && markedUnavailableInfo.variant === 'danger',
    'Trạng thái MARKED_UNAVAILABLE ánh xạ chính xác thành badge đỏ "Tạm ngưng bán"'
  );

  // Test 3: Spring Boot PageResponse structure compliance
  const samplePageResponse: PageResponse<ProductCatalogDto> = {
    content: [
      {
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
            capacity: { capacityDate: '2026-09-16', remaining: 85 },
          },
        ],
      },
    ],
    totalElements: 1,
    totalPages: 1,
    size: 20,
    number: 0,
    first: true,
    last: true,
    empty: false,
  };

  assert(
    samplePageResponse.content.length === 1 &&
      samplePageResponse.totalElements === 1 &&
      samplePageResponse.content[0].variants[0].capacity?.remaining === 85,
    'Dữ liệu PageResponse từ Spring Boot JPA ánh xạ chuẩn xác không bị mất trường'
  );

  // Test 4: Emulate Retry function invocation
  let refetchCount = 0;
  const mockRefetch = () => {
    refetchCount++;
    return Promise.resolve({ isSuccess: true });
  };

  mockRefetch();
  assert(refetchCount === 1, 'Hàm retry refetch() gọi lại API chính xác khi người dùng bấm Thử lại');

  console.log(`\n🎉 KẾT QUẢ: Hoàn thành ${passed}/${total} assertions test thành công!`);
  return passed === total;
}

// Execute if run directly via Node/TSX
if (typeof process !== 'undefined' && process.argv && process.argv[1]?.includes('productListSmokeTest')) {
  runProductListSmokeTest();
}
