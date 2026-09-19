import React, { useState } from 'react';
import { Modal, Button, Badge } from '@/components/ui';
import {
  ProductCatalogDto,
  StoreCategoryDto,
  CartItem,
  OrderHistoryItem,
} from '@/types/api.types';
import {
  validateCartForCheckout,
  AvailabilityContext,
} from '../utils/dependentAvailability';
import { formatVND } from '../utils/formatters';
import {
  ShoppingCart,
  History,
  AlertTriangle,
  CheckCircle,
  Ban,
  Clock,
  ShieldCheck,
} from 'lucide-react';

export interface CartPreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  categories: StoreCategoryDto[];
  products: ProductCatalogDto[];
  onToggleCategory?: (categoryId: number) => void;
  onToggleProduct?: (productId: number) => void;
}

const SAMPLE_CART_ITEMS: CartItem[] = [
  {
    id: 'cart_1',
    productId: 1,
    productName: 'Trà Đào Cam Sả',
    variantId: 10,
    variantName: 'Trà Đào Cam Sả - Size M',
    size: 'M',
    price: 35000,
    quantity: 2,
    storeCategoryId: 1,
  },
  {
    id: 'cart_2',
    productId: 3,
    productName: 'Bánh Mì Chảo Đặc Biệt',
    variantId: 30,
    variantName: 'Bánh Mì Chảo - STANDARD',
    size: null,
    price: 55000,
    quantity: 1,
    storeCategoryId: 4, // Belongs to Category 4 (Bánh mì & Món ăn nhẹ)
  },
  {
    id: 'cart_3',
    productId: 2,
    productName: 'Cà Phê Sữa Đá Sài Gòn',
    variantId: 20,
    variantName: 'Cà Phê Sữa Đá - STANDARD',
    size: null,
    price: 28000,
    quantity: 1,
    storeCategoryId: 1,
  },
];

const SAMPLE_ORDER_HISTORY: OrderHistoryItem[] = [
  {
    orderId: 'ORD-2026-089',
    orderTime: '2026-09-18 10:30',
    productId: 3,
    productName: 'Bánh Mì Chảo Đặc Biệt',
    variantId: 30,
    variantName: 'Bánh Mì Chảo - STANDARD',
    size: null,
    unitPrice: 55000,
    quantity: 2,
    totalPrice: 110000,
    storeCategoryId: 4,
  },
  {
    orderId: 'ORD-2026-088',
    orderTime: '2026-09-17 15:45',
    productId: 1,
    productName: 'Trà Đào Cam Sả',
    variantId: 11,
    variantName: 'Trà Đào Cam Sả - Size L',
    size: 'L',
    unitPrice: 45000,
    quantity: 1,
    totalPrice: 45000,
    storeCategoryId: 1,
  },
];

