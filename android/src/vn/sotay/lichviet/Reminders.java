/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/**
 * Báo thức mỗi ngày một lần vào giờ đã chọn: đọc danh sách lời nhắc (do giao diện web tính sẵn
 * cho 400 ngày tới, đã đổi âm sang dương) và hiện thông báo cho những mục của hôm nay.
 */
public class Reminders extends BroadcastReceiver {
    static final String CHANNEL = "nhac-ngay";
    static final String ACTION_FIRE = "vn.sotay.lichviet.NHAC";
    static final String ACTION_TRIAL_END = "vn.sotay.lichviet.HET_DUNG_THU";

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("sotay", Context.MODE_PRIVATE);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        postToday(c, ACTION_FIRE.equals(intent.getAction()));
        postTrialNotices(c);
        scheduleNext(c);
        // Hết hạn dùng thử (hoặc mới mua): dựng lại tiện ích để khóa/mở đúng trạng thái.
        Widgets.rebuildAll(c);
    }

    static String todayKey() {
        Calendar k = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                k.get(Calendar.YEAR), k.get(Calendar.MONTH) + 1, k.get(Calendar.DAY_OF_MONTH));
    }

    /** Hiện thông báo của hôm nay, mỗi ngày một lần. Ngoài báo thức thì chỉ hiện khi đã qua giờ nhắc. */
    static void postToday(Context c, boolean fromAlarm) {
        if (!Pro.isPro(c)) return; // nhắc ngày thuộc Bản Đầy Đủ
        SharedPreferences p = prefs(c);
        String today = todayKey();
        if (today.equals(p.getString("last", ""))) return;
        if (!fromAlarm && Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < p.getInt("hour", 7)) return;
        String list = p.getString("list", null);
        if (list == null) return;
        try {
            JSONArray arr = new JSONArray(list);
            int n = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (today.equals(o.optString("date"))) {
                    post(c, 100 + n, o.optString("title"), o.optString("text"));
                    n++;
                }
            }
        } catch (Exception ignored) {
        }
        p.edit().putString("last", today).apply();
    }

    /** Số ngày nhớ của người dùng (không tính ngày lễ) rơi trong "days" ngày tới, đọc từ danh sách lời nhắc đã lưu. */
    static int upcomingEvents(Context c, int days) {
        String list = prefs(c).getString("list", null);
        if (list == null) return 0;
        Calendar k = Calendar.getInstance();
        k.add(Calendar.DAY_OF_MONTH, days);
        String limit = String.format(Locale.US, "%04d-%02d-%02d", k.get(Calendar.YEAR), k.get(Calendar.MONTH) + 1, k.get(Calendar.DAY_OF_MONTH));
        String today = todayKey();
        int n = 0;
        try {
            JSONArray arr = new JSONArray(list);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String d = o.optString("date");
                if (o.optString("title").startsWith("Hôm nay: ") && d.compareTo(today) >= 0 && d.compareTo(limit) <= 0) n++;
            }
        } catch (Exception ignored) {
        }
        return n;
    }

    /**
     * Báo trước khi hết dùng thử (còn 2 ngày) và báo khi đã hết, mỗi loại một lần. Không báo nếu đã mua.
     * Báo trước để người dùng không bị mất nhắc ngày giỗ mà không hay biết.
     */
    static void postTrialNotices(Context c) {
        if (Pro.purchased(c) || Pro.trialStart(c) <= 0) return;
        SharedPreferences t = Pro.tprefs(c);
        if (Pro.trialActive(c)) {
            int left = Pro.trialLeftDays(c);
            if (left <= 2 && !t.getBoolean("trialWarn", false)) {
                int n = upcomingEvents(c, 30);
                String text = (n > 0 ? "Có " + n + " ngày nhớ trong 30 ngày tới sẽ không được nhắc nếu bạn chưa mở khóa. " : "Sau đó nhắc ngày, chi tiêu, ghi chú sẽ bị khóa. ")
                        + "Dữ liệu của bạn vẫn được giữ. Mở khóa một lần, 20.000đ.";
                post(c, 200, "Còn " + left + " ngày dùng thử Bản Đầy Đủ", text, NotesWidget.paywallIntent(c));
                t.edit().putBoolean("trialWarn", true).apply();
            }
        } else if (!t.getBoolean("trialEnd", false)) {
            post(c, 201, "Đã hết thời gian dùng thử",
                    "Nhắc ngày, chi tiêu, ghi chú đã tắt. Lịch âm dương vẫn dùng bình thường và dữ liệu của bạn vẫn được giữ. Mở khóa 20.000đ để dùng tiếp.",
                    NotesWidget.paywallIntent(c));
            t.edit().putBoolean("trialEnd", true).apply();
        }
    }

    /** Đặt báo thức đúng lúc hết dùng thử để khóa tiện ích ngay, không phải chờ lần báo kế tiếp. */
    static void scheduleTrialEnd(Context c) {
        long s = Pro.trialStart(c);
        if (s <= 0 || Pro.purchased(c)) return;
        long at = s + Pro.TRIAL_DAYS * Pro.DAY + 1000;
        if (at <= System.currentTimeMillis()) return;
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        Intent i = new Intent(c, Reminders.class).setAction(ACTION_TRIAL_END);
        PendingIntent pi = PendingIntent.getBroadcast(c, 2, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
    }

    static void createChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Nhắc ngày", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Sinh nhật, đám giỗ, ngày lễ, mùng 1 và rằm");
        nm.createNotificationChannel(ch);
    }

    static void post(Context c, int id, String title, String text) {
        post(c, id, title, text, new Intent(c, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    static void post(Context c, int id, String title, String text, Intent open) {
        createChannel(c);
        PendingIntent pi = PendingIntent.getActivity(c, id, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setColor(0xFFB3201B)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        try {
            if (nm != null) nm.notify(id, n);
        } catch (SecurityException ignored) {
        }
    }

    static void scheduleNext(Context c) {
        int hour = prefs(c).getInt("hour", 7);
        Calendar k = Calendar.getInstance();
        k.set(Calendar.HOUR_OF_DAY, hour);
        k.set(Calendar.MINUTE, 0);
        k.set(Calendar.SECOND, 0);
        k.set(Calendar.MILLISECOND, 0);
        if (k.getTimeInMillis() <= System.currentTimeMillis()) k.add(Calendar.DAY_OF_MONTH, 1);
        long at = k.getTimeInMillis();

        Intent i = new Intent(c, Reminders.class).setAction(ACTION_FIRE);
        PendingIntent pi = PendingIntent.getBroadcast(c, 1, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        if (am == null) return;
        // Báo thức không cần chính xác tới từng phút nên không dùng quyền báo thức chính xác (bị CH Play hạn chế).
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        scheduleTrialEnd(c);
    }
}
