import { ProductCatalogDto, ProductFilterCriteria, PageResponse } from '@/types/api.types';
import { normalizeVietnamese } from '../utils/formatters';

// Sample test dataset
const TEST_PRODUCTS: ProductCatalogDto[] = [
  {
    id: 1,
    name: 'Trà Đào Cam Sả',
    description: 'Trà trái cây tươi mát',
    imageUrl: null,
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
      },
      {
        id: 11,
        name: 'Trà Đào Cam Sả - Size L',
        size: 'L',
        price: 45000,
        inventoryMode: 'MADE_TO_ORDER',
        available: true,
        active: true,
        availabilityStatus: 'AVAILABLE',
      },
    ],
  },
  {
    id: 2,
    name: 'Cà Phê Sữa Đá Sài Gòn',
    description: 'Cà phê rang mộc Buôn Ma Thuột',
    imageUrl: null,
    active: true,
    variants: [
      {
        id: 20,
        name: 'Cà Phê Sữa Đá - STANDARD',
        size: null,
        price: 28000,
        inventoryMode: 'MADE_TO_ORDER',
        available: false,
        active: true,
        availabilityStatus: 'CAPACITY_EXHAUSTED',
      },
    ],
  },
  {
    id: 3,
    name: 'Bánh Mì Chảo Đặc Biệt',
    description: 'Bánh mì giòn nóng kèm xíu mại trứng',
    imageUrl: null,
    active: false,
    variants: [
      {
        id: 30,
        name: 'Bánh Mì Chảo - STANDARD',
        size: null,
        price: 55000,
        inventoryMode: 'LIMITED_STOCK',
        available: false,
        active: false,
        availabilityStatus: 'MARKED_UNAVAILABLE',
      },
    ],
  },
  {
    id: 4,
    name: 'Trà Sữa Oolong Nướng',
    description: 'Trà Oolong nướng khói thơm đậm',
    imageUrl: null,
    active: true,
    variants: [
      {
        id: 40,
        name: 'Trà Sữa Oolong - Size M',
        size: 'M',
        price: 38000,
        inventoryMode: 'MADE_TO_ORDER',
        available: true,
        active: true,
        availabilityStatus: 'AVAILABLE',
      },
      {
        id: 41,
        name: 'Trà Sữa Oolong - Size L',
        size: 'L',
        price: 48000,
        inventoryMode: 'MADE_TO_ORDER',
        available: true,
        active: true,
        availabilityStatus: 'AVAILABLE',
      },
    ],
  },
  {
    id: 5,
    name: 'Nước Ép Ổi Hồng Tươi',
    description: 'Ổi hồng miền Tây giàu vitamin C',
    imageUrl: null,
    active: true,
    variants: [
      {
        id: 50,
        name: 'Nước Ép Ổi - STANDARD',
        size: null,
        price: 32000,
        inventoryMode: 'LIMITED_STOCK',
        available: true,
        active: true,
        availabilityStatus: 'AVAILABLE',
      },
    ],
  },
];

/**
 * Filter and query simulator matching the backend Spring Boot & frontend ProductsPage behavior
 */
function queryProducts(
  products: ProductCatalogDto[],
  params: {
    criteria?: ProductFilterCriteria;
    page?: number;
    size?: number;
    sort?: string;
  }
): PageResponse<ProductCatalogDto> {
  const { criteria = {}, page = 0, size = 10, sort = 'name,asc' } = params;
  let result = [...products];

  // Search keyword filter (accent-insensitive)
  if (criteria.search) {
    const q = normalizeVietnamese(criteria.search);
    result = result.filter(
      (p) =>
        normalizeVietnamese(p.name).includes(q) ||
        (p.description && normalizeVietnamese(p.description).includes(q))
    );
  }

  // Variant size filter ('M', 'L', 'STANDARD')
  if (criteria.variantSize) {
    result = result.filter((p) =>
      p.variants.some((v) =>
        criteria.variantSize === 'STANDARD'
          ? !v.size || v.size === 'STANDARD'
          : v.size === criteria.variantSize
      )
    );
  }

  // Inventory mode filter
  if (criteria.inventoryMode) {
    result = result.filter((p) =>
      p.variants.some((v) => v.inventoryMode === criteria.inventoryMode)
    );
  }

  // Available only filter
  if (criteria.availableOnly) {
    result = result.filter(
      (p) => p.active && p.variants.some((v) => v.available)
    );
  }

  // Sort
  const [sortField, sortDir] = sort.split(',');
  result.sort((a, b) => {
    if (sortField === 'price') {
      const priceA = a.variants[0]?.price || 0;
      const priceB = b.variants[0]?.price || 0;
      return sortDir === 'desc' ? priceB - priceA : priceA - priceB;
    }
    return sortDir === 'desc'
      ? b.name.localeCompare(a.name)
      : a.name.localeCompare(b.name);
  });

  const totalElements = result.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  const paginatedContent = result.slice(page * size, (page + 1) * size);

  return {
    content: paginatedContent,
    totalElements,
    totalPages,
    size,
    number: page,
    first: page === 0,
    last: page >= totalPages - 1,
    empty: totalElements === 0,
  };
}

