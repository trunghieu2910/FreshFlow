# FF-06-07-1 — W06 review: transaction, Room và checkout

**Ngày rà soát:** 2026-10-08  
**Phạm vi:** Code và tài liệu trong repository tại thời điểm rà soát. Đây là review kiến trúc, không thay đổi luồng nghiệp vụ.

## Ranh giới nhất quán hiện tại

Ranh giới ACID là **một PostgreSQL transaction** do `CheckoutService.checkout()` mở bằng `@Transactional`, không phải ranh giới package `order`, `catalog` hay toàn bộ HTTP request. `CatalogCheckoutReservationService.reserve()` yêu cầu transaction đang có (`MANDATORY`); `OrderPersistenceService.create()` mặc định tham gia transaction đó. JDBC và JPA cùng dùng datasource/transaction của Spring. Khi một bước ném exception trước commit, các thay đổi database trong checkout cùng rollback.

| Dữ liệu hoặc hành động | Nằm trong transaction checkout? | Kết luận |
| --- | --- | --- |
| Cart Customer trong Android Room | Không; Room còn ở mức thiết kế, thư mục mobile hiện chỉ có README | Cart là bản nháp có thể cũ. Room chỉ có atomicity trên thiết bị; backend phải đọc lại catalog, giá và capacity. Không có commit chung Room–PostgreSQL. |
| `idempotency_records` và khóa `(user_id, idempotency_key)` | Có | Claim key, kiểm tra hash và lưu response cùng commit với Order. Retry cùng key/body sau khi commit trả lại kết quả đã lưu; body khác trả `409`. |
| Kiểm tra Store/Product/Variant, giữ daily capacity hoặc limited stock | Có | Catalog rows được khóa khi kiểm tra; conditional `UPDATE` chỉ tăng `reserved_quantity` nếu còn đủ. Nhiều item được xử lý theo variant ID để giảm nguy cơ deadlock. |
| `orders`, `order_items`, snapshot địa chỉ/tên/giá, total server-side | Có | Order và reservation hoặc cùng commit hoặc cùng rollback. Giá/total từ client không được tin cậy. |
| Payment attempt `PENDING`, order audit và response idempotency | Có | Đây là bản ghi trạng thái nội bộ trong PostgreSQL, chưa phải tiền đã được thu hay một lệnh đã tới payment provider. |
| HTTP response gửi tới client | Không | Có thể commit thành công nhưng client timeout trước khi nhận `201`. Client phải giữ nguyên key và body để retry, sau đó đối chiếu order từ server. |
| Merchant acceptance, thanh toán tiếp theo, giao hàng, dispute | Không cùng transaction checkout | Đây là các bước nghiệp vụ sau checkout, mỗi bước cần transaction và idempotency riêng. Không được giả định rollback checkout khi bước sau thất bại. |
| RabbitMQ hoặc cổng thanh toán bên ngoài | Không tham gia checkout hiện tại | RabbitMQ mới là hạ tầng demo, không có producer/consumer nghiệp vụ trong API. Payment method hiện chỉ tạo attempt `PENDING`. |

`READ COMMITTED` là isolation mặc định của PostgreSQL đang dùng. Conditional update và database check constraint bảo vệ giới hạn trên một inventory row khi các checkout tranh chấp. Điều này không đồng nghĩa toàn bộ workflow đã có serializable isolation. Nếu transaction bị deadlock hoặc lỗi commit, caller cần dùng cùng idempotency key để xác định kết quả trước khi tạo request mới.

## Tình huống cần tự giải thích

1. **Item thứ hai sai Store:** item thứ nhất đã tăng reservation trong transaction; bước sau ném lỗi thì cả reservation đầu, Order, Payment, audit và idempotency record chưa commit đều rollback. Test `rollsBackFirstReservationWhenLaterItemBelongsToAnotherStore` kiểm tra phần reservation.
2. **Client mất mạng sau commit:** backend đã có Order nhưng client không thấy `201`. Gửi lại đúng key/body trả cùng order/response; không lấy tổng tiền từ Room để tự quyết định thành công.
3. **Hai Customer tranh capacity cuối:** SQL `UPDATE ... WHERE capacity_limit - reserved_quantity >= quantity` khiến chỉ request còn đủ capacity được tăng row. Cần bổ sung test đồng thời thực sự trên PostgreSQL; test hiện tại chưa chứng minh race hai checkout.
4. **Merchant reject hoặc payment fail sau checkout:** đây không phải rollback của transaction cũ. Phải có transaction mới chuyển trạng thái và release reservation. Code hiện chưa release khi reject/cancel/payment failure, nên reservation có thể bị giữ sai lâu dài.
5. **Tách Inventory/Payment thành service hoặc dùng payment provider thật:** PostgreSQL transaction của Order không thể rollback database độc lập hoặc một side effect bên ngoài. Cần xác định owner dữ liệu, commit Order cùng outbox event, phát event sau commit, dùng consumer idempotent, retry và bước bù trừ như release reservation/refund. Saga/outbox là hướng phối hợp bất đồng bộ khi thật sự tách; không coi hai-phase commit là điều đã có trong MVP.

