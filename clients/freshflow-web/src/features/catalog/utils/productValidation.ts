import {
  ProductCreateFormData,
  VariantFormData,
  CreateProductRequest,
  CreateProductVariantRequest,
} from '@/types/api.types';

export interface VariantFieldError {
  name?: string;
  size?: string;
  price?: string;
  dailyCapacityDefault?: string;
  maxQuantityPerOrder?: string;
  duplicate?: string;
}

export interface ProductFormValidationResult {
  isValid: boolean;
  errors: {
    name?: string;
    storeCategoryId?: string;
    description?: string;
    imageUrl?: string;
    variants?: string;
    variantErrors: Record<string, VariantFieldError>;
  };
}

/**
 * Resolve effective size string or null based on sizeType
 * STANDARD variant MUST have size: null as required by Spring Boot CatalogVariantService
 */
export function resolveVariantSize(variant: VariantFormData): string | null {
  if (variant.sizeType === 'STANDARD') {
    return null;
  }
  if (variant.sizeType === 'CUSTOM') {
    return variant.customSize?.trim() || null;
  }
  return variant.sizeType;
}

/**
 * Resolve display or API name for a variant
 */
export function resolveVariantName(
  variant: VariantFormData,
  productName?: string
): string {
  if (variant.name && variant.name.trim()) {
    return variant.name.trim();
  }
  if (variant.sizeType === 'STANDARD') {
    return 'STANDARD';
  }
  const size = resolveVariantSize(variant);
  if (size) {
    return productName && productName.trim()
      ? `${productName.trim()} - Size ${size}`
      : `Size ${size}`;
  }
  return 'Biến thể';
}

/**
 * Comprehensive validation engine for Product & Variant creation form
 */
export function validateProductCreateForm(
  formData: ProductCreateFormData
): ProductFormValidationResult {
  const errors: ProductFormValidationResult['errors'] = {
    variantErrors: {},
  };
  let isValid = true;

  // 1. Validate Product Name
  if (!formData.name || !formData.name.trim()) {
    errors.name = 'Vui lòng nhập tên món ăn';
    isValid = false;
  } else if (formData.name.trim().length > 150) {
    errors.name = 'Tên món ăn không được vượt quá 150 ký tự';
    isValid = false;
  }

  // 2. Validate Store Category
  if (!formData.storeCategoryId || formData.storeCategoryId <= 0) {
    errors.storeCategoryId = 'Vui lòng chọn danh mục cho món ăn';
    isValid = false;
  }

  // 3. Validate Description (optional)
  if (formData.description && formData.description.length > 2000) {
    errors.description = 'Mô tả món ăn không được vượt quá 2000 ký tự';
    isValid = false;
  }

  // 4. Validate Image URL (optional)
  if (formData.imageUrl && formData.imageUrl.trim()) {
    const imgUrl = formData.imageUrl.trim();
    if (imgUrl.length > 500) {
      errors.imageUrl = 'Đường dẫn ảnh không được vượt quá 500 ký tự';
      isValid = false;
    } else if (!/^https?:\/\/.+/i.test(imgUrl)) {
      errors.imageUrl = 'Đường dẫn ảnh phải bắt đầu bằng http:// hoặc https://';
      isValid = false;
    }
  }

  // 5. Validate Variants Presence
  if (!formData.variants || formData.variants.length === 0) {
    errors.variants = 'Món ăn phải có ít nhất 1 biến thể (STANDARD hoặc có kích cỡ)';
    isValid = false;
    return { isValid: false, errors };
  }

  // Track unique sizes and names to detect duplicates
  // Note: STANDARD resolves to null, so we key null as '__STANDARD__'
  const seenSizes = new Map<string, string>(); // sizeKey -> variantId
  const seenNames = new Map<string, string>(); // nameKey -> variantId

  for (const variant of formData.variants) {
    const vErrors: VariantFieldError = {};
    const resolvedSize = resolveVariantSize(variant);
    const resolvedName = resolveVariantName(variant, formData.name);

    // Validate Name
    if (!resolvedName) {
      vErrors.name = 'Tên biến thể không được để trống';
    } else if (resolvedName.length > 80) {
      vErrors.name = 'Tên biến thể không được vượt quá 80 ký tự';
    }

    // Validate Sized vs STANDARD convention
    if (variant.sizeType === 'CUSTOM' && !variant.customSize?.trim()) {
      vErrors.size = 'Vui lòng nhập tên kích cỡ tùy chỉnh';
    }

    // Validate Price
    if (variant.price === '' || variant.price === undefined || variant.price === null) {
      vErrors.price = 'Vui lòng nhập giá bán cho biến thể';
    } else {
      const priceNum = Number(variant.price);
      if (isNaN(priceNum) || priceNum <= 0) {
        vErrors.price = 'Giá bán phải lớn hơn 0 VND';
      }
    }

    // Validate Daily Capacity Default (optional for LIMITED_STOCK, recommended for MADE_TO_ORDER)
    if (
      variant.dailyCapacityDefault !== '' &&
      variant.dailyCapacityDefault !== undefined &&
      variant.dailyCapacityDefault !== null
    ) {
      const capNum = Number(variant.dailyCapacityDefault);
      if (isNaN(capNum) || capNum < 0) {
        vErrors.dailyCapacityDefault = 'Công suất phục vụ mỗi ngày không được là số âm';
      } else if (!Number.isInteger(capNum)) {
        vErrors.dailyCapacityDefault = 'Công suất phải là số nguyên';
      }
    }

    // Validate Max Quantity Per Order
    if (
      variant.maxQuantityPerOrder !== '' &&
      variant.maxQuantityPerOrder !== undefined &&
      variant.maxQuantityPerOrder !== null
    ) {
      const maxQty = Number(variant.maxQuantityPerOrder);
      if (isNaN(maxQty) || maxQty <= 0) {
        vErrors.maxQuantityPerOrder = 'Số lượng tối đa mỗi đơn phải lớn hơn 0';
      }
    }

    // Duplicate Variant Checking (Size and Name)
    const sizeKey = resolvedSize === null ? '__STANDARD__' : resolvedSize.trim().toUpperCase();
    if (seenSizes.has(sizeKey)) {
      vErrors.duplicate =
        resolvedSize === null
          ? 'Đã có biến thể STANDARD. Không thể tạo 2 biến thể cùng loại không kích cỡ.'
          : `Kích cỡ '${resolvedSize}' đã tồn tại. Không được tạo biến thể trùng kích cỡ.`;
    } else {
      seenSizes.set(sizeKey, variant.id);
    }

    const nameKey = resolvedName.toLowerCase();
    if (seenNames.has(nameKey) && !vErrors.duplicate) {
      vErrors.duplicate = `Tên biến thể '${resolvedName}' đã tồn tại trong món này.`;
    } else {
      seenNames.set(nameKey, variant.id);
    }

    if (Object.keys(vErrors).length > 0) {
      errors.variantErrors[variant.id] = vErrors;
      isValid = false;
    }
  }

  return { isValid, errors };
}

