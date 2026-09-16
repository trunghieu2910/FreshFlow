import React from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { MerchantLayout } from './MerchantLayout';
import { useAuth } from '@/context';

export const AppLayout: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout } = useAuth();

  const path = location.pathname;

  // Determine active nav item and metadata based on route path
  let activeNavId = 'catalog';
  let title = 'Thực đơn & Món ăn';
  let description =
    'Quản lý danh mục món ăn, phân bổ biến thể kích cỡ và định mức công suất phục vụ trong ngày.';
  let breadcrumbs = [
    { label: 'Cửa hàng', href: '/products' },
    { label: 'Chi nhánh Q.1', href: '/products' },
    { label: 'Thực đơn số' },
  ];

  if (path.startsWith('/dashboard')) {
    activeNavId = 'dashboard';
    title = 'Tổng quan Vận hành';
    description =
      'Báo cáo nhanh các chỉ số vận hành, doanh thu bán hàng trong ngày và cảnh báo hệ thống.';
    breadcrumbs = [
      { label: 'Cửa hàng', href: '/dashboard' },
      { label: 'Tổng quan vận hành' },
    ];
  } else if (path.startsWith('/orders')) {
    activeNavId = 'orders';
    title = 'Quản lý Đơn hàng';
    description =
      'Theo dõi và xử lý đơn đặt hàng trực tuyến từ khách hàng theo thời gian thực.';
    breadcrumbs = [
      { label: 'Cửa hàng', href: '/products' },
      { label: 'Đơn hàng' },
    ];
  } else if (path.startsWith('/settings')) {
    activeNavId = 'settings';
    title = 'Cài đặt Cửa hàng';
    description = 'Thiết lập thông tin hoạt động, chi nhánh và thông số tự động duyệt đơn.';
    breadcrumbs = [
      { label: 'Cửa hàng', href: '/products' },
      { label: 'Cài đặt' },
    ];
  }

  const handleSelectNav = (id: string) => {
    switch (id) {
      case 'dashboard':
        navigate('/dashboard');
        break;
      case 'catalog':
        navigate('/products');
        break;
      case 'orders':
        navigate('/orders');
        break;
      case 'settings':
        navigate('/settings');
        break;
      default:
        navigate('/products');
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <MerchantLayout
      title={title}
      description={description}
      activeNavId={activeNavId}
      onSelectNav={handleSelectNav}
      breadcrumbs={breadcrumbs}
      onLogout={handleLogout}
    >
      <Outlet />
    </MerchantLayout>
  );
};
