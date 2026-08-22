# Hướng dẫn container hóa PostgreSQL bằng Docker Compose

## 1. Mục tiêu của hướng dẫn

Tài liệu này hướng dẫn cách chạy PostgreSQL trong Docker container để làm database local cho backend Spring Boot của FreshFlow. Mỗi bước đều giải thích ba nội dung: **cần làm gì**, **tại sao cần làm** và **kết quả mong đợi**.

Cách làm này được gọi là **container hóa cơ sở dữ liệu** hoặc **containerized database**. Trong trường hợp của FreshFlow, tên đầy đủ là:

> **Container hóa PostgreSQL bằng Docker Compose cho môi trường phát triển local.**

Docker không thay thế PostgreSQL. PostgreSQL vẫn là hệ quản trị cơ sở dữ liệu; Docker chỉ cung cấp môi trường độc lập để chạy PostgreSQL một cách dễ cài đặt, dễ xóa, dễ khởi động lại và dễ chia sẻ cho các thành viên khác trong nhóm.

## 2. Mô hình hoạt động

Khi cài PostgreSQL trực tiếp, cấu trúc thường là:

```text
Windows
└── PostgreSQL Service
    └── Database: freshflow
```

Khi dùng Docker, cấu trúc của FreshFlow là:

```text
Windows
└── Docker Desktop
    └── Docker Engine
        └── Container: freshflow-postgres
            └── PostgreSQL 16
                └── Database: freshflow
```

Spring Boot không kết nối trực tiếp với Docker. Spring Boot kết nối đến PostgreSQL qua cổng được Docker ánh xạ:

```text
Spring Boot
    ↓
jdbc:postgresql://localhost:5432/freshflow
    ↓
Windows localhost:5432
    ↓
Docker container port 5432
    ↓
PostgreSQL 16
```

Docker Compose cho phép mô tả service, network và volume trong một file YAML, sau đó khởi động toàn bộ cấu hình bằng một lệnh [1].

## 3. Những thành phần cần hiểu trước

| Thành phần | Ý nghĩa | Vai trò trong FreshFlow |
|---|---|---|
| Docker Desktop | Ứng dụng Docker trên Windows | Cung cấp Docker Engine để chạy container |
| Docker Engine | Bộ máy chạy container | Tạo, khởi động và quản lý container |
| Image | Mẫu đóng gói phần mềm | `postgres:16` là mẫu PostgreSQL 16 |
| Container | Một instance đang chạy từ image | `freshflow-postgres` là PostgreSQL đang chạy |
| Docker Compose | Công cụ khai báo và quản lý nhiều service | Đọc `docker-compose.yml` |
| Port mapping | Ánh xạ cổng máy thật với cổng container | `5432:5432` |
| Volume | Vùng lưu trữ dữ liệu do Docker quản lý | Giữ dữ liệu PostgreSQL sau khi container bị xóa |
| PostgreSQL | Hệ quản trị cơ sở dữ liệu | Lưu dữ liệu FreshFlow |

Một **image** giống như bản mẫu. Một **container** giống như chương trình được tạo và chạy từ bản mẫu đó. Vì vậy, `postgres:16` là image, còn `freshflow-postgres` là container.

## 4. Chuẩn bị môi trường

### 4.1. Cài Docker Desktop

Trên Windows, cài Docker Desktop và mở ứng dụng. Docker Desktop thường sử dụng Docker Engine bên trong môi trường Linux/WSL2 để chạy các Linux container.

**Mục đích:** Docker Desktop là nền tảng giúp các lệnh `docker` và `docker compose` giao tiếp được với Docker Engine.

Kiểm tra Docker Engine bằng Git Bash:

```bash
docker version
```

Kết quả hợp lệ cần có cả phần `Client` và `Server`. Nếu chỉ có `Client` hoặc báo không thể kết nối đến Docker Engine, hãy mở Docker Desktop và chờ đến khi Docker báo đang hoạt động.

Kiểm tra Docker Compose:

```bash
docker compose version
```

Kết quả cần có dạng tương tự:

```text
Docker Compose version v5.3.0
```

### 4.2. Vì sao không cần cài PostgreSQL trực tiếp trên Windows?

