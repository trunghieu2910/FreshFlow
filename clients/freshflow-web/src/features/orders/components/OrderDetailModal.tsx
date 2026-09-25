import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { orderApi } from '@/api';
import { Modal, Button, Badge, Skeleton, ErrorState } from '@/components/ui';
import { useOrderMutations } from '../api/useOrderMutations';
import {
  ShoppingBag,
  CreditCard,
  User,
  Phone,
  Clock,
  CheckCircle2,
  XCircle,
  ChefHat,
  Truck,
  AlertTriangle,
} from 'lucide-react';
import { OrderStatus } from '@/types/api.types';

interface OrderDetailModalProps {
  orderId: number | null;
  storeId?: number;
  isOpen: boolean;
  onClose: () => void;
}

export const OrderDetailModal: React.FC<OrderDetailModalProps> = ({
  orderId,
  storeId = 1,
  isOpen,
  onClose,
}) => {
  const [rejectReason, setRejectReason] = useState('Hết nguyên liệu chế biến');
  const [showRejectPrompt, setShowRejectPrompt] = useState(false);

  const { data: order, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['merchant-order-detail', storeId, orderId],
    queryFn: () => orderApi.getOrderDetail(storeId, orderId!),
    enabled: isOpen && orderId !== null && orderId > 0,
  });

  const {
    acceptOrder,
    isAccepting,
    rejectOrder,
    isRejecting,
    prepareOrder,
    isPreparing,
    dispatchOrder,
    isDispatching,
  } = useOrderMutations(storeId);

  const formatVnd = (amount: number) => {
    return new Intl.NumberFormat('vi-VN').format(amount) + '\u00A0đ';
  };

  const getBadgeVariant = (status: OrderStatus) => {
    switch (status) {
      case 'AWAITING_MERCHANT_CONFIRMATION':
      case 'AWAITING_PAYMENT':
        return 'warning' as const;
      case 'PENDING':
      case 'PROCESSING':
        return 'info' as const;
      case 'SHIPPING':
      case 'COMPLETED':
        return 'success' as const;
      case 'CANCELLED':
      case 'DELIVERY_FAILED':
      case 'DISPUTED':
        return 'danger' as const;
      default:
        return 'default' as const;
    }
  };

  const handleAccept = async () => {
    if (!orderId) return;
    await acceptOrder(orderId);
    refetch();
  };

  const handleReject = async () => {
    if (!orderId) return;
    await rejectOrder({ orderId, reason: rejectReason });
    setShowRejectPrompt(false);
    refetch();
  };

  const handlePrepare = async () => {
    if (!orderId) return;
    await prepareOrder(orderId);
    refetch();
  };

  const handleDispatch = async () => {
    if (!orderId) return;
    await dispatchOrder(orderId);
    refetch();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={order ? `Chi tiết Đơn hàng #${order.orderNumber}` : 'Chi tiết Đơn hàng'}
      size="lg"
    >
      <div className="space-y-6">
        {isLoading && (
          <div className="space-y-4 py-4">
            <Skeleton className="h-6 w-48" />
            <Skeleton className="h-20 w-full rounded-xl" />
            <Skeleton className="h-36 w-full rounded-xl" />
          </div>
        )}

        {isError && (
          <ErrorState
            title="Không thể tải chi tiết đơn hàng"
            message={error?.message || 'Vui lòng thử lại sau.'}
            onRetry={() => refetch()}
          />
        )}

        {order && (
          <>
            {/* Status & Timing Banner */}
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-slate-50 p-4 rounded-xl border border-slate-200">
              <div className="flex items-center gap-2">
                <Badge variant={getBadgeVariant(order.status)} dot>
                  {order.statusLabel}
                </Badge>
                <span className="text-xs text-slate-500 font-mono">
                  {order.paymentMethod}
                </span>
              </div>
              <div className="flex items-center gap-1.5 text-xs text-slate-500">
                <Clock className="h-3.5 w-3.5 text-slate-400" aria-hidden="true" />
                <span>Đặt lúc: {new Date(order.createdAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</span>
              </div>
            </div>

            {/* Customer & Address Info */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="rounded-xl border border-slate-200 p-4 space-y-2 bg-white">
                <h3 className="text-xs font-semibold text-slate-500 flex items-center gap-1.5">
                  <User className="h-4 w-4 text-slate-400" aria-hidden="true" /> Khách hàng
                </h3>
                <div className="text-sm font-bold text-slate-900">{order.customerName}</div>
                <div className="text-xs font-mono text-slate-500 flex items-center gap-1">
                  <Phone className="h-3.5 w-3.5 text-slate-400" aria-hidden="true" /> {order.customerPhoneMasked}
                </div>
              </div>

              <div className="rounded-xl border border-slate-200 p-4 space-y-2 bg-white">
                <h3 className="text-xs font-semibold text-slate-500 flex items-center gap-1.5">
                  <CreditCard className="h-4 w-4 text-slate-400" aria-hidden="true" /> Thanh toán
                </h3>
                <div className="text-sm font-bold text-slate-900 tabular-nums">
                  {formatVnd(order.totalAmount)}
                </div>
                <p className="text-[11px] text-slate-400">
                  {order.paymentMethod === 'ONLINE_MOCK'
                    ? 'Thanh toán online qua ứng dụng'
                    : 'Thu tiền khi giao hàng (COD)'}
                </p>
              </div>
            </div>

            {/* Order Items Table */}
            <div className="rounded-xl border border-slate-200 overflow-hidden">
              <div className="bg-slate-50 px-4 py-2.5 border-b border-slate-200 text-xs font-semibold text-slate-600 flex items-center gap-2">
                <ShoppingBag className="h-4 w-4 text-slate-500" aria-hidden="true" />
                <span>Danh sách món trong đơn ({order.items.length} món)</span>
              </div>
              <div className="divide-y divide-slate-100">
                {order.items.map((item) => (
                  <div key={item.id} className="p-4 flex items-center justify-between gap-4">
                    <div className="min-w-0">
                      <div className="text-sm font-semibold text-slate-900 truncate">
                        {item.productName}
                      </div>
                      <div className="text-xs text-slate-500">
                        Biến thể: <span className="font-medium text-slate-700">{item.variantName}</span>
                      </div>
                    </div>
                    <div className="text-right shrink-0">
                      <div className="text-xs text-slate-400 tabular-nums">
                        {item.quantity} x {formatVnd(item.unitPrice)}
                      </div>
                      <div className="text-sm font-bold text-slate-900 tabular-nums">
                        {formatVnd(item.lineTotal)}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
              <div className="bg-slate-50 p-4 border-t border-slate-200 flex justify-between items-center text-sm font-bold text-slate-900">
                <span>Tổng cộng</span>
                <span className="tabular-nums text-emerald-700 text-base">{formatVnd(order.totalAmount)}</span>
              </div>
            </div>

            {/* Cancellation reason if cancelled */}
            {order.status === 'CANCELLED' && order.cancelReason && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-start gap-2">
                <AlertTriangle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" aria-hidden="true" />
                <div>
                  <span className="font-bold">Lý do từ chối / hủy đơn: </span>
                  {order.cancelReason}
                </div>
              </div>
            )}

            {/* Reject Reason Sub-prompt */}
            {showRejectPrompt && (
              <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl space-y-3 animate-fadeIn">
                <h4 className="text-xs font-bold text-amber-900">Chọn lý do từ chối nhận đơn:</h4>
                <div className="space-y-1.5">
                  {['Hết nguyên liệu chế biến', 'Bếp đang quá tải đơn', 'Ngoài khung giờ phục vụ', 'Khách yêu cầu hủy'].map(
                    (reason) => (
                      <label key={reason} className="flex items-center gap-2 text-xs text-amber-950 cursor-pointer">
                        <input
                          type="radio"
                          name="rejectReason"
                          checked={rejectReason === reason}
                          onChange={() => setRejectReason(reason)}
                          className="text-amber-600 focus:ring-amber-500"
                        />
                        <span>{reason}</span>
                      </label>
                    )
                  )}
                </div>
                <div className="flex gap-2 justify-end pt-2">
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setShowRejectPrompt(false)}
                    disabled={isRejecting}
                  >
                    Hủy bỏ
                  </Button>
                  <Button
                    size="sm"
                    variant="danger"
                    onClick={handleReject}
                    disabled={isRejecting}
                  >
                    {isRejecting ? 'Đang từ chối…' : 'Xác nhận từ chối'}
                  </Button>
                </div>
              </div>
            )}

            {/* Workflow Action Buttons */}
            <div className="flex flex-wrap items-center justify-end gap-3 pt-3 border-t border-slate-200">
              <Button variant="ghost" onClick={onClose}>
                Đóng
              </Button>

              {order.status === 'AWAITING_MERCHANT_CONFIRMATION' && !showRejectPrompt && (
                <>
                  <Button
                    variant="danger"
                    onClick={() => setShowRejectPrompt(true)}
                    disabled={isAccepting || isRejecting}
                    leftIcon={<XCircle className="h-4 w-4" aria-hidden="true" />}
                  >
                    Từ chối đơn
                  </Button>
                  <Button
                    variant="primary"
                    onClick={handleAccept}
                    disabled={isAccepting || isRejecting}
                    leftIcon={<CheckCircle2 className="h-4 w-4" aria-hidden="true" />}
                  >
                    {isAccepting ? 'Đang nhận đơn…' : 'Nhận đơn ngay'}
                  </Button>
                </>
              )}

              {order.status === 'PENDING' && (
                <Button
                  variant="primary"
                  onClick={handlePrepare}
                  disabled={isPreparing}
                  leftIcon={<ChefHat className="h-4 w-4" aria-hidden="true" />}
                >
                  {isPreparing ? 'Đang cập nhật…' : 'Bắt đầu làm món'}
                </Button>
              )}

              {order.status === 'PROCESSING' && (
                <Button
                  variant="primary"
                  onClick={handleDispatch}
                  disabled={isDispatching}
                  leftIcon={<Truck className="h-4 w-4" aria-hidden="true" />}
                >
                  {isDispatching ? 'Đang bàn giao…' : 'Giao cho tài xế'}
                </Button>
              )}
            </div>
          </>
        )}
      </div>
    </Modal>
  );
};
