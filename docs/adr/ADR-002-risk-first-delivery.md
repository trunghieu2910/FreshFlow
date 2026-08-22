# ADR-002: Risk-first delivery and scope control for FreshFlow MVP

- **Status:** Accepted
- **Date:** 2026-08-22
- **Decision owners:** FreshFlow project team
- **Task:** `FF-01-06-2`
- **Related ADR:** [`ADR-001-modular-monolith`](./ADR-001-modular-monolith.md)

## 1. Context

FreshFlow is a 12-week MVP built as a modular monolith with one Spring Boot backend, PostgreSQL persistence, a React Merchant client, an Android Customer client and an Android Driver client. The approved product scope contains several flows that are individually manageable but collectively create delivery risk: catalog and ProductVariant availability, cart and checkout, Merchant acceptance, payment methods, Driver assignment, delivery confirmation, OTP and dispute handling.

The highest risks are not limited to implementation difficulty. Scope expansion can consume the schedule before the core flows are stable. Concurrent checkout can oversell limited stock or overbook daily capacity. Incorrect JWT, RBAC or Store-ownership checks can expose another user's or Store's data. Unreliable mobile networks can make checkout and delivery flows appear successful while producing duplicate requests or unclear client state.

RabbitMQ is present in the local infrastructure demo for FF-01-06-1, but the approved MVP architecture does not make messaging a mandatory business dependency. This ADR records how the team will control that risk while continuing to deliver the MVP.

## 2. Decision

FreshFlow will use a **risk-first delivery and scope-control policy** for the remainder of the MVP. A task is not complete merely because its happy path works; the task must also address the relevant high-priority risk and provide reproducible verification evidence.

The policy has six parts:

1. **Protect the MVP boundary.** Must work for Customer, Merchant, Driver and Backend flows is delivered before optional features. GPS, maps, production payment, full microservice decomposition, Electron and additional roles remain outside the MVP unless a separate scope decision approves them.
2. **Treat stock and capacity consistency as a correctness requirement.** The backend rechecks availability during checkout and reserves stock or daily capacity inside a database transaction. The implementation must use row-level locking or an equivalent version check, prevent negative quantities and release reservations on reject, cancel or payment failure.
3. **Treat authentication and ownership as acceptance requirements.** JWT handling, RBAC and Store/resource ownership checks are implemented together. A role check alone is not sufficient to authorize access to an order, product, Store or delivery resource.
4. **Test unreliable network conditions deliberately.** Client and API verification includes repeatable timeout, latency, offline and retry scenarios. A retry must not create a duplicate order, and the UI must expose loading, error and recovery states.
5. **Keep RabbitMQ optional for business logic.** RabbitMQ may remain available as a Docker Compose infrastructure/demo service, but the MVP REST and database transaction path must not require the broker. Producers, consumers or Spring AMQP integration require a separate issue and decision record.
6. **Review risks continuously.** The team reviews `docs/risk-register.md` at the end of every week and immediately when a trigger appears. Critical and High risks block the relevant Must task until mitigation evidence exists or the remaining risk is explicitly accepted.

## 3. Risk response matrix

| Risk | Decision response | Required evidence before dependent work is Done |
|---|---|---|
| Broad MVP scope | Avoid and reduce. Freeze the approved boundary and move non-essential requests to Backlog V2. | Issue or task remains traceable to an approved MVP flow; scope changes have a decision record. |
| Stock/capacity concurrency | Reduce. Use transaction boundaries, locking/version checks, server-side revalidation and release rules. | PostgreSQL integration or concurrency test demonstrates no oversell/overbooking and correct release. |
| JWT/token and ownership | Reduce. Combine authentication, role authorization and Store/resource ownership checks. | Tests cover unauthenticated, wrong-role, expired-token and cross-Store access. |
| Network emulator conditions | Reduce. Use deterministic timeout/offline/latency/retry scenarios and idempotent checkout requests. | Reproducible client/API verification shows safe retry and recoverable UI state. |
| RabbitMQ scope drift | Avoid. Keep the broker outside the core business path until separately approved. | API and core flow documentation remains runnable without RabbitMQ business integration. |
| Schema/model drift | Reduce. Use Flyway migrations and review ERD/data dictionary alignment before schema changes. | PostgreSQL-backed migration and startup verification pass from a clean checkout. |

