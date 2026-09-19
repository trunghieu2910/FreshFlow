import React, { useState, useId } from 'react';
import {
  ProductCreateFormData,
  VariantFormData,
  InventoryMode,
  VariantSizeType,
} from '@/types/api.types';
import {
  validateProductCreateForm,
  createDefaultVariant,
  ProductFormValidationResult,
  resolveVariantSize,
  resolveVariantName,
} from '../utils/productValidation';
import { formatVND } from '../utils/formatters';
import { Button, Input } from '@/components/ui';
import {
  Plus,
  Trash2,
  AlertCircle,
  Sparkles,
  Layers,
  ChefHat,
  Package,
  Zap,
  CheckCircle,
} from 'lucide-react';

export interface ProductCreateFormProps {
  initialData?: Partial<ProductCreateFormData>;
  onSubmit: (formData: ProductCreateFormData) => Promise<void> | void;
  onCancel: () => void;
  isLoading?: boolean;
  serverError?: string | null;
}

const CATEGORY_OPTIONS = [
  { id: 1, name: 'Cà phê & Trà trái cây' },
  { id: 2, name: 'Trà sữa & Macchiato' },
  { id: 3, name: 'Nước ép & Sinh tố' },
  { id: 4, name: 'Bánh mì & Món ăn nhẹ' },
  { id: 5, name: 'Tráng miệng & Đồ ngọt' },
];

