# FreshFlow — Thiết kế Domain cho Task FF-01-02-2

## 1. Task này là gì?

Task **FF-01-02-2** là bước xây dựng phần **domain nghiệp vụ cốt lõi** cho backend FreshFlow. Trong task này, bạn chưa tạo API, chưa kết nối database và chưa viết giao diện. Bạn đang xây dựng các lớp Java đại diện cho những quy tắc quan trọng nhất của một hệ thống đặt món:

```text
Money
AddressSnapshot
OrderItemSnapshot
OrderPricingCalculator
```

Các file Java sẽ được đặt tại:

```text
D:\FreshFlow\services\freshow-api\src\main\java\com\freshflow\api\domain\
```

Package Java:

```java
com.freshflow.api.domain
```

Các file test sẽ được đặt tại:

```text
D:\FreshFlow\services\freshow-api\src\test\java\com\freshflow\api\domain\
```

---

# 2. Vì sao phải làm task này?

## 2.1. Vì FreshFlow cần quy tắc nghiệp vụ trước khi có API và database

Một hệ thống đặt món không chỉ là các màn hình và endpoint. Trước khi viết API checkout, hệ thống phải biết chính xác:

```text
Giá tiền nào hợp lệ?
Số lượng món nào hợp lệ?
Tổng tiền được tính như thế nào?
Discount lớn hơn subtotal xử lý ra sao?
Địa chỉ cũ của đơn hàng có bị thay đổi khi khách sửa địa chỉ mới không?
Giá món thay đổi sau khi đặt hàng có làm đơn cũ thay đổi không?
```

Nếu những quy tắc này không được chốt trong domain, mỗi controller hoặc service có thể tự tính theo một cách khác nhau. Khi đó cùng một đơn hàng có thể cho ra kết quả khác nhau giữa Web Merchant, Android Customer, Android Driver và backend.

> Domain là nơi tập trung các quy tắc nghiệp vụ để những phần còn lại của hệ thống không tự suy diễn hoặc tự tính tiền theo cách riêng.

## 2.2. Vì tiền là phần có rủi ro cao nhất trong hệ thống đặt món

FreshFlow có nhiều trường tiền:

| Nghiệp vụ | Giá trị tiền |
|---|---|
| ProductVariant | Giá bán |
| OrderItemSnapshot | Đơn giá và thành tiền của từng món |
| Order | Subtotal, discount, phí giao hàng, grand total |
| Payment | Số tiền khách thanh toán |
| Refund | Số tiền hoàn lại |

Nếu sử dụng `double`, phép tính tiền có thể phát sinh sai số. Vì vậy task này tạo lớp `Money` để buộc hệ thống:

```text
Dùng BigDecimal.
Không cho số tiền âm.
Chuẩn hóa scale thống nhất.
Không tự động làm tròn dữ liệu đầu vào sai.
Các phép tính tiền tạo ra đối tượng Money mới.
```

## 2.3. Vì đơn hàng phải lưu lịch sử, không được phụ thuộc dữ liệu hiện tại

Giả sử khách đặt một Burger giá 50.000 VND. Ngày hôm sau Merchant đổi tên sản phẩm thành Burger đặc biệt và đổi giá thành 60.000 VND.

Đơn cũ vẫn phải hiển thị:

```text
Tên lúc đặt: Burger
Giá lúc đặt: 50.000 VND
```

Đó là lý do cần `OrderItemSnapshot`: lưu bản sao thông tin món tại thời điểm checkout.

Tương tự, nếu khách đổi địa chỉ sau khi đặt hàng, Driver vẫn phải giao đến địa chỉ cũ đã được chốt trong đơn. Đó là lý do cần `AddressSnapshot`.

## 2.4. Vì đây là nền tảng để viết unit test và chứng minh năng lực

Mục tiêu của bạn không chỉ là làm cho ứng dụng chạy. Bạn cần portfolio đủ mạnh để xin intern. Một repository tốt nên cho nhà tuyển dụng thấy rằng bạn biết:

```text
Thiết kế domain model.
Tách nghiệp vụ khỏi controller.
Viết unit test trước hoặc song song với code.
Xử lý validation và edge case.
Bảo vệ dữ liệu lịch sử của đơn hàng.
Tính tiền chính xác bằng BigDecimal.
```

