import React, { useState, useEffect } from 'react';
import { Search, X, RotateCcw, ArrowUpDown, Check } from 'lucide-react';
import { useDebounce } from '@/hooks';
import { InventoryMode } from '@/types/api.types';
import { cn } from '@/lib/utils';

export interface CatalogFilterValues {
  search?: string;
  storeCategoryId?: number;
  variantSize?: string;
  inventoryMode?: InventoryMode;
  availableOnly?: boolean;
  sort?: string;
}

export interface CatalogFilterBarProps {
  values: CatalogFilterValues;
  onChange: (updates: Partial<CatalogFilterValues>) => void;
  onReset: () => void;
  isLoading?: boolean;
  className?: string;
}

const SIZE_OPTIONS = [
  { label: 'Tất cả kích cỡ', value: '' },
  { label: 'Size M', value: 'M' },
  { label: 'Size L', value: 'L' },
  { label: 'STANDARD', value: 'STANDARD' },
];

const SORT_OPTIONS = [
  { label: 'Tên: A → Z', value: 'name,asc' },
  { label: 'Tên: Z → A', value: 'name,desc' },
  { label: 'Giá: Thấp đến Cao', value: 'price,asc' },
  { label: 'Giá: Cao đến Thấp', value: 'price,desc' },
];

const INVENTORY_OPTIONS: { label: string; value: InventoryMode | '' }[] = [
  { label: 'Mọi mô hình kho', value: '' },
  { label: 'Nấu theo order (Kitchen)', value: 'MADE_TO_ORDER' },
  { label: 'Kho vật lý giới hạn', value: 'LIMITED_STOCK' },
];

