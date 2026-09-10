package com.freshflow.api.catalog.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.freshflow.api.catalog.application.query.ProductFilterCriteria;
import com.freshflow.api.catalog.domain.Category;
import com.freshflow.api.catalog.domain.InventoryMode;
import com.freshflow.api.catalog.domain.Product;
import com.freshflow.api.catalog.domain.ProductVariant;
import com.freshflow.api.catalog.domain.Store;
import com.freshflow.api.catalog.domain.StoreCategory;
import com.freshflow.api.catalog.domain.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductSpecificationIntegrationTest {

  @Autowired private EntityManager entityManager;
  @Autowired private ProductRepository productRepository;

  private Store store;
  private StoreCategory beverageCategory;
  private StoreCategory bakeryCategory;
  private Product milkTeaProduct;
  private Product croissantProduct;
  private Product inactiveProduct;

  @BeforeEach
  void setUp() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    User owner = new User();
    owner.setEmail("spec.owner@freshflow.local");
    owner.setPasswordHash("hash");
    owner.setFullName("Spec Owner");
    owner.setStatus("ACTIVE");
    owner.setCreatedAt(now);
    owner.setUpdatedAt(now);
    entityManager.persist(owner);

    store = new Store();
    store.setOwnerUser(owner);
    store.setName("Spec Store");
    store.setAddressLine("Spec Street");
    store.setAutoAcceptDefault(false);
    store.setStatus("ACTIVE");
    store.setCreatedAt(now);
    store.setUpdatedAt(now);
    entityManager.persist(store);

    Category bevCat = new Category();
    bevCat.setName("Spec Drinks");
    bevCat.setIsActive(true);
    bevCat.setCreatedAt(now);
    bevCat.setUpdatedAt(now);
    entityManager.persist(bevCat);

    Category bakeCat = new Category();
    bakeCat.setName("Spec Bakery");
    bakeCat.setIsActive(true);
    bakeCat.setCreatedAt(now);
    bakeCat.setUpdatedAt(now);
    entityManager.persist(bakeCat);

    beverageCategory = new StoreCategory();
    beverageCategory.setStore(store);
    beverageCategory.setCategory(bevCat);
    beverageCategory.setIsActive(true);
    beverageCategory.setDisplayOrder(1);
    beverageCategory.setCreatedAt(now);
    beverageCategory.setUpdatedAt(now);
    entityManager.persist(beverageCategory);

    bakeryCategory = new StoreCategory();
    bakeryCategory.setStore(store);
    bakeryCategory.setCategory(bakeCat);
    bakeryCategory.setIsActive(true);
    bakeryCategory.setDisplayOrder(2);
    bakeryCategory.setCreatedAt(now);
    bakeryCategory.setUpdatedAt(now);
    entityManager.persist(bakeryCategory);

    // Product 1: Classic Milk Tea with M and L variants (MADE_TO_ORDER)
    milkTeaProduct = new Product();
    milkTeaProduct.setStore(store);
    milkTeaProduct.setStoreCategory(beverageCategory);
    milkTeaProduct.setName("Pearl Milk Tea");
    milkTeaProduct.setDescription("Delicious brown sugar pearl tea");
    milkTeaProduct.setIsActive(true);
    milkTeaProduct.setCreatedAt(now);
    milkTeaProduct.setUpdatedAt(now);
    entityManager.persist(milkTeaProduct);

    ProductVariant teaM =
        createVariant(
            milkTeaProduct, "M", "M", "35000.00", InventoryMode.MADE_TO_ORDER, true, true);
    ProductVariant teaL =
        createVariant(
            milkTeaProduct, "L", "L", "45000.00", InventoryMode.MADE_TO_ORDER, true, true);
    milkTeaProduct.setVariants(new ArrayList<>(List.of(teaM, teaL)));

    // Product 2: Butter Croissant with STANDARD variant (LIMITED_STOCK)
    croissantProduct = new Product();
    croissantProduct.setStore(store);
    croissantProduct.setStoreCategory(bakeryCategory);
    croissantProduct.setName("Almond Croissant");
    croissantProduct.setDescription("Flaky french pastry");
    croissantProduct.setIsActive(true);
    croissantProduct.setCreatedAt(now);
    croissantProduct.setUpdatedAt(now);
    entityManager.persist(croissantProduct);

    ProductVariant croissantStd =
        createVariant(
            croissantProduct,
            "STANDARD",
            null,
            "28000.00",
            InventoryMode.LIMITED_STOCK,
            true,
            true);
    croissantProduct.setVariants(new ArrayList<>(List.of(croissantStd)));

    // Product 3: Inactive product (should NEVER appear in public catalog)
    inactiveProduct = new Product();
    inactiveProduct.setStore(store);
    inactiveProduct.setStoreCategory(beverageCategory);
    inactiveProduct.setName("Old Winter Melon Tea");
    inactiveProduct.setDescription("Discontinued tea");
    inactiveProduct.setIsActive(false);
    inactiveProduct.setCreatedAt(now);
    inactiveProduct.setUpdatedAt(now);
    entityManager.persist(inactiveProduct);

    ProductVariant inactiveVariant =
        createVariant(
            inactiveProduct,
            "STANDARD",
            null,
            "20000.00",
            InventoryMode.MADE_TO_ORDER,
            true,
            false);
    inactiveProduct.setVariants(new ArrayList<>(List.of(inactiveVariant)));

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void filtersActiveProductsOnly_excludingInactiveProducts() {
    Specification<Product> spec =
        ProductSpecifications.withFilter(
            store.getId(), ProductFilterCriteria.publicCatalog(null, null, null, null, null));

    List<Product> products = productRepository.findAll(spec);

    assertEquals(2, products.size());
    assertTrue(products.stream().noneMatch(p -> Boolean.FALSE.equals(p.getIsActive())));
    assertTrue(products.stream().anyMatch(p -> p.getName().equals("Pearl Milk Tea")));
    assertTrue(products.stream().anyMatch(p -> p.getName().equals("Almond Croissant")));
  }

  @Test
  void searchesKeywordInNameAndDescription() {
    Specification<Product> spec =
        ProductSpecifications.withFilter(
            store.getId(), ProductFilterCriteria.publicCatalog("pearl", null, null, null, null));

    List<Product> products = productRepository.findAll(spec);

    assertEquals(1, products.size());
    assertEquals("Pearl Milk Tea", products.get(0).getName());
  }

  @Test
  void filtersByStoreCategory() {
    Specification<Product> spec =
        ProductSpecifications.withFilter(
            store.getId(),
            ProductFilterCriteria.publicCatalog(null, bakeryCategory.getId(), null, null, null));

    List<Product> products = productRepository.findAll(spec);

    assertEquals(1, products.size());
    assertEquals("Almond Croissant", products.get(0).getName());
  }

  @Test
  void filtersBySizedVariant_M() {
    Specification<Product> spec =
        ProductSpecifications.withFilter(
            store.getId(), ProductFilterCriteria.publicCatalog(null, null, "M", null, null));

    List<Product> products = productRepository.findAll(spec);

    assertEquals(1, products.size());
    assertEquals("Pearl Milk Tea", products.get(0).getName());
  }

  @Test
  void filtersByStandardVariant_NullSize() {
    Specification<Product> spec =
        ProductSpecifications.withFilter(
            store.getId(), ProductFilterCriteria.publicCatalog(null, null, "STANDARD", null, null));

    List<Product> products = productRepository.findAll(spec);

    assertEquals(1, products.size());
    assertEquals("Almond Croissant", products.get(0).getName());
  }

  @Test
  void filtersByInventoryMode() {
    Specification<Product> spec =
        ProductSpecifications.withFilter(
            store.getId(),
            ProductFilterCriteria.publicCatalog(
                null, null, null, InventoryMode.LIMITED_STOCK, null));

    List<Product> products = productRepository.findAll(spec);

    assertEquals(1, products.size());
    assertEquals("Almond Croissant", products.get(0).getName());
  }

  @Test
  void supportsPaginationAndSorting() {
    Specification<Product> spec =
        ProductSpecifications.withFilter(
            store.getId(), ProductFilterCriteria.publicCatalog(null, null, null, null, null));

    PageRequest pageRequest = PageRequest.of(0, 1, Sort.by("name").ascending());
    Page<Product> page = productRepository.findAll(spec, pageRequest);

    assertEquals(1, page.getContent().size());
    assertEquals(2, page.getTotalElements());
    assertEquals(2, page.getTotalPages());
    assertEquals("Almond Croissant", page.getContent().get(0).getName());
    assertTrue(page.hasNext());
    assertFalse(page.isLast());
  }

  private ProductVariant createVariant(
      Product product,
      String name,
      String size,
      String price,
      InventoryMode mode,
      boolean available,
      boolean active) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    variant.setName(name);
    variant.setSize(size);
    variant.setPrice(new BigDecimal(price));
    variant.setInventoryMode(mode);
    variant.setAutoAcceptOverride(null);
    variant.setMaxQuantityPerOrder(10);
    variant.setIsAvailable(available);
    variant.setIsActive(active);
    variant.setCreatedAt(now);
    variant.setUpdatedAt(now);
    entityManager.persist(variant);
    return variant;
  }
}
