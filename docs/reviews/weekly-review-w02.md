# FreshFlow Weekly Review & Retrospective — Week 02 (Sprint 2)

- **Tuần:** W02 — Sprint 2: Catalog API, PostgreSQL và OpenAPI
- **Thời gian:** 2026-09-07 đến 2026-09-13
- **Nhiệm vụ:** `FF-02-07-1 — Ôn JPA/REST và viết retrospective`
- **Mục tiêu học:** Active recall, Phân tích thiết kế hệ thống và Technical writing

---

## Phần 1. Retrospective Sprint 2 (Catalog Vertical Slice)

### 1.1. Mục tiêu tuần đã đặt ra
Xây dựng hoàn chỉnh lát cắt dọc (Vertical Slice) của module **Catalog**:
- Thiết kế cơ sở dữ liệu quan hệ PostgreSQL với 5 Flyway migrations (`V1` đến `V5`).
- Xây dựng JPA Entities, Repositories, Application Services theo kiến trúc Modular Monolith.
- Hiện thực toàn bộ REST APIs cho Customer (duyệt, tìm kiếm, lọc, phân trang) và Merchant (CRUD sản phẩm, biến thể, kiểm tra quyền sở hữu).
- Tích hợp chuẩn hóa OpenAPI 3.0, Swagger UI và kiểm thử tự động với Postman Collection.
- Tối ưu hóa hiệu năng truy vấn database (chống N+1 query bằng Batch Fetching, đánh chỉ mục Functional B-Tree index).
- Đạt 100% test coverage trên 151 test cases tự động và đóng gói bản release `catalog-v0.1` (`v0.1.0`).

### 1.2. What Went Well (Những điểm làm tốt)
1. **Kiến trúc phân tầng sạch sẽ (Clean Modular Monolith):**
   - Ranh giới giữa các module (`catalog`, `order`, `common`) được giữ độc lập. Entity, DTO, Request, Command, Query và ReadModel được tách biệt rõ ràng.
2. **Quản lý Database chuyên nghiệp bằng Flyway:**
   - 5 file migration được tổ chức theo thứ tự logic, đảm bảo tính bất biến (immutable) và an toàn (idempotent với `ON CONFLICT DO NOTHING`).
   - Dữ liệu seed thực tế gồm 36 món F&B và 57 biến thể giúp kiểm thử giao diện và hiệu năng chân thực.
3. **Hiệu năng & Tối ưu hóa truy vấn:**
   - Loại bỏ triệt để vấn đề N+1 query bằng `@BatchSize(size = 50)` và `hibernate.default_batch_fetch_size=50`.
   - Tạo Functional Index `idx_products_store_active_lower_name` trên `LOWER(name)` giúp tìm kiếm sản phẩm tức thì mà không cần scan toàn bảng.
4. **Chất lượng kiểm thử cao (High Quality Testing):**
   - Đạt tổng cộng **151 test cases** (vượt xa chỉ tiêu ban đầu), bao phủ từ Unit test logic nghiệp vụ (`CatalogAccessServiceTest`, `CatalogVariantServiceTest`), Controller slice test (`MockMvc`), JPA integration test, cho tới Hibernate Query Performance test.

### 1.3. What Could Be Improved (Điểm cần cải thiện & Bài học)
1. **Tuân thủ ranh giới package ngay từ đầu:**
   - Trong quá trình phát triển các task đầu tuần, `CatalogAccessService` từng bị đặt nhầm vào package `application.exception` và `CatalogController` bị đặt ngoài thư mục `controller`. Mặc dù đã được refactor sạch sẽ ở task FF-02-06-2, bài học rút ra là cần kiểm tra cấu trúc package boundary kỹ càng trước khi commit mỗi tính năng.
2. **Cơ chế xác thực còn đơn giản:**
   - Hiện tại quyền Merchant đang được xác thực qua mock header `X-User-Id`. Đây là giải pháp tạm thời cho Sprint 2 và cần được thay thế bằng JWT Bearer Token hoàn chỉnh ở Sprint 3.
3. **Quản lý tồn kho động:**
   - Sức chứa hàng ngày (`dailyCapacityDefault`) đang phản ánh hạn mức tĩnh. Cần tích hợp trừ kho thời gian thực khi module Order & Checkout được triển khai.

