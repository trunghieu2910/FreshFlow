# FreshFlow Risk Register

> **Task:** `FF-01-06-2`
> **Status:** Active for MVP planning
> **Owner:** FreshFlow project team
> **Last reviewed:** 2026-08-22

## 1. Purpose

This risk register records the principal delivery, technical and security risks that may affect the FreshFlow MVP. It is intentionally a planning and review document; it does not introduce a new application feature or change the approved MVP scope.

The register focuses on the four risks required by FF-01-06-2: broad scope, stock and capacity concurrency, token security, and network emulator conditions. Supporting risks are included when they directly affect those four risks or the ability to deliver a reproducible MVP.

## 2. Scoring method

Each risk receives a **Probability (P)** and **Impact (I)** score from 1 to 5. The score is calculated as `P × I`.

| Score | Rating | Required response |
|---:|---|---|
| 1–4 | Low | Monitor during the weekly review. |
| 5–9 | Medium | Assign a mitigation owner and review the trigger. |
| 10–16 | High | Treat the mitigation as part of the relevant task acceptance criteria. |
| 17–25 | Critical | Resolve or explicitly accept before dependent work proceeds. |

Probability and impact are planning judgements, not production measurements. They must be reassessed when a trigger appears or when the affected architecture or scope changes.

## 3. Active risk register

| ID | Risk and consequence | P | I | Score / rating | Mitigation | Trigger | Owner / review point |
|---|---|---:|---:|---:|---|---|---|
| R-01 | **MVP scope expansion.** The project attempts to deliver GPS, maps, production payment, full microservices, Electron or additional roles before the Customer, Merchant, Driver and Backend flows are stable. The result may be schedule overrun and incomplete core features. | 4 | 5 | 20 / Critical | Freeze the 12-week MVP boundary. Deliver Must items first. Record every new request as an issue or decision record with time and dependency impact. Move non-essential requests to Backlog V2. Keep no more than two planned tasks per day. | A request is outside the approved Customer, Merchant, Driver or Backend flow; a new infrastructure dependency is proposed; or a task threatens the 12-week boundary. | Product/technical lead; review every weekly planning session and at every scope change. |
| R-02 | **Stock or daily-capacity concurrency.** Two checkout requests can reserve the same limited stock or capacity, causing overselling, overbooking or incorrect release after cancellation. | 4 | 5 | 20 / Critical | Recheck availability on the server during checkout. Reserve stock/capacity inside a database transaction. Use row-level locking or a version check, enforce non-negative quantities, and release reservations on reject, cancel or payment failure. Add concurrent checkout integration tests against PostgreSQL. | Two concurrent checkouts exceed available quantity/capacity; `reserved_quantity` becomes invalid; or a failed order does not release its reservation. | Backend/order owner; review before checkout and inventory tasks are accepted. |
| R-03 | **JWT/token and ownership failure.** A leaked token, weak token handling or role-only authorization could expose another user's order or another Store's data. | 3 | 5 | 15 / High | Use BCrypt for passwords and short-lived access tokens. Never commit or log tokens, passwords or secret keys. Apply RBAC together with Store/resource ownership checks. Test unauthenticated, wrong-role, expired-token and cross-Store requests with 401/403 expectations. | A token or secret appears in logs, screenshots, PRs or repository files; a user can access another Store's resource; or token expiry and 401/403 handling is inconsistent. | Backend/security owner; review with every auth, controller or client-session task. |
| R-04 | **Network emulator and unreliable network behaviour.** Client flows may work only on a fast network and fail under timeout, latency or offline conditions. The result may be duplicate checkout, stuck loading state or lost user feedback. | 4 | 3 | 12 / High | Define repeatable slow-network, timeout, offline and retry scenarios. Configure explicit Retrofit/API timeouts. Make loading, error, retry and idempotency states visible in the client. Verify that retrying checkout cannot create duplicate orders. | A request times out, the UI remains stuck, a retry duplicates an order, or the client cannot recover after connectivity returns. | Android/client owner with backend support; review before declaring checkout and delivery flows complete. |
| R-05 | **RabbitMQ scope drift.** RabbitMQ added for the environment demo becomes an unplanned business dependency and blocks the modular-monolith MVP when the broker is unavailable. | 2 | 3 | 6 / Medium | Keep RabbitMQ as an infrastructure/demo service only for the current task. Do not add producers, consumers or Spring AMQP business flows unless a later issue and ADR approve them. REST plus database transactions remain the MVP path. | API startup or a core Customer/Merchant/Driver flow fails when RabbitMQ is stopped; or a new queue-driven requirement appears without scope approval. | Technical lead; review whenever messaging is proposed. |
| R-06 | **Database schema and application model drift.** Flyway migrations, the ERD, the data dictionary and application code become inconsistent, causing startup or runtime failures. | 3 | 4 | 12 / High | Make schema changes through versioned Flyway migrations. Use PostgreSQL for integration verification. Review the canonical ERD and relational model before migration work. Do not rely on manual schema edits. | Migration failure, API database health failure, column/type mismatch, or a clean clone that cannot start from the documented setup. | Backend/database owner; review before each schema-changing task. |

