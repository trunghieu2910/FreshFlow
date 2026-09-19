import React, { useState } from 'react';
import { Modal, useToast } from '@/components/ui';
import { ProductEditForm } from './ProductEditForm';
import {
  ProductCatalogDto,
  StoreCategoryDto,
  UpdateProductRequest,
  UpdateProductVariantRequest,
  CreateProductVariantRequest,
} from '@/types/api.types';
import { catalogApi } from '@/api';

export interface ProductEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  product: (ProductCatalogDto & { storeCategoryId?: number }) | null;
  categories?: StoreCategoryDto[];
  storeId: number;
  onSuccess?: (updatedProduct: ProductCatalogDto) => void;
  onMockUpdate?: (
    productId: number,
    productUpdates: UpdateProductRequest,
    variantUpdates: { variantId: number; updates: UpdateProductVariantRequest }[],
    newVariants?: CreateProductVariantRequest[]
  ) => void;
  isMockMode?: boolean;
}

export const ProductEditModal: React.FC<ProductEditModalProps> = ({
  isOpen,
  onClose,
  product,
  categories,
  storeId,
  onSuccess,
  onMockUpdate,
  isMockMode = false,
}) => {
  const [isLoading, setIsLoading] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const toast = useToast();

  if (!product) return null;

  const handleSubmit = async (payload: {
    productUpdates: UpdateProductRequest;
    variantUpdates: { variantId: number; updates: UpdateProductVariantRequest }[];
    newVariants?: CreateProductVariantRequest[];
  }) => {
    setIsLoading(true);
    setServerError(null);

    try {
      if (isMockMode) {
        onMockUpdate?.(
          product.id,
          payload.productUpdates,
          payload.variantUpdates,
          payload.newVariants
        );
        onClose();
        return;
      }

      // Real Spring Boot PATCH API calls
      // 1. PATCH Product
      await catalogApi.updateProduct(
        storeId,
        product.id,
        payload.productUpdates
      );

      // 2. PATCH each variant
      for (const vu of payload.variantUpdates) {
        await catalogApi.updateProductVariant(
          storeId,
          product.id,
          vu.variantId,
          vu.updates
        );
      }

      // 3. Create any new variants
      if (payload.newVariants && payload.newVariants.length > 0) {
        for (const nv of payload.newVariants) {
          await catalogApi.createProductVariant(storeId, product.id, nv);
        }
      }

      // 4. Fetch updated full product with fresh variants
      const fullUpdated = await catalogApi.getProductById(storeId, product.id);
      onSuccess?.(fullUpdated);
      onClose();
    } catch (err: unknown) {
      console.error('Lỗi khi cập nhật món ăn (PATCH):', err);
      const error = err instanceof Error ? err : new Error(String(err));
      const errMsg =
        error.message ||
        'Không thể cập nhật món ăn lên máy chủ Spring Boot. Bạn có thể bật "Xem Mock PostgreSQL" để thử nghiệm offline.';
      setServerError(errMsg);
      toast.error(errMsg, { title: 'Lỗi cập nhật món ăn' });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Chỉnh sửa món ăn: ${product.name}`}
      description="Cập nhật thông tin chi tiết, giá bán, công suất và trạng thái hiển thị (is_active / is_available)."
      size="xl"
    >
      <ProductEditForm
        product={product}
        categories={categories}
        onSubmit={handleSubmit}
        onCancel={onClose}
        isLoading={isLoading}
        serverError={serverError}
      />
    </Modal>
  );
};
