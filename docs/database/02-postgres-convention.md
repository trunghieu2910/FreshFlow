# FreshFlow PostgreSQL Convention

## 1. Mục đích

Tài liệu này quy định các quy ước PostgreSQL dùng trong FreshFlow. Mục tiêu là bảo đảm ERD, Flyway migration, Java entity và API sử dụng cùng một cách đặt tên, kiểu dữ liệu và quy tắc toàn vẹn dữ liệu.

## 2. Database local

PostgreSQL local được chạy bằng Docker Compose tại `infrastructure/docker-compose.yml`.

| Thành phần | Quy ước |
|---|---|
| Database | `freshflow` |
| PostgreSQL image | `postgres:16` |
| Host port | `5432` |
| Container | `freshflow-postgres` |
| Storage | Named volume `freshflow_postgres_data` |
| Healthcheck | `pg_isready` |
| Credentials | Đọc từ file local `.env`, không commit `.env` |

RabbitMQ hoặc messaging broker không phải dependency bắt buộc của MVP. Chỉ thêm khi một task cụ thể yêu cầu hoặc còn thời gian sau khi order flow cốt lõi đã ổn định.

## 3. Naming convention

Dùng `snake_case` cho tên schema, bảng, cột, index, constraint và migration.

Tên bảng dùng dạng số nhiều:

```text
users
stores
products
orders
order_items
