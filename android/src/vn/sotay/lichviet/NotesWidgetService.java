/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Cung cấp các dòng cho danh sách ghi chú của tiện ích (ghim lên đầu, rồi mới sửa gần nhất).
 * Ghi chú thu gọn hiện phần cuối của nội dung; ghi chú mở rộng hiện đủ nội dung.
 */
public class NotesWidgetService extends RemoteViewsService {
    /** Số ký tự tối đa của một ghi chú mở rộng (giữ gói dữ liệu gửi tới tiện ích nhỏ gọn). */
    static final int FULL_LIMIT = 6000;
    private static final int TAIL_CHARS = 180, TAIL_LINES = 4;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    /** Vài dòng cuối của ghi chú, có dấu "…" ở đầu nếu đã bỏ bớt phần trước. */
    static String tailPreview(String body) {
        String b = body == null ? "" : body.replaceAll("\\s+$", "");
        if (b.isEmpty()) return "";
        String[] lines = b.split("\n", -1);
        ArrayList<String> out = new ArrayList<String>();
        int len = 0;
        boolean chopped = false;
        for (int i = lines.length - 1; i >= 0 && out.size() < TAIL_LINES; i--) {
            String ln = lines[i];
            if (ln.length() > TAIL_CHARS) {
                if (out.isEmpty()) {
                    out.add("…" + ln.substring(ln.length() - TAIL_CHARS));
                    chopped = true;
                }
                break;
            }
            if (len + ln.length() > TAIL_CHARS && !out.isEmpty()) break;
            out.add(0, ln);
            len += ln.length();
        }
        StringBuilder sb = new StringBuilder();
        // Dấu "…" nằm ngay đầu dòng đầu tiên (không chiếm một dòng riêng) để dòng cuối luôn còn chỗ hiển thị.
        if (!chopped && out.size() < lines.length) sb.append("… ");
        for (int k = 0; k < out.size(); k++) {
            if (k > 0) sb.append('\n');
            sb.append(out.get(k));
        }
        return sb.toString();
    }

    /** Vài dòng đầu của ghi chú (khi tắt chế độ xem phần cuối). */
    static String headPreview(String body) {
        String b = body == null ? "" : body.trim();
        String[] lines = b.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length && i < TAIL_LINES; i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines[i]);
        }
        String r = sb.toString();
        return r.length() > TAIL_CHARS ? r.substring(0, TAIL_CHARS) + "…" : r;
    }

    static final class Factory implements RemoteViewsFactory {
        private final Context c;
        private final List<JSONObject> notes = new ArrayList<JSONObject>();
        private Set<String> expanded;
        private boolean tailFirst = true;

        Factory(Context c) {
            this.c = c;
        }

        static List<JSONObject> load(Context c) {
            List<JSONObject> out = new ArrayList<JSONObject>();
            try {
                JSONObject all = NoteStore.readRoot(c).getJSONObject("data").getJSONObject("notes");
                Iterator<String> it = all.keys();
                while (it.hasNext()) {
                    JSONObject n = all.optJSONObject(it.next());
                    if (n != null) out.add(n);
                }
            } catch (Exception ignored) {
            }
            Collections.sort(out, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject a, JSONObject b) {
                    boolean pa = a.optBoolean("pinned"), pb = b.optBoolean("pinned");
                    if (pa != pb) return pa ? -1 : 1;
                    return Long.compare(b.optLong("updated"), a.optLong("updated"));
                }
            });
            return out.size() > 50 ? new ArrayList<JSONObject>(out.subList(0, 50)) : out;
        }

        static int background(String color) {
            if ("y".equals(color)) return R.drawable.note_y;
            if ("r".equals(color)) return R.drawable.note_r;
            if ("g".equals(color)) return R.drawable.note_g;
            if ("b".equals(color)) return R.drawable.note_b;
            if ("v".equals(color)) return R.drawable.note_v;
            return R.drawable.note_w;
        }

        @Override
        public void onCreate() {
        }

        @Override
        public void onDataSetChanged() {
            notes.clear();
            if (Pro.isPro(c)) notes.addAll(load(c));
            expanded = NoteStore.expanded(c);
            tailFirst = Reminders.prefs(c).getBoolean("tailFirst", true);
        }

        @Override
        public void onDestroy() {
            notes.clear();
        }

        @Override
        public int getCount() {
            return notes.size();
        }

        @Override
        public RemoteViews getViewAt(int i) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_ghichu_item);
            if (i >= notes.size()) return v;
            if (expanded == null) onDataSetChanged();
            JSONObject n = notes.get(i);
            String id = n.optString("id");
            boolean open = expanded.contains(id), pinned = n.optBoolean("pinned");
            String title = n.optString("title").trim(), body = n.optString("body");
            String content = body;
            if (title.isEmpty()) {
                // Không có tiêu đề: dòng đầu của nội dung làm tiêu đề, phần còn lại là nội dung.
                String t = body.trim();
                int nl = t.indexOf('\n');
                title = nl < 0 ? t : t.substring(0, nl);
                content = nl < 0 ? "" : t.substring(nl + 1);
            }
            boolean truncatedFull = false;
            if (open) {
                if (content.length() > FULL_LIMIT) {
                    content = content.substring(content.length() - FULL_LIMIT);
                    truncatedFull = true;
                }
                v.setViewVisibility(R.id.gi_body, View.GONE);
                v.setViewVisibility(R.id.gi_full, View.VISIBLE);
                v.setTextViewText(R.id.gi_full, (truncatedFull ? "…\n" : "") + content.replaceAll("\\s+$", ""));
            } else {
                v.setViewVisibility(R.id.gi_full, View.GONE);
                v.setViewVisibility(R.id.gi_body, View.VISIBLE);
                v.setTextViewText(R.id.gi_body, tailFirst ? tailPreview(content) : headPreview(content));
            }
            v.setTextViewText(R.id.gi_title, title.isEmpty() ? "(Ghi chú trống)" : title);
            StringBuilder meta = new StringBuilder();
            if (pinned) meta.append("Đã ghim · ");
            String date = n.optString("date");
            if (date.length() == 10) meta.append("Ngày ").append(date.substring(8)).append('/').append(date.substring(5, 7)).append(" · ");
            meta.append("Sửa ").append(new SimpleDateFormat("dd/MM HH:mm", Locale.US).format(new Date(n.optLong("updated"))));
            if (!open && content.trim().length() > TAIL_CHARS) meta.append(" · đang hiện phần cuối");
            v.setTextViewText(R.id.gi_meta, meta.toString());
            v.setInt(R.id.gi_root, "setBackgroundResource", background(n.optString("color")));
            v.setImageViewResource(R.id.gi_pin, pinned ? R.drawable.ic_pin_on : R.drawable.ic_pin_off);
            v.setImageViewResource(R.id.gi_toggle, open ? R.drawable.ic_collapse : R.drawable.ic_expand);
            v.setOnClickFillInIntent(R.id.gi_root, fill(id, WidgetActionActivity.ACT_EDIT));
            v.setOnClickFillInIntent(R.id.gi_pin, fill(id, WidgetActionActivity.ACT_PIN));
            v.setOnClickFillInIntent(R.id.gi_toggle, fill(id, WidgetActionActivity.ACT_TOGGLE));
            return v;
        }

        private static Intent fill(String id, String action) {
            return new Intent().putExtra(QuickNoteActivity.EXTRA_ID, id).putExtra(WidgetActionActivity.EXTRA_ACTION, action);
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int i) {
            return i < notes.size() ? notes.get(i).optString("id").hashCode() : i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
