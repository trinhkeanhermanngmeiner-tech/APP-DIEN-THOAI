package vn.sotay.lichviet;

import static org.robolectric.Shadows.shadowOf;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/** 14 ngày dùng thử: bắt đầu một lần, tự hết hạn, chống chỉnh đồng hồ, gộp từ sao lưu, báo trước khi hết. */
@RunWith(RobolectricTestRunner.class)
public class TrialTest {
    private static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }

    private static Object call(Object o, String name, Object... args) throws Exception {
        for (Method m : o.getClass().getDeclaredMethods()) if (m.getName().equals(name)) { m.setAccessible(true); return m.invoke(o, args); }
        throw new NoSuchMethodException(name);
    }

    private static void setStart(Context c, long start) {
        Pro.tprefs(c).edit().putLong("trialStart", start).putLong("lastSeen", 0).apply();
    }

    private void run() throws Exception {
        Context c = RuntimeEnvironment.getApplication();
        final long DAY = Pro.DAY;
        long now = System.currentTimeMillis();

        // 1. Chưa dùng thử: khóa. Bắt đầu một lần duy nhất
        check(!Pro.isPro(c) && Pro.trialStart(c) == 0 && Pro.trialLeftDays(c) == 0, "mặc định phải khóa, chưa dùng thử");
        check(Pro.startTrial(c), "startTrial phải thành công lần đầu");
        check(Pro.isPro(c) && Pro.trialActive(c) && Pro.trialLeftDays(c) == 14, "vừa bắt đầu phải còn 14 ngày");
        check(!Pro.startTrial(c), "không được bắt đầu dùng thử lần hai");
        long start = Pro.trialStart(c);
        check(start > 0 && Math.abs(start - now) < 60_000, "ngày bắt đầu phải là bây giờ");

        // 2. Trôi qua các ngày
        setStart(c, now - 13 * DAY - 1000);
        check(Pro.isPro(c) && Pro.trialLeftDays(c) == 1, "ngày 14 phải còn 1 ngày");
        setStart(c, now - 14 * DAY - 1000);
        check(!Pro.isPro(c) && !Pro.trialActive(c) && Pro.trialLeftDays(c) == 0, "quá 14 ngày phải hết hạn");
        check(!Pro.startTrial(c), "đã hết hạn thì không được dùng thử lại");
        Pro.set(c, true);
        check(Pro.isPro(c) && Pro.purchased(c), "mua xong phải mở khóa dù hết dùng thử");
        Pro.set(c, false);
        check(!Pro.isPro(c), "hoàn tiền: khóa lại, không quay lại dùng thử");

        // 3. Chống chỉnh lùi đồng hồ: mốc đã thấy ở tương lai thì thời gian không lùi
        Pro.tprefs(c).edit().putLong("trialStart", now - 5 * DAY).putLong("lastSeen", now + 20 * DAY).apply();
        check(!Pro.trialActive(c), "đồng hồ bị chỉnh lùi không được làm dùng thử dài thêm");
        Pro.tprefs(c).edit().clear().apply();

        // 4. Gộp từ sao lưu: lấy ngày sớm nhất, bỏ ngày ở tương lai
        check(!Pro.mergeTrial(c, now + 5 * DAY), "ngày tương lai phải bị bỏ qua");
        check(Pro.mergeTrial(c, now - 20 * DAY) && Pro.trialStart(c) == now - 20 * DAY && !Pro.isPro(c), "sao lưu cũ: dùng thử đã hết");
        check(!Pro.mergeTrial(c, now - 3 * DAY), "ngày muộn hơn không được ghi đè ngày sớm hơn");
        Pro.tprefs(c).edit().clear().apply();
        check(Pro.startTrial(c) && Pro.mergeTrial(c, now - 10 * DAY) && Pro.trialLeftDays(c) == 4, "nhập sao lưu cũ hơn phải rút ngắn dùng thử");
        Pro.tprefs(c).edit().clear().apply();

        // 5. Thông báo: báo trước khi còn 2 ngày, báo một lần khi hết; không báo nếu đã mua
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        String today = String.format(Locale.US, "%04d-%02d-%02d", Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        Reminders.prefs(c).edit().putString("list", "[{\"date\":\"" + today + "\",\"title\":\"Hôm nay: Giỗ ông\",\"text\":\"x\"},{\"date\":\"" + today + "\",\"title\":\"Tết\",\"text\":\"x\"}]").apply();
        check(Reminders.upcomingEvents(c, 30) == 1, "chỉ đếm ngày nhớ của người dùng, không đếm ngày lễ");
        Pro.startTrial(c);
        setStart(c, now - 5 * DAY);
        Reminders.postTrialNotices(c);
        check(shadowOf(nm).getAllNotifications().isEmpty(), "còn nhiều ngày: chưa báo");
        setStart(c, now - 12 * DAY - 1000);
        Reminders.postTrialNotices(c);
        check(shadowOf(nm).getAllNotifications().size() == 1, "còn 2 ngày: phải báo trước");
        String warn = shadowOf(shadowOf(nm).getAllNotifications().get(0)).getContentText().toString();
        System.out.println("báo trước: " + warn);
        check(warn.contains("1 ngày nhớ"), "thông báo phải nêu số ngày nhớ sắp mất nhắc");
        Reminders.postTrialNotices(c);
        check(shadowOf(nm).getAllNotifications().size() == 1, "báo trước chỉ một lần");
        setStart(c, now - 15 * DAY);
        Reminders.postTrialNotices(c);
        check(shadowOf(nm).getAllNotifications().size() == 2, "hết hạn: phải báo");
        Reminders.postTrialNotices(c);
        check(shadowOf(nm).getAllNotifications().size() == 2, "báo hết hạn chỉ một lần");
        // nhắc ngày: trong dùng thử có, hết dùng thử không
        nm.cancelAll();
        Reminders.postToday(c, true);
        check(shadowOf(nm).getAllNotifications().stream().noneMatch(n -> shadowOf(n).getContentTitle().toString().startsWith("Hôm nay: ")), "hết dùng thử: không nhắc ngày");
        Pro.tprefs(c).edit().clear().apply();
        Pro.set(c, true);
        Pro.tprefs(c).edit().putLong("trialStart", now - 30 * DAY).apply();
        nm.cancelAll();
        Reminders.postTrialNotices(c);
        check(shadowOf(nm).getAllNotifications().isEmpty(), "đã mua: không báo gì về dùng thử");
        Pro.set(c, false);
        Pro.tprefs(c).edit().clear().apply();

        // 6. Báo thức hết dùng thử: khóa tiện ích đúng lúc, không lỗi
        Pro.startTrial(c);
        new Reminders().onReceive(c, new Intent(Reminders.ACTION_TRIAL_END));
        NotesWidget.build(c, 1).apply(c, new FrameLayout(c));

        // 7. Cầu nối: startTrial, proInfo, mergeTrial, devResetTrial
        Pro.tprefs(c).edit().clear().apply();
        ActivityController<MainActivity> mc = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity m = mc.get();
        WebView web = (WebView) ((ViewGroup) ((ViewGroup) m.findViewById(android.R.id.content)).getChildAt(0)).getChildAt(1);
        Object bridge = shadowOf(web).getJavascriptInterface("AndroidBridge");
        check(((String) call(bridge, "proInfo")).contains("\"trialUsed\":false"), "chưa dùng thử");
        check(Boolean.TRUE.equals(call(bridge, "startTrial")), "bridge.startTrial");
        String info = (String) call(bridge, "proInfo");
        System.out.println("proInfo(trial) = " + info);
        check(info.contains("\"pro\":true") && info.contains("\"trialActive\":true") && info.contains("\"trialLeft\":14") && info.contains("\"purchased\":false"), "proInfo dùng thử sai");
        check(shadowOf(web).getLastEvaluatedJavascript().contains("window.onPro("), "phải báo cho giao diện");
        check(Boolean.FALSE.equals(call(bridge, "startTrial")), "bridge: lần hai phải false");
        check(Boolean.TRUE.equals(call(bridge, "mergeTrial", String.valueOf(now - 20 * DAY))) && !Pro.isPro(c), "mergeTrial từ sao lưu cũ");
        check(Boolean.FALSE.equals(call(bridge, "mergeTrial", "không phải số")), "mergeTrial phải bỏ qua giá trị sai");
        call(bridge, "devResetTrial");
        check(Pro.trialStart(c) == 0, "devResetTrial phải xóa dữ liệu dùng thử");
        mc.pause().stop().destroy();

        // 8. Sao lưu tự động: chỉ ngày dùng thử và tệp dữ liệu, không kèm trạng thái đã mua
        String rules = new String(Files.readAllBytes(new File("../res/xml/backup_rules.xml").toPath()), "UTF-8");
        String ext = new String(Files.readAllBytes(new File("../res/xml/data_extraction_rules.xml").toPath()), "UTF-8");
        String manifest = new String(Files.readAllBytes(new File("../AndroidManifest.xml").toPath()), "UTF-8");
        check(rules.contains("sotay_trial.xml") && rules.contains("so-tay.json") && !rules.contains("\"sotay.xml\""), "backup_rules sai");
        check(ext.contains("<cloud-backup>") && ext.contains("sotay_trial.xml") && !ext.contains("\"sotay.xml\""), "data_extraction_rules sai");
        check(manifest.contains("android:allowBackup=\"true\"") && manifest.contains("@xml/backup_rules") && manifest.contains("@xml/data_extraction_rules"), "manifest chưa nối quy tắc sao lưu");
        System.out.println("OK trial sdk " + android.os.Build.VERSION.SDK_INT);
    }

    @Test @Config(sdk = 36) public void api36() throws Exception { run(); }
    @Test @Config(sdk = 30) public void api30() throws Exception { run(); }
    @Test @Config(sdk = 26) public void api26() throws Exception { run(); }
}