Task này tạo ra một phần dễ nhìn thấy trong GitHub: code domain có trách nhiệm rõ ràng và test bao phủ các trường hợp quan trọng.

## 2.5. Task này liên quan trực tiếp đến 3 môn học

| Môn học | Kiến thức được áp dụng trong task |
|---|---|
| Microservices — MSS301 | Tách domain logic khỏi API, chuẩn bị ranh giới module và service |
| Android App — PRM392 | Backend trả dữ liệu đơn hàng ổn định cho Customer và Driver app |
| React RESTful API — SBA301 | Web Merchant sử dụng kết quả pricing và snapshot nhất quán từ REST API |
| Java/Spring Boot | Package, class, immutable object, exception, unit test |
| Database | Các trường domain sau này sẽ ánh xạ vào bảng order, order_items, payments và addresses |

---

# 3. Quan hệ giữa các class

```text
Money
└── Được sử dụng bởi OrderItemSnapshot và OrderPricingCalculator

AddressSnapshot
└── Lưu địa chỉ giao hàng tại thời điểm checkout

OrderItemSnapshot
├── Lưu thông tin ProductVariant tại thời điểm checkout
├── Dùng Money cho unitPrice và lineTotal
└── Tự tính lineTotal từ unitPrice × quantity

OrderPricingCalculator
├── Nhận danh sách OrderItemSnapshot
├── Tính subtotal
├── Tính discount cố định hoặc phần trăm
├── Cộng delivery fee
└── Trả về PricingResult
```

---

# 4. Thiết kế class `Money`

## 4.1. `Money` dùng để làm gì?

`Money` là **Value Object** đại diện cho một khoản tiền hợp lệ trong FreshFlow. Nó không phải Entity, không có ID database và không đại diện riêng cho Payment hay Refund.

Nó chỉ trả lời câu hỏi:

> Khoản tiền này có hợp lệ không và các phép tính với nó có an toàn không?

Ví dụ:

```java
Money price = new Money(new BigDecimal("35000.00"));
```

Đối tượng trên đại diện cho 35.000 VND.

## 4.2. Thuộc tính

```java
private final BigDecimal amount;
```

Yêu cầu:

```text
private.
final.
Không có setter.
Luôn được chuẩn hóa về scale 2.
```

`Money` phải immutable. Sau khi tạo, số tiền bên trong không được thay đổi. Các phép cộng, trừ, nhân đều trả về một `Money` mới.

## 4.3. Quy tắc constructor

```java
public Money(BigDecimal amount)
```

| Giá trị đầu vào | Kết quả mong muốn |
|---|---|
| `null` | Ném `IllegalArgumentException` |
| `-100.00` | Ném `IllegalArgumentException` |
| `0` | Cho phép, lưu thành `0.00` |
| `100` | Cho phép, lưu thành `100.00` |
| `100.0` | Cho phép, lưu thành `100.00` |
| `100.000` | Cho phép, lưu thành `100.00` |
| `100.123` | Ném `IllegalArgumentException` |

Khi chuẩn hóa input, dùng `RoundingMode.UNNECESSARY`. Không được âm thầm làm tròn giá trị đầu vào sai. Nếu `setScale` ném `ArithmeticException`, chuyển thành `IllegalArgumentException`.

## 4.4. API đầy đủ

```java
public BigDecimal getAmount();
public Money add(Money other);
public Money subtract(Money other);
public Money multiply(BigDecimal multiplier);
public int compareTo(Money other);
public boolean isZero();
public boolean isGreaterThan(Money other);
public boolean isLessThan(Money other);
@Override public boolean equals(Object object);
@Override public int hashCode();
@Override public String toString();
```

## 4.5. `add`

```java
public Money add(Money other)
```

Quy tắc:

```text
other == null → IllegalArgumentException.
Kết quả = amount + other.amount.
Trả về Money mới.
Không thay đổi hai object ban đầu.
```

Ví dụ:

```text
Money(100.00).add(Money(25.50)) = Money(125.50)
```

## 4.6. `subtract`

```java
public Money subtract(Money other)
```

Quy tắc:

