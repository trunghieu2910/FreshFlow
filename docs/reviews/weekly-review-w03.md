# FreshFlow Weekly Review & Retrospective — Week 03 (Sprint 3)

> **Tuần:** W03 — Sprint 3: React Web Nền tảng và Merchant Catalog  
> **Thời gian:** 2026-09-14 đến 2026-09-20 (Tổng kết ngày 25/09/2026)  
> **Nhiệm vụ:** `FF-03-07-1 — Ôn React/TypeScript và review code`  
> **Mục tiêu học:** Component boundary, Phân tách trách nhiệm (Separation of Concerns), Typing an toàn, Phản hồi UX trực quan và Khả năng tiếp cận (Accessibility - A11y).  

---

## Phần 1. Retrospective Sprint 3 (Merchant Catalog Web & Foundations)

### 1.1. Mục tiêu tuần đã đặt ra
Xây dựng nền tảng ứng dụng Web cho chủ quán (Merchant) với React, Vite, TypeScript, Tailwind CSS:
- Thiết lập routing, layout quản trị (Sidebar, Header, AppLayout) và context xác thực.
- Xây dựng hệ thống UI feedback: Toast notification đa kênh, ErrorBoundary chống crash/trắng trang và EmptyState với CTA kép.
- Phát triển module Merchant Catalog đầy đủ: Xem bảng danh sách, tìm kiếm có debounce, lọc kích cỡ, tạo món mới kèm biến thể động, chỉnh sửa theo PATCH semantics và ẩn mềm (Soft hide).
- Đảm bảo tính khả dụng và khả năng tiếp cận (Accessibility - A11y) theo chuẩn Vercel Web Interface Guidelines và WCAG 2.1 Level AA.
- Xây dựng bộ test tự động toàn diện với Vitest và React Testing Library (104 assertions).
- Chốt hợp đồng API xác thực dùng chung (Auth API Contract) và quy hoạch kiến trúc Android Mobile (Sprint 3 Day 6).

### 1.2. What Went Well (Những điểm làm tốt)
1. **Phân định ranh giới Component rõ ràng (Clear Component Boundaries):**
   - Các form nhập liệu phức tạp được tách thành Form thuần và Modal bọc ngoài, giúp tái sử dụng và kiểm thử độc lập mà không phụ thuộc vào dialog backdrop.
2. **Tuân thủ xuất sắc Vercel Web Interface Guidelines:**
   - 100% các ô nhập liệu có `<label>` hoặc `aria-label`.
   - Vùng bảng cuộn ngang trên mobile có `role="region"`, `tabIndex={0}` và `aria-label`, cho phép cuộn hoàn toàn bằng phím mũi tên.
   - Sử dụng font `tabular-nums font-mono` cho các cột tiền tệ và ngày giờ giúp so sánh số liệu thẳng cột.
   - Microcopy chuẩn mực: Toàn bộ trạng thái tải kết thúc bằng dấu chấm lửng Unicode `…` (`"Đang lưu…"`, `"Đang tải…"`).
