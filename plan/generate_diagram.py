import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as patches

def create_flowchart_image(output_path='d:/FreshFlow/plan/assets/so-do-6-cuc-phat-trien.png'):
    import os
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    fig, ax = plt.subplots(figsize=(13, 8), dpi=300)
    ax.set_facecolor('#F8FAFC')
    fig.patch.set_facecolor('#F8FAFC')

    # Coordinates: (center_x, center_y, width, height)
    boxes = {
        'C1': {
            'xy': (0.5, 0.90), 'w': 0.72, 'h': 0.11,
            'title': 'CỤC 1: NỀN TẢNG HỆ THỐNG & MÔ HÌNH HÓA DOMAIN',
            'sub': '14 tasks  |  12 Done (85.7%)  |  2 Todo (14.3%)\nMonorepo, Domain Models, State Machine, PostgreSQL Docker, ERD v1',
            'bg': '#E8F5E9', 'border': '#2E7D32', 'title_color': '#1B5E20'
        },
        'C2': {
            'xy': (0.5, 0.70), 'w': 0.82, 'h': 0.14,
            'title': 'CỤC 2: TOÀN BỘ BACKEND & HỆ THỐNG APIS  [CHIẾN LƯỢC BACKEND-FIRST]',
            'sub': '59 tasks  |  14 Done  |  5 Review  |  40 Todo\nCatalog CRUD, Auth/JWT/RBAC, Order State Machine, Inventory Concurrency, Payment Mock, Driver APIs\n★ Hoàn thiện 100% Backend & API Contracts trước khi phát triển các Frontend Clients ★',
            'bg': '#E3F2FD', 'border': '#1565C0', 'title_color': '#0D47A1'
        },
        'C3': {
            'xy': (0.26, 0.46), 'w': 0.44, 'h': 0.13,
            'title': 'CỤC 3: FRONTEND — MOBILE (ANDROID KOTLIN)',
            'sub': '33 tasks  |  1 In Progress  |  32 Todo\nCustomer: Room Cart, Checkout, Order Tracking\nDriver: Sẵn sàng, Nhận đơn, OTP/COD, Báo lỗi\nJetpack Compose, MVVM Clean Architecture',
            'bg': '#FFF3E0', 'border': '#E65100', 'title_color': '#BF360C'
        },
        'C4': {
            'xy': (0.74, 0.46), 'w': 0.44, 'h': 0.13,
            'title': 'CỤC 4: FRONTEND — WEB MERCHANT (REACT TS)',
            'sub': '30 tasks  |  22 Review (73.3%)  |  8 Todo\nMerchant Portal: Catalog CRUD & Variants\nOrders Dashboard thời gian thực & Operations\nVite, React 19, TailwindCSS, Vitest',
            'bg': '#FCE4EC', 'border': '#C2185B', 'title_color': '#880E4F'
        },
        'C5': {
            'xy': (0.5, 0.25), 'w': 0.72, 'h': 0.11,
            'title': 'CỤC 5: TÍCH HỢP ĐA NỀN TẢNG, DEVOPS & E2E TESTING',
            'sub': '14 tasks  |  14 Todo\nHợp nhất 3-Client (API + Web + Mobile), Docker Compose full stack, E2E regression',
            'bg': '#EDE7F6', 'border': '#512DA8', 'title_color': '#311B92'
        },
        'C6': {
            'xy': (0.5, 0.08), 'w': 0.72, 'h': 0.10,
            'title': 'CỤC 6: ĐÓNG GÓI SẢN PHẨM, DEMO & HỒ SƠ NGHỀ NGHIỆP',
            'sub': '18 tasks  |  18 Todo\nRelease v1.0.0, Video Demo 10-15m, Technical Case Study, GitHub Presentation, CV & Phỏng vấn',
            'bg': '#ECEFF1', 'border': '#455A64', 'title_color': '#263238'
        }
    }

    # Draw boxes
    for k, v in boxes.items():
        cx, cy = v['xy']
        w, h = v['w'], v['h']
        x = cx - w / 2
        y = cy - h / 2
        
        # Shadow
        shadow = patches.FancyBboxPatch((x + 0.005, y - 0.005), w, h,
                                       boxstyle="round,pad=0.015,rounding_size=0.02",
                                       facecolor='#CBD5E1', edgecolor='none', alpha=0.4, zorder=1)
        ax.add_patch(shadow)

        # Main box
        rect = patches.FancyBboxPatch((x, y), w, h,
                                      boxstyle="round,pad=0.015,rounding_size=0.02",
                                      facecolor=v['bg'], edgecolor=v['border'], linewidth=2.2, zorder=2)
        ax.add_patch(rect)

        # Title
        ax.text(cx, cy + h * 0.22, v['title'], ha='center', va='center',
                fontsize=11.5, fontweight='bold', color=v['title_color'], zorder=3)
        # Subtext
        ax.text(cx, cy - h * 0.18, v['sub'], ha='center', va='center',
                fontsize=8.5, color='#334155', linespacing=1.35, zorder=3)

    # Draw Arrows
    def draw_arrow(x1, y1, x2, y2, color='#334155'):
        ax.annotate('', xy=(x2, y2), xytext=(x1, y1),
                    arrowprops=dict(facecolor=color, edgecolor=color, width=2.0, headwidth=8, headlength=8, shrink=0.04),
                    zorder=4)

    # C1 -> C2
    draw_arrow(0.5, 0.84, 0.5, 0.77, '#2E7D32')
    
    # C2 -> C3
    draw_arrow(0.38, 0.63, 0.28, 0.53, '#1565C0')
    
    # C2 -> C4
    draw_arrow(0.62, 0.63, 0.72, 0.53, '#1565C0')

    # C3 -> C5
    draw_arrow(0.28, 0.39, 0.38, 0.31, '#E65100')

    # C4 -> C5
    draw_arrow(0.72, 0.39, 0.62, 0.31, '#C2185B')

    # C5 -> C6
    draw_arrow(0.5, 0.19, 0.5, 0.13, '#512DA8')

    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis('off')

    plt.tight_layout()
    plt.savefig(output_path, dpi=300, bbox_inches='tight', facecolor=fig.get_facecolor(), edgecolor='none')
    plt.close()
    print(f"Diagram successfully generated at: {output_path}")

if __name__ == '__main__':
    create_flowchart_image()
