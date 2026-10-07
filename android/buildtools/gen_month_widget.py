"""Sinh bố cục tiện ích lịch tháng (6 hàng × 7 ô) và bảng id tương ứng cho Java."""
from pathlib import Path
root = Path(__file__).resolve().parent.parent
rows = []
for r in range(6):
    cells = []
    for c in range(7):
        i = r * 7 + c
        cells.append(f'''            <LinearLayout android:id="@+id/c{i}" android:layout_width="0dp" android:layout_height="match_parent" android:layout_weight="1"
                android:layout_margin="1dp" android:gravity="center" android:orientation="vertical">
                <TextView android:id="@+id/s{i}" android:layout_width="wrap_content" android:layout_height="wrap_content" android:includeFontPadding="false"
                    android:textColor="@color/widget_ink" android:textSize="14sp" android:textStyle="bold" />
                <TextView android:id="@+id/l{i}" android:layout_width="wrap_content" android:layout_height="wrap_content" android:includeFontPadding="false"
                    android:textColor="@color/widget_muted" android:textSize="9sp" />
                <TextView android:id="@+id/d{i}" android:layout_width="wrap_content" android:layout_height="wrap_content" android:includeFontPadding="false"
                    android:textSize="8sp" />
            </LinearLayout>''')
    rows.append(f'''        <LinearLayout android:id="@+id/r{r}" android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:orientation="horizontal">
{chr(10).join(cells)}
        </LinearLayout>''')
week = "\n".join(
    f'''        <TextView android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1" android:gravity="center" android:text="{t}"
            android:textColor="{'@color/widget_red' if t == 'CN' else '@color/widget_muted'}" android:textSize="10sp" android:textStyle="bold" />'''
    for t in ["T2", "T3", "T4", "T5", "T6", "T7", "CN"])
xml = f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Tiện ích lịch tháng · Sổ Tay Lịch Việt. Tệp sinh tự động bởi buildtools/gen_month_widget.py, đừng sửa tay. -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" android:id="@+id/m_root"
    android:layout_width="match_parent" android:layout_height="match_parent" android:background="@drawable/widget_bg" android:orientation="vertical">
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:background="@drawable/widget_band"
        android:gravity="center_vertical" android:orientation="horizontal" android:paddingTop="4dp" android:paddingBottom="4dp">
        <TextView android:id="@+id/m_prev" android:layout_width="40dp" android:layout_height="36dp" android:gravity="center" android:text="‹"
            android:textColor="#FFF7EE" android:textSize="24sp" android:contentDescription="Tháng trước" />
        <LinearLayout android:id="@+id/m_titlebox" android:layout_width="0dp" android:layout_height="wrap_content" android:layout_weight="1"
            android:gravity="center" android:orientation="vertical">
            <TextView android:id="@+id/m_title" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text="Tháng 10, 2026"
                android:textColor="#FFF7EE" android:textSize="15sp" android:textStyle="bold" />
            <TextView android:id="@+id/m_sub" android:layout_width="wrap_content" android:layout_height="wrap_content" android:text=""
                android:textColor="#F3D9A0" android:textSize="10sp" />
        </LinearLayout>
        <TextView android:id="@+id/m_add" android:layout_width="36dp" android:layout_height="36dp" android:gravity="center" android:text="＋"
            android:textColor="#FFF7EE" android:textSize="18sp" android:contentDescription="Ghi chú hôm nay" />
        <TextView android:id="@+id/m_next" android:layout_width="40dp" android:layout_height="36dp" android:gravity="center" android:text="›"
            android:textColor="#FFF7EE" android:textSize="24sp" android:contentDescription="Tháng sau" />
    </LinearLayout>
    <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content" android:orientation="horizontal" android:paddingTop="4dp" android:paddingBottom="2dp">
{week}
    </LinearLayout>
    <LinearLayout android:layout_width="match_parent" android:layout_height="0dp" android:layout_weight="1" android:orientation="vertical" android:paddingStart="3dp" android:paddingEnd="3dp">
{chr(10).join(rows)}
    </LinearLayout>
    <TextView android:id="@+id/m_foot" android:layout_width="match_parent" android:layout_height="wrap_content" android:ellipsize="end" android:gravity="center"
        android:maxLines="1" android:paddingStart="8dp" android:paddingEnd="8dp" android:paddingTop="2dp" android:paddingBottom="6dp"
        android:textColor="@color/widget_red" android:textSize="11sp" />
</LinearLayout>
'''
(root / "res/layout/widget_thang.xml").write_text(xml, encoding="utf-8")
ids = lambda p: ", ".join(f"R.id.{p}{i}" for i in range(42))
java = f'''/* Tệp sinh tự động bởi buildtools/gen_month_widget.py, đừng sửa tay. */
package vn.sotay.lichviet;

final class MonthIds {{
    static final int[] CELL = {{{ids("c")}}};
    static final int[] SOLAR = {{{ids("s")}}};
    static final int[] LUNAR = {{{ids("l")}}};
    static final int[] DOTS = {{{ids("d")}}};
    static final int[] ROW = {{{", ".join(f"R.id.r{r}" for r in range(6))}}};

    private MonthIds() {{
    }}
}}
'''
(root / "src/vn/sotay/lichviet/MonthIds.java").write_text(java, encoding="utf-8")
print("đã sinh widget_thang.xml và MonthIds.java")
