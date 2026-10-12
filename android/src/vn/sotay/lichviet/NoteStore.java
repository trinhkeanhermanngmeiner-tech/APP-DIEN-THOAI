/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Tệp dữ liệu chung (so-tay.json) cho giao diện web, cửa sổ ghi chú nhanh và tiện ích. */
final class NoteStore {
    private NoteStore() {
    }

    static File file(Context c) {
        return new File(c.getFilesDir(), "so-tay.json");
    }

    static synchronized String read(Context c) {
        File f = file(c);
        if (!f.exists()) return "";
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int off = 0;
            while (off < buf.length) {
                int n = in.read(buf, off, buf.length - off);
                if (n < 0) break;
                off += n;
            }
            return new String(buf, 0, off, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    static synchronized boolean write(Context c, String json) {
        File tmp = new File(c.getFilesDir(), "so-tay.json.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch (Exception e) {
            return false;
        }
        return tmp.renameTo(file(c));
    }

    /** Toàn bộ dữ liệu dạng JSON; luôn có data.notes. */
    static JSONObject readRoot(Context c) {
        JSONObject root;
        try {
            String s = read(c);
            root = s.isEmpty() ? new JSONObject() : new JSONObject(s);
        } catch (Exception e) {
            root = new JSONObject();
        }
        try {
            if (root.optJSONObject("data") == null) root.put("data", new JSONObject());
            JSONObject data = root.getJSONObject("data");
            if (data.optJSONObject("notes") == null) data.put("notes", new JSONObject());
            if (!root.has("app")) root.put("app", "so-tay-lich-viet");
        } catch (Exception ignored) {
        }
        return root;
    }

    /** Đổi trạng thái ghim của ghi chú (không đổi giờ sửa để thứ tự không xáo trộn). */
    static boolean togglePin(Context c, String id) {
        try {
            JSONObject root = readRoot(c);
            JSONObject n = root.getJSONObject("data").getJSONObject("notes").optJSONObject(id);
            if (n == null) return false;
            n.put("pinned", !n.optBoolean("pinned"));
            String json = root.toString();
            if (!write(c, json)) return false;
            changed(c, json, true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Các ghi chú đang mở rộng trong tiện ích. */
    static Set<String> expanded(Context c) {
        return new HashSet<String>(Reminders.prefs(c).getStringSet("wexp", new HashSet<String>()));
    }

    static void toggleExpanded(Context c, String id) {
        Set<String> s = expanded(c);
        if (!s.remove(id)) s.add(id);
        Reminders.prefs(c).edit().putStringSet("wexp", s).apply();
    }

    /** Mở rộng tất cả, hoặc thu gọn tất cả nếu đang có ghi chú mở rộng. */
    static void toggleAllExpanded(Context c, Collection<String> allIds) {
        Set<String> next = new HashSet<String>();
        if (expanded(c).isEmpty()) next.addAll(allIds);
        Reminders.prefs(c).edit().putStringSet("wexp", next).apply();
    }

    /** Sau mỗi lần dữ liệu đổi: tự lưu lên Google Drive (nếu đã liên kết) và làm mới tiện ích ghi chú. */
    static void changed(Context c, String json, boolean fromOutsideApp) {
        DriveSync.schedule(c, json);
        NotesWidget.refreshAll(c);
        if (fromOutsideApp) Reminders.prefs(c).edit().putLong("extWrite", System.currentTimeMillis()).apply();
    }
}
