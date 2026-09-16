import React from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui';
import { Compass, ArrowLeft } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-4 text-center">
      <div className="flex h-20 w-20 items-center justify-center rounded-3xl bg-emerald-50 text-emerald-600 mb-6 border border-emerald-100 shadow-sm">
        <Compass className="h-10 w-10 animate-spin-slow" aria-hidden="true" />
      </div>

      <span className="text-xs font-bold uppercase tracking-widest text-emerald-600 bg-emerald-100/60 px-2.5 py-1 rounded-full mb-3">
        Lỗi 404
      </span>

      <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-slate-900 mb-2">
        Không tìm thấy trang yêu cầu
      </h1>

      <p className="text-sm text-slate-500 max-w-md mb-8 leading-relaxed">
        Đường dẫn bạn vừa truy cập không tồn tại hoặc đã bị thay đổi trong hệ thống FreshFlow Portal.
      </p>

      <div className="flex items-center gap-3">
        <Link to="/products">
          <Button
            variant="primary"
            leftIcon={<ArrowLeft className="h-4 w-4" aria-hidden="true" />}
          >
            Quay về Thực đơn số
          </Button>
        </Link>
        <Link to="/dashboard">
          <Button variant="outline">Xem Tổng quan</Button>
        </Link>
      </div>
    </div>
  );
};
