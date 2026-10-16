package vn.sotay.lichviet;

import static org.robolectric.Shadows.shadowOf;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/** Bản miễn phí chỉ có lịch âm dương; Bản Đầy Đủ mở khóa phần còn lại. */
@RunWith(RobolectricTestRunner.class)
public class ProGatingTest {
    private static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }

    private static Object call(Object o, String name, Object... args) throws Exception {
        for (Method m : o.getClass().getDeclaredMethods()) if (m.getName().equals(name)) { m.setAccessible(true); return m.invoke(o, args); }
        throw new NoSuchMethodException(name);
    }

    private void run() throws Exception {
        Context c = RuntimeEnvironment.getApplication();
        String today = String.format(Locale.US, "%04d-%02d-%02d", Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH) + 1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        NoteStore.write(c, "{\"data\":{\"notes\":{\"a\":{\"id\":\"a\",\"title\":\"Riêng tư\",\"body\":\"x\",\"updated\":1}}}}");
        check(!Pro.isPro(c), "mặc định phải là bản miễn phí");
        check(BuildInfo.DEV, "bản dùng thử phải có BuildInfo.DEV");

        // 1. Nhắc ngày: bản miễn phí không hiện thông báo
        Reminders.prefs(c).edit().putString("list", "[{\"date\":\"" + today + "\",\"title\":\"Giỗ\",\"text\":\"x\"}]").putInt("hour", 0).apply();
        Reminders.postToday(c, true);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        check(shadowOf(nm).getAllNotifications().isEmpty(), "bản miễn phí không được báo nhắc ngày");

        // 2. Tiện ích Ghi chú bị khóa: danh sách rỗng, chạm mở màn hình mở khóa
        NotesWidgetService.Factory f = new NotesWidgetService.Factory(c);
        f.onDataSetChanged();
        check(f.getCount() == 0, "bản miễn phí không được hiện ghi chú trong tiện ích");
        NotesWidget.build(c, 1).apply(c, new FrameLayout(c));
        Intent pay = NotesWidget.paywallIntent(c);
        check("sotay".equals(pay.getData().getScheme()) && "mokhoa".equals(pay.getData().getHost()), "intent mở khóa sai");
        check("window.sotayPaywall&&window.sotayPaywall()".equals(MainActivity.linkFromIntent(pay)), "liên kết mokhoa phải gọi sotayPaywall");

        // 3. Cửa sổ ghi chú nhanh và nút trong tiện ích chuyển sang mở khóa, không sửa được dữ liệu
        ActivityController<QuickNoteActivity> q = Robolectric.buildActivity(QuickNoteActivity.class, new Intent().putExtra(QuickNoteActivity.EXTRA_ID, "a")).create();
        check(q.get().isFinishing(), "QuickNoteActivity phải tự đóng khi chưa mở khóa");
        check("mokhoa".equals(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity().getData().getHost()), "phải mở màn hình mở khóa");
        ActivityController<WidgetActionActivity> w = Robolectric.buildActivity(WidgetActionActivity.class, new Intent()
                .putExtra(QuickNoteActivity.EXTRA_ID, "a").putExtra(WidgetActionActivity.EXTRA_ACTION, WidgetActionActivity.ACT_PIN)).create();
        check(w.get().isFinishing(), "WidgetActionActivity phải tự đóng");
        check(!NoteStore.readRoot(c).getJSONObject("data").getJSONObject("notes").getJSONObject("a").optBoolean("pinned"), "ghim không được chạy khi chưa mở khóa");

        // 4. Tự lưu Google Drive: không ghi khi chưa mở khóa
        java.io.File target = java.io.File.createTempFile("drive", ".json");
        Reminders.prefs(c).edit().putString("driveUri", Uri.fromFile(target).toString()).apply();
        DriveSync.schedule(c, "{\"x\":1}");
        org.robolectric.shadows.ShadowLooper.idleMainLooper(5, java.util.concurrent.TimeUnit.SECONDS);
        DriveSync.IO.submit(new Runnable() { public void run() { } }).get();
        check(target.length() == 0, "bản miễn phí không được tự lưu lên Drive");

        // 5. Giao diện web gọi qua cầu nối: isPro / proInfo / mã mở khóa thử
        ActivityController<MainActivity> mc = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity m = mc.get();
        WebView web = (WebView) ((ViewGroup) ((ViewGroup) m.findViewById(android.R.id.content)).getChildAt(0)).getChildAt(1);
        Object bridge = shadowOf(web).getJavascriptInterface("AndroidBridge");
        check(Boolean.FALSE.equals(call(bridge, "isPro")), "isPro phải false");
        String info = (String) call(bridge, "proInfo");
        System.out.println("proInfo(free) = " + info);
        check(info.contains("\"pro\":false") && info.contains("\"dev\":true"), "proInfo sai");
        check(Boolean.FALSE.equals(call(bridge, "devUnlock", "00000000")) && !Pro.isPro(c), "mã sai không được mở khóa");
        String code = new String(Files.readAllBytes(Paths.get("../.dev-code")), "UTF-8").trim();
        check(Boolean.TRUE.equals(call(bridge, "devUnlock", code)) && Pro.isPro(c), "mã đúng phải mở khóa");
        check(shadowOf(web).getLastEvaluatedJavascript().contains("window.onPro("), "phải báo cho giao diện sau khi mở khóa");
        System.out.println("proInfo(pro) = " + call(bridge, "proInfo"));

        // 6. Sau khi mở khóa mọi thứ chạy lại
        f.onDataSetChanged();
        check(f.getCount() == 1, "mở khóa xong tiện ích phải hiện ghi chú");
        Reminders.postToday(c, true);
        check(shadowOf(nm).getAllNotifications().size() == 1, "mở khóa xong phải có thông báo nhắc");
        Method onNew = MainActivity.class.getDeclaredMethod("onNewIntent", Intent.class); onNew.setAccessible(true);
        call(bridge, "devLock");
        check(!Pro.isPro(c), "devLock phải khóa lại");
        // 7. Billing: bản dùng thử dùng StubBilling
        check(Billing.get(c) instanceof Billing.StubBilling && !Billing.get(c).available(), "APK dùng thử phải dùng StubBilling");
        mc.pause().stop().destroy();
        System.out.println("OK pro-gating sdk " + android.os.Build.VERSION.SDK_INT);
    }

    @Test @Config(sdk = 36) public void api36() throws Exception { run(); }
    @Test @Config(sdk = 30) public void api30() throws Exception { run(); }
    @Test @Config(sdk = 26) public void api26() throws Exception { run(); }
}
