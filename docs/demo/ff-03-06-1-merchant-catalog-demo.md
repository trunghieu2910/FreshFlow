# FreshFlow Merchant Catalog Demo & Manual E2E Checklist (FF-03-06-1)

> **Mã nhiệm vụ:** `FF-03-06-1` — Demo Merchant Catalog  
> **Phạm vi:** Client-Server Integration giữa React Web và Spring Boot Backend (PostgreSQL)  
> **Thời gian:** 25/09/2026  
> **Trạng thái:** Verified & Passed  

---

## 1. Mục tiêu Demo

Chứng minh lát cắt dọc (Vertical Slice) của module **Merchant Catalog Management** hoạt động hoàn chỉnh và trơn tru khi kết nối giữa Frontend React (`freshflow-web`) và Backend Spring Boot (`freshflow-api`) với cơ sở dữ liệu thật sau một lượt khởi động sạch (Clean Start).

Luồng demo bao gồm 7 bước liên hoàn:
1. **Khởi động sạch hệ thống (Clean Start).**
2. **Đăng nhập Merchant (Demo Login Placeholder / 1-Click Login).**
3. **Hiển thị danh sách sản phẩm (Product List with Real Seed Data).**
4. **Tìm kiếm và lọc đa điều kiện (Search, Filter by Category, Size, Stock status).**
5. **Tạo mới món ăn và biến thể (Product Create with Nested Variants & Capacity Limit).**
6. **Chỉnh sửa món ăn (Product Edit with PATCH semantics).**
7. **Ẩn tạm thời / Xóa mềm (Soft Hide & Dependent Category Availability).**

---

## 2. Chuẩn bị Môi trường (Clean Start)

### 2.1. Khởi động Cơ sở dữ liệu và Backend API

```powershell
# 1. Khởi động PostgreSQL Container
cd d:\FreshFlow
docker compose up -d db

# 2. Khởi động Backend Spring Boot
cd services\freshflow-api
.\mvnw.cmd spring-boot:run
```

*Kiểm tra sức khỏe Backend:* Truy cập `http://localhost:8080/actuator/health` -> Trả về `{"status":"UP"}`.

### 2.2. Khởi động Web Frontend

```powershell
# 3. Khởi động React Web Dev Server
cd d:\FreshFlow\clients\freshflow-web
cmd /c npm run dev
```

*Địa chỉ truy cập Web:* `http://localhost:5173`

---

## 3. Bảng Checklist Kiểm định Thủ công (Manual E2E Checklist)

| Bước | Hành động thao tác | Kết quả kỳ vọng | Trạng thái |
|:---:|---|---|:---:|
| **1** | Truy cập `http://localhost:5173/login` | Hiển thị màn hình đăng nhập với 3 Demo Account cards (Merchant, Customer, Driver). | ✅ PASS |
| **2** | Bấm nút **"Đăng nhập với tư cách Merchant"** (`merchant@freshflow.com`) | Chuyển hướng thành công sang Dashboard `/dashboard`, hiển thị thông tin quán "Trà Sữa & Cà Phê Tươi FreshFlow". | ✅ PASS |
| **3** | Nhấp vào mục **"Quản lý Thực đơn"** trên Sidebar hoặc nút CTA trên Dashboard | Mở trang `/products`. Bảng danh sách hiển thị đầy đủ các món ăn nạp từ seed V5 (Trà đào cam sả, Cà phê sữa, Trà ô long...). | ✅ PASS |
| **4** | Gõ `"Trà đào"` vào ô tìm kiếm | Sau debounce 300ms, bảng chỉ hiển thị món "Trà Đào Cam Sả Tươi", không tải lại trang. | ✅ PASS |
| **5** | Chọn chip lọc kích cỡ **"Size L"** | Danh sách lọc chính xác các món có biến thể Size L với giá bán tương ứng. | ✅ PASS |
| **6** | Bật checkbox **"Chỉ hiện món còn bán"** | Các món có `active: false` hoặc biến thể `available: false` bị ẩn khỏi danh sách. | ✅ PASS |
| **7** | Bấm nút **"+ Thêm món mới"** ở góc trên bên phải | Mở Modal tạo món `ProductCreateModal`. Focus tự động đặt vào ô "Tên món ăn". | ✅ PASS |
| **8** | Nhập: Tên = `"Trà Sữa Oolong Nướng Hoàng Kim"`, Danh mục = `"Trà trái cây & Trà sữa"`, thêm 2 biến thể: Size M (35.000đ, hạn mức 50 ly), Size L (42.000đ, hạn mức 40 ly) | Dữ liệu hợp lệ, không có thông báo lỗi inline. | ✅ PASS |
| **9** | Bấm nút **"Lưu món ăn"** | Nút chuyển sang trạng thái `"Đang lưu…"` kèm Spinner, tự động khóa click. Sau khi thành công: Modal đóng, Toast `"Thêm món ăn mới thành công!"` hiển thị góc trên bên phải (`aria-live="polite"`), món mới xuất hiện đầu bảng. | ✅ PASS |
| **10** | Tìm món vừa tạo và bấm nút **"Sửa"** (icon bút) | Mở `ProductEditModal` với toàn bộ thông tin món và biến thể đã điền sẵn. | ✅ PASS |
| **11** | Đổi giá Size M thành `38.000đ`, đổi tên thành `"Trà Sữa Oolong Nướng Cháy (Đặc Biệt)"`, bấm **"Cập nhật"** | Gửi yêu cầu PATCH. Toast thông báo cập nhật thành công, giá trên bảng đổi thành `38.000đ` với font `tabular-nums`. | ✅ PASS |
| **12** | Gạt nút chuyển đổi trạng thái bán sang **"Tạm ngưng bán"** (Soft Hide) | Trạng thái chuyển sang màu xám "Tạm ngưng", badge cập nhật tương ứng. | ✅ PASS |
| **13** | Mở Giỏ hàng xem trước (`CartPreviewModal`) | Món vừa bị ẩn hiển thị cảnh báo không thể thanh toán (Disabled Checkout button) do quy tắc bảo vệ Dependent Availability. | ✅ PASS |

---

## 4. Bằng chứng Kiểm thử Tự động Hỗ trợ (Supporting Automated Tests)

Ngoài kiểm thử giao diện thủ công, toàn bộ các luồng con trên đã được bao phủ bởi 67 bài kiểm thử smoke và 12 RTL component tests:

```text
✓ tests/catalogComponentTests.test.tsx:
  - ProductCatalogTable rendering with formatted currency (tabular-nums)
  - CatalogFilterBar search debouncing & category selection
  - Form validation error display & button loading spinners
  - Soft-hide toggle and confirmation dialogs
✓ tests/keyboardAccessibilitySmokeTest.ts:
  - 100% label and aria-label compliance (25/25 form controls)
  - Table overflow region with role="region" & keyboard arrow scrolling
  - Modal focus trapping and Escape key restoration
```
