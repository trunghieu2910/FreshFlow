# FF-07-01-1 — Kế hoạch identity, Store RBAC và Driver profile

Ngày khảo sát: 2026-10-09. Trạng thái: đã triển khai; bằng chứng kiểm chứng ở mục 6. Task này xây nền dữ liệu và access policy; login/JWT/refresh token và thay thế mock identity sẽ thuộc bước auth tiếp theo.

## 1. Hiện trạng đã đối chiếu

| Thành phần | Đã có | Khoảng trống |
| --- | --- | --- |
| Flyway V1 | `users`, `roles`, `user_store_roles`, `stores`, `driver_profiles`, availability history | Không cần tạo lại bảng hoặc thêm lại `is_available`/status |
| Role assignment | Unique `(user_id, store_id, role_id)`, FK và assignment status | Chưa có entity/repository, chưa seed role; Customer không thuộc Store nhưng `store_id` đang NOT NULL |
| Ownership | Store owner FK + unique một Store/owner; `CatalogAccessService` kiểm tra owner | Chưa kiểm tra user status, role và assignment ACTIVE |
| Driver read | `OrderReadService` giới hạn assignment mới nhất và Store khớp | Chưa kiểm tra user ACTIVE, Driver profile ACTIVE, role assignment ACTIVE |
| Credential | Cột `password_hash` | V2/V6 dùng placeholder, chưa có PasswordEncoder; không phải credential đăng nhập hợp lệ |
| Response | Catalog list trả Store entity qua `listStores()` | Có đường serialize `ownerUser.passwordHash`; cần DTO và test không lộ credential |
| Spring Security | OpenAPI có mô tả JWT | POM chưa có Security/JWT; header `X-User-Id` đang là mock identity |

Nguồn chính: V1/V2/V6 migrations; `CatalogAccessService`, `CatalogService`, `CatalogController`, `User`, `Store`, `OrderReadService`; ERD, relational model, DB-04-A và auth API contract.

## 2. Quyết định thiết kế đề xuất

1. Giữ `users` chỉ chứa identity/credential/status. Role luôn lấy qua `roles` và `user_store_roles`, không thêm `users.role`.
2. CUSTOMER là role toàn cục, `store_id = NULL`; MERCHANT/DRIVER cần Store cụ thể. Đây là thay đổi có chủ đích so với ERD hiện tại để phù hợp auth contract Customer không thuộc Store.
3. Giữ unique triplet cho assignment có Store; bổ sung unique partial index `(user_id, role_id) WHERE store_id IS NULL` để chặn duplicate role toàn cục. Một user được nhiều role trong cùng Store; không thay bằng unique `(user_id, store_id)`.
4. Rule scope theo role code được kiểm tra trong service ghi assignment. Không dùng CHECK truy vấn bảng `roles`; FK/unique xử lý structural integrity, service xử lý rule liên bảng. Không mở API gán role tùy ý cho client.
5. MERCHANT cần user ACTIVE + role assignment MERCHANT ACTIVE tại Store + đúng `owner_user_id`. Role MERCHANT ở Store A không cấp quyền ở Store B; role đơn lẻ cũng không biến user thành owner.
6. DRIVER cần user ACTIVE + profile ACTIVE + role assignment DRIVER ACTIVE tại profile Store + assignment của đúng order/Store. `is_available=false` ngăn nhận đơn mới, không tự cấm đọc đơn đã được gán. Account `LOCKED` hoặc profile `SUSPENDED`/`INACTIVE` phải bị từ chối dù vẫn có assignment.
7. Giữ policy lịch sử đang dùng: chỉ Driver của assignment mới nhất xem order detail/history; sau reassignment Driver cũ mất quyền. Mutation giao hàng tương lai cần assignment còn hoạt động, không chỉ điều kiện đọc lịch sử.
8. PasswordEncoder lưu `{bcrypt}...` qua DelegatingPasswordEncoder; không dùng NoOp/plaintext fallback. Placeholder cũ không được coi là password hợp lệ.

## 3. Các bước triển khai theo thứ tự

### Bước 1 — Preflight và chốt schema

- Rà database hiện có: email trùng sau lowercase/trim, role assignments hiện có, owner/profile lệch Store; chỉ đọc số lượng/ID, không in password hash.
- Tạo `docs/database/07-identity-schema.md` (DB-07-A) ghi dictionary, phạm vi global/Store, unique constraints, trạng thái account/profile và least privilege.
- Giữ nguyên ID users/stores để bảo toàn FK từ catalog/order/payment/audit. Không sửa migrations đã áp dụng V1–V8.

### Bước 2 — Migration V9 và role backfill

