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

/** Tiện ích Ghi chú: danh sách cuộn được; chạm vào ghi chú để sửa ngay trong cửa sổ nhỏ, ＋ để viết mới. */
public class NotesWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) m.updateAppWidget(id, build(c, id));
    }

    static RemoteViews build(Context c, int widgetId) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_ghichu);
        Intent svc = new Intent(c, NotesWidgetService.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
        v.setRemoteAdapter(R.id.gc_list, svc);
        v.setEmptyView(R.id.gc_list, R.id.gc_empty);
        // Mẫu cho từng dòng: mỗi dòng gắn thêm mã ghi chú (fill-in), nên PendingIntent phải MUTABLE.
        Intent edit = new Intent(c, QuickNoteActivity.class);
        v.setPendingIntentTemplate(R.id.gc_list, PendingIntent.getActivity(c, 30, edit,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        Intent add = new Intent(c, QuickNoteActivity.class).setAction("vn.sotay.lichviet.GHI_CHU_MOI");
        PendingIntent addPi = PendingIntent.getActivity(c, 31, add, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.gc_add, addPi);
        v.setOnClickPendingIntent(R.id.gc_empty, addPi);
        Intent open = new Intent(c, MainActivity.class).setAction(Intent.ACTION_VIEW).setData(Uri.parse("sotay://ghichu/"))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        v.setOnClickPendingIntent(R.id.gc_title, PendingIntent.getActivity(c, 32, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        return v;
    }

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        if (m == null) return;
        int[] ids = m.getAppWidgetIds(new ComponentName(c, NotesWidget.class));
        if (ids == null || ids.length == 0) return;
        m.notifyAppWidgetViewDataChanged(ids, R.id.gc_list);
    }
}
