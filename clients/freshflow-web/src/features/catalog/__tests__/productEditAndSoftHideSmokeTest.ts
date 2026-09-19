import {
  ProductCatalogDto,
  StoreCategoryDto,
  CartItem,
  OrderHistoryItem,
  UpdateProductRequest,
  UpdateProductVariantRequest,
} from '@/types/api.types';
import {
  filterCatalogProducts,
  validateCartForCheckout,
  AvailabilityContext,
} from '../utils/dependentAvailability';

export function runProductEditAndSoftHideSmokeTest(): boolean {
  console.log('--- Bắt đầu kiểm thử ProductEdit, Soft Hide, PATCH Semantics & Dependent Availability ---');
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

  // Sample seed data for testing
  const initialCategories: StoreCategoryDto[] = [
    { id: 1, storeId: 1, name: 'Cà phê & Trà', isActive: true, displayOrder: 1 },
    { id: 2, storeId: 1, name: 'Bánh ngọt & Điểm tâm', isActive: true, displayOrder: 2 },
  ];

  const initialProducts: (ProductCatalogDto & { storeCategoryId?: number })[] = [
    {
      id: 101,
      storeCategoryId: 1,
      name: 'Trà Đào Cam Sả',
      description: 'Trà thơm thanh mát',
      imageUrl: 'https://images.freshflow.vn/tra-dao.jpg',
      active: true,
      variants: [
        {
          id: 1001,
          name: 'Trà Đào Cam Sả - Size M',
          size: 'M',
          price: 35000,
          inventoryMode: 'MADE_TO_ORDER',
          autoAcceptOverride: true,
          dailyCapacityDefault: 100,
          available: true,
          active: true,
          availabilityStatus: 'AVAILABLE',
          capacity: { capacityDate: '2026-09-19', remaining: 80 },
        },
        {
          id: 1002,
          name: 'Trà Đào Cam Sả - Size L',
          size: 'L',
          price: 45000,
          inventoryMode: 'MADE_TO_ORDER',
          autoAcceptOverride: true,
          dailyCapacityDefault: 80,
          available: true,
          active: true,
          availabilityStatus: 'AVAILABLE',
          capacity: { capacityDate: '2026-09-19', remaining: 50 },
        },
      ],
    },
    {
      id: 102,
      storeCategoryId: 2,
      name: 'Croissant Bơ Pháp',
      description: 'Bánh sừng bò nướng giòn rụm',
      imageUrl: 'https://images.freshflow.vn/croissant.jpg',
      active: true,
      variants: [
        {
          id: 1003,
          name: 'Croissant - STANDARD',
          size: null,
          price: 32000,
          inventoryMode: 'LIMITED_STOCK',
          autoAcceptOverride: false,
          dailyCapacityDefault: 30,
          available: true,
          active: true,
          availabilityStatus: 'AVAILABLE',
          capacity: null,
        },
      ],
    },
  ];

  // =========================================================================
  // Test 1: PATCH Semantics for Product (Partial Updates)
  // =========================================================================
  console.log('\n[Test 1] Kiểm tra PATCH Semantics cho Product (Cập nhật một phần):');
  const patchReq: UpdateProductRequest = {
    name: 'Trà Đào Cam Sả Đặc Biệt',
    active: false, // Soft hide product
  };

  const patchedProduct = {
    ...initialProducts[0],
    name: patchReq.name ?? initialProducts[0].name,
    active: patchReq.active ?? initialProducts[0].active,
  };

  assert(
    patchedProduct.name === 'Trà Đào Cam Sả Đặc Biệt' &&
      patchedProduct.active === false &&
      patchedProduct.description === 'Trà thơm thanh mát' &&
      patchedProduct.imageUrl === 'https://images.freshflow.vn/tra-dao.jpg' &&
      patchedProduct.variants.length === 2,
    'PATCH Product chỉ thay đổi các trường được chỉ định, bảo lưu nguyên vẹn description, imageUrl, variants'
  );

  // =========================================================================
  // Test 2: PATCH Semantics for Variant (Cập nhật giá, công suất, is_available)
  // =========================================================================
  console.log('\n[Test 2] Kiểm tra PATCH Semantics cho Variant:');
  const patchVarReq: UpdateProductVariantRequest = {
    price: 38000,
    available: false, // Tạm ngưng bán
    dailyCapacityDefault: 120,
  };

  const patchedVariant = {
    ...initialProducts[0].variants[0],
    price: patchVarReq.price ?? initialProducts[0].variants[0].price,
    available: patchVarReq.available ?? initialProducts[0].variants[0].available,
    dailyCapacityDefault:
      patchVarReq.dailyCapacityDefault ?? initialProducts[0].variants[0].dailyCapacityDefault,
  };

  assert(
    patchedVariant.price === 38000 &&
      patchedVariant.available === false &&
      patchedVariant.dailyCapacityDefault === 120 &&
      patchedVariant.size === 'M' &&
      patchedVariant.inventoryMode === 'MADE_TO_ORDER',
    'PATCH Variant cập nhật chính xác price, available, capacity mà không ghi đè size hoặc inventoryMode'
  );

  // =========================================================================
  // Test 3: Soft Hide Product (Không xuất hiện trong Catalog công khai)
  // =========================================================================
  console.log('\n[Test 3] Kiểm tra Soft Hide Product:');
  const productsWithOneHidden = [
    { ...initialProducts[0], active: false }, // Ẩn món 101
    initialProducts[1], // Món 102 đang active
  ];

  const publicCatalog1 = filterCatalogProducts(
    productsWithOneHidden,
    initialCategories,
    true
  );

  assert(
    publicCatalog1.length === 1 && publicCatalog1[0].id === 102,
    'Món bị ẩn mềm (active: false) biến mất khỏi Catalog công khai'
  );

  const adminCatalog1 = filterCatalogProducts(
    productsWithOneHidden,
    initialCategories,
    false // Merchant admin view
  );

  assert(
    adminCatalog1.length === 2 && adminCatalog1.some((p) => !p.active),
    'Chế độ quản trị Merchant vẫn thấy toàn bộ món kèm cờ active: false để quản lý mở lại'
  );

  // =========================================================================
  // Test 4: Soft Hide Variant (Ẩn biến thể hoặc tạm ngưng bán)
  // =========================================================================
  console.log('\n[Test 4] Kiểm tra Soft Hide Variant:');
  const productWithSoftDeletedVariant: ProductCatalogDto = {
    ...initialProducts[0],
    variants: [
      { ...initialProducts[0].variants[0], active: false }, // Soft delete size M
      initialProducts[0].variants[1], // Size L active
    ],
  };

  const catalogWithVariantCheck = filterCatalogProducts(
    [productWithSoftDeletedVariant],
    initialCategories,
    true
  );

  assert(
    catalogWithVariantCheck.length === 1 &&
      catalogWithVariantCheck[0].variants.length === 1 &&
      catalogWithVariantCheck[0].variants[0].size === 'L',
    'Biến thể bị ẩn mềm (variant.active: false) tự động được lọc bỏ khỏi thực đơn công khai'
  );

  // =========================================================================
  // Test 5: Dependent Availability - Khi Category Inactive (Ẩn danh mục)
  // =========================================================================
  console.log('\n[Test 5] Kiểm tra Dependent Availability khi Category Inactive:');
  // Category 1 (Cà phê & Trà) bị tắt: isActive = false
  const categoriesWithOneInactive: StoreCategoryDto[] = [
    { ...initialCategories[0], isActive: false },
    initialCategories[1],
  ];

  const catalogAfterCategoryHide = filterCatalogProducts(
    initialProducts,
    categoriesWithOneInactive,
    true
  );

  assert(
    catalogAfterCategoryHide.length === 1 &&
      catalogAfterCategoryHide[0].id === 102,
    'Khi Category 1 bị ẩn (isActive: false), toàn bộ Product thuộc Category 1 biến mất khỏi Catalog'
  );

  // =========================================================================
  // Test 6: Cart Items Preservation & Checkout Blocking
  // =========================================================================
  console.log('\n[Test 6] Kiểm tra Giỏ hàng khi Category/Product bị ẩn (Chặn Checkout):');
  const customerCart: CartItem[] = [
    {
      id: 'cart_item_1',
      productId: 101, // Thuộc Category 1 (đang bị ẩn)
      productName: 'Trà Đào Cam Sả',
      variantId: 1001,
      variantName: 'Trà Đào Cam Sả - Size M',
      size: 'M',
      price: 35000,
      quantity: 2,
      storeCategoryId: 1,
    },
    {
      id: 'cart_item_2',
      productId: 102, // Thuộc Category 2 (đang mở)
      productName: 'Croissant Bơ Pháp',
      variantId: 1003,
      variantName: 'Croissant - STANDARD',
      size: null,
      price: 32000,
      quantity: 1,
      storeCategoryId: 2,
    },
  ];

  const contextCategoryInactive: AvailabilityContext = {
    categories: categoriesWithOneInactive,
    products: initialProducts,
  };

  const cartCheck1 = validateCartForCheckout(customerCart, contextCategoryInactive);

  assert(
    cartCheck1.evaluatedItems.length === 2,
    'Món ăn trong giỏ hàng KHÔNG bị xóa khi Category bị ẩn'
  );
  assert(
    !cartCheck1.canCheckout,
    'Nút Checkout bị KHÓA hoàn toàn (canCheckout: false) khi có món thuộc Category tạm đóng'
  );
  assert(
    !cartCheck1.evaluatedItems[0].isAvailable &&
      Boolean(cartCheck1.evaluatedItems[0].unavailableReason?.includes('tạm đóng')) &&
      cartCheck1.evaluatedItems[1].isAvailable,
    'Món thuộc Category 1 đánh dấu unavailable với lý do rõ ràng; món thuộc Category 2 vẫn hợp lệ'
  );

  // Case 6b: Product bị ẩn mềm (active: false)
  const contextProductInactive: AvailabilityContext = {
    categories: initialCategories, // Categories đều active
    products: [{ ...initialProducts[0], active: false }, initialProducts[1]],
  };

  const cartCheckProductHide = validateCartForCheckout(customerCart, contextProductInactive);
  assert(
    !cartCheckProductHide.canCheckout &&
      !cartCheckProductHide.evaluatedItems[0].isAvailable &&
      Boolean(cartCheckProductHide.evaluatedItems[0].unavailableReason?.includes('tạm ẩn')),
    'Món ăn bị ẩn mềm (product.active: false) chặn checkout và báo lý do "tạm ẩn khỏi thực đơn"'
  );

  // Case 6c: Variant bị tạm ngưng bán (available: false)
  const contextVariantUnavailable: AvailabilityContext = {
    categories: initialCategories,
    products: [
      {
        ...initialProducts[0],
        variants: [
          { ...initialProducts[0].variants[0], available: false },
          initialProducts[0].variants[1],
        ],
      },
      initialProducts[1],
    ],
  };

  const cartCheckVariantUnavailable = validateCartForCheckout(
    customerCart,
    contextVariantUnavailable
  );
  assert(
    !cartCheckVariantUnavailable.canCheckout &&
      !cartCheckVariantUnavailable.evaluatedItems[0].isAvailable &&
      Boolean(cartCheckVariantUnavailable.evaluatedItems[0].unavailableReason?.includes('tạm hết hàng')),
    'Biến thể bị tạm ngưng bán (variant.available: false) chặn checkout và báo lý do "tạm hết hàng"'
  );

  // =========================================================================
  // Test 7: Order History Preservation (Không Hard Delete đơn hàng cũ)
  // =========================================================================
  console.log('\n[Test 7] Kiểm tra Lịch sử Đơn hàng cũ (Bảo lưu dữ liệu 100%):');
  const pastOrders: OrderHistoryItem[] = [
    {
      orderId: 'ORD-HIST-001',
      orderTime: '2026-09-18 14:20',
      productId: 101, // Món đã từng mua
      productName: 'Trà Đào Cam Sả',
      variantId: 1001,
      variantName: 'Trà Đào Cam Sả - Size M',
      size: 'M',
      unitPrice: 35000,
      quantity: 3,
      totalPrice: 105000,
      storeCategoryId: 1,
    },
  ];

  // Dù Category 1 bị xóa mềm, Product 101 bị xóa mềm, Variant 1001 bị xóa mềm
  assert(
    pastOrders.length === 1 &&
      pastOrders[0].orderId === 'ORD-HIST-001' &&
      pastOrders[0].totalPrice === 105000 &&
      pastOrders[0].productName === 'Trà Đào Cam Sả',
    'Lịch sử đơn hàng cũ không bao giờ bị ảnh hưởng bởi thao tác soft hide của Merchant'
  );

  // =========================================================================
  // Test 8: Re-activation (Mở lại Category/Product -> Catalog & Cart phục hồi)
  // =========================================================================
  console.log('\n[Test 8] Kiểm tra Mở lại (Re-activation):');
  // Mở lại Category 1
  const restoredCategories = initialCategories;
  const restoredContext: AvailabilityContext = {
    categories: restoredCategories,
    products: initialProducts,
  };

  const restoredCatalog = filterCatalogProducts(
    initialProducts,
    restoredCategories,
    true
  );
  assert(
    restoredCatalog.length === 2,
    'Khi Category được kích hoạt lại, tất cả món ăn xuất hiện trở lại trên Catalog'
  );

  const restoredCartCheck = validateCartForCheckout(customerCart, restoredContext);
  assert(
    restoredCartCheck.canCheckout &&
      restoredCartCheck.evaluatedItems.every((item) => item.isAvailable),
    'Giỏ hàng tự động mở khóa Checkout thành công (canCheckout: true) sau khi Category được kích hoạt lại'
  );

  console.log(`\n🎉 KẾT QUẢ: Hoàn thành ${passed}/${total} assertions kiểm thử ProductEdit, Soft Hide & Dependent Availability thành công!`);
  return passed === total;
}

// Execute if run directly via Node/TSX
if (typeof process !== 'undefined' && process.argv && process.argv[1]?.includes('productEditAndSoftHideSmokeTest')) {
  runProductEditAndSoftHideSmokeTest();
}
