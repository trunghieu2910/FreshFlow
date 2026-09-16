import React, { useState, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Button,
  Input,
  Modal,
  TableSkeleton,
  EmptyState,
  ErrorState,
  Pagination,
} from '@/components/ui';
import {
  useCatalogProducts,
  ProductCatalogTable,
  CatalogFilterBar,
  CatalogFilterValues,
  normalizeVietnamese,
} from '@/features/catalog';
import {
  ProductCatalogDto,
  InventoryMode,
} from '@/types/api.types';
import {
  Plus,
  CheckCircle2,
  Database,
  Radio,
} from 'lucide-react';

// Sample PostgreSQL Seed data fallback for manual review and offline testing
const POSTGRESQL_SEED_PRODUCTS: ProductCatalogDto[] = [
  {
    id: 1,
    name: 'Trà Đào Cam Sả',
    description: 'Trà đào thơm thanh mát kết hợp lát cam vàng và hương sả tự nhiên',
    imageUrl: 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=150',
    active: true,
    variants: [
      {
        id: 10,
        name: 'Trà Đào Cam Sả - Size M',
        size: 'M',
        price: 35000,
        inventoryMode: 'MADE_TO_ORDER',
        autoAcceptOverride: true,
        dailyCapacityDefault: 100,
        available: true,
        active: true,
        availabilityStatus: 'AVAILABLE',
        capacity: { capacityDate: '2026-09-16', remaining: 85 },
      },
      {
        id: 11,
        name: 'Trà Đào Cam Sả - Size L',
        size: 'L',
        price: 45000,
        inventoryMode: 'MADE_TO_ORDER',
        autoAcceptOverride: true,
        dailyCapacityDefault: 80,
        available: true,
        active: true,
        availabilityStatus: 'AVAILABLE',
        capacity: { capacityDate: '2026-09-16', remaining: 62 },
      },
    ],
  },
  {
    id: 2,
    name: 'Cà Phê Sữa Đá Sài Gòn',
    description: 'Cà phê rang mộc Robusta Buôn Ma Thuột pha phin truyền thống hòa quyện sữa đặc',
    imageUrl: 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=150',
    active: true,
    variants: [
      {
        id: 20,
        name: 'Cà Phê Sữa Đá - STANDARD',
        size: null,
        price: 28000,
        inventoryMode: 'MADE_TO_ORDER',
        autoAcceptOverride: true,
        dailyCapacityDefault: 120,
        available: false,
        active: true,
        availabilityStatus: 'CAPACITY_EXHAUSTED',
        capacity: { capacityDate: '2026-09-16', remaining: 0 },
      },
    ],
  },
  {
    id: 3,
    name: 'Bánh Mì Chảo Đặc Biệt',
    description: 'Bánh mì giòn nóng ăn kèm xíu mại, trứng ốp la, pate gan béo ngậy và sốt cà chua',
    imageUrl: 'https://images.unsplash.com/photo-1509722747041-616f39b57569?w=150',
    active: false,
    variants: [
      {
        id: 30,
        name: 'Bánh Mì Chảo - STANDARD',
        size: null,
        price: 55000,
        inventoryMode: 'LIMITED_STOCK',
        autoAcceptOverride: false,
        dailyCapacityDefault: 50,
        available: false,
        active: false,
        availabilityStatus: 'MARKED_UNAVAILABLE',
        capacity: null,
      },
    ],
  },
  {
    id: 4,
    name: 'Trà Sữa Oolong Nướng',
    description: 'Trà Oolong rang thơm khói nồng nàn kết hợp sữa tươi thanh trùng',
    imageUrl: 'https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=150',
    active: true,
    variants: [
      {
        id: 40,
        name: 'Trà Sữa Oolong - Size M',
        size: 'M',
        price: 38000,
        inventoryMode: 'MADE_TO_ORDER',
        autoAcceptOverride: true,
        dailyCapacityDefault: 150,
        available: true,
        active: true,
        availabilityStatus: 'AVAILABLE',
        capacity: { capacityDate: '2026-09-16', remaining: 110 },
      },
      {
        id: 41,
        name: 'Trà Sữa Oolong - Size L',
        size: 'L',
        price: 48000,
        inventoryMode: 'MADE_TO_ORDER',
        autoAcceptOverride: true,
        dailyCapacityDefault: 100,
        available: true,
        active: true,
        availabilityStatus: 'AVAILABLE',
        capacity: { capacityDate: '2026-09-16', remaining: 75 },
      },
    ],
  },
  {
    id: 5,
    name: 'Nước Ép Ổi Hồng Tươi',
    description: 'Ổi hồng miền Tây tươi ngon ép nguyên chất giàu vitamin C',
    imageUrl: 'https://images.unsplash.com/photo-1534353473418-4cfa6c56fd38?w=150',
    active: true,
    variants: [
      {
        id: 50,
        name: 'Nước Ép Ổi - STANDARD',
        size: null,
        price: 32000,
        inventoryMode: 'LIMITED_STOCK',
        autoAcceptOverride: false,
        dailyCapacityDefault: 40,
        available: true,
        active: true,
        availabilityStatus: 'AVAILABLE',
        capacity: null,
      },
    ],
  },
];