/**
 * Transform UI Form Data into backend Spring Boot requests
 */
export function transformToApiPayloads(formData: ProductCreateFormData): {
  product: CreateProductRequest;
  variants: CreateProductVariantRequest[];
} {
  const product: CreateProductRequest = {
    storeCategoryId: formData.storeCategoryId,
    name: formData.name.trim(),
    description: formData.description?.trim() || undefined,
    imageUrl: formData.imageUrl?.trim() || undefined,
    active: formData.active,
  };

  const variants: CreateProductVariantRequest[] = formData.variants.map((v) => {
    const resolvedSize = resolveVariantSize(v);
    const resolvedName = resolveVariantName(v, formData.name);
    return {
      name: resolvedName,
      size: resolvedSize, // strictly null for STANDARD
      price: Number(v.price),
      inventoryMode: v.inventoryMode,
      autoAcceptOverride: Boolean(v.autoAcceptOverride),
      maxQuantityPerOrder:
        v.maxQuantityPerOrder !== '' && v.maxQuantityPerOrder !== undefined
          ? Number(v.maxQuantityPerOrder)
          : 10,
      dailyCapacityDefault:
        v.dailyCapacityDefault !== '' &&
        v.dailyCapacityDefault !== undefined &&
        v.dailyCapacityDefault !== null
          ? Number(v.dailyCapacityDefault)
          : undefined,
    };
  });

  return { product, variants };
}

/**
 * Helper to generate empty default variant
 */
export function createDefaultVariant(
  sizeType: VariantFormData['sizeType'] = 'STANDARD',
  defaultPrice: number | '' = 35000,
  inventoryMode: VariantFormData['inventoryMode'] = 'MADE_TO_ORDER'
): VariantFormData {
  return {
    id: `var_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
    name: sizeType === 'STANDARD' ? 'STANDARD' : `Size ${sizeType}`,
    sizeType,
    customSize: '',
    price: defaultPrice,
    inventoryMode,
    dailyCapacityDefault: inventoryMode === 'MADE_TO_ORDER' ? 100 : '',
    autoAcceptOverride: true,
    maxQuantityPerOrder: 10,
    available: true,
  };
}
