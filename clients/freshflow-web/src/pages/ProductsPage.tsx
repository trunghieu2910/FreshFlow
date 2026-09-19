import React, { useState, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Button,
  TableSkeleton,
  EmptyState,
  ErrorState,
  Pagination,
  useToast,
  ErrorBoundary,
} from '@/components/ui';
import {
  useCatalogProducts,
  ProductCatalogTable,
  CatalogFilterBar,
  CatalogFilterValues,
  normalizeVietnamese,
  ProductCreateModal,
  ProductEditModal,
  CartPreviewModal,
} from '@/features/catalog';
import {
  ProductCatalogDto,
  InventoryMode,
  StoreCategoryDto,
  UpdateProductRequest,
  UpdateProductVariantRequest,
  CreateProductVariantRequest,
} from '@/types/api.types';
import { catalogApi } from '@/api';
import {
  Plus,
  Database,
  Radio,
  ShoppingCart,
  AlertTriangle,
  Bug,
  WifiOff,
  Bell,
  Sparkles,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

// Helper component to intentionally test ErrorBoundary crash without breaking compilation
const CrashTestComponent: React.FC<{ shouldCrash: boolean }> = ({ shouldCrash }) => {
  if (shouldCrash) {
    throw new Error(
      'Lỗi render giả lập: Component con gặp sự cố (Kiểm thử ErrorBoundary bắt lỗi & không làm trắng trang)!'
    );
  }
  return null;
};

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

  const toast = useToast();

  // Failure states simulation for UX Failure Testing (FF-03-05-1)
  const [isTestPanelOpen, setIsTestPanelOpen] = useState(false);
  const [simulateRenderCrash, setSimulateRenderCrash] = useState(false);
  const [simulateApiError, setSimulateApiError] = useState(false);
  const [simulateEmptyList, setSimulateEmptyList] = useState(false);

  // Toggle for testing between Real API mode and PostgreSQL mock fallback
  const [useMockFallback, setUseMockFallback] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [mockProducts, setMockProducts] = useState<ProductCatalogDto[]>(POSTGRESQL_SEED_PRODUCTS);

  // Categories state for dependent availability
  const [categories, setCategories] = useState<StoreCategoryDto[]>([
    { id: 1, storeId, name: 'Cà phê & Trà trái cây', isActive: true, displayOrder: 1 },
    { id: 2, storeId, name: 'Trà sữa & Macchiato', isActive: true, displayOrder: 2 },
    { id: 3, storeId, name: 'Nước ép & Sinh tố', isActive: true, displayOrder: 3 },
    { id: 4, storeId, name: 'Bánh mì & Món ăn nhẹ', isActive: true, displayOrder: 4 },
    { id: 5, storeId, name: 'Tráng miệng & Đồ ngọt', isActive: true, displayOrder: 5 },
  ]);

  // Product Edit & Cart Simulation modals
  const [editingProduct, setEditingProduct] = useState<ProductCatalogDto | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isCartModalOpen, setIsCartModalOpen] = useState(false);

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
    let result = [...mockProducts];

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
  }, [search, variantSize, inventoryMode, availableOnly, sort, currentPage, pageSize, mockProducts]);

  // Derived state depending on mock vs real API mode
  const productsToRender = simulateEmptyList
    ? []
    : useMockFallback
    ? mockProcessed.content
    : data?.content || [];

  const totalPages = simulateEmptyList
    ? 0
    : useMockFallback
    ? mockProcessed.totalPages
    : data?.totalPages || 0;

  const totalElements = simulateEmptyList
    ? 0
    : useMockFallback
    ? mockProcessed.totalElements
    : data?.totalElements || 0;

  const handleEdit = (product: ProductCatalogDto) => {
    setEditingProduct(product);
    setIsEditModalOpen(true);
  };

  // Quick toggle active (Soft Hide / Un-hide)
  const handleToggleActive = async (product: ProductCatalogDto) => {
    const nextActive = !product.active;
    if (!useMockFallback) {
      try {
        await catalogApi.patchProduct(storeId, product.id, { active: nextActive });
        refetch();
        toast.info(
          `Đã ${nextActive ? 'mở bán lại' : 'tạm ẩn mềm'} món "${product.name}" khỏi thực đơn công khai!`,
          { title: 'Trạng thái hiển thị món' }
        );
      } catch (err: unknown) {
        const error = err instanceof Error ? err : new Error(String(err));
        toast.error(error.message || 'Lỗi khi cập nhật trạng thái món', { title: 'Thất bại' });
      }
    } else {
      setMockProducts((prev) =>
        prev.map((p) => (p.id === product.id ? { ...p, active: nextActive } : p))
      );
      toast.info(
        `[Mock] Đã ${nextActive ? 'mở bán lại' : 'tạm ẩn mềm'} món "${product.name}"!`,
        { title: 'Trạng thái hiển thị món' }
      );
    }
  };

  // Soft Delete (is_active = false)
  const handleDelete = async (productId: number) => {
    const prod = (useMockFallback ? mockProducts : productsToRender).find(
      (p) => p.id === productId
    );
    if (
      confirm(
        `Bạn có chắc chắn muốn ẩn mềm món "${prod?.name || productId}" khỏi thực đơn?\n(Quy tắc an toàn: Không hard delete dữ liệu, đơn hàng cũ và giỏ hàng của khách vẫn được bảo lưu)`
      )
    ) {
      if (!useMockFallback) {
        try {
          await catalogApi.deleteProduct(storeId, productId);
          refetch();
          toast.warning(
            `Đã ẩn mềm món #${productId} thành công. Dữ liệu đơn hàng cũ và giỏ hàng được bảo lưu 100%.`,
            { title: 'Đã ẩn mềm món' }
          );
        } catch (err: unknown) {
          const error = err instanceof Error ? err : new Error(String(err));
          toast.error(error.message || 'Lỗi khi xóa món ăn', { title: 'Thất bại' });
        }
      } else {
        setMockProducts((prev) =>
          prev.map((p) => (p.id === productId ? { ...p, active: false } : p))
        );
        toast.warning(`[Mock] Đã ẩn mềm món #${productId} thành công.`, {
          title: 'Đã ẩn mềm món',
        });
      }
    }
  };

  // Toggle category active status to test dependent availability
  const handleToggleCategory = (categoryId: number) => {
    setCategories((prev) =>
      prev.map((c) => (c.id === categoryId ? { ...c, isActive: !c.isActive } : c))
    );
    const cat = categories.find((c) => c.id === categoryId);
    const nextState = cat ? !cat.isActive : true;
    toast.info(
      `Đã ${nextState ? 'kích hoạt mở lại' : 'tạm đóng ẩn mềm'} danh mục "${cat?.name}". Các món thuộc danh mục này sẽ ${nextState ? 'hiển thị lại' : 'tạm ẩn khỏi'} thực đơn!`,
      { title: 'Cập nhật danh mục' }
    );
  };

  // Handle mock update from ProductEditModal
  const handleMockProductUpdate = (
    productId: number,
    productUpdates: UpdateProductRequest,
    variantUpdates: { variantId: number; updates: UpdateProductVariantRequest }[],
    newVariants?: CreateProductVariantRequest[]
  ) => {
    setMockProducts((prev) =>
      prev.map((p) => {
        if (p.id !== productId) return p;
        const updatedVariants = p.variants.map((v) => {
          const vu = variantUpdates.find((u) => u.variantId === v.id);
          if (!vu) return v;
          return {
            ...v,
            name: vu.updates.name !== undefined ? vu.updates.name : v.name,
            price: vu.updates.price !== undefined ? vu.updates.price : v.price,
            inventoryMode: vu.updates.inventoryMode !== undefined ? vu.updates.inventoryMode : v.inventoryMode,
            autoAcceptOverride: vu.updates.autoAcceptOverride !== undefined ? vu.updates.autoAcceptOverride : v.autoAcceptOverride,
            dailyCapacityDefault: vu.updates.dailyCapacityDefault !== undefined ? vu.updates.dailyCapacityDefault : v.dailyCapacityDefault,
            available: vu.updates.available !== undefined ? vu.updates.available : v.available,
            active: vu.updates.active !== undefined ? vu.updates.active : v.active,
          };
        });

        // Add any new variants
        if (newVariants && newVariants.length > 0) {
          newVariants.forEach((nv, idx) => {
            updatedVariants.push({
              id: Date.now() + idx,
              name: nv.name,
              size: nv.size || null,
              price: nv.price,
              inventoryMode: nv.inventoryMode,
              autoAcceptOverride: nv.autoAcceptOverride ?? true,
              dailyCapacityDefault: nv.dailyCapacityDefault ?? 80,
              available: nv.available ?? true,
              active: true,
              availabilityStatus: 'AVAILABLE',
              capacity: nv.inventoryMode === 'MADE_TO_ORDER' ? {
                capacityDate: new Date().toISOString().slice(0, 10),
                remaining: nv.dailyCapacityDefault || 80,
              } : null,
            });
          });
        }

        return {
          ...p,
          name: productUpdates.name !== undefined ? productUpdates.name : p.name,
          description: productUpdates.description !== undefined ? productUpdates.description : p.description,
          imageUrl: productUpdates.imageUrl !== undefined ? productUpdates.imageUrl : p.imageUrl,
          active: productUpdates.active !== undefined ? productUpdates.active : p.active,
          variants: updatedVariants,
        };
      })
    );
    toast.success(`[Mock] Đã cập nhật thành công món ăn #${productId} (PATCH semantics)!`, {
      title: 'Cập nhật thành công',
    });
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
          {/* UX Failure States Testing Button */}
          <Button
            variant="outline"
            size="sm"
            leftIcon={<Bug className="h-4 w-4 text-amber-600" />}
            onClick={() => setIsTestPanelOpen(!isTestPanelOpen)}
            title="Mở bảng điều khiển kiểm thử trực quan các trạng thái lỗi UX, ErrorBoundary và Toast"
            className={isTestPanelOpen ? 'bg-amber-50 border-amber-300 text-amber-900' : ''}
          >
            Kiểm thử UX Failure States{' '}
            {isTestPanelOpen ? (
              <ChevronUp className="h-3.5 w-3.5 ml-1" />
            ) : (
              <ChevronDown className="h-3.5 w-3.5 ml-1" />
            )}
          </Button>

          {/* Cart & History Simulation Button */}
          <Button
            variant="outline"
            size="sm"
            leftIcon={<ShoppingCart className="h-4 w-4 text-emerald-600" />}
            onClick={() => setIsCartModalOpen(true)}
            title="Xem mô phỏng giỏ hàng khách hàng và lịch sử đơn hàng khi Category/Product/Variant bị ẩn mềm"
          >
            Thử nghiệm Giỏ hàng & Lịch sử
          </Button>

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

      {/* Visual UX Failure States Testing Panel (FF-03-05-1) */}
      {isTestPanelOpen && (
        <div className="bg-gradient-to-r from-amber-50/90 via-orange-50/70 to-rose-50/90 border border-amber-200/80 rounded-2xl p-4 shadow-sm space-y-3 animate-fadeIn">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="p-1.5 bg-amber-500 text-white rounded-lg shadow-xs">
                <Bug className="h-4 w-4" />
              </div>
              <div>
                <h3 className="text-xs sm:text-sm font-bold text-slate-900">
                  Bảng Điều Khiển Kiểm Thử Trực Quan UX Failure States (FF-03-05-1)
                </h3>
                <p className="text-[11px] text-slate-500">
                  Kiểm chứng: ErrorBoundary bắt lỗi render không làm trắng trang, ErrorState khi mất API, EmptyState có CTA, và Toast phản hồi mọi mutation.
                </p>
              </div>
            </div>
            <span className="text-[10px] font-mono font-bold bg-amber-200/60 text-amber-900 px-2 py-0.5 rounded-full">
              Testing Mode
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2.5 pt-1">
            {/* Test 1: Render Crash / ErrorBoundary */}
            <div className="bg-white/90 p-3 rounded-xl border border-amber-200 flex flex-col justify-between">
              <div>
                <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5 mb-1">
                  <AlertTriangle className="h-3.5 w-3.5 text-rose-500" />
                  1. Lỗi Render Crash
                </span>
                <p className="text-[11px] text-slate-500 mb-2 leading-relaxed">
                  Cố tình ném lỗi trong quá trình render để kiểm chứng ErrorBoundary chặn trắng trang.
                </p>
              </div>
              <Button
                size="sm"
                variant="danger"
                className="w-full text-xs"
                onClick={() => setSimulateRenderCrash(true)}
              >
                Kích hoạt Render Crash
              </Button>
            </div>

            {/* Test 2: Offline / API 500 Error */}
            <div className="bg-white/90 p-3 rounded-xl border border-amber-200 flex flex-col justify-between">
              <div>
                <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5 mb-1">
                  <WifiOff className="h-3.5 w-3.5 text-rose-500" />
                  2. Tắt API / Mất mạng
                </span>
                <p className="text-[11px] text-slate-500 mb-2 leading-relaxed">
                  Giả lập backend Spring Boot tắt hoặc mất mạng để kiểm tra ErrorState và nút Thử lại.
                </p>
              </div>
              <Button
                size="sm"
                variant={simulateApiError ? 'primary' : 'outline'}
                className="w-full text-xs"
                onClick={() => setSimulateApiError(!simulateApiError)}
              >
                {simulateApiError ? 'Tắt Giả lập Lỗi API' : 'Bật Giả lập Lỗi API'}
              </Button>
            </div>

            {/* Test 3: Empty State with Dual CTA */}
            <div className="bg-white/90 p-3 rounded-xl border border-amber-200 flex flex-col justify-between">
              <div>
                <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5 mb-1">
                  <Sparkles className="h-3.5 w-3.5 text-amber-500" />
                  3. Danh Sách Trống
                </span>
                <p className="text-[11px] text-slate-500 mb-2 leading-relaxed">
                  Giả lập không có món nào để kiểm tra EmptyState với nút Tạo món mới & Xóa bộ lọc.
                </p>
              </div>
              <Button
                size="sm"
                variant={simulateEmptyList ? 'primary' : 'outline'}
                className="w-full text-xs"
                onClick={() => setSimulateEmptyList(!simulateEmptyList)}
              >
                {simulateEmptyList ? 'Tắt Giả lập Trống' : 'Bật Giả lập Trống'}
              </Button>
            </div>

            {/* Test 4: Toast Trigger */}
            <div className="bg-white/90 p-3 rounded-xl border border-amber-200 flex flex-col justify-between">
              <div>
                <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5 mb-1">
                  <Bell className="h-3.5 w-3.5 text-sky-500" />
                  4. Bắn 4 Loại Toast
                </span>
                <p className="text-[11px] text-slate-500 mb-2 leading-relaxed">
                  Kích hoạt đồng thời 4 toast: Success, Info, Warning, Error để kiểm tra animation & auto-dismiss.
                </p>
              </div>
              <Button
                size="sm"
                variant="outline"
                className="w-full text-xs text-sky-700 hover:bg-sky-50"
                onClick={() => {
                  toast.success('Thao tác tạo món thành công!', { title: 'Thành công (4s)' });
                  toast.info('Hệ thống đang đồng bộ 5 biến thể.', { title: 'Thông tin (4s)' });
                  toast.warning('Món ăn sắp đạt công suất tối đa hôm nay!', { title: 'Cảnh báo (5s)' });
                  toast.error('Không thể kết nối đến máy chủ thanh toán!', { title: 'Lỗi hệ thống (6s)' });
                }}
              >
                Bắn 4 Toast Mẫu
              </Button>
            </div>
          </div>
        </div>
      )}

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
              (isError || simulateApiError) && !useMockFallback ? 'text-rose-500 animate-pulse' : 'text-emerald-500'
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
          <span>Nguồn: {useMockFallback ? 'PostgreSQL Mock' : simulateApiError ? 'API Error Simulated' : 'Spring Boot API'}</span>
        </div>
      </div>

      {/* Content Area Lifecycle Management */}
      {!useMockFallback && isLoading && !simulateApiError && (
        <div className="space-y-3">
          <div className="flex items-center justify-between text-xs font-semibold text-slate-500">
            <span>Đang tải danh sách món ăn từ máy chủ…</span>
          </div>
          <TableSkeleton rows={5} />
        </div>
      )}

      {/* Simulated or Real API Error State */}
      {(!useMockFallback && isError && !simulateApiError) && (
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

      {simulateApiError && (
        <ErrorState
          title="Không thể kết nối với máy chủ API (Giả lập Offline)"
          message="Dịch vụ backend Spring Boot tại http://localhost:8080 đang bị ngắt kết nối để kiểm thử visual state của ứng dụng khi gặp sự cố."
          onRetry={() => {
            setSimulateApiError(false);
            refetch();
            toast.info('Đang thử kết nối lại với máy chủ Spring Boot...', { title: 'Thử lại' });
          }}
          isRetrying={isFetching}
        />
      )}

      {(useMockFallback || (!isLoading && !isError && !simulateApiError)) && (
        <ErrorBoundary
          title="Không thể hiển thị bảng danh sách món ăn"
          description="Đã xảy ra sự cố kết xuất dữ liệu trong bảng thực đơn. ErrorBoundary đã ngăn chặn hiện tượng trắng trang để bảo vệ giao diện của bạn."
          onReset={() => {
            setSimulateRenderCrash(false);
            toast.info('Đã đặt lại trạng thái hiển thị của bảng món ăn.', { title: 'Phục hồi giao diện' });
          }}
        >
          <CrashTestComponent shouldCrash={simulateRenderCrash} />

          {productsToRender.length === 0 ? (
            <EmptyState
              variant={search || variantSize || inventoryMode || availableOnly ? 'search' : 'default'}
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
              secondaryActionLabel={simulateEmptyList ? 'Tắt giả lập trống' : undefined}
              onSecondaryAction={simulateEmptyList ? () => setSimulateEmptyList(false) : undefined}
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
                onToggleActive={handleToggleActive}
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
        </ErrorBoundary>
      )}

      {/* Product Create Modal with Nested Variants */}
      <ProductCreateModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        storeId={storeId}
        isMockMode={useMockFallback}
        onSuccess={(created) => {
          refetch();
          toast.success(
            `Đã tạo thành công món "${created.name}" với ${created.variants?.length || 0} biến thể!`,
            { title: 'Tạo món thành công' }
          );
        }}
        onMockCreate={(formData) => {
          const newId = Date.now();
          const newMockProduct: ProductCatalogDto = {
            id: newId,
            name: formData.name,
            description: formData.description || null,
            imageUrl: formData.imageUrl || null,
            active: formData.active,
            variants: formData.variants.map((v, i) => {
              const size =
                v.sizeType === 'STANDARD'
                  ? null
                  : v.sizeType === 'CUSTOM'
                  ? v.customSize || null
                  : v.sizeType;
              const name =
                v.name ||
                (v.sizeType === 'STANDARD' ? 'STANDARD' : `Size ${v.sizeType}`);
              return {
                id: newId * 10 + i,
                name,
                size,
                price: Number(v.price),
                inventoryMode: v.inventoryMode,
                autoAcceptOverride: v.autoAcceptOverride,
                dailyCapacityDefault:
                  v.dailyCapacityDefault !== ''
                    ? Number(v.dailyCapacityDefault)
                    : null,
                available: v.available,
                active: true,
                availabilityStatus: 'AVAILABLE',
                capacity:
                  v.inventoryMode === 'MADE_TO_ORDER'
                    ? {
                        capacityDate: new Date().toISOString().slice(0, 10),
                        remaining: Number(v.dailyCapacityDefault) || 100,
                      }
                    : null,
              };
            }),
          };
          setMockProducts((prev) => [newMockProduct, ...prev]);
          toast.success(
            `[Mock PostgreSQL] Đã thêm món "${newMockProduct.name}" với ${newMockProduct.variants.length} biến thể!`,
            { title: 'Tạo món thành công' }
          );
        }}
      />

      {/* Product Edit Modal (PATCH Semantics & Soft Hide) */}
      <ProductEditModal
        isOpen={isEditModalOpen}
        onClose={() => {
          setIsEditModalOpen(false);
          setEditingProduct(null);
        }}
        product={editingProduct}
        categories={categories}
        storeId={storeId}
        isMockMode={useMockFallback}
        onSuccess={(updated) => {
          refetch();
          toast.success(`Đã cập nhật món "${updated.name}" thành công!`, {
            title: 'Cập nhật thành công',
          });
        }}
        onMockUpdate={handleMockProductUpdate}
      />

      {/* Customer Cart & Order History Simulation Modal (Dependent Availability) */}
      <CartPreviewModal
        isOpen={isCartModalOpen}
        onClose={() => setIsCartModalOpen(false)}
        categories={categories}
        products={useMockFallback ? mockProducts : productsToRender}
        onToggleCategory={handleToggleCategory}
        onToggleProduct={(id) => {
          const prod = (useMockFallback ? mockProducts : productsToRender).find(
            (p) => p.id === id
          );
          if (prod) handleToggleActive(prod);
        }}
      />
    </div>
  );
};
