# FreshFlow Android Mobile Client (`freshflow-mobile`)

> **Task:** `FF-03-07-2`  
> **Target Audience:** Customer & Driver Mobile App  
> **Platform:** Android 8.0+ (API Level 26+), 100% Kotlin, Jetpack Compose, Material 3  

---

## 1. Giới Thiệu

`freshflow-mobile` là ứng dụng di động chính thức của nền tảng FreshFlow, phục vụ hai đối tượng người dùng chính trên thiết bị di động:
1. **Khách hàng (Customer)**: Xem thực đơn món ăn, chọn kích cỡ biến thể (Size M, Size L, STANDARD), kiểm tra tình trạng công suất theo ngày (Capacity), quản lý giỏ hàng cục bộ (Room DB), thanh toán đơn hàng (COD / Chuyển khoản / Mock) và theo dõi tiến trình giao hàng theo thời gian thực kèm mã OTP.
2. **Tài xế (Driver)**: Bật/tắt trạng thái sẵn sàng nhận đơn (`is_available`), tiếp nhận các đơn hàng được gán, xác nhận thu tiền mặt COD, nhập mã OTP từ khách hàng và báo cáo sự cố giao hàng không thành công.

---

## 2. Kiến Trúc & Công Nghệ Nền Tảng

- **Ngôn ngữ**: Kotlin 1.9+
- **Giao diện (UI)**: Jetpack Compose + Material 3
- **Mô hình kiến trúc**: MVVM + Clean Architecture:
  - **Data Layer**: Retrofit2 (REST API `/api/v1`), OkHttp3 (Interceptors), Room Database (Cart offline), Jetpack DataStore (Token).
  - **Domain Layer**: Repositories, Use Cases (Cart validation, Auth, Order state machine).
  - **UI Layer**: Compose Screens, ViewModels với `StateFlow` và `SharedFlow`.
- **Đồng bộ Xác thực (Cross-Client Auth)**:
  - Tích hợp `AuthInterceptor`: Tự động gắn header `Authorization: Bearer <token>`.
  - Tích hợp `TokenAuthenticator`: Tự động gọi `/api/v1/auth/refresh` khi nhận `401 Unauthorized`.

---

## 3. Ma Trận Tài Khoản Demo (Demo Accounts)

Hệ thống hỗ trợ tính năng **1-Click Demo Login** ngay tại màn hình đăng nhập với 3 tài khoản mặc định:

| Vai trò | Email | Mật khẩu | Phân hệ trên App |
| :--- | :--- | :--- | :--- |
| **Merchant** (Chủ quán) | `merchant@freshflow.com` | `Password@123` | *Dành cho React Web* |
| **Customer** (Khách hàng) | `customer@freshflow.com` | `Password@123` | Khách mua hàng, giỏ hàng, checkout |
| **Driver** (Tài xế) | `driver@freshflow.com` | `Password@123` | Nhận đơn, xác nhận COD/OTP, giao hàng |

---

## 4. Sơ Đồ Điều Hướng Màn Hình (Screen Map)

Chi tiết sơ đồ điều hướng màn hình và đặc tả hợp đồng API:
👉 Xem tại tài liệu: [docs/architecture/auth-api-contract-and-mobile-plan.md](../../docs/architecture/auth-api-contract-and-mobile-plan.md).

### Sơ đồ luồng ứng dụng chính:
- **Khởi động**: `SplashScreen` $\rightarrow$ `LoginScreen` (hoặc tự động vào Home nếu Token còn hiệu lực).
- **Khách hàng**: `CatalogHomeScreen` $\rightarrow$ `ProductDetailScreen` $\rightarrow$ `CartScreen` $\rightarrow$ `CheckoutScreen` $\rightarrow$ `OrderDetailScreen` $\rightarrow$ `CreateDisputeScreen`.
- **Tài xế**: `DriverHomeScreen` (Switch Sẵn sàng) $\rightarrow$ `DeliveryDetailScreen` $\rightarrow$ `DeliveryConfirmationScreen` (Nhập OTP / Thu tiền COD) $\rightarrow$ `DeliveryFailureReportScreen`.

---

## 5. Cấu Trúc Mã Nguồn

```
clients/freshflow-mobile/
├── README.md
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           └── java/com/freshflow/mobile/
│               ├── FreshFlowApplication.kt
│               ├── core/
│               │   ├── network/ (Retrofit, OkHttp, AuthInterceptor)
│               │   ├── storage/ (DataStore, Room DB Cart)
│               │   └── ui/theme/ (Material 3 Theme)
│               ├── features/
│               │   ├── auth/ (LoginScreen, AuthRepository)
│               │   ├── customer/ (Catalog, Cart, Checkout, Order)
│               │   └── driver/ (DriverHome, DeliveryDetail, Confirm)
│               └── navigation/ (NavHost, Destinations)
```
