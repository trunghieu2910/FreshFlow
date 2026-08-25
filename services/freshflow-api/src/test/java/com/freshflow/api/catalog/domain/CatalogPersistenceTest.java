package com.freshflow.api.catalog.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CatalogPersistenceTest {

  @Autowired private EntityManager entityManager;

  @Test
  void shouldPersistSizedVariantsAndStandardVariant() {
    Product product = persistProduct("Milk Tea");

    ProductVariant medium = newVariant(product, "M", "M", "35.00");
    ProductVariant large = newVariant(product, "L", "L", "40.00");
    product.setVariants(new java.util.ArrayList<>(java.util.List.of(medium, large)));

    entityManager.flush();
    entityManager.clear();

    Product persistedProduct = entityManager.find(Product.class, product.getId());

    assertEquals(2, persistedProduct.getVariants().size());
    assertEquals("M", persistedProduct.getVariants().get(0).getName());
    assertEquals("L", persistedProduct.getVariants().get(1).getName());
    assertEquals(new BigDecimal("35.00"), persistedProduct.getVariants().get(0).getPrice());
    assertEquals(new BigDecimal("40.00"), persistedProduct.getVariants().get(1).getPrice());
  }

  @Test
  void shouldPersistStandardVariantWithNullSize() {
    Product product = persistProduct("Black Coffee");
    ProductVariant standard = newVariant(product, "STANDARD", null, "25.00");
    product.setVariants(new java.util.ArrayList<>(java.util.List.of(standard)));

    entityManager.flush();
    entityManager.clear();

    Product persistedProduct = entityManager.find(Product.class, product.getId());
    ProductVariant persistedVariant = persistedProduct.getVariants().get(0);

    assertEquals("STANDARD", persistedVariant.getName());
    assertNull(persistedVariant.getSize());
    assertEquals(new BigDecimal("25.00"), persistedVariant.getPrice());
  }

  @Test
  void shouldRejectDuplicateVariantNameWithinProduct() {
    Product product = persistProduct("Fruit Tea");
    ProductVariant first = newVariant(product, "M", "M", "30.00");
    product.setVariants(new java.util.ArrayList<>(java.util.List.of(first)));
    entityManager.flush();

    ProductVariant duplicate = newVariant(product, "M", "M", "32.00");
    product.getVariants().add(duplicate);

    assertThrows(
        PersistenceException.class,
        () -> {
          entityManager.flush();
          entityManager.clear();
        });
  }

  private Product persistProduct(String productName) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    User owner = new User();
    owner.setEmail(productName.toLowerCase().replace(' ', '.') + "@example.test");
    owner.setPasswordHash("test-password-hash");
    owner.setFullName("Test Owner");
    owner.setStatus("ACTIVE");
    owner.setCreatedAt(now);
    owner.setUpdatedAt(now);
    entityManager.persist(owner);

    Store store = new Store();
    store.setOwnerUser(owner);
    store.setName(productName + " Store");
    store.setAddressLine("Test address");
    store.setAutoAcceptDefault(false);
    store.setStatus("ACTIVE");
    store.setCreatedAt(now);
    store.setUpdatedAt(now);
    entityManager.persist(store);

    Category category = new Category();
    category.setName(productName + " Category");
    category.setIsActive(true);
    category.setCreatedAt(now);
    category.setUpdatedAt(now);
    entityManager.persist(category);

    StoreCategory storeCategory = new StoreCategory();
    storeCategory.setStore(store);
    storeCategory.setCategory(category);
    storeCategory.setIsActive(true);
    storeCategory.setDisplayOrder(1);
    storeCategory.setCreatedAt(now);
    storeCategory.setUpdatedAt(now);
    entityManager.persist(storeCategory);

    Product product = new Product();
    product.setStore(store);
    product.setStoreCategory(storeCategory);
    product.setName(productName);
    product.setIsActive(true);
    product.setCreatedAt(now);
    product.setUpdatedAt(now);
    entityManager.persist(product);

    return product;
  }

  private ProductVariant newVariant(Product product, String name, String size, String price) {
    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    variant.setName(name);
    variant.setSize(size);
    variant.setPrice(new BigDecimal(price));
    variant.setInventoryMode(InventoryMode.MADE_TO_ORDER);
    variant.setAutoAcceptOverride(null);
    variant.setMaxQuantityPerOrder(5);
    variant.setIsAvailable(true);
    variant.setIsActive(true);
    variant.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    variant.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return variant;
  }
}
