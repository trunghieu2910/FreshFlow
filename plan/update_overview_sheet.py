import openpyxl, sys
sys.stdout.reconfigure(encoding='utf-8')
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.drawing.image import Image
import os

def update_overview_sheet():
    wb_path = 'd:/FreshFlow/plan/backlog-freshflow-mvp-12-tuan-updated.xlsx'
    wb = openpyxl.load_workbook(wb_path)

    sheet_name = 'Overview'
    if sheet_name in wb.sheetnames:
        ws = wb[sheet_name]
        # Clear existing cells
        for row in ws.iter_rows():
            for cell in row:
                cell.value = None
    else:
        ws = wb.create_sheet(title=sheet_name, index=0)

    # Move Overview to first position if not already
    if wb.sheetnames[0] != sheet_name:
        idx = wb.sheetnames.index(sheet_name)
        wb._sheets.insert(0, wb._sheets.pop(idx))

    font_family = 'Calibri'
    
    # Styles
    title_font = Font(name=font_family, size=15, bold=True, color='FFFFFF')
    title_fill = PatternFill(start_color='1F4E79', end_color='1F4E79', fill_type='solid')

    section_font = Font(name=font_family, size=12, bold=True, color='FFFFFF')
    section_fill = PatternFill(start_color='203764', end_color='203764', fill_type='solid')

    table_header_font = Font(name=font_family, size=10, bold=True, color='FFFFFF')
    table_header_fill = PatternFill(start_color='17365D', end_color='17365D', fill_type='solid')

    bold_font = Font(name=font_family, size=10, bold=True)
    regular_font = Font(name=font_family, size=10)
    italic_note_font = Font(name=font_family, size=9, italic=True, color='595959')

    total_row_fill = PatternFill(start_color='D9E1F2', end_color='D9E1F2', fill_type='solid')
    total_row_font = Font(name=font_family, size=10, bold=True, color='1F4E79')

    thin_border_side = Side(border_style='thin', color='D9D9D9')
    table_border = Border(left=thin_border_side, right=thin_border_side, top=thin_border_side, bottom=thin_border_side)
    thick_bottom_border = Border(left=thin_border_side, right=thin_border_side, top=thin_border_side, bottom=Side(border_style='medium', color='1F4E79'))

    # Status badge styles
    status_row_styles = {
        'Done': PatternFill(start_color='E2EFDA', end_color='E2EFDA', fill_type='solid'),
        'Review': PatternFill(start_color='FCE4D6', end_color='FCE4D6', fill_type='solid'),
        'In Progress': PatternFill(start_color='FFF2CC', end_color='FFF2CC', fill_type='solid'),
        'Blocked': PatternFill(start_color='F8CECC', end_color='F8CECC', fill_type='solid'),
        'Deferred': PatternFill(start_color='F2F2F2', end_color='F2F2F2', fill_type='solid'),
        'Todo': PatternFill(start_color='FFFFFF', end_color='FFFFFF', fill_type='solid')
    }

    # 1. Main Title
    ws.merge_cells('A1:M1')
    cell_a1 = ws['A1']
    cell_a1.value = "FRESHFLOW MVP — TỔNG QUAN DỰ ÁN & TIẾN ĐỘ THEO CỤC (BACKEND-FIRST DASHBOARD)"
    cell_a1.font = title_font
    cell_a1.fill = title_fill
    cell_a1.alignment = Alignment(horizontal='center', vertical='center')
    ws.row_dimensions[1].height = 36

    ws.merge_cells('A2:M2')
    cell_a2 = ws['A2']
    cell_a2.value = "Chiến lược: Hoàn thành trọn vẹn toàn bộ Backend Core & APIs trước khi phát triển các Frontend (Mobile Kotlin & React Web). Dữ liệu Dashboard tự động cập nhật từ trang 'Kế hoạch theo cục'."
    cell_a2.font = italic_note_font
    cell_a2.alignment = Alignment(horizontal='center', vertical='center')
    ws.row_dimensions[2].height = 20

    # 2. Section 1: Tổng quan trạng thái toàn hệ thống
    ws.merge_cells('A4:M4')
    s1_cell = ws['A4']
    s1_cell.value = "❖ 1. TỔNG QUAN TRẠNG THÁI TOÀN BỘ DỰ ÁN (STATUS DISTRIBUTION)"
    s1_cell.font = section_font
    s1_cell.fill = section_fill
    s1_cell.alignment = Alignment(horizontal='left', vertical='center', indent=1)
    ws.row_dimensions[4].height = 26

    # Headers for Table 1
    t1_headers = [
        ('A5', 'Trạng thái (Status)', 20),
        ('B5', 'Số lượng Task', 16),
        ('C5', 'Tỷ lệ (%)', 14),
        ('D5', 'Ý nghĩa & Đánh giá tiến độ', 75)
    ]
    # Merge D5:M5
    ws.merge_cells('D5:M5')
    for pos, text, _ in t1_headers:
        c = ws[pos]
        c.value = text
        c.font = table_header_font
        c.fill = table_header_fill
        c.alignment = Alignment(horizontal='center', vertical='center')
        c.border = table_border
    ws.row_dimensions[5].height = 24

    # Table 1 rows (Rows 6 to 12)
    # Using dynamic formulas linking to 'Kế hoạch theo cục'!$P$3:$P$250
    t1_data = [
        ('Done', '=COUNTIF(\'Kế hoạch theo cục\'!$P$3:$P$250, "Done")', '=B6/$B$12', 'Hoàn thành Cục 1 (Tuần 1) và toàn bộ Catalog Backend v0.1 (Tuần 2)'),
        ('Review', '=COUNTIF(\'Kế hoạch theo cục\'!$P$3:$P$250, "Review")', '=B7/$B$12', 'Đã code xong & đang review: React Web Catalog (Tuần 3), React Orders Dashboard & Backend Order API v1 (Tuần 4)'),
        ('In Progress', '=COUNTIF(\'Kế hoạch theo cục\'!$P$3:$P$250, "In Progress")', '=B8/$B$12', 'Task FF-04-07-2: Chuẩn bị kiến trúc Android Customer app'),
        ('Blocked', '=COUNTIF(\'Kế hoạch theo cục\'!$P$3:$P$250, "Blocked")', '=B9/$B$12', 'Các tác vụ đang bị nghẽn do phụ thuộc'),
        ('Deferred', '=COUNTIF(\'Kế hoạch theo cục\'!$P$3:$P$250, "Deferred")', '=B10/$B$12', 'Các tác vụ tạm hoãn sang giai đoạn sau'),
        ('Todo', '=COUNTIF(\'Kế hoạch theo cục\'!$P$3:$P$250, "Todo")', '=B11/$B$12', 'Các task Backend nâng cao (Auth/RBAC, Concurrency, Driver/Payment), Mobile Kotlin, Web hoàn thiện, E2E & Release'),
    ]

    for idx, (st_name, count_fml, pct_fml, desc) in enumerate(t1_data, start=6):
        ws.merge_cells(f'D{idx}:M{idx}')
        
        c_status = ws[f'A{idx}']
        c_status.value = st_name
        c_status.font = bold_font
        c_status.alignment = Alignment(horizontal='center', vertical='center')
        c_status.fill = status_row_styles.get(st_name, PatternFill(fill_type=None))
        c_status.border = table_border

        c_count = ws[f'B{idx}']
        c_count.value = count_fml
        c_count.font = bold_font
        c_count.alignment = Alignment(horizontal='center', vertical='center')
        c_count.border = table_border

        c_pct = ws[f'C{idx}']
        c_pct.value = pct_fml
        c_pct.font = regular_font
        c_pct.number_format = '0.0%'
        c_pct.alignment = Alignment(horizontal='center', vertical='center')
        c_pct.border = table_border

        c_desc = ws[f'D{idx}']
        c_desc.value = desc
        c_desc.font = regular_font
        c_desc.alignment = Alignment(horizontal='left', vertical='center', indent=1)
        c_desc.border = table_border
        
        ws.row_dimensions[idx].height = 22

    # Row 12: Total Row for Table 1
    ws.merge_cells('D12:M12')
    c_tot_label = ws['A12']
    c_tot_label.value = "TỔNG CỘNG"
    c_tot_label.font = total_row_font
    c_tot_label.fill = total_row_fill
    c_tot_label.alignment = Alignment(horizontal='center', vertical='center')
    c_tot_label.border = thick_bottom_border

    c_tot_count = ws['B12']
    c_tot_count.value = "=SUM(B6:B11)"
    c_tot_count.font = total_row_font
    c_tot_count.fill = total_row_fill
    c_tot_count.alignment = Alignment(horizontal='center', vertical='center')
    c_tot_count.border = thick_bottom_border

    c_tot_pct = ws['C12']
    c_tot_pct.value = "=SUM(C6:C11)"
    c_tot_pct.font = total_row_font
    c_tot_pct.fill = total_row_fill
    c_tot_pct.number_format = '0.0%'
    c_tot_pct.alignment = Alignment(horizontal='center', vertical='center')
    c_tot_pct.border = thick_bottom_border

    c_tot_desc = ws['D12']
    c_tot_desc.value = "Toàn bộ 168/168 task được bảo toàn chính xác 100% trạng thái thực tế."
    c_tot_desc.font = total_row_font
    c_tot_desc.fill = total_row_fill
    c_tot_desc.alignment = Alignment(horizontal='left', vertical='center', indent=1)
    c_tot_desc.border = thick_bottom_border
    ws.row_dimensions[12].height = 24


    # 3. Section 2: Bảng Ma trận & Phần trăm các trạng thái theo từng Cục
    ws.merge_cells('A14:M14')
    s2_cell = ws['A14']
    s2_cell.value = "❖ 2. MA TRẬN TIẾN ĐỘ & TỶ LỆ PHẦN TRĂM CÁC TRẠNG THÁI THEO TỪNG CỤC (STATUS BY BLOCK MATRIX)"
    s2_cell.font = section_font
    s2_cell.fill = section_fill
    s2_cell.alignment = Alignment(horizontal='left', vertical='center', indent=1)
    ws.row_dimensions[14].height = 26

    # Headers for Table 2
    t2_headers = [
        ('A15', 'Cục', 10),
        ('B15', 'Tên Cục phát triển', 32),
        ('C15', 'Tổng Task', 12),
        ('D15', 'Done', 10),
        ('E15', 'Review', 10),
        ('F15', 'In Prog', 10),
        ('G15', 'Blocked', 10),
        ('H15', 'Todo', 10),
        ('I15', '% Done', 12),
        ('J15', '% Review', 12),
        ('K15', '% In Prog', 12),
        ('L15', '% Todo', 12),
        ('M15', 'Đánh giá tiến độ Cục', 42)
    ]

    for pos, text, _ in t2_headers:
        c = ws[pos]
        c.value = text
        c.font = table_header_font
        c.fill = table_header_fill
        c.alignment = Alignment(horizontal='center', vertical='center', wrap_text=True)
        c.border = table_border
    ws.row_dimensions[15].height = 26

    # Data for 6 blocks (Rows 16 to 21)
    # Using COUNTIF and COUNTIFS formulas linking to 'Kế hoạch theo cục'!$C:$C and $P:$P
    blocks_meta = [
        ('CỤC 1', 'Nền tảng hệ thống & Domain Architecture', 'Gần hoàn tất (85.7% Done), chỉ còn 2 task review/planning W1'),
        ('CỤC 2', 'Toàn bộ Backend & Hệ thống APIs', 'Trọng tâm số 1: Catalog đã Done, Order v1 đang Review, tiếp tục Auth/Inventory/Driver'),
        ('CỤC 3', 'Frontend - Mobile Client (Android Kotlin)', 'Đang khởi động: 1 task In Progress, sẵn sàng đón API hoàn thiện từ Cục 2'),
        ('CỤC 4', 'Frontend - Web Merchant (React TypeScript)', 'Tiến độ rất tốt: 73.3% tasks đang ở trạng thái Review (Catalog & Orders UI)'),
        ('CỤC 5', 'Tích hợp Đa nền tảng, DevOps & E2E Testing', 'Chờ hoàn thành Backend & Frontends'),
        ('CỤC 6', 'Đóng gói Sản phẩm, Demo & Hồ sơ Nghề nghiệp', 'Giai đoạn cuối dự án (v1.0.0, Demo Video & Portfolio)')
    ]

    for idx, (b_code, b_name, b_note) in enumerate(blocks_meta, start=16):
        ws[f'A{idx}'].value = b_code
        ws[f'A{idx}'].font = bold_font
        ws[f'A{idx}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'A{idx}'].border = table_border

        ws[f'B{idx}'].value = b_name
        ws[f'B{idx}'].font = bold_font
        ws[f'B{idx}'].alignment = Alignment(horizontal='left', vertical='center', indent=1)
        ws[f'B{idx}'].border = table_border

        # Total tasks in block
        ws[f'C{idx}'].value = f'=COUNTIF(\'Kế hoạch theo cục\'!$C$3:$C$250, "{b_code}")'
        ws[f'C{idx}'].font = bold_font
        ws[f'C{idx}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'C{idx}'].border = table_border

        # Done
        ws[f'D{idx}'].value = f'=COUNTIFS(\'Kế hoạch theo cục\'!$C$3:$C$250, "{b_code}", \'Kế hoạch theo cục\'!$P$3:$P$250, "Done")'
        ws[f'D{idx}'].font = bold_font
        ws[f'D{idx}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'D{idx}'].fill = PatternFill(start_color='E2EFDA', end_color='E2EFDA', fill_type='solid')
        ws[f'D{idx}'].border = table_border

        # Review
        ws[f'E{idx}'].value = f'=COUNTIFS(\'Kế hoạch theo cục\'!$C$3:$C$250, "{b_code}", \'Kế hoạch theo cục\'!$P$3:$P$250, "Review")'
        ws[f'E{idx}'].font = bold_font
        ws[f'E{idx}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'E{idx}'].fill = PatternFill(start_color='FCE4D6', end_color='FCE4D6', fill_type='solid')
        ws[f'E{idx}'].border = table_border

        # In Progress
        ws[f'F{idx}'].value = f'=COUNTIFS(\'Kế hoạch theo cục\'!$C$3:$C$250, "{b_code}", \'Kế hoạch theo cục\'!$P$3:$P$250, "In Progress")'
        ws[f'F{idx}'].font = bold_font
        ws[f'F{idx}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'F{idx}'].fill = PatternFill(start_color='FFF2CC', end_color='FFF2CC', fill_type='solid')
        ws[f'F{idx}'].border = table_border

        # Blocked / Deferred
        ws[f'G{idx}'].value = f'=COUNTIFS(\'Kế hoạch theo cục\'!$C$3:$C$250, "{b_code}", \'Kế hoạch theo cục\'!$P$3:$P$250, "Blocked") + COUNTIFS(\'Kế hoạch theo cục\'!$C$3:$C$250, "{b_code}", \'Kế hoạch theo cục\'!$P$3:$P$250, "Deferred")'
        ws[f'G{idx}'].font = regular_font
        ws[f'G{idx}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'G{idx}'].border = table_border

        # Todo
        ws[f'H{idx}'].value = f'=COUNTIFS(\'Kế hoạch theo cục\'!$C$3:$C$250, "{b_code}", \'Kế hoạch theo cục\'!$P$3:$P$250, "Todo")'
        ws[f'H{idx}'].font = regular_font
        ws[f'H{idx}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'H{idx}'].border = table_border

        # % Done
        ws[f'I{idx}'].value = f'=D{idx}/C{idx}'
        ws[f'I{idx}'].font = bold_font
        ws[f'I{idx}'].number_format = '0.0%'
        ws[f'I{idx}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'I{idx}'].fill = PatternFill(start_color='E2EFDA', end_color='E2EFDA', fill_type='solid')
        ws[f'I{idx}'].border = table_border

        # % Review
        ws[f'J{idx}'].value = f'=E{idx}/C{idx}'
        ws[f'J{idx}'].font = regular_font
        ws[f'J{idx}'].number_format = '0.0%'
        ws[f'J{idx}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'J{idx}'].fill = PatternFill(start_color='FCE4D6', end_color='FCE4D6', fill_type='solid')
        ws[f'J{idx}'].border = table_border

        # % In Prog
        ws[f'K{idx}'].value = f'=F{idx}/C{idx}'
        ws[f'K{idx}'].font = regular_font
        ws[f'K{idx}'].number_format = '0.0%'
        ws[f'K{idx}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'K{idx}'].fill = PatternFill(start_color='FFF2CC', end_color='FFF2CC', fill_type='solid')
        ws[f'K{idx}'].border = table_border

        # % Todo
        ws[f'L{idx}'].value = f'=H{idx}/C{idx}'
        ws[f'L{idx}'].font = regular_font
        ws[f'L{idx}'].number_format = '0.0%'
        ws[f'L{idx}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'L{idx}'].border = table_border

        # Note
        ws[f'M{idx}'].value = b_note
        ws[f'M{idx}'].font = regular_font
        ws[f'M{idx}'].alignment = Alignment(horizontal='left', vertical='center', indent=1)
        ws[f'M{idx}'].border = table_border

        ws.row_dimensions[idx].height = 22

    # Row 22: Total row for Table 2
    tot_row = 22
    ws[f'A{tot_row}'].value = "TỔNG"
    ws[f'A{tot_row}'].font = total_row_font
    ws[f'A{tot_row}'].fill = total_row_fill
    ws[f'A{tot_row}'].alignment = Alignment(horizontal='center', vertical='center')
    ws[f'A{tot_row}'].border = thick_bottom_border

    ws[f'B{tot_row}'].value = "Toàn bộ 6 Cục phát triển MVP"
    ws[f'B{tot_row}'].font = total_row_font
    ws[f'B{tot_row}'].fill = total_row_fill
    ws[f'B{tot_row}'].alignment = Alignment(horizontal='left', vertical='center', indent=1)
    ws[f'B{tot_row}'].border = thick_bottom_border

    ws[f'C{tot_row}'].value = "=SUM(C16:C21)"
    ws[f'C{tot_row}'].font = total_row_font
    ws[f'C{tot_row}'].fill = total_row_fill
    ws[f'C{tot_row}'].alignment = Alignment(horizontal='center', vertical='center')
    ws[f'C{tot_row}'].border = thick_bottom_border

    ws[f'D{tot_row}'].value = "=SUM(D16:D21)"
    ws[f'D{tot_row}'].font = total_row_font
    ws[f'D{tot_row}'].fill = total_row_fill
    ws[f'D{tot_row}'].alignment = Alignment(horizontal='center', vertical='center')
    ws[f'D{tot_row}'].border = thick_bottom_border

    ws[f'E{tot_row}'].value = "=SUM(E16:E21)"
    ws[f'E{tot_row}'].font = total_row_font
    ws[f'E{tot_row}'].fill = total_row_fill
    ws[f'E{tot_row}'].alignment = Alignment(horizontal='center', vertical='center')
    ws[f'E{tot_row}'].border = thick_bottom_border

    ws[f'F{tot_row}'].value = "=SUM(F16:F21)"
    ws[f'F{tot_row}'].font = total_row_font
    ws[f'F{tot_row}'].fill = total_row_fill
    ws[f'F{tot_row}'].alignment = Alignment(horizontal='center', vertical='center')
    ws[f'F{tot_row}'].border = thick_bottom_border

    ws[f'G{tot_row}'].value = "=SUM(G16:G21)"
    ws[f'G{tot_row}'].font = total_row_font
    ws[f'G{tot_row}'].fill = total_row_fill
    ws[f'G{tot_row}'].alignment = Alignment(horizontal='center', vertical='center')
    ws[f'G{tot_row}'].border = thick_bottom_border

    ws[f'H{tot_row}'].value = "=SUM(H16:H21)"
    ws[f'H{tot_row}'].font = total_row_font
    ws[f'H{tot_row}'].fill = total_row_fill
    ws[f'H{tot_row}'].alignment = Alignment(horizontal='center', vertical='center')
    ws[f'H{tot_row}'].border = thick_bottom_border

    ws[f'I{tot_row}'].value = f'=D{tot_row}/C{tot_row}'
    ws[f'I{tot_row}'].font = total_row_font
    ws[f'I{tot_row}'].fill = total_row_fill
    ws[f'I{tot_row}'].number_format = '0.0%'
    ws[f'I{tot_row}'].alignment = Alignment(horizontal='center', vertical='center')
    ws[f'I{tot_row}'].border = thick_bottom_border

    ws[f'J{tot_row}'].value = f'=E{tot_row}/C{tot_row}'
    ws[f'J{tot_row}'].font = total_row_font
    ws[f'J{tot_row}'].fill = total_row_fill
    ws[f'J{tot_row}'].number_format = '0.0%'
    ws[f'J{tot_row}'].alignment = Alignment(horizontal='center', vertical='center')
    ws[f'J{tot_row}'].border = thick_bottom_border

    ws[f'K{tot_row}'].value = f'=F{tot_row}/C{tot_row}'
    ws[f'K{tot_row}'].font = total_row_font
    ws[f'K{tot_row}'].fill = total_row_fill
    ws[f'K{tot_row}'].number_format = '0.0%'
    ws[f'K{tot_row}'].alignment = Alignment(horizontal='center', vertical='center')
    ws[f'K{tot_row}'].border = thick_bottom_border

    ws[f'L{tot_row}'].value = f'=H{tot_row}/C{tot_row}'
    ws[f'L{tot_row}'].font = total_row_font
    ws[f'L{tot_row}'].fill = total_row_fill
    ws[f'L{tot_row}'].number_format = '0.0%'
    ws[f'L{tot_row}'].alignment = Alignment(horizontal='center', vertical='center')
    ws[f'L{tot_row}'].border = thick_bottom_border

    ws[f'M{tot_row}'].value = "Tỷ lệ tổng thể cập nhật tự động"
    ws[f'M{tot_row}'].font = total_row_font
    ws[f'M{tot_row}'].fill = total_row_fill
    ws[f'M{tot_row}'].alignment = Alignment(horizontal='left', vertical='center', indent=1)
    ws[f'M{tot_row}'].border = thick_bottom_border
    ws.row_dimensions[tot_row].height = 24


    # 4. Section 3: Sơ đồ cấu trúc 6 Cục phát triển (Chèn ảnh vào sheet)
    img_row = 24
    ws.merge_cells(f'A{img_row}:M{img_row}')
    s3_cell = ws[f'A{img_row}']
    s3_cell.value = "❖ 3. SƠ ĐỒ CẤU TRÚC 6 CỤC PHÁT TRIỂN (BACKEND-FIRST ARCHITECTURE FLOWCHART)"
    s3_cell.font = section_font
    s3_cell.fill = section_fill
    s3_cell.alignment = Alignment(horizontal='left', vertical='center', indent=1)
    ws.row_dimensions[img_row].height = 26

    # Insert Image
    img_path = 'd:/FreshFlow/plan/assets/so-do-6-cuc-phat-trien.png'
    if os.path.exists(img_path):
        img = Image(img_path)
        img.width = 960
        img.height = 580
        ws.add_image(img, f'A{img_row + 2}')
        print("Image added to Overview sheet at A26!")

    # Set row heights for image area (approx rows 26 to 55)
    for r in range(img_row + 1, img_row + 32):
        ws.row_dimensions[r].height = 20

    # 5. Section 4: Tóm tắt chi tiết 6 Cục phát triển
    detail_row = img_row + 32
    ws.merge_cells(f'A{detail_row}:M{detail_row}')
    s4_cell = ws[f'A{detail_row}']
    s4_cell.value = "❖ 4. TÓM TẮT CHI TIẾT NỘI DUNG & PHÂN NHÓM 6 CỤC PHÁT TRIỂN"
    s4_cell.font = section_font
    s4_cell.fill = section_fill
    s4_cell.alignment = Alignment(horizontal='left', vertical='center', indent=1)
    ws.row_dimensions[detail_row].height = 26

    # Detail Headers
    detail_headers = [
        (f'A{detail_row + 1}', 'Cục', 10),
        (f'B{detail_row + 1}', 'Phân nhóm chức năng', 32),
        (f'C{detail_row + 1}', 'Số Task', 12),
        (f'D{detail_row + 1}', 'Nội dung kỹ thuật chính & Đầu ra nghiệm thu', 75)
    ]
    ws.merge_cells(f'D{detail_row + 1}:M{detail_row + 1}')
    for pos, text, _ in detail_headers:
        c = ws[pos]
        c.value = text
        c.font = table_header_font
        c.fill = table_header_fill
        c.alignment = Alignment(horizontal='center', vertical='center')
        c.border = table_border
    ws.row_dimensions[detail_row + 1].height = 24

    details_data = [
        # CỤC 1
        ('CỤC 1', '1.1. Phạm vi MVP & Domain State Machine', 4, 'Chốt 3 actor (Customer, Merchant, Driver), 8 user stories, Order State Machine (9 trạng thái), Value Objects tiền tệ Money/BigDecimal.'),
        ('CỤC 1', '1.2. Backend Skeleton & Docker Database', 4, 'Spring Boot skeleton port 8080, Docker Compose PostgreSQL 16 freshflow_dev, package boundary, JUnit 5/Spotless.'),
        ('CỤC 1', '1.3. ERD Quan hệ & Quản trị Rủi ro', 6, 'ERD 23 bảng vật lý, khóa chính/ngoại, issue/PR templates, demo môi trường, risk register v1.'),

        # CỤC 2
        ('CỤC 2', '2.1. Catalog & Store Module', 14, 'Entities Store/Category/Product/ProductVariant, Flyway V1-V5, DTO mappers, CRUD API, phân trang/lọc/tìm kiếm, B-Tree index, 151 test cases, release v0.1.'),
        ('CỤC 2', '2.2. User, Store Ownership & Auth/RBAC', 10, 'Spring Security 6, JWT Access/Refresh tokens, RBAC roles (CUSTOMER, MERCHANT, DRIVER), cô lập dữ liệu cửa hàng đa người thuê, security test.'),
        ('CỤC 2', '2.3. Customer Public Catalog Endpoint', 1, 'Endpoint công khai cho khách hàng duyệt món, lọc theo kích cỡ biến thể và kiểm tra công suất theo ngày.'),
        ('CỤC 2', '2.4. Order Management & State Machine', 7, 'Bảng orders/order_items, API tạo đơn, đọc đơn, chi tiết lịch sử và API chuyển trạng thái (Accept, Reject, Cancel, Prepare, Dispatch).'),
        ('CỤC 2', '2.5. Inventory, Daily Capacity & Idempotency', 12, 'Khóa bi quan (Pessimistic Locking), Idempotency-Key chống trùng lặp, trừ kho/công suất thời gian thực, bảng kiểm toán tồn kho, test race condition.'),
        ('CỤC 2', '2.6. Payment Mock & Driver Delivery APIs', 7, 'Payment Mock (ONLINE_MOCK, COD, BANK_TRANSFER), thuật toán gán đơn tài xế, xác thực OTP/PIN, cơ chế hoàn tiền bồi hoàn (Compensation).'),
        ('CỤC 2', '2.7. Optimization & Technical Hardening', 7, 'Testcontainers với PostgreSQL thật, tối ưu chỉ mục functional index, bộ API smoke test scripts, chốt hợp đồng OpenAPI 3.0 / Swagger UI.'),

        # CỤC 3
        ('CỤC 3', '3.1. Nền tảng Android App & MVVM', 4, 'Jetpack Compose, Material 3, Navigation graph, Kotlin domain models & UI state, Clean Architecture MVVM.'),
        ('CỤC 3', '3.2. Customer Catalog Browsing & Detail', 10, 'Duyệt danh sách quán, món ăn, Retrofit API client, chọn size M/L/STANDARD, hiển thị tình trạng hết suất (Capacity).'),
        ('CỤC 3', '3.3. Offline Cart Persistence (Room DB)', 3, 'Giỏ hàng lưu cục bộ bằng Room DB, quy tắc giỏ hàng đơn quán (Single-store policy), CartRepository.'),
        ('CỤC 3', '3.4. Mobile Authentication & DataStore', 3, 'Màn hình đăng nhập, lưu JWT vào DataStore, AuthInterceptor & TokenAuthenticator tự động refresh token.'),
        ('CỤC 3', '3.5. Customer Checkout & Order Tracking', 6, 'Màn hình checkout, chọn hình thức thanh toán, vô hiệu hóa giỏ khi hết công suất, theo dõi đơn thời gian thực.'),
        ('CỤC 3', '3.6. Mobile Driver Client Workflow', 3, 'App tài xế: chuyển đổi sẵn sàng nhận đơn, danh sách đơn gán, xác nhận tiền mặt COD, nhập OTP / báo lỗi giao hàng.'),
        ('CỤC 3', '3.7. Mobile Quality & Lifecycle Polish', 3, 'Unit test ViewModel/Repository, kiểm thử xoay màn hình, gián đoạn mạng và tối ưu UX mượt mà.'),

        # CỤC 4
        ('CỤC 4', '4.1. Web Setup & AppShell Layout', 4, 'Vite, React 19, TypeScript, TailwindCSS, AppShell, Router, typed API client.'),
        ('CỤC 4', '4.2. Merchant Catalog Management', 10, 'Quản lý món ăn, phân trang, lọc, form tạo/sửa món kèm biến thể, sức chứa và auto-accept, kiểm thử Vitest.'),
        ('CỤC 4', '4.3. Merchant Order Dashboard & Actions', 8, 'Bảng điều khiển số liệu thật, danh sách đơn URL sync, modal chi tiết đơn, hành động nhận đơn/từ chối/chuẩn bị/giao hàng.'),
        ('CỤC 4', '4.4. Web Customer Preview & Auth Guard', 3, 'Giao diện xem trước cho khách hàng, AuthContext, route guards bảo vệ và Axios interceptor.'),
        ('CỤC 4', '4.5. Inventory Adjustment & Order Workflow', 2, 'Điều chỉnh nhanh giới hạn suất bán theo ngày và xử lý khiếu nại đơn hàng trên Web.'),
        ('CỤC 4', '4.6. Accessibility & UX Polish', 3, 'Chuẩn WCAG AA color contrast, điều hướng bàn phím phím tắt, tối ưu loading và error handling.'),

        # CỤC 5
        ('CỤC 5', '5.1. Tích hợp Xác thực 3-Client', 3, 'Demo liên thông xác thực giữa Backend, React Web và Mobile App; lập kế hoạch deployment.'),
        ('CỤC 5', '5.2. DevOps & Docker Compose Full Stack', 3, 'Dockerfile backend & frontend, Docker Compose chạy toàn bộ ứng dụng cục bộ, sơ đồ kiến trúc hệ thống.'),
        ('CỤC 5', '5.3. Bảo mật, Hardening & Scope Freeze', 6, 'Hardening cấu hình, dependency audit, clean-machine deployment rehearsal, đóng gói MVP v0.8.'),
        ('CỤC 5', '5.4. Full Regression Demo & Dọn dẹp Code', 2, 'Diễn tập kiểm thử hồi quy toàn diện, rà soát mã nguồn lần cuối.'),

        # CỤC 6
        ('CỤC 6', '6.1. Media, Video Demo & Portfolio v1', 4, 'Chụp screenshot, quay video demo 3 client, viết CV bullets và project summary.'),
        ('CỤC 6', '6.2. Sửa lỗi cuối cùng & Gắn tag Release', 4, 'Fix release blockers, hoàn thiện README chính, chạy automated checks, gắn Git tag v1.0.0.'),
        ('CỤC 6', '6.3. Kịch bản Demo 10-15 phút & Dry Run', 2, 'Kịch bản demo thực tế cho cả 3 client và 3 hình thức thanh toán, chạy thử nghiệm trơn tru.'),
        ('CỤC 6', '6.4. GitHub Presentation & Case Study', 4, 'Trang GitHub presentation chuyên nghiệp, bài viết case study kiến trúc kỹ thuật sâu, tối ưu LinkedIn profile.'),
        ('CỤC 6', '6.5. Retrospective & Bắt đầu Ứng tuyển', 4, 'Tổng kết bài học kinh nghiệm, phân tích kiến thức thu hoạch, đặt lịch ứng tuyển kỹ sư.')
    ]

    cur_r = detail_row + 2
    for b_code, sub_title, num_tasks, tech_desc in details_data:
        ws.merge_cells(f'D{cur_r}:M{cur_r}')
        ws[f'A{cur_r}'].value = b_code
        ws[f'A{cur_r}'].font = bold_font
        ws[f'A{cur_r}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'A{cur_r}'].border = table_border

        ws[f'B{cur_r}'].value = sub_title
        ws[f'B{cur_r}'].font = bold_font
        ws[f'B{cur_r}'].alignment = Alignment(horizontal='left', vertical='center', indent=1)
        ws[f'B{cur_r}'].border = table_border

        ws[f'C{cur_r}'].value = num_tasks
        ws[f'C{cur_r}'].font = regular_font
        ws[f'C{cur_r}'].alignment = Alignment(horizontal='center', vertical='center')
        ws[f'C{cur_r}'].border = table_border

        ws[f'D{cur_r}'].value = tech_desc
        ws[f'D{cur_r}'].font = regular_font
        ws[f'D{cur_r}'].alignment = Alignment(horizontal='left', vertical='center', wrap_text=True, indent=1)
        ws[f'D{cur_r}'].border = table_border

        ws.row_dimensions[cur_r].height = 24
        cur_r += 1

    # Column widths
    col_widths = {
        'A': 12,  # Cục
        'B': 32,  # Tên / Phân nhóm
        'C': 14,  # Số task / Tổng task
        'D': 12,  # Done
        'E': 12,  # Review
        'F': 12,  # In Prog
        'G': 12,  # Blocked
        'H': 12,  # Todo
        'I': 14,  # % Done
        'J': 14,  # % Review
        'K': 14,  # % In Prog
        'L': 14,  # % Todo
        'M': 45   # Đánh giá tiến độ
    }
    for col_letter, width in col_widths.items():
        ws.column_dimensions[col_letter].width = width

    # Save
    wb.save(wb_path)
    print(f"SUCCESS: Overview sheet updated in {wb_path} with dynamic formulas, status table, block matrix and image!")

if __name__ == '__main__':
    update_overview_sheet()
