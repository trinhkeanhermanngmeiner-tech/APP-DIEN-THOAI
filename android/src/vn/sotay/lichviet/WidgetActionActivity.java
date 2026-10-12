/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Nhận các lần chạm trong tiện ích Ghi chú và không hiện giao diện:
 * ghim / bỏ ghim, mở rộng / thu gọn một ghi chú hoặc tất cả, hoặc mở cửa sổ sửa nhanh.
 */
public class WidgetActionActivity extends Activity {
    static final String EXTRA_ACTION = "vn.sotay.lichviet.ACTION";
    static final String ACT_EDIT = "edit", ACT_PIN = "pin", ACT_TOGGLE = "toggle", ACT_ALL = "all";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent in = getIntent();
        String act = in.getStringExtra(EXTRA_ACTION);
        String id = in.getStringExtra(QuickNoteActivity.EXTRA_ID);
        if (act == null) act = ACT_EDIT;
        if (ACT_EDIT.equals(act)) {
            startActivity(new Intent(this, QuickNoteActivity.class).putExtra(QuickNoteActivity.EXTRA_ID, id));
        } else if (ACT_PIN.equals(act) && id != null) {
            NoteStore.togglePin(this, id);
        } else if (ACT_TOGGLE.equals(act) && id != null) {
            NoteStore.toggleExpanded(this, id);
            NotesWidget.restyleAll(this);
            NotesWidget.refreshAll(this);
        } else if (ACT_ALL.equals(act)) {
            List<String> ids = new ArrayList<String>();
            for (org.json.JSONObject n : NotesWidgetService.Factory.load(this)) ids.add(n.optString("id"));
            NoteStore.toggleAllExpanded(this, ids);
            NotesWidget.restyleAll(this);
            NotesWidget.refreshAll(this);
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
