import React from 'react';
import { ProductCatalogDto } from '@/types/api.types';
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
import { formatVND, getCapacityStatusInfo } from '../utils/formatters';
import { Utensils, Edit3, Trash2, Eye, EyeOff } from 'lucide-react';

export interface ProductCatalogTableProps {
  products: ProductCatalogDto[];
  onEdit?: (product: ProductCatalogDto) => void;
  onDelete?: (productId: number) => void;
  onToggleActive?: (product: ProductCatalogDto) => void;
}

export const ProductCatalogTable: React.FC<ProductCatalogTableProps> = ({
  products,
  onEdit,
  onDelete,
  onToggleActive,
}) => {
  return (
    <Table className="min-w-[720px]">
      <TableHeader>
        <TableRow>
          <TableHead className="w-16">ID</TableHead>
          <TableHead>Món ăn</TableHead>
          <TableHead>Biến thể kích cỡ & Giá bán</TableHead>
          <TableHead>Công suất hôm nay</TableHead>
          <TableHead>Trạng thái</TableHead>
          <TableHead className="text-right">Thao tác</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {products.map((product) => {
          // Check overall product availability from variants
          const primaryVariant = product.variants[0];
          const hasExhaustedVariant = product.variants.some(
            (v) => v.availabilityStatus === 'CAPACITY_EXHAUSTED'
          );
          const isFullyUnavailable =
            !product.active || product.variants.every((v) => !v.available);

          const statusInfo = !product.active
            ? { label: 'Đã ẩn mềm (is_active: false)', variant: 'danger' as const }
            : getCapacityStatusInfo(
                isFullyUnavailable
                  ? 'MARKED_UNAVAILABLE'
                  : hasExhaustedVariant
                  ? 'CAPACITY_EXHAUSTED'
                  : primaryVariant?.availabilityStatus || 'AVAILABLE'
              );

          return (
            <TableRow
              key={product.id}
              className={!product.active ? 'bg-slate-50/75 opacity-80' : ''}
            >
              {/* Product ID */}
              <TableCell className="font-mono text-xs text-slate-500 font-semibold">
                #{product.id}
              </TableCell>

              {/* Product Thumbnail & Name */}
              <TableCell>
                <div className="flex items-center gap-3 min-w-0">
                  <div className="h-10 w-10 shrink-0 rounded-xl bg-slate-100 border border-slate-200 overflow-hidden flex items-center justify-center text-slate-400">
                    {product.imageUrl ? (
                      <img
                        src={product.imageUrl}
                        alt={product.name}
                        className="h-full w-full object-cover"
                        loading="lazy"
                        onError={(e) => {
                          // Fallback to placeholder icon on image load error
                          (e.target as HTMLImageElement).style.display = 'none';
                        }}
                      />
                    ) : (
                      <Utensils className="h-5 w-5" aria-hidden="true" />
                    )}
                  </div>

                  <div className="min-w-0">
                    <div className="flex items-center gap-1.5">
                      <span className="font-semibold text-slate-900 text-sm truncate">
                        {product.name}
                      </span>
                      {!product.active && (
                        <span className="text-[10px] bg-rose-100 text-rose-800 px-1.5 py-0.2 rounded font-bold">
                          Ẩn
                        </span>
                      )}
                    </div>
                    {product.description && (
                      <span className="block text-xs text-slate-500 truncate max-w-xs sm:max-w-sm">
                        {product.description}
                      </span>
                    )}
                  </div>
                </div>
              </TableCell>

              {/* Variants and VND Prices */}
              <TableCell>
                <div className="flex flex-wrap gap-1.5 max-w-xs">
                  {product.variants.length > 0 ? (
                    product.variants.map((v) => (
                      <span
                        key={v.id}
                        className={`inline-flex items-center gap-1 rounded-md border px-2 py-0.5 text-xs font-mono ${
                          !v.active
                            ? 'bg-slate-200/70 border-slate-300 text-slate-500 line-through'
                            : !v.available
                            ? 'bg-amber-50 border-amber-300 text-amber-900'
                            : 'bg-slate-100 border-slate-200/80 text-slate-700'
                        }`}
                        title={
                          !v.active
                            ? 'Biến thể đã ẩn mềm'
                            : !v.available
                            ? 'Biến thể tạm ngưng bán'
                            : 'Đang mở bán'
                        }
                      >
                        <span className="font-semibold text-slate-900">
                          {v.size ? `Size ${v.size}` : 'STANDARD'}:
                        </span>
                        <span className="text-emerald-700 font-bold">{formatVND(v.price)}</span>
                        {!v.available && v.active && (
                          <span className="text-[9px] text-amber-800 font-bold">(Ngưng)</span>
                        )}
                      </span>
                    ))
                  ) : (
                    <span className="text-xs text-slate-500 italic">Chưa có biến thể</span>
                  )}
                </div>
              </TableCell>

              {/* Daily Capacity Snapshot */}
              <TableCell>
                {primaryVariant?.capacity ? (
                  <div className="space-y-0.5">
                    <span className="block text-xs font-semibold text-slate-800 tabular-nums">
                      Còn {primaryVariant.capacity.remaining} suất
                    </span>
                    <span className="block text-[10px] text-slate-500">
                      Định mức: {primaryVariant.dailyCapacityDefault || 100} / ngày
                    </span>
                  </div>
                ) : (
                  <span className="text-xs text-slate-500 font-mono">
                    {primaryVariant?.inventoryMode === 'MADE_TO_ORDER'
                      ? 'Nấu theo order'
                      : 'Kho giới hạn'}
                  </span>
                )}
              </TableCell>

              {/* Status Badge */}
              <TableCell>
                <Badge variant={statusInfo.variant} dot size="sm">
                  {statusInfo.label}
                </Badge>
              </TableCell>

              {/* Actions */}
              <TableCell className="text-right">
                <div className="inline-flex items-center gap-1.5 justify-end">
                  {/* Quick Toggle Active Soft Hide */}
                  <Button
                    size="sm"
                    variant="ghost"
                    className={
                      product.active
                        ? 'text-slate-600 hover:text-rose-600'
                        : 'text-emerald-600 hover:text-emerald-700'
                    }
                    onClick={() => onToggleActive?.(product)}
                    aria-label={
                      product.active
                        ? `Ẩn mềm món ${product.name}`
                        : `Mở lại món ${product.name}`
                    }
                    title={
                      product.active
                        ? 'Ẩn mềm món ăn khỏi thực đơn công khai'
                        : 'Mở lại món ăn trên thực đơn'
                    }
                  >
                    {product.active ? (
                      <EyeOff className="h-3.5 w-3.5" aria-hidden="true" />
                    ) : (
                      <Eye className="h-3.5 w-3.5" aria-hidden="true" />
                    )}
                  </Button>

                  <Button
                    size="sm"
                    variant="outline"
                    leftIcon={<Edit3 className="h-3.5 w-3.5" aria-hidden="true" />}
                    onClick={() => onEdit?.(product)}
                    aria-label={`Sửa món ${product.name}`}
                  >
                    Sửa
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="text-rose-600 hover:text-rose-700 hover:bg-rose-50"
                    onClick={() => onDelete?.(product.id)}
                    aria-label={`Xóa món ${product.name}`}
                    title="Ẩn mềm món ăn khỏi thực đơn (Không hard delete)"
                  >
                    <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </Table>
  );
};
