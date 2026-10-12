/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.content.Context;

/** Làm mới đồng loạt các tiện ích (ngày mới, đổi theme, dữ liệu thay đổi). */
final class Widgets {
    private Widgets() {
    }

    static void refreshAll(Context c) {
        LichWidget.refreshAll(c);
        LichMonthWidget.refreshAll(c);
        NotesWidget.restyleAll(c);
        NotesWidget.refreshAll(c);
    }
}
