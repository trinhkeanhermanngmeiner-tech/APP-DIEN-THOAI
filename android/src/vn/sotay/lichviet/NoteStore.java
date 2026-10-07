/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

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

    /** Sau mỗi lần dữ liệu đổi: tự lưu lên Google Drive (nếu đã liên kết) và làm mới tiện ích ghi chú. */
    static void changed(Context c, String json, boolean fromOutsideApp) {
        DriveSync.schedule(c, json);
        NotesWidget.refreshAll(c);
        if (fromOutsideApp) Reminders.prefs(c).edit().putLong("extWrite", System.currentTimeMillis()).apply();
    }
}