- Tạo `V9__align_identity_role_scope.sql` theo version còn trống lúc triển khai.
- Cho phép `user_store_roles.store_id` nullable và thêm global-role unique index; giữ constraint hiện hữu cho Store-scoped assignments.
- Chuẩn hóa email lowercase/trim sau kiểm tra collision; collision thì dừng rõ ràng, không tự merge user. Bổ sung constraint email canonical để code và DB thống nhất.
- Seed danh mục CUSTOMER/MERCHANT/DRIVER bằng natural key `roles.code`; không hard-code numeric role/Store IDs.
- Backfill MERCHANT theo owner đã tồn tại, CUSTOMER theo các identity khách hàng đã xác định; Driver role theo profile cùng Store nếu profile hiện có. Không grant role rộng cho toàn bộ users.
- `is_available` và profile status đã có: chỉ thêm constraint/index nếu thực sự thiếu và có truy vấn sử dụng; không tạo migration ADD COLUMN trùng.
- Kiểm tra migration trên database trống và bản sao dữ liệu V8; startup `ddl-auto=validate` phải qua, số Order/Item và FK không đổi.

### Bước 3 — Entity, repository và public identity contract

- Chuyển `User`/`UserRepository` từ catalog sang `identity/model` và `identity/repository`; cập nhật imports của Store, Order, catalog và tests. Chỉ có một entity map bảng users.
- Thêm `Role`, `UserStoreRole`, enum trạng thái và repository scoped query trong identity.
- Thêm `DriverProfile`/repository trong delivery; map `isAvailable`, status và affiliation Store hiện có.
- Tạo public `IdentityAccessService`/contract trả actor ID và grants theo Store. Catalog/order gọi contract thay vì tự suy luận role từ profile hoặc owner.
- Role grant, Store creation + owner grant và Driver creation + DRIVER grant phải ghi trong một transaction; trùng assignment bị DB chặn. Reactivation cập nhật row cũ thay vì chèn bản trùng.

### Bước 4 — Credential và dev seed

- Thêm dependency `spring-security-crypto` theo BOM Spring Boot và PasswordEncoder bean. Chưa bật SecurityFilterChain chỉ để có encoder, tránh thay toàn bộ HTTP behavior ngoài phạm vi task.
- Dev seed chạy theo profile rõ ràng: Merchant của hai Store hiện có, Customer và Driver của từng Store; lấy IDs từ email/owner relationship.
- Giữ email/ID demo V6 đang được tests sử dụng. Đồng bộ auth demo matrix đang mô tả email khác với seed thực tế.
- Với demo user đã có placeholder: chỉ thay placeholder đã nhận diện, không reset credential hợp lệ hoặc unlock user thật. Password nhận từ cấu hình local, encode trước persist; không log/commit plaintext hay nhúng credential demo vào JWT.
- Seed chạy lại không nhân đôi role/profile/Store và không bật `is_available` ngoài ý muốn. Role catalog thuộc migration; demo credentials thuộc dev seed, không tự cấp tài khoản demo trong production.
- Bảo vệ `passwordHash` khỏi JSON/toString; `listStores()` trả Store DTO thay entity, không serialize owner credential.

### Bước 5 — Gắn access policy vào API hiện có

- `CatalogAccessService`: kiểm tra user ACTIVE, active MERCHANT grant đúng Store và ownership.
- `OrderReadService`: thêm active user/profile/DRIVER grant vào cả list/count và detail scope. Không để detail chặn nhưng history còn lộ dữ liệu.
- Checkout/customer reads: kiểm tra active global CUSTOMER grant và ownership; Merchant/Driver có thêm CUSTOMER grant được đặt đơn với vai trò Customer.
- Account/profile không được phép thao tác trả access-denied `403`; order ngoài phạm vi tiếp tục `404` để giữ contract hiện có. Identity exception có handler riêng; duplicate grant nếu qua service trả conflict có ý nghĩa.
- `X-User-Id` vẫn là adapter dev trong giai đoạn này. Kết quả test chứng minh authorization theo actor được cung cấp; chưa chứng minh chống giả mạo identity. Bước JWT sau phải lấy actor từ principal và bỏ khả năng client tự chọn actor.

### Bước 6 — Kiểm thử nghiệm thu

| Nhóm | Trường hợp bắt buộc |
| --- | --- |
| PostgreSQL uniqueness | Duplicate user/Store/role bị chặn; nhiều role khác nhau được phép; cùng role ở Store khác được phép khi rule cho phép; duplicate CUSTOMER global bị chặn |
| Migration/seed | Fresh database, upgrade V8, seed chạy lại, không mất FK/history, không tự reset hash hợp lệ |
| Scope | Owner A đọc/sửa Store B bị từ chối; MERCHANT role không đúng owner bị từ chối; inactive role grant không cấp quyền |
| Driver | Đúng latest assignment + đúng Store được đọc; chưa assigned, wrong Store, reassigned Driver bị chặn ở list và detail |
| Status | users.LOCKED, profile.SUSPENDED/INACTIVE và inactive DRIVER grant đều bị chặn; unavailable nhưng ACTIVE vẫn đọc assignment của mình |
| Credential | Encoder.matches đúng password; sai password bị từ chối; stored value không bằng plaintext; response Store/User không có password/hash |
| Regression | Catalog ownership, checkout, order projection tests tiếp tục qua; fixtures tạo đủ role grants theo policy mới |

