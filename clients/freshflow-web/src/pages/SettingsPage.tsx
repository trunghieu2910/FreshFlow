import React from 'react';
import { Button, Input } from '@/components/ui';
import { Store, Save } from 'lucide-react';

export const SettingsPage: React.FC = () => {
  return (
    <div className="max-w-3xl space-y-6">
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs space-y-6">
        <div className="flex items-center gap-3 border-b border-slate-100 pb-4">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
            <Store className="h-5 w-5" aria-hidden="true" />
          </div>
          <div>
            <h2 className="text-base font-semibold text-slate-900">Thông tin Cửa hàng</h2>
            <p className="text-xs text-slate-500">Cấu hình chi nhánh hoạt động của Merchant.</p>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Input label="Tên cửa hàng" defaultValue="FreshFlow Coffee & Tea" />
          <Input label="Tên chi nhánh" defaultValue="Chi nhánh Quận 1 (Nguyễn Huệ)" />
          <Input label="Số điện thoại hotline" defaultValue="028 3822 ****" />
          <Input label="Thời gian hoạt động" defaultValue="07:00 - 22:30" />
          <div className="sm:col-span-2">
            <Input
              label="Địa chỉ chi nhánh"
              defaultValue="68 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh"
            />
          </div>
        </div>

        <div className="border-t border-slate-100 pt-4 flex justify-end">
          <Button
            variant="primary"
            leftIcon={<Save className="h-4 w-4" aria-hidden="true" />}
            onClick={() => alert('Đã lưu cài đặt cửa hàng!')}
          >
            Lưu thay đổi
          </Button>
        </div>
      </div>
    </div>
  );
};
