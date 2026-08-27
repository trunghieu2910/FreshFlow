package com.freshflow.api.catalog.application;

import com.freshflow.api.catalog.application.command.CreateCategoryCommand;
import com.freshflow.api.catalog.application.command.CreateProductCommand;
import com.freshflow.api.catalog.application.command.CreateStoreCommand;
import com.freshflow.api.catalog.application.command.UpdateCategoryCommand;
import com.freshflow.api.catalog.application.command.UpdateProductCommand;
import com.freshflow.api.catalog.application.command.UpdateStoreCommand;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
  private final UserRepository userRepository;
  private final StoreRepository storeRepository;
  private final CategoryRepository categoryRepository;
  private final StoreCategoryRepository storeCategoryRepository;
  private final ProductRepository productRepository;

  public CatalogService(
      UserRepository userRepository,
      StoreRepository storeRepository,
      CategoryRepository categoryRepository,
      StoreCategoryRepository storeCategoryRepository,
      ProductRepository productRepository) {
    this.userRepository = userRepository;
    this.storeRepository = storeRepository;
    this.categoryRepository = categoryRepository;
    this.storeCategoryRepository = storeCategoryRepository;
    this.productRepository = productRepository;
  }

  @Transactional
  public Store createStore(CreateStoreCommand command) {
    requireCommand(command);
    User owner = requireUser(command.ownerUserId());
    OffsetDateTime now = now();

    Store store = new Store();
    store.setOwnerUser(owner);
    store.setName(requireText(command.name(), "Store name"));
    store.setPhone(command.phone());
    store.setAddressLine(requireText(command.addressLine(), "Store address"));
    store.setAutoAcceptDefault(
        command.autoAcceptDefault() == null ? Boolean.FALSE : command.autoAcceptDefault());
    store.setStatus(command.status() == null ? StoreStatus.ACTIVE.name() : command.status().name());
    store.setCreatedAt(now);
    store.setUpdatedAt(now);
    return storeRepository.save(store);
  }

  public List<Store> listStores() {
    return storeRepository.findAllByOrderByNameAsc();
  }

  public Store getStore(Long storeId) {
    return storeRepository
        .findById(requireId(storeId, "Store"))
        .orElseThrow(() -> notFound(CatalogErrorCode.STORE_NOT_FOUND, "Store", storeId));
  }

  @Transactional
  public Store updateStore(Long storeId, UpdateStoreCommand command) {
    requireCommand(command);
    Store store = getStore(storeId);
    if (command.name() != null) {
      store.setName(requireText(command.name(), "Store name"));
    }
    if (command.phone() != null) {
      store.setPhone(command.phone());
    }
    if (command.addressLine() != null) {
      store.setAddressLine(requireText(command.addressLine(), "Store address"));
    }
    if (command.autoAcceptDefault() != null) {
      store.setAutoAcceptDefault(command.autoAcceptDefault());
    }
    if (command.status() != null) {
      store.setStatus(command.status().name());
    }
    store.setUpdatedAt(now());
    return storeRepository.save(store);
  }

  @Transactional
  public void deleteStore(Long storeId) {
    Store store = getStore(storeId);
    store.setStatus(StoreStatus.INACTIVE.name());
    store.setUpdatedAt(now());
    storeRepository.save(store);
  }

  @Transactional
  public Category createCategory(CreateCategoryCommand command) {
    requireCommand(command);
    OffsetDateTime now = now();

    Category category = new Category();
    category.setName(requireText(command.name(), "Category name"));
    category.setDescription(command.description());
    category.setIsActive(command.active() == null ? Boolean.TRUE : command.active());
    category.setCreatedAt(now);
    category.setUpdatedAt(now);
    return categoryRepository.save(category);
  }

  public List<Category> listCategories() {
    return categoryRepository.findAllByOrderByNameAsc();
  }

  public Category getCategory(Long categoryId) {
    return categoryRepository
        .findById(requireId(categoryId, "Category"))
        .orElseThrow(() -> notFound(CatalogErrorCode.CATEGORY_NOT_FOUND, "Category", categoryId));
  }

  @Transactional
  public Category updateCategory(Long categoryId, UpdateCategoryCommand command) {
    requireCommand(command);
    Category category = getCategory(categoryId);
    if (command.name() != null) {
      category.setName(requireText(command.name(), "Category name"));
    }
    if (command.description() != null) {
      category.setDescription(command.description());
    }
    if (command.active() != null) {
      category.setIsActive(command.active());
    }
    category.setUpdatedAt(now());
    return categoryRepository.save(category);
  }

  @Transactional
  public void deleteCategory(Long categoryId) {
    Category category = getCategory(categoryId);
    category.setIsActive(Boolean.FALSE);
    category.setUpdatedAt(now());
    categoryRepository.save(category);
  }

  @Transactional
  public Product createProduct(CreateProductCommand command) {
    requireCommand(command);
    Store store = getStore(command.storeId());
    StoreCategory storeCategory = requireStoreCategory(command.storeCategoryId());
    validateStoreCategoryOwnership(store, storeCategory);
    OffsetDateTime now = now();

    Product product = new Product();
    product.setStore(store);
    product.setStoreCategory(storeCategory);
    product.setName(requireText(command.name(), "Product name"));
    product.setDescription(command.description());
    product.setImageUrl(command.imageUrl());
    product.setIsActive(command.active() == null ? Boolean.TRUE : command.active());
    product.setCreatedAt(now);
    product.setUpdatedAt(now);
    return productRepository.save(product);
  }

  public List<Product> listProductsByStore(Long storeId) {
    getStore(storeId);
    return productRepository.findAllByStore_IdOrderByNameAsc(storeId);
  }

  public Product getProduct(Long productId) {
    return productRepository
        .findById(requireId(productId, "Product"))
        .orElseThrow(() -> notFound(CatalogErrorCode.PRODUCT_NOT_FOUND, "Product", productId));
  }

  @Transactional
  public Product updateProduct(Long productId, UpdateProductCommand command) {
    requireCommand(command);
    Product product = getProduct(productId);
    if (command.name() != null) {
      product.setName(requireText(command.name(), "Product name"));
    }
    if (command.description() != null) {
      product.setDescription(command.description());
    }
    if (command.imageUrl() != null) {
      product.setImageUrl(command.imageUrl());
    }
    if (command.active() != null) {
      product.setIsActive(command.active());
    }
    product.setUpdatedAt(now());
    return productRepository.save(product);
  }

  @Transactional
  public void deleteProduct(Long productId) {
    Product product = getProduct(productId);
    product.setIsActive(Boolean.FALSE);
    product.setUpdatedAt(now());
    productRepository.save(product);
  }

  private User requireUser(Long userId) {
    return userRepository
        .findById(requireId(userId, "User"))
        .orElseThrow(
            () -> new CatalogNotFoundException(CatalogErrorCode.USER_NOT_FOUND, "User", userId));
  }

  private StoreCategory requireStoreCategory(Long storeCategoryId) {
    return storeCategoryRepository
        .findById(requireId(storeCategoryId, "StoreCategory"))
        .orElseThrow(
            () ->
                notFound(
                    CatalogErrorCode.STORE_CATEGORY_NOT_FOUND, "StoreCategory", storeCategoryId));
  }

  private void validateStoreCategoryOwnership(Store store, StoreCategory storeCategory) {
    if (storeCategory.getStore() == null
        || !Objects.equals(storeCategory.getStore().getId(), store.getId())) {
      throw new CatalogRuleViolationException(
          CatalogErrorCode.STORE_CATEGORY_OWNERSHIP_MISMATCH,
          "StoreCategory does not belong to the requested Store");
    }
  }

  private CatalogNotFoundException notFound(
      CatalogErrorCode errorCode, String resourceName, Long id) {
    return new CatalogNotFoundException(errorCode, resourceName, id);
  }

  private static Long requireId(Long id, String resourceName) {
    if (id == null || id <= 0) {
      throw new IllegalArgumentException(resourceName + " id must be positive");
    }
    return id;
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value.trim();
  }

  private static void requireCommand(Object command) {
    if (command == null) {
      throw new IllegalArgumentException("Command must not be null");
    }
  }

  private static OffsetDateTime now() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }
}