### 1.4. Action Items cho Sprint 3 (Week 3)
1. **Bảo mật & Phân quyền:** Triển khai Spring Security, JWT authentication, mã hóa mật khẩu BCrypt và phân quyền RBAC (Customer, Merchant, Admin, Driver).
2. **Frontend Architecture:** Khởi động kiến trúc React Web cho Merchant quản trị cửa hàng.
3. **Chuẩn bị Cart & Order:** Thiết kế giỏ hàng và liên kết kiểm tra giá thời gian thực với Catalog.

---

## Phần 2. Trả lời 5 câu hỏi phỏng vấn kỹ thuật cốt lõi

### Câu 1 (Acceptance Criteria 1): Vì sao trong REST API chúng ta KHÔNG NÊN trả trực tiếp JPA Entity ra Controller mà bắt buộc phải qua DTO?

Việc để Controller trả về trực tiếp JPA Entity là một anti-pattern nghiêm trọng trong kiến trúc phần mềm doanh nghiệp, vì các lý do sau:

1. **Lỗ hổng bảo mật Over-fetching & Mass Assignment:**
   - **Lộ thông tin nhạy cảm:** Entity thường chứa các trường nội bộ của hệ thống như mật khẩu (`password_hash`), cờ xóa mềm (`deleted_at`), thông tin kiểm toán (`created_by`, `updated_at`, `version`). Trả trực tiếp Entity sẽ làm rò rỉ dữ liệu này ra Client.
   - **Nguy cơ tấn công Mass Assignment:** Khi dùng Entity làm request body nhận dữ liệu, kẻ tấn công có thể chèn thêm các trường nguy hiểm (ví dụ chèn `role: ADMIN` hoặc `id: 1` hoặc `is_active: true`) và Hibernate có thể tự động đồng bộ giá trị đó vào cơ sở dữ liệu (dirty checking).
2. **Lỗi `LazyInitializationException` ngoài Transaction:**
   - Các thuộc tính quan hệ lười (Lazy collection như `Product.variants`) chỉ có thể được nạp khi Session/Transaction của JPA còn mở.
   - Khi controller trả về Entity, luồng ra ngoài Service và Transaction đã đóng. Khi bộ chuyển đổi JSON (Jackson ObjectMapper) cố gắng đọc getter của trường lazy để serialize, Hibernate sẽ ném ngoại lệ `LazyInitializationException: could not initialize proxy - no Session`.
3. **Vòng lặp vô tận khi serialize JSON (Infinite Recursion):**
   - Trong quan hệ hai chiều (Bidirectional relationship, ví dụ `@ManyToOne Store` $\leftrightarrow$ `@OneToMany Product`), Jackson sẽ liên tục serialize `Store` $\rightarrow$ `Product` $\rightarrow$ `Store` $\rightarrow$ `Product`... dẫn đến tràn bộ nhớ (`StackOverflowError`).
4. **Phá vỡ tính độc lập giữa Tầng Lưu Trữ (Persistence) và Tầng Giao Diện (Presentation):**
   - DTO đóng vai trò là bản giao ước (API Contract) ổn định với Mobile/Web clients.
   - Nếu trả trực tiếp Entity, mỗi khi bảng trong database đổi tên cột, chuẩn hóa quan hệ hoặc tách bảng, API Contract của Client sẽ bị vỡ vụn ngay lập tức. DTO giúp Presentation model và Database model tiến hóa hoàn toàn độc lập.

---

### Câu 2 (Acceptance Criteria 2): Vì sao SERVER PHẢI tính toán lại tổng tiền (total, line item total) tại thời điểm Checkout/Order, tuyệt đối không được tin tưởng giá hay tổng tiền từ Client gửi lên?

Đây là quy tắc vàng bất di bất dịch về bảo mật và tính toàn vẹn kinh doanh trong thương mại điện tử:

1. **Nguyên lý Zero Trust đối với Client:**
   - Client (Web Browser, Android App, iOS App) là môi trường hoàn toàn nằm dưới quyền kiểm soát của người dùng hoặc kẻ tấn công.
   - Mọi dữ liệu gửi từ Client qua HTTP/REST API đều có thể bị xem trộm và chỉnh sửa tùy ý bằng công cụ can thiệp như DevTools, Postman, cURL, Fiddler hoặc BurpSuite. Nếu Server tin tưởng trường `"totalPrice": 1000` do client gửi lên, kẻ tấn công có thể mua đơn hàng 1,000,000 VND với giá chỉ 1,000 VND.
