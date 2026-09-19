import React, { useState, useId } from 'react';
import {
  ProductCatalogDto,
  StoreCategoryDto,
  UpdateProductRequest,
  UpdateProductVariantRequest,
  CreateProductVariantRequest,
  InventoryMode,
} from '@/types/api.types';
import { Button, Input, Badge } from '@/components/ui';
import { formatVND } from '../utils/formatters';
import {
  Save,
  AlertCircle,
  Eye,
  EyeOff,
  Package,
  Layers,
  Plus,
  Trash2,
  Sparkles,
} from 'lucide-react';

export interface ProductEditFormProps {
  product: ProductCatalogDto & { storeCategoryId?: number };
  categories?: StoreCategoryDto[];
  onSubmit: (payload: {
    productUpdates: UpdateProductRequest;
    variantUpdates: { variantId: number; updates: UpdateProductVariantRequest }[];
    newVariants?: CreateProductVariantRequest[];
  }) => Promise<void> | void;
  onCancel: () => void;
  isLoading?: boolean;
  serverError?: string | null;
}

const DEFAULT_CATEGORIES: StoreCategoryDto[] = [
  { id: 1, storeId: 1, name: 'Cà phê & Trà trái cây', isActive: true, displayOrder: 1 },
  { id: 2, storeId: 1, name: 'Trà sữa & Macchiato', isActive: true, displayOrder: 2 },
  { id: 3, storeId: 1, name: 'Nước ép tươi', isActive: true, displayOrder: 3 },
  { id: 4, storeId: 1, name: 'Bánh mì & Món ăn nhẹ', isActive: false, displayOrder: 4 }, // Example inactive category
];