```text
other == null → IllegalArgumentException.
Kết quả âm → IllegalArgumentException.
Kết quả bằng hoặc lớn hơn 0 → trả về Money mới.
Không thay đổi hai object ban đầu.
```

Ví dụ:

```text
Money(100.00).subtract(Money(25.50)) = Money(74.50)
Money(25.50).subtract(Money(100.00)) → exception
```

## 4.7. `multiply`

```java
public Money multiply(BigDecimal multiplier)
```

Method này được dùng cho:

```text
unitPrice × quantity = lineTotal.
subtotal × phần trăm = discountAmount.
```

| Multiplier | Kết quả |
|---|---|
| `null` | Ném `IllegalArgumentException` |
| Âm | Ném `IllegalArgumentException` |
| `0` | Cho phép, trả về `0.00` |
| Dương | Nhân và trả về Money mới |
| Kết quả có nhiều hơn 2 chữ số lẻ | Làm tròn về scale 2 bằng `HALF_UP` |

Ví dụ:

```text
Money(35000.00).multiply(BigDecimal.valueOf(2)) = Money(70000.00)
Money(100000.00).multiply(new BigDecimal("0.10")) = Money(10000.00)
Money(1.00).multiply(new BigDecimal("0.005")) = Money(0.01)
```

Không dùng `double`. Hãy dùng:

```java
new BigDecimal("0.10")
BigDecimal.valueOf(2)
```

## 4.8. So sánh và kiểm tra zero

```java
public int compareTo(Money other)
public boolean isZero()
public boolean isGreaterThan(Money other)
public boolean isLessThan(Money other)
```

`compareTo` trả về số âm nếu nhỏ hơn, `0` nếu bằng và số dương nếu lớn hơn. `other == null` phải bị từ chối.

`isZero` nên kiểm tra bằng:

```java
amount.signum() == 0
```

Không dùng:

```java
amount == BigDecimal.ZERO
```

vì đó là so sánh địa chỉ object, không phải so sánh giá trị.

## 4.9. `equals`, `hashCode`, `toString`

Vì `Money` là Value Object:

```text
Money(100.00).equals(Money(100.00)) → true
Money(100.00).equals(Money(200.00)) → false
```

`equals` và `hashCode` phải dựa trên `amount`. `toString()` nên trả về chuỗi dễ đọc như:

```text
100.00
```

## 4.10. Test cần có cho `Money`

File:

```text
src/test/java/com/freshflow/api/domain/MoneyTest.java
```

Các test tối thiểu:

```text
shouldAllowZeroAmount
shouldRejectNullAmount
shouldRejectNegativeAmount
shouldRejectAmountWithMoreThanTwoDecimalPlaces
shouldNormalizeTrailingZerosToScaleTwo
shouldAddTwoMoneyValues
shouldRejectNullWhenAdding
shouldKeepOriginalValuesWhenAdding
shouldSubtractTwoMoneyValues
shouldRejectNullWhenSubtracting
shouldRejectNegativeResultWhenSubtracting
shouldKeepOriginalValuesWhenSubtracting
shouldMultiplyMoneyByPositiveValue
shouldMultiplyMoneyByZero
shouldRejectNullWhenMultiplying
shouldRejectNegativeMultiplier
shouldRoundMultiplicationResultUsingHalfUp
shouldReturnTrueWhenMoneyIsZero
shouldReturnFalseWhenMoneyIsNotZero
shouldCompareMoneyValues
shouldDetectGreaterThan
shouldDetectLessThan
shouldBeEqualWhenAmountsAreEqual
shouldNotBeEqualWhenAmountsAreDifferent
shouldHaveSameHashCodeWhenAmountsAreEqual
shouldReturnReadableString
```

---

# 5. Thiết kế class `AddressSnapshot`

## 5.1. Class này dùng để làm gì?

`AddressSnapshot` lưu địa chỉ giao hàng **tại thời điểm khách checkout**. Đây là bản chụp lịch sử, không phải tham chiếu trực tiếp đến địa chỉ hiện tại của Customer.

Ví dụ:

```text
Ngày 1: Customer đặt hàng đến 12 Nguyen Trai.
Ngày 2: Customer sửa địa chỉ mặc định thành 99 Le Loi.
```

