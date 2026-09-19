import { apiClient } from './client';
import {
  ProductCatalogDto,
  ProductVariantDto,
  CreateProductRequest,
  UpdateProductRequest,
  CreateProductVariantRequest,
  UpdateProductVariantRequest,
  ProductFilterCriteria,
  PaginationParams,
  PageResponse,
} from '@/types/api.types';

export const catalogApi = {
  /**
   * Fetch paginated products for a store with filtering and sorting
   */
  getProducts: async (
    storeId: number,
    params?: ProductFilterCriteria & PaginationParams
  ): Promise<PageResponse<ProductCatalogDto>> => {
    return apiClient.get<PageResponse<ProductCatalogDto>, PageResponse<ProductCatalogDto>>(
      `/api/v1/stores/${storeId}/products`,
      { params }
    );
  },

  /**
   * Fetch a single product by ID with its variants
   */
  getProductById: async (
    storeId: number,
    productId: number
  ): Promise<ProductCatalogDto> => {
    return apiClient.get<ProductCatalogDto, ProductCatalogDto>(
      `/api/v1/stores/${storeId}/products/${productId}`
    );
  },

  /**
   * Merchant: Create a new product in the store catalog
   */
  createProduct: async (
    storeId: number,
    payload: CreateProductRequest
  ): Promise<ProductCatalogDto> => {
    return apiClient.post<ProductCatalogDto, ProductCatalogDto>(
      `/api/v1/merchant/stores/${storeId}/products`,
      payload
    );
  },

  /**
   * Merchant: Update product details or active status (PATCH)
   */
  updateProduct: async (
    storeId: number,
    productId: number,
    payload: UpdateProductRequest
  ): Promise<ProductCatalogDto> => {
    return apiClient.patch<ProductCatalogDto, ProductCatalogDto>(
      `/api/v1/merchant/stores/${storeId}/products/${productId}`,
      payload
    );
  },

  /**
   * Merchant: Soft-delete / hide a product from catalog
   */
  deleteProduct: async (storeId: number, productId: number): Promise<void> => {
    return apiClient.delete<void, void>(
      `/api/v1/merchant/stores/${storeId}/products/${productId}`
    );
  },

  /**
   * Merchant: Create a variant (e.g. Size M, L or STANDARD) for a product
   */
  createProductVariant: async (
    storeId: number,
    productId: number,
    payload: CreateProductVariantRequest
  ): Promise<ProductVariantDto> => {
    return apiClient.post<ProductVariantDto, ProductVariantDto>(
      `/api/v1/merchant/stores/${storeId}/products/${productId}/variants`,
      payload
    );
  },

  /**
   * Merchant: Update variant details, price or capacity (PATCH)
   */
  updateProductVariant: async (
    storeId: number,
    productId: number,
    variantId: number,
    payload: UpdateProductVariantRequest
  ): Promise<ProductVariantDto> => {
    return apiClient.patch<ProductVariantDto, ProductVariantDto>(
      `/api/v1/merchant/stores/${storeId}/products/${productId}/variants/${variantId}`,
      payload
    );
  },

  /**
   * Merchant: Soft-delete a variant
   */
  deleteProductVariant: async (
    storeId: number,
    productId: number,
    variantId: number
  ): Promise<void> => {
    return apiClient.delete<void, void>(
      `/api/v1/merchant/stores/${storeId}/products/${productId}/variants/${variantId}`
    );
  },

  /**
   * Merchant: Create a new product along with all its variants in sequence
   */
  createProductWithVariants: async (
    storeId: number,
    productPayload: CreateProductRequest,
    variantsPayload: CreateProductVariantRequest[]
  ): Promise<ProductCatalogDto> => {
    // 1. Create base product
    const createdProduct = await catalogApi.createProduct(storeId, productPayload);

    // 2. Create each variant for this product
    const createdVariants: ProductVariantDto[] = [];
    for (const variantReq of variantsPayload) {
      const v = await catalogApi.createProductVariant(storeId, createdProduct.id, variantReq);
      createdVariants.push(v);
    }

    return {
      ...createdProduct,
      variants: createdVariants,
    };
  },

  /**
   * Alias for updateProduct using PATCH semantics
   */
  patchProduct: async (
    storeId: number,
    productId: number,
    payload: UpdateProductRequest
  ): Promise<ProductCatalogDto> => {
    return catalogApi.updateProduct(storeId, productId, payload);
  },

  /**
   * Alias for updateProductVariant using PATCH semantics
   */
  patchProductVariant: async (
    storeId: number,
    productId: number,
    variantId: number,
    payload: UpdateProductVariantRequest
  ): Promise<ProductVariantDto> => {
    return catalogApi.updateProductVariant(storeId, productId, variantId, payload);
  },

  /**
   * Merchant: Update category active status or details (PATCH)
   */
  updateCategory: async (
    storeId: number,
    categoryId: number,
    payload: { isActive?: boolean; name?: string }
  ): Promise<void> => {
    return apiClient.patch<void, void>(
      `/api/v1/merchant/stores/${storeId}/categories/${categoryId}`,
      payload
    );
  },
};
