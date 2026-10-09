# FF-06-05-1 — Order detail and history projection

The read API uses the existing order, payment, delivery, dispute and audit tables. No migration is needed. Item names and prices come exclusively from `order_items` snapshot columns; delivery address comes from the order snapshot added in V8. Legacy seeded orders can have a null address.

| Viewer | History | Detail scope |
| --- | --- | --- |
| Customer | `GET /api/v1/orders` | `GET /api/v1/orders/{orderId}` with `orders.customer_user_id = X-User-Id` |
| Merchant | `GET /api/v1/merchant/stores/{storeId}/orders/history` | Existing `GET /api/v1/merchant/stores/{storeId}/orders/{orderId}` after Store-owner verification and `orders.store_id = storeId` |
| Driver | `GET /api/v1/driver/orders` | `GET /api/v1/driver/orders/{orderId}` with the latest delivery assignment's Driver profile user and Store matching the Order |

History responses are paged newest-first and contain order/acceptance, latest payment, latest delivery and latest dispute status. Detail adds snapshot items and address, payment attempts, delivery attempts, disputes and order audit events. A missing or out-of-scope order returns 404; a Merchant who does not own the specified Store receives 403. The prior Merchant list and transition endpoints remain available.

Driver detail shows only the latest assignment and redacts dispute messages, merchant resolution and audit reasons. Reassignment removes the previous Driver's detail/history access. Customer and Merchant can see delivery attempt history. The API never includes `otp_hash`, OTP plaintext or payment provider references. `otp.required` indicates COD. `otp.verificationReady` is true only for the Customer while the Order is shipping, the latest payment is `CASH_COLLECTED`, and an unexpired, unused credential with remaining attempts exists. `otp.codeVisible` is always false because the database stores only a hash; a future OTP issuance/display channel must supply plaintext transiently under its own authorization policy.

`X-User-Id` is the existing development identity contract. Its production replacement must bind the actor to an authenticated principal; this read API does not introduce a new identity mechanism.


## FF-07-01-1 implementation update (2026-10-09)

Identity implementation and current scope rules are documented in [DB-07-A](../database/07-identity-schema.md). User/UserRepository now belong to identity. CUSTOMER grants have null Store scope; MERCHANT/DRIVER grants require a Store. Active account and scoped grant are checked on existing APIs; Drivers also need an ACTIVE profile and their latest assignment in the same Store. Availability controls new assignments, not access to assigned work. Store responses omit owner credentials. Demo emails now follow existing V6 identities and the profile-gated dev seed, not the earlier proposed login matrix. JWT/principal integration remains a subsequent task.
