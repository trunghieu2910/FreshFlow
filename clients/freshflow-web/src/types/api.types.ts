/**
 * Spring Boot Page representation matching Spring Data Pageable responses
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first?: boolean;
  last?: boolean;
  empty?: boolean;
}

/**
 * Field-level validation error detail matching Spring Boot ApiErrorResponse.FieldError
 */
export interface FieldError {
  field: string;
  message: string;
}

/**
 * Standard FreshFlow API error payload matching backend Spring Boot ApiErrorResponse
 */
export interface ApiErrorResponse {
  code: string;
  message: string;
  path?: string;
  timestamp?: string;
  fieldErrors?: FieldError[];
}

/**
 * Inventory mode: MADE_TO_ORDER (kitchen preparation) or LIMITED_STOCK (physical inventory)
 */
export type InventoryMode = 'MADE_TO_ORDER' | 'LIMITED_STOCK';

/**
 * Availability status for product variants
 */
export type AvailabilityStatus =
  | 'AVAILABLE'
  | 'MARKED_UNAVAILABLE'
  | 'CAPACITY_EXHAUSTED'
  | 'CAPACITY_NOT_CONFIGURED';

/**
 * Daily capacity snapshot for made-to-order kitchen items
 */
export interface CapacityDto {
  capacityDate: string;
  remaining: number;
}

/**
 * Product variant details including pricing, size, inventory mode, and capacity
 */
export interface ProductVariantDto {
  id: number;
  name: string;
  size: string | null;
  price: number;
  inventoryMode: InventoryMode;
  autoAcceptOverride?: boolean | null;
  maxQuantityPerOrder?: number | null;
  dailyCapacityDefault?: number | null;
  available: boolean;
  active: boolean;
  availabilityStatus: AvailabilityStatus;
  capacity?: CapacityDto | null;
}

/**
 * Product catalog item representation with available variants
 */
export interface ProductCatalogDto {
  id: number;
  name: string;
  description: string | null;
  imageUrl: string | null;
  active: boolean;
  variants: ProductVariantDto[];
}

/**
 * Request payload for creating a new catalog product
 */
export interface CreateProductRequest {
  storeCategoryId: number;
  name: string;
  description?: string;
  imageUrl?: string;
  active?: boolean;
}

/**
 * Request payload for updating an existing product
 */
export interface UpdateProductRequest {
  name?: string;
  description?: string;
  imageUrl?: string;
  active?: boolean;
}

/**
 * Request payload for creating a new product variant
 */
export interface CreateProductVariantRequest {
  name: string;
  size?: string | null;
  price: number;
  inventoryMode: InventoryMode;
  autoAcceptOverride?: boolean;
  maxQuantityPerOrder?: number;
  dailyCapacityDefault?: number;
  available?: boolean;
}

/**
 * Request payload for updating a product variant
 */
export interface UpdateProductVariantRequest {
  name?: string;
  price?: number;
  inventoryMode?: InventoryMode;
  autoAcceptOverride?: boolean;
  maxQuantityPerOrder?: number;
  dailyCapacityDefault?: number;
  active?: boolean;
  available?: boolean;
}

/**
 * Filter criteria for querying catalog products
 */
export interface ProductFilterCriteria {
  search?: string;
  storeCategoryId?: number;
  variantSize?: string;
  inventoryMode?: InventoryMode;
  availableOnly?: boolean;
}

/**
 * Standard pagination and sorting query parameters
 */
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
}

/**
 * Size selector options for product variants
 */
export type VariantSizeType = 'STANDARD' | 'M' | 'L' | 'CUSTOM';

/**
 * Form state model for an individual product variant
 */
export interface VariantFormData {
  id: string; // client-side unique id for key mapping
  name: string;
  sizeType: VariantSizeType;
  customSize?: string;
  price: number | '';
  inventoryMode: InventoryMode;
  autoAcceptOverride: boolean;
  maxQuantityPerOrder: number | '';
  dailyCapacityDefault: number | '';
  available: boolean;
}

/**
 * Form state model for creating a product with nested variants
 */
export interface ProductCreateFormData {
  storeCategoryId: number;
  name: string;
  description: string;
  imageUrl: string;
  active: boolean;
  variants: VariantFormData[];
}

/**
 * Store category representation with soft active status
 */
export interface StoreCategoryDto {
  id: number;
  storeId: number;
  name: string;
  description?: string | null;
  isActive: boolean;
  displayOrder: number;
}

/**
 * Form state model for editing an existing product and its variants (PATCH semantics)
 */
export interface ProductEditFormData {
  id: number;
  storeCategoryId: number;
  name: string;
  description: string;
  imageUrl: string;
  active: boolean;
  variants: (VariantFormData & {
    variantId?: number;
    active: boolean; // soft delete / hide flag for variant
  })[];
}

/**
 * Cart item model for tracking customer cart with dependent availability
 */
export interface CartItem {
  id: string;
  productId: number;
  productName: string;
  variantId: number;
  variantName: string;
  size: string | null;
  price: number;
  quantity: number;
  storeCategoryId: number;
  imageUrl?: string | null;
}

/**
 * Evaluated cart item with dependent availability status
 */
export interface EvaluatedCartItem extends CartItem {
  isAvailable: boolean;
  unavailableReason?: string;
}

/**
 * Cart state model
 */
export interface CartState {
  items: CartItem[];
  storeId: number;
}

/**
 * Historical order item model for verifying soft delete preservation
 */
export interface OrderHistoryItem {
  orderId: string;
  orderTime: string;
  productId: number;
  productName: string;
  variantId: number;
  variantName: string;
  size: string | null;
  unitPrice: number;
  quantity: number;
  totalPrice: number;
  storeCategoryId: number;
}