Không cần cài PostgreSQL riêng trên Windows nếu Docker đã chạy PostgreSQL container. Cài đồng thời hai PostgreSQL và cho cả hai sử dụng cổng `5432` có thể gây xung đột cổng.

Docker phù hợp với FreshFlow vì mọi thành viên có thể dùng cùng phiên bản PostgreSQL, cùng tên database, cùng username, cùng password local và cùng port. Chỉ cần có Docker, họ có thể khởi động môi trường bằng file Compose trong repository.

## 5. Tạo cấu trúc thư mục

Trong Git Bash, chuyển đến thư mục gốc FreshFlow:

```bash
cd /d/FreshFlow
```

Tạo thư mục infrastructure:

```bash
mkdir -p infrastructure
```

Cấu trúc mục tiêu:

```text
FreshFlow/
├── infrastructure/
│   └── docker-compose.yml
└── services/
    └── freshflow-api/
```

**Mục đích:** Tách cấu hình hạ tầng khỏi source code của backend. Docker Compose là cấu hình infrastructure, không phải code nghiệp vụ của order, payment hoặc delivery.

## 6. Tạo file `docker-compose.yml`

Tạo file:

```text
D:/FreshFlow/infrastructure/docker-compose.yml
```

Dán nội dung sau:

```yaml
services:
  postgres:
    image: postgres:16
    container_name: freshflow-postgres
    environment:
      POSTGRES_DB: freshflow
      POSTGRES_USER: freshflow
      POSTGRES_PASSWORD: freshflow123
    ports:
      - "5432:5432"
    volumes:
      - freshflow_postgres_data:/var/lib/postgresql/data

volumes:
  freshflow_postgres_data:
```

### 6.1. Giải thích từng dòng

| Dòng cấu hình | Làm gì? | Vì sao cần? |
|---|---|---|
| `services:` | Bắt đầu danh sách service | Compose biết các container cần quản lý |
| `postgres:` | Đặt tên service là `postgres` | Dùng để quản lý service trong Compose |
| `image: postgres:16` | Chọn image PostgreSQL 16 | Đảm bảo mọi người dùng cùng major version |
| `container_name: freshflow-postgres` | Đặt tên container cố định | Dễ nhận diện trong Docker Desktop và lệnh CLI |
| `POSTGRES_DB: freshflow` | Tạo database `freshflow` ở lần khởi tạo đầu tiên | Khớp với datasource của Spring Boot |
| `POSTGRES_USER: freshflow` | Tạo user database `freshflow` | Spring Boot dùng user này để kết nối |
| `POSTGRES_PASSWORD: freshflow123` | Đặt password local | Spring Boot phải cung cấp đúng password |
| `5432:5432` | Ánh xạ port Windows vào port container | Cho phép ứng dụng truy cập database qua `localhost:5432` |
| `freshflow_postgres_data:` | Khai báo named volume | Giữ dữ liệu ngoài vòng đời container |
| `/var/lib/postgresql/data` | Đường dẫn data directory của PostgreSQL 16 trong container | Đây là nơi PostgreSQL lưu file dữ liệu |

### 6.2. Về username và password

Các giá trị sau chỉ dùng cho môi trường local học tập:

```text
Database: freshflow
Username: freshflow
Password: freshflow123
```

Trong môi trường production không nên commit password thật vào GitHub. Khi triển khai thật, nên dùng biến môi trường, Docker secrets hoặc secret manager.

## 7. Khởi động PostgreSQL

Mở Git Bash tại thư mục infrastructure:

```bash
cd /d/FreshFlow/infrastructure
```

Khởi động service ở chế độ nền:

```bash
docker compose up -d
```

### 7.1. Ý nghĩa của `docker compose up -d`

| Phần | Ý nghĩa |
|---|---|
| `docker compose` | Gọi công cụ quản lý cấu hình Compose |
| `up` | Tạo và khởi động các service |
| `-d` | Chạy detached, tức là chạy nền và trả terminal về cho bạn |

Lần chạy đầu tiên Docker có thể phải tải image `postgres:16`. Sau đó Docker sẽ tạo network, volume và container nếu chúng chưa tồn tại.

Docker Compose được thiết kế để quản lý vòng đời service như khởi động, dừng, rebuild, xem trạng thái và xem log [1].

## 8. Kiểm tra container

