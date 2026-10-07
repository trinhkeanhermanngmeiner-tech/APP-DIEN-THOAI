package vn.sotay.lichviet;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Vỏ Android: hiển thị giao diện web trong assets/www và nối các tính năng của máy (lưu dữ liệu, thông báo, lưu tệp). */
public class MainActivity extends Activity {
    private static final int REQ_FILE = 41;
    private static final int REQ_NOTIFY = 42;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#B3201B"));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Chọn tệp sao lưu"), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/www/index.html");

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY);
        }
        Reminders.createChannel(this);
        Reminders.scheduleNext(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) result = new Uri[]{data.getData()};
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.sotayBack?String(window.sotayBack()):'false'", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String v) {
                if (!"\"true\"".equals(v)) finish();
            }
        });
    }

    private File dataFile() {
        return new File(getFilesDir(), "so-tay.json");
    }

    /** Các hàm gọi được từ JavaScript qua window.AndroidBridge. */
    private class Bridge {
        @JavascriptInterface
        public String loadData() {
            File f = dataFile();
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

        @JavascriptInterface
        public void saveData(String json) {
            File tmp = new File(getFilesDir(), "so-tay.json.tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(json.getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            } catch (Exception e) {
                return;
            }
            tmp.renameTo(dataFile());
        }

        @JavascriptInterface
        public void setReminders(String json, int hour) {
            Reminders.prefs(MainActivity.this).edit()
                    .putString("list", json)
                    .putInt("hour", hour < 0 || hour > 23 ? 7 : hour)
                    .apply();
            Reminders.postToday(MainActivity.this, false);
            Reminders.scheduleNext(MainActivity.this);
        }

        @JavascriptInterface
        public void setBarColor(final String hex) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try { getWindow().setStatusBarColor(Color.parseColor(hex.trim())); } catch (Exception ignored) { }
                }
            });
        }

        @JavascriptInterface
        public void testNotify() {
            Reminders.post(MainActivity.this, 999, "Sổ Tay Lịch Việt",
                    "Thông báo đang hoạt động. Mỗi ngày app sẽ nhắc vào giờ bạn đã chọn.");
        }

        @JavascriptInterface
        public String saveFile(String name, String content, String mime) {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) return "Không lưu được tệp.";
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        os.write(bytes);
                    }
                    return "Đã lưu vào thư mục Tải xuống: " + name;
                }
                File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (dir == null) dir = getFilesDir();
                File f = new File(dir, name);
                try (FileOutputStream out = new FileOutputStream(f)) {
                    out.write(bytes);
                }
                return "Đã lưu: " + f.getAbsolutePath();
            } catch (Exception e) {
                return "Không lưu được tệp: " + e.getMessage();
            }
        }
    }
}
