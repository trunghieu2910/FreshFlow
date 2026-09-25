import React from 'react';
import { Link } from 'react-router-dom';
import { Button, Skeleton, ErrorState } from '@/components/ui';
import { useMerchantDashboard } from '@/features/orders/api/useMerchantDashboard';
import {
  DollarSign,
  ShoppingBag,
  UtensilsCrossed,
  ArrowRight,
  Clock,
  RotateCw,
  Sparkles,
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const storeId = 1; // Default merchant store id in MVP
  const { data, isLoading, isError, error, refetch, isFetching } = useMerchantDashboard(storeId);

  const formatVnd = (amount: number) => {
    return new Intl.NumberFormat('vi-VN').format(amount) + '\u00A0đ';
  };

  return (
    <div className="space-y-6">
      {/* Top Banner with Real-time Refresh Action */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-lg font-bold tracking-tight text-slate-900" style={{ textWrap: 'balance' }}>
              Bảng điều khiển Cửa hàng
            </h1>
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold text-emerald-700 border border-emerald-200">
              <Sparkles className="h-3 w-3" aria-hidden="true" /> Số liệu trực tiếp
            </span>
          </div>
          <p className="mt-1 text-xs text-slate-500">
            Tổng quan hiệu suất bán hàng, trạng thái chế biến và thực đơn món trong ngày hôm nay.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={() => refetch()}
            disabled={isFetching}
            aria-label="Làm mới số liệu"
            leftIcon={
              <RotateCw
                className={`h-4 w-4 ${isFetching ? 'animate-spin text-emerald-600' : 'text-slate-500'}`}
                aria-hidden="true"
              />
            }
          >
            {isFetching ? 'Đang làm mới…' : 'Làm mới số liệu'}
          </Button>
        </div>
      </div>

      {/* Error State */}
      {isError && (
        <ErrorState
          title="Không thể tải số liệu bảng điều khiển"
          message={error?.message || 'Vui lòng kiểm tra lại kết nối máy chủ backend.'}
          onRetry={() => refetch()}
        />
      )}

      {/* Loading Skeletons */}
      {isLoading && !data && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs space-y-3">
              <div className="flex justify-between items-center">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-8 w-8 rounded-lg" />
              </div>
              <Skeleton className="h-8 w-32" />
              <Skeleton className="h-3 w-20" />
            </div>
          ))}
        </div>
      )}

      {/* Real KPI Cards Grid */}
      {data && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {/* Card 1: Today Revenue */}
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs transition hover:border-slate-300">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500">Doanh thu hôm nay</span>
              <div className="rounded-lg bg-emerald-50 p-2 text-emerald-600">
                <DollarSign className="h-5 w-5" aria-hidden="true" />
              </div>
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold tracking-tight text-slate-900 tabular-nums">
                {formatVnd(data.todayRevenue)}
              </span>
            </div>
            <p className="mt-1 text-[11px] text-slate-400">
              Tổng từ <strong className="text-slate-600 tabular-nums">{data.todayOrdersCount}</strong> đơn đặt hôm nay
            </p>
          </div>

          {/* Card 2: Pending Orders */}
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs transition hover:border-slate-300">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500">Đơn hàng cần xử lý</span>
              <div className={`rounded-lg p-2 ${data.pendingOrdersCount > 0 ? 'bg-amber-50 text-amber-600' : 'bg-blue-50 text-blue-600'}`}>
                <ShoppingBag className="h-5 w-5" aria-hidden="true" />
              </div>
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold tracking-tight text-slate-900 tabular-nums">
                {data.pendingOrdersCount} đơn mới
              </span>
              {data.pendingOrdersCount > 0 && (
                <span className="rounded-md bg-amber-100 px-1.5 py-0.5 text-[10px] font-bold text-amber-800">
                  Cần duyệt ngay
                </span>
              )}
            </div>
            <p className="mt-1 text-[11px] text-slate-400">Đang chờ quán xác nhận nhận đơn</p>
          </div>

          {/* Card 3: Active Products */}
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs transition hover:border-slate-300">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500">Món đang mở bán</span>
              <div className="rounded-lg bg-amber-50 p-2 text-amber-600">
                <UtensilsCrossed className="h-5 w-5" aria-hidden="true" />
              </div>
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-2xl font-bold tracking-tight text-slate-900 tabular-nums">
                {data.activeProductsCount} / {data.totalProductsCount} món
              </span>
            </div>
            <p className="mt-1 text-[11px] text-slate-400">
              {data.totalProductsCount - data.activeProductsCount} món đang tạm ngưng phục vụ
            </p>
          </div>

          {/* Card 4: Operation Status */}
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs transition hover:border-slate-300">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-500">Tình trạng phục vụ</span>
              <div className="rounded-lg bg-emerald-50 p-2 text-emerald-600">
                <Clock className="h-5 w-5" aria-hidden="true" />
              </div>
            </div>
            <div className="mt-2 flex items-baseline gap-2">
              <span className="text-lg font-bold text-emerald-700">
                {data.operationalStatus === 'NORMAL' ? 'Bình thường' : 'Quá tải bếp'}
              </span>
            </div>
            <p className="mt-1 text-[11px] text-slate-400">
              Thời gian chuẩn bị TB: <span className="tabular-nums font-semibold text-slate-600">{data.avgPreparationMinutes}</span> phút
            </p>
          </div>
        </div>
      )}

      {/* Quick Navigation Cards */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-xs flex flex-col justify-between space-y-4">
          <div className="space-y-1">
            <h2 className="font-semibold text-base text-slate-900" style={{ textWrap: 'balance' }}>
              Quản lý Thực đơn & Món ăn
            </h2>
            <p className="text-xs text-slate-500">
              Xem bảng danh sách món, điều chỉnh giá biến thể, kích cỡ M/L/STANDARD và định mức công
              suất bán trong ngày.
            </p>
          </div>
          <Link to="/products">
            <Button
              variant="primary"
              size="sm"
              rightIcon={<ArrowRight className="h-4 w-4" aria-hidden="true" />}
            >
              Xem danh sách thực đơn
            </Button>
          </Link>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-xs flex flex-col justify-between space-y-4">
          <div className="space-y-1">
            <h2 className="font-semibold text-base text-slate-900" style={{ textWrap: 'balance' }}>
              Theo dõi Đơn hàng
            </h2>
            <p className="text-xs text-slate-500">
              Xử lý tiếp nhận đơn đặt hàng trực tuyến từ khách hàng, duyệt đơn và bàn giao cho tài xế giao nhận.
            </p>
          </div>
          <Link to="/orders">
            <Button
              variant="outline"
              size="sm"
              rightIcon={<ArrowRight className="h-4 w-4" aria-hidden="true" />}
            >
              Xem đơn hàng {data ? `(${data.pendingOrdersCount} đơn mới)` : ''}
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
};