2. **Dữ liệu giá và điều kiện kinh doanh biến động theo thời gian thực:**
   - Trong lúc khách hàng lướt xem menu và thêm món vào giỏ hàng (có thể mất 5 - 15 phút), Merchant có thể đã:
     - Tăng giá sản phẩm hoặc cập nhật kích cỡ.
     - Hết hạn chương trình khuyến mãi / mã giảm giá (voucher).
     - Sản phẩm đã hết hạn mức suất bán trong ngày (`available = false` hoặc hết `dailyCapacity`).
   - Server bắt buộc phải truy vấn lại giá gốc từ cơ sở dữ liệu tại đúng thời điểm bắt đầu giao dịch thanh toán (Checkout Transaction) để áp dụng chính sách giá mới nhất.
3. **Tính toán thuế (VAT), phí vận chuyển và chiết khấu phức tạp:**
   - Các công thức tính phí giao hàng (theo khoảng cách, thời tiết, giờ cao điểm), quy tắc chiết khấu bậc thang và thuế là logic nghiệp vụ cốt lõi (Core Domain Logic) chỉ được phép chạy tập trung tại backend để đảm bảo tính nhất quán kế toán và pháp lý.
4. **Đóng băng lịch sử giao dịch bằng Snapshot (`OrderItemSnapshot`):**
   - Khi Server tính toán và tạo đơn hàng thành công, toàn bộ thông tin giá tại thời điểm đó được "đóng băng" vào snapshot (`unit_price`, `quantity`, `line_total`). Sau này, dù cửa hàng có tăng/giảm giá sản phẩm trên Catalog, giá trị hóa đơn trong quá khứ vẫn giữ nguyên vẹn giá trị kế toán.

---

### Câu 3: Vấn đề N+1 Query trong JPA là gì? Tại sao `JOIN FETCH` lại nguy hiểm khi phân trang, và FreshFlow đã xử lý bài toán này như thế nào?

1. **Bản chất N+1 Query:**
   - Xảy ra khi câu lệnh SQL đầu tiên lấy ra $N$ bản ghi cha (`SELECT * FROM products LIMIT 20`).
   - Khi duyệt qua từng đối tượng cha để lấy danh sách con (ví dụ `product.getVariants()`), nếu quan hệ là `LAZY`, Hibernate sẽ phát sinh thêm $N$ câu lệnh con riêng rẽ (`SELECT * FROM product_variants WHERE product_id = ?`).
   - Tổng số truy vấn: $1 + N = 21$ queries! Với lưu lượng lớn, cơ sở dữ liệu sẽ bị quá tải kết nối và độ trễ tăng vọt.
2. **Tại sao `JOIN FETCH` nguy hiểm với Phân trang (Pagination):**
   - Khi viết `SELECT p FROM Product p JOIN FETCH p.variants`, kết quả SQL trả về dạng tích Descartes (Cartesian product: 1 product có 3 variant sẽ sinh ra 3 dòng dữ liệu).
   - Database SQL không thể thực hiện phân trang cấp Product bằng `LIMIT / OFFSET` được nữa vì các dòng bị nhân bản.
   - Hậu quả: Hibernate phát ra cảnh báo nghiêm trọng:
     `WARN: HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!`
     Nghĩa là Hibernate buộc phải kéo **toàn bộ dữ liệu của bảng từ database về RAM của ứng dụng**, sau đó tự cắt trang trong bộ nhớ! Nếu bảng có 100,000 sản phẩm, ứng dụng sẽ bị tràn bộ nhớ (`OutOfMemoryError`) ngay lập tức.
3. **Giải pháp FreshFlow áp dụng: Batch Fetching:**
   - Đặt `@BatchSize(size = 50)` trên tập hợp `variants` trong entity `Product.java`.
   - Cấu hình toàn cục trong `application.properties`: `spring.jpa.properties.hibernate.default_batch_fetch_size=50`.
   - Cơ chế: Khi truy vấn 20 sản phẩm trên trang, Hibernate gom 20 `product_id` lại và chỉ chạy đúng **1 câu lệnh duy nhất**:
     `SELECT * FROM product_variants WHERE product_id IN (?, ?, ?, ...)`
   - Kết quả: Từ 21 truy vấn giảm xuống còn đúng 2 truy vấn SQL, phân trang bằng `LIMIT / OFFSET` diễn ra an toàn 100% tại database server.

