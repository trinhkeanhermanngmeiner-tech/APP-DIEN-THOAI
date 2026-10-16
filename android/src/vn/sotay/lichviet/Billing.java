/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.app.Activity;
import android.content.Context;

/**
 * Cầu nối thanh toán. Bản Google Play dùng PlayBilling (thư viện Play Billing, nằm trong billing/ và chỉ được
 * biên dịch ở quy trình build dành cho Google Play); bản APK cài trực tiếp dùng bản giả không thanh toán.
 */
abstract class Billing {
    private static Billing instance;
    protected Context app;
    protected Runnable listener;

    static synchronized Billing get(Context c) {
        if (instance == null) {
            Billing b;
            try {
                b = (Billing) Class.forName("vn.sotay.lichviet.PlayBilling").getDeclaredConstructor().newInstance();
            } catch (Throwable t) {
                b = new StubBilling();
            }
            b.app = c.getApplicationContext();
            instance = b;
        }
        return instance;
    }

    /** Gọi khi trạng thái mở khóa, giá hoặc thông báo lỗi thay đổi. */
    final void setListener(Runnable r) {
        listener = r;
    }

    protected final void changed() {
        Runnable r = listener;
        if (r != null) r.run();
    }

    /** Kết nối Google Play, tải giá và xác nhận lại các giao dịch đã mua. */
    abstract void connect(Activity a);

    abstract void buy(Activity a);

    /** Khôi phục giao dịch đã mua (cài lại app, đổi máy). */
    abstract void restore(Activity a);

    /** Giá đã định dạng theo tiền tệ của người dùng, hoặc "" khi chưa có. */
    abstract String price();

    abstract String error();

    /** true nếu app đang nối với Google Play (có thể thanh toán). */
    abstract boolean available();

    /** Bản giả cho APK cài trực tiếp: không thanh toán, chỉ mở khóa bằng mã thử. */
    static final class StubBilling extends Billing {
        @Override
        void connect(Activity a) {
        }

        @Override
        void buy(Activity a) {
        }

        @Override
        void restore(Activity a) {
        }

        @Override
        String price() {
            return "";
        }

        @Override
        String error() {
            return BuildInfo.DEV ? "" : "Chưa kết nối được Google Play.";
        }

        @Override
        boolean available() {
            return false;
        }
    }
}
