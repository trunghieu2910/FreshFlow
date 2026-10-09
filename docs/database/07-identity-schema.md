# DB-07-A — Identity, credentials và Store-scoped RBAC

Task FF-07-01-1. V9 mở rộng schema V1, không tạo lại identity tables, không đổi primary keys hay Order/Item history. CUSTOMER là grant toàn cục; MERCHANT/DRIVER là grant theo Store.

| Bảng | Dữ liệu / ràng buộc | Owner |
| --- | --- | --- |
| users | email lowercase/trim unique; password_hash; full_name; phone; status ACTIVE/LOCKED/PENDING; UTC timestamps | identity |
| roles | unique uppercase code CUSTOMER/MERCHANT/DRIVER; name; created_at | identity |
| user_store_roles | user_id, nullable store_id, role_id, status ACTIVE/INACTIVE, timestamps; unique user/store/role; unique user/role WHERE store_id IS NULL | identity |
| stores | owner_user_id FK unique: một Store/owner trong MVP | catalog |
| driver_profiles | user_id FK unique; store_id FK; is_available default false; vehicle_type; status ACTIVE/SUSPENDED/INACTIVE; timestamps | delivery |
| driver_availability_history | Driver, trạng thái sẵn sàng, actor, changed_at, reason | delivery |

DB bảo vệ FK/unique/status và canonical email. `RoleGrantService` kiểm tra CUSTOMER có Store null, MERCHANT/DRIVER có Store cụ thể; Reactivation cập nhật row hiện có. Không expose client role assignment API. Cross-table rule được enforce trong application contract, không dùng CHECK đọc bảng khác.

`User`/`UserRepository` thuộc module identity. Catalog/order dùng `IdentityAccessService`: actor ACTIVE, grant ACTIVE đúng role/scope; Merchant còn phải là Store owner, Driver còn phải có profile ACTIVE và latest assignment cùng Store. Driver unavailable vẫn đọc assignment của mình, nhưng LOCKED/SUSPENDED/INACTIVE bị từ chối 403. Resource ngoài scope trả 404 như contract order hiện có. Driver reads kiểm tra predicate trạng thái/grant ngay trong SQL list/count/detail.

## Credential và seed

`CredentialConfig` dùng Spring DelegatingPasswordEncoder, default lưu `{bcrypt}...`. Không có plaintext fallback. `User.passwordHash` không serialize; endpoint Store list trả StoreDto chứa ownerUserId, không trả owner entity hay hash. V2/V6 placeholder cũ chưa phải credential hợp lệ.

Danh mục role và grants từ Store owners, Order customers, Driver profiles được backfill trong V9. Customer không được gắn vào một Store giả để có role. Email collision sau normalization khiến migration dừng, không tự merge identities.

Dev seed chỉ bật qua Spring profile `dev`. Cấu hình `FRESHFLOW_DEMO_PASSWORD` (Spring property `freshflow.demo.password`, tối thiểu 8 ký tự) trong môi trường local rồi chạy `mvn spring-boot:run -Dspring-boot.run.profiles=dev`. Không đặt giá trị secret trên command line hoặc commit vào repository.

Seed dùng email demo hiện có `customer.demo@freshflow.vn`, `merchant.tea@freshflow.vn`, `merchant.bakery@freshflow.vn`; thêm `driver.tea@freshflow.vn` và `driver.bakery@freshflow.vn` ở đúng Store. Lookup bằng email/ownership, không hard-code ID. Chỉ thay placeholder đã biết; không reset hash hợp lệ, không unlock account, không nhân đôi profile/grant, không bật Driver availability. Seed không chạy ở profile mặc định/production.

Store creation và MERCHANT grant ghi cùng transaction. `DriverProfileService.create` ghi profile và DRIVER grant cùng transaction. Không dùng cascade xóa identity/history. JWT/login/refresh token chưa thuộc task này: `X-User-Id` vẫn là development actor adapter; production phải lấy actor từ authenticated principal.

## Kiểm chứng

Tests identity kiểm tra duplicate scoped/global grants, reactivation, invalid scope, credential encode/matches, repeatable dev seed và response không lộ credential. Order projection tests kiểm tra account LOCKED, profile SUSPENDED/INACTIVE, inactive grant ở cả history/detail, cùng scope/assignment cases. Migration được kiểm tra trên PostgreSQL, không dùng H2 thay nullable uniqueness.

Ngày 2026-10-09: 48/48 tests nghiệm thu liên quan qua, gồm hai test rollback khi cấp role thất bại. PostgreSQL 16.15 đã kiểm chứng migration V8→V9 và database trống V1→V9, JPA validation, dev startup health `UP` với 5 BCrypt credentials và 2 Driver unavailable. Formatting và diff checks qua. Suite backend trước khi thêm test rollback có 199/203 tests qua, còn bốn lỗi baseline catalog; chi tiết tại [kết quả triển khai](../plans/ff-07-01-1-identity-rbac-plan.md#6-kết-quả-triển-khai-và-kiểm-chứng--2026-10-09).