Chạy:

```bash
docker compose ps
```

Kết quả mong đợi:

```text
NAME                 IMAGE         SERVICE    STATUS    PORTS
freshflow-postgres   postgres:16   postgres   Up        0.0.0.0:5432->5432/tcp
```

### 8.1. Ý nghĩa kết quả

- `NAME` là tên container.
- `IMAGE` cho biết container được tạo từ image nào.
- `SERVICE` là tên service trong Compose.
- `STATUS: Up` nghĩa là container đang chạy.
- `5432->5432` nghĩa là cổng `5432` của máy Windows được chuyển tiếp vào cổng `5432` của container.

Nếu không thấy container ở trạng thái `Up`, PostgreSQL chưa sẵn sàng để Spring Boot kết nối.

## 9. Kiểm tra log PostgreSQL

Xem log:

```bash
docker compose logs postgres
```

Theo dõi log liên tục:

```bash
docker compose logs -f postgres
```

Dừng việc theo dõi log bằng:

```text
Ctrl + C
```

**Mục đích:** Log giúp phát hiện các lỗi như password không hợp lệ, cổng bị chiếm, database không khởi động hoặc volume bị lỗi.

## 10. Cấu hình Spring Boot kết nối PostgreSQL

Mở file:

```text
services/freshflow-api/src/main/resources/application.properties
```

Cấu hình:

```properties
spring.application.name=freshflow-api
server.port=8080

# Datasource
spring.datasource.url=jdbc:postgresql://localhost:5432/freshflow
spring.datasource.username=freshflow
spring.datasource.password=freshflow123
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# Actuator
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

### 10.1. Giải thích datasource

| Property | Mục đích |
|---|---|
| `spring.datasource.url` | Cho Spring Boot biết loại database, host, port và database name |
| `spring.datasource.username` | User để đăng nhập PostgreSQL |
| `spring.datasource.password` | Password của user |
| `spring.datasource.driver-class-name` | JDBC driver dùng để giao tiếp với PostgreSQL |

Chuỗi:

```text
jdbc:postgresql://localhost:5432/freshflow
```

được đọc như sau:

```text
jdbc       → giao thức Java Database Connectivity
postgresql → loại database
localhost  → máy đang chạy Docker Desktop
5432       → port PostgreSQL được expose
freshflow  → tên database
```

### 10.2. Vì sao dùng `ddl-auto=validate`?

`validate` yêu cầu Hibernate kiểm tra entity và schema có tương thích hay không, nhưng không tự ý tạo hoặc sửa bảng.

FreshFlow dùng Flyway để quản lý schema bằng migration. Vì vậy, để Hibernate tự động tạo bảng bằng `create` hoặc `update` sẽ làm mất tính kiểm soát của migration.

Ở giai đoạn skeleton chưa có bảng entity và chưa có migration nghiệp vụ, ứng dụng vẫn có thể khởi động với schema trống. Khi bắt đầu tạo bảng, migration Flyway sẽ được thêm vào `src/main/resources/db/migration/`.

## 11. Thêm dependency Flyway cho PostgreSQL

Với phiên bản Flyway mới, hỗ trợ database PostgreSQL được tách thành module riêng. Trong `pom.xml`, cần có:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>

<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

**Mục đích:** Starter cung cấp tích hợp Flyway với Spring Boot, còn `flyway-database-postgresql` cung cấp khả năng nhận diện và làm việc với PostgreSQL.

Nếu thiếu module PostgreSQL, ứng dụng có thể kết nối được database nhưng Flyway báo lỗi tương tự:

```text
Unsupported Database: PostgreSQL 16.15
```

## 12. Tạo thư mục migration

Tạo thư mục:

```text
services/freshflow-api/src/main/resources/db/migration/
```

Có thể thêm file placeholder:

```text
.gitkeep
```

**Mục đích:** Flyway mặc định tìm migration ở `classpath:db/migration`. Hiện tại FreshFlow chưa có bảng nghiệp vụ nên thư mục có thể chưa chứa file SQL nào. Khi tạo schema, tên file thường có dạng:

```text
V1__create_users.sql
V2__create_stores.sql
V3__create_products.sql
```

Số version giúp Flyway chạy migration theo thứ tự và ghi lại migration nào đã chạy.

## 13. Chạy test để kiểm tra toàn bộ kết nối

Trong Git Bash:

```bash
cd /d/FreshFlow/services/freshflow-api
mvn clean test
```

**Mục đích:** Không chỉ kiểm tra code domain, lệnh này còn kiểm tra Spring Application Context, datasource, JPA, Flyway và PostgreSQL thật.

Kết quả mong đợi:

```text
Tests run: 68, Failures: 0, Errors: 0
BUILD SUCCESS
```

Không dùng H2 để né lỗi datasource. FreshFlow chủ động dùng PostgreSQL thật vì mục tiêu là luyện tập stack gần với dự án thực tế.

## 14. Chạy ứng dụng và kiểm tra health

Chạy `FreshflowApiApplication` bằng nút Run trong IntelliJ IDEA.

Log thành công thường có các dòng tương tự:

```text
Database: jdbc:postgresql://localhost:5432/freshflow
Successfully validated 0 migrations
Tomcat started on port 8080
Started FreshflowApiApplication
```

Mở trình duyệt:

```text
http://localhost:8080/actuator/health
```

Kết quả cần có:

```json
{"status":"UP"}
```

**Mục đích:** Endpoint health xác nhận ứng dụng đã khởi động và các health indicator cơ bản, bao gồm kết nối datasource, đang hoạt động.

## 15. Làm việc với container trong hằng ngày

### 15.1. Dừng container nhưng giữ dữ liệu

```bash
docker compose stop
```

Lệnh này dừng service nhưng giữ container, network và volume.

Khởi động lại:

```bash
docker compose start
```

Hoặc dùng:

```bash
docker compose up -d
```

### 15.2. Dừng và xóa container

```bash
docker compose down
```

Lệnh này xóa container và network do Compose tạo, nhưng named volume thường vẫn còn. Vì volume tồn tại độc lập với vòng đời container, dữ liệu vẫn được giữ lại [2].

Khởi động lại:

```bash
docker compose up -d
```

### 15.3. Xóa cả volume và dữ liệu

```bash
docker compose down -v
```

**CẢNH BÁO:** `-v` xóa volume `freshflow_postgres_data`. Toàn bộ dữ liệu PostgreSQL local sẽ mất.

Chỉ dùng lệnh này khi bạn thực sự muốn reset database từ đầu, ví dụ khi đang học và muốn chạy lại quá trình khởi tạo schema.

## 16. Vì sao phải dùng volume?

Filesystem bên trong container có vòng đời gắn với container. Nếu container bị xóa mà không có volume, file dữ liệu database có thể mất. Volume là vùng lưu trữ do Docker quản lý, tồn tại độc lập với container và là cơ chế được khuyến nghị để lưu dữ liệu của container [2].

Trong Compose:

```yaml
volumes:
  - freshflow_postgres_data:/var/lib/postgresql/data
