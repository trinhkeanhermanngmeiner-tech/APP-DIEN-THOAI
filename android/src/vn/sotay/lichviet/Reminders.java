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
import android.os.Build;

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

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("sotay", Context.MODE_PRIVATE);
    }

    @Override
    public void onReceive(Context c, Intent intent) {
        postToday(c, ACTION_FIRE.equals(intent.getAction()));
        scheduleNext(c);
    }

    static String todayKey() {
        Calendar k = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                k.get(Calendar.YEAR), k.get(Calendar.MONTH) + 1, k.get(Calendar.DAY_OF_MONTH));
    }

    /** Hiện thông báo của hôm nay, mỗi ngày một lần. Ngoài báo thức thì chỉ hiện khi đã qua giờ nhắc. */
    static void postToday(Context c, boolean fromAlarm) {
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

    static void createChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Nhắc ngày", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Sinh nhật, đám giỗ, ngày lễ, mùng 1 và rằm");
        nm.createNotificationChannel(ch);
    }

    static void post(Context c, int id, String title, String text) {
        createChannel(c);
        Intent open = new Intent(c, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
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
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }
}
