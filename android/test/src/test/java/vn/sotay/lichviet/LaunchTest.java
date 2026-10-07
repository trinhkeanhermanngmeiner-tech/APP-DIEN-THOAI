package vn.sotay.lichviet;

import static org.robolectric.Shadows.shadowOf;

import android.app.NotificationManager;
import android.webkit.WebView;
import android.view.ViewGroup;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class LaunchTest {
    private static Object call(Object o, String name, Object... args) throws Exception {
        for (Method m : o.getClass().getDeclaredMethods()) if (m.getName().equals(name)) { m.setAccessible(true); return m.invoke(o, args); }
        throw new NoSuchMethodException(name);
    }
    private void run() throws Exception {
        ActivityController<MainActivity> c = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = c.get();
        ViewGroup root = (ViewGroup) a.findViewById(android.R.id.content);
        WebView web = (WebView) ((ViewGroup) root.getChildAt(0)).getChildAt(1);
        System.out.println("url=" + shadowOf(web).getLastLoadedUrl());
        Object b = shadowOf(web).getJavascriptInterface("AndroidBridge");
        Calendar k = Calendar.getInstance();
        String today = String.format(Locale.US, "%04d-%02d-%02d", k.get(Calendar.YEAR), k.get(Calendar.MONTH) + 1, k.get(Calendar.DAY_OF_MONTH));
        call(b, "saveData", "{\"data\":{}}");
        System.out.println("loadData=" + call(b, "loadData"));
        call(b, "setReminders", "[{\"date\":\"" + today + "\",\"title\":\"Giỗ ông\",\"text\":\"3/9 âm\"}]", 0);
        call(b, "setBarColor", "#B3201B");
        call(b, "setNavColor", "rgb(239, 239, 236)");
        call(b, "testNotify");
        System.out.println("saveFile=" + call(b, "saveFile", "a.json", "{}", "application/json"));
        NotificationManager nm = a.getSystemService(NotificationManager.class);
        System.out.println("notifications=" + shadowOf(nm).getAllNotifications().size());
        // Tiện ích tờ lịch: lưu dữ liệu, dựng RemoteViews và thử hiển thị bố cục thật
        call(b, "setWidgetData", "{\"" + today + "\":{\"l\":\"27 tháng Tám\",\"y\":\"Bính Ngọ\",\"n\":\"Còn 5 ngày: Giỗ ông nội\"}}");
        android.widget.RemoteViews rv = LichWidget.build(a);
        android.view.View wv = rv.apply(a, new android.widget.FrameLayout(a));
        android.widget.TextView lunar = wv.findViewById(R.id.w_lunar), next = wv.findViewById(R.id.w_next);
        System.out.println("widget: " + lunar.getText() + " | " + next.getText());
        if (!lunar.getText().toString().contains("Bính Ngọ")) throw new AssertionError("widget không đọc được dữ liệu");
        android.appwidget.AppWidgetManager wm = android.appwidget.AppWidgetManager.getInstance(a);
        int wid = shadowOf(wm).createWidget(LichWidget.class, R.layout.widget_lich);
        new LichWidget().onUpdate(a, wm, new int[]{wid});
        LichWidget.refreshAll(a);
        // Vân tay: kiểm tra có/không và gọi hộp thoại (Robolectric không có cảm biến thật)
        System.out.println("canBiometric=" + call(b, "canBiometric"));
        try { call(b, "biometricAuth"); org.robolectric.shadows.ShadowLooper.idleMainLooper(); System.out.println("biometricAuth: không lỗi"); }
        catch (Throwable t) { throw new AssertionError("biometricAuth lỗi", t); }
        a.onBackPressed();
        new Reminders().onReceive(a, new android.content.Intent(Reminders.ACTION_FIRE));
        new Reminders().onReceive(a, new android.content.Intent("android.intent.action.BOOT_COMPLETED"));
        c.pause().stop().destroy();
        System.out.println("OK sdk " + android.os.Build.VERSION.SDK_INT);
    }
    @Test @Config(sdk = 36) public void api36() throws Exception { run(); }
    @Test @Config(sdk = 35) public void api35() throws Exception { run(); }
    @Test @Config(sdk = 34) public void api34() throws Exception { run(); }
    @Test @Config(sdk = 30) public void api30() throws Exception { run(); }
    @Test @Config(sdk = 33) public void api33() throws Exception { run(); }
    @Test @Config(sdk = 29) public void api29() throws Exception { run(); }
    @Test @Config(sdk = 26) public void api26() throws Exception { run(); }
}
