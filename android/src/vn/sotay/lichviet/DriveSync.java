/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tự lưu dữ liệu vào một tệp người dùng chọn qua trình chọn tệp của Android (Storage Access Framework),
 * thường là một tệp trên Google Drive. Ghi gộp: chờ 3 giây sau lần thay đổi cuối rồi mới ghi.
 */
final class DriveSync {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static String pendingJson;
    private static Context appContext;

    private static final Runnable FLUSH = new Runnable() {
        @Override
        public void run() {
            final String json;
            final Context c;
            synchronized (DriveSync.class) {
                json = pendingJson;
                c = appContext;
                pendingJson = null;
            }
            if (json == null || c == null) return;
            IO.execute(new Runnable() {
                @Override
                public void run() {
                    writeNow(c, json);
                }
            });
        }
    };

    private DriveSync() {
    }

    static Uri uri(Context c) {
        String s = Reminders.prefs(c).getString("driveUri", null);
        return s == null ? null : Uri.parse(s);
    }

    static void schedule(Context c, String json) {
        if (uri(c) == null) return;
        synchronized (DriveSync.class) {
            pendingJson = json;
            appContext = c.getApplicationContext();
        }
        MAIN.removeCallbacks(FLUSH);
        MAIN.postDelayed(FLUSH, 3000);
    }

    /** Ghi ngay (gọi trên luồng nền). */
    static boolean writeNow(Context c, String json) {
        Uri u = uri(c);
        if (u == null) return false;
        try {
            OutputStream os;
            try {
                os = c.getContentResolver().openOutputStream(u, "wt");
            } catch (Exception e) {
                os = c.getContentResolver().openOutputStream(u, "w");
            }
            if (os == null) throw new IOException("không mở được tệp");
            try {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            } finally {
                os.close();
            }
            Reminders.prefs(c).edit().putLong("driveLast", System.currentTimeMillis()).putString("driveErr", "").apply();
            return true;
        } catch (SecurityException e) {
            Reminders.prefs(c).edit().putString("driveErr", "Mất quyền ghi vào tệp. Hãy ngắt rồi liên kết lại.").apply();
        } catch (Exception e) {
            Reminders.prefs(c).edit().putString("driveErr", "Chưa lưu được (" + e.getMessage() + "). Sẽ thử lại ở lần thay đổi sau.").apply();
        }
        return false;
    }

    static String read(Context c, Uri u) throws IOException {
        try (InputStream in = c.getContentResolver().openInputStream(u)) {
            if (in == null) throw new IOException("không mở được tệp");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /** Ghi nhớ tệp đã chọn và giữ quyền đọc/ghi lâu dài. */
    static boolean link(Context c, Uri u) {
        boolean persisted = true;
        try {
            c.getContentResolver().takePersistableUriPermission(u,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception e) {
            persisted = false;
        }
        Reminders.prefs(c).edit().putString("driveUri", u.toString()).putString("driveName", displayName(c, u))
                .putString("driveErr", persisted ? "" : "Tệp này không cho phép lưu lâu dài; có thể phải liên kết lại.").apply();
        return persisted;
    }

    static void unlink(Context c) {
        Uri u = uri(c);
        if (u != null) {
            try {
                c.getContentResolver().releasePersistableUriPermission(u,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception ignored) {
            }
        }
        Reminders.prefs(c).edit().remove("driveUri").remove("driveName").remove("driveLast").remove("driveErr").apply();
    }

    static String displayName(Context c, Uri u) {
        try (Cursor cur = c.getContentResolver().query(u, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                String n = cur.getString(0);
                if (n != null) return n;
            }
        } catch (Exception ignored) {
        }
        String last = u.getLastPathSegment();
        return last == null ? "tệp đã chọn" : last;
    }

    static String status(Context c) {
        JSONObject o = new JSONObject();
        try {
            Uri u = uri(c);
            o.put("linked", u != null);
            if (u != null) {
                String auth = u.getAuthority() == null ? "" : u.getAuthority();
                o.put("provider", auth.contains("com.google.android.apps.docs") ? "Google Drive" : "tệp đã chọn");
                o.put("name", Reminders.prefs(c).getString("driveName", ""));
                o.put("last", Reminders.prefs(c).getLong("driveLast", 0));
                o.put("err", Reminders.prefs(c).getString("driveErr", ""));
            }
        } catch (Exception ignored) {
        }
        return o.toString();
    }
}
