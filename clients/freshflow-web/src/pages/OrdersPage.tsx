import React from 'react';
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
  Badge,
  Button,
} from '@/components/ui';
import { ShoppingBag, Eye, CheckCircle, Clock } from 'lucide-react';

export const OrdersPage: React.FC = () => {
  const sampleOrders = [
    {
      id: 'ORD-2026-001',
      customer: 'Nguyễn Văn An',
      phone: '0901***456',
      items: '2x Trà Đào Cam Sả (L), 1x Bánh Mì Chảo',
      total: '145.000đ',
      time: '11:24 (10 phút trước)',
      status: 'AWAITING_MERCHANT_CONFIRMATION',
      statusLabel: 'Chờ quán xác nhận',
      badgeVariant: 'warning' as const,
    },
    {
      id: 'ORD-2026-002',
      customer: 'Lê Hoàng Yến',
      phone: '0912***789',
      items: '1x Cà Phê Sữa Đá Sài Gòn',
      total: '28.000đ',
      time: '11:15 (19 phút trước)',
      status: 'PREPARING',
      statusLabel: 'Đang pha chế',
      badgeVariant: 'info' as const,
    },
    {
      id: 'ORD-2026-003',
      customer: 'Phạm Đức Thắng',
      phone: '0988***321',
      items: '3x Trà Ô Long Sen Vàng (M)',
      total: '120.000đ',
      time: '11:02 (32 phút trước)',
      status: 'READY_FOR_PICKUP',
      statusLabel: 'Chờ tài xế lấy',
      badgeVariant: 'success' as const,
    },
  ];

  return (
    <div className="space-y-6">
      {/* Overview Banner */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-600 border border-blue-100">
            <ShoppingBag className="h-5 w-5" aria-hidden="true" />
          </div>
          <div>
            <h2 className="text-base font-semibold text-slate-900">Danh sách Đơn hàng Mới</h2>
            <p className="text-xs text-slate-500">
              Có <strong>3 đơn hàng</strong> đang trong luồng xử lý hôm nay.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Badge variant="warning" dot>
            Tự động duyệt: BẬT
          </Badge>
        </div>
      </div>

      {/* Orders Table */}
      <Table>
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
          {sampleOrders.map((order) => (
            <TableRow key={order.id}>
              <TableCell className="font-mono text-xs font-bold text-slate-700">
                {order.id}
              </TableCell>
              <TableCell>
                <div className="font-medium text-slate-900 text-xs">{order.customer}</div>
                <div className="text-[11px] text-slate-400 font-mono">{order.phone}</div>
              </TableCell>
              <TableCell className="text-xs text-slate-600 max-w-xs truncate">
                {order.items}
              </TableCell>
              <TableCell className="font-bold text-xs text-slate-900 tabular-nums">
                {order.total}
              </TableCell>
              <TableCell className="text-xs text-slate-500 flex items-center gap-1">
                <Clock className="h-3 w-3 text-slate-400 shrink-0" aria-hidden="true" />
                <span>{order.time}</span>
              </TableCell>
              <TableCell>
                <Badge variant={order.badgeVariant} dot size="sm">
                  {order.statusLabel}
                </Badge>
              </TableCell>
              <TableCell className="text-right">
                <div className="inline-flex items-center gap-1.5 justify-end">
                  <Button
                    size="sm"
                    variant="primary"
                    leftIcon={<CheckCircle className="h-3.5 w-3.5" aria-hidden="true" />}
                  >
                    Xác nhận
                  </Button>
                  <Button size="sm" variant="outline" aria-label="Xem chi tiết">
                    <Eye className="h-3.5 w-3.5" aria-hidden="true" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};
