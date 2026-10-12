/* Tệp sinh tự động bởi buildtools/gen_skins.py, đừng sửa tay. */
package vn.sotay.lichviet;

import android.content.Context;
import android.content.res.Configuration;

/** Theme màu của tiện ích (cùng bảng màu với giao diện trong app). */
final class Skins {
    static final String[] IDS = {"macdinh", "luc", "cham", "sen", "nghe", "tim", "muc"};
    static final String[] NAMES = {"Sơn mài", "Lục bảo", "Chàm", "Sen", "Nghệ", "Tím mận", "Mực tàu"};
    private static final int[] ACCENT = {0xFFC2261F, 0xFF0E6150, 0xFF2F5DA8, 0xFFB5386A, 0xFF9A5F05, 0xFF6A45A8, 0xFF3A3430};
    private static final int[] ACCENT_NIGHT = {0xFFFF6B5E, 0xFF4CC5A2, 0xFF7EA6EE, 0xFFF08AB0, 0xFFF0C062, 0xFFC08BE0, 0xFFF0C062};
    private static final int[] BAND = {R.drawable.widget_band_macdinh, R.drawable.widget_band_luc, R.drawable.widget_band_cham, R.drawable.widget_band_sen, R.drawable.widget_band_nghe, R.drawable.widget_band_tim, R.drawable.widget_band_muc};
    private static final int[] BAND_NOTES = {R.drawable.widget_bandnotes_macdinh, R.drawable.widget_bandnotes_luc, R.drawable.widget_bandnotes_cham, R.drawable.widget_bandnotes_sen, R.drawable.widget_bandnotes_nghe, R.drawable.widget_bandnotes_tim, R.drawable.widget_bandnotes_muc};
    private static final int[] BG = {R.drawable.widget_bg_macdinh, R.drawable.widget_bg_luc, R.drawable.widget_bg_cham, R.drawable.widget_bg_sen, R.drawable.widget_bg_nghe, R.drawable.widget_bg_tim, R.drawable.widget_bg_muc};
    private static final int[] TODAY = {R.drawable.widget_today_macdinh, R.drawable.widget_today_luc, R.drawable.widget_today_cham, R.drawable.widget_today_sen, R.drawable.widget_today_nghe, R.drawable.widget_today_tim, R.drawable.widget_today_muc};

    private Skins() {
    }

    static int index(String id) {
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return 0;
    }

    /** Theme đang dùng cho tiện ích: theo lựa chọn riêng, hoặc theo theme của app khi chọn "Theo app". */
    static int current(Context c) {
        String w = Reminders.prefs(c).getString("wskin", "auto");
        return index("auto".equals(w) ? Reminders.prefs(c).getString("skin", "macdinh") : w);
    }

    static boolean night(Context c) {
        return (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    static int accent(Context c) {
        int i = current(c);
        return night(c) ? ACCENT_NIGHT[i] : ACCENT[i];
    }

    static int band(Context c) {
        return BAND[current(c)];
    }

    static int bandNotes(Context c) {
        return BAND_NOTES[current(c)];
    }

    static int background(Context c) {
        return BG[current(c)];
    }

    static int today(Context c) {
        return TODAY[current(c)];
    }
}
