"""Sinh tài nguyên và lớp Skins.java cho các theme màu của tiện ích (cùng bảng màu với giao diện web)."""
from pathlib import Path

root = Path(__file__).resolve().parent.parent
# id: (dải đầu, dải đầu ghi chú, màu nhấn sáng, màu nhấn tối, nền sáng, nền tối, ô hôm nay sáng, ô hôm nay tối)
SKINS = {
    "macdinh": ("#B3201B", "#2A2623", "#C2261F", "#FF6B5E", "#FFFFFF", "#1B1918", "#C2261F", "#E0463C"),
    "luc":     ("#0E6150", "#0E6150", "#0E6150", "#4CC5A2", "#F2F9F6", "#14201C", "#0E6150", "#1F8F73"),
    "cham":    ("#23407A", "#23407A", "#2F5DA8", "#7EA6EE", "#F4F7FD", "#161B27", "#2F5DA8", "#3D6BC0"),
    "sen":     ("#A8325E", "#A8325E", "#B5386A", "#F08AB0", "#FDF4F7", "#241820", "#B5386A", "#C94677"),
    "nghe":    ("#A86A06", "#A86A06", "#9A5F05", "#F0C062", "#FFF9EC", "#231D10", "#B87408", "#C98410"),
    "tim":     ("#5B3A8E", "#5B3A8E", "#6A45A8", "#C08BE0", "#F7F3FC", "#1E1828", "#6A45A8", "#8A5FCB"),
    "muc":     ("#2A2623", "#2A2623", "#3A3430", "#F0C062", "#F6F4F0", "#171514", "#2A2623", "#7A6A5C"),
}
NAMES = {"macdinh": "Sơn mài", "luc": "Lục bảo", "cham": "Chàm", "sen": "Sen", "nghe": "Nghệ", "tim": "Tím mận", "muc": "Mực tàu"}


def write(path: Path, text: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def shape(color: str, top_only: bool = False, radius: int = 18) -> str:
    corners = (f'android:topLeftRadius="{radius}dp" android:topRightRadius="{radius}dp" android:bottomLeftRadius="0dp" android:bottomRightRadius="0dp"'
               if top_only else f'android:radius="{radius}dp"')
    return (f'<?xml version="1.0" encoding="utf-8"?>\n<!-- Sinh tự động bởi buildtools/gen_skins.py -->\n'
            f'<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
            f'    <solid android:color="{color}" />\n    <corners {corners} />\n</shape>\n')


for sid, (band, bandn, acc, accn, bg, bgn, today, todayn) in SKINS.items():
    write(root / f"res/drawable/widget_band_{sid}.xml", shape(band, True))
    write(root / f"res/drawable/widget_bandnotes_{sid}.xml", shape(bandn, True))
    write(root / f"res/drawable/widget_bg_{sid}.xml", shape(bg))
    write(root / f"res/drawable-night/widget_bg_{sid}.xml", shape(bgn))
    write(root / f"res/drawable/widget_today_{sid}.xml", shape(today, False, 9))
    write(root / f"res/drawable-night/widget_today_{sid}.xml", shape(todayn, False, 9))

ids = list(SKINS)
col = lambda h: "0xFF" + h[1:].upper()
res = lambda p: ", ".join(f"R.drawable.widget_{p}_{i}" for i in ids)
java = f'''/* Tệp sinh tự động bởi buildtools/gen_skins.py, đừng sửa tay. */
package vn.sotay.lichviet;

import android.content.Context;
import android.content.res.Configuration;

/** Theme màu của tiện ích (cùng bảng màu với giao diện trong app). */
final class Skins {{
    static final String[] IDS = {{{", ".join(f'"{i}"' for i in ids)}}};
    static final String[] NAMES = {{{", ".join(f'"{NAMES[i]}"' for i in ids)}}};
    private static final int[] ACCENT = {{{", ".join(col(SKINS[i][2]) for i in ids)}}};
    private static final int[] ACCENT_NIGHT = {{{", ".join(col(SKINS[i][3]) for i in ids)}}};
    private static final int[] BAND = {{{res("band")}}};
    private static final int[] BAND_NOTES = {{{res("bandnotes")}}};
    private static final int[] BG = {{{res("bg")}}};
    private static final int[] TODAY = {{{res("today")}}};

    private Skins() {{
    }}

    static int index(String id) {{
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return 0;
    }}

    /** Theme đang dùng cho tiện ích: theo lựa chọn riêng, hoặc theo theme của app khi chọn "Theo app". */
    static int current(Context c) {{
        String w = Reminders.prefs(c).getString("wskin", "auto");
        return index("auto".equals(w) ? Reminders.prefs(c).getString("skin", "macdinh") : w);
    }}

    static boolean night(Context c) {{
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }}

    static int accent(Context c) {{
        int i = current(c);
        return night(c) ? ACCENT_NIGHT[i] : ACCENT[i];
    }}

    static int band(Context c) {{
        return BAND[current(c)];
    }}

    static int bandNotes(Context c) {{
        return BAND_NOTES[current(c)];
    }}

    static int background(Context c) {{
        return BG[current(c)];
    }}

    static int today(Context c) {{
        return TODAY[current(c)];
    }}
}}
'''
write(root / "src/vn/sotay/lichviet/Skins.java", java)
print("đã sinh", len(SKINS), "theme")
