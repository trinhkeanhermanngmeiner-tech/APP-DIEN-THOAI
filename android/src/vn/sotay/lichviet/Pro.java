/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.content.Context;

import java.security.MessageDigest;

/**
 * Trạng thái "Bản Đầy Đủ". Bản miễn phí chỉ có lịch âm dương; các tính năng còn lại cần mở khóa một lần.
 * Giá trị được lưu cục bộ để dùng được khi không có mạng; Google Play xác nhận lại mỗi lần mở app.
 */
final class Pro {
    private Pro() {
    }

    static boolean isPro(Context c) {
        return Reminders.prefs(c).getBoolean("pro", false);
    }

    static void set(Context c, boolean value) {
        boolean old = isPro(c);
        Reminders.prefs(c).edit().putBoolean("pro", value).apply();
        if (old != value) Widgets.rebuildAll(c);
    }

    /** Mã mở khóa thử: chỉ có tác dụng ở bản APK cài trực tiếp (BuildInfo.DEV). */
    static boolean devUnlock(Context c, String code) {
        if (!BuildInfo.DEV || code == null) return false;
        if (!sha256("sotay-dev:" + code.trim()).equals(BuildInfo.DEV_CODE_HASH)) return false;
        set(c, true);
        return true;
    }

    static void devLock(Context c) {
        if (BuildInfo.DEV) set(c, false);
    }

    static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