## Giới hạn và việc cần hoàn tất

- **Room chưa triển khai.** `clients/freshflow-mobile` chỉ có README; mô hình offline cart và cách lưu/reuse idempotency key trên Android là kế hoạch, chưa có kiểm thử. Khi làm client, key cần giữ bền qua timeout/process restart cho cùng một lệnh checkout; đổi nội dung lệnh thì dùng key mới.
- **Reservation lifecycle chưa trọn vẹn.** Checkout đã reserve, nhưng reject/cancel/payment failure chưa release. Đây là khoảng trống nghiệp vụ ngay trong monolith, không phải vấn đề distributed transaction.
- **Chưa có test checkout đồng thời** và kịch bản response timeout thực trên client. `READ COMMITTED` cộng conditional update là cơ chế trong code; race/timeout cần bằng chứng kiểm thử riêng.
- **Idempotency expiry chưa vận hành.** `expires_at` được ghi 24 giờ nhưng đường đọc hiện không kiểm tra expiry và chưa có cleanup. Hiện key có hiệu lực thực tế tới khi record bị xóa.
- **Identity là mock header `X-User-Id`**, chưa ràng buộc với authenticated principal. Không dùng kết quả này để kết luận production authorization đã hoàn tất.
- **Ngày capacity tính theo UTC** (`LocalDate.now(ZoneOffset.UTC)`); phải chốt ngày nghiệp vụ của Store trước khi dùng cho vận hành thực tế ở múi giờ khác.

## Bản tự giải thích để ghi âm (khoảng 90 giây)

> FreshFlow hiện là một modular monolith dùng chung PostgreSQL. Khi checkout, server nhận Store, item, địa chỉ, payment method và idempotency key. Room của Android chỉ là nơi dự kiến lưu giỏ hàng offline; hiện app mobile chưa triển khai. Dữ liệu Room có thể cũ nên server kiểm tra lại Store, Product, Variant, giá và số lượng còn khả dụng.
>
> `CheckoutService` mở một transaction PostgreSQL. Trong transaction đó, server claim idempotency key, khóa catalog rows, reserve capacity hoặc stock bằng update có điều kiện, tạo Order và item snapshot, tính total từ giá server, rồi ghi payment attempt pending, audit và response để replay. Nếu item thứ hai lỗi, mọi write trước đó rollback. Nếu commit xong mà mạng làm mất response, client gửi lại đúng key và cùng body để nhận lại kết quả, không tạo đơn khác.
>
> Ranh giới này không bao gồm Room, HTTP response hay payment provider. Merchant acceptance và giao hàng diễn ra sau checkout trong các transaction khác. Hiện code chưa release reservation khi reject hoặc payment fail, chưa có test hai checkout tranh capacity cuối và chưa có client Room để kiểm tra retry sau timeout. Khi tách Inventory hoặc Payment sang service riêng, transaction PostgreSQL này không còn bao trùm các database và side effect bên ngoài; cần outbox, message idempotent, retry và bù trừ như release hoặc refund. Vì thế giới hạn nhất quán hiện tại là atomicity trong một PostgreSQL transaction, còn workflow nhiều bước cần thiết kế riêng.

**Minh chứng ghi âm:** Chưa có bản ghi giọng nói cá nhân trong repository. Đoạn trên là transcript chuẩn bị cho người học tự thu âm; không được ghi là đã hoàn tất bằng chứng audio khi chưa có file ghi âm thật.

## Nguồn đối chiếu

- `services/freshflow-api/src/main/java/com/freshflow/api/order/service/CheckoutService.java`
- `services/freshflow-api/src/main/java/com/freshflow/api/catalog/service/CatalogCheckoutReservationService.java`
- `services/freshflow-api/src/main/java/com/freshflow/api/order/service/OrderPersistenceService.java`
- `services/freshflow-api/src/test/java/com/freshflow/api/order/controller/CheckoutControllerIntegrationTest.java`
- `docs/database/06-checkout-transaction.md`, `docs/adr/ADR-001-modular-monolith.md`
- `docs/architecture/erd.md`, `docs/risk-register.md`, `clients/freshflow-mobile/README.md`
