package vn.sotay.lichviet;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/** Tiện ích Ghi chú: ghim, mở rộng, xem phần cuối; theme màu; cửa sổ sửa nhanh. */
@RunWith(RobolectricTestRunner.class)
public class NotesFeatureTest {
    private static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }

    private static String lines(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= n; i++) sb.append(i > 1 ? "\n" : "").append("Dòng ").append(i);
        return sb.toString();
    }

    private void run() throws Exception {
        Pro.set(org.robolectric.RuntimeEnvironment.getApplication(), true);
        Context c = RuntimeEnvironment.getApplication();
        // --- tailPreview
        String t = NotesWidgetService.tailPreview(lines(20));
        System.out.println("tail(20 dòng) = " + t.replace("\n", " / "));
        check(t.startsWith("… Dòng 17") && t.endsWith("Dòng 20") && t.contains("Dòng 17") && !t.contains("Dòng 16"), "tail 20 dòng sai");
        check(NotesWidgetService.tailPreview("ngắn\nvừa").equals("ngắn\nvừa"), "ghi chú ngắn không được thêm …");
        StringBuilder longLine = new StringBuilder();
        for (int i = 0; i < 100; i++) longLine.append("abcdefghij");
        String tl = NotesWidgetService.tailPreview("đầu\n" + longLine);
        check(tl.startsWith("…") && tl.length() <= 241 && tl.endsWith("abcdefghij"), "dòng quá dài phải lấy phần cuối");
        check(NotesWidgetService.tailPreview("").isEmpty() && NotesWidgetService.tailPreview(null).isEmpty(), "rỗng");
        check(NotesWidgetService.headPreview(lines(20)).startsWith("Dòng 1\nDòng 2"), "head sai");

        // --- dữ liệu mẫu: 3 ghi chú, chưa ghim
        NoteStore.write(c, "{\"data\":{\"notes\":{"
                + "\"a\":{\"id\":\"a\",\"title\":\"Nhật ký\",\"body\":" + JSONObject.quote(lines(20)) + ",\"pinned\":false,\"color\":\"b\",\"updated\":300},"
                + "\"b\":{\"id\":\"b\",\"title\":\"\",\"body\":\"Mua sữa\\nMua bánh\",\"pinned\":false,\"color\":\"y\",\"updated\":200},"
                + "\"c\":{\"id\":\"c\",\"title\":\"Cũ\",\"body\":\"x\",\"pinned\":false,\"color\":\"\",\"updated\":100}}}}");
        NotesWidgetService.Factory f = new NotesWidgetService.Factory(c);
        f.onDataSetChanged();
        check(f.getCount() == 3, "phải có 3 dòng");
        View r0 = f.getViewAt(0).apply(c, new FrameLayout(c));
        check(((TextView) r0.findViewById(R.id.gi_title)).getText().toString().equals("Nhật ký"), "dòng đầu = ghi chú sửa gần nhất");
        String collapsed = ((TextView) r0.findViewById(R.id.gi_body)).getText().toString();
        check(collapsed.endsWith("Dòng 20") && r0.findViewById(R.id.gi_full).getVisibility() == View.GONE, "thu gọn phải hiện phần cuối");
        System.out.println("meta: " + ((TextView) r0.findViewById(R.id.gi_meta)).getText());

        // --- ghim ghi chú "c" từ tiện ích → đứng đầu danh sách
        Robolectric.buildActivity(WidgetActionActivity.class, new Intent().putExtra(QuickNoteActivity.EXTRA_ID, "c")
                .putExtra(WidgetActionActivity.EXTRA_ACTION, WidgetActionActivity.ACT_PIN)).create().get();
        f.onDataSetChanged();
        View pinned = f.getViewAt(0).apply(c, new FrameLayout(c));
        check(((TextView) pinned.findViewById(R.id.gi_title)).getText().toString().equals("Cũ"), "ghi chú đã ghim phải lên đầu");
        check(NoteStore.readRoot(c).getJSONObject("data").getJSONObject("notes").getJSONObject("c").optLong("updated") == 100, "ghim không được đổi giờ sửa");
        check(((TextView) pinned.findViewById(R.id.gi_meta)).getText().toString().startsWith("Đã ghim"), "meta phải ghi Đã ghim");
        Robolectric.buildActivity(WidgetActionActivity.class, new Intent().putExtra(QuickNoteActivity.EXTRA_ID, "c")
                .putExtra(WidgetActionActivity.EXTRA_ACTION, WidgetActionActivity.ACT_PIN)).create().get();
        f.onDataSetChanged();
        check(((TextView) f.getViewAt(0).apply(c, new FrameLayout(c)).findViewById(R.id.gi_title)).getText().toString().equals("Nhật ký"), "bỏ ghim phải trả về thứ tự cũ");

        // --- mở rộng một ghi chú: hiện đủ nội dung
        Robolectric.buildActivity(WidgetActionActivity.class, new Intent().putExtra(QuickNoteActivity.EXTRA_ID, "a")
                .putExtra(WidgetActionActivity.EXTRA_ACTION, WidgetActionActivity.ACT_TOGGLE)).create().get();
        f.onDataSetChanged();
        View open = f.getViewAt(0).apply(c, new FrameLayout(c));
        String full = ((TextView) open.findViewById(R.id.gi_full)).getText().toString();
        check(open.findViewById(R.id.gi_full).getVisibility() == View.VISIBLE && open.findViewById(R.id.gi_body).getVisibility() == View.GONE, "mở rộng phải hiện gi_full");
        check(full.startsWith("Dòng 1\n") && full.endsWith("Dòng 20"), "mở rộng phải hiện đủ nội dung");
        // ghi chú khác vẫn thu gọn
        check(f.getViewAt(1).apply(c, new FrameLayout(c)).findViewById(R.id.gi_full).getVisibility() == View.GONE, "ghi chú khác vẫn thu gọn");
        // mở rộng tất cả / thu gọn tất cả
        Robolectric.buildActivity(WidgetActionActivity.class, new Intent().putExtra(WidgetActionActivity.EXTRA_ACTION, WidgetActionActivity.ACT_ALL)).create().get();
        check(NoteStore.expanded(c).isEmpty(), "đang có ghi chú mở rộng thì 'tất cả' = thu gọn hết");
        Robolectric.buildActivity(WidgetActionActivity.class, new Intent().putExtra(WidgetActionActivity.EXTRA_ACTION, WidgetActionActivity.ACT_ALL)).create().get();
        check(NoteStore.expanded(c).size() == 3, "không có ghi chú mở rộng thì 'tất cả' = mở hết");
        Robolectric.buildActivity(WidgetActionActivity.class, new Intent().putExtra(WidgetActionActivity.EXTRA_ACTION, WidgetActionActivity.ACT_ALL)).create().get();

        // --- tắt "xem phần cuối trước": thu gọn hiện phần đầu
        Reminders.prefs(c).edit().putBoolean("tailFirst", false).apply();
        f.onDataSetChanged();
        check(((TextView) f.getViewAt(0).apply(c, new FrameLayout(c)).findViewById(R.id.gi_body)).getText().toString().startsWith("Dòng 1"), "tắt tailFirst phải hiện phần đầu");
        Reminders.prefs(c).edit().putBoolean("tailFirst", true).apply();

        // --- cửa sổ sửa nhanh: ghim + sửa
        ActivityController<QuickNoteActivity> q = Robolectric.buildActivity(QuickNoteActivity.class, new Intent().putExtra(QuickNoteActivity.EXTRA_ID, "b")).setup();
        ViewGroup root = (ViewGroup) ((ViewGroup) q.get().findViewById(android.R.id.content)).getChildAt(0);
        EditText body = null;
        for (int i = 0; i < root.getChildCount(); i++) if (root.getChildAt(i) instanceof EditText) body = (EditText) root.getChildAt(i);
        check(body.getSelectionStart() == body.getText().length(), "caret phải ở cuối");
        ViewGroup row = (ViewGroup) root.getChildAt(root.getChildCount() - 1);
        row.getChildAt(0).performClick(); // nút Ghim
        q.pause().stop().destroy();
        JSONObject b = NoteStore.readRoot(c).getJSONObject("data").getJSONObject("notes").getJSONObject("b");
        check(b.optBoolean("pinned") && b.optLong("updated") == 200, "ghim trong cửa sổ nhanh phải lưu, không đổi giờ sửa");

        // --- theme: mọi theme dựng được cả 3 tiện ích
        for (String id : Skins.IDS) {
            Reminders.prefs(c).edit().putString("wskin", id).apply();
            check(Skins.IDS[Skins.current(c)].equals(id), "chọn theme " + id);
            LichWidget.build(c).apply(c, new FrameLayout(c));
            LichMonthWidget.build(c).apply(c, new FrameLayout(c));
            NotesWidget.build(c, 1).apply(c, new FrameLayout(c));
            NotesWidget.restyleAll(c);
        }
        Reminders.prefs(c).edit().putString("wskin", "auto").putString("skin", "cham").apply();
        check(Skins.IDS[Skins.current(c)].equals("cham"), "'Theo app' phải theo theme của app");
        System.out.println("OK features sdk " + android.os.Build.VERSION.SDK_INT);
    }

    @Test @Config(sdk = 36) public void api36() throws Exception { run(); }
    @Test @Config(sdk = 30) public void api30() throws Exception { run(); }
    @Test @Config(sdk = 26) public void api26() throws Exception { run(); }
}