## 4. Immediate mitigation backlog

| Priority | Action | Related risk | Evidence of completion |
|---|---|---|---|
| Must | Keep the approved MVP boundary visible in every task and issue. | R-01 | Scope/SRS and issue templates link to the approved scope. |
| Must | Add transaction and concurrency tests before accepting stock/capacity checkout work. | R-02 | PostgreSQL integration test demonstrates competing reservations and correct release. |
| Must | Add authorization tests for role and Store ownership. | R-03 | 401/403 and cross-Store test cases pass. |
| Must | Define network scenarios before declaring a client checkout flow complete. | R-04 | Reproducible timeout/offline/retry checklist and screenshots or test results. |
| Should | Keep RabbitMQ disconnected from business flow until a separate decision is approved. | R-05 | API can start and core REST flow remains documented without broker dependency. |
| Should | Require an ERD/data-dictionary review for every future migration. | R-06 | Migration PR links to the affected model and passes PostgreSQL verification. |

## 5. Review procedure

The project team reviews this register at the end of every week and whenever a trigger occurs. The review must update probability, impact, status, mitigation progress and next action. A risk is not considered closed merely because no incident has occurred; it is closed only when the mitigation is implemented and its verification evidence exists.

If a Critical or High risk blocks a Must task, the dependent task must not be marked Done until the risk owner records either a passing verification result or an explicit, documented acceptance of the remaining risk.

## 6. Traceability to project decisions

The scope risk is controlled by the approved 12-week MVP scope. The inventory risk follows the rule that `MADE_TO_ORDER` uses daily capacity and `LIMITED_STOCK` uses stock/reservation. The security risk follows the JWT/RBAC and Store-ownership requirements. The network risk is associated with the planned Android clients and shared REST API. The RabbitMQ risk preserves the decision that the MVP remains a modular monolith and that messaging is not a mandatory business dependency.

## 7. References

[1]: https://github.com/trunghieu2910/FreshFlow/blob/main/docs/FF-01-01-1_scope.md "FreshFlow MVP scope"
[2]: https://github.com/trunghieu2910/FreshFlow/blob/main/docs/FF-01-02-1_FreshFlow%20Order%20State%20Machine.md "FreshFlow order state machine"
[3]: https://github.com/trunghieu2910/FreshFlow/blob/main/docs/adr/ADR-001-modular-monolith.md "ADR-001: Modular Monolith"
[4]: https://github.com/trunghieu2910/FreshFlow/blob/main/docs/architecture/erd.md "FreshFlow canonical ERD"
[5]: https://github.com/trunghieu2910/FreshFlow/blob/main/docs/database/01-relational-model.md "FreshFlow relational model"

- [1] Approved MVP scope and actor boundaries.
- [2] Order state transitions and payment/delivery rules.
- [3] Modular-monolith boundary and messaging decision.
- [4] Ownership, inventory and order persistence model.
- [5] Relational normalization and migration planning.
