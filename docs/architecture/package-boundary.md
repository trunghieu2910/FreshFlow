# FreshFlow Package Boundary

## 1. Mục đích

Tài liệu này quy định ranh giới package cho FreshFlow backend. FreshFlow được triển khai dưới dạng modular monolith: các module nghiệp vụ chạy trong cùng một Spring Boot application nhưng được tổ chức thành các package có trách nhiệm và quyền phụ thuộc rõ ràng.

Mục tiêu là tránh việc tất cả class nghiệp vụ nằm trong một package chung, đồng thời giữ khả năng tách module thành service riêng trong tương lai nếu quy mô hệ thống yêu cầu.

## 2. Root package

```text
com.freshflow.api
```

Application class nằm ở root package để Spring Boot có thể scan các package con:

```text
com.freshflow.api.FreshflowApiApplication
```

## 3. Module hiện tại

```text
com.freshflow.api
├── common
│   └── domain
│       └── Money
├── catalog
│   └── domain
├── order
│   └── domain
│       ├── AddressSnapshot
│       ├── OrderItemSnapshot
│       └── OrderPricingCalculator
├── payment
│   └── domain
└── delivery
    └── domain
```

Các package `catalog`, `payment` và `delivery` được tạo trước để ghi nhận boundary, dù domain class của các module này sẽ được bổ sung ở các task sau.

## 4. Trách nhiệm module

| Module | Trách nhiệm chính | Ví dụ dữ liệu hoặc use case |
|---|---|---|
| `common` | Thành phần dùng chung, không thuộc một nghiệp vụ riêng | `Money`, base result, shared error |
| `catalog` | Store, category, product, product variant và khả năng bán | Product availability, inventory mode |
| `order` | Cart, order, order item và order state machine | Tạo order, tính giá, hủy order |
| `payment` | Payment method, payment attempt và payment state | Online mock, COD, transfer on delivery |
| `delivery` | Driver, delivery assignment và giao hàng | Gán driver, shipping, completed |

## 5. Quy tắc phụ thuộc

### 5.1. Quy tắc bắt buộc

Một module chỉ được truy cập public API hoặc application contract của module khác. Không truy cập trực tiếp private implementation, repository hoặc database detail của module khác.

`common` không được phụ thuộc vào `catalog`, `order`, `payment` hoặc `delivery`.

Các module nghiệp vụ có thể phụ thuộc vào `common` khi cần shared value object hoặc shared technical type.

### 5.2. Sơ đồ phụ thuộc mức cao

```text
                 common
                /  |  \\
               /   |   \\
          catalog order payment
                    |
                 delivery
```

Sơ đồ trên biểu diễn khả năng dùng shared type ở mức khái niệm, không có nghĩa mọi module đều được phép gọi trực tiếp mọi module khác.

### 5.3. Quy tắc cho `Money`

`Money` thuộc `com.freshflow.api.common.domain` vì tiền xuất hiện trong catalog, order và payment. Các module chỉ sử dụng hành vi public của `Money`; không tạo thêm một class tiền riêng trong từng module.

### 5.4. Quy tắc cho order snapshot

`AddressSnapshot`, `OrderItemSnapshot` và `OrderPricingCalculator` thuộc `com.freshflow.api.order.domain` vì chúng mô tả dữ liệu và logic được đóng băng trong ngữ cảnh order tại thời điểm checkout.

Order không được phụ thuộc trực tiếp vào implementation nội bộ của catalog để đọc lại giá hoặc tên sản phẩm sau khi order đã được tạo. Order phải sử dụng snapshot đã lưu.

## 6. Cấu trúc package mục tiêu cho module đầy đủ

Khi module phát triển, package con được mở rộng theo cấu trúc:

```text
com.freshflow.api.<module>
├── api
├── application
├── domain
└── infrastructure
```

Ý nghĩa các package:

| Package | Trách nhiệm |
|---|---|
| `api` | REST controller, request/response DTO và public inbound adapter |
| `application` | Use case, command, query và transaction orchestration |
| `domain` | Entity, value object, domain service và business rule |
| `infrastructure` | JPA repository, external adapter và technical implementation |

Không tạo đầy đủ các package con khi chưa có code thực tế. Chỉ thêm package khi module có nhu cầu để tránh tạo cấu trúc rỗng không có ý nghĩa.

## 7. Quy tắc đặt class

Class phải nằm trong module sở hữu nghiệp vụ của nó. Không đặt class vào `common` chỉ vì nhiều module đang sử dụng nó; `common` chỉ dành cho khái niệm thực sự dùng chung và ổn định.

Tên package dùng `lowercase`. Tên class dùng `PascalCase`. Tên biến và method dùng `camelCase`.

## 8. Kiểm tra boundary trong code review

Mỗi pull request cần kiểm tra các câu hỏi sau:

1. Class mới thuộc module nghiệp vụ nào?
2. Class có đang được đặt nhầm vào `common` không?
3. Module mới có truy cập trực tiếp repository hoặc entity nội bộ của module khác không?
4. Có thể thay thế dependency bằng application contract hoặc domain event không?
5. Việc thay đổi có tạo circular dependency giữa các module không?

## 9. Phạm vi của tài liệu

Tài liệu này định nghĩa package boundary cho modular monolith. Nó chưa biến các package thành Maven module hoặc microservice độc lập. Việc tách deployment, database hoặc repository sẽ chỉ được thực hiện khi có yêu cầu kiến trúc riêng.
