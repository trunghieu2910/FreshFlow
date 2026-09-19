import openpyxl

def update_status():
    file_path = 'd:/FreshFlow/plan/backlog-freshflow-mvp-12-tuan-updated.xlsx'
    try:
        wb = openpyxl.load_workbook(file_path)
        sheet = wb['Backlog 12 Tuan']
        for r in range(1, 200):
            if sheet.cell(row=r, column=1).value == 'FF-03-05-1':
                sheet.cell(row=r, column=16, value='Done')
                print(f'Row {r} (FF-03-05-1) Col 16 updated to Done')
                break
        wb.save(file_path)
        print('SUCCESS: Updated backlog-freshflow-mvp-12-tuan-updated.xlsx!')
    except PermissionError:
        print('File is currently opened in Excel. Please close it and rerun.')

if __name__ == '__main__':
    update_status()
