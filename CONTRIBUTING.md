# Contributing to FreshFlow

Thank you for contributing to FreshFlow. This document defines the local development setup, backend quality commands, API error convention, and Git commit convention used by the project.

## 1. Project principles

FreshFlow is developed as a modular monolith for the MVP. The backend is implemented with Java 21 and Spring Boot. PostgreSQL is the local development database and must be run through Docker Compose.

The MVP does not require H2, RabbitMQ, Kafka, or any other messaging broker. Do not add a messaging dependency unless a separate task explicitly requires and documents it.

Business modules are organized under the following package boundaries:

```text
com.freshflow.api.common
com.freshflow.api.catalog
com.freshflow.api.order
com.freshflow.api.payment
com.freshflow.api.delivery
```

## 2. Development prerequisites

Install the following tools before working on the project:

| Tool | Required version or requirement |
|---|---|
| Git | A current version with Git Bash on Windows |
| Java | JDK 21 |
| Maven | Maven Wrapper or Maven 3.9+ |
| Docker Desktop | Docker Engine and Docker Compose enabled |
| IntelliJ IDEA | Recommended IDE; Java 21 configured as the project SDK |
| PostgreSQL client | Optional; `psql` inside the PostgreSQL container is sufficient |

Verify Java and Docker:

```bash
java -version
docker version
docker compose version
```

## 3. Clone and open the project

Run the following commands only when obtaining the repository for the first time:

```bash
git clone https://github.com/trunghieu2910/FreshFlow.git
cd FreshFlow
```

If the repository is already present, do not clone it again. Navigate directly to the existing root:

```bash
cd /d/FreshFlow
```

Open the repository root in IntelliJ IDEA. The backend module is located at:

```text
services/freshflow-api
```

## 4. Local PostgreSQL setup

The backend connects to PostgreSQL at `localhost:5432` using the `freshflow` database. PostgreSQL is provided by Docker Compose; PostgreSQL does not need to be installed directly on Windows.

Start the database from the repository root:

```bash
cd /d/FreshFlow/infrastructure
docker compose up -d
```

Check the container and health status:

```bash
docker compose ps
```

The PostgreSQL container should show a healthy status. To verify that PostgreSQL accepts connections:

```bash
docker compose exec -T postgres pg_isready -U freshflow -d freshflow
```

Expected output contains:

```text
accepting connections
```

Stop the container without deleting its named volume:

```bash
docker compose down
```

Start it again when needed:

```bash
docker compose up -d
```

Do not use `docker compose down -v` unless you intentionally want to delete the local database volume and all local data.

## 5. Backend configuration

The backend module is located at:

```text
services/freshflow-api
```

The application uses the local PostgreSQL instance configured in `application.properties`. Local credentials must not be committed to Git.

Never commit any of the following:

```text
.env
.idea/
target/
credentials
private keys
local database dumps containing sensitive data
```

Use `infrastructure/.env.example` as the shareable template for local environment variables.

## 6. Running the backend

From the repository root, use:

```bash
cd /d/FreshFlow/services/freshflow-api
mvn spring-boot:run
```

The application runs on port `8080` unless the local configuration specifies another port.

Check the health endpoint:

```text
http://localhost:8080/actuator/health
```

The expected response contains an `UP` status.

## 7. Test commands

Run the complete test suite:

```bash
cd /d/FreshFlow/services/freshflow-api
mvn clean test
```

Run a single test class:

```bash
mvn -Dtest=ApiErrorResponseTest test
```

The backend test suite uses the real local PostgreSQL database for Spring context tests. Do not introduce H2 merely to avoid starting PostgreSQL or to hide a configuration problem.

Before creating a commit, the complete test suite must pass with zero failures and zero errors.

## 8. Code formatting

FreshFlow uses Spotless with Google Java Format for Java source files under:

```text
src/main/java/**/*.java
src/test/java/**/*.java
```

Apply formatting automatically:

```bash
cd /d/FreshFlow/services/freshflow-api
mvn spotless:apply
```

Check formatting without changing files:

```bash
mvn spotless:check
```

Run the full verification lifecycle, including the Spotless check:

```bash
mvn verify
```

A pull request or commit must not be created while `spotless:check` or `mvn verify` is failing.

## 9. API error convention

All future REST API error responses should use the following four-field schema:

```json
{
  "code": "ORDER_NOT_FOUND",
  "message": "Order was not found",
  "path": "/api/orders/123",
  "timestamp": "2026-08-20T10:15:30Z"
}
```

The fields have the following meanings:

| Field | Type | Meaning |
|---|---|---|
| `code` | String | Stable machine-readable error code |
| `message` | String | Human-readable error message |
| `path` | String | Request path that produced the error |
| `timestamp` | ISO-8601 UTC string | Time at which the error was created |

The backend represents the timestamp with Java `Instant`. Timestamps must use UTC and should normally end with `Z`.

Error codes should be stable and descriptive. Examples include:

```text
VALIDATION_FAILED
RESOURCE_NOT_FOUND
ORDER_NOT_FOUND
ORDER_STATE_INVALID
ACCESS_DENIED
INTERNAL_ERROR
```

Do not expose stack traces, database credentials, SQL statements, or internal implementation details in the API response.

The current task defines the error response model and its serialization test. A global exception handler will be introduced in a later REST API task.

## 10. Commit convention

FreshFlow follows Conventional Commits. Use this format:

```text
<type>(<scope>): <imperative summary>
```

Allowed types include:

| Type | Usage |
|---|---|
| `feat` | Add a user-visible or business capability |
| `fix` | Correct an existing defect |
| `refactor` | Change structure without changing behavior |
| `test` | Add or improve tests and test infrastructure |
| `docs` | Change documentation only |
| `chore` | Maintenance, configuration, or tooling work |
| `build` | Change build or dependency configuration |

Recommended scopes include:

```text
backend
infra
catalog
order
payment
delivery
common
android
web
```

The summary should be short, written in the imperative mood, and should not end with a period.

Examples:

```text
feat(order): add order pricing calculation
fix(payment): reject duplicate payment confirmation
refactor(backend): establish modular monolith package boundaries
test(backend): establish test and API quality conventions
docs(database): document PostgreSQL naming conventions
chore(infra): standardize local PostgreSQL with Docker Compose
```

## 11. Git safety rules

Always inspect the working tree before staging files:

```bash
git status --short
git diff --check
```

Stage only files belonging to the current task. Do not use the following command in a working tree containing unrelated changes:

```bash
git add .
```

Review the staged changes before committing:

```bash
git diff --cached --check
git diff --cached --name-status
git diff --cached --stat
```

Do not commit `.env`, `.idea/`, `target/`, credentials, or unrelated personal documents. Do not force-push to `main`.

## 12. Definition of done for backend changes

A backend change is ready for commit only when the relevant source code is implemented, tests are added or updated, formatting is applied, and the following commands pass:

```bash
cd /d/FreshFlow/services/freshflow-api
mvn clean test
mvn spotless:check
mvn verify
```

The final Git diff must contain only files relevant to the current task, and the commit message must follow the Conventional Commits format.