export const ProductEditForm: React.FC<ProductEditFormProps> = ({
  product,
  categories = DEFAULT_CATEGORIES,
  onSubmit,
  onCancel,
  isLoading = false,
  serverError = null,
}) => {
  const formId = useId();

  // Product level state
  const [name, setName] = useState(product.name);
  const [description, setDescription] = useState(product.description || '');
  const [imageUrl, setImageUrl] = useState(product.imageUrl || '');
  const [active, setActive] = useState(product.active);
  const [storeCategoryId, setStoreCategoryId] = useState(product.storeCategoryId || 1);

  // Existing variants state (mapped to local editable model)
  const [variants, setVariants] = useState<
    Array<{
      variantId: number;
      name: string;
      size: string | null;
      price: number | '';
      inventoryMode: InventoryMode;
      dailyCapacityDefault: number | '';
      autoAcceptOverride: boolean;
      available: boolean;
      active: boolean;
      isNew: boolean;
    }>
  >(() =>
    product.variants.map((v) => ({
      variantId: v.id,
      name: v.name,
      size: v.size,
      price: v.price as number | '',
      inventoryMode: v.inventoryMode,
      dailyCapacityDefault: (v.dailyCapacityDefault ?? '') as number | '',
      autoAcceptOverride: v.autoAcceptOverride ?? false,
      available: v.available,
      active: v.active,
      isNew: false,
    }))
  );

  // New variants added during edit
  const [newVariants, setNewVariants] = useState<
    Array<{
      id: string;
      name: string;
      size: string | null;
      price: number | '';
      inventoryMode: InventoryMode;
      dailyCapacityDefault: number | '';
      autoAcceptOverride: boolean;
      available: boolean;
    }>
  >([]);

  // Validation state
  const [errors, setErrors] = useState<{
    name?: string;
    variants?: Record<string | number, { price?: string; dailyCapacityDefault?: string; name?: string }>;
  }>({});

  const selectedCategory = categories.find((c) => c.id === storeCategoryId);

  const handleVariantChange = (
    variantId: number,
    field: string,
    value: unknown
  ) => {
    setVariants((prev) =>
      prev.map((v) => (v.variantId === variantId ? { ...v, [field]: value } : v))
    );
  };

  const handleAddNewVariant = () => {
    const nextSize = variants.length % 2 === 0 ? 'L' : 'XL';
    const lastPrice = variants[variants.length - 1]?.price || 35000;
    const newVar = {
      id: `new_${Date.now()}`,
      name: `Size ${nextSize}`,
      size: nextSize,
      price: Number(lastPrice) + 10000,
      inventoryMode: 'MADE_TO_ORDER' as InventoryMode,
      dailyCapacityDefault: 80,
      autoAcceptOverride: true,
      available: true,
    };
    setNewVariants((prev) => [...prev, newVar]);
  };

  const handleNewVariantChange = (id: string, field: string, value: unknown) => {
    setNewVariants((prev) =>
      prev.map((v) => (v.id === id ? { ...v, [field]: value } : v))
    );
  };

  const handleRemoveNewVariant = (id: string) => {
    setNewVariants((prev) => prev.filter((v) => v.id !== id));
  };

  const validate = (): boolean => {
    const newErrors: typeof errors = { variants: {} };
    let isValid = true;

    if (!name.trim()) {
      newErrors.name = 'Tên món ăn không được để trống';
      isValid = false;
    }

    // Check existing variants
    for (const v of variants) {
      const vErr: { price?: string; dailyCapacityDefault?: string; name?: string } = {};
      if (v.price === '' || Number(v.price) <= 0) {
        vErr.price = 'Giá bán phải lớn hơn 0 VND';
        isValid = false;
      }
      if (v.dailyCapacityDefault !== '' && Number(v.dailyCapacityDefault) < 0) {
        vErr.dailyCapacityDefault = 'Công suất không được âm';
        isValid = false;
      }
      if (!v.name.trim()) {
        vErr.name = 'Tên biến thể không được trống';
        isValid = false;
      }
      if (Object.keys(vErr).length > 0) {
        newErrors.variants![v.variantId] = vErr;
      }
    }

    // Check new variants
    for (const nv of newVariants) {
      const vErr: { price?: string; dailyCapacityDefault?: string; name?: string } = {};
      if (nv.price === '' || Number(nv.price) <= 0) {
        vErr.price = 'Giá bán phải lớn hơn 0 VND';
        isValid = false;
      }
      if (nv.dailyCapacityDefault !== '' && Number(nv.dailyCapacityDefault) < 0) {
        vErr.dailyCapacityDefault = 'Công suất không được âm';
        isValid = false;
      }
      if (Object.keys(vErr).length > 0) {
        newErrors.variants![nv.id] = vErr;
      }
    }

    setErrors(newErrors);
    return isValid;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    // 1. Prepare Product PATCH updates
    const productUpdates: UpdateProductRequest = {
      name: name.trim(),
      description: description.trim() || undefined,
      imageUrl: imageUrl.trim() || undefined,
      active,
    };

    // 2. Prepare Variant PATCH updates
    const variantUpdates: { variantId: number; updates: UpdateProductVariantRequest }[] =
      variants.map((v) => ({
        variantId: v.variantId,
        updates: {
          name: v.name.trim(),
          price: Number(v.price),
          inventoryMode: v.inventoryMode,
          autoAcceptOverride: v.autoAcceptOverride,
          dailyCapacityDefault:
            v.dailyCapacityDefault !== '' ? Number(v.dailyCapacityDefault) : undefined,
          available: v.available,
          active: v.active,
        },
      }));

    // 3. Prepare New Variants to create
    const newVariantsPayload: CreateProductVariantRequest[] = newVariants.map((nv) => ({
      name: nv.name.trim(),
      size: nv.size,
      price: Number(nv.price),
      inventoryMode: nv.inventoryMode,
      autoAcceptOverride: nv.autoAcceptOverride,
      dailyCapacityDefault:
        nv.dailyCapacityDefault !== '' ? Number(nv.dailyCapacityDefault) : undefined,
      available: nv.available,
    }));

    await onSubmit({
      productUpdates,
      variantUpdates,
      newVariants: newVariantsPayload.length ? newVariantsPayload : undefined,
    });
  };

  return (
    <form id={formId} onSubmit={handleSubmit} noValidate className="space-y-6">
      {/* Server error alert */}
      {serverError && (
        <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-start gap-2">
          <AlertCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
          <span>{serverError}</span>
        </div>
      )}

      {/* Dependent Availability Warning if Category is Inactive */}
      {selectedCategory && !selectedCategory.isActive && (
        <div className="p-3 bg-amber-50 border border-amber-300 rounded-xl text-xs text-amber-900 flex items-start gap-2.5 animate-fadeIn">
          <AlertCircle className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
          <div>
            <p className="font-bold">
              Danh mục "{selectedCategory.name}" hiện đang TẠM ĐÓNG
            </p>
            <p className="text-[11px] text-amber-800 mt-0.5">
              Món ăn này sẽ không xuất hiện trên thực đơn công khai cho khách hàng
              do phụ thuộc vào trạng thái danh mục cha (Dependent Availability).
              Tuy nhiên, các đơn hàng cũ và giỏ hàng của khách hàng vẫn được bảo lưu an toàn.
            </p>
          </div>
        </div>
      )}

      {/* Section 1: Product General Info & Soft Status */}
      <div className="space-y-4 bg-white p-4 sm:p-5 rounded-2xl border border-slate-200 shadow-xs">
        <div className="flex items-center justify-between border-b border-slate-100 pb-2.5">
          <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
            <Layers className="h-4 w-4 text-slate-500" />
            <span>Thông tin món ăn (ID: #{product.id})</span>
          </h3>
          <Badge variant={active ? 'success' : 'danger'} dot size="sm">
            {active ? 'Đang kích hoạt' : 'Tạm ẩn khỏi menu'}
          </Badge>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="md:col-span-2">
            <Input
              label="Tên món ăn"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              error={errors.name}
            />
          </div>

          <div className="space-y-1.5">
            <label htmlFor={`${formId}-category`} className="block text-xs font-semibold text-slate-700">
              Danh mục thực đơn <span className="text-rose-500">*</span>
            </label>
            <select
              id={`${formId}-category`}
              value={storeCategoryId}
              onChange={(e) => setStoreCategoryId(Number(e.target.value))}
              className="block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus-visible:border-emerald-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20"
            >
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name} {!cat.isActive ? '(Tạm đóng)' : ''}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5">
            <Input
              label="Đường dẫn ảnh món ăn"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
            />
          </div>

          <div className="md:col-span-2 space-y-1.5">
            <label htmlFor={`${formId}-desc`} className="block text-xs font-semibold text-slate-700">
              Mô tả món ăn
            </label>
            <textarea
              id={`${formId}-desc`}
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus-visible:border-emerald-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20"
            />
          </div>

          {/* Soft Hide Switch for Product */}
          <div className="md:col-span-2 flex items-center justify-between p-3 bg-slate-50 rounded-xl border border-slate-200">
            <div>
              <label htmlFor={`${formId}-product-active`} className="text-xs font-bold text-slate-800 flex items-center gap-1.5 cursor-pointer">
                {active ? (
                  <Eye className="h-4 w-4 text-emerald-600" aria-hidden="true" />
                ) : (
                  <EyeOff className="h-4 w-4 text-rose-600" aria-hidden="true" />
                )}
                <span>Trạng thái mở bán món ăn (Soft Hide: is_active)</span>
              </label>
              <span className="text-[11px] text-slate-500 block mt-0.5">
                {active
                  ? 'Món ăn đang hiển thị trên thực đơn công khai.'
                  : 'Món ăn đã được ẩn mềm (soft hide). Không bị xóa cứng khỏi cơ sở dữ liệu.'}
              </span>
            </div>
            <label htmlFor={`${formId}-product-active`} className="relative inline-flex items-center cursor-pointer">
              <input
                id={`${formId}-product-active`}
                type="checkbox"
                checked={active}
                onChange={(e) => setActive(e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-200 peer-focus-visible:ring-2 peer-focus-visible:ring-emerald-500 peer-focus-visible:ring-offset-2 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-emerald-600" />
            </label>
          </div>
        </div>
      </div>

      {/* Section 2: Variants Management (PATCH semantics) */}
      <div className="space-y-4 bg-white p-4 sm:p-5 rounded-2xl border border-slate-200 shadow-xs">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Package className="h-4 w-4 text-emerald-600" />
              <span>Biến thể kích cỡ & Trạng thái tồn kho ({variants.length + newVariants.length})</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Cập nhật giá, công suất, bật/tắt bán (is_available) hoặc ẩn mềm (is_active) từng biến thể.
            </p>
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            leftIcon={<Plus className="h-3.5 w-3.5" />}
            onClick={handleAddNewVariant}
          >
            Thêm biến thể mới
          </Button>
        </div>

        {/* Existing Variants List */}
        <div className="space-y-3">
          {variants.map((variant, index) => {
            const vErr = errors.variants?.[variant.variantId] || {};

            return (
              <div
                key={variant.variantId}
                className={`p-4 rounded-xl border transition-all ${
                  !variant.active
                    ? 'border-slate-200 bg-slate-100/70 opacity-75'
                    : !variant.available
                    ? 'border-amber-200 bg-amber-50/40'
                    : 'border-slate-200 bg-slate-50/50'
                }`}
              >
                {/* Variant Header */}
                <div className="flex items-center justify-between gap-2 mb-3 pb-2 border-b border-slate-200/60">
                  <div className="flex items-center gap-2">
                    <span className="w-5 h-5 rounded-full bg-slate-200 text-slate-800 text-[11px] font-bold flex items-center justify-center">
                      {index + 1}
                    </span>
                    <span className="text-xs font-bold text-slate-800">{variant.name}</span>
                    <span
                      className={`text-[10px] font-mono px-2 py-0.5 rounded font-semibold ${
                        variant.size === null
                          ? 'bg-purple-100 text-purple-800'
                          : 'bg-blue-100 text-blue-800'
                      }`}
                    >
                      {variant.size ? `Size ${variant.size}` : 'STANDARD (size: null)'}
                    </span>
                    {!variant.active && (
                      <span className="text-[10px] bg-rose-100 text-rose-800 px-2 py-0.5 rounded font-bold">
                        Đã ẩn mềm (is_active: false)
                      </span>
                    )}
                    {variant.active && !variant.available && (
                      <span className="text-[10px] bg-amber-100 text-amber-800 px-2 py-0.5 rounded font-bold">
                        Tạm ngưng bán (is_available: false)
                      </span>
                    )}
                  </div>

                  <div className="flex items-center gap-2">
                    {/* Soft Delete / Hide toggle */}
                    <button
                      type="button"
                      onClick={() =>
                        handleVariantChange(variant.variantId, 'active', !variant.active)
                      }
                      className={`text-xs px-2.5 py-1 rounded-lg border font-semibold transition-colors ${
                        variant.active
                          ? 'border-slate-200 text-slate-600 hover:bg-slate-100'
                          : 'border-emerald-300 bg-emerald-50 text-emerald-800'
                      } focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500`}
                      title="Bật/tắt trạng thái kinh doanh biến thể này"
                    >
                      {variant.active ? 'Ẩn biến thể' : 'Khôi phục'}
                    </button>
                  </div>
                </div>

                {/* Variant Edit Fields */}
                {(() => {
                  const nameId = `${formId}-v-${variant.variantId}-name`;
                  const priceId = `${formId}-v-${variant.variantId}-price`;
                  const modeId = `${formId}-v-${variant.variantId}-mode`;
                  const capacityId = `${formId}-v-${variant.variantId}-capacity`;
                  const availableId = `${formId}-v-${variant.variantId}-available`;
                  const autoAcceptId = `${formId}-v-${variant.variantId}-autoAccept`;

                  return (
                    <>
                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
                        {/* Name */}
                        <div className="space-y-1">
                          <label htmlFor={nameId} className="block font-semibold text-slate-700">Tên biến thể</label>
                          <input
                            id={nameId}
                            type="text"
                            value={variant.name}
                            onChange={(e) =>
                              handleVariantChange(variant.variantId, 'name', e.target.value)
                            }
                            className="block w-full rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500"
                          />
                          {vErr.name && (
                            <p className="text-[11px] text-rose-600 font-medium">{vErr.name}</p>
                          )}
                        </div>

                        {/* Price */}
                        <div className="space-y-1">
                          <label htmlFor={priceId} className="block font-semibold text-slate-700">Giá bán (VND)</label>
                          <div className="relative">
                            <input
                              id={priceId}
                              type="number"
                              min="1"
                              step="1000"
                              value={variant.price}
                              onChange={(e) =>
                                handleVariantChange(
                                  variant.variantId,
                                  'price',
                                  e.target.value === '' ? '' : Number(e.target.value)
                                )
                              }
                              className={`block w-full rounded-lg border px-2.5 py-1.5 text-xs pr-10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500 ${
                                vErr.price
                                  ? 'border-rose-400 bg-rose-50 text-rose-900'
                                  : 'border-slate-300 text-slate-900'
                              }`}
                            />
                            <span className="absolute inset-y-0 right-0 pr-2.5 flex items-center text-[11px] text-slate-500 font-mono pointer-events-none">
                              đ
                            </span>
                          </div>
                          {vErr.price ? (
                            <p className="text-[11px] text-rose-600 font-medium">{vErr.price}</p>
                          ) : (
                            <p className="text-[10px] text-slate-500">
                              {formatVND(Number(variant.price))}
                            </p>
                          )}
                        </div>

                        {/* Inventory Mode */}
                        <div className="space-y-1">
                          <label htmlFor={modeId} className="block font-semibold text-slate-700">Chế độ kho</label>
                          <select
                            id={modeId}
                            value={variant.inventoryMode}
                            onChange={(e) =>
                              handleVariantChange(
                                variant.variantId,
                                'inventoryMode',
                                e.target.value as InventoryMode
                              )
                            }
                            className="block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-xs text-slate-900 focus-visible:border-emerald-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20"
                          >
                            <option value="MADE_TO_ORDER">MADE_TO_ORDER (Chế biến)</option>
                            <option value="LIMITED_STOCK">LIMITED_STOCK (Kho đóng gói)</option>
                          </select>
                        </div>

                        {/* Daily Capacity */}
                        <div className="space-y-1">
                          <label htmlFor={capacityId} className="block font-semibold text-slate-700">
                            Công suất ngày (suất)
                          </label>
                          <input
                            id={capacityId}
                            type="number"
                            min="0"
                            value={variant.dailyCapacityDefault}
                            onChange={(e) =>
                              handleVariantChange(
                                variant.variantId,
                                'dailyCapacityDefault',
                                e.target.value === '' ? '' : Number(e.target.value)
                              )
                            }
                            className={`block w-full rounded-lg border px-2.5 py-1.5 text-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500 ${
                              vErr.dailyCapacityDefault
                                ? 'border-rose-400 bg-rose-50 text-rose-900'
                                : 'border-slate-300 text-slate-900'
                            }`}
                          />
                          {vErr.dailyCapacityDefault && (
                            <p className="text-[11px] text-rose-600 font-medium">
                              {vErr.dailyCapacityDefault}
                            </p>
                          )}
                        </div>
                      </div>

                      {/* Variant Toggles: is_available & auto_accept */}
                      <div className="mt-3 pt-3 border-t border-slate-200/60 flex flex-wrap items-center justify-between gap-3 text-xs">
                        {/* Available Toggle */}
                        <label htmlFor={availableId} className="flex items-center gap-2 font-semibold text-slate-700 cursor-pointer">
                          <input
                            id={availableId}
                            type="checkbox"
                            aria-label="Trạng thái mở bán biến thể"
                            checked={variant.available}
                            onChange={(e) =>
                              handleVariantChange(variant.variantId, 'available', e.target.checked)
                            }
                            className="rounded text-emerald-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-1"
                          />
                          <span>
                            {variant.available ? (
                              <span className="text-emerald-700">Đang mở bán (is_available: true)</span>
                            ) : (
                              <span className="text-amber-800">Tạm ngưng bán (is_available: false)</span>
                            )}
                          </span>
                        </label>

                        {/* Auto Accept Override */}
                        <label htmlFor={autoAcceptId} className="flex items-center gap-2 font-semibold text-slate-700 cursor-pointer">
                          <input
                            id={autoAcceptId}
                            type="checkbox"
                            aria-label="Tự động nhận đơn cho biến thể"
                            checked={variant.autoAcceptOverride}
                            onChange={(e) =>
                              handleVariantChange(
                                variant.variantId,
                                'autoAcceptOverride',
                                e.target.checked
                              )
                            }
                            className="rounded text-emerald-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-1"
                          />
                          <span>Tự động nhận đơn</span>
                        </label>
                      </div>
                    </>
                  );
                })()}
              </div>
            );
          })}

          {/* New Variants Added */}
          {newVariants.map((nv, idx) => {
            const vErr = errors.variants?.[nv.id] || {};
            const nvNameId = `${formId}-nv-${nv.id}-name`;
            const nvPriceId = `${formId}-nv-${nv.id}-price`;
            const nvCapacityId = `${formId}-nv-${nv.id}-capacity`;

            return (
              <div
                key={nv.id}
                className="p-4 rounded-xl border border-emerald-200 bg-emerald-50/30 space-y-3"
              >
                <div className="flex items-center justify-between border-b border-emerald-200/60 pb-2">
                  <div className="flex items-center gap-2">
                    <Sparkles className="h-4 w-4 text-emerald-600" aria-hidden="true" />
                    <span className="text-xs font-bold text-emerald-900">
                      Biến thể mới #{idx + 1} ({nv.name})
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleRemoveNewVariant(nv.id)}
                    aria-label={`Xóa biến thể mới ${nv.name}`}
                    className="text-rose-600 hover:text-rose-800 text-xs font-bold p-1 rounded-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500 transition-colors"
                  >
                    <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
                  <div>
                    <label htmlFor={nvNameId} className="block font-semibold text-slate-700">Tên biến thể</label>
                    <input
                      id={nvNameId}
                      type="text"
                      value={nv.name}
                      onChange={(e) => handleNewVariantChange(nv.id, 'name', e.target.value)}
                      className="block w-full rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500"
                    />
                    {vErr.name && (
                      <p className="text-[11px] text-rose-600 font-medium">{vErr.name}</p>
                    )}
                  </div>
                  <div>
                    <label htmlFor={nvPriceId} className="block font-semibold text-slate-700">Giá bán (VND)</label>
                    <input
                      id={nvPriceId}
                      type="number"
                      value={nv.price}
                      onChange={(e) =>
                        handleNewVariantChange(
                          nv.id,
                          'price',
                          e.target.value === '' ? '' : Number(e.target.value)
                        )
                      }
                      className="block w-full rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500"
                    />
                    {vErr.price && (
                      <p className="text-[11px] text-rose-600 font-medium">{vErr.price}</p>
                    )}
                  </div>
                  <div>
                    <label htmlFor={nvCapacityId} className="block font-semibold text-slate-700">Công suất ngày</label>
                    <input
                      id={nvCapacityId}
                      type="number"
                      value={nv.dailyCapacityDefault}
                      onChange={(e) =>
                        handleNewVariantChange(
                          nv.id,
                          'dailyCapacityDefault',
                          e.target.value === '' ? '' : Number(e.target.value)
                        )
                      }
                      className="block w-full rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500"
                    />
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Form Action Controls */}
      <div className="flex items-center justify-end gap-3 pt-2">
        <Button type="button" variant="outline" onClick={onCancel} disabled={isLoading}>
          Hủy bỏ
        </Button>
        <Button
          type="submit"
          variant="primary"
          isLoading={isLoading}
          leftIcon={<Save className="h-4 w-4" aria-hidden="true" />}
        >
          {isLoading ? 'Đang lưu thay đổi…' : 'Lưu thay đổi (PATCH)'}
        </Button>
      </div>
    </form>
  );
};
