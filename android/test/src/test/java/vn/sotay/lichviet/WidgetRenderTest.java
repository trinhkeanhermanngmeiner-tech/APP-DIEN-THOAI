package vn.sotay.lichviet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.widget.FrameLayout;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/** Vẽ hai tiện ích ra ảnh PNG (target/widget-*.png) để xem bố cục. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
public class WidgetRenderTest {
    private static void render(View v, int wDp, int hDp, String file) throws Exception {
        float d = v.getResources().getDisplayMetrics().density;
        int w = (int) (wDp * d), h = (int) (hDp * d);
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(0xFF5B6B7A);
        v.draw(new Canvas(bmp));
        try (FileOutputStream out = new FileOutputStream(file)) { bmp.compress(Bitmap.CompressFormat.PNG, 100, out); }
    }

    @Test
    public void renderWidgets() throws Exception {
        Context c = RuntimeEnvironment.getApplication();
        Calendar k = Calendar.getInstance();
        StringBuilder days = new StringBuilder();
        Calendar d = Calendar.getInstance(); d.set(Calendar.DAY_OF_MONTH, 1); d.add(Calendar.DAY_OF_MONTH, -10);
        for (int i = 0; i < 60; i++) {
            String key = String.format(Locale.US, "%04d-%02d-%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1, d.get(Calendar.DAY_OF_MONTH));
            int ld = (i + 11) % 30 + 1; String f = ld == 1 || ld == 15 ? "m" : "";
            if (i == 22) f += "H"; if (i == 15) f += "e"; if (i == 18) f += "n"; if (i == 30) f += "en";
            if (days.length() > 0) days.append(',');
            days.append('"').append(key).append("\":\"").append(ld == 1 ? "1/9" : String.valueOf(ld)).append('|').append(f).append('"');
            d.add(Calendar.DAY_OF_MONTH, 1);
        }
        String today = String.format(Locale.US, "%04d-%02d-%02d", k.get(Calendar.YEAR), k.get(Calendar.MONTH) + 1, k.get(Calendar.DAY_OF_MONTH));
        String ym = String.format(Locale.US, "%04d-%02d", k.get(Calendar.YEAR), k.get(Calendar.MONTH) + 1);
        Reminders.prefs(c).edit()
                .putString("cal", "{\"days\":{" + days + "},\"months\":{\"" + ym + "\":\"Bính Ngọ · 21/8 – 22/9 âm\"}}")
                .putString("widget", "{\"" + today + "\":{\"l\":\"27 tháng Tám\",\"y\":\"Bính Ngọ\",\"n\":\"Còn 5 ngày: Giỗ ông nội · giỗ lần thứ 11\"}}")
                .apply();
        Reminders.prefs(c).edit().putString("widget", "{\"" + today + "\":{\"l\":\"27 tháng Tám\",\"y\":\"Bính Ngọ\",\"n\":\"Còn 5 ngày: Giỗ ông nội · giỗ lần thứ 11\"}}").apply();
        StringBuilder long1 = new StringBuilder();
        for (int i = 1; i <= 14; i++) long1.append(i > 1 ? "\n" : "").append("Ngày ").append(i).append(": bệnh nhân ổn định, theo dõi thêm");
        NoteStore.write(c, "{\"data\":{\"notes\":{"
                + "\"p\":{\"id\":\"p\",\"title\":\"Đi chợ Tết\",\"body\":\"- Lá dong\\n- Gạo nếp\\n- Đỗ xanh\",\"pinned\":true,\"color\":\"y\",\"updated\":500,\"date\":\"" + today + "\"},"
                + "\"a\":{\"id\":\"a\",\"title\":\"Nhật ký bệnh nhân\",\"body\":" + org.json.JSONObject.quote(long1.toString()) + ",\"pinned\":false,\"color\":\"b\",\"updated\":400},"
                + "\"b\":{\"id\":\"b\",\"title\":\"\",\"body\":\"Gọi bác Hùng\\nMua quà cho mẹ\",\"pinned\":false,\"color\":\"g\",\"updated\":300}}}}");
        java.util.Set<String> open = new java.util.HashSet<String>();
        open.add("p");
        Reminders.prefs(c).edit().putStringSet("wexp", open).apply();
        String[] ids = Skins.IDS;
        for (int i = 0; i < ids.length; i++) {
            Reminders.prefs(c).edit().putString("wskin", ids[i]).apply();
            render(LichMonthWidget.build(c).apply(c, new FrameLayout(c)), 320, 330, "target/widget-thang-" + ids[i] + ".png");
            render(LichWidget.build(c).apply(c, new FrameLayout(c)), 150, 160, "target/widget-gon-" + ids[i] + ".png");
            // Tiện ích Ghi chú: phần đầu + các dòng ghi chú (danh sách thật chỉ dựng được trên máy, nên ghép tay)
            FrameLayout shell = new FrameLayout(c);
            android.widget.LinearLayout col = new android.widget.LinearLayout(c);
            col.setOrientation(android.widget.LinearLayout.VERTICAL);
            View head = NotesWidget.build(c, 1).apply(c, new FrameLayout(c));
            android.view.ViewGroup hv = (android.view.ViewGroup) head;
            View list = hv.findViewById(R.id.gc_list);
            ((android.view.ViewGroup) list.getParent()).removeView(list);
            View empty = hv.findViewById(R.id.gc_empty);
            ((android.view.ViewGroup) empty.getParent()).removeView(empty);
            hv.setBackgroundResource(Skins.background(c));
            NotesWidgetService.Factory f = new NotesWidgetService.Factory(c);
            f.onDataSetChanged();
            android.widget.LinearLayout rows = new android.widget.LinearLayout(c);
            rows.setOrientation(android.widget.LinearLayout.VERTICAL);
            for (int n = 0; n < f.getCount(); n++) rows.addView(f.getViewAt(n).apply(c, rows));
            col.addView(head, new android.widget.LinearLayout.LayoutParams(-1, -2));
            col.addView(rows, new android.widget.LinearLayout.LayoutParams(-1, -2));
            col.setBackgroundResource(Skins.background(c));
            shell.addView(col);
            render(shell, 300, 400, "target/widget-ghichu-" + ids[i] + ".png");
        }
    }
}