export const CartPreviewModal: React.FC<CartPreviewModalProps> = ({
  isOpen,
  onClose,
  categories,
  products,
  onToggleCategory,
  onToggleProduct,
}) => {
  const [activeTab, setActiveTab] = useState<'cart' | 'history'>('cart');
  const [cartItems, setCartItems] = useState<CartItem[]>(SAMPLE_CART_ITEMS);

  // Evaluate cart items against current categories & products
  const context: AvailabilityContext = { categories, products };
  const validation = validateCartForCheckout(cartItems, context);

  const cartTotal = cartItems.reduce((sum, item) => sum + item.price * item.quantity, 0);

  const handleRemoveCartItem = (id: string) => {
    setCartItems((prev) => prev.filter((item) => item.id !== id));
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Mô phỏng Dependent Availability: Giỏ hàng & Lịch sử Đơn"
      description="Kiểm chứng quy tắc bảo toàn dữ liệu: khi Category/Product/Variant bị ẩn mềm, giỏ hàng giữ nguyên nhưng chặn checkout; lịch sử đơn hàng bảo lưu 100%."
      size="xl"
    >
      <div className="space-y-4">
        {/* Navigation Tabs */}
        <div className="flex items-center gap-2 border-b border-slate-200 pb-2">
          <button
            type="button"
            onClick={() => setActiveTab('cart')}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeTab === 'cart'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 bg-slate-100'
            }`}
          >
            <ShoppingCart className="h-4 w-4" />
            <span>Giỏ hàng Khách hàng ({cartItems.length} món)</span>
            {validation.unavailableCount > 0 && (
              <span className="ml-1 px-1.5 py-0.2 rounded-full bg-rose-500 text-white text-[10px]">
                {validation.unavailableCount} lỗi
              </span>
            )}
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('history')}
            className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeTab === 'history'
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 bg-slate-100'
            }`}
          >
            <History className="h-4 w-4" />
            <span>Lịch sử Đơn hàng (Đã mua trước đó)</span>
          </button>
        </div>

        {/* TAB 1: CART SIMULATION */}
        {activeTab === 'cart' && (
          <div className="space-y-4">
            {/* Quick Status Bar for Categories */}
            <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
              <span className="text-xs font-bold text-slate-800 block">
                Trạng thái Danh mục cha (Bấm để thử nghiệm Dependent Availability):
              </span>
              <div className="flex flex-wrap gap-2">
                {categories.map((cat) => (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => onToggleCategory?.(cat.id)}
                    className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold border transition-all ${
                      cat.isActive
                        ? 'bg-white border-emerald-300 text-emerald-800 shadow-2xs hover:bg-emerald-50'
                        : 'bg-rose-50 border-rose-300 text-rose-800 line-through'
                    }`}
                    title="Bấm để bật/tắt Category này"
                  >
                    <span
                      className={`w-2 h-2 rounded-full ${
                        cat.isActive ? 'bg-emerald-500' : 'bg-rose-500'
                      }`}
                    />
                    <span>{cat.name}</span>
                    <span className="text-[10px] font-mono">
                      ({cat.isActive ? 'Mở' : 'Ẩn'})
                    </span>
                  </button>
                ))}
              </div>

              <span className="text-xs font-bold text-slate-800 block pt-1.5 border-t border-slate-200/60">
                Trạng thái Món ăn (Bấm để ẩn/hiện món trong giỏ):
              </span>
              <div className="flex flex-wrap gap-2">
                {products.slice(0, 4).map((prod) => (
                  <button
                    key={prod.id}
                    type="button"
                    onClick={() => onToggleProduct?.(prod.id)}
                    className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold border transition-all ${
                      prod.active
                        ? 'bg-white border-blue-300 text-blue-800 hover:bg-blue-50'
                        : 'bg-rose-50 border-rose-300 text-rose-800 line-through'
                    }`}
                    title="Bấm để ẩn/hiện món này"
                  >
                    <span
                      className={`w-2 h-2 rounded-full ${
                        prod.active ? 'bg-blue-500' : 'bg-rose-500'
                      }`}
                    />
                    <span>{prod.name}</span>
                    <span className="text-[10px] font-mono">
                      ({prod.active ? 'Hiện' : 'Ẩn'})
                    </span>
                  </button>
                ))}
              </div>
            </div>

            {/* Cart Items List */}
            <div className="space-y-2.5">
              {validation.evaluatedItems.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-400">
                  Giỏ hàng hiện đang trống.
                </div>
              ) : (
                validation.evaluatedItems.map((item) => (
                  <div
                    key={item.id}
                    className={`p-3.5 rounded-xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                      !item.isAvailable
                        ? 'border-rose-300 bg-rose-50/50'
                        : 'border-slate-200 bg-white'
                    }`}
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-xs font-bold text-slate-900">
                          {item.productName}
                        </span>
                        <span className="text-[11px] font-mono px-1.5 py-0.5 rounded bg-slate-100 text-slate-700">
                          {item.size ? `Size ${item.size}` : 'STANDARD'}
                        </span>
                        <span className="text-xs text-slate-500">
                          x{item.quantity}
                        </span>
                        {!item.isAvailable && (
                          <Badge variant="danger" size="sm" dot>
                            Không khả dụng
                          </Badge>
                        )}
                      </div>

                      {!item.isAvailable && item.unavailableReason && (
                        <p className="text-xs font-medium text-rose-700 flex items-center gap-1">
                          <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
                          <span>{item.unavailableReason}</span>
                        </p>
                      )}
                    </div>

                    <div className="flex items-center justify-between sm:justify-end gap-3">
                      <span className="text-xs font-bold text-slate-900 tabular-nums">
                        {formatVND(item.price * item.quantity)}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleRemoveCartItem(item.id)}
                        className="text-xs text-slate-400 hover:text-rose-600 font-semibold px-2 py-1 rounded"
                      >
                        Xóa
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Cart Checkout Banner & Actions */}
            <div className="p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-600">Tổng tiền giỏ hàng:</span>
                <span className="text-base font-bold text-slate-900">
                  {formatVND(cartTotal)}
                </span>
              </div>

              {!validation.canCheckout ? (
                <div className="p-3 bg-rose-100/90 border border-rose-300 rounded-lg text-xs text-rose-800 space-y-1">
                  <div className="flex items-center gap-1.5 font-bold">
                    <Ban className="h-4 w-4 text-rose-600 shrink-0" />
                    <span>CHẶN THANH TOÁN (CHECKOUT BLOCKED)</span>
                  </div>
                  <p className="text-[11px]">
                    Giỏ hàng có món tạm ngưng phục vụ hoặc thuộc danh mục tạm đóng.
                    Khách hàng bắt buộc phải xóa các món không khả dụng để tiếp tục đặt đơn.
                  </p>
                </div>
              ) : (
                <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg text-xs text-emerald-800 flex items-center gap-2">
                  <CheckCircle className="h-4 w-4 text-emerald-600 shrink-0" />
                  <span>
                    Tất cả các món đều khả dụng. Sẵn sàng cho khách hàng thanh toán!
                  </span>
                </div>
              )}

              <div className="flex justify-end pt-1">
                <Button
                  variant="primary"
                  disabled={!validation.canCheckout}
                  className={!validation.canCheckout ? 'opacity-50 cursor-not-allowed' : ''}
                  onClick={() => alert('Thanh toán thành công!')}
                >
                  {validation.canCheckout
                    ? 'Thanh toán ngay (Checkout)'
                    : 'Bị chặn: Vui lòng xóa món không khả dụng'}
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: ORDER HISTORY PRESERVATION */}
        {activeTab === 'history' && (
          <div className="space-y-3">
            <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-900 flex items-start gap-2">
              <ShieldCheck className="h-4 w-4 text-blue-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-bold">
                  Quy tắc bảo toàn dữ liệu lịch sử (No Hard Delete):
                </p>
                <p className="text-[11px] text-blue-800 mt-0.5">
                  Dù Category, Product hay Variant bị ẩn mềm hoặc ngừng kinh doanh,
                  các đơn hàng khách đã mua trong quá khứ vẫn giữ nguyên 100% dữ liệu
                  để bảo đảm tính pháp lý, đối soát doanh thu và hóa đơn tài chính.
                </p>
              </div>
            </div>

            <div className="space-y-3">
              {SAMPLE_ORDER_HISTORY.map((order) => {
                const cat = categories.find((c) => c.id === order.storeCategoryId);
                return (
                  <div
                    key={order.orderId}
                    className="p-4 rounded-xl border border-slate-200 bg-white space-y-2 shadow-2xs"
                  >
                    <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold text-slate-800">
                          {order.orderId}
                        </span>
                        <Badge variant="success" size="sm">
                          Đã hoàn thành
                        </Badge>
                        {cat && !cat.isActive && (
                          <span className="text-[10px] bg-amber-100 text-amber-800 px-1.5 py-0.5 rounded font-semibold">
                            Danh mục cha đã ẩn
                          </span>
                        )}
                      </div>
                      <span className="text-[11px] text-slate-400 font-mono flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {order.orderTime}
                      </span>
                    </div>

                    <div className="flex items-center justify-between text-xs text-slate-700">
                      <div>
                        <span className="font-semibold">{order.productName}</span>
                        <span className="text-slate-500 ml-1.5">
                          ({order.size ? `Size ${order.size}` : 'STANDARD'}) x
                          {order.quantity}
                        </span>
                      </div>
                      <span className="font-bold text-slate-900">
                        {formatVND(order.totalPrice)}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
};
