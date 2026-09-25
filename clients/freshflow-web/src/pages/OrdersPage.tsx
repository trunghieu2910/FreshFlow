import React, { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
  Badge,
  Button,
  Pagination,
  EmptyState,
  ErrorState,
  Skeleton,
} from '@/components/ui';
import { useMerchantOrders } from '@/features/orders/api/useMerchantOrders';
import { OrderDetailModal } from '@/features/orders/components/OrderDetailModal';
import { OrderStatus } from '@/types/api.types';
import {
  ShoppingBag,
  Eye,
  RotateCw,
  Clock,
  Filter,
} from 'lucide-react';

const STATUS_TABS: { label: string; value: string }[] = [
  { label: 'Tất cả đơn', value: 'ALL' },
  { label: 'Chờ duyệt', value: 'AWAITING_MERCHANT_CONFIRMATION' },
  { label: 'Đang pha chế', value: 'PROCESSING' },
  { label: 'Đang giao', value: 'SHIPPING' },
  { label: 'Hoàn thành', value: 'COMPLETED' },
  { label: 'Đã hủy', value: 'CANCELLED' },
];

export const OrdersPage: React.FC = () => {
  const storeId = 1;
  const [searchParams, setSearchParams] = useSearchParams();

  // Read URL query params with defaults
  const currentStatus = searchParams.get('status') || 'ALL';
  const currentPage = parseInt(searchParams.get('page') || '1', 10);
  const pageSize = 10;

  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);

  const { data, isLoading, isError, error, refetch, isFetching } = useMerchantOrders({
    storeId,
    status: currentStatus,
    page: currentPage - 1, // backend 0-indexed
    size: pageSize,
  });

  const handleStatusChange = (status: string) => {
    const params = new URLSearchParams(searchParams);
    if (status === 'ALL') {
      params.delete('status');
    } else {
      params.set('status', status);
    }
    params.set('page', '1'); // reset to page 1 on filter change
    setSearchParams(params);
  };

  const handlePageChange = (page: number) => {
    const params = new URLSearchParams(searchParams);
    params.set('page', page.toString());
    setSearchParams(params);
  };

  const openOrderDetail = (orderId: number) => {
    setSelectedOrderId(orderId);
    setIsDetailOpen(true);
  };

  const formatVnd = (amount: number) => {
    return new Intl.NumberFormat('vi-VN').format(amount) + '\u00A0đ';
  };

  const formatRelativeTime = (isoString: string) => {
    try {
      const date = new Date(isoString);
      return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    } catch {
      return isoString;
    }
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

  return (
    <div className="space-y-6">
      {/* Overview Banner */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-600 border border-blue-100">
            <ShoppingBag className="h-6 w-6" aria-hidden="true" />
          </div>
          <div>
            <h1 className="text-lg font-bold tracking-tight text-slate-900" style={{ textWrap: 'balance' }}>
              Quản lý Đơn hàng
            </h1>
            <p className="text-xs text-slate-500">
              Tổng số <strong className="text-slate-800 tabular-nums">{data?.totalElements ?? 0} đơn hàng</strong> trong hệ thống.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isFetching}
            aria-label="Làm mới danh sách đơn"
            leftIcon={
              <RotateCw
                className={`h-4 w-4 ${isFetching ? 'animate-spin text-blue-600' : 'text-slate-500'}`}
                aria-hidden="true"
              />
            }
          >
            {isFetching ? 'Đang cập nhật…' : 'Làm mới'}
          </Button>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-slate-200" role="tablist">
        <span className="text-xs font-semibold text-slate-400 mr-1 flex items-center gap-1 shrink-0">
          <Filter className="h-3.5 w-3.5" aria-hidden="true" /> Lọc trạng thái:
        </span>
        {STATUS_TABS.map((tab) => {
          const isActive = currentStatus === tab.value;
          return (
            <button
              key={tab.value}
              type="button"
              role="tab"
              aria-selected={isActive}
              onClick={() => handleStatusChange(tab.value)}
              className={`rounded-xl px-3.5 py-1.5 text-xs font-semibold transition shrink-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 ${
                isActive
                  ? 'bg-blue-600 text-white shadow-xs'
                  : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
              }`}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Error State */}
      {isError && (
        <ErrorState
          title="Không thể tải danh sách đơn hàng"
          message={error?.message || 'Vui lòng kiểm tra lại kết nối máy chủ backend.'}
          onRetry={() => refetch()}
        />
      )}

      {/* Loading Skeletons */}
      {isLoading && !data && (
        <div className="space-y-3 bg-white p-5 rounded-2xl border border-slate-200">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="flex justify-between items-center py-2.5">
              <Skeleton className="h-4 w-28" />
              <Skeleton className="h-4 w-40" />
              <Skeleton className="h-4 w-48" />
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-6 w-16 rounded-full" />
              <Skeleton className="h-8 w-12 rounded-lg" />
            </div>
          ))}
        </div>
      )}

      {/* Orders Table */}
      {data && data.content.length > 0 && (
        <div className="space-y-4">
          <div
            className="overflow-x-auto rounded-2xl border border-slate-200 bg-white shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600"
            role="region"
            tabIndex={0}
            aria-label="Danh sách đơn hàng của quán"
          >
            <Table className="min-w-[760px]">
              <TableHeader>
                <TableRow>
                  <TableHead className="w-28">Mã đơn</TableHead>
                  <TableHead>Khách hàng</TableHead>
                  <TableHead>Chi tiết món</TableHead>
                  <TableHead>Tổng tiền</TableHead>
                  <TableHead>Thời gian</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Thao tác</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((order) => (
                  <TableRow
                    key={order.id}
                    className="hover:bg-slate-50/80 cursor-pointer transition"
                    onClick={() => openOrderDetail(order.id)}
                  >
                    <TableCell className="font-mono text-xs font-bold text-slate-800">
                      {order.orderNumber}
                    </TableCell>
                    <TableCell>
                      <div className="font-semibold text-slate-900 text-xs">{order.customerName}</div>
                      <div className="text-[11px] text-slate-400 font-mono">{order.customerPhoneMasked}</div>
                    </TableCell>
                    <TableCell className="text-xs text-slate-600 max-w-xs truncate">
                      {order.itemsSummary}
                    </TableCell>
                    <TableCell className="font-bold text-xs text-slate-900 tabular-nums">
                      {formatVnd(order.totalAmount)}
                    </TableCell>
                    <TableCell className="text-xs text-slate-500 font-mono">
                      <div className="flex items-center gap-1">
                        <Clock className="h-3 w-3 text-slate-400" aria-hidden="true" />
                        <span>{formatRelativeTime(order.createdAt)}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant={getBadgeVariant(order.status)} dot>
                        {order.statusLabel}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right" onClick={(e) => e.stopPropagation()}>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => openOrderDetail(order.id)}
                        aria-label={`Xem chi tiết đơn hàng ${order.orderNumber}`}
                        leftIcon={<Eye className="h-3.5 w-3.5" aria-hidden="true" />}
                      >
                        Chi tiết
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          {/* Pagination */}
          {data.totalPages > 1 && (
            <div className="pt-2">
              <Pagination
                currentPage={currentPage}
                totalPages={data.totalPages}
                totalElements={data.totalElements}
                pageSize={pageSize}
                onPageChange={handlePageChange}
              />
            </div>
          )}
        </div>
      )}

      {/* Empty State */}
      {data && data.content.length === 0 && (
        <EmptyState
          title="Không tìm thấy đơn hàng nào"
          description={
            currentStatus !== 'ALL'
              ? 'Hiện tại không có đơn hàng nào ở trạng thái đã chọn.'
              : 'Cửa hàng hiện chưa có đơn hàng nào được ghi nhận.'
          }
          variant="search"
          actionLabel={currentStatus !== 'ALL' ? 'Xem tất cả đơn' : undefined}
          onAction={currentStatus !== 'ALL' ? () => handleStatusChange('ALL') : undefined}
        />
      )}

      {/* Order Detail Modal */}
      <OrderDetailModal
        orderId={selectedOrderId}
        storeId={storeId}
        isOpen={isDetailOpen}
        onClose={() => {
          setIsDetailOpen(false);
          setSelectedOrderId(null);
        }}
      />
    </div>
  );
};