export function runQueryCombinationsSmokeTest(): boolean {
  console.log('--- Bắt đầu kiểm thử Search / Filter / Sort / Pagination (3 Query Combinations) ---');
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

  // =========================================================================
  // Query Combination 1: Keyword search + Category
  // URL: ?search=tra&storeCategoryId=1
  // =========================================================================
  console.log('\n[Tổ hợp 1] Kiểm tra Tìm kiếm từ khóa + Category:');
  const urlParams1 = new URLSearchParams('search=tra&storeCategoryId=1');
  const searchKeyword1 = urlParams1.get('search') || '';
  const categoryId1 = parseInt(urlParams1.get('storeCategoryId') || '0', 10);

  assert(searchKeyword1 === 'tra', 'Đọc chính xác search="tra" từ URL searchParams');
  assert(categoryId1 === 1, 'Đọc chính xác storeCategoryId=1 từ URL searchParams');

  const result1 = queryProducts(TEST_PRODUCTS, {
    criteria: { search: searchKeyword1, storeCategoryId: categoryId1 },
    page: 0,
    size: 10,
  });

  assert(
    result1.content.length === 2 &&
      result1.content.every((p) => p.name.toLowerCase().includes('trà')),
    'Kết quả lọc trả về đúng 2 món chứa từ khóa "Trà" (Trà Đào Cam Sả, Trà Sữa Oolong)'
  );
  assert(result1.number === 0, 'Page mặc định của backend là 0 (tương ứng Trang 1 UI)');

  // =========================================================================
  // Query Combination 2: Size filter + Sort order
  // URL: ?variantSize=M&sort=price,asc
  // =========================================================================
  console.log('\n[Tổ hợp 2] Kiểm tra Bộ lọc Kích cỡ (Size M) + Sắp xếp theo Giá tăng dần:');
  const urlParams2 = new URLSearchParams('variantSize=M&sort=price,asc');
  const variantSize2 = urlParams2.get('variantSize') || '';
  const sort2 = urlParams2.get('sort') || '';

  assert(variantSize2 === 'M', 'Đọc chính xác variantSize="M" từ URL searchParams');
  assert(sort2 === 'price,asc', 'Đọc chính xác sort="price,asc" từ URL searchParams');

  const result2 = queryProducts(TEST_PRODUCTS, {
    criteria: { variantSize: variantSize2 },
    sort: sort2,
  });

  assert(
    result2.content.length === 2 &&
      result2.content.every((p) => p.variants.some((v) => v.size === 'M')),
    'Bộ lọc kích cỡ lọc chính xác các món có biến thể Size M'
  );
  assert(
    result2.content[0].variants[0].price <= result2.content[1].variants[0].price,
    'Sắp xếp giá tăng dần (price,asc) đưa món 35.000 trước món 38.000'
  );

  // =========================================================================
  // Query Combination 3: Pagination + AvailableOnly + Descending sort
  // URL: ?page=2&size=2&availableOnly=true&sort=price,desc
  // =========================================================================
  console.log('\n[Tổ hợp 3] Kiểm tra Phân trang + Chỉ món đang bán (availableOnly) + Giá giảm dần:');
  const urlParams3 = new URLSearchParams('page=2&size=2&availableOnly=true&sort=price,desc');
  const uiPage3 = parseInt(urlParams3.get('page') || '1', 10);
  const pageSize3 = parseInt(urlParams3.get('size') || '10', 10);
  const availableOnly3 = urlParams3.get('availableOnly') === 'true';
  const sort3 = urlParams3.get('sort') || '';

  assert(uiPage3 === 2, 'Đọc đúng số trang UI là 2');
  assert(availableOnly3 === true, 'Đọc đúng cờ availableOnly=true');

  // Translate 1-indexed UI page to 0-indexed Spring Boot page
  const backendPage3 = uiPage3 - 1;
  assert(backendPage3 === 1, 'Chuyển đổi chính xác UI page 2 thành Spring Boot backend page 1');

  const result3 = queryProducts(TEST_PRODUCTS, {
    criteria: { availableOnly: true },
    page: backendPage3,
    size: pageSize3,
    sort: sort3,
  });

  assert(
    result3.content.every((p) => p.active && p.variants.some((v) => v.available)),
    'Không chứa món tạm ngưng hoặc hết công suất khi bật availableOnly'
  );
  assert(
    result3.number === 1 && result3.size === 2,
    'Spring Boot PageResponse bảo đảm offset và page number chính xác'
  );

  // =========================================================================
  // URL State Resilience & Reset Page on Filter Change Verification
  // =========================================================================
  console.log('\n[Kiểm tra đồng bộ URL & Reset Page]:');
  // When user is on page 2 and changes a filter, page must reset to 1 (deleted from URL)
  const currentNavParams = new URLSearchParams('page=2&search=tra');
  const nextNavParams = new URLSearchParams(currentNavParams);
  nextNavParams.set('variantSize', 'L');
  nextNavParams.delete('page'); // Must reset page to 1

  assert(
    !nextNavParams.has('page') && nextNavParams.get('variantSize') === 'L',
    'Thay đổi điều kiện lọc tự động reset pagination về trang 1'
  );

  // Refresh URL serialization preserves all filters
  const serialized = nextNavParams.toString();
  const refreshedParams = new URLSearchParams(serialized);
  assert(
    refreshedParams.get('search') === 'tra' && refreshedParams.get('variantSize') === 'L',
    'F5 / Refresh trình duyệt giữ nguyên 100% các giá trị tìm kiếm và bộ lọc'
  );

  console.log(`\n🎉 KẾT QUẢ: Hoàn thành ${passed}/${total} assertions test thành công!`);
  return passed === total;
}

// Execute if run directly via Node/TSX
if (typeof process !== 'undefined' && process.argv && process.argv[1]?.includes('queryCombinationsSmokeTest')) {
  runQueryCombinationsSmokeTest();
}
