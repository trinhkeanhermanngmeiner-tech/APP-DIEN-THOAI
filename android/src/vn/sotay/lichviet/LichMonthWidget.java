/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/**
 * Tiện ích lịch tháng: 6 hàng × 7 ô, mỗi ô có ngày dương, ngày âm và chấm đánh dấu
 * (đỏ: ngày lễ, xanh lục: ngày nhớ, xanh dương: ghi chú). Nút ‹ › đổi tháng; chạm vào ngày để mở
 * bảng ghi chú nhanh của ngày đó trong app. Dữ liệu âm lịch do giao diện web tính sẵn (SharedPreferences "cal").
 */
public class LichMonthWidget extends AppWidgetProvider {
    static final String ACTION_PREV = "vn.sotay.lichviet.THANG_TRUOC";
    static final String ACTION_NEXT = "vn.sotay.lichviet.THANG_SAU";
    static final String ACTION_NOW = "vn.sotay.lichviet.THANG_NAY";
    private static final int MAX_OFFSET = 12;

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        RemoteViews v = build(c);
        for (int id : ids) m.updateAppWidget(id, v);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        super.onReceive(c, intent);
        String a = intent.getAction();
        if (!ACTION_PREV.equals(a) && !ACTION_NEXT.equals(a) && !ACTION_NOW.equals(a)) return;
        int off = Reminders.prefs(c).getInt("monthOffset", 0);
        if (ACTION_PREV.equals(a)) off--;
        else if (ACTION_NEXT.equals(a)) off++;
        else off = 0;
        off = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, off));
        Reminders.prefs(c).edit().putInt("monthOffset", off).apply();
        refreshAll(c);
    }

    private static PendingIntent broadcast(Context c, String action, int code) {
        Intent i = new Intent(c, LichMonthWidget.class).setAction(action);
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Mở app tại bảng ghi chú của ngày (dạng sotay://ngay/2026-10-07). */
    static PendingIntent openDay(Context c, String key, int code) {
        Intent i = new Intent(c, MainActivity.class).setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse("sotay://ngay/" + key))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, code, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static String key(Calendar k) {
        return String.format(Locale.US, "%04d-%02d-%02d", k.get(Calendar.YEAR), k.get(Calendar.MONTH) + 1, k.get(Calendar.DAY_OF_MONTH));
    }

    static RemoteViews build(Context c) {
        Calendar now = Calendar.getInstance();
        String todayKey = key(now);
        int off = Reminders.prefs(c).getInt("monthOffset", 0);
        Calendar first = Calendar.getInstance();
        first.set(Calendar.DAY_OF_MONTH, 1);
        first.add(Calendar.MONTH, off);
        int month = first.get(Calendar.MONTH) + 1, year = first.get(Calendar.YEAR);
        int dim = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int lead = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7; // tuần bắt đầu từ Thứ Hai
        int rows = (lead + dim + 6) / 7;

        JSONObject days = new JSONObject(), months = new JSONObject();
        try {
            JSONObject all = new JSONObject(Reminders.prefs(c).getString("cal", "{}"));
            JSONObject d = all.optJSONObject("days"), m = all.optJSONObject("months");
            if (d != null) days = d;
            if (m != null) months = m;
        } catch (Exception ignored) {
        }

        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_thang);
        v.setTextViewText(R.id.m_title, "Tháng " + month + ", " + year);
        String sub = months.optString(String.format(Locale.US, "%04d-%02d", year, month), "");
        v.setTextViewText(R.id.m_sub, sub.isEmpty() ? (days.length() == 0 ? "Mở app một lần để hiện ngày âm" : "") : sub);

        int ink = c.getColor(R.color.widget_ink), muted = c.getColor(R.color.widget_muted), red = c.getColor(R.color.widget_red);
        int gold = c.getColor(R.color.widget_gold), jade = c.getColor(R.color.widget_jade), note = c.getColor(R.color.widget_note);

        Calendar day = (Calendar) first.clone();
        day.add(Calendar.DAY_OF_MONTH, -lead);
        for (int i = 0; i < 42; i++) {
            String k = key(day);
            boolean inMonth = day.get(Calendar.MONTH) + 1 == month;
            boolean sunday = day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
            String[] info = days.optString(k, "|").split("\\|", -1);
            String lunar = info.length > 0 ? info[0] : "", flags = info.length > 1 ? info[1] : "";
            boolean today = k.equals(todayKey), offDay = flags.contains("H");

            v.setTextViewText(MonthIds.SOLAR[i], String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
            v.setTextViewText(MonthIds.LUNAR[i], lunar);
            int solarColor = today ? 0xFFFFFFFF : (sunday || offDay) ? red : ink;
            int lunarColor = today ? 0xFFFFFFFF : flags.contains("m") ? gold : muted;
            if (!inMonth && !today) {
                solarColor = (solarColor & 0x00FFFFFF) | 0x55000000;
                lunarColor = (lunarColor & 0x00FFFFFF) | 0x55000000;
            }
            v.setTextColor(MonthIds.SOLAR[i], solarColor);
            v.setTextColor(MonthIds.LUNAR[i], lunarColor);
            v.setInt(MonthIds.CELL[i], "setBackgroundResource",
                    today ? R.drawable.widget_today : (offDay && inMonth) ? R.drawable.widget_offday : 0);

            SpannableStringBuilder dots = new SpannableStringBuilder();
            if (flags.contains("H") || flags.contains("h")) dot(dots, today ? 0xFFFFFFFF : red);
            if (flags.contains("e")) dot(dots, today ? 0xFFFFFFFF : jade);
            if (flags.contains("n")) dot(dots, today ? 0xFFFFFFFF : note);
            v.setTextViewText(MonthIds.DOTS[i], dots);
            v.setOnClickPendingIntent(MonthIds.CELL[i], openDay(c, k, 100 + i));
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        for (int r = 0; r < 6; r++) v.setViewVisibility(MonthIds.ROW[r], r < rows ? View.VISIBLE : View.GONE);

        String foot = "";
        try {
            JSONObject t = new JSONObject(Reminders.prefs(c).getString("widget", "{}")).optJSONObject(todayKey);
            if (t != null) foot = t.optString("n");
        } catch (Exception ignored) {
        }
        v.setTextViewText(R.id.m_foot, off == 0 ? foot : "Chạm vào tên tháng để về tháng này");

        v.setOnClickPendingIntent(R.id.m_prev, broadcast(c, ACTION_PREV, 11));
        v.setOnClickPendingIntent(R.id.m_next, broadcast(c, ACTION_NEXT, 12));
        v.setOnClickPendingIntent(R.id.m_titlebox, broadcast(c, ACTION_NOW, 13));
        v.setOnClickPendingIntent(R.id.m_add, openDay(c, todayKey, 99));
        return v;
    }

    private static void dot(SpannableStringBuilder sb, int color) {
        int s = sb.length();
        sb.append("●");
        sb.setSpan(new ForegroundColorSpan(color), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        if (m == null) return;
        int[] ids = m.getAppWidgetIds(new ComponentName(c, LichMonthWidget.class));
        if (ids == null || ids.length == 0) return;
        RemoteViews v = build(c);
        for (int id : ids) m.updateAppWidget(id, v);
    }
}