Đơn hàng ngày 1 vẫn phải giao đến 12 Nguyen Trai.

MVP chưa tính phí theo GPS nên class này **không có latitude và longitude**.

## 5.2. Thuộc tính

```java
private final String recipientName;
private final String phone;
private final String addressLine;
private final String ward;
private final String district;
private final String province;
```

## 5.3. Constructor

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

Tất cả 6 trường đều bắt buộc. Nếu trường nào `null` hoặc blank sau khi trim thì ném `IllegalArgumentException`.

Nên lưu giá trị đã `trim()` để tránh dữ liệu như:

```text
"  Nguyen Van A  "
```

bị lưu kèm khoảng trắng không cần thiết.

## 5.4. API

```java
public String getRecipientName();
public String getPhone();
public String getAddressLine();
public String getWard();
public String getDistrict();
public String getProvince();
```

Class phải immutable, không có setter và nên override `equals`, `hashCode`, `toString` dựa trên cả 6 trường.

## 5.5. Test cần có

Tạo file:

```text
src/test/java/com/freshflow/api/domain/AddressSnapshotTest.java
```

Các test tối thiểu:

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

---

# 6. Thiết kế class `OrderItemSnapshot`

## 6.1. Class này dùng để làm gì?

`OrderItemSnapshot` lưu một ProductVariant đúng như nó tồn tại tại thời điểm checkout. Nó bảo vệ lịch sử đơn hàng khỏi các thay đổi sau này.

Nếu Merchant đổi tên món hoặc đổi giá, đơn cũ vẫn giữ tên và giá cũ.

Customer mua `ProductVariant`, không mua Product chung chung. Với món không có size, tạo variant mặc định:

```text
variantName = STANDARD
```

## 6.2. Thuộc tính

```java
private final Long productVariantId;
private final String productName;
private final String variantName;
private final Money unitPrice;
private final int quantity;
private final Money lineTotal;
```

`lineTotal` phải do constructor tự tính, không nhận từ bên ngoài. Nếu caller tự truyền lineTotal, dữ liệu có thể mâu thuẫn:

```text
unitPrice = 30000.00
quantity  = 2
lineTotal = 50000.00  // sai
```

## 6.3. Constructor

```java
public OrderItemSnapshot(
    Long productVariantId,
    String productName,
    String variantName,
    Money unitPrice,
    int quantity
)
```

| Trường | Quy tắc |
|---|---|
| `productVariantId` | Không null và lớn hơn 0 |
| `productName` | Không null hoặc blank |
| `variantName` | Không null hoặc blank; món không có size dùng `STANDARD` |
| `unitPrice` | Không null |
| `quantity` | Lớn hơn 0 |
| `lineTotal` | Tự tính bằng `unitPrice.multiply(BigDecimal.valueOf(quantity))` |

Constructor nên trim hai trường text và chỉ tính `lineTotal` sau khi validation đầu vào hoàn tất.

## 6.4. API

```java
public Long getProductVariantId();
public String getProductName();
public String getVariantName();
public Money getUnitPrice();
public int getQuantity();
public Money getLineTotal();
```

Class phải immutable, không có setter và nên override `equals`, `hashCode`, `toString`.

## 6.5. Ví dụ

Món có size:

```text
productVariantId = 101
productName      = Burger
variantName      = LARGE
unitPrice        = 65000.00
quantity         = 2
lineTotal        = 130000.00
```

Món không có size:

```text
productVariantId = 102
productName      = Banh mi
variantName      = STANDARD
unitPrice        = 25000.00
quantity         = 1
lineTotal        = 25000.00
```

## 6.6. Test cần có

Tạo file:

```text
src/test/java/com/freshflow/api/domain/OrderItemSnapshotTest.java
```

Các test tối thiểu:

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

Test quan trọng nhất:

```text
unitPrice = 30000.00
quantity  = 2
lineTotal = 60000.00
```

---

# 7. Thiết kế class `OrderPricingCalculator`

## 7.1. Class này dùng để làm gì?

`OrderPricingCalculator` tính tổng tiền checkout từ các `OrderItemSnapshot`, discount và phí giao hàng.