```

Có hai phía:

```text
freshflow_postgres_data        → tên volume trên Docker
/var/lib/postgresql/data       → thư mục dữ liệu bên trong PostgreSQL container
```

Kiểm tra volume:

```bash
docker volume ls
```

Xem chi tiết volume:

```bash
docker volume inspect infrastructure_freshflow_postgres_data
```

Tên volume thực tế có thể được Compose thêm prefix theo tên project. Vì vậy, hãy dùng `docker volume ls` để xem tên chính xác.

## 17. Kiểm tra database từ bên trong container

Bạn không bắt buộc phải cài `psql` trên Windows. PostgreSQL image đã có sẵn client `psql` bên trong container.

Mở shell PostgreSQL:

```bash
docker exec -it freshflow-postgres psql -U freshflow -d freshflow
```

Sau đó có thể chạy:

```sql
SELECT version();
\dt
\q
```

Ý nghĩa:

| Lệnh | Mục đích |
|---|---|
| `docker exec` | Chạy một lệnh bên trong container đang chạy |
| `-it` | Mở phiên terminal tương tác |
| `psql` | PostgreSQL command-line client |
| `-U freshflow` | Đăng nhập bằng user `freshflow` |
| `-d freshflow` | Kết nối database `freshflow` |
| `SELECT version();` | Kiểm tra phiên bản PostgreSQL |
| `\dt` | Liệt kê bảng |
| `\q` | Thoát khỏi psql |

## 18. Các lỗi thường gặp

### Lỗi 1: Cannot connect to the Docker daemon

Nguyên nhân thường là Docker Desktop chưa mở hoặc Docker Engine chưa sẵn sàng.

Cách xử lý:

```bash
docker version
```

Mở Docker Desktop, chờ Docker Engine running rồi chạy lại.

### Lỗi 2: Port is already allocated

Nguyên nhân là cổng `5432` đã được một PostgreSQL khác hoặc ứng dụng khác sử dụng.

Kiểm tra container:

```bash
docker ps
```

Nếu có PostgreSQL cũ đang dùng cổng đó, dừng nó hoặc đổi port host:

```yaml
ports:
  - "5433:5432"
