import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ProductCatalogTable } from '../components/ProductCatalogTable';
import { CatalogFilterBar } from '../components/CatalogFilterBar';
import { ProductCreateForm } from '../components/ProductCreateForm';
import { ProductCatalogDto } from '@/types/api.types';

// Mock sample products for testing
const MOCK_PRODUCTS: ProductCatalogDto[] = [
  {
    id: 1,
    name: 'Trà Đào Cam Sả',
    description: 'Trà đào thơm thanh mát',
    imageUrl: 'https://example.com/tra-dao.jpg',
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
        capacity: { capacityDate: '2026-09-19', remaining: 85 },
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
        capacity: { capacityDate: '2026-09-19', remaining: 62 },
      },
    ],
  },
  {
    id: 2,
    name: 'Cà Phê Sữa Đá Sài Gòn',
    description: 'Cà phê rang mộc Robusta',
    imageUrl: null,
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
        available: true,
        active: true,
        availabilityStatus: 'CAPACITY_EXHAUSTED',
        capacity: { capacityDate: '2026-09-19', remaining: 0 },
      },
    ],
  },
  {
    id: 3,
    name: 'Bánh Mì Chảo Đặc Biệt',
    description: 'Bánh mì giòn nóng',
    imageUrl: null,
    active: false, // Soft hidden
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
];

