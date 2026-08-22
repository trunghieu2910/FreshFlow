# FreshFlow — Domain Design for FF-01-02-2

## Purpose

This document defines the implementation design for the first FreshFlow domain layer. The goal is to let the developer implement the classes and tests in one pass, then submit the complete files for review.

The scope contains four main domain types:

```text
Money
AddressSnapshot
OrderItemSnapshot
OrderPricingCalculator
```

The implementation belongs in the Spring Boot service:

```text
services/freshow-api/src/main/java/com/freshflow/api/domain/
```

The project currently uses the package:

```java
com.freshflow.api.order.domain.domain
```

The project folder is still named `freshow-api`, but the Java package `com.freshflow.api` is correct and should remain consistent for this task.

---

## 1. Domain relationships

The following relationship describes the intended responsibility of each class:

```text
OrderItemSnapshot
├── productVariantId
├── productName
├── variantName
├── unitPrice: Money
├── quantity
└── lineTotal: Money

OrderPricingCalculator
├── receives a non-empty list of OrderItemSnapshot
├── calculates subtotal from lineTotal
├── calculates a fixed or percentage discount
├── adds delivery fee
└── returns PricingResult
```

`Money` is the low-level value object used by the other domain classes. `AddressSnapshot` is independent from pricing and stores the delivery address exactly as it was submitted at checkout.

> The most important rule is that historical order data must not change when the original product, price or customer address changes later.

---

# 2. `Money`

## 2.1 Purpose

`Money` represents one valid monetary amount in FreshFlow. It prevents the rest of the domain from passing unvalidated `BigDecimal` values everywhere.

Typical usage includes:

| Business field | Type |
|---|---|
| ProductVariant unit price | `Money` |
| Order item unit price | `Money` |
| Order item line total | `Money` |
| Order subtotal | `Money` |
| Discount amount | `Money` |
| Delivery fee | `Money` |
| Grand total | `Money` |
| Payment amount | `Money` |
| Refund amount | `Money` |

The MVP uses VND and does not need a `currency` field yet.

## 2.2 Fields

```java
private final BigDecimal amount;
```

The field must be `private` and `final`. There must be no setter. Every operation returns a new `Money` object instead of changing the current one.

## 2.3 Constructor rules

```java
public Money(BigDecimal amount)
```

The constructor must apply these rules:

| Input | Expected behavior |
|---|---|
| `null` | Throw `IllegalArgumentException` |
| Negative value | Throw `IllegalArgumentException` |
| `0` | Allow and store as `0.00` |
| `100` | Allow and store as `100.00` |
| `100.0` | Allow and store as `100.00` |
| `100.000` | Allow and store as `100.00` |
| `100.123` | Throw `IllegalArgumentException` |

The value must be normalized with scale 2. The input must not be silently rounded. Use `RoundingMode.UNNECESSARY` during input normalization. If normalization throws `ArithmeticException`, convert it to `IllegalArgumentException`.

## 2.4 Public API

```java
public BigDecimal getAmount()
public Money add(Money other)
public Money subtract(Money other)
public Money multiply(BigDecimal multiplier)
public int compareTo(Money other)
public boolean isZero()
public boolean isGreaterThan(Money other)
public boolean isLessThan(Money other)
@Override public boolean equals(Object object)
@Override public int hashCode()
@Override public String toString()
```

## 2.5 `add`

```java
public Money add(Money other)
```

Rules:

```text
other == null → IllegalArgumentException
result = this.amount + other.amount
return a new Money(result)
do not mutate either operand
```

Example:

```text
Money(100.00).add(Money(25.50)) = Money(125.50)
```

## 2.6 `subtract`

```java
public Money subtract(Money other)
```

Rules:

```text
other == null → IllegalArgumentException
result < 0 → IllegalArgumentException
result >= 0 → return a new Money(result)
do not mutate either operand
```

Example:

```text
Money(100.00).subtract(Money(25.50)) = Money(74.50)
Money(25.50).subtract(Money(100.00)) → exception
```

The pricing calculator must ensure that the applied discount is never greater than the subtotal before calling `subtract`.

## 2.7 `multiply`

```java
public Money multiply(BigDecimal multiplier)
```

This method is used for both item totals and percentage discounts:

```text
unitPrice × quantity = lineTotal
subtotal × percentageFactor = discountAmount
```

Rules:

| Input | Expected behavior |
|---|---|
| `null` | Throw `IllegalArgumentException` |
| Negative multiplier | Throw `IllegalArgumentException` |
| Zero multiplier | Allow and return `0.00` |
| Positive multiplier | Multiply and return a new `Money` |
| More than 2 result decimals | Round result to scale 2 using `HALF_UP` |

Examples:

```text
Money(35000.00).multiply(BigDecimal.valueOf(2)) = Money(70000.00)
Money(100000.00).multiply(new BigDecimal("0.10")) = Money(10000.00)
Money(1.00).multiply(new BigDecimal("0.005")) = Money(0.01)
```

Do not use `double` when creating the multiplier. Use a string or `BigDecimal.valueOf`.

## 2.8 Comparison methods

```java
public int compareTo(Money other)
```

`compareTo` returns a negative number when this amount is smaller, zero when the amounts are equal, and a positive number when this amount is larger. `other == null` must be rejected with `IllegalArgumentException`.

The convenience methods should delegate to `compareTo`:

```java
public boolean isZero()
public boolean isGreaterThan(Money other)
public boolean isLessThan(Money other)
```

Use `amount.signum() == 0` for `isZero`. Do not compare `BigDecimal` using `==`.

## 2.9 Value-object methods

Two `Money` objects with the same normalized amount must be equal. `equals` and `hashCode` must use the amount value. `toString` should return a readable amount such as `100.00`.

Do not include database IDs, product IDs or currency text in `Money` equality.

---

# 3. `AddressSnapshot`

## 3.1 Purpose

`AddressSnapshot` stores the delivery address copied into an order at checkout. It is a snapshot, not a live reference to the customer's editable address book.

If the customer changes their profile address after ordering, the old order must continue to display the original delivery address.

The MVP does not include GPS coordinates in this class because the current flow does not calculate delivery fees using GPS.

## 3.2 Fields

```java
private final String recipientName;
private final String phone;
private final String addressLine;
private final String ward;
private final String district;
private final String province;
```

## 3.3 Constructor

```java
public AddressSnapshot(
    String recipientName,
    String phone,
    String addressLine,
    String ward,
    String district,
    String province
)
```

All six fields are required. A field is invalid when it is `null` or blank after trimming. Invalid input must throw `IllegalArgumentException`.

The class should store trimmed values so that accidental surrounding spaces do not become part of the snapshot.

Example:

```text
recipientName = "Nguyen Van A"
phone         = "0901234567"
addressLine   = "12 Nguyen Trai"
ward          = "Ben Thanh"
district      = "District 1"
province      = "Ho Chi Minh City"
```

No GPS field, geocoding logic, address lookup or delivery-fee calculation belongs in this class.

## 3.4 Public API

```java
public String getRecipientName()
public String getPhone()
public String getAddressLine()
public String getWard()
public String getDistrict()
public String getProvince()
```

Implement `equals`, `hashCode` and `toString` using all six fields because this is an immutable value object.

## 3.5 Address tests

Create:

```text
src/test/java/com/freshflow/api/domain/AddressSnapshotTest.java
```

Minimum tests:

```text
shouldCreateValidAddressSnapshot
shouldTrimTextFields
shouldRejectNullRecipientName
shouldRejectBlankRecipientName
shouldRejectNullPhone
shouldRejectBlankAddressLine
shouldRejectNullWard
shouldRejectNullDistrict
shouldRejectNullProvince
shouldBeEqualWhenAllFieldsAreEqual
```

The tests should verify that the snapshot stores the submitted values independently of any external mutable object. Since Java `String` is immutable, no deep copy is required.

---

# 4. `OrderItemSnapshot`

## 4.1 Purpose

`OrderItemSnapshot` stores one purchased variant exactly as it was at checkout. It prevents a later product rename, price change or variant change from modifying historical order data.

The customer purchases a `ProductVariant`, not a generic `Product`. A product without size still has a default variant name such as `STANDARD`.

## 4.2 Fields

```java
private final Long productVariantId;
private final String productName;
private final String variantName;
private final Money unitPrice;
private final int quantity;
private final Money lineTotal;
```

`lineTotal` is derived by the constructor and must not be passed independently by the caller. This prevents inconsistent data such as `unitPrice = 30.00`, `quantity = 2`, but `lineTotal = 50.00`.

## 4.3 Constructor

```java
public OrderItemSnapshot(
    Long productVariantId,
    String productName,
    String variantName,
    Money unitPrice,
    int quantity
)
```