export const ProductCreateForm: React.FC<ProductCreateFormProps> = ({
  initialData,
  onSubmit,
  onCancel,
  isLoading = false,
  serverError = null,
}) => {
  const formId = useId();

  // Initial form state
  const [formData, setFormData] = useState<ProductCreateFormData>(() => ({
    storeCategoryId: initialData?.storeCategoryId || 1,
    name: initialData?.name || '',
    description: initialData?.description || '',
    imageUrl: initialData?.imageUrl || '',
    active: initialData?.active ?? true,
    variants: initialData?.variants?.length
      ? initialData.variants
      : [createDefaultVariant('STANDARD', 35000, 'MADE_TO_ORDER')],
  }));

  const [validationResult, setValidationResult] = useState<ProductFormValidationResult>({
    isValid: true,
    errors: { variantErrors: {} },
  });

  const [hasAttemptedSubmit, setHasAttemptedSubmit] = useState(false);

  // Quick preset switcher: Single standard vs Multiple sizes
  const currentMode =
    formData.variants.length === 1 && formData.variants[0].sizeType === 'STANDARD'
      ? 'STANDARD'
      : 'SIZED';

  const handleApplyPreset = (mode: 'STANDARD' | 'SIZED') => {
    if (mode === 'STANDARD') {
      const standardVar: VariantFormData = {
        id: `var_${Date.now()}_std`,
        name: 'STANDARD',
        sizeType: 'STANDARD',
        customSize: '',
        price: formData.variants[0]?.price || 35000,
        inventoryMode: formData.variants[0]?.inventoryMode || 'MADE_TO_ORDER',
        dailyCapacityDefault: 120,
        autoAcceptOverride: true,
        maxQuantityPerOrder: 10,
        available: true,
      };
      setFormData((prev) => ({ ...prev, variants: [standardVar] }));
    } else {
      const basePrice = Number(formData.variants[0]?.price) || 35000;
      const sizeM: VariantFormData = {
        id: `var_${Date.now()}_m`,
        name: 'Size M',
        sizeType: 'M',
        customSize: '',
        price: basePrice,
        inventoryMode: 'MADE_TO_ORDER',
        dailyCapacityDefault: 100,
        autoAcceptOverride: true,
        maxQuantityPerOrder: 10,
        available: true,
      };
      const sizeL: VariantFormData = {
        id: `var_${Date.now() + 1}_l`,
        name: 'Size L',
        sizeType: 'L',
        customSize: '',
        price: basePrice + 10000,
        inventoryMode: 'MADE_TO_ORDER',
        dailyCapacityDefault: 80,
        autoAcceptOverride: true,
        maxQuantityPerOrder: 10,
        available: true,
      };
      setFormData((prev) => ({ ...prev, variants: [sizeM, sizeL] }));
    }

    if (hasAttemptedSubmit) {
      setTimeout(runValidation, 0);
    }
  };

  const runValidation = (dataToValidate = formData): boolean => {
    const result = validateProductCreateForm(dataToValidate);
    setValidationResult(result);
    return result.isValid;
  };

  // Field change handlers
  const handleProductFieldChange = <K extends keyof ProductCreateFormData>(
    field: K,
    value: ProductCreateFormData[K]
  ) => {
    const nextData = { ...formData, [field]: value };
    setFormData(nextData);
    if (hasAttemptedSubmit) {
      runValidation(nextData);
    }
  };

  const handleVariantChange = (
    id: string,
    field: keyof VariantFormData,
    value: unknown
  ) => {
    const nextVariants = formData.variants.map((v) => {
      if (v.id !== id) return v;
      const updated = { ...v, [field]: value };

      // Auto adjust name if changing sizeType
      if (field === 'sizeType') {
        const newSizeType = value as VariantSizeType;
        if (newSizeType === 'STANDARD') {
          updated.name = 'STANDARD';
          updated.customSize = '';
        } else if (newSizeType === 'M') {
          updated.name = 'Size M';
          updated.customSize = '';
        } else if (newSizeType === 'L') {
          updated.name = 'Size L';
          updated.customSize = '';
        }
      }

      return updated;
    });

    const nextData = { ...formData, variants: nextVariants };
    setFormData(nextData);
    if (hasAttemptedSubmit) {
      runValidation(nextData);
    }
  };

  // Add a new variant
  const handleAddVariant = (sizeType: VariantSizeType = 'M') => {
    const existingSizes = new Set(formData.variants.map((v) => v.sizeType));
    let targetSize = sizeType;
    if (existingSizes.has(targetSize)) {
      if (!existingSizes.has('M')) targetSize = 'M';
      else if (!existingSizes.has('L')) targetSize = 'L';
      else targetSize = 'CUSTOM';
    }

    const lastVariant = formData.variants[formData.variants.length - 1];
    const defaultPrice = lastVariant ? Number(lastVariant.price) + 5000 : 40000;

    const newVariant = createDefaultVariant(
      targetSize,
      defaultPrice,
      lastVariant?.inventoryMode || 'MADE_TO_ORDER'
    );

    const nextData = {
      ...formData,
      variants: [...formData.variants, newVariant],
    };
    setFormData(nextData);
    if (hasAttemptedSubmit) {
      runValidation(nextData);
    }
  };

  // Remove a variant
  const handleRemoveVariant = (id: string) => {
    if (formData.variants.length <= 1) return;
    const nextData = {
      ...formData,
      variants: formData.variants.filter((v) => v.id !== id),
    };
    setFormData(nextData);
    if (hasAttemptedSubmit) {
      runValidation(nextData);
    }
  };

  // Submit Handler
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setHasAttemptedSubmit(true);

    const isValid = runValidation(formData);
    if (!isValid) return;

    await onSubmit(formData);
  };

  const { errors } = validationResult;

  return (
    <form
      id={formId}
      onSubmit={handleSubmit}
      noValidate
      className="space-y-6 animate-fadeIn"
    >
      {/* Top Banner Server Error */}
      {serverError && (
        <div
          role="alert"
          className="flex items-start gap-3 p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800"
        >
          <AlertCircle className="h-4 w-4 text-rose-600 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold">Đã xảy ra lỗi khi tạo món ăn</p>
            <p className="mt-0.5">{serverError}</p>
          </div>
        </div>
      )}

      {/* Preset Switcher Header */}
      <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-3 sm:p-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 text-emerald-600 shrink-0" />
            <div>
              <span className="text-xs font-bold text-slate-800 block">
                Cấu hình phân loại món ăn
              </span>
              <span className="text-[11px] text-slate-500">
                Chọn kiểu món chuẩn một kích cỡ hoặc món có nhiều size (M, L...).
              </span>
            </div>
          </div>

          <div className="inline-flex rounded-xl bg-slate-200/70 p-1 self-start sm:self-auto">
            <button
              type="button"
              onClick={() => handleApplyPreset('STANDARD')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 transition-all ${
                currentMode === 'STANDARD'
                  ? 'bg-white text-emerald-800 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Món chuẩn (STANDARD)
            </button>
            <button
              type="button"
              onClick={() => handleApplyPreset('SIZED')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 transition-all ${
                currentMode === 'SIZED'
                  ? 'bg-white text-emerald-800 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              Nhiều kích cỡ (Size M / L)
            </button>
          </div>
        </div>
      </div>

      {/* SECTION 1: Product General Info */}
      <div className="space-y-4 bg-white p-4 sm:p-5 rounded-2xl border border-slate-200 shadow-xs">
        <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2 border-b border-slate-100 pb-2.5">
          <Layers className="h-4 w-4 text-slate-500" />
          <span>Thông tin món ăn cơ bản</span>
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Product Name */}
          <div className="md:col-span-2">
            <Input
              label="Tên món ăn"
              required
              placeholder="Ví dụ: Trà Đào Cam Sả, Cà Phê Muối..."
              value={formData.name}
              onChange={(e) => handleProductFieldChange('name', e.target.value)}
              error={hasAttemptedSubmit ? errors.name : undefined}
              maxLength={150}
            />
          </div>

          {/* Store Category */}
          <div className="space-y-1.5">
            <label
              htmlFor={`${formId}-category`}
              className="block text-xs font-semibold text-slate-700"
            >
              Danh mục thực đơn <span className="text-rose-500">*</span>
            </label>
            <select
              id={`${formId}-category`}
              value={formData.storeCategoryId}
              onChange={(e) =>
                handleProductFieldChange('storeCategoryId', Number(e.target.value))
              }
              className="block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
            >
              {CATEGORY_OPTIONS.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>
            {hasAttemptedSubmit && errors.storeCategoryId && (
              <p className="text-xs font-medium text-rose-600">
                {errors.storeCategoryId}
              </p>
            )}
          </div>

          {/* Image URL */}
          <div className="space-y-1.5">
            <Input
              label="Đường dẫn ảnh món ăn (Tùy chọn)"
              placeholder="https://images.freshflow.vn/products/tra-dao.jpg"
              value={formData.imageUrl}
              onChange={(e) => handleProductFieldChange('imageUrl', e.target.value)}
              error={hasAttemptedSubmit ? errors.imageUrl : undefined}
            />
          </div>

          {/* Description */}
          <div className="md:col-span-2 space-y-1.5">
            <label
              htmlFor={`${formId}-desc`}
              className="block text-xs font-semibold text-slate-700"
            >
              Mô tả chi tiết món ăn (Tùy chọn)
            </label>
            <textarea
              id={`${formId}-desc`}
              rows={2}
              placeholder="Mô tả nguyên liệu, hương vị đặc sắc..."
              value={formData.description}
              onChange={(e) => handleProductFieldChange('description', e.target.value)}
              maxLength={2000}
              className="block w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
            />
            {hasAttemptedSubmit && errors.description && (
              <p className="text-xs font-medium text-rose-600">
                {errors.description}
              </p>
            )}
          </div>

          {/* Active toggle */}
          <div className="md:col-span-2 flex items-center justify-between p-3 bg-slate-50 rounded-xl border border-slate-200/70">
            <div>
              <label htmlFor={`${formId}-product-active`} className="text-xs font-bold text-slate-800 block cursor-pointer">
                Trạng thái hiển thị trong thực đơn
              </label>
              <span className="text-[11px] text-slate-500">
                Cho phép khách hàng nhìn thấy và đặt món này ngay sau khi tạo.
              </span>
            </div>
            <label htmlFor={`${formId}-product-active`} className="relative inline-flex items-center cursor-pointer">
              <input
                id={`${formId}-product-active`}
                type="checkbox"
                checked={formData.active}
                onChange={(e) => handleProductFieldChange('active', e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-200 peer-focus-visible:ring-2 peer-focus-visible:ring-emerald-500 peer-focus-visible:ring-offset-2 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-emerald-600" />
            </label>
          </div>
        </div>
      </div>

      {/* SECTION 2: Variants Management (Nested Form) */}
      <div className="space-y-4 bg-white p-4 sm:p-5 rounded-2xl border border-slate-200 shadow-xs">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
          <div>
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Package className="h-4 w-4 text-emerald-600" />
              <span>Biến thể & Giá bán ({formData.variants.length} biến thể)</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Quy ước: Size STANDARD có giá trị kích cỡ là <code>null</code>. Không
              cho phép trùng lặp kích cỡ hoặc tên.
            </p>
          </div>

          {/* Quick Add Variant Buttons */}
          <div className="flex items-center gap-1.5 flex-wrap">
            {!formData.variants.some((v) => v.sizeType === 'M') && (
              <button
                type="button"
                onClick={() => handleAddVariant('M')}
                className="px-2.5 py-1 rounded-lg border border-emerald-300 bg-emerald-50 text-emerald-800 text-xs font-semibold hover:bg-emerald-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 transition-colors"
              >
                + Size M
              </button>
            )}
            {!formData.variants.some((v) => v.sizeType === 'L') && (
              <button
                type="button"
                onClick={() => handleAddVariant('L')}
                className="px-2.5 py-1 rounded-lg border border-emerald-300 bg-emerald-50 text-emerald-800 text-xs font-semibold hover:bg-emerald-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 transition-colors"
              >
                + Size L
              </button>
            )}
            <Button
              type="button"
              variant="outline"
              size="sm"
              leftIcon={<Plus className="h-3.5 w-3.5" aria-hidden="true" />}
              onClick={() => handleAddVariant('CUSTOM')}
            >
              Thêm biến thể
            </Button>
          </div>
        </div>

        {/* Form-level variant error */}
        {hasAttemptedSubmit && errors.variants && (
          <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-700 font-medium">
            {errors.variants}
          </div>
        )}

        {/* Variants List Cards */}
        <div className="space-y-4">
          {formData.variants.map((variant, index) => {
            const vErr = errors.variantErrors[variant.id] || {};
            const resolvedSize = resolveVariantSize(variant);
            const resolvedName = resolveVariantName(variant, formData.name);

            return (
              <div
                key={variant.id}
                className={`p-4 rounded-xl border transition-all ${
                  vErr.duplicate
                    ? 'border-rose-400 bg-rose-50/40 shadow-xs ring-1 ring-rose-300'
                    : 'border-slate-200 bg-slate-50/50 hover:border-slate-300'
                }`}
              >
                {/* Variant Header Row */}
                <div className="flex items-center justify-between gap-2 mb-3 pb-2 border-b border-slate-200/60">
                  <div className="flex items-center gap-2">
                    <span className="flex items-center justify-center w-5 h-5 rounded-full bg-slate-200 text-slate-700 text-[11px] font-bold">
                      {index + 1}
                    </span>
                    <span className="text-xs font-bold text-slate-800">
                      {resolvedName}
                    </span>
                    <span
                      className={`text-[10px] font-mono px-2 py-0.5 rounded font-semibold ${
                        resolvedSize === null
                          ? 'bg-purple-100 text-purple-800 border border-purple-200'
                          : 'bg-blue-100 text-blue-800 border border-blue-200'
                      }`}
                    >
                      {resolvedSize === null ? 'size: null (STANDARD)' : `size: "${resolvedSize}"`}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    {formData.variants.length > 1 && (
                      <button
                        type="button"
                        onClick={() => handleRemoveVariant(variant.id)}
                        className="p-1 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500 transition-colors"
                        title="Xóa biến thể này"
                        aria-label={`Xóa biến thể ${resolvedName}`}
                      >
                        <Trash2 className="h-4 w-4" aria-hidden="true" />
                      </button>
                    )}
                  </div>
                </div>

                {/* Duplicate Variant Error Alert */}
                {hasAttemptedSubmit && vErr.duplicate && (
                  <div
                    role="alert"
                    className="mb-3 p-2.5 bg-rose-100 border border-rose-300 rounded-lg text-xs font-semibold text-rose-800 flex items-center gap-2"
                  >
                    <AlertCircle className="h-4 w-4 shrink-0" />
                    <span>{vErr.duplicate}</span>
                  </div>
                )}

                {/* Variant Main Fields Grid */}
                {(() => {
                  const sizeTypeId = `${formId}-v-${variant.id}-sizeType`;
                  const customSizeId = `${formId}-v-${variant.id}-customSize`;
                  const nameId = `${formId}-v-${variant.id}-name`;
                  const priceId = `${formId}-v-${variant.id}-price`;
                  const modeId = `${formId}-v-${variant.id}-mode`;
                  const capacityId = `${formId}-v-${variant.id}-capacity`;
                  const maxQtyId = `${formId}-v-${variant.id}-maxQty`;
                  const autoAcceptId = `${formId}-v-${variant.id}-autoAccept`;

                  return (
                    <>
                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                        {/* Size Type Selector */}
                        <div className="space-y-1">
                          <label htmlFor={sizeTypeId} className="block text-xs font-semibold text-slate-700">
                            Kích cỡ <span className="text-rose-500">*</span>
                          </label>
                          <select
                            id={sizeTypeId}
                            value={variant.sizeType}
                            onChange={(e) =>
                              handleVariantChange(
                                variant.id,
                                'sizeType',
                                e.target.value as VariantSizeType
                              )
                            }
                            className="block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-xs text-slate-900 focus-visible:border-emerald-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20"
                          >
                            <option value="STANDARD">STANDARD (Không size - size: null)</option>
                            <option value="M">Size M</option>
                            <option value="L">Size L</option>
                            <option value="CUSTOM">Kích cỡ tùy chỉnh...</option>
                          </select>

                          {/* Custom size input if CUSTOM selected */}
                          {variant.sizeType === 'CUSTOM' && (
                            <div className="pt-1">
                              <label htmlFor={customSizeId} className="sr-only">
                                Kích cỡ tùy chỉnh
                              </label>
                              <input
                                id={customSizeId}
                                type="text"
                                placeholder="Ví dụ: XL, Nhỏ, Chai 500ml"
                                aria-label="Kích cỡ tùy chỉnh"
                                value={variant.customSize || ''}
                                onChange={(e) =>
                                  handleVariantChange(variant.id, 'customSize', e.target.value)
                                }
                                className={`block w-full rounded-lg border px-2.5 py-1 text-xs text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500 ${
                                  hasAttemptedSubmit && vErr.size
                                    ? 'border-rose-400 bg-rose-50'
                                    : 'border-slate-300'
                                }`}
                              />
                              {hasAttemptedSubmit && vErr.size && (
                                <p className="text-[11px] text-rose-600 mt-0.5">
                                  {vErr.size}
                                </p>
                              )}
                            </div>
                          )}
                        </div>

                        {/* Variant Name Input */}
                        <div className="space-y-1">
                          <label htmlFor={nameId} className="block text-xs font-semibold text-slate-700">
                            Tên biến thể <span className="text-rose-500">*</span>
                          </label>
                          <input
                            id={nameId}
                            type="text"
                            value={variant.name}
                            placeholder={
                              variant.sizeType === 'STANDARD' ? 'STANDARD' : `Size ${variant.sizeType}`
                            }
                            onChange={(e) =>
                              handleVariantChange(variant.id, 'name', e.target.value)
                            }
                            className={`block w-full rounded-lg border px-2.5 py-1.5 text-xs text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500 ${
                              hasAttemptedSubmit && vErr.name
                                ? 'border-rose-400 bg-rose-50'
                                : 'border-slate-300'
                            }`}
                          />
                          {hasAttemptedSubmit && vErr.name && (
                            <p className="text-[11px] text-rose-600 mt-0.5">
                              {vErr.name}
                            </p>
                          )}
                        </div>

                        {/* Price Input */}
                        <div className="space-y-1">
                          <label htmlFor={priceId} className="block text-xs font-semibold text-slate-700">
                            Giá bán (VND) <span className="text-rose-500">*</span>
                          </label>
                          <div className="relative">
                            <input
                              id={priceId}
                              type="number"
                              min="1"
                              step="1000"
                              placeholder="35000"
                              value={variant.price}
                              onChange={(e) =>
                                handleVariantChange(
                                  variant.id,
                                  'price',
                                  e.target.value === '' ? '' : Number(e.target.value)
                                )
                              }
                              className={`block w-full rounded-lg border px-2.5 py-1.5 text-xs text-slate-900 pr-12 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500 ${
                                hasAttemptedSubmit && vErr.price
                                  ? 'border-rose-400 bg-rose-50 text-rose-900'
                                  : 'border-slate-300'
                              }`}
                            />
                            <span className="absolute inset-y-0 right-0 pr-2.5 flex items-center text-[11px] text-slate-500 font-mono pointer-events-none">
                              đ
                            </span>
                          </div>
                          {hasAttemptedSubmit && vErr.price ? (
                            <p className="text-[11px] font-medium text-rose-600 mt-0.5">
                              {vErr.price}
                            </p>
                          ) : (
                            variant.price !== '' && (
                              <p className="text-[10px] text-slate-500">
                                {formatVND(Number(variant.price))}
                              </p>
                            )
                          )}
                        </div>

                        {/* Inventory Mode */}
                        <div className="space-y-1">
                          <label htmlFor={modeId} className="block text-xs font-semibold text-slate-700">
                            Chế độ kho <span className="text-rose-500">*</span>
                          </label>
                          <select
                            id={modeId}
                            value={variant.inventoryMode}
                            onChange={(e) =>
                              handleVariantChange(
                                variant.id,
                                'inventoryMode',
                                e.target.value as InventoryMode
                              )
                            }
                            className="block w-full rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-xs text-slate-900 focus-visible:border-emerald-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20"
                          >
                            <option value="MADE_TO_ORDER">MADE_TO_ORDER (Chế biến)</option>
                            <option value="LIMITED_STOCK">LIMITED_STOCK (Tồn kho đóng gói)</option>
                          </select>
                        </div>
                      </div>

                      {/* Variant Configuration Details: Capacity & Auto-accept */}
                      <div className="mt-3 pt-3 border-t border-slate-200/60 grid grid-cols-1 sm:grid-cols-3 gap-3 items-center text-xs">
                        {/* Daily Capacity */}
                        <div className="space-y-1">
                          <div className="flex items-center justify-between">
                            <label htmlFor={capacityId} className="font-semibold text-slate-700 flex items-center gap-1 cursor-pointer">
                              <ChefHat className="h-3.5 w-3.5 text-amber-600" aria-hidden="true" />
                              <span>Công suất mỗi ngày</span>
                            </label>
                            <span className="text-[10px] text-slate-500">
                              {variant.inventoryMode === 'MADE_TO_ORDER' ? 'Khuyên dùng' : 'Tùy chọn'}
                            </span>
                          </div>
                          <input
                            id={capacityId}
                            type="number"
                            min="0"
                            placeholder="Ví dụ: 120"
                            value={variant.dailyCapacityDefault}
                            onChange={(e) =>
                              handleVariantChange(
                                variant.id,
                                'dailyCapacityDefault',
                                e.target.value === '' ? '' : Number(e.target.value)
                              )
                            }
                            className={`block w-full rounded-lg border px-2.5 py-1.5 text-xs text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500 ${
                              hasAttemptedSubmit && vErr.dailyCapacityDefault
                                ? 'border-rose-400 bg-rose-50 text-rose-900'
                                : 'border-slate-300'
                            }`}
                          />
                          {hasAttemptedSubmit && vErr.dailyCapacityDefault && (
                            <p className="text-[11px] font-medium text-rose-600 mt-0.5">
                              {vErr.dailyCapacityDefault}
                            </p>
                          )}
                        </div>

                        {/* Max quantity per order */}
                        <div className="space-y-1">
                          <label htmlFor={maxQtyId} className="font-semibold text-slate-700 block cursor-pointer">
                            Tối đa / 1 đơn hàng
                          </label>
                          <input
                            id={maxQtyId}
                            type="number"
                            min="1"
                            placeholder="10"
                            value={variant.maxQuantityPerOrder}
                            onChange={(e) =>
                              handleVariantChange(
                                variant.id,
                                'maxQuantityPerOrder',
                                e.target.value === '' ? '' : Number(e.target.value)
                              )
                            }
                            className="block w-full rounded-lg border border-slate-300 px-2.5 py-1.5 text-xs text-slate-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500"
                          />
                        </div>

                        {/* Auto-accept override toggle */}
                        <div className="flex items-center justify-between p-2 rounded-lg bg-white border border-slate-200 sm:self-end h-[38px]">
                          <label htmlFor={autoAcceptId} className="font-semibold text-slate-700 flex items-center gap-1 cursor-pointer">
                            <Zap className="h-3.5 w-3.5 text-emerald-600" aria-hidden="true" />
                            <span>Tự động nhận đơn</span>
                          </label>
                          <input
                            id={autoAcceptId}
                            type="checkbox"
                            aria-label="Tự động nhận đơn"
                            checked={variant.autoAcceptOverride}
                            onChange={(e) =>
                              handleVariantChange(
                                variant.id,
                                'autoAcceptOverride',
                                e.target.checked
                              )
                            }
                            className="h-4 w-4 text-emerald-600 rounded border-slate-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-1 cursor-pointer"
                          />
                        </div>
                      </div>
                    </>
                  );
                })()}
              </div>
            );
          })}
        </div>
      </div>

      {/* Form Action Controls */}
      <div className="flex items-center justify-end gap-3 pt-2">
        <Button
          type="button"
          variant="outline"
          onClick={onCancel}
          disabled={isLoading}
        >
          Hủy bỏ
        </Button>
        <Button
          type="submit"
          variant="primary"
          isLoading={isLoading}
          leftIcon={<CheckCircle className="h-4 w-4" aria-hidden="true" />}
        >
          {isLoading ? 'Đang lưu món ăn…' : 'Tạo món ăn & Lưu biến thể'}
        </Button>
      </div>
    </form>
  );
};
