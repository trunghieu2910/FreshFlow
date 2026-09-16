import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/context';
import { Button, Input } from '@/components/ui';
import { Store, ShieldCheck, ArrowRight, Lock } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('merchant@freshflow.vn');
  const [password, setPassword] = useState('freshflow2026');
  const [isLoading, setIsLoading] = useState(false);

  // Where to redirect after login (default: /products)
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const from = (location.state as any)?.from?.pathname || '/products';

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setTimeout(() => {
      login('1');
      setIsLoading(false);
      navigate(from, { replace: true });
    }, 400);
  };

  const handleQuickDemoLogin = () => {
    setIsLoading(true);
    setTimeout(() => {
      login('1');
      setIsLoading(false);
      navigate(from, { replace: true });
    }, 200);
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      {/* Brand Header */}
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-600 text-white shadow-md mb-3">
          <Store className="h-8 w-8" aria-hidden="true" />
        </div>
        <h2 className="text-2xl font-bold tracking-tight text-slate-900">
          FreshFlow Merchant Portal
        </h2>
        <p className="mt-1 text-xs text-slate-500">
          Cổng thông tin quản lý thực đơn số và đơn hàng cho Chủ quán
        </p>
      </div>

      {/* Login Card */}
      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md px-4 sm:px-0">
        <div className="bg-white py-8 px-6 shadow-xl rounded-2xl border border-slate-200 sm:px-10 space-y-6">
          {/* Redirect Notice if user was blocked by ProtectedRoute */}
          {location.state?.from && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-2.5 text-xs text-amber-800 animate-fadeIn">
              <Lock className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" aria-hidden="true" />
              <div>
                <strong className="font-semibold">Yêu cầu đăng nhập:</strong> Bạn cần đăng nhập để
                truy cập trang <code className="font-mono bg-amber-100/80 px-1 py-0.5 rounded">{from}</code>.
              </div>
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-4">
            <Input
              label="Email đăng nhập"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="tenchuquan@freshflow.vn"
              required
            />

            <Input
              label="Mật khẩu"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />

            <Button
              type="submit"
              variant="primary"
              isLoading={isLoading}
              className="w-full"
              rightIcon={<ArrowRight className="h-4 w-4" aria-hidden="true" />}
            >
              Đăng nhập tài khoản
            </Button>
          </form>

          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-slate-200" />
            </div>
            <div className="relative flex justify-center text-xs">
              <span className="bg-white px-2 text-slate-400 font-medium">Hoặc thử nghiệm</span>
            </div>
          </div>

          <Button
            type="button"
            variant="secondary"
            onClick={handleQuickDemoLogin}
            disabled={isLoading}
            className="w-full justify-center border-slate-300 hover:bg-slate-50 text-slate-700"
            leftIcon={<ShieldCheck className="h-4 w-4 text-emerald-600" aria-hidden="true" />}
          >
            Đăng nhập nhanh (Tài khoản Demo)
          </Button>

          <div className="rounded-xl bg-slate-50 p-3 border border-slate-100 text-[11px] text-slate-500 leading-relaxed">
            💡 <strong>Giai đoạn Tuần 3:</strong> Quản lý xác thực qua AuthContext và Protected Route
            Guard. API contract Authentication hoàn chỉnh sẽ được liên kết ở Tuần 4.
          </div>
        </div>
      </div>
    </div>
  );
};
