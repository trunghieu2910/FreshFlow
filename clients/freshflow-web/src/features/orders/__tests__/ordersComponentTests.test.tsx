import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastProvider } from '@/components/ui';
import { DashboardPage } from '@/pages/DashboardPage';
import { OrdersPage } from '@/pages/OrdersPage';
import { OrderDetailModal } from '../components/OrderDetailModal';
import { orderApi } from '@/api';

vi.mock('@/api', () => ({
  orderApi: {
    getDashboardSummary: vi.fn(),
    getOrders: vi.fn(),
    getOrderDetail: vi.fn(),
    acceptOrder: vi.fn(),
    rejectOrder: vi.fn(),
    prepareOrder: vi.fn(),
    dispatchOrder: vi.fn(),
  },
}));

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ToastProvider>{ui}</ToastProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('Sprint 4 Orders & Dashboard Component Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders DashboardPage with real KPI numbers from backend', async () => {
    vi.mocked(orderApi.getDashboardSummary).mockResolvedValue({
      activeProductsCount: 24,
      totalProductsCount: 26,
      pendingOrdersCount: 3,
      todayOrdersCount: 18,
      todayRevenue: 4850000,
      operationalStatus: 'NORMAL',
      avgPreparationMinutes: 8,
    });

    renderWithProviders(<DashboardPage />);

    expect(screen.getByText('Bảng điều khiển Cửa hàng')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText(/4\.850\.000/)).toBeInTheDocument();
      expect(screen.getByText('3 đơn mới')).toBeInTheDocument();
      expect(screen.getByText('24 / 26 món')).toBeInTheDocument();
      expect(screen.getByText('Bình thường')).toBeInTheDocument();
    });
  });

  it('renders OrdersPage with orders table, status badge and masked phone', async () => {
    vi.mocked(orderApi.getOrders).mockResolvedValue({
      content: [
        {
          id: 1,
          orderNumber: 'ORD-2026-001',
          customerName: 'Nguyễn Văn An',
          customerPhoneMasked: '0901***456',
          itemsSummary: '2x Trà Đào Cam Sả Tươi (Size L)',
          totalAmount: 160000,
          status: 'AWAITING_MERCHANT_CONFIRMATION',
          statusLabel: 'Chờ quán xác nhận',
          paymentMethod: 'CASH_ON_DELIVERY',
          createdAt: new Date().toISOString(),
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
    });

    renderWithProviders(<OrdersPage />);

    expect(screen.getByText('Quản lý Đơn hàng')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('ORD-2026-001')).toBeInTheDocument();
      expect(screen.getByText('Nguyễn Văn An')).toBeInTheDocument();
      expect(screen.getByText('0901***456')).toBeInTheDocument();
      expect(screen.getByText('Chờ quán xác nhận')).toBeInTheDocument();
    });
  });

  it('renders OrderDetailModal with line items and Accept/Reject actions for confirmation status', async () => {
    vi.mocked(orderApi.getOrderDetail).mockResolvedValue({
      id: 1,
      orderNumber: 'ORD-2026-001',
      customerName: 'Nguyễn Văn An',
      customerPhoneMasked: '0901***456',
      status: 'AWAITING_MERCHANT_CONFIRMATION',
      statusLabel: 'Chờ quán xác nhận',
      paymentMethod: 'CASH_ON_DELIVERY',
      merchantAcceptanceStatus: 'PENDING',
      subtotal: 145000,
      deliveryFee: 15000,
      discountAmount: 0,
      totalAmount: 160000,
      createdAt: new Date().toISOString(),
      items: [
        {
          id: 101,
          productName: 'Trà Đào Cam Sả Tươi',
          variantName: 'Size L',
          unitPrice: 45000,
          quantity: 2,
          lineTotal: 90000,
        },
      ],
    });

    const onClose = vi.fn();

    renderWithProviders(
      <OrderDetailModal orderId={1} storeId={1} isOpen={true} onClose={onClose} />
    );

    await waitFor(() => {
      expect(screen.getByText('Chi tiết Đơn hàng #ORD-2026-001')).toBeInTheDocument();
      expect(screen.getByText('Trà Đào Cam Sả Tươi')).toBeInTheDocument();
      expect(screen.getByText('Size L')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /nhận đơn ngay/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /từ chối đơn/i })).toBeInTheDocument();
    });

    // Clicking reject opens reason prompt
    fireEvent.click(screen.getByRole('button', { name: /từ chối đơn/i }));
    expect(screen.getByText('Chọn lý do từ chối nhận đơn:')).toBeInTheDocument();
  });
});