3. **Trải nghiệm chịu lỗi cao (Resilient UX):**
   - [`ErrorBoundary`](file:///d:/FreshFlow/clients/freshflow-web/src/components/ui/ErrorBoundary.tsx) ngăn ngừa hoàn toàn tình trạng trắng trang khi xảy ra lỗi runtime trong component con.
   - [`Toast`](file:///d:/FreshFlow/clients/freshflow-web/src/components/ui/Toast.tsx) hệ thống hoạt động mượt mà với hàng đợi bất đồng bộ (`aria-live="polite"`).
4. **Kiểm thử tự động vững chắc:**
   - Đạt 100% tỷ lệ pass trên toàn bộ 104 assertions và 12 RTL tests, chạy sạch sẽ trong CI/CD.

### 1.3. What Could Be Improved & Danh sách Refactor Backlog
1. **Tách nhỏ Bundle Size:**
   - Khi chạy `npm run build`, bundle chính `index-DJDQVKgo.js` đạt ~599 kB (vượt mức cảnh báo 500 kB).
   - *Refactor Action:* Cần áp dụng Dynamic Import (`React.lazy`) cho các trang `/products`, `/orders`, `/settings` trong Sprint 4 để tách nhỏ vendor chunks.
2. **URL State Synchronization:**
   - Hiện tại bộ lọc trên `CatalogFilterBar` đang lưu ở component state cục bộ. Khi người dùng refresh trang (F5), các điều kiện lọc sẽ bị reset về mặc định.
   - *Refactor Action:* Sẽ chuyển đổi sang URL Query Parameters (`useSearchParams`) ở Task `FF-04-04-1`.
3. **Xác thực 0 TODO chưa ghi nhận:**
   - Toàn bộ codebase `clients/freshflow-web/src` đã được rà soát và không còn bất kỳ comment `TODO` nào chưa được đưa vào backlog chính thức.

---

## Phần 2. Đánh giá 5 Component Cốt lõi & Lý do Phân tách Ranh giới

### 1. `ProductCreateModal` và `ProductCreateForm`
* **Vấn đề trước khi tách:** Logic mở/đóng dialog, hiệu ứng trượt backdrop và logic validate form (tên món, danh mục, mảng động các biến thể) nằm chung trong một file hơn 900 dòng, gây khó khăn cho việc viết unit test form.
* **Quyết định phân tách:**
  - `ProductCreateModal`: Đóng vai trò Container điều phối hiển thị, quản lý focus trap, nút đóng và bắt phím `Escape`.
  - `ProductCreateForm`: Pure Form Component nhận props `onSubmit`, `onCancel`, `initialValues`.
* **Lợi ích:** Form có thể test độc lập bằng React Testing Library mà không cần mount Portal hay Modal backdrop.

### 2. `ProductCatalogTable` và Hàng hiển thị Sản phẩm
* **Vấn đề:** Bảng quản trị danh mục chứa nhiều cột phức tạp (thông tin món, danh sách chip kích cỡ, giá tiền, công suất ngày, nút sửa, nút đổi trạng thái). Khi một món cập nhật, toàn bộ bảng bị re-render.
* **Quyết định phân tách:**
  - Tách vùng cuộn `Table` thành scrollable container độc lập có `role="region"` và `tabIndex={0}`.
  - Tách cell biến thể và nhóm action thành các sub-components gọn gàng.
* **Lợi ích:** Tối ưu hóa render, mã nguồn dễ đọc và thỏa mãn tiêu chuẩn A11y của W3C.

### 3. `CatalogFilterBar`
* **Vấn đề:** Ô tìm kiếm văn bản gõ liên tục gây re-filter ngay lập tức, làm giật lag giao diện danh sách khi có hàng chục món.
* **Quyết định phân tách:**
  - Tích hợp hook [`useDebounce`](file:///d:/FreshFlow/clients/freshflow-web/src/hooks/useDebounce.ts) (300ms) riêng cho ô text input.
  - Tách các chip lọc kích cỡ (Size M, L, Standard) thành toggle button groups độc lập.
* **Lợi ích:** Giảm số lần re-render không cần thiết đi hơn 80% khi người dùng gõ tìm kiếm.

### 4. `ToastContainer` & `ToastContext`
* **Vấn đề:** Nếu để từng component tự quản lý state alert/toast của mình, giao diện sẽ bị phân mảnh và thông báo có thể bị đè lên nhau.
* **Quyết định phân tách:**
  - Sử dụng kiến trúc Event Emitter / Context Provider bọc tại gốc ứng dụng (`App.tsx`).
  - Cung cấp hook `useToast()` để mọi component mutation chỉ cần gọi `toast.success(...)` hoặc `toast.error(...)`.
* **Lợi ích:** Đảm bảo thứ tự hiển thị thông báo, tự động dọn dẹp timer và hỗ trợ `aria-live="polite"` tập trung cho người khiếm thị.

### 5. `ErrorBoundary`
* **Vấn đề:** Khi một component con gặp lỗi JavaScript trong quá trình render (ví dụ truy cập thuộc tính của `undefined`), cả cây React bị unmount dẫn đến màn hình trắng xóa (White Screen of Death).
* **Quyết định phân tách:**
  - Viết Class Component `ErrorBoundary` bắt lỗi qua `getDerivedStateFromError` và `componentDidCatch`.
  - Tách giao diện thông báo lỗi ra component [`ErrorState`](file:///d:/FreshFlow/clients/freshflow-web/src/components/ui/ErrorState.tsx) có CTA "Thử lại ngay" và "Tải lại trang".
* **Lợi ích:** Cô lập sự cố ở phạm vi cục bộ; các phần khác của dashboard (như Header, Sidebar) vẫn hoạt động bình thường.

---

## Phần 3. Câu hỏi Phỏng vấn Kỹ thuật Chuyên sâu (React & TypeScript)

### Câu 1: Tại sao nên ưu tiên Phân tích Trạng thái Dẫn xuất (Derived State) hơn là lạm dụng `useEffect` để đồng bộ state trong React?

Khi một giá trị có thể được tính toán trực tiếp từ `props` hoặc `state` hiện có (ví dụ: danh sách món ăn đã qua bộ lọc tìm kiếm và danh mục), ta nên tính toán trực tiếp giá trị đó trong quá trình render (hoặc dùng `useMemo` nếu phép tính nặng) thay vì lưu một state thứ hai và cập nhật nó qua `useEffect`.

Lý do:
1. **Tránh re-render thừa:** Cập nhật state trong `useEffect` luôn kích hoạt thêm một chu kỳ render thứ hai (Render lần 1 với state cũ $\rightarrow$ chạy effect $\rightarrow$ `setState` $\rightarrow$ Render lần 2 với state mới).
2. **Ngăn chặn lỗi bất đồng bộ trạng thái (State desynchronization):** Khi phụ thuộc vào `useEffect`, luôn tồn tại một tích tắc giao diện hiển thị dữ liệu không nhất quán giữa prop mới và state phái sinh cũ.
3. **Đơn giản hóa mã nguồn:** Loại bỏ mảng phụ thuộc (`dependency array`) dễ bị lỗi lặp vô tận (infinite loop).

---

### Câu 2: Sự khác biệt bản chất giữa `:focus` và `:focus-visible` trong CSS hiện đại và tại sao Web Interface Guidelines lại bắt buộc `:focus-visible`?

- `:focus` kích hoạt bất cứ khi nào một phần tử nhận tiêu điểm, bất kể người dùng tương tác bằng **chuột**, **chạm cảm ứng** hay **bàn phím**. Điều này dẫn đến việc người dùng nhấp chuột vào nút nhưng lại xuất hiện một viền đen/xanh gây mất thẩm mỹ giao diện. Vì lý do thẩm mỹ, nhiều lập trình viên đã thêm `outline: none`, vô tình tước đoạt hoàn toàn khả năng sử dụng của người khuyết tật dùng bàn phím.
- `:focus-visible` là bộ chọn thông minh (Heuristic selector). Nó chỉ hiển thị viền tiêu điểm khi người dùng tương tác bằng **bàn phím** (bấm phím `Tab`, phím mũi tên) hoặc khi ô nhập liệu (`input`, `textarea`) cần con trỏ soạn thảo.

**Quy chuẩn áp dụng:** Luôn sử dụng Tailwind class `focus-visible:ring-2 focus-visible:ring-emerald-600 focus-visible:outline-none`, giúp giao diện vừa sạch đẹp khi dùng chuột, vừa chuẩn mực tuyệt đối về Accessibility khi dùng bàn phím.
