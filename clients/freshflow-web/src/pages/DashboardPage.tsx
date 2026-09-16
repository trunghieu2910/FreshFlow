import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui';
import {
  DollarSign,
  ShoppingBag,
  UtensilsCrossed,
  TrendingUp,
  ArrowRight,
  Clock,
} from 'lucide-react';

export const DashboardPage: React.FC = () => {
  return (
    <div className="space-y-6">
      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">Doanh thu hôm nay</span>
            <div className="rounded-lg bg-emerald-50 p-2 text-emerald-600">
              <DollarSign className="h-5 w-5" aria-hidden="true" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold tracking-tight text-slate-900 tabular-nums">
              4.850.000đ
            </span>
            <span className="text-xs font-semibold text-emerald-600 flex items-center">
              <TrendingUp className="h-3.5 w-3.5 mr-0.5" aria-hidden="true" /> +12%
            </span>
          </div>
          <p className="mt-1 text-[11px] text-slate-400">So với cùng giờ ngày hôm qua</p>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">Đơn hàng cần xử lý</span>
            <div className="rounded-lg bg-blue-50 p-2 text-blue-600">
              <ShoppingBag className="h-5 w-5" aria-hidden="true" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold tracking-tight text-slate-900 tabular-nums">
              3 đơn mới
            </span>
          </div>
          <p className="mt-1 text-[11px] text-slate-400">2 đơn giao ngay • 1 đơn hẹn giờ</p>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">Món đang mở bán</span>
            <div className="rounded-lg bg-amber-50 p-2 text-amber-600">
              <UtensilsCrossed className="h-5 w-5" aria-hidden="true" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-2xl font-bold tracking-tight text-slate-900 tabular-nums">
              24 / 26 món
            </span>
          </div>
          <p className="mt-1 text-[11px] text-slate-400">2 món tạm hết công suất ngày</p>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-500">Tình trạng phục vụ</span>
            <div className="rounded-lg bg-emerald-50 p-2 text-emerald-600">
              <Clock className="h-5 w-5" aria-hidden="true" />
            </div>
          </div>
          <div className="mt-2 flex items-baseline gap-2">
            <span className="text-lg font-bold text-emerald-700">Bình thường</span>
          </div>
          <p className="mt-1 text-[11px] text-slate-400">Thời gian chuẩn bị TB: 8 phút</p>
        </div>
      </div>

      {/* Quick Navigation Cards */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-xs flex flex-col justify-between space-y-4">
          <div className="space-y-1">
            <h3 className="font-semibold text-base text-slate-900">Quản lý Thực đơn & Món ăn</h3>
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
            <h3 className="font-semibold text-base text-slate-900">Theo dõi Đơn hàng</h3>
            <p className="text-xs text-slate-500">
              Xử lý tiếp nhận đơn đặt hàng trực tuyến từ khách hàng và bàn giao cho tài xế giao nhận.
            </p>
          </div>
          <Link to="/orders">
            <Button
              variant="outline"
              size="sm"
              rightIcon={<ArrowRight className="h-4 w-4" aria-hidden="true" />}
            >
              Xem đơn hàng (3 đơn mới)
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
};
