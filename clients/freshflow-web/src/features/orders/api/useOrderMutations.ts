import { useMutation, useQueryClient } from '@tanstack/react-query';
import { orderApi } from '@/api';
import { useToast } from '@/components/ui';

export function useOrderMutations(storeId: number) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['merchant-orders'] });
    queryClient.invalidateQueries({ queryKey: ['merchant-dashboard-summary'] });
    queryClient.invalidateQueries({ queryKey: ['merchant-order-detail'] });
  };

  const acceptMutation = useMutation({
    mutationFn: (orderId: number) => orderApi.acceptOrder(storeId, orderId),
    onSuccess: (data) => {
      invalidate();
      toast.success(`Đã nhận đơn hàng #${data.orderNumber} (${data.statusLabel})`);
    },
    onError: (error: Error) => {
      toast.error(error?.message || 'Không thể nhận đơn hàng, vui lòng thử lại sau.');
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ orderId, reason }: { orderId: number; reason?: string }) =>
      orderApi.rejectOrder(storeId, orderId, reason),
    onSuccess: (data) => {
      invalidate();
      toast.info(`Đã từ chối đơn hàng #${data.orderNumber}. Đã hoàn trả công suất.`);
    },
    onError: (error: Error) => {
      toast.error(error?.message || 'Không thể từ chối đơn hàng, vui lòng thử lại sau.');
    },
  });

  const prepareMutation = useMutation({
    mutationFn: (orderId: number) => orderApi.prepareOrder(storeId, orderId),
    onSuccess: (data) => {
      invalidate();
      toast.success(`Đơn hàng #${data.orderNumber} chuyển sang đang pha chế.`);
    },
    onError: (error: Error) => {
      toast.error(error?.message || 'Không thể cập nhật trạng thái làm món.');
    },
  });

  const dispatchMutation = useMutation({
    mutationFn: (orderId: number) => orderApi.dispatchOrder(storeId, orderId),
    onSuccess: (data) => {
      invalidate();
      toast.success(`Đơn hàng #${data.orderNumber} đã bàn giao cho tài xế.`);
    },
    onError: (error: Error) => {
      toast.error(error?.message || 'Không thể bàn giao đơn hàng.');
    },
  });

  return {
    acceptOrder: acceptMutation.mutateAsync,
    isAccepting: acceptMutation.isPending,
    rejectOrder: rejectMutation.mutateAsync,
    isRejecting: rejectMutation.isPending,
    prepareOrder: prepareMutation.mutateAsync,
    isPreparing: prepareMutation.isPending,
    dispatchOrder: dispatchMutation.mutateAsync,
    isDispatching: dispatchMutation.isPending,
  };
}