Repository/constraints dùng PostgreSQL thực, không thay bằng H2 cho test nullable uniqueness. Service policy tests và MockMvc tests kiểm tra denial/projection. Chạy tests liên quan trước, rồi suite backend và formatting/diff checks; ghi riêng lỗi baseline nếu có.

### Bước 7 — Đồng bộ tài liệu và bàn giao cho JWT

- Cập nhật ERD và relational model cho nullable global scope; sửa dictionary dùng `driver_availability_history` đúng tên schema.
- Cập nhật DB-04-A, auth demo account matrix, order read policy và package boundary.
- Ghi rõ token roles/store context chỉ phục vụ chọn ngữ cảnh; backend vẫn kiểm tra resource ownership và trạng thái hiện tại. Không flatten grants ở nhiều Store thành quyền chung.
- Login/JWT issuance, refresh-token storage, logout/revocation và client integration làm ở task tiếp theo với nền identity này.

## 4. Ước lượng và Definition of Done

| Công việc | Ước lượng |
| --- | ---: |
| Preflight, scope decision, schema/backfill | 1–1,5 giờ |
| Entity/repository và access contract | 1–1,5 giờ |
| Credential seed, DTO và policy integration | 1–1,5 giờ |
| Migration/authorization tests và docs | 1,5–2 giờ |
| Tổng cho scope nghiệm thu đầy đủ | 4,5–6,5 giờ |

Ước lượng backlog 2,5 giờ phù hợp phần migration/seed hẹp hơn; nếu giữ cả locked Driver/cross-store tests và tích hợp vào APIs hiện có thì nên dùng estimate trên. Task hoàn tất khi migration nâng cấp được, demo seed không tạo duplicate, không lộ credential, cả role scope và status đều được kiểm tra, và tests nghiệm thu qua. Dòng deadline `1/10/1016` cần sửa metadata; dự kiến là `01/10/2026`, không coi đây là ngày đã được xác nhận.

## 5. Nguồn kỹ thuật

- [Spring Security Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html): encoder và định dạng `{id}encodedPassword`.
- [PostgreSQL 16 Constraints](https://www.postgresql.org/docs/16/ddl-constraints.html): nullable uniqueness, partial indexes và giới hạn CHECK liên bảng.

## 6. Kết quả triển khai và kiểm chứng — 2026-10-09

- Đã thực hiện các bước 1–7: V9, identity/delivery entities và repositories, scoped grant contract, credential encoder/dev seed, Store DTO, access guards và tài liệu DB-07-A.
- PostgreSQL 16.15: upgrade database local từ V8 lên V9 thành công; giữ nguyên 4 Order và 4 OrderItem. Database kiểm chứng trống chạy V1–V9 và JPA validation thành công, sau đó đã xóa database kiểm chứng.
- Smoke test ứng dụng thật trên database kiểm chứng với profile `dev`: health `UP`, 5 credential `{bcrypt}` và 2 Driver mặc định unavailable; ứng dụng đã dừng. Test seed chạy hai lần giữ nguyên hash hợp lệ và account LOCKED; profile/grant không nhân đôi. Profile mặc định không tạo bean demo seeder.
- 48/48 tests thuộc các nhóm nghiệm thu identity, rollback, checkout, order reads, catalog access/controller và merchant orders đã qua. Kiểm thử rollback gây lỗi tại bước cấp grant, xác nhận Store/Driver profile không còn row sau khi transaction thất bại. `spotless:check` và `git diff --check` qua.
- Lượt `mvn -q clean test` trước khi thêm hai test rollback: 203 tests, 199 qua; 1 failure và 3 errors thuộc baseline catalog. Không coi toàn bộ suite là xanh. Cụ thể: `ProductControllerIntegrationTest.listsProductsWithPaginationAndSorting` (totalElements); `CatalogQueryPerformanceIntegrationTest.catalogPagination_navigatesMultiplePagesConsistently` và `listProductsByStore_doesNotProduceNPlusOneQueries` (criteria null); `CatalogServiceTest.listProductsByStore_paginated_returnsMappedProductDtosWithCapacity` (Store không tìm thấy).
- Login/JWT và chống giả mạo `X-User-Id` chưa thuộc task này. Identity contract giữ `storeId` trên từng grant để task JWT không flatten quyền giữa các Store.

Data dictionary và hướng dẫn seed: [DB-07-A](../database/07-identity-schema.md). Deadline backlog `1/10/1016` chưa được sửa trong nguồn backlog vì chưa xác nhận metadata.