```

Nếu đổi thành `5433:5432`, Spring Boot phải đổi URL thành:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/freshflow
```

Phần bên phải vẫn là port `5432` của PostgreSQL trong container; chỉ port bên trái trên Windows đổi thành `5433`.

### Lỗi 3: Password authentication failed

Kiểm tra ba giá trị trong `docker-compose.yml`:

```yaml
POSTGRES_DB: freshflow
POSTGRES_USER: freshflow
POSTGRES_PASSWORD: freshflow123
```

và ba giá trị tương ứng trong `application.properties`.

Một điểm quan trọng: các biến `POSTGRES_*` của official PostgreSQL image có tác dụng khi data directory được khởi tạo lần đầu. Nếu volume đã có dữ liệu, đổi password trong YAML không tự động đổi password của database hiện tại.

Trong môi trường học tập, nếu muốn reset hoàn toàn:

```bash
docker compose down -v
docker compose up -d
```

Nhắc lại rằng lệnh này xóa dữ liệu volume.

### Lỗi 4: Unsupported Database: PostgreSQL

Kiểm tra `pom.xml` có module sau không:

```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Sau đó chạy:

```bash
mvn clean test
```

### Lỗi 5: Container bị dừng ngay sau khi khởi động

Xem log:

```bash
docker compose logs postgres
```

Log thường cho biết nguyên nhân thật, chẳng hạn quyền truy cập volume, port, cấu hình environment hoặc dữ liệu database không tương thích.

### Lỗi 6: Spring Boot không kết nối được database

Kiểm tra theo thứ tự:

```bash
docker compose ps
```

Nếu container chưa `Up`, xử lý Docker trước.

Sau đó kiểm tra URL:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/freshflow
```

Tiếp theo kiểm tra username, password và database name. Cuối cùng kiểm tra PostgreSQL có expose đúng port không.

## 19. Quy trình làm việc hằng ngày của FreshFlow

Mỗi lần bắt đầu code backend:

```bash
cd /d/FreshFlow/infrastructure
docker compose up -d
docker compose ps
```

Sau đó chạy backend trong IntelliJ.

Khi kết thúc buổi học, có thể dừng container:

```bash
docker compose stop
```

Nếu muốn giải phóng container nhưng vẫn giữ dữ liệu:

```bash
docker compose down
```

Không chạy `docker compose down -v` theo thói quen vì lệnh đó xóa dữ liệu database.

## 20. Commit cấu hình Docker vào Git

File cần commit:

```text
infrastructure/docker-compose.yml
```

Kiểm tra:

```bash
cd /d/FreshFlow
git status
git diff -- infrastructure/docker-compose.yml
```

Thêm và commit:

```bash
git add infrastructure/docker-compose.yml
git commit -m "chore(infra): add PostgreSQL Docker Compose setup"
git push origin main
```

**Mục đích:** Người khác clone repository có thể nhìn thấy và dùng đúng cấu hình database local. Docker Compose trở thành một phần có thể tái tạo của dự án, thay vì cấu hình chỉ tồn tại trên máy của bạn.

## 21. Cách áp dụng cho hệ quản trị khác

Nguyên tắc chung không đổi:

```text
Chọn image
→ cấu hình environment
→ ánh xạ port
→ gắn volume
→ khởi động container
→ cấu hình connection string cho ứng dụng
```