---

### Câu 4: So sánh Phân trang Offset-based và Keyset-based (Cursor-based)? Khi nào cần Functional Index trong PostgreSQL?

1. **So sánh hai cơ chế phân trang:**

| Tiêu chí | Offset-based Pagination | Keyset / Cursor-based Pagination |
|---|---|---|
| **Cú pháp SQL** | `LIMIT 20 OFFSET 1000` | `WHERE id > 1000 ORDER BY id ASC LIMIT 20` |
| **Độ phức tạp** | $O(N)$ — Database phải duyệt qua và loại bỏ $N$ bản ghi trước đó | $O(1)$ — Nhảy trực tiếp đến vị trí con trỏ thông qua chỉ mục B-tree |
| **Hiệu năng khi trang sâu** | Giảm dần khi trang càng sâu (Offset càng lớn càng chậm) | Ổn định tuyệt đối ở mọi độ sâu dữ liệu |
| **Vấn đề trôi dữ liệu** | Dễ bị trùng hoặc sót bản ghi nếu có dữ liệu mới chèn vào giữa lúc duyệt | Không bị ảnh hưởng bởi dữ liệu mới chèn |
| **Khả năng nhảy trang** | Nhảy tùy ý đến trang bất kỳ (Trang 1 $\rightarrow$ Trang 50) | Chỉ hỗ trợ duyệt tuần tự (Next / Previous, Infinite Scroll) |
| **Áp dụng trong FreshFlow** | Dùng cho Merchant Catalog Dashboard (cần biết tổng số trang và nhảy trang) | Khuyên dùng cho Customer Mobile App feed (Infinite scroll) |

2. **Chỉ mục hàm (Functional Index) trong PostgreSQL:**
   - Thông thường, chỉ mục B-tree trên cột `name` chỉ có tác dụng khi tìm kiếm chính xác `WHERE name = 'Cà phê'`.
   - Khi tìm kiếm không phân biệt hoa/thường bằng hàm `LOWER(name) LIKE lower(?)`, PostgreSQL không thể dùng chỉ mục thông thường trên cột `name` mà phải quét toàn bộ bảng (Sequential Scan).
   - FreshFlow đã tối ưu bằng **Functional Index**:
     ```sql
     CREATE INDEX idx_products_store_active_lower_name
     ON products (store_id, is_active, LOWER(name));
     ```
   - Chỉ mục này lưu sẵn giá trị chữ thường đã được tính toán trong cây B-Tree. Nhờ đó câu lệnh tìm kiếm tên món ăn có thể đạt tốc độ truy vấn tính bằng mili-giây.

---

### Câu 5: Các quy tắc vàng khi viết Flyway Migration trong Production? Kiến trúc Validation 2 lớp (Bean Validation + Domain Rule) hoạt động ra sao?

1. **Quy tắc vàng khi viết Flyway Migration trong môi trường Production:**
   - **Tính bất biến (Immutability):** Tuyệt đối không bao giờ sửa đổi file migration đã được thực thi và lưu checksum trong bảng `flyway_schema_history`. Mọi thay đổi đều phải tạo một version mới (Forward-only).
   - **Tương thích ngược (Expand / Contract Pattern):**
     - Khi đổi tên cột hoặc cấu trúc bảng, không được `DROP` hoặc `RENAME` ngay lập tức (vì code cũ đang chạy sẽ crash).
     - Bước 1 (Expand): Thêm cột mới, ghi dữ liệu song song.
     - Bước 2: Deploy mã nguồn mới đọc từ cột mới.
     - Bước 3 (Contract): Sau khi ổn định, tạo migration mới để xóa cột cũ.
   - **Đảm bảo tính Idempotent:** Luôn sử dụng các mệnh đề an toàn như `IF NOT EXISTS`, `IF EXISTS`, `ON CONFLICT DO NOTHING` để việc chạy lại migration không làm gãy pipeline CI/CD.