## 4. Consequences

### Positive consequences

The team receives an explicit rule for deciding what to do when time is limited: protect the core flows and resolve correctness and security risks before adding breadth. The risk register becomes actionable because every High or Critical item has a trigger and an expected verification artifact. The architecture remains simple while the project still demonstrates awareness of concurrency, security, unreliable networks and future event-driven options.

The decision also improves reproducibility. A contributor can review the scope, risk register and ADR before implementing a task, and can understand why RabbitMQ is present in local infrastructure without assuming that the application already depends on a message broker.

### Negative consequences

Some attractive features will be postponed even when they appear technically interesting. Concurrency, authorization and network tests may increase the effort of a task before its happy path is considered complete. Keeping RabbitMQ outside the business path means that a future event-driven migration will require an additional design step rather than being available automatically.

These costs are intentional. They protect the 12-week learning and portfolio objective from uncontrolled scope and fragile demonstrations.

## 5. Implementation rules

Every new issue must identify its related MVP flow, priority, acceptance criteria, verification plan and output. If the issue introduces a new dependency, client, actor, external integration or persistence rule, the issue must also state its schedule impact and link to an ADR when appropriate.

Order and inventory work must document its transaction boundary and failure-release behaviour. Authentication work must document both role checks and ownership checks. Client work involving remote calls must document timeout, retry and offline behaviour. Infrastructure work must state whether a service is required for the business path or is only a local/demo dependency.

No rule in this ADR requires implementation of a feature that is outside the approved MVP scope. This ADR governs prioritization and verification; it does not replace the SRS, state machine, ERD or API contract.

## 6. Review trigger

This ADR must be reviewed when one of the following occurs:

- The MVP boundary, actor list or 12-week schedule changes.
- A stock/capacity defect, duplicate checkout or reservation-release defect is found.
- A token, authorization or cross-Store access defect is found.
- Network testing reveals an unsafe retry or unrecoverable client state.
- RabbitMQ becomes a required business dependency or a team proposes producers/consumers.
- A database migration conflicts with the canonical ERD or relational model.

The review outcome must be recorded by updating this ADR or creating a superseding ADR. The risk register must be updated in the same change when a risk score, trigger or mitigation changes.

## 7. Decision outcome

FreshFlow continues with the approved modular-monolith MVP. The project will prioritize Must scope, transactional stock/capacity correctness, JWT/RBAC plus ownership, and reproducible network-condition testing. RabbitMQ remains a local/demo infrastructure capability and is not a mandatory application dependency for the MVP.

## 8. References

[1]: https://github.com/trunghieu2910/FreshFlow/blob/main/docs/FF-01-01-1_scope.md "FreshFlow MVP scope"
[2]: https://github.com/trunghieu2910/FreshFlow/blob/main/docs/FF-01-02-1_FreshFlow%20Order%20State%20Machine.md "FreshFlow order state machine"
[3]: https://github.com/trunghieu2910/FreshFlow/blob/main/docs/adr/ADR-001-modular-monolith.md "ADR-001: Modular Monolith"
[4]: https://github.com/trunghieu2910/FreshFlow/blob/main/docs/architecture/erd.md "FreshFlow canonical ERD"
[5]: https://github.com/trunghieu2910/FreshFlow/blob/main/docs/database/01-relational-model.md "FreshFlow relational model"
[6]: https://github.com/trunghieu2910/FreshFlow/blob/main/docs/risk-register.md "FreshFlow risk register"

- [1] Approved MVP boundary, actors and integrations.
- [2] Order state and payment/delivery constraints.
- [3] Modular-monolith boundary and messaging decision.
- [4] Persistence ownership and inventory model.
- [5] Relational model and migration planning.
- [6] Risk scores, mitigations, triggers and review procedure.