describe('FF-03-05-2: Catalog Component Tests (React Testing Library & User Interaction)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // =========================================================================
  // 1. ProductCatalogTable Tests (Render, Status Badges, Inactive, Actions)
  // =========================================================================

  it('1. renders product list with name, variants, formatted prices, and status badges', () => {
    render(<ProductCatalogTable products={MOCK_PRODUCTS} />);

    // Product names rendered
    expect(screen.getByText('Trà Đào Cam Sả')).toBeInTheDocument();
    expect(screen.getByText('Cà Phê Sữa Đá Sài Gòn')).toBeInTheDocument();

    // Variants rendered
    expect(screen.getByText(/Size M/)).toBeInTheDocument();
    expect(screen.getByText(/Size L/)).toBeInTheDocument();

    // Formatted VND prices rendered
    expect(screen.getByText(/35\.000/)).toBeInTheDocument();
    expect(screen.getByText(/45\.000/)).toBeInTheDocument();
    expect(screen.getByText(/28\.000/)).toBeInTheDocument();

    // Capacity status badges
    expect(screen.getByText('Đang mở bán')).toBeInTheDocument();
    expect(screen.getByText('Hết công suất')).toBeInTheDocument();
  });

  it('2. renders inactive / soft-hidden products with "Ẩn" badge and muted styling', () => {
    render(<ProductCatalogTable products={MOCK_PRODUCTS} />);

    // Inactive product #3
    expect(screen.getByText('Bánh Mì Chảo Đặc Biệt')).toBeInTheDocument();
    expect(screen.getByText('Ẩn')).toBeInTheDocument();
    expect(screen.getByText('Đã ẩn mềm (is_active: false)')).toBeInTheDocument();
  });

  it('3. triggers onEdit callback with correct product when clicking "Sửa" button', () => {
    const onEdit = vi.fn();
    render(<ProductCatalogTable products={MOCK_PRODUCTS} onEdit={onEdit} />);

    const editButtons = screen.getAllByRole('button', { name: /Sửa/i });
    expect(editButtons.length).toBeGreaterThan(0);

    fireEvent.click(editButtons[0]);
    expect(onEdit).toHaveBeenCalledTimes(1);
    expect(onEdit).toHaveBeenCalledWith(MOCK_PRODUCTS[0]);
  });

  it('4. triggers onToggleActive callback when clicking eye / eye-off toggle icon', () => {
    const onToggleActive = vi.fn();
    render(<ProductCatalogTable products={MOCK_PRODUCTS} onToggleActive={onToggleActive} />);

    // Find the toggle button for product #1 (active: true -> title="Ẩn mềm món ăn khỏi thực đơn công khai")
    const toggleButtons = screen.getAllByTitle('Ẩn mềm món ăn khỏi thực đơn công khai');
    fireEvent.click(toggleButtons[0]);

    expect(onToggleActive).toHaveBeenCalledTimes(1);
    expect(onToggleActive).toHaveBeenCalledWith(MOCK_PRODUCTS[0]);
  });

  it('5. triggers onDelete callback when clicking trash button', () => {
    const onDelete = vi.fn();
    render(<ProductCatalogTable products={MOCK_PRODUCTS} onDelete={onDelete} />);

    const deleteButtons = screen.getAllByRole('button', { name: /Xóa món/i });
    expect(deleteButtons.length).toBeGreaterThan(0);

    fireEvent.click(deleteButtons[0]);
    expect(onDelete).toHaveBeenCalledTimes(1);
    expect(onDelete).toHaveBeenCalledWith(1);
  });

  // =========================================================================
  // 2. CatalogFilterBar Tests (Search, Sizing, Availability, Reset)
  // =========================================================================

  it('6. triggers onChange when user types in search input', async () => {
    const onChange = vi.fn();
    const onReset = vi.fn();

    render(
      <CatalogFilterBar
        values={{ search: '' }}
        onChange={onChange}
        onReset={onReset}
      />
    );

    const searchInput = screen.getByPlaceholderText('Tìm kiếm theo tên món ăn, hương vị…');
    expect(searchInput).toBeInTheDocument();

    fireEvent.change(searchInput, { target: { value: 'tra dao' } });

    // Wait for debounced search (300ms)
    await waitFor(
      () => {
        expect(onChange).toHaveBeenCalledWith({ search: 'tra dao' });
      },
      { timeout: 600 }
    );
  });

  it('7. triggers onChange when selecting variant size or checking availableOnly', () => {
    const onChange = vi.fn();
    const onReset = vi.fn();

    render(
      <CatalogFilterBar
        values={{ variantSize: '', availableOnly: false }}
        onChange={onChange}
        onReset={onReset}
      />
    );

    // Size filter button "Size M"
    const sizeMButton = screen.getByRole('button', { name: 'Size M' });
    fireEvent.click(sizeMButton);
    expect(onChange).toHaveBeenCalledWith({ variantSize: 'M' });

    // "Chỉ hiện món còn bán" checkbox
    const availableCheckbox = screen.getByRole('checkbox');
    fireEvent.click(availableCheckbox);
    expect(onChange).toHaveBeenCalledWith({ availableOnly: true });
  });

  it('8. triggers onReset callback when clicking "Đặt lại" button', () => {
    const onChange = vi.fn();
    const onReset = vi.fn();

    render(
      <CatalogFilterBar
        values={{ search: 'tra dao', variantSize: 'M' }}
        onChange={onChange}
        onReset={onReset}
      />
    );

    const resetButton = screen.getByRole('button', { name: /Đặt lại tất cả bộ lọc/i });
    expect(resetButton).toBeInTheDocument();

    fireEvent.click(resetButton);
    expect(onReset).toHaveBeenCalledTimes(1);
  });

  // =========================================================================
  // 3. ProductCreateForm Tests (Form Invalid Validation States)
  // =========================================================================

  it('9. displays validation error when submitting with an empty product name', async () => {
    const onSubmit = vi.fn();
    const onCancel = vi.fn();

    render(<ProductCreateForm onSubmit={onSubmit} onCancel={onCancel} />);

    // Clear default name if any, then submit
    const nameInput = screen.getByPlaceholderText('Ví dụ: Trà Đào Cam Sả, Cà Phê Muối...');
    fireEvent.change(nameInput, { target: { value: '' } });

    const submitButton = screen.getByRole('button', { name: /Tạo món ăn & Lưu biến thể/i });
    fireEvent.click(submitButton);

    // Must display error and block submit
    await waitFor(() => {
      expect(screen.getByText('Vui lòng nhập tên món ăn')).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('10. displays validation error when variant price is 0 or negative', async () => {
    const onSubmit = vi.fn();
    const onCancel = vi.fn();

    render(<ProductCreateForm onSubmit={onSubmit} onCancel={onCancel} />);

    // Enter valid product name
    const nameInput = screen.getByPlaceholderText('Ví dụ: Trà Đào Cam Sả, Cà Phê Muối...');
    fireEvent.change(nameInput, { target: { value: 'Trà Chanh Gừng' } });

    // Set price to 0
    const priceInput = screen.getByDisplayValue('35000');
    fireEvent.change(priceInput, { target: { value: '0' } });

    const submitButton = screen.getByRole('button', { name: /Tạo món ăn & Lưu biến thể/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText('Giá bán phải lớn hơn 0 VND')).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('11. displays validation error when duplicate variant sizes are configured', async () => {
    const onSubmit = vi.fn();
    const onCancel = vi.fn();

    render(<ProductCreateForm onSubmit={onSubmit} onCancel={onCancel} />);

    // Enter valid product name
    const nameInput = screen.getByPlaceholderText('Ví dụ: Trà Đào Cam Sả, Cà Phê Muối...');
    fireEvent.change(nameInput, { target: { value: 'Trà Sữa Trân Châu' } });

    // Switch to multi-size preset (Size M & Size L)
    const sizedPresetButton = screen.getByRole('button', { name: /Nhiều kích cỡ/i });
    fireEvent.click(sizedPresetButton);

    // sizeSelects contains [Variant 0 select (value="M"), Variant 1 select (value="L")]
    const sizeSelects = screen.getAllByRole('combobox').filter((el) =>
      Array.from((el as HTMLSelectElement).options).some((opt) => opt.value === 'L')
    );
    expect(sizeSelects.length).toBeGreaterThanOrEqual(2);
    // Change the second variant's size from "L" to "M" to create a duplicate
    fireEvent.change(sizeSelects[1], { target: { value: 'M' } });

    const submitButton = screen.getByRole('button', { name: /Tạo món ăn & Lưu biến thể/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(screen.getAllByText(/đã tồn tại/i).length).toBeGreaterThan(0);
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  // =========================================================================
  // 4. Delete Confirmation Flow Test
  // =========================================================================

  it('12. delete confirm flow: confirms soft delete or cancels without deleting', () => {
    const onDelete = vi.fn();

    // Handler simulating delete confirmation in ProductsPage
    const handleDeleteWithConfirm = (productId: number) => {
      const confirmed = window.confirm(
        `Bạn có chắc chắn muốn ẩn mềm món #${productId} khỏi thực đơn?`
      );
      if (confirmed) {
        onDelete(productId);
      }
    };

    render(
      <ProductCatalogTable
        products={MOCK_PRODUCTS}
        onDelete={handleDeleteWithConfirm}
      />
    );

    const deleteButtons = screen.getAllByRole('button', { name: /Xóa món/i });

    // Case A: User clicks CANCEL on confirm dialog
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);
    fireEvent.click(deleteButtons[0]);
    expect(confirmSpy).toHaveBeenCalledTimes(1);
    expect(onDelete).not.toHaveBeenCalled();

    // Case B: User clicks OK on confirm dialog
    confirmSpy.mockReturnValue(true);
    fireEvent.click(deleteButtons[0]);
    expect(confirmSpy).toHaveBeenCalledTimes(2);
    expect(onDelete).toHaveBeenCalledWith(1);

    confirmSpy.mockRestore();
  });
});
