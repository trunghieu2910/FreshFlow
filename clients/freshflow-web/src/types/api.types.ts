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
