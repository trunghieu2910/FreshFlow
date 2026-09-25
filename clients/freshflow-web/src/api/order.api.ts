import { apiClient } from './client';
import {
  MerchantDashboardSummary,
  MerchantOrderSummary,
  MerchantOrderDetail,
  PageResponse,
} from '@/types/api.types';

export const orderApi = {
  /**
   * Merchant: Fetch real KPI metrics for store dashboard
   */
  getDashboardSummary: async (storeId: number): Promise<MerchantDashboardSummary> => {
    return apiClient.get<MerchantDashboardSummary, MerchantDashboardSummary>(
      `/api/v1/merchant/stores/${storeId}/dashboard/summary`
    );
  },

  /**
   * Merchant: Fetch paginated orders with status filtering
   */
  getOrders: async (
    storeId: number,
    params?: { status?: string; page?: number; size?: number }
  ): Promise<PageResponse<MerchantOrderSummary>> => {
    return apiClient.get<
      PageResponse<MerchantOrderSummary>,
      PageResponse<MerchantOrderSummary>
    >(`/api/v1/merchant/stores/${storeId}/orders`, { params });
  },

  /**
   * Merchant: Fetch full order details by ID
   */
  getOrderDetail: async (
    storeId: number,
    orderId: number
  ): Promise<MerchantOrderDetail> => {
    return apiClient.get<MerchantOrderDetail, MerchantOrderDetail>(
      `/api/v1/merchant/stores/${storeId}/orders/${orderId}`
    );
  },

  /**
   * Merchant: Accept an incoming order
   */
  acceptOrder: async (
    storeId: number,
    orderId: number
  ): Promise<MerchantOrderDetail> => {
    return apiClient.post<MerchantOrderDetail, MerchantOrderDetail>(
      `/api/v1/merchant/stores/${storeId}/orders/${orderId}/accept`
    );
  },

  /**
   * Merchant: Reject an incoming order with reason
   */
  rejectOrder: async (
    storeId: number,
    orderId: number,
    reason?: string
  ): Promise<MerchantOrderDetail> => {
    return apiClient.post<MerchantOrderDetail, MerchantOrderDetail>(
      `/api/v1/merchant/stores/${storeId}/orders/${orderId}/reject`,
      { reason }
    );
  },

  /**
   * Merchant: Start preparing/cooking an order
   */
  prepareOrder: async (
    storeId: number,
    orderId: number
  ): Promise<MerchantOrderDetail> => {
    return apiClient.post<MerchantOrderDetail, MerchantOrderDetail>(
      `/api/v1/merchant/stores/${storeId}/orders/${orderId}/prepare`
    );
  },

  /**
   * Merchant: Hand off order to driver (dispatch)
   */
  dispatchOrder: async (
    storeId: number,
    orderId: number
  ): Promise<MerchantOrderDetail> => {
    return apiClient.post<MerchantOrderDetail, MerchantOrderDetail>(
      `/api/v1/merchant/stores/${storeId}/orders/${orderId}/dispatch`
    );
  },
};
