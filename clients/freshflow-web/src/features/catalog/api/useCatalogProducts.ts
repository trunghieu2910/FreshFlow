import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { catalogApi } from '@/api';
import {
  PageResponse,
  ProductCatalogDto,
  ProductFilterCriteria,
} from '@/types/api.types';
import { ApiError } from '@/api/error';

export interface UseCatalogProductsParams {
  storeId: number;
  criteria?: ProductFilterCriteria;
  page?: number;
  size?: number;
  sort?: string;
  enabled?: boolean;
}

/**
 * Custom React Query hook for fetching paginated store catalog products from backend API
 */
export function useCatalogProducts({
  storeId,
  criteria,
  page = 0,
  size = 20,
  sort = 'name,asc',
  enabled = true,
}: UseCatalogProductsParams) {
  return useQuery<PageResponse<ProductCatalogDto>, ApiError>({
    queryKey: ['catalog', storeId, criteria, page, size, sort],
    queryFn: async (): Promise<PageResponse<ProductCatalogDto>> => {
      return catalogApi.getProducts(storeId, {
        ...criteria,
        page,
        size,
        sort,
      });
    },
    placeholderData: keepPreviousData,
    enabled: enabled && storeId > 0,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}
