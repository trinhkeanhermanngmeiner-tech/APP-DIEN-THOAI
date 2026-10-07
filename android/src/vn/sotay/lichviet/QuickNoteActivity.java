/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/**
 * Cửa sổ ghi chú nhỏ hiện ngay trên màn hình chính khi chạm vào tiện ích Ghi chú:
 * xem, sửa, xóa hoặc viết ghi chú mới mà không cần mở cả ứng dụng. Ghi thẳng vào tệp dữ liệu chung.
 */
public class QuickNoteActivity extends Activity {
    static final String EXTRA_ID = "vn.sotay.lichviet.NOTE_ID";
    private String noteId;
    private JSONObject original;
    private EditText title, body;
    private boolean deleteArmed, deleted;

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        noteId = getIntent().getStringExtra(EXTRA_ID);
        if (noteId != null) {
            JSONObject notes = notes(NoteStore.readRoot(this));
            original = notes == null ? null : notes.optJSONObject(noteId);
            if (original == null) noteId = null;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(12));

        TextView head = new TextView(this);
        head.setText(noteId == null ? "Ghi chú mới" : "Ghi chú");
        head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        head.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(head);
        String date = original == null ? "" : original.optString("date");
        if (date.length() == 10) {
            TextView d = new TextView(this);
            d.setText("Gắn với ngày " + date.substring(8) + "/" + date.substring(5, 7) + "/" + date.substring(0, 4));
            d.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            d.setAlpha(0.7f);
            root.addView(d);
        }

        title = new EditText(this);
        title.setHint("Tiêu đề");
        title.setSingleLine(true);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body = new EditText(this);
        body.setHint("Nội dung ghi chú…");
        body.setMinLines(5);
        body.setMaxLines(12);
        body.setGravity(Gravity.TOP | Gravity.START);
        body.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (original != null) {
            title.setText(original.optString("title"));
            body.setText(original.optString("body"));
        }

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, 0);
        if (original != null) {
            final Button del = flatButton("Xóa");
            del.setTextColor(0xFFC2261F);
            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!deleteArmed) {
                        deleteArmed = true;
                        del.setText("Chạm lần nữa để xóa");
                        return;
                    }
                    delete();
                }
            });
            row.addView(del);
            Button open = flatButton("Mở trong app");
            open.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    save(false);
                    startActivity(new Intent(QuickNoteActivity.this, MainActivity.class).setAction(Intent.ACTION_VIEW)
                            .setData(Uri.parse("sotay://ghichu/" + noteId))
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                }
            });
            row.addView(open);
        }
        View space = new View(this);
        row.addView(space, new LinearLayout.LayoutParams(0, 1, 1f));
        Button saveBtn = flatButton("Lưu");
        saveBtn.setTextColor(0xFFC2261F);
        saveBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save(true);
                finish();
            }
        });
        row.addView(saveBtn);
        root.addView(row);
        setContentView(root);
        body.requestFocus();
        if (original != null) body.setSelection(body.getText().length());
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Chạm ra ngoài hoặc bấm Quay lại cũng tự lưu, giống ghi chú trong app.
        if (!deleted) save(false);
    }

    private Button flatButton(String text) {
        Button b = new Button(this, null, android.R.attr.borderlessButtonStyle);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    private static JSONObject notes(JSONObject root) {
        JSONObject data = root.optJSONObject("data");
        return data == null ? null : data.optJSONObject("notes");
    }

    private static String newId() {
        return Long.toString(System.currentTimeMillis(), 36) + Long.toString((long) (Math.random() * 1e9), 36);
    }

    /** Lưu vào tệp dữ liệu chung. Trả về false nếu không có gì để lưu. */
    private boolean save(boolean showToast) {
        String t = title.getText().toString().trim(), b = body.getText().toString();
        if (t.isEmpty() && b.trim().isEmpty()) return false;
        if (original != null && t.equals(original.optString("title")) && b.equals(original.optString("body"))) return false;
        try {
            JSONObject root = NoteStore.readRoot(this);
            JSONObject notes = notes(root);
            JSONObject n = original != null ? notes.optJSONObject(noteId) : null;
            if (n == null) {
                if (noteId == null) noteId = newId();
                n = new JSONObject().put("id", noteId).put("pinned", false).put("color", "").put("date", "");
            }
            n.put("title", t).put("body", b).put("updated", System.currentTimeMillis());
            notes.put(noteId, n);
            original = n;
            String json = root.toString();
            if (NoteStore.write(this, json)) {
                NoteStore.changed(this, json, true);
                if (showToast) Toast.makeText(this, "Đã lưu ghi chú", Toast.LENGTH_SHORT).show();
                return true;
            }
        } catch (Exception ignored) {
        }
        Toast.makeText(this, "Không lưu được ghi chú", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void delete() {
        deleted = true;
        try {
            JSONObject root = NoteStore.readRoot(this);
            JSONObject notes = notes(root);
            if (notes != null) notes.remove(noteId);
            String json = root.toString();
            if (NoteStore.write(this, json)) {
                NoteStore.changed(this, json, true);
                Toast.makeText(this, "Đã xóa ghi chú", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ignored) {
        }
        finish();
    }
}
