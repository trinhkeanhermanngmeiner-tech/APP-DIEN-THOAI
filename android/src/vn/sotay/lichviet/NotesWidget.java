/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

/**
 * Tiện ích Ghi chú: danh sách cuộn. Mỗi ghi chú có nút ghim và nút mở rộng; ghi chú thu gọn hiện phần cuối,
 * mở rộng thì hiện đủ nội dung và kéo danh sách để đọc. Chạm vào ghi chú để sửa trong cửa sổ nhỏ, ＋ để viết mới.
 */
public class NotesWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c, id));
    }

    /** Phần giao diện đổi theo theme và trạng thái (không đụng tới danh sách nên không làm nhảy vị trí cuộn). */
    static void applyStyle(Context c, RemoteViews v) {
        v.setInt(R.id.gc_root, "setBackgroundResource", Skins.background(c));
        v.setInt(R.id.gc_band, "setBackgroundResource", Skins.bandNotes(c));
        v.setImageViewResource(R.id.gc_all, NoteStore.expanded(c).isEmpty() ? R.drawable.ic_expand_all : R.drawable.ic_collapse_all);
    }

    static RemoteViews build(Context c, int widgetId) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_ghichu);
        applyStyle(c, v);
        Intent svc = new Intent(c, NotesWidgetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
        v.setRemoteAdapter(R.id.gc_list, svc);
        v.setEmptyView(R.id.gc_list, R.id.gc_empty);
        if (!Pro.isPro(c)) {
            // Bản miễn phí: tiện ích Ghi chú bị khóa, chạm vào đâu cũng mở màn hình mở khóa trong app.
            v.setTextViewText(R.id.gc_empty, "Tiện ích Ghi chú thuộc Bản Đầy Đủ.\nChạm để mở khóa, chỉ 20.000đ một lần.");
            PendingIntent buy = PendingIntent.getActivity(c, 34, paywallIntent(c), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            v.setOnClickPendingIntent(R.id.gc_empty, buy);
            v.setOnClickPendingIntent(R.id.gc_add, buy);
            v.setOnClickPendingIntent(R.id.gc_all, buy);
            v.setOnClickPendingIntent(R.id.gc_title, buy);
            return v;
        }
        v.setTextViewText(R.id.gc_empty, "Chưa có ghi chú.\nChạm ＋ để viết ghi chú đầu tiên.");
        // Một mẫu chung cho mọi dòng; từng nút gắn thêm mã ghi chú và hành động (fill-in), nên PendingIntent phải MUTABLE.
        Intent act = new Intent(c, WidgetActionActivity.class);
        v.setPendingIntentTemplate(R.id.gc_list, PendingIntent.getActivity(c, 30, act,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        Intent add = new Intent(c, QuickNoteActivity.class).setAction("vn.sotay.lichviet.GHI_CHU_MOI");
        PendingIntent addPi = PendingIntent.getActivity(c, 31, add, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.gc_add, addPi);
        v.setOnClickPendingIntent(R.id.gc_empty, addPi);
        Intent all = new Intent(c, WidgetActionActivity.class).putExtra(WidgetActionActivity.EXTRA_ACTION, WidgetActionActivity.ACT_ALL);
        v.setOnClickPendingIntent(R.id.gc_all, PendingIntent.getActivity(c, 33, all, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        Intent open = new Intent(c, MainActivity.class).setAction(Intent.ACTION_VIEW).setData(Uri.parse("sotay://ghichu/"))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.gc_title, PendingIntent.getActivity(c, 32, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        return v;
    }

    static Intent paywallIntent(Context c) {
        return new Intent(c, MainActivity.class).setAction(Intent.ACTION_VIEW).setData(Uri.parse("sotay://mokhoa"))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    /** Đổi trạng thái Bản Đầy Đủ: dựng lại toàn bộ tiện ích (nội dung, nút bấm, danh sách). */
    static void rebuildAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int id : ids(c)) m.updateAppWidget(id, build(c, id));
        refreshAll(c);
    }

    private static int[] ids(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        return m == null ? new int[0] : m.getAppWidgetIds(new ComponentName(c, NotesWidget.class));
    }

    /** Dữ liệu ghi chú thay đổi: nạp lại danh sách. */
    static void refreshAll(Context c) {
        int[] ids = ids(c);
        if (ids.length > 0) AppWidgetManager.getInstance(c).notifyAppWidgetViewDataChanged(ids, R.id.gc_list);
    }

    /** Theme hoặc trạng thái mở rộng đổi: cập nhật phần giao diện, giữ nguyên danh sách. */
    static void restyleAll(Context c) {
        int[] ids = ids(c);
        if (ids.length == 0) return;
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_ghichu);
        applyStyle(c, v);
        try {
            AppWidgetManager.getInstance(c).partiallyUpdateAppWidget(ids, v);
        } catch (Exception e) {
            for (int id : ids) AppWidgetManager.getInstance(c).updateAppWidget(id, build(c, id));
        }
    }
}
