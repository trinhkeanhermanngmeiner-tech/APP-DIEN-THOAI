/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/**
 * Tiện ích "tờ lịch" ngoài màn hình chính. Ngày âm và ngày nhớ sắp tới được giao diện web tính sẵn
 * cho 400 ngày (lưu ở SharedPreferences "widget"), nên tiện ích chỉ cần tra theo ngày hôm nay.
 */
public class LichWidget extends AppWidgetProvider {
    private static final String[] WEEKDAYS = {"Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        RemoteViews v = build(c);
        for (int id : ids) m.updateAppWidget(id, v);
    }

    static RemoteViews build(Context c) {
        Calendar k = Calendar.getInstance();
        int d = k.get(Calendar.DAY_OF_MONTH), mo = k.get(Calendar.MONTH) + 1, y = k.get(Calendar.YEAR);
        int wd = k.get(Calendar.DAY_OF_WEEK) - 1;
        String key = String.format(Locale.US, "%04d-%02d-%02d", y, mo, d);

        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_lich);
        v.setTextViewText(R.id.w_month, "THÁNG " + mo + " · " + y);
        v.setTextViewText(R.id.w_day, String.valueOf(d));
        v.setTextViewText(R.id.w_wd, WEEKDAYS[wd]);
        if (wd == 0) {
            v.setTextColor(R.id.w_day, 0xFFC2261F);
            v.setTextColor(R.id.w_wd, 0xFFC2261F);
        }
        String lunar = "Mở app để cập nhật ngày âm", next = "";
        try {
            JSONObject o = new JSONObject(Reminders.prefs(c).getString("widget", "{}")).optJSONObject(key);
            if (o != null) {
                lunar = o.optString("l") + " · " + o.optString("y");
                next = o.optString("n");
            }
        } catch (Exception ignored) {
        }
        v.setTextViewText(R.id.w_lunar, lunar);
        v.setTextViewText(R.id.w_next, next);

        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(c, 2, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        return v;
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        if (m == null) return;
        int[] ids = m.getAppWidgetIds(new ComponentName(c, LichWidget.class));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = build(c);
        for (int id : ids) m.updateAppWidget(id, v);
    }
}
