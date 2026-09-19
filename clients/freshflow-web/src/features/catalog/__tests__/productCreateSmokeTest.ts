import {
  validateProductCreateForm,
  resolveVariantSize,
  resolveVariantName,
  transformToApiPayloads,
  createDefaultVariant,
} from '../utils/productValidation';
import { ProductCreateFormData, VariantFormData } from '@/types/api.types';

export function runProductCreateSmokeTest(): boolean {
  console.log('--- Bắt đầu kiểm thử ProductCreate Form, Variants, Sizing & Validations ---');
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
  // Test 1: Tạo món chuẩn không kích cỡ (STANDARD variant -> size: null)
  // =========================================================================
  console.log('\n[Test 1] Tạo món không kích cỡ (STANDARD - size: null):');
  const standardVariant = createDefaultVariant('STANDARD', 28000, 'MADE_TO_ORDER');
  standardVariant.dailyCapacityDefault = 120;
  standardVariant.autoAcceptOverride = true;

  const productStandardForm: ProductCreateFormData = {
    name: 'Cà Phê Sữa Đá Sài Gòn',
    storeCategoryId: 1,
    description: 'Cà phê Robusta rang mộc pha phin truyền thống',
    imageUrl: 'https://images.unsplash.com/cafe-sua-da.jpg',
    active: true,
    variants: [standardVariant],
  };

  const valStandard = validateProductCreateForm(productStandardForm);
  assert(valStandard.isValid, 'Form tạo món STANDARD hợp lệ 100%');
  assert(
    resolveVariantSize(standardVariant) === null,
    'Hàm resolveVariantSize trả về chính xác null cho biến thể STANDARD'
  );
  assert(
    resolveVariantName(standardVariant) === 'STANDARD',
    'Tên biến thể mặc định của món không size là "STANDARD"'
  );

  const payloadStandard = transformToApiPayloads(productStandardForm);
  assert(
    payloadStandard.variants.length === 1 &&
      payloadStandard.variants[0].size === null &&
      payloadStandard.variants[0].name === 'STANDARD' &&
      payloadStandard.variants[0].price === 28000 &&
      payloadStandard.variants[0].inventoryMode === 'MADE_TO_ORDER' &&
      payloadStandard.variants[0].dailyCapacityDefault === 120 &&
      payloadStandard.variants[0].autoAcceptOverride === true,
    'API Request payload cho STANDARD tuân thủ quy ước Spring Boot (size: null, dailyCapacity: 120)'
  );

  // =========================================================================
  // Test 2: Tạo món có kích cỡ (Size M và Size L)
  // =========================================================================
  console.log('\n[Test 2] Tạo món có kích cỡ (Size M và Size L):');
  const sizeMVariant: VariantFormData = {
    id: 'var_m_1',
    name: 'Size M',
    sizeType: 'M',
    customSize: '',
    price: 35000,
    inventoryMode: 'MADE_TO_ORDER',
    dailyCapacityDefault: 100,
    autoAcceptOverride: true,
    maxQuantityPerOrder: 10,
    available: true,
  };

  const sizeLVariant: VariantFormData = {
    id: 'var_l_1',
    name: 'Size L',
    sizeType: 'L',
    customSize: '',
    price: 45000,
    inventoryMode: 'MADE_TO_ORDER',
    dailyCapacityDefault: 80,
    autoAcceptOverride: false,
    maxQuantityPerOrder: 5,
    available: true,
  };

  const productSizedForm: ProductCreateFormData = {
    name: 'Trà Đào Cam Sả',
    storeCategoryId: 1,
    description: 'Trà trái cây thơm thanh mát',
    imageUrl: 'https://images.unsplash.com/tra-dao.jpg',
    active: true,
    variants: [sizeMVariant, sizeLVariant],
  };

  const valSized = validateProductCreateForm(productSizedForm);
  assert(valSized.isValid, 'Form tạo món nhiều kích cỡ (M và L) hợp lệ không có lỗi');

  const payloadSized = transformToApiPayloads(productSizedForm);
  assert(
    payloadSized.variants.length === 2 &&
      payloadSized.variants[0].size === 'M' &&
      payloadSized.variants[0].price === 35000 &&
      payloadSized.variants[0].autoAcceptOverride === true &&
      payloadSized.variants[1].size === 'L' &&
      payloadSized.variants[1].price === 45000 &&
      payloadSized.variants[1].autoAcceptOverride === false,
    'API Request payload ánh xạ đúng từng size riêng biệt M và L kèm price và autoAcceptOverride'
  );

  // =========================================================================
  // Test 3: Kiểm tra Validation lỗi Giá bán (Price Validation)
  // =========================================================================
  console.log('\n[Test 3] Kiểm tra lỗi Giá bán (Price Validation):');
  // Case 3a: Giá bằng 0
  const zeroPriceVar: VariantFormData = {
    ...sizeMVariant,
    id: 'var_zero_price',
    price: 0,
  };
  const valZeroPrice = validateProductCreateForm({
    ...productSizedForm,
    variants: [zeroPriceVar],
  });
  assert(
    !valZeroPrice.isValid &&
      valZeroPrice.errors.variantErrors['var_zero_price']?.price === 'Giá bán phải lớn hơn 0 VND',
    'Từ chối giá bán bằng 0 với thông điệp: "Giá bán phải lớn hơn 0 VND"'
  );

  // Case 3b: Giá âm
  const negativePriceVar: VariantFormData = {
    ...sizeMVariant,
    id: 'var_neg_price',
    price: -15000,
  };
  const valNegPrice = validateProductCreateForm({
    ...productSizedForm,
    variants: [negativePriceVar],
  });
  assert(
    !valNegPrice.isValid &&
      valNegPrice.errors.variantErrors['var_neg_price']?.price === 'Giá bán phải lớn hơn 0 VND',
    'Từ chối giá bán số âm với thông điệp: "Giá bán phải lớn hơn 0 VND"'
  );

  // Case 3c: Giá để trống
  const emptyPriceVar: VariantFormData = {
    ...sizeMVariant,
    id: 'var_empty_price',
    price: '',
  };
  const valEmptyPrice = validateProductCreateForm({
    ...productSizedForm,
    variants: [emptyPriceVar],
  });
  assert(
    !valEmptyPrice.isValid &&
      valEmptyPrice.errors.variantErrors['var_empty_price']?.price ===
        'Vui lòng nhập giá bán cho biến thể',
    'Từ chối giá để trống với thông điệp: "Vui lòng nhập giá bán cho biến thể"'
  );

  // =========================================================================
  // Test 4: Kiểm tra Validation lỗi Công suất hàng ngày (Capacity Validation)
  // =========================================================================
  console.log('\n[Test 4] Kiểm tra lỗi Công suất hàng ngày (Capacity Validation):');
  // Case 4a: Công suất âm
  const negativeCapVar: VariantFormData = {
    ...sizeMVariant,
    id: 'var_neg_cap',
    dailyCapacityDefault: -10,
  };
  const valNegCap = validateProductCreateForm({
    ...productSizedForm,
    variants: [negativeCapVar],
  });
  assert(
    !valNegCap.isValid &&
      valNegCap.errors.variantErrors['var_neg_cap']?.dailyCapacityDefault ===
        'Công suất phục vụ mỗi ngày không được là số âm',
    'Từ chối công suất âm với thông báo: "Công suất phục vụ mỗi ngày không được là số âm"'
  );

  // Case 4b: Công suất không phải số nguyên
  const decimalCapVar: VariantFormData = {
    ...sizeMVariant,
    id: 'var_decimal_cap',
    dailyCapacityDefault: 12.5,
  };
  const valDecimalCap = validateProductCreateForm({
    ...productSizedForm,
    variants: [decimalCapVar],
  });
  assert(
    !valDecimalCap.isValid &&
      valDecimalCap.errors.variantErrors['var_decimal_cap']?.dailyCapacityDefault ===
        'Công suất phải là số nguyên',
    'Từ chối công suất số thập phân: "Công suất phải là số nguyên"'
  );

  // Case 4c: Công suất bằng 0 hoặc để trống (hợp lệ)
  const zeroCapVar: VariantFormData = {
    ...sizeMVariant,
    id: 'var_zero_cap',
    dailyCapacityDefault: 0,
  };
  const valZeroCap = validateProductCreateForm({
    ...productSizedForm,
    variants: [zeroCapVar],
  });
  assert(valZeroCap.isValid, 'Công suất bằng 0 (hết suất phục vụ) là giá trị hợp lệ');

  // =========================================================================
  // Test 5: Kiểm tra Ngăn chặn Biến thể Trùng lặp (Duplicate Variant Prevention)
  // =========================================================================
  console.log('\n[Test 5] Kiểm tra ngăn chặn biến thể trùng lặp (Duplicate Prevention):');
  // Case 5a: Trùng 2 biến thể cùng Size M
  const dupSizeVar1: VariantFormData = {
    id: 'var_m_dup_1',
    name: 'Size M - Nhẹ',
    sizeType: 'M',
    price: 35000,
    inventoryMode: 'MADE_TO_ORDER',
    dailyCapacityDefault: 50,
    autoAcceptOverride: true,
    maxQuantityPerOrder: 10,
    available: true,
  };
  const dupSizeVar2: VariantFormData = {
    id: 'var_m_dup_2',
    name: 'Size M - Đậm',
    sizeType: 'M',
    price: 37000,
    inventoryMode: 'MADE_TO_ORDER',
    dailyCapacityDefault: 50,
    autoAcceptOverride: true,
    maxQuantityPerOrder: 10,
    available: true,
  };

  const valDupSize = validateProductCreateForm({
    ...productSizedForm,
    variants: [dupSizeVar1, dupSizeVar2],
  });
  assert(
    !valDupSize.isValid &&
      Boolean(valDupSize.errors.variantErrors['var_m_dup_2']?.duplicate?.includes('đã tồn tại')),
    'Phát hiện và chặn 2 biến thể cùng chọn kích cỡ Size M'
  );

  // Case 5b: Trùng 2 biến thể STANDARD
  const dupStd1 = createDefaultVariant('STANDARD', 30000);
  dupStd1.id = 'std_1';
  const dupStd2 = createDefaultVariant('STANDARD', 32000);
  dupStd2.id = 'std_2';

  const valDupStd = validateProductCreateForm({
    ...productSizedForm,
    variants: [dupStd1, dupStd2],
  });
  assert(
    !valDupStd.isValid &&
      Boolean(valDupStd.errors.variantErrors['std_2']?.duplicate?.includes('Đã có biến thể STANDARD')),
    'Phát hiện và chặn không cho tạo 2 biến thể cùng là STANDARD không kích cỡ'
  );

  // Case 5c: Trùng tên biến thể
  const dupNameVar1: VariantFormData = {
    id: 'name_dup_1',
    name: 'Đặc Biệt',
    sizeType: 'CUSTOM',
    customSize: 'DacBiet1',
    price: 40000,
    inventoryMode: 'MADE_TO_ORDER',
    dailyCapacityDefault: 50,
    autoAcceptOverride: true,
    maxQuantityPerOrder: 10,
    available: true,
  };
  const dupNameVar2: VariantFormData = {
    id: 'name_dup_2',
    name: 'Đặc Biệt', // Duplicate name
    sizeType: 'CUSTOM',
    customSize: 'DacBiet2',
    price: 45000,
    inventoryMode: 'MADE_TO_ORDER',
    dailyCapacityDefault: 50,
    autoAcceptOverride: true,
    maxQuantityPerOrder: 10,
    available: true,
  };
  const valDupName = validateProductCreateForm({
    ...productSizedForm,
    variants: [dupNameVar1, dupNameVar2],
  });
  assert(
    !valDupName.isValid &&
      Boolean(valDupName.errors.variantErrors['name_dup_2']?.duplicate?.includes('Tên biến thể')),
    'Phát hiện và chặn không cho tạo biến thể trùng tên nhau trong cùng món'
  );

  // =========================================================================
  // Test 6: Kiểm tra Validation Tên món ăn và Danh mục
  // =========================================================================
  console.log('\n[Test 6] Kiểm tra Validation Tên món ăn và Danh mục:');
  const emptyNameForm: ProductCreateFormData = {
    ...productStandardForm,
    name: '   ',
  };
  const valEmptyName = validateProductCreateForm(emptyNameForm);
  assert(
    !valEmptyName.isValid && valEmptyName.errors.name === 'Vui lòng nhập tên món ăn',
    'Tên món ăn để trống hoặc toàn dấu cách bị chặn: "Vui lòng nhập tên món ăn"'
  );

  const tooLongName = 'A'.repeat(151);
  const valTooLongName = validateProductCreateForm({
    ...productStandardForm,
    name: tooLongName,
  });
  assert(
    !valTooLongName.isValid &&
      valTooLongName.errors.name === 'Tên món ăn không được vượt quá 150 ký tự',
    'Tên món ăn vượt quá 150 ký tự bị chặn theo đúng giới hạn Spring Boot @Size(max=150)'
  );

  const noCategoryForm: ProductCreateFormData = {
    ...productStandardForm,
    storeCategoryId: 0,
  };
  const valNoCat = validateProductCreateForm(noCategoryForm);
  assert(
    !valNoCat.isValid &&
      valNoCat.errors.storeCategoryId === 'Vui lòng chọn danh mục cho món ăn',
    'Danh mục cửa hàng chưa chọn (id = 0) bị chặn bắt buộc nhập'
  );

  // Không có biến thể nào
  const noVariantsForm: ProductCreateFormData = {
    ...productStandardForm,
    variants: [],
  };
  const valNoVars = validateProductCreateForm(noVariantsForm);
  assert(
    !valNoVars.isValid &&
      valNoVars.errors.variants ===
        'Món ăn phải có ít nhất 1 biến thể (STANDARD hoặc có kích cỡ)',
    'Danh sách biến thể rỗng bị chặn: "Món ăn phải có ít nhất 1 biến thể"'
  );

  // =========================================================================
  // Test 7: Inventory Mode & Auto-accept Override Combination
  // =========================================================================
  console.log('\n[Test 7] Kiểm tra Chế độ Kho (MADE_TO_ORDER vs LIMITED_STOCK) & Auto-Accept:');
  const limitedStockVar: VariantFormData = {
    id: 'lim_1',
    name: 'STANDARD',
    sizeType: 'STANDARD',
    price: 55000,
    inventoryMode: 'LIMITED_STOCK',
    dailyCapacityDefault: '',
    autoAcceptOverride: false,
    maxQuantityPerOrder: 5,
    available: false,
  };

  const payloadLimited = transformToApiPayloads({
    ...productStandardForm,
    variants: [limitedStockVar],
  });

  assert(
    payloadLimited.variants[0].inventoryMode === 'LIMITED_STOCK' &&
      payloadLimited.variants[0].autoAcceptOverride === false &&
      payloadLimited.variants[0].dailyCapacityDefault === undefined &&
      payloadLimited.variants[0].maxQuantityPerOrder === 5,
    'LIMITED_STOCK với autoAcceptOverride: false và dailyCapacity: undefined chuẩn hóa chính xác'
  );

  console.log(`\n🎉 KẾT QUẢ: Hoàn thành ${passed}/${total} assertions test tạo món và biến thể thành công!`);
  return passed === total;
}

// Execute if run directly via Node/TSX
if (typeof process !== 'undefined' && process.argv && process.argv[1]?.includes('productCreateSmokeTest')) {
  runProductCreateSmokeTest();
}