Validation rules:

| Field | Rule |
|---|---|
| `productVariantId` | Must not be null and must be greater than 0 |
| `productName` | Must not be null or blank |
| `variantName` | Must not be null or blank; use `STANDARD` for no-size products |
| `unitPrice` | Must not be null; `Money` already rejects invalid prices |
| `quantity` | Must be greater than 0 |
| `lineTotal` | Computed as `unitPrice.multiply(BigDecimal.valueOf(quantity))` |

The constructor should trim text fields. It should calculate `lineTotal` only after all inputs pass validation.

## 4.4 Public API

```java
public Long getProductVariantId()
public String getProductName()
public String getVariantName()
public Money getUnitPrice()
public int getQuantity()
public Money getLineTotal()
```

The class should be immutable and should implement `equals`, `hashCode` and `toString` using all fields.

## 4.5 Order item examples

Example with a size variant:

```text
productVariantId = 101
productName      = "Burger"
variantName      = "LARGE"
unitPrice        = 65000.00
quantity         = 2
lineTotal        = 130000.00
```

Example without a meaningful size:

```text
productVariantId = 102
productName      = "Banh mi"
variantName      = "STANDARD"
unitPrice        = 25000.00
quantity         = 1
lineTotal        = 25000.00
```

## 4.6 Order item tests

Create:

```text
src/test/java/com/freshflow/api/domain/OrderItemSnapshotTest.java
```

Minimum tests:

```text
shouldCalculateLineTotal
shouldCalculateLineTotalForMultipleQuantity
shouldRejectNullProductVariantId
shouldRejectNonPositiveProductVariantId
shouldRejectBlankProductName
shouldRejectBlankVariantName
shouldRejectNullUnitPrice
shouldRejectZeroQuantity
shouldRejectNegativeQuantity
shouldKeepSnapshotValues
```

The most important test is:

```text
unitPrice = 30000.00
quantity  = 2
lineTotal = 60000.00
```

---

# 5. `OrderPricingCalculator`

## 5.1 Purpose

`OrderPricingCalculator` calculates the final price of a cart after the item snapshots, discount and delivery fee are known. It does not access the database, payment gateway, inventory or product service.

The formula is:

```text
subtotal      = sum(all lineTotal values)
discountAmount = fixed discount or percentage discount
grandTotal    = max(0, subtotal - discountAmount + deliveryFee)
```

The discount must be limited so that it cannot make the amount before delivery fee negative.

## 5.2 Nested discount type

To avoid adding another source file in this task, define an enum inside `OrderPricingCalculator`:

```java
public enum DiscountType {
    NONE,
    FIXED_AMOUNT,
    PERCENTAGE
}
```

Meaning:

| Type | Meaning of `discountValue` |
|---|---|
| `NONE` | Must be zero; no discount |
| `FIXED_AMOUNT` | A VND amount such as `20000.00` |
| `PERCENTAGE` | A percentage from `0` to `100`, such as `10` for 10% |

## 5.3 Pricing result

Define a nested immutable result class inside `OrderPricingCalculator` so no extra source file is required for this task:

```java
public static final class PricingResult {
    private final Money subtotal;
    private final Money discountAmount;
    private final Money deliveryFee;
    private final Money grandTotal;

    // constructor and getters
}
```

Required getters:

```java
public Money getSubtotal()
public Money getDiscountAmount()
public Money getDeliveryFee()
public Money getGrandTotal()
```

`PricingResult` should also implement `equals`, `hashCode` and `toString`.

## 5.4 Calculator method

```java
public PricingResult calculate(
    List<OrderItemSnapshot> items,
    DiscountType discountType,
    BigDecimal discountValue,
    Money deliveryFee
)
```

Validation rules:

| Input | Rule |
|---|---|
| `items` | Must not be null or empty |
| An item in `items` | Must not be null |
| `discountType` | Must not be null |
| `discountValue` | Must not be null and must not be negative |
| `deliveryFee` | Must not be null; `Money` already guarantees non-negative |
| Percentage value | Must be between 0 and 100 |
| `NONE` | `discountValue` must be zero |

## 5.5 Calculation steps

Implement the method in this order:

```text
1. Validate all inputs.
2. Start subtotal at Money(0.00).
3. Add every item.lineTotal to subtotal.
4. Calculate the requested discount.
5. If discount > subtotal, replace discount with subtotal.
6. Calculate subtotal - discount.
7. Add delivery fee.
8. Return PricingResult.
```