Class này **không** truy cập database, Payment Gateway, Inventory hay Product service. Nó chỉ tính toán domain thuần túy nên rất dễ viết unit test.

Công thức:

```text
subtotal       = tổng lineTotal của tất cả item
discountAmount = discount cố định hoặc discount phần trăm
grandTotal     = max(0, subtotal - discountAmount + deliveryFee)
```

## 7.2. Enum loại discount

Để không tạo thêm file trong task này, định nghĩa enum bên trong `OrderPricingCalculator`:

```java
public enum DiscountType {
    NONE,
    FIXED_AMOUNT,
    PERCENTAGE
}
```

| Loại | Ý nghĩa của `discountValue` |
|---|---|
| `NONE` | Phải bằng 0, không giảm giá |
| `FIXED_AMOUNT` | Số tiền VND cố định, ví dụ `20000.00` |
| `PERCENTAGE` | Phần trăm từ 0 đến 100, ví dụ `10` nghĩa là 10% |

## 7.3. `PricingResult`

Định nghĩa một class kết quả immutable bên trong `OrderPricingCalculator`:

```java
public static final class PricingResult {
    private final Money subtotal;
    private final Money discountAmount;
    private final Money deliveryFee;
    private final Money grandTotal;
}
```

Các getter bắt buộc:

```java
public Money getSubtotal();
public Money getDiscountAmount();
public Money getDeliveryFee();
public Money getGrandTotal();
```

`PricingResult` nên override `equals`, `hashCode`, `toString`.

## 7.4. Method calculate

```java
public PricingResult calculate(
    List<OrderItemSnapshot> items,
    DiscountType discountType,
    BigDecimal discountValue,
    Money deliveryFee
)
```

Validation:

| Input | Quy tắc |
|---|---|
| `items` | Không null, không rỗng |
| Item trong list | Không item nào được null |
| `discountType` | Không null |
| `discountValue` | Không null, không âm |
| `deliveryFee` | Không null |
| Discount phần trăm | Từ 0 đến 100 |
| `NONE` | `discountValue` phải bằng 0 |

## 7.5. Thứ tự tính toán

```text
1. Validate tất cả input.
2. Khởi tạo subtotal = Money(0.00).
3. Cộng lineTotal của từng item vào subtotal.
4. Tính discount theo loại.
5. Nếu discount > subtotal, giới hạn discount = subtotal.
6. Tính subtotal - discount.
7. Cộng deliveryFee.
8. Trả về PricingResult.
```

Với discount cố định:

```text
discountAmount = Money(discountValue)
```

Với discount phần trăm:

```text
percentageFactor = discountValue / 100
rawDiscount = subtotal.multiply(percentageFactor)
```

`Money.multiply` sẽ làm tròn kết quả về scale 2 bằng `HALF_UP`.

Ví dụ discount lớn hơn subtotal:

```text
subtotal = 100.00
discount  = 150.00
applied discount = 100.00
```

Sau đó:

```text
grandTotal = 100.00 - 100.00 + deliveryFee
```

## 7.6. Ví dụ hoàn chỉnh

| Dữ liệu | Giá trị |
|---|---:|
| Món 1: `30000 × 2` | `60000.00` |
| Món 2: `25000 × 1` | `25000.00` |
| Subtotal | `85000.00` |
| Discount 10% | `8500.00` |
| Delivery fee | `15000.00` |
| Grand total | `91500.00` |

## 7.7. Test cần có

Tạo file:

```text
src/test/java/com/freshflow/api/domain/OrderPricingCalculatorTest.java
```

Các test tối thiểu:

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

Kịch bản chính:

```text
item 1: unitPrice 30000.00, quantity 2, lineTotal 60000.00
item 2: unitPrice 25000.00, quantity 1, lineTotal 25000.00
subtotal: 85000.00
discount: 10% = 8500.00
delivery: 15000.00
grandTotal: 91500.00
```

## 7.8. Giỏ hàng rỗng

Giỏ hàng rỗng phải bị từ chối vì không thể checkout đơn hàng không có item:

```text
items == null → IllegalArgumentException
items.isEmpty() → IllegalArgumentException
```

Điều này khác với subtotal nội bộ:

```text
giỏ hàng rỗng → reject
cart hợp lệ → khởi tạo subtotal 0.00 rồi cộng các item
```