Ví dụ MySQL:

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: freshflow-mysql
    environment:
      MYSQL_DATABASE: freshflow
      MYSQL_USER: freshflow
      MYSQL_PASSWORD: freshflow123
      MYSQL_ROOT_PASSWORD: root123
    ports:
      - "3306:3306"
    volumes:
      - freshflow_mysql_data:/var/lib/mysql

volumes:
  freshflow_mysql_data:
```

Spring Boot sẽ dùng driver và URL khác:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/freshflow
spring.datasource.username=freshflow
spring.datasource.password=freshflow123
```

Ví dụ MongoDB:

```yaml
services:
  mongodb:
    image: mongo:8
    container_name: freshflow-mongodb
    environment:
      MONGO_INITDB_DATABASE: freshflow
    ports:
      - "27017:27017"
    volumes:
      - freshflow_mongo_data:/data/db

volumes:
  freshflow_mongo_data:
```

MongoDB là database NoSQL nên cấu hình ứng dụng và cách truy vấn sẽ khác PostgreSQL. Tuy nhiên, ý tưởng image, container, port và volume vẫn giống nhau.

## 22. Docker Compose không phải production database hoàn chỉnh

Cấu hình hiện tại phù hợp cho **local development và học tập**. Nó chưa phải cấu hình production hoàn chỉnh vì production cần thêm:

- Password được quản lý bằng secret, không ghi thẳng vào repository.
- Backup tự động và kiểm tra khả năng restore.
- Monitoring và cảnh báo.
- Giới hạn tài nguyên.
- Chính sách network và firewall.
- High availability hoặc replication nếu hệ thống cần uptime cao.
- Quy trình nâng cấp PostgreSQL có kiểm soát.

Do đó, câu mô tả chính xác là:

> “FreshFlow uses Docker Compose to run a persistent PostgreSQL container for local development and integration testing.”

## 23. Tóm tắt một câu lệnh và mục đích

| Lệnh | Mục đích |
|---|---|
| `docker version` | Kiểm tra Docker Engine |
| `docker compose version` | Kiểm tra Docker Compose |
| `docker compose up -d` | Tạo và chạy database container ở chế độ nền |
| `docker compose ps` | Kiểm tra trạng thái service |
| `docker compose logs postgres` | Xem log PostgreSQL |
| `docker compose stop` | Dừng container nhưng giữ cấu hình và dữ liệu |
| `docker compose start` | Chạy lại container đã dừng |
| `docker compose down` | Xóa container và network, thường giữ volume |
| `docker compose down -v` | Xóa container, network và volume; mất dữ liệu |
| `docker exec -it ... psql` | Mở PostgreSQL client bên trong container |
| `docker volume ls` | Liệt kê volume |
| `docker volume inspect ...` | Xem thông tin volume |

## 24. Kết luận

Trong FreshFlow, quy trình container hóa PostgreSQL là:

```text
Mở Docker Desktop
→ tạo docker-compose.yml
→ chọn image postgres:16
→ cấu hình database, user và password
→ ánh xạ port 5432
→ gắn named volume
→ chạy docker compose up -d
→ kiểm tra container Up
→ cấu hình Spring Boot datasource
→ chạy test
→ kiểm tra /actuator/health
```

Mỗi thành phần có trách nhiệm riêng:

- **PostgreSQL** lưu và truy vấn dữ liệu.
- **Docker image** đóng gói PostgreSQL.
- **Container** là PostgreSQL instance đang chạy.
- **Docker Compose** mô tả và điều khiển container.
- **Port mapping** cho phép Spring Boot truy cập database.
- **Volume** bảo vệ dữ liệu khỏi vòng đời container.

Đây là cách làm tiêu chuẩn và có thể áp dụng tương tự cho MySQL, MariaDB, MongoDB, Redis, SQL Server và nhiều hệ quản trị khác, miễn là có image phù hợp và biết đúng port, biến môi trường, thư mục dữ liệu cùng connection string.

## Tài liệu tham khảo

[1]: https://docs.docker.com/compose/ "Docker Docs — Docker Compose"

[2]: https://docs.docker.com/engine/storage/volumes/ "Docker Docs — Volumes"

[3]: https://docs.docker.com/guides/postgresql/ "Docker Docs — PostgreSQL specific guide"

[4]: https://hub.docker.com/_/postgres "Docker Hub — Official PostgreSQL Image"