2. **Kiến trúc Validation 2 lớp (Two-Layer Validation):**
   - **Lớp 1: Cú pháp & Định dạng tại Controller (Bean Validation - Jakarta Constraints):**
     - Sử dụng `@Valid`, `@NotBlank`, `@Size`, `@PositiveOrZero` ngay tại DTO Request.
     - Lợi ích: Chặn đứng dữ liệu rác, giá âm, chuỗi rỗng ngay tại cửa ngõ API trước khi dữ liệu chạm vào tầng Service hoặc Database, trả về `400 Bad Request` với mã lỗi `VALIDATION_ERROR`.
   - **Lớp 2: Ràng buộc nghiệp vụ & Trạng thái tại Service (Domain / Business Rules):**
     - Thực hiện trong Service: Kiểm tra quyền sở hữu store (`CatalogAccessService`), kiểm tra trùng tên biến thể (`VARIANT_DUPLICATE`), kiểm tra tính hợp lệ của size (`STANDARD_SIZE_INVALID`).
     - Lợi ích: Đảm bảo các bất biến nghiệp vụ (Business Invariants) phức tạp phụ thuộc vào trạng thái cơ sở dữ liệu hiện tại, trả về `403 STORE_ACCESS_DENIED` hoặc `409 CONFLICT`.

---

## Phần 3. Bài kiểm tra Active Recall (Self-Quiz 10 câu)

Dưới đây là 10 câu hỏi tự đánh giá giúp bạn củng cố kiến thức tuần 2. Mỗi câu 10 điểm (Thang điểm 100). Tiêu chí đạt: $\ge 80/100$ điểm.

### Đề kiểm tra:

**Câu 1:** Trong Spring Boot REST API, nếu Jackson ObjectMapper cố gắng serialize một trường quan hệ `@OneToMany(fetch = FetchType.LAZY)` khi Transaction đã đóng, hiện tượng gì sẽ xảy ra?
- A) Thuộc tính đó sẽ tự động trả về `null`.
- B) Hệ thống tự động mở một transaction mới để đọc dữ liệu.
- C) Ném ngoại lệ `LazyInitializationException`.
- D) Cơ sở dữ liệu tự động bị khóa (deadlock).

**Câu 2:** Tại sao không nên cho phép Client truyền trực tiếp trường `totalPrice` trong request tạo đơn hàng (`POST /api/v1/orders`)?
- A) Vì làm tăng kích thước payload của gói tin HTTP.
- B) Vì Client có thể can thiệp sửa đổi tổng tiền thấp hơn giá trị thực (Zero Trust).
- C) Vì Spring Boot không hỗ trợ kiểu dữ liệu BigDecimal trong request body.
- D) Vì database PostgreSQL không cho phép lưu trường số thực.

**Câu 3:** Cơ chế Batch Fetching với `@BatchSize(size = 50)` giúp giải quyết bài toán gì?
- A) Giảm kích thước dung lượng lưu trữ trên ổ cứng.
- B) Gom nhiều truy vấn con lấy collection thành 1 câu lệnh sử dụng mệnh đề `WHERE ... IN (?, ?, ...)`.
- C) Tự động phân trang trên tất cả các table.
- D) Tự động backup database mỗi 50 phút.

**Câu 4:** Khi sử dụng `JOIN FETCH` trên một quan hệ `@OneToMany` kết hợp với `Pageable` trong Spring Data JPA, rủi ro lớn nhất là gì?
- A) Bị lỗi cú pháp SQL.
- B) Hibernate buộc phải kéo toàn bộ dữ liệu bảng về RAM để phân trang trong bộ nhớ, dễ gây tràn RAM (OOM).
- C) Không thể sắp xếp theo trường ID.
- D) Tự động xóa dữ liệu của bảng con.

**Câu 5:** Lợi ích bảo mật chính của việc sử dụng DTO thay vì JPA Entity ở tầng Controller là gì?
- A) Giúp code chạy nhanh gấp 10 lần.
- B) Ngăn chặn lộ thông tin nhạy cảm và ngăn chặn tấn công Mass Assignment.
- C) Tránh phải cấu hình database connection pool.
- D) Bắt buộc database phải mã hóa dữ liệu.

**Câu 6:** Một file Flyway migration có tên `V2__add_constraints.sql` đã chạy trên môi trường Production. Cách đúng đắn nhất để thêm một cột mới vào bảng là gì?
- A) Mở file `V2__add_constraints.sql` thêm câu lệnh `ALTER TABLE` vào cuối file.
- B) Xóa bảng `flyway_schema_history` rồi chạy lại `V2`.
- C) Tạo file migration mới `V3__add_new_column.sql` để thực thi thay đổi.
- D) Dùng tool kết nối database chạy lệnh tay mà không cần Flyway.