Start value:

```java
Money subtotal = new Money(BigDecimal.ZERO);
```

For a fixed discount:

```text
discountAmount = Money(discountValue)
```

For a percentage discount:

```text
percentageFactor = discountValue / 100
rawDiscount = subtotal.multiply(percentageFactor)
```

The `Money.multiply` method performs the final scale-2 `HALF_UP` rounding.

For a discount larger than subtotal:

```text
subtotal = 100.00
fixed discount = 150.00
a pplied discount = 100.00
```

Then:

```text
grandTotal = 100.00 - 100.00 + deliveryFee
```

## 5.6 Pricing examples

| Items | Subtotal | Discount | Delivery | Grand total |
|---|---:|---:|---:|---:|
| `30000 × 2` | 60000.00 | 0.00 | 15000.00 | 75000.00 |
| `30000 × 2` | 60000.00 | fixed 10000.00 | 15000.00 | 65000.00 |
| `30000 × 2` | 60000.00 | 10% = 6000.00 | 15000.00 | 69000.00 |
| `30000 × 2` | 60000.00 | fixed 100000.00, capped | 15000.00 | 15000.00 |

## 5.7 Pricing tests

Create:

```text
src/test/java/com/freshflow/api/domain/OrderPricingCalculatorTest.java
```

Minimum test cases:

```text
shouldRejectNullItems
shouldRejectEmptyItems
shouldRejectNullItemInsideList
shouldCalculateSubtotalFromLineTotals
shouldCalculateGrandTotalWithoutDiscount
shouldCalculateFixedDiscount
shouldCalculatePercentageDiscount
shouldRoundPercentageDiscountUsingHalfUp
shouldCapDiscountAtSubtotal
shouldCalculateDeliveryFee
shouldRejectNullDiscountType
shouldRejectNullDiscountValue
shouldRejectNegativeDiscountValue
shouldRejectPercentageGreaterThan100
shouldRejectNonZeroDiscountValueForNone
```

Recommended main scenario:

```text
item 1: unitPrice 30000.00, quantity 2, lineTotal 60000.00
item 2: unitPrice 25000.00, quantity 1, lineTotal 25000.00
subtotal: 85000.00
discount: 10% = 8500.00
delivery: 15000.00
grandTotal: 91500.00
```

## 5.8 Empty cart behavior

An empty list is invalid for checkout and must throw `IllegalArgumentException`. The calculator should not return a result with a zero subtotal for an empty order.

This is different from the internal starting value used while summing a non-empty list:

```text
empty cart → reject
non-empty cart → start subtotal at 0.00, then add items
```

---

# 6. Required source files

After implementation, the minimum source structure should be:

```text
src/main/java/com/freshflow/api/domain/
├── Money.java
├── AddressSnapshot.java
├── OrderItemSnapshot.java
└── OrderPricingCalculator.java
```

The minimum test structure should be:

```text
src/test/java/com/freshflow/api/domain/
├── MoneyTest.java
├── AddressSnapshotTest.java
├── OrderItemSnapshotTest.java
└── OrderPricingCalculatorTest.java
```

Do not create JPA entities, repositories, controllers, database migrations or payment classes for this task. FF-01-02-2 is only the domain calculation foundation.

---

# 7. Implementation checklist

Before submitting the code for review, verify the following:

| Check | Expected result |
|---|---|
| Money uses `BigDecimal` | Yes |
| No money calculation uses `double` | Yes |
| Money is immutable | `final` fields, no setters |
| Negative money is rejected | Yes |
| Money input is normalized to scale 2 | Yes |
| Invalid input is not silently rounded | Yes |
| Add and subtract return new objects | Yes |
| Multiply uses `HALF_UP` | Yes |
| Address snapshot has no GPS fields | Yes |
| Order item stores ProductVariant ID | Yes |
| Order item calculates line total internally | Yes |
| Empty cart is rejected | Yes |
| Discount supports fixed amount and percentage | Yes |
| Discount cannot make grand total negative | Yes |
| Delivery fee is included | Yes |
| Every important rule has a unit test | Yes |

Once all code is written, run the entire test suite from IntelliJ or Maven. Then send the complete contents of all four main classes and all test classes for a single review pass.

## References

This document is a project-specific implementation design based on the FreshFlow domain decisions already established in the repository documentation. It does not introduce an external framework or database requirement for FF-01-02-2.
