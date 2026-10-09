package com.freshflow.api.catalog.service;

import com.freshflow.api.catalog.exception.CatalogErrorCode;
import com.freshflow.api.catalog.exception.CatalogNotFoundException;
import com.freshflow.api.catalog.exception.CatalogRuleViolationException;
import com.freshflow.api.catalog.model.Product;
import com.freshflow.api.catalog.model.Store;
import com.freshflow.api.catalog.repository.ProductRepository;
import com.freshflow.api.catalog.repository.StoreRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogAccessService {
  private final StoreRepository storeRepository;
  private final ProductRepository productRepository;
  private final com.freshflow.api.identity.service.IdentityAccessService identityAccessService;

  public Store requireOwnedStore(Long storeId, Long actorUserId) {
    if (actorUserId == null || actorUserId <= 0) {
      throw new CatalogRuleViolationException(
          CatalogErrorCode.ACTOR_REQUIRED, "X-User-Id is required for merchant operations");
    }
    Store store =
        storeRepository
            .findById(storeId)
            .orElseThrow(
                () ->
                    new CatalogNotFoundException(
                        CatalogErrorCode.STORE_NOT_FOUND, "Store", storeId));
    if (store.getOwnerUser() == null
        || !Objects.equals(store.getOwnerUser().getId(), actorUserId)) {
      throw new CatalogRuleViolationException(
          CatalogErrorCode.STORE_ACCESS_DENIED, "Merchant does not own this Store");
    }
    identityAccessService.requireGrant(actorUserId, "MERCHANT", storeId);
    return store;
  }

  public Product requireProductInStore(Long storeId, Long productId) {
    return productRepository
        .findByIdAndStore_Id(productId, storeId)
        .orElseThrow(
            () ->
                new CatalogNotFoundException(
                    CatalogErrorCode.PRODUCT_NOT_FOUND, "Product", productId));
  }

  public Product requireOwnedProduct(Long storeId, Long productId, Long actorUserId) {
    requireOwnedStore(storeId, actorUserId);
    return requireProductInStore(storeId, productId);
  }
}
