package com.freshflow.api.catalog.application.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.freshflow.api.catalog.domain.Product;
import com.freshflow.api.catalog.domain.Store;
import com.freshflow.api.catalog.domain.User;
import com.freshflow.api.catalog.infrastructure.persistence.ProductRepository;
import com.freshflow.api.catalog.infrastructure.persistence.StoreRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogAccessServiceTest {

  @Mock private StoreRepository storeRepository;
  @Mock private ProductRepository productRepository;

  private CatalogAccessService accessService;

  @BeforeEach
  void setUp() {
    accessService = new CatalogAccessService(storeRepository, productRepository);
  }

  @Test
  @DisplayName("requireOwnedStore should throw ACTOR_REQUIRED when actorUserId is null")
  void requireOwnedStore_whenActorUserIdIsNull_shouldThrowActorRequiredException() {
    CatalogRuleViolationException exception =
        assertThrows(
            CatalogRuleViolationException.class, () -> accessService.requireOwnedStore(1L, null));

    assertEquals(CatalogErrorCode.ACTOR_REQUIRED, exception.getErrorCode());
    verifyNoInteractions(storeRepository);
  }

  @Test
  @DisplayName("requireOwnedStore should throw ACTOR_REQUIRED when actorUserId is zero or negative")
  void requireOwnedStore_whenActorUserIdIsZeroOrNegative_shouldThrowActorRequiredException() {
    CatalogRuleViolationException exception =
        assertThrows(
            CatalogRuleViolationException.class, () -> accessService.requireOwnedStore(1L, 0L));

    assertEquals(CatalogErrorCode.ACTOR_REQUIRED, exception.getErrorCode());
    verifyNoInteractions(storeRepository);
  }

  @Test
  @DisplayName("requireOwnedStore should throw STORE_NOT_FOUND when store does not exist")
  void requireOwnedStore_whenStoreNotFound_shouldThrowStoreNotFoundException() {
    when(storeRepository.findById(99L)).thenReturn(Optional.empty());

    CatalogNotFoundException exception =
        assertThrows(
            CatalogNotFoundException.class, () -> accessService.requireOwnedStore(99L, 2L));

    assertEquals(CatalogErrorCode.STORE_NOT_FOUND, exception.getErrorCode());
    verify(storeRepository).findById(99L);
  }

  @Test
  @DisplayName("requireOwnedStore should throw STORE_ACCESS_DENIED when store has no owner")
  void requireOwnedStore_whenStoreOwnerIsNull_shouldThrowStoreAccessDeniedException() {
    Store store = new Store();
    store.setId(1L);
    store.setOwnerUser(null);
    when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

    CatalogRuleViolationException exception =
        assertThrows(
            CatalogRuleViolationException.class, () -> accessService.requireOwnedStore(1L, 2L));

    assertEquals(CatalogErrorCode.STORE_ACCESS_DENIED, exception.getErrorCode());
  }

  @Test
  @DisplayName("requireOwnedStore should throw STORE_ACCESS_DENIED when actor is not store owner")
  void requireOwnedStore_whenActorIsNotStoreOwner_shouldThrowStoreAccessDeniedException() {
    User owner = new User();
    owner.setId(10L);

    Store store = new Store();
    store.setId(1L);
    store.setOwnerUser(owner);
    when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

    CatalogRuleViolationException exception =
        assertThrows(
            CatalogRuleViolationException.class, () -> accessService.requireOwnedStore(1L, 99L));

    assertEquals(CatalogErrorCode.STORE_ACCESS_DENIED, exception.getErrorCode());
  }

  @Test
  @DisplayName("requireOwnedStore should return Store when actor is owner")
  void requireOwnedStore_whenActorIsOwner_shouldReturnStore() {
    User owner = new User();
    owner.setId(2L);

    Store store = new Store();
    store.setId(1L);
    store.setOwnerUser(owner);
    when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

    Store result = accessService.requireOwnedStore(1L, 2L);

    assertNotNull(result);
    assertEquals(1L, result.getId());
  }

  @Test
  @DisplayName("requireProductInStore should throw PRODUCT_NOT_FOUND when product is not in store")
  void requireProductInStore_whenProductNotFound_shouldThrowProductNotFoundException() {
    when(productRepository.findByIdAndStore_Id(50L, 1L)).thenReturn(Optional.empty());

    CatalogNotFoundException exception =
        assertThrows(
            CatalogNotFoundException.class, () -> accessService.requireProductInStore(1L, 50L));

    assertEquals(CatalogErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("requireProductInStore should return Product when found in store")
  void requireProductInStore_whenProductExistsInStore_shouldReturnProduct() {
    Product product = new Product();
    product.setId(50L);
    when(productRepository.findByIdAndStore_Id(50L, 1L)).thenReturn(Optional.of(product));

    Product result = accessService.requireProductInStore(1L, 50L);

    assertNotNull(result);
    assertEquals(50L, result.getId());
  }

  @Test
  @DisplayName("requireOwnedProduct should verify store ownership and return product")
  void requireOwnedProduct_whenValidOwnerAndProductInStore_shouldReturnProduct() {
    User owner = new User();
    owner.setId(2L);

    Store store = new Store();
    store.setId(1L);
    store.setOwnerUser(owner);
    when(storeRepository.findById(1L)).thenReturn(Optional.of(store));

    Product product = new Product();
    product.setId(100L);
    when(productRepository.findByIdAndStore_Id(100L, 1L)).thenReturn(Optional.of(product));

    Product result = accessService.requireOwnedProduct(1L, 100L, 2L);

    assertNotNull(result);
    assertEquals(100L, result.getId());
  }
}
