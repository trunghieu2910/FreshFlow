import { useQuery } from '@tanstack/react-query';
import { orderApi } from '@/api';
import { MerchantDashboardSummary } from '@/types/api.types';
import { ApiError } from '@/api/error';

export function useMerchantDashboard(storeId: number) {
  return useQuery<MerchantDashboardSummary, ApiError>({
    queryKey: ['merchant-dashboard-summary', storeId],
    queryFn: async (): Promise<MerchantDashboardSummary> => {
      return orderApi.getDashboardSummary(storeId);
    },
    enabled: storeId > 0,
    staleTime: 30 * 1000, // 30 seconds
    refetchInterval: 60 * 1000, // auto refresh every minute
  });
}
