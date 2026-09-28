import openpyxl, sys
sys.stdout.reconfigure(encoding='utf-8')

def generate_markdown_plan():
    wb_path = 'd:/FreshFlow/plan/backlog-freshflow-mvp-12-tuan-updated.xlsx'
    wb = openpyxl.load_workbook(wb_path, data_only=True)
    ws = wb['Kế hoạch theo cục']

    md_lines = []
    md_lines.append("# Kế Hoạch Phát Triển FreshFlow MVP Theo Cục (Backend-First Strategy)\n\n")
    md_lines.append("> **Chiến lược tổ chức mới:** Hoàn thiện trọn vẹn toàn bộ Backend Core, Database, State Machine và REST APIs trước khi phát triển các Frontend Clients (Mobile Kotlin và React Web), giúp cố định API Contract, tránh phụ thuộc mock và tối ưu năng suất kiểm thử.\n")
    md_lines.append("> **Cam kết tính toàn vẹn:** Giữ nguyên 100% Task ID, nội dung yêu cầu, ước tính giờ, tiêu chí nghiệm thu và **Trạng thái thực tế (Status)** của toàn bộ 168 task.\n\n")

    md_lines.append("## 1. Tổng quan Trạng thái Hệ thống (Dynamic Status Distribution)\n\n")
    md_lines.append("*Bảng này trên trang tính **Overview** trong file Excel được liên kết bằng công thức động (`COUNTIF`), tự động nhảy số khi cập nhật trạng thái tại trang **Kế hoạch theo cục**.*\n\n")
    md_lines.append("| Trạng thái (Status) | Số lượng Task | Tỷ lệ (%) | Ghi chú & Đánh giá tiến độ |\n")
    md_lines.append("| :--- | :---: | :---: | :--- |\n")
    md_lines.append("| 🟢 **Done** | **26** | 15.5% | Hoàn thành Cục 1 (Tuần 1) và toàn bộ Catalog Backend v0.1 (Tuần 2) |\n")
    md_lines.append("| 🟣 **Review** | **27** | 16.1% | Đã code xong & đang review: React Web Catalog (Tuần 3), React Orders Dashboard & Backend Order API v1 (Tuần 4) |\n")
    md_lines.append("| 🟡 **In Progress** | **1** | 0.6% | `FF-04-07-2`: Chuẩn bị kiến trúc Android customer app |\n")
    md_lines.append("| 🔴 **Blocked** | **0** | 0.0% | Chưa có tác vụ nào bị chặn |\n")
    md_lines.append("| ⚪ **Deferred** | **0** | 0.0% | Chưa có tác vụ nào bị hoãn |\n")
    md_lines.append("| ⚪ **Todo** | **114** | 67.8% | Các task Backend nâng cao, Mobile Kotlin, Web hoàn thiện, E2E và Release |\n")
    md_lines.append("| **TỔNG CỘNG** | **168** | **100.0%** | **Đầy đủ 168/168 task từ kế hoạch 12 tuần gốc** |\n\n")

    md_lines.append("## 2. Ma trận Tiến độ & Tỷ lệ Phần trăm theo từng Cục (Status Matrix by Block)\n\n")
    md_lines.append("*Bảng công thức đa điều kiện (`COUNTIFS`) tính tỷ lệ hoàn thành, đang review, đang làm và chưa làm riêng cho từng Cục:*\n\n")
    md_lines.append("| Cục | Tên Cục phát triển | Tổng Task | Done | Review | In Prog | Todo | % Done | % Review | % In Prog | % Todo | Đánh giá tiến độ Cục |\n")
    md_lines.append("| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |\n")
    md_lines.append("| **CỤC 1** | Nền tảng hệ thống & Domain Architecture | 14 | 12 | 0 | 0 | 2 | **85.7%** | 0.0% | 0.0% | 14.3% | Gần hoàn tất, chỉ còn 2 task review/planning W1 |\n")
    md_lines.append("| **CỤC 2** | Toàn bộ Backend & Hệ thống APIs | 59 | 14 | 5 | 0 | 40 | **23.7%** | 8.5% | 0.0% | 67.8% | Trọng tâm số 1: Catalog Done, Order v1 Review, tiếp tục Auth/Inventory/Driver |\n")
    md_lines.append("| **CỤC 3** | Frontend - Mobile Client (Android Kotlin) | 33 | 0 | 0 | 1 | 32 | **0.0%** | 0.0% | 3.0% | 97.0% | Đang khởi động kiến trúc Customer & Driver app |\n")
    md_lines.append("| **CỤC 4** | Frontend - Web Merchant (React TypeScript) | 30 | 0 | 22 | 0 | 8 | **0.0%** | 73.3% | 0.0% | 26.7% | Tiến độ rất tốt: 73.3% tasks đang ở trạng thái Review (Catalog & Orders UI) |\n")
    md_lines.append("| **CỤC 5** | Tích hợp Đa nền tảng, DevOps & E2E Testing | 14 | 0 | 0 | 0 | 14 | **0.0%** | 0.0% | 0.0% | 100.0% | Chờ hoàn thành Backend & Frontends |\n")
    md_lines.append("| **CỤC 6** | Đóng gói Sản phẩm, Demo & Hồ sơ Nghề nghiệp | 18 | 0 | 0 | 0 | 18 | **0.0%** | 0.0% | 0.0% | 100.0% | Giai đoạn cuối dự án (v1.0.0, Demo Video & Portfolio) |\n")
    md_lines.append("| **TỔNG** | **Toàn bộ 6 Cục phát triển MVP** | **168** | **26** | **27** | **1** | **114** | **15.5%** | **16.1%** | **0.6%** | **67.8%** | **Theo dõi tiến độ tự động toàn hệ thống** |\n\n")

    md_lines.append("## 3. Sơ đồ Cấu trúc 6 Cục Phát triển (Backend-First Flowchart)\n\n")
    md_lines.append("![Sơ đồ 6 Cục Phát Triển](assets/so-do-6-cuc-phat-trien.png)\n\n")
    md_lines.append("```mermaid\n")
    md_lines.append("flowchart TD\n")
    md_lines.append("    C1[\"CỤC 1: Nền tảng hệ thống & Domain<br/>(14 tasks — 12 Done, 2 Todo)\"] --> C2[\"CỤC 2: Toàn bộ Backend & Hệ thống APIs<br/>(59 tasks — 14 Done, 5 Review, 40 Todo)\"]\n")
    md_lines.append("    \n")
    md_lines.append("    subgraph BACKEND_FIRST [\"Chiến lược Backend-First: Cố định API & DB Contract\"]\n")
    md_lines.append("        C2\n")
    md_lines.append("    end\n")
    md_lines.append("    \n")
    md_lines.append("    C2 --> C3[\"CỤC 3: Frontend Mobile App (Android Kotlin)<br/>(33 tasks — 1 In Progress, 32 Todo)\"]\n")
    md_lines.append("    C2 --> C4[\"CỤC 4: Frontend Web Merchant (React TypeScript)<br/>(30 tasks — 22 Review, 8 Todo)\"]\n")
    md_lines.append("    \n")
    md_lines.append("    C3 --> C5[\"CỤC 5: Tích hợp Đa nền tảng, DevOps & E2E<br/>(14 tasks — 14 Todo)\"]\n")
    md_lines.append("    C4 --> C5\n")
    md_lines.append("    \n")
    md_lines.append("    C5 --> C6[\"CỤC 6: Đóng gói Release v1.0, Demo & Portfolio<br/>(18 tasks — 18 Todo)\"]\n\n")
    md_lines.append("    style BACKEND_FIRST fill:#e1f5fe,stroke:#0288d1,stroke-width:2px\n")
    md_lines.append("    style C1 fill:#dcedc8,stroke:#689f38\n")
    md_lines.append("    style C2 fill:#bbdefb,stroke:#1976d2\n")
    md_lines.append("    style C3 fill:#ffe0b2,stroke:#f57c00\n")
    md_lines.append("    style C4 fill:#f8bbd0,stroke:#c2185b\n")
    md_lines.append("    style C5 fill:#d1c4e9,stroke:#512da8\n")
    md_lines.append("    style C6 fill:#cfd8dc,stroke:#455a64\n")
    md_lines.append("```\n\n")

    md_lines.append("---\n\n")
    md_lines.append("## 4. Danh Sách Chi Tiết Toàn Bộ 168 Task Theo Cục\n\n")

    current_block = ""
    current_subblock = ""

    for r in range(3, ws.max_row + 1):
        cell_val = ws.cell(row=r, column=1).value
        if cell_val is None:
            continue

        str_val = str(cell_val).strip()

        # Block banner
        if str_val.startswith("❖"):
            current_block = str_val.replace("❖", "").strip()
            md_lines.append(f"### {current_block}\n\n")
            continue

        # Subblock banner
        if str_val.startswith("▶"):
            current_subblock = str_val.replace("▶", "").strip()
            md_lines.append(f"#### {current_subblock}\n\n")
            md_lines.append("| STT | Task ID | Tuần gốc | Track | Task cần làm | Ưu tiên | Giờ | Trạng thái | Ghi chú & Rationale |\n")
            md_lines.append("| :---: | :---: | :---: | :---: | :--- | :---: | :---: | :---: | :--- |\n")
            continue

        # Task row
        if isinstance(cell_val, int):
            seq = cell_val
            tid = ws.cell(row=r, column=2).value
            orig_w = ws.cell(row=r, column=5).value
            track = ws.cell(row=r, column=6).value
            name = ws.cell(row=r, column=7).value
            prio = ws.cell(row=r, column=14).value
            est = ws.cell(row=r, column=13).value
            st = ws.cell(row=r, column=16).value
            note = ws.cell(row=r, column=18).value or ""

            # Format status badge
            badge = st
            if st == "Done":
                badge = "🟢 **Done**"
            elif st == "Review":
                badge = "🟣 **Review**"
            elif st == "In Progress":
                badge = "🟡 **In Progress**"
            elif st == "Todo":
                badge = "⚪ Todo"

            clean_name = str(name).replace("|", "\\|")
            clean_note = str(note).replace("|", "\\|")
            md_lines.append(f"| {seq} | `{tid}` | {orig_w} | `{track}` | {clean_name} | {prio} | {est}h | {badge} | {clean_note} |\n")

    output_path = 'd:/FreshFlow/plan/ke-hoach-theo-cuc.md'
    with open(output_path, 'w', encoding='utf-8') as f:
        f.writelines(md_lines)

    print(f"SUCCESS: Generated {output_path} with {len(md_lines)} lines!")

if __name__ == '__main__':
    generate_markdown_plan()