export const CatalogFilterBar: React.FC<CatalogFilterBarProps> = ({
  values,
  onChange,
  onReset,
  isLoading = false,
  className,
}) => {
  // Local state for instant input feedback
  const [searchInput, setSearchInput] = useState(values.search || '');
  const debouncedSearch = useDebounce(searchInput, 300);

  // Sync external search updates (e.g. from URL or Reset) to local input
  useEffect(() => {
    setSearchInput(values.search || '');
  }, [values.search]);

  // Push debounced search value to parent handler
  useEffect(() => {
    const currentParam = values.search || '';
    const nextParam = debouncedSearch.trim();

    if (nextParam !== currentParam) {
      onChange({ search: nextParam || undefined });
    }
  }, [debouncedSearch, values.search, onChange]);

  const handleClearSearch = () => {
    setSearchInput('');
    onChange({ search: undefined });
  };

  const isFiltered = Boolean(
    values.search ||
    values.variantSize ||
    values.inventoryMode ||
    values.storeCategoryId ||
    values.availableOnly ||
    (values.sort && values.sort !== 'name,asc')
  );

  return (
    <div
      className={cn(
        'bg-white p-4 rounded-2xl border border-slate-200 shadow-xs space-y-4',
        className
      )}
    >
      {/* Top row: Search input & Sort dropdown */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
        {/* Search input with Debounce & Clear */}
        <div className="relative flex-1">
          <label htmlFor="catalog-search" className="sr-only">
            Tìm kiếm theo tên món ăn
          </label>
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
            <Search className="h-4 w-4" aria-hidden="true" />
          </div>
          <input
            id="catalog-search"
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Tìm kiếm theo tên món ăn, hương vị…"
            aria-label="Tìm kiếm theo tên món ăn"
            className={cn(
              'block w-full rounded-xl border bg-slate-50/50 pl-9 pr-9 py-2 text-sm text-slate-900 placeholder:text-slate-400 transition-colors',
              'focus-visible:outline-none focus-visible:bg-white focus-visible:border-emerald-500 focus-visible:ring-2 focus-visible:ring-emerald-500/20',
              'border-slate-300 hover:border-slate-400'
            )}
          />
          {searchInput && (
            <button
              type="button"
              onClick={handleClearSearch}
              aria-label="Xóa từ khóa tìm kiếm"
              className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 rounded-md transition-colors"
            >
              <X className="h-4 w-4" aria-hidden="true" />
            </button>
          )}
        </div>

        {/* Sort Dropdown */}
        <div className="flex items-center gap-2">
          <label htmlFor="catalog-sort" className="sr-only">
            Sắp xếp theo
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-2.5 flex items-center pointer-events-none text-slate-500">
              <ArrowUpDown className="h-3.5 w-3.5" aria-hidden="true" />
            </div>
            <select
              id="catalog-sort"
              value={values.sort || 'name,asc'}
              onChange={(e) => onChange({ sort: e.target.value })}
              className={cn(
                'appearance-none pl-8 pr-8 py-2 rounded-xl border border-slate-300 bg-white text-xs font-semibold text-slate-700 shadow-xs cursor-pointer',
                'focus-visible:outline-none focus-visible:border-emerald-500 focus-visible:ring-2 focus-visible:ring-emerald-500/20 hover:border-slate-400 transition-colors'
              )}
            >
              {SORT_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {/* Reset Filters button */}
          {isFiltered && (
            <button
              type="button"
              onClick={onReset}
              aria-label="Đặt lại tất cả bộ lọc"
              className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 text-xs font-semibold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500 focus-visible:ring-offset-1 transition-colors shrink-0"
            >
              <RotateCcw className="h-3.5 w-3.5" aria-hidden="true" />
              <span>Đặt lại</span>
            </button>
          )}
        </div>
      </div>

      {/* Bottom row: Filter chips (Size, Inventory Mode, Availability) */}
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2.5 pt-1 border-t border-slate-100 text-xs">
        {/* Size Filter Chips */}
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="font-semibold text-slate-500 select-none mr-0.5">Kích cỡ:</span>
          {SIZE_OPTIONS.map((opt) => {
            const isSelected = (values.variantSize || '') === opt.value;
            return (
              <button
                key={opt.value}
                type="button"
                onClick={() => onChange({ variantSize: opt.value || undefined })}
                className={cn(
                  'px-2.5 py-1 rounded-lg border text-xs font-medium transition-all select-none',
                  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-1',
                  isSelected
                    ? 'bg-emerald-50 border-emerald-500 text-emerald-800 font-semibold shadow-2xs'
                    : 'bg-white border-slate-200 text-slate-600 hover:border-slate-300 hover:bg-slate-50'
                )}
              >
                {opt.label}
              </button>
            );
          })}
        </div>

        <div className="h-4 w-px bg-slate-200 hidden sm:block" />

        {/* Inventory Mode Selector */}
        <div className="flex items-center gap-1.5">
          <label htmlFor="inventory-mode-select" className="font-semibold text-slate-500 select-none">
            Kho:
          </label>
          <select
            id="inventory-mode-select"
            value={values.inventoryMode || ''}
            onChange={(e) =>
              onChange({
                inventoryMode: (e.target.value as InventoryMode) || undefined,
              })
            }
            className="rounded-lg border border-slate-200 bg-white px-2 py-1 text-xs text-slate-700 hover:border-slate-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/20 focus-visible:border-emerald-500"
          >
            {INVENTORY_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        <div className="h-4 w-px bg-slate-200 hidden sm:block" />

        {/* Available Only Toggle */}
        <label htmlFor="catalog-available-only" className="inline-flex items-center gap-2 cursor-pointer select-none">
          <input
            id="catalog-available-only"
            type="checkbox"
            checked={Boolean(values.availableOnly)}
            onChange={(e) => onChange({ availableOnly: e.target.checked || undefined })}
            className="sr-only peer"
          />
          <div
            className={cn(
              'h-4 w-4 rounded border flex items-center justify-center transition-colors',
              'peer-focus-visible:ring-2 peer-focus-visible:ring-emerald-500 peer-focus-visible:ring-offset-2',
              values.availableOnly
                ? 'bg-emerald-600 border-emerald-600 text-white'
                : 'bg-white border-slate-300 hover:border-slate-400'
            )}
          >
            {values.availableOnly && <Check className="h-3 w-3 stroke-[3]" />}
          </div>
          <span className="font-medium text-slate-700 hover:text-slate-900">
            Chỉ hiện món còn bán
          </span>
        </label>

        {/* Loading / syncing indicator */}
        {isLoading && (
          <span className="ml-auto text-emerald-600 text-[11px] font-medium animate-pulse">
            Đang lọc dữ liệu…
          </span>
        )}
      </div>
    </div>
  );
};
