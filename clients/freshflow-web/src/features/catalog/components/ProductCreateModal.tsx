import React, { useState } from 'react';
import { Modal, useToast } from '@/components/ui';
import { ProductCreateForm } from './ProductCreateForm';
import { ProductCreateFormData, ProductCatalogDto } from '@/types/api.types';
import { catalogApi } from '@/api';
import { transformToApiPayloads } from '../utils/productValidation';

export interface ProductCreateModalProps {
  isOpen: boolean;
  onClose: () => void;
  storeId: number;
  onSuccess?: (createdProduct: ProductCatalogDto) => void;
  onMockCreate?: (formData: ProductCreateFormData) => void;
  isMockMode?: boolean;
}

export const ProductCreateModal: React.FC<ProductCreateModalProps> = ({
  isOpen,
  onClose,
  storeId,
  onSuccess,
  onMockCreate,
  isMockMode = false,
}) => {
  const [isLoading, setIsLoading] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const toast = useToast();

  const handleSubmit = async (formData: ProductCreateFormData) => {
    setIsLoading(true);
    setServerError(null);

    try {
      if (isMockMode) {
        // Fallback for offline PostgreSQL mock test
        onMockCreate?.(formData);
        onClose();
        return;
      }

      // Real Spring Boot API call with nested variants
      const { product: productPayload, variants: variantsPayload } =
        transformToApiPayloads(formData);

      const created = await catalogApi.createProductWithVariants(
        storeId,
        productPayload,
        variantsPayload
      );

      onSuccess?.(created);
      onClose();
    } catch (err: unknown) {
      console.error('Lỗi tạo món ăn:', err);
      const error = err instanceof Error ? err : new Error(String(err));
      const errMsg =
        error.message ||
        'Không thể kết nối hoặc lưu món ăn vào máy chủ Spring Boot. Bạn có thể bật chế độ "Xem Mock PostgreSQL" để thử nghiệm offline.';
      setServerError(errMsg);
      toast.error(errMsg, { title: 'Lỗi tạo món ăn' });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Thêm món ăn mới vào thực đơn"
      description="Nhập thông tin sản phẩm và cấu hình các biến thể kích cỡ, công suất phục vụ, giá bán."
      size="xl"
    >
      <ProductCreateForm
        onSubmit={handleSubmit}
        onCancel={onClose}
        isLoading={isLoading}
        serverError={serverError}
      />
    </Modal>
  );
};