export const ProductsPage: React.FC = () => {
  const storeId = 1; // Current active merchant store ID
  const pageSize = 10;

  // Read URL search params for 2-way data binding
  const [searchParams, setSearchParams] = useSearchParams();

  const search = searchParams.get('search') || '';
  const variantSize = searchParams.get('variantSize') || undefined;
  const inventoryMode = (searchParams.get('inventoryMode') as InventoryMode) || undefined;
  const availableOnly = searchParams.get('availableOnly') === 'true';
  const sort = searchParams.get('sort') || 'name,asc';
  const rawPage = parseInt(searchParams.get('page') || '1', 10);
  const currentPage = isNaN(rawPage) || rawPage < 1 ? 1 : rawPage;

  // Filter criteria passed to Spring Boot backend
  const filterValues: CatalogFilterValues = useMemo(
    () => ({
      search: search || undefined,
      variantSize,
      inventoryMode,
      availableOnly: availableOnly || undefined,
      sort,
    }),
    [search, variantSize, inventoryMode, availableOnly, sort]
  );

  // TanStack React Query connecting to real Spring Boot API (0-indexed page)
  const {
    data,
    isLoading,
    isError,
    error,
    refetch,
    isFetching,
  } = useCatalogProducts({
    storeId,
    criteria: filterValues,
    page: currentPage - 1, // Translate 1-indexed URL page to Spring Boot 0-indexed page
    size: pageSize,
    sort,
  });

  // Toggle for testing between Real API mode and PostgreSQL mock fallback
  const [useMockFallback, setUseMockFallback] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // When updating filters, update URL params and reset page to 1
  const handleFilterChange = (updates: Partial<CatalogFilterValues>) => {
    const nextParams = new URLSearchParams(searchParams);

    Object.entries(updates).forEach(([key, val]) => {
      if (val === undefined || val === '' || val === false) {
        nextParams.delete(key);
      } else {
        nextParams.set(key, String(val));
      }
    });

    // Reset pagination to page 1 on filter/search change
    nextParams.delete('page');

    setSearchParams(nextParams, { replace: true });
  };

  // Handle pagination navigation
  const handlePageChange = (newPage: number) => {
    const nextParams = new URLSearchParams(searchParams);
    if (newPage <= 1) {
      nextParams.delete('page');
    } else {
      nextParams.set('page', String(newPage));
    }
    setSearchParams(nextParams);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Reset all filters to default
  const handleResetFilters = () => {
    const nextParams = new URLSearchParams();
    setSearchParams(nextParams, { replace: true });
  };

  // In-memory filter, sort, and pagination for PostgreSQL mock preview
  const mockProcessed = useMemo(() => {
    let result = [...POSTGRESQL_SEED_PRODUCTS];

    if (search) {
      const q = normalizeVietnamese(search);
      result = result.filter(
        (p) =>
          normalizeVietnamese(p.name).includes(q) ||
          (p.description && normalizeVietnamese(p.description).includes(q))
      );
    }

    if (variantSize) {
      result = result.filter((p) =>
        p.variants.some((v) =>
          variantSize === 'STANDARD' ? !v.size || v.size === 'STANDARD' : v.size === variantSize
        )
      );
    }

    if (inventoryMode) {
      result = result.filter((p) =>
        p.variants.some((v) => v.inventoryMode === inventoryMode)
      );
    }

    if (availableOnly) {
      result = result.filter(
        (p) => p.active && p.variants.some((v) => v.available)
      );
    }

    // Sorting
    const [sortField, sortDir] = sort.split(',');
    result.sort((a, b) => {
      if (sortField === 'price') {
        const priceA = a.variants[0]?.price || 0;
        const priceB = b.variants[0]?.price || 0;
        return sortDir === 'desc' ? priceB - priceA : priceA - priceB;
      }
      return sortDir === 'desc'
        ? b.name.localeCompare(a.name)
        : a.name.localeCompare(b.name);
    });

    const totalElements = result.length;
    const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));
    const paginated = result.slice((currentPage - 1) * pageSize, currentPage * pageSize);

    return {
      content: paginated,
      totalElements,
      totalPages,
    };
  }, [search, variantSize, inventoryMode, availableOnly, sort, currentPage, pageSize]);

  // Derived state depending on mock vs real API mode
  const productsToRender = useMockFallback
    ? mockProcessed.content
    : data?.content || [];

  const totalPages = useMockFallback
    ? mockProcessed.totalPages
    : data?.totalPages || 0;

  const totalElements = useMockFallback
    ? mockProcessed.totalElements
    : data?.totalElements || 0;

  const handleEdit = (product: ProductCatalogDto) => {
    alert(`Chỉnh sửa món: ${product.name} (ID: ${product.id})`);
  };

  const handleDelete = (productId: number) => {
    if (confirm(`Bạn có chắc chắn muốn ngừng bán món ăn #${productId}?`)) {
      alert(`Đã gửi yêu cầu ngừng bán món #${productId}`);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Controls Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-white p-4 rounded-2xl border border-slate-200 shadow-xs">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Quản lý Thực đơn Món ăn</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Xem danh sách, tìm kiếm, lọc theo kích cỡ và trạng thái công suất món ăn.
          </p>
        </div>

        {/* Action Controls & Mode Switcher */}
        <div className="flex items-center gap-2 flex-wrap w-full sm:w-auto justify-end">
          {/* Mock Fallback Switcher for Reviewing when backend is off */}
          <button
            type="button"
            onClick={() => setUseMockFallback(!useMockFallback)}
            className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border text-xs font-semibold transition-colors ${
              useMockFallback
                ? 'bg-amber-50 text-amber-800 border-amber-300'
                : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
            }`}
            title="Bật để xem trước dữ liệu mẫu PostgreSQL khi backend Spring Boot đang tắt"
          >
            <Database className="h-3.5 w-3.5 text-amber-600" aria-hidden="true" />
            <span>Xem Mock PostgreSQL ({useMockFallback ? 'BẬT' : 'TẮT'})</span>
          </button>

          <Button
            variant="primary"
            size="sm"
            leftIcon={<Plus className="h-4 w-4" aria-hidden="true" />}
            onClick={() => setIsModalOpen(true)}
          >
            Thêm món mới
          </Button>
        </div>
      </div>

      {/* Filter & Search Bar with URL Query State Binding */}
      <CatalogFilterBar
        values={filterValues}
        onChange={handleFilterChange}
        onReset={handleResetFilters}
        isLoading={isFetching}
      />

      {/* Backend Status Notification Strip */}
      <div className="flex items-center justify-between px-4 py-2 bg-slate-100/80 rounded-xl border border-slate-200 text-xs text-slate-600">
        <div className="flex items-center gap-2">
          <Radio
            className={`h-3.5 w-3.5 ${
              isError && !useMockFallback ? 'text-rose-500 animate-pulse' : 'text-emerald-500'
            }`}
            aria-hidden="true"
          />
          <span>
            API Endpoint:{' '}
            <code className="font-mono bg-white px-1.5 py-0.5 rounded border border-slate-200 text-slate-800 font-semibold">
              GET /api/v1/stores/{storeId}/products
            </code>
          </span>
        </div>

        <div className="flex items-center gap-3 font-mono text-[11px]">
          {isFetching && <span className="text-emerald-600 animate-pulse">Đang đồng bộ…</span>}
          <span>Nguồn: {useMockFallback ? 'PostgreSQL Mock' : 'Spring Boot API'}</span>
        </div>
      </div>

      {/* Content Area Lifecycle Management */}
      {!useMockFallback && isLoading && (
        <div className="space-y-3">
          <div className="flex items-center justify-between text-xs font-semibold text-slate-500">
            <span>Đang tải danh sách món ăn từ máy chủ…</span>
          </div>
          <TableSkeleton rows={5} />
        </div>
      )}

      {!useMockFallback && isError && (
        <ErrorState
          title="Không thể kết nối với máy chủ API"
          message={
            error?.message ||
            'Dịch vụ backend Spring Boot tại http://localhost:8080 đang tắt hoặc gặp sự cố kết nối (Manual Backend-off Test).'
          }
          onRetry={() => refetch()}
          isRetrying={isFetching}
        />
      )}

      {(useMockFallback || (!isLoading && !isError)) && (
        <>
          {productsToRender.length === 0 ? (
            <EmptyState
              title={
                search || variantSize || inventoryMode || availableOnly
                  ? 'Không tìm thấy món ăn phù hợp'
                  : 'Chưa có món ăn nào trong thực đơn'
              }
              description={
                search || variantSize || inventoryMode || availableOnly
                  ? 'Không có kết quả khớp với điều kiện lọc hiện tại. Hãy thử thay đổi từ khóa hoặc đặt lại bộ lọc.'
                  : 'Chi nhánh này hiện chưa tạo món ăn nào. Hãy nhấn vào nút bên dưới để tạo món đầu tiên cho cửa hàng.'
              }
              actionLabel={
                search || variantSize || inventoryMode || availableOnly
                  ? 'Đặt lại bộ lọc'
                  : '+ Thêm món ăn ngay'
              }
              onAction={
                search || variantSize || inventoryMode || availableOnly
                  ? handleResetFilters
                  : () => setIsModalOpen(true)
              }
            />
          ) : (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold text-sm text-slate-900 flex items-center gap-2">
                  <span>Danh sách Món ăn Chi nhánh</span>
                  <span className="text-xs font-normal text-slate-500">
                    ({totalElements} món)
                  </span>
                </h3>

                {useMockFallback && (
                  <span className="text-xs bg-amber-100 text-amber-800 font-semibold px-2 py-0.5 rounded border border-amber-200">
                    Đang xem mẫu Mock PostgreSQL
                  </span>
                )}
              </div>

              {/* Products Table */}
              <ProductCatalogTable
                products={productsToRender}
                onEdit={handleEdit}
                onDelete={handleDelete}
              />

              {/* Pagination Controls */}
              {totalPages > 1 && (
                <Pagination
                  currentPage={currentPage}
                  totalPages={totalPages}
                  totalElements={totalElements}
                  pageSize={pageSize}
                  onPageChange={handlePageChange}
                  disabled={isFetching}
                />
              )}
            </div>
          )}
        </>
      )}

      {/* Modal Add Product */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Thêm món ăn mới vào thực đơn"
        description="Điền thông tin món ăn và các biến thể kích cỡ."
        size="md"
        footer={
          <>
            <Button variant="outline" onClick={() => setIsModalOpen(false)}>
              Hủy bỏ
            </Button>
            <Button
              variant="primary"
              leftIcon={<CheckCircle2 className="h-4 w-4" aria-hidden="true" />}
              onClick={() => {
                alert('Tạo món thành công!');
                setIsModalOpen(false);
              }}
            >
              Lưu món ăn
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Input
            label="Tên món ăn"
            placeholder="Ví dụ: Trà đào cam sả"
            defaultValue="Trà sữa Matcha Macchiato"
            required
          />
          <Input
            label="Giá cơ sở (VND)"
            type="number"
            placeholder="Ví dụ: 35000"
            defaultValue="45000"
            required
          />
          <div className="p-3 bg-emerald-50 rounded-lg border border-emerald-200 text-xs text-emerald-800">
            💡 Dữ liệu món ăn sẽ được gửi tới <code>POST /api/v1/merchant/stores/{storeId}/products</code>.
          </div>
        </div>
      </Modal>
    </div>
  );
};
