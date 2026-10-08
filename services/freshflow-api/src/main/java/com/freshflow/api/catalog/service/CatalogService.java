package com.freshflow.api.catalog.service;

import com.freshflow.api.catalog.dto.request.CreateCategoryRequest;
import com.freshflow.api.catalog.dto.request.CreateProductRequest;
import com.freshflow.api.catalog.dto.request.CreateStoreRequest;
import com.freshflow.api.catalog.dto.request.ProductFilterCriteria;
import com.freshflow.api.catalog.dto.request.UpdateCategoryRequest;
import com.freshflow.api.catalog.dto.request.UpdateProductRequest;
import com.freshflow.api.catalog.dto.request.UpdateStoreRequest;
import com.freshflow.api.catalog.dto.response.CapacitySnapshot;
import com.freshflow.api.catalog.dto.response.ProductCatalogDto;
import com.freshflow.api.catalog.enums.StoreStatus;
import com.freshflow.api.catalog.exception.CatalogErrorCode;
import com.freshflow.api.catalog.exception.CatalogNotFoundException;
import com.freshflow.api.catalog.exception.CatalogRuleViolationException;
import com.freshflow.api.catalog.mapper.CatalogDtoMapper;
import com.freshflow.api.catalog.model.Category;
import com.freshflow.api.catalog.model.Product;
import com.freshflow.api.catalog.model.ProductVariant;
import com.freshflow.api.catalog.model.Store;
import com.freshflow.api.catalog.model.StoreCategory;
import com.freshflow.api.catalog.model.User;
import com.freshflow.api.catalog.repository.CategoryRepository;
import com.freshflow.api.catalog.repository.ProductRepository;
import com.freshflow.api.catalog.repository.ProductSpecifications;
import com.freshflow.api.catalog.repository.StoreCategoryRepository;
import com.freshflow.api.catalog.repository.StoreRepository;
import com.freshflow.api.catalog.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogService {
  private final UserRepository userRepository;
  private final StoreRepository storeRepository;
  private final CategoryRepository categoryRepository;
  private final StoreCategoryRepository storeCategoryRepository;
  private final ProductRepository productRepository;
  private final CatalogCapacityService capacityService;
  private final CatalogDtoMapper dtoMapper;

  @Transactional
  public Store createStore(CreateStoreRequest request) {
    requireRequest(request);
    User owner = requireUser(request.ownerUserId());
    OffsetDateTime now = now();

    Store store = new Store();
    store.setOwnerUser(owner);
    store.setName(requireText(request.name(), "Store name"));
    store.setPhone(request.phone());
    store.setAddressLine(requireText(request.addressLine(), "Store address"));
    store.setAutoAcceptDefault(
        request.autoAcceptDefault() == null ? Boolean.FALSE : request.autoAcceptDefault());
    store.setStatus(request.status() == null ? StoreStatus.ACTIVE.name() : request.status().name());
    store.setCreatedAt(now);
    store.setUpdatedAt(now);
    return storeRepository.save(store);
  }

  public List<Store> listStores() {
    return storeRepository.findAllByStatusOrderByNameAsc(StoreStatus.ACTIVE.name());
  }

  public Store getStore(Long storeId) {
    return storeRepository
        .findById(requireId(storeId, "Store"))
        .orElseThrow(() -> notFound(CatalogErrorCode.STORE_NOT_FOUND, "Store", storeId));
  }

  @Transactional
  public Store updateStore(Long storeId, UpdateStoreRequest request) {
    requireRequest(request);
    Store store = getStore(storeId);
    if (request.name() != null) {
      store.setName(requireText(request.name(), "Store name"));
    }
    if (request.phone() != null) {
      store.setPhone(request.phone());
    }
    if (request.addressLine() != null) {
      store.setAddressLine(requireText(request.addressLine(), "Store address"));
    }
    if (request.autoAcceptDefault() != null) {
      store.setAutoAcceptDefault(request.autoAcceptDefault());
    }
    if (request.status() != null) {
      store.setStatus(request.status().name());
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
  public Category createCategory(CreateCategoryRequest request) {
    requireRequest(request);
    OffsetDateTime now = now();

    Category category = new Category();
    category.setName(requireText(request.name(), "Category name"));
    category.setDescription(request.description());
    category.setIsActive(request.active() == null ? Boolean.TRUE : request.active());
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
  public Category updateCategory(Long categoryId, UpdateCategoryRequest request) {
    requireRequest(request);
    Category category = getCategory(categoryId);
    if (request.name() != null) {
      category.setName(requireText(request.name(), "Category name"));
    }
    if (request.description() != null) {
      category.setDescription(request.description());
    }
    if (request.active() != null) {
      category.setIsActive(request.active());
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
  public Product createProduct(Long storeId, CreateProductRequest request) {
    requireRequest(request);
    Store store = getStore(storeId);
    StoreCategory storeCategory = requireStoreCategory(request.storeCategoryId());
    validateStoreCategoryOwnership(store, storeCategory);
    OffsetDateTime now = now();

    Product product = new Product();
    product.setStore(store);
    product.setStoreCategory(storeCategory);
    product.setName(requireText(request.name(), "Product name"));
    product.setDescription(request.description());
    product.setImageUrl(request.imageUrl());
    product.setIsActive(request.active() == null ? Boolean.TRUE : request.active());
    product.setCreatedAt(now);
    product.setUpdatedAt(now);
    return productRepository.save(product);
  }

  public List<Product> listProductsByStore(Long storeId) {
    getStore(storeId);
    return productRepository.findAllByStore_IdOrderByNameAsc(storeId);
  }

  public Page<ProductCatalogDto> listProductsByStore(
      Long storeId, ProductFilterCriteria criteria, Pageable pageable) {
    requireActiveStore(storeId);
    Specification<Product> spec = ProductSpecifications.withFilter(storeId, criteria);
    Page<Product> page = productRepository.findAll(spec, pageable);

    List<ProductVariant> allVariants =
        page.getContent().stream()
            .filter(p -> p.getVariants() != null)
            .flatMap(p -> p.getVariants().stream())
            .toList();

    Map<Long, CapacitySnapshot> capacityMap =
        capacityService.getCapacitySnapshots(allVariants, LocalDate.now());

    List<ProductCatalogDto> content =
        page.getContent().stream()
            .map(product -> dtoMapper.toPublicProductDto(product, capacityMap))
            .filter(product -> !product.variants().isEmpty())
            .filter(
                product ->
                    !Boolean.TRUE.equals(criteria.availableOnly())
                        || product.variants().stream().anyMatch(v -> v.available()))
            .toList();
    return new PageImpl<>(content, pageable, content.size());
  }

  /**
   * Returns a product only when its store, category assignment, category and at least one variant
   * are active. Capacity is read from the database for this response.
   */
  public ProductCatalogDto getPublicProduct(Long storeId, Long productId) {
    requireActiveStore(storeId);
    Product product =
        productRepository
            .findByIdAndStore_Id(requireId(productId, "Product"), storeId)
            .orElseThrow(() -> notFound(CatalogErrorCode.PRODUCT_NOT_FOUND, "Product", productId));
    if (!isPubliclyPurchasable(product)) {
      throw notFound(CatalogErrorCode.PRODUCT_NOT_FOUND, "Product", productId);
    }
    Map<Long, CapacitySnapshot> capacityMap =
        capacityService.getCapacitySnapshots(product.getVariants(), LocalDate.now());
    return dtoMapper.toPublicProductDto(product, capacityMap);
  }

  public Product getProduct(Long productId) {
    return productRepository
        .findById(requireId(productId, "Product"))
        .orElseThrow(() -> notFound(CatalogErrorCode.PRODUCT_NOT_FOUND, "Product", productId));
  }

  @Transactional
  public Product updateProduct(Long productId, UpdateProductRequest request) {
    requireRequest(request);
    Product product = getProduct(productId);
    if (request.name() != null) {
      product.setName(requireText(request.name(), "Product name"));
    }
    if (request.description() != null) {
      product.setDescription(request.description());
    }
    if (request.imageUrl() != null) {
      product.setImageUrl(request.imageUrl());
    }
    if (request.active() != null) {
      product.setIsActive(request.active());
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

  public long countActiveProductsByStore(Long storeId) {
    return productRepository.countByStore_IdAndIsActiveTrue(storeId);
  }

  public long countTotalProductsByStore(Long storeId) {
    return productRepository.countByStore_Id(storeId);
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

  private Store requireActiveStore(Long storeId) {
    Store store = getStore(storeId);
    if (!StoreStatus.ACTIVE.name().equals(store.getStatus())) {
      throw notFound(CatalogErrorCode.STORE_NOT_FOUND, "Store", storeId);
    }
    return store;
  }

  private static boolean isPubliclyPurchasable(Product product) {
    StoreCategory storeCategory = product.getStoreCategory();
    return Boolean.TRUE.equals(product.getIsActive())
        && storeCategory != null
        && Boolean.TRUE.equals(storeCategory.getIsActive())
        && storeCategory.getCategory() != null
        && Boolean.TRUE.equals(storeCategory.getCategory().getIsActive())
        && product.getVariants() != null
        && product.getVariants().stream().anyMatch(v -> Boolean.TRUE.equals(v.getIsActive()));
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

  private static void requireRequest(Object request) {
    if (request == null) {
      throw new IllegalArgumentException("Request must not be null");
    }
  }

  private static OffsetDateTime now() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }
}
