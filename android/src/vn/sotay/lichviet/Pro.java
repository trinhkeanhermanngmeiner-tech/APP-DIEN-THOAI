/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.content.Context;
import android.content.SharedPreferences;

import java.security.MessageDigest;

/**
 * Trạng thái "Bản Đầy Đủ": đã mua, hoặc đang trong 14 ngày dùng thử. Bản miễn phí chỉ có lịch âm dương.
 *
 * Ngày bắt đầu dùng thử được ghi vào tệp tùy chọn riêng "sotay_trial" (nằm trong sao lưu tự động của Android,
 * nên cài lại bằng cùng tài khoản Google vẫn nhớ) và vào tệp dữ liệu sao lưu của người dùng (Google Drive).
 * Đồng hồ không được lùi: thời điểm "bây giờ" luôn là mốc lớn nhất từng thấy.
 */
final class Pro {
    static final int TRIAL_DAYS = 14;
    static final long DAY = 24L * 60 * 60 * 1000;

    private Pro() {
    }

    /** Tùy chọn của phần dùng thử; được đưa vào sao lưu tự động (xem res/xml/backup_rules.xml). */
    static SharedPreferences tprefs(Context c) {
        return c.getSharedPreferences("sotay_trial", Context.MODE_PRIVATE);
    }

    /** Thời điểm hiện tại, nhưng không bao giờ nhỏ hơn mốc đã thấy trước đó (chống chỉnh lùi đồng hồ). */
    static long now(Context c) {
        long now = System.currentTimeMillis();
        SharedPreferences p = tprefs(c);
        long seen = p.getLong("lastSeen", 0);
        if (now < seen) return seen;
        if (now - seen > 60 * 1000) p.edit().putLong("lastSeen", now).apply();
        return now;
    }

    static boolean purchased(Context c) {
        return Reminders.prefs(c).getBoolean("pro", false);
    }

    /** Ngày bắt đầu dùng thử (mili giây), 0 nếu chưa dùng thử. */
    static long trialStart(Context c) {
        return tprefs(c).getLong("trialStart", 0);
    }

    static boolean trialActive(Context c) {
        long s = trialStart(c);
        return s > 0 && now(c) < s + TRIAL_DAYS * DAY;
    }

    /** Số ngày dùng thử còn lại (làm tròn lên), 0 nếu hết hạn hoặc chưa bắt đầu. */
    static int trialLeftDays(Context c) {
        long s = trialStart(c);
        if (s <= 0) return 0;
        long left = s + TRIAL_DAYS * DAY - now(c);
        return left <= 0 ? 0 : (int) ((left + DAY - 1) / DAY);
    }

    /** Có được dùng các tính năng trả phí không: đã mua hoặc đang dùng thử. */
    static boolean isPro(Context c) {
        return purchased(c) || trialActive(c);
    }

    /** Đặt trạng thái đã mua (Google Play xác nhận). */
    static void set(Context c, boolean value) {
        boolean old = isPro(c);
        Reminders.prefs(c).edit().putBoolean("pro", value).apply();
        if (old != isPro(c)) Widgets.rebuildAll(c);
    }

    /** Bắt đầu 14 ngày dùng thử. Chỉ được một lần; không có tác dụng nếu đã mua. */
    static boolean startTrial(Context c) {
        if (purchased(c) || trialStart(c) > 0) return false;
        long now = now(c);
        tprefs(c).edit().putLong("trialStart", now).putLong("lastSeen", now)
                .putBoolean("trialWarn", false).putBoolean("trialEnd", false).apply();
        Reminders.scheduleNext(c);
        Widgets.rebuildAll(c);
        return true;
    }

    /**
     * Gộp ngày bắt đầu dùng thử đọc từ bản sao lưu (Google Drive): lấy ngày sớm nhất, nên cài lại rồi nhập
     * sao lưu cũ không làm dùng thử bắt đầu lại. Ngày ở tương lai bị bỏ qua.
     */
    static boolean mergeTrial(Context c, long start) {
        long now = now(c);
        if (start <= 0 || start > now + DAY) return false;
        long cur = trialStart(c);
        if (cur > 0 && cur <= start) return false;
        boolean old = isPro(c);
        tprefs(c).edit().putLong("trialStart", start).apply();
        Reminders.scheduleNext(c);
        if (old != isPro(c)) Widgets.rebuildAll(c);
        return true;
    }

    /** Chỉ dành cho APK dùng thử: xóa dữ liệu dùng thử để thử lại. */
    static void devResetTrial(Context c) {
        if (!BuildInfo.DEV) return;
        boolean old = isPro(c);
        tprefs(c).edit().clear().apply();
        if (old != isPro(c)) Widgets.rebuildAll(c);
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