---

# 8. Cấu trúc file cần có sau khi code

```text
src/main/java/com/freshflow/api/domain/
├── Money.java
├── AddressSnapshot.java
├── OrderItemSnapshot.java
└── OrderPricingCalculator.java
```

```text
src/test/java/com/freshflow/api/domain/
├── MoneyTest.java
├── AddressSnapshotTest.java
├── OrderItemSnapshotTest.java
└── OrderPricingCalculatorTest.java
```

Không tạo trong task này:

```text
JPA Entity
Repository
Controller
REST endpoint
Flyway migration
Payment service
Inventory service
```

Những phần đó thuộc các task sau. FF-01-02-2 chỉ tập trung vào domain calculation foundation.

---

# 9. Checklist trước khi gửi review

| Nội dung kiểm tra | Kết quả cần đạt |
|---|---|
| Tiền dùng `BigDecimal` | Có |
| Không có phép tính tiền bằng `double` | Có |
| Money immutable | Có `final`, không setter |
| Tiền âm bị từ chối | Có |
| Money được chuẩn hóa scale 2 | Có |
| Input sai không bị âm thầm làm tròn | Có |
| Add và subtract trả về object mới | Có |
| Multiply dùng `HALF_UP` | Có |
| AddressSnapshot không có GPS | Có |
| OrderItemSnapshot dùng ProductVariant ID | Có |
| lineTotal được tự tính | Có |
| Giỏ hàng rỗng bị từ chối | Có |
| Hỗ trợ discount cố định và phần trăm | Có |
| Discount không làm tổng tiền âm | Có |
| Delivery fee được cộng vào grand total | Có |
| Các quy tắc quan trọng có unit test | Có |

---

# 10. Vì sao làm task này trước các task API/database?

Thứ tự triển khai hợp lý là:

```text
Domain rules
    ↓
Database model
    ↓
Application service
    ↓
REST API
    ↓
Web/Mobile clients
```

Nếu viết API trước khi chốt domain, endpoint checkout sẽ phải tự xử lý quá nhiều quy tắc. Nếu thiết kế database trước mà chưa biết chính xác dữ liệu cần lưu, schema dễ bị sửa nhiều lần.

Sau task này, các task tiếp theo sẽ có nền tảng rõ ràng:

| Task sau | Sử dụng kết quả từ FF-01-02-2 |
|---|---|
| Database Foundation | Xác định kiểu tiền, quantity và snapshot fields |
| Product/Variant | Dùng giá và variant để tạo OrderItemSnapshot |
| Cart/Checkout | Dùng OrderPricingCalculator để tính preview và final total |
| Order module | Lưu snapshot vào order items |
| Payment | Dùng grandTotal làm amount cần thanh toán |
| React Merchant | Hiển thị subtotal, discount, delivery fee, grand total |
| Android Customer | Hiển thị pricing và địa chỉ snapshot |
| Android Driver | Đọc địa chỉ giao hàng cố định của order |

---

# 11. Nhiệm vụ của bạn sau khi đọc tài liệu

Bạn hãy code toàn bộ bốn class và bốn file test theo tài liệu này, sau đó chạy toàn bộ test trong IntelliJ.

Khi hoàn thành, gửi:

```text
Money.java
MoneyTest.java
AddressSnapshot.java
AddressSnapshotTest.java
OrderItemSnapshot.java
OrderItemSnapshotTest.java
OrderPricingCalculator.java
OrderPricingCalculatorTest.java
```

Tôi sẽ review một lượt toàn bộ code, kiểm tra:

```text
Thiết kế class.
Tên method và biến.
Validation.
Tính immutable.
Công thức tính tiền.
Các edge case.
Chất lượng unit test.
Khả năng mở rộng cho API và database sau này.
```

## Kết luận

FF-01-02-2 không phải là việc tạo vài class cho đủ task. Đây là bước bạn chuyển các quyết định nghiệp vụ của FreshFlow thành code có thể kiểm thử được. Sau khi hoàn thành, bạn sẽ có nền tảng để xây checkout, payment, order, Merchant Web, Customer App và Driver App mà không phải viết lại quy tắc tính tiền ở nhiều nơi.
