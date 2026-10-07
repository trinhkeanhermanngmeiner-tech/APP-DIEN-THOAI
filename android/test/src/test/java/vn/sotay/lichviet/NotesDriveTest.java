package vn.sotay.lichviet;

import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.RemoteViews;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/** Ghi chú nhanh từ tiện ích, danh sách ghi chú của tiện ích, và đồng bộ tệp (Google Drive). */
@RunWith(RobolectricTestRunner.class)
public class NotesDriveTest {
    private static EditText[] fields(Activity a) {
        ViewGroup root = (ViewGroup) ((ViewGroup) a.findViewById(android.R.id.content)).getChildAt(0);
        EditText t = null, b = null;
        for (int i = 0; i < root.getChildCount(); i++) if (root.getChildAt(i) instanceof EditText) { if (t == null) t = (EditText) root.getChildAt(i); else b = (EditText) root.getChildAt(i); }
        return new EditText[]{t, b};
    }

    private void run() throws Exception {
        android.content.Context c = org.robolectric.RuntimeEnvironment.getApplication();
        NoteStore.write(c, "{\"data\":{\"notes\":{\"a1\":{\"id\":\"a1\",\"title\":\"Đi chợ\",\"body\":\"Rau\\nThịt\",\"pinned\":true,\"color\":\"y\",\"updated\":1}}},\"settings\":{}}");

        // 1. Viết ghi chú mới từ nút ＋ của tiện ích
        ActivityController<QuickNoteActivity> q = Robolectric.buildActivity(QuickNoteActivity.class, new Intent()).setup();
        EditText[] f = fields(q.get());
        f[1].setText("Gọi điện cho bác Hùng");
        q.pause().stop().destroy();
        // 2. Sửa ghi chú có sẵn
        q = Robolectric.buildActivity(QuickNoteActivity.class, new Intent().putExtra(QuickNoteActivity.EXTRA_ID, "a1")).setup();
        f = fields(q.get());
        if (!"Đi chợ".equals(f[0].getText().toString())) throw new AssertionError("không nạp được ghi chú");
        f[1].setText("Rau\nThịt\nTrứng");
        q.pause().stop().destroy();
        JSONObject notes = NoteStore.readRoot(c).getJSONObject("data").getJSONObject("notes");
        System.out.println("notes sau khi sửa từ tiện ích: " + notes.length() + " · a1=" + notes.getJSONObject("a1").getString("body").replace("\n", "/"));
        if (notes.length() != 2 || !notes.getJSONObject("a1").getString("body").endsWith("Trứng")) throw new AssertionError("lưu ghi chú nhanh sai");

        // 3. Danh sách của tiện ích: ghi chú ghim đứng đầu, dựng được từng dòng
        NotesWidgetService.Factory fac = new NotesWidgetService.Factory(c);
        fac.onDataSetChanged();
        RemoteViews row = fac.getViewAt(0);
        android.view.View rv = row.apply(c, new android.widget.FrameLayout(c));
        System.out.println("tiện ích ghi chú: " + fac.getCount() + " dòng, dòng 1 = " + ((android.widget.TextView) rv.findViewById(R.id.gi_title)).getText());
        RemoteViews w = NotesWidget.build(c, 1);
        w.apply(c, new android.widget.FrameLayout(c));
        android.appwidget.AppWidgetManager wm = android.appwidget.AppWidgetManager.getInstance(c);
        int wid = shadowOf(wm).createWidget(NotesWidget.class, R.layout.widget_ghichu);
        NotesWidget.refreshAll(c);

        // 4. Xóa từ cửa sổ ghi chú nhanh (chạm 2 lần)
        String newId = null;
        for (java.util.Iterator<String> it = notes.keys(); it.hasNext(); ) { String k = it.next(); if (!k.equals("a1")) newId = k; }
        q = Robolectric.buildActivity(QuickNoteActivity.class, new Intent().putExtra(QuickNoteActivity.EXTRA_ID, newId)).setup();
        ViewGroup root = (ViewGroup) ((ViewGroup) q.get().findViewById(android.R.id.content)).getChildAt(0);
        ViewGroup btnRow = (ViewGroup) root.getChildAt(root.getChildCount() - 1);
        btnRow.getChildAt(0).performClick(); btnRow.getChildAt(0).performClick();
        q.pause().stop().destroy();
        if (NoteStore.readRoot(c).getJSONObject("data").getJSONObject("notes").has(newId)) throw new AssertionError("xóa ghi chú nhanh sai");

        // 5. Liên kết "Google Drive" (dùng tệp thay cho Drive) và tự lưu
        File target = File.createTempFile("drive", ".json");
        ActivityController<MainActivity> mc = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity m = mc.get();
        Method onResult = MainActivity.class.getDeclaredMethod("onActivityResult", int.class, int.class, Intent.class);
        onResult.setAccessible(true);
        onResult.invoke(m, 43, Activity.RESULT_OK, new Intent().setData(Uri.fromFile(target)));
        DriveSync.IO.submit(new Runnable() { public void run() { } }).get();
        ShadowLooper.idleMainLooper();
        String onDisk = new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
        System.out.println("drive: " + DriveSync.status(c) + " · đã ghi " + onDisk.length() + " byte");
        if (!onDisk.contains("Trứng")) throw new AssertionError("chưa ghi được lên tệp liên kết");
        // thay đổi mới → tự ghi sau 3 giây
        Object bridge = shadowOf((WebView) ((ViewGroup) ((ViewGroup) m.findViewById(android.R.id.content)).getChildAt(0)).getChildAt(1)).getJavascriptInterface("AndroidBridge");
        Method save = bridge.getClass().getDeclaredMethod("saveData", String.class); save.setAccessible(true);
        save.invoke(bridge, "{\"app\":\"so-tay-lich-viet\",\"data\":{\"notes\":{\"z\":{\"id\":\"z\",\"body\":\"mới\"}}}}");
        ShadowLooper.idleMainLooper(4, java.util.concurrent.TimeUnit.SECONDS);
        DriveSync.IO.submit(new Runnable() { public void run() { } }).get();
        onDisk = new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
        if (!onDisk.contains("mới")) throw new AssertionError("không tự lưu sau khi đổi");
        // 6. Khôi phục: chọn tệp → gửi nội dung cho giao diện
        WebView web = (WebView) ((ViewGroup) ((ViewGroup) m.findViewById(android.R.id.content)).getChildAt(0)).getChildAt(1);
        onResult.invoke(m, 44, Activity.RESULT_OK, new Intent().setData(Uri.fromFile(target)));
        DriveSync.IO.submit(new Runnable() { public void run() { } }).get();
        ShadowLooper.idleMainLooper();
        List<String> js = new java.util.ArrayList<String>();
        String last = shadowOf(web).getLastEvaluatedJavascript();
        System.out.println("khôi phục gọi JS: " + (last == null ? "null" : last.substring(0, Math.min(60, last.length()))));
        // 7. Ghi chú sửa từ tiện ích khi app đang chạy nền → quay lại app thì nạp lại
        shadowOf(web).getWebViewClient().onPageFinished(web, "file:///android_asset/www/index.html");
        mc.pause();
        Thread.sleep(5);
        Reminders.prefs(c).edit().putLong("extWrite", System.currentTimeMillis()).apply();
        mc.resume();
        System.out.println("quay lại app gọi JS: " + shadowOf(web).getLastEvaluatedJavascript());
        if (!shadowOf(web).getLastEvaluatedJavascript().contains("sotayReload")) throw new AssertionError("không nạp lại sau khi sửa từ tiện ích");
        // 8. Liên kết mở ghi chú
        System.out.println("link ghi chú: " + MainActivity.linkFromIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("sotay://ghichu/a1"))));
        DriveSync.unlink(c);
        mc.pause().stop().destroy();
        System.out.println("OK notes+drive sdk " + android.os.Build.VERSION.SDK_INT);
    }

    @Test @Config(sdk = 36) public void api36() throws Exception { run(); }
    @Test @Config(sdk = 30) public void api30() throws Exception { run(); }
    @Test @Config(sdk = 26) public void api26() throws Exception { run(); }
}