**Câu 7:** Functional Index trong PostgreSQL phát huy tác dụng tối đa trong trường hợp nào?
- A) Khi truy vấn tìm kiếm có sử dụng hàm biến đổi trên cột (ví dụ `WHERE LOWER(name) LIKE lower(?)`).
- B) Khi bảng có ít hơn 10 dòng dữ liệu.
- C) Khi bảng chỉ chứa khóa chính.
- D) Khi chỉ thực hiện các câu lệnh `DELETE`.

**Câu 8:** Trong kiến trúc Modular Monolith của FreshFlow, service `CatalogAccessService` đóng vai trò gì?
- A) Kết nối với cổng thanh toán VNPay.
- B) Xác thực quyền sở hữu của Merchant đối với Store và Product trước khi cho phép sửa/xóa.
- C) Gửi email thông báo cho khách hàng.
- D) Khởi động container Docker.

**Câu 9:** Ý nghĩa của `OrderItemSnapshot` trong thiết kế đơn hàng (Order Domain) là gì?
- A) Để chụp ảnh sản phẩm bằng camera điện thoại.
- B) Đóng băng thông tin tên, đơn giá và thành tiền tại thời điểm đặt hàng, bảo đảm tính bất biến lịch sử kể cả khi Catalog đổi giá.
- C) Tự động cập nhật giá mới nhất của Catalog vào đơn hàng cũ.
- D) Lưu trữ thông tin định vị GPS của tài xế.

**Câu 10:** Hai tầng validation (Two-layer Validation) trong FreshFlow phân chia trách nhiệm thế nào?
- A) Tầng 1: Validate HTML/CSS; Tầng 2: Validate JavaScript.
- B) Tầng 1: Bean Validation tại DTO chặn cú pháp rác; Tầng 2: Domain Rule tại Service kiểm tra quy tắc nghiệp vụ sâu.
- C) Tầng 1: Kiểm tra phần cứng máy chủ; Tầng 2: Kiểm tra mạng.
- D) Cả 2 tầng đều làm nhiệm vụ giống hệt nhau.

---

### Bảng Đáp Án & Hướng Dẫn Tự Chấm Điểm:

| Câu hỏi | Đáp án đúng | Giải thích vắn tắt |
|:---:|:---:|:---|
| **Câu 1** | **C** | Ngoài Transaction, Hibernate Session đã đóng nên việc nạp proxy lazy sẽ ném `LazyInitializationException`. |
| **Câu 2** | **B** | Nguyên tắc Zero Trust: Client không đáng tin cậy và có thể sửa đổi dữ liệu request HTTP. |
| **Câu 3** | **B** | `@BatchSize` gom các ID vào một truy vấn `WHERE id IN (...)` để triệt tiêu N+1 queries. |
| **Câu 4** | **B** | `JOIN FETCH` collection làm nhân bản dòng, khiến Hibernate phải tải hết về RAM để phân trang thủ công. |
| **Câu 5** | **B** | DTO tách biệt hợp đồng API, ẩn giấu dữ liệu nội bộ và ngăn chặn việc gán đè thuộc tính trái phép. |
| **Câu 6** | **C** | File migration đã chạy là bất biến; mọi thay đổi tiếp theo bắt buộc phải tạo file version mới (Forward-only). |
| **Câu 7** | **A** | Chỉ mục hàm đánh index trên kết quả của biểu thức hàm (như `LOWER(name)`), tăng tốc tìm kiếm tức thì. |
| **Câu 8** | **B** | Kiểm tra `store.getOwnerUser().getId() == actorUserId` để ngăn chặn lỗ hổng truy cập trái phép BOLA/IDOR. |
| **Câu 9** | **B** | Đơn hàng là bản ghi kế toán có giá trị pháp lý, giá quá khứ không được phép thay đổi theo menu hiện tại. |
| **Câu 10** | **B** | Tách biệt rành mạch: Jakarta annotations chặn dữ liệu vô lệ tại API gateway, Service xử lý business logic. |

**Thang đánh giá kết quả:**
- **90 - 100 điểm:** Xuất sắc (Nắm rất vững kiến thức JPA/REST chuyên sâu).
- **80 điểm:** Đạt yêu cầu nghiệm thu (Sẵn sàng cho Sprint 3).
- **< 80 điểm:** Cần đọc lại Phần 2 của tài liệu này để củng cố các khái niệm bị hổng.
