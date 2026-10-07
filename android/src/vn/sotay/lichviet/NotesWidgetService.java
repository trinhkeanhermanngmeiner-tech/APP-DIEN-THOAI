/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.content.Context;
import android.content.Intent;
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

/** Cung cấp các dòng cho danh sách ghi chú của tiện ích (ghim lên đầu, rồi mới sửa gần nhất). */
public class NotesWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    static final class Factory implements RemoteViewsFactory {
        private final Context c;
        private final List<JSONObject> notes = new ArrayList<JSONObject>();

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
            notes.addAll(load(c));
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
            JSONObject n = notes.get(i);
            String title = n.optString("title").trim(), body = n.optString("body").trim();
            if (title.isEmpty()) {
                int nl = body.indexOf('\n');
                title = nl < 0 ? body : body.substring(0, nl);
                body = nl < 0 ? "" : body.substring(nl + 1).trim();
            }
            v.setTextViewText(R.id.gi_title, title.isEmpty() ? "(Ghi chú trống)" : title);
            v.setTextViewText(R.id.gi_body, body);
            StringBuilder meta = new StringBuilder();
            if (n.optBoolean("pinned")) meta.append("Đã ghim · ");
            String date = n.optString("date");
            if (date.length() == 10) meta.append("Ngày ").append(date.substring(8)).append('/').append(date.substring(5, 7)).append(" · ");
            meta.append("Sửa ").append(new SimpleDateFormat("dd/MM HH:mm", Locale.US).format(new Date(n.optLong("updated"))));
            v.setTextViewText(R.id.gi_meta, meta.toString());
            v.setInt(R.id.gi_root, "setBackgroundResource", background(n.optString("color")));
            v.setOnClickFillInIntent(R.id.gi_root, new Intent().putExtra(QuickNoteActivity.EXTRA_ID, n.optString("id")));
            return v;
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
