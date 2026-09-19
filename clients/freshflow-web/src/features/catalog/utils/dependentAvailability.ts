import {
  ProductCatalogDto,
  StoreCategoryDto,
  CartItem,
  EvaluatedCartItem,
} from '@/types/api.types';

export interface AvailabilityContext {
  categories: StoreCategoryDto[];
  products: ProductCatalogDto[];
}

export interface CartValidationResult {
  evaluatedItems: EvaluatedCartItem[];
  canCheckout: boolean;
  unavailableCount: number;
  errorMessages: string[];
}

/**
 * Check dependent availability for an item in a cart or order:
 * An item is ONLY available if:
 * 1. The store category is active (Category.isActive === true)
 * 2. The product is active (Product.active === true)
 * 3. The variant is active (Variant.active === true)
 * 4. The variant is marked available (Variant.available === true)
 */
export function checkItemAvailability(
  item: CartItem,
  context: AvailabilityContext
): { isAvailable: boolean; unavailableReason?: string } {
  // 1. Check Store Category status
  const category = context.categories.find((c) => c.id === item.storeCategoryId);
  if (category && !category.isActive) {
    return {
      isAvailable: false,
      unavailableReason: `Danh mục "${category.name}" hiện đang tạm đóng.`,
    };
  }

  // 2. Check Product status
  const product = context.products.find((p) => p.id === item.productId);
  if (!product) {
    return {
      isAvailable: false,
      unavailableReason: 'Món ăn không còn tồn tại trong hệ thống.',
    };
  }

  if (!product.active) {
    return {
      isAvailable: false,
      unavailableReason: `Món "${product.name}" hiện đang tạm ẩn khỏi thực đơn.`,
    };
  }

  // 3. Check Variant status
  const variant = product.variants.find((v) => v.id === item.variantId);
  if (!variant) {
    return {
      isAvailable: false,
      unavailableReason: 'Biến thể món ăn không còn tồn tại.',
    };
  }

  if (!variant.active) {
    return {
      isAvailable: false,
      unavailableReason: `Biến thể "${variant.name}" đã ngừng kinh doanh.`,
    };
  }

  if (!variant.available) {
    return {
      isAvailable: false,
      unavailableReason:
        variant.availabilityStatus === 'CAPACITY_EXHAUSTED'
          ? `Biến thể "${variant.name}" đã hết công suất phục vụ hôm nay.`
          : `Biến thể "${variant.name}" tạm hết hàng.`,
    };
  }

  return { isAvailable: true };
}

/**
 * Filter catalog products according to dependent availability rules:
 * - When activeOnly is true (Public Catalog), excludes:
 *   1. Products whose store category is inactive
 *   2. Products whose active flag is false
 *   3. Variants whose active flag is false
 */
export function filterCatalogProducts(
  products: (ProductCatalogDto & { storeCategoryId?: number })[],
  categories: StoreCategoryDto[],
  activeOnly: boolean = true
): ProductCatalogDto[] {
  if (!activeOnly) {
    return products;
  }

  const activeCategoryIds = new Set(
    categories.filter((c) => c.isActive).map((c) => c.id)
  );

  return products
    .filter((p) => {
      // Must be active
      if (!p.active) return false;
      // If product belongs to an inactive category, hide from catalog
      if (p.storeCategoryId && !activeCategoryIds.has(p.storeCategoryId)) {
        return false;
      }
      return true;
    })
    .map((p) => ({
      ...p,
      // Filter out soft-deleted variants
      variants: p.variants.filter((v) => v.active),
    }))
    .filter((p) => p.variants.length > 0);
}

/**
 * Filter products by category and active status strictly
 */
export function filterProductsByCategoryActive(
  products: (ProductCatalogDto & { storeCategoryId?: number })[],
  categories: StoreCategoryDto[],
  activeOnly: boolean = true
): ProductCatalogDto[] {
  const activeCategoryIds = new Set(
    categories.filter((c) => c.isActive).map((c) => c.id)
  );

  return products
    .filter((p) => {
      if (activeOnly) {
        if (!p.active) return false;
        if (p.storeCategoryId && !activeCategoryIds.has(p.storeCategoryId)) {
          return false;
        }
      }
      return true;
    })
    .map((p) => ({
      ...p,
      variants: activeOnly ? p.variants.filter((v) => v.active) : p.variants,
    }))
    .filter((p) => (activeOnly ? p.variants.length > 0 : true));
}

/**
 * Validate customer cart for checkout:
 * - Evaluates every cart item against current category/product/variant status
 * - BLOCKS checkout (canCheckout = false) if any item is unavailable
 * - NEVER deletes items from cart automatically
 */
export function validateCartForCheckout(
  cartItems: CartItem[],
  context: AvailabilityContext
): CartValidationResult {
  const evaluatedItems: EvaluatedCartItem[] = [];
  const errorMessages: string[] = [];
  let unavailableCount = 0;

  for (const item of cartItems) {
    const { isAvailable, unavailableReason } = checkItemAvailability(item, context);
    if (!isAvailable) {
      unavailableCount++;
      if (unavailableReason && !errorMessages.includes(unavailableReason)) {
        errorMessages.push(unavailableReason);
      }
    }
    evaluatedItems.push({
      ...item,
      isAvailable,
      unavailableReason,
    });
  }

  const canCheckout = unavailableCount === 0 && evaluatedItems.length > 0;

  return {
    evaluatedItems,
    canCheckout,
    unavailableCount,
    errorMessages,
  };
}
