import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { orderApi } from '@/api';
import { MerchantOrderSummary, PageResponse } from '@/types/api.types';
import { ApiError } from '@/api/error';

export interface UseMerchantOrdersParams {
  storeId: number;
  status?: string;
  page?: number;
  size?: number;
  enabled?: boolean;
}

export function useMerchantOrders({
  storeId,
  status,
  page = 0,
  size = 10,
  enabled = true,
}: UseMerchantOrdersParams) {
  return useQuery<PageResponse<MerchantOrderSummary>, ApiError>({
    queryKey: ['merchant-orders', storeId, status, page, size],
    queryFn: async (): Promise<PageResponse<MerchantOrderSummary>> => {
      return orderApi.getOrders(storeId, {
        status: status && status !== 'ALL' ? status : undefined,
        page,
        size,
      });
    },
    placeholderData: keepPreviousData,
    enabled: enabled && storeId > 0,
    staleTime: 15 * 1000,
  });
}
