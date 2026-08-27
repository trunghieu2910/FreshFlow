package com.freshflow.api.catalog.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshflow.api.catalog.application.command.CreateCategoryCommand;
import com.freshflow.api.catalog.application.command.CreateProductCommand;
import com.freshflow.api.catalog.application.command.CreateStoreCommand;
import com.freshflow.api.catalog.application.exception.CatalogErrorCode;
import com.freshflow.api.catalog.application.exception.CatalogNotFoundException;
import com.freshflow.api.catalog.application.exception.CatalogRuleViolationException;
import com.freshflow.api.catalog.domain.Category;
import com.freshflow.api.catalog.domain.Product;
import com.freshflow.api.catalog.domain.Store;
import com.freshflow.api.catalog.domain.StoreCategory;
import com.freshflow.api.catalog.domain.StoreStatus;
import com.freshflow.api.catalog.domain.User;
import com.freshflow.api.catalog.infrastructure.persistence.CategoryRepository;
import com.freshflow.api.catalog.infrastructure.persistence.ProductRepository;
import com.freshflow.api.catalog.infrastructure.persistence.StoreCategoryRepository;
import com.freshflow.api.catalog.infrastructure.persistence.StoreRepository;
import com.freshflow.api.catalog.infrastructure.persistence.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {
  @Mock private UserRepository userRepository;
  @Mock private StoreRepository storeRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private StoreCategoryRepository storeCategoryRepository;
  @Mock private ProductRepository productRepository;

  @InjectMocks private CatalogService catalogService;

  @Test
  void createStore_savesStoreWithOwnerAndDefaults() {
    User owner = new User();
    owner.setId(10L);
    when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
    when(storeRepository.save(any(Store.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Store result =
        catalogService.createStore(
            new CreateStoreCommand(
                10L, "Demo Store", "0900000000", "1 Demo Street", true, StoreStatus.ACTIVE));

    assertEquals("Demo Store", result.getName());
    assertEquals(owner, result.getOwnerUser());
    assertEquals("ACTIVE", result.getStatus());
    assertEquals(Boolean.TRUE, result.getAutoAcceptDefault());
    verify(storeRepository).save(result);
  }

  @Test
  void getStore_whenMissing_throwsCodeBearingException() {
    when(storeRepository.findById(404L)).thenReturn(Optional.empty());

    CatalogNotFoundException exception =
        assertThrows(CatalogNotFoundException.class, () -> catalogService.getStore(404L));

    assertEquals(CatalogErrorCode.STORE_NOT_FOUND.code(), exception.getCode());
  }

  @Test
  void createCategory_savesActiveCategory() {
    when(categoryRepository.save(any(Category.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Category result =
        catalogService.createCategory(new CreateCategoryCommand("Bakery", "Baked items", null));

    assertEquals("Bakery", result.getName());
    assertEquals(Boolean.TRUE, result.getIsActive());
    verify(categoryRepository).save(result);
  }

  @Test
  void getCategory_whenMissing_throwsCodeBearingException() {
    when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

    CatalogNotFoundException exception =
        assertThrows(CatalogNotFoundException.class, () -> catalogService.getCategory(404L));

    assertEquals(CatalogErrorCode.CATEGORY_NOT_FOUND.code(), exception.getCode());
  }

  @Test
  void createProduct_rejectsStoreCategoryFromAnotherStore() {
    Store requestedStore = new Store();
    requestedStore.setId(1L);
    Store anotherStore = new Store();
    anotherStore.setId(2L);
    StoreCategory storeCategory = new StoreCategory();
    storeCategory.setStore(anotherStore);

    when(storeRepository.findById(1L)).thenReturn(Optional.of(requestedStore));
    when(storeCategoryRepository.findById(7L)).thenReturn(Optional.of(storeCategory));

    CatalogRuleViolationException exception =
        assertThrows(
            CatalogRuleViolationException.class,
            () ->
                catalogService.createProduct(
                    new CreateProductCommand(1L, 7L, "Tea", null, null, true)));

    assertEquals(CatalogErrorCode.STORE_CATEGORY_OWNERSHIP_MISMATCH.code(), exception.getCode());
  }

  @Test
  void createProduct_savesProductForMatchingStoreCategory() {
    Store store = new Store();
    store.setId(1L);
    StoreCategory storeCategory = new StoreCategory();
    storeCategory.setId(7L);
    storeCategory.setStore(store);
    when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
    when(storeCategoryRepository.findById(7L)).thenReturn(Optional.of(storeCategory));
    when(productRepository.save(any(Product.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Product result =
        catalogService.createProduct(
            new CreateProductCommand(1L, 7L, "Milk Tea", "Tea", null, null));

    assertEquals("Milk Tea", result.getName());
    assertEquals(store, result.getStore());
    assertEquals(storeCategory, result.getStoreCategory());
    assertEquals(Boolean.TRUE, result.getIsActive());
    verify(productRepository).save(result);
  }

  @Test
  void getProduct_whenMissing_throwsCodeBearingException() {
    when(productRepository.findById(404L)).thenReturn(Optional.empty());

    CatalogNotFoundException exception =
        assertThrows(CatalogNotFoundException.class, () -> catalogService.getProduct(404L));

    assertEquals(CatalogErrorCode.PRODUCT_NOT_FOUND.code(), exception.getCode());
  }

  @Test
  void deleteProduct_softDeletesInsteadOfCallingDeleteById() {
    Product product = new Product();
    product.setId(9L);
    product.setIsActive(Boolean.TRUE);
    when(productRepository.findById(9L)).thenReturn(Optional.of(product));

    catalogService.deleteProduct(9L);

    assertFalse(product.getIsActive());
    verify(productRepository).save(product);
  }
}
