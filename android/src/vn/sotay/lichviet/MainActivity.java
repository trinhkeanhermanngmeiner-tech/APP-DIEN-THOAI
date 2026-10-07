/* Sổ Tay Lịch Việt · © 2026 BS. Trịnh Kế An (bstrinhkean@gmail.com). Mọi quyền được bảo lưu. */
package vn.sotay.lichviet;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Vỏ Android: hiển thị giao diện web trong assets/www và nối các tính năng của máy (lưu dữ liệu, thông báo, lưu tệp). */
public class MainActivity extends Activity {
    private static final int REQ_FILE = 41;
    private static final int REQ_NOTIFY = 42;
    private static final int REQ_DRIVE_CREATE = 43;
    private static final int REQ_DRIVE_OPEN = 44;
    private long seenExtWrite;
    private WebView web;
    private View statusSpacer, navSpacer;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingDay;
    private boolean pageReady;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setupEdgeToEdge();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        statusSpacer = new View(this);
        statusSpacer.setBackgroundColor(Color.parseColor("#B3201B"));
        navSpacer = new View(this);
        navSpacer.setBackgroundColor(Color.parseColor("#EFEFEC"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#B3201B"));
        root.addView(statusSpacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        root.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(navSpacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        // Ứng dụng vẽ tràn viền; hai dải đệm có màu theo giao diện thay cho thanh trạng thái và thanh điều hướng.
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int top, bottom;
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                    top = bars.top;
                    bottom = Math.max(bars.bottom, ime.bottom);
                    v.setPadding(bars.left, 0, bars.right, 0);
                } else {
                    top = insets.getSystemWindowInsetTop();
                    bottom = insets.getSystemWindowInsetBottom();
                    v.setPadding(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                }
                setHeight(statusSpacer, top);
                setHeight(navSpacer, bottom);
                return Build.VERSION.SDK_INT >= 30 ? WindowInsets.CONSUMED : insets.consumeSystemWindowInsets();
            }
        });
        setContentView(root);
        setBarIcons(false, true);

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

            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                openPendingDay();
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

        pendingDay = linkFromIntent(getIntent());
        seenExtWrite = Reminders.prefs(this).getLong("extWrite", 0);
        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/www/index.html");

        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+ (và bắt buộc từ Android 16): nút/cử chỉ Quay lại đi qua OnBackInvokedCallback.
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    new OnBackInvokedCallback() {
                        @Override
                        public void onBackInvoked() {
                            handleBack();
                        }
                    });
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY);
            }
        }
        Reminders.createChannel(this);
        Reminders.scheduleNext(this);
    }

    @SuppressWarnings("deprecation")
    private void setupEdgeToEdge() {
        Window w = getWindow();
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
        } else {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    /** Màu biểu tượng trên thanh trạng thái / thanh điều hướng: tối khi nền sáng, sáng khi nền tối. */
    @SuppressWarnings("deprecation")
    private void setBarIcons(boolean statusLight, boolean navLight) {
        // Chỉ gọi sau setContentView: trước đó khung cửa sổ (DecorView) chưa có, getInsetsController() sẽ lỗi.
        getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c == null) return;
            c.setSystemBarsAppearance(
                    (statusLight ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0)
                            | (navLight ? WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS : 0),
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        } else {
            View d = getWindow().getDecorView();
            int f = d.getSystemUiVisibility() & ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            if (statusLight) f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (navLight && Build.VERSION.SDK_INT >= 27) f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            d.setSystemUiVisibility(f);
        }
    }

    private static void setHeight(View v, int h) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        if (p.height != h) {
            p.height = h;
            v.setLayoutParams(p);
        }
    }

    /** Đọc màu CSS dạng #rrggbb hoặc rgb(r, g, b). */
    private static Integer parseCssColor(String css) {
        if (css == null) return null;
        String s = css.trim();
        try {
            if (s.startsWith("#")) return Color.parseColor(s);
            if (s.startsWith("rgb")) {
                String[] p = s.substring(s.indexOf('(') + 1, s.indexOf(')')).split(",");
                return Color.rgb(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean isLight(int c) {
        return (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) > 160;
    }

    private void handleBack() {
        web.evaluateJavascript("window.sotayBack?String(window.sotayBack()):'false'", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String v) {
                if (!"\"true\"".equals(v)) finish();
            }
        });
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBack();
    }

    /**
     * Liên kết từ tiện ích: sotay://ngay/2026-10-07 mở bảng của ngày; sotay://ghichu/<mã> mở ghi chú
     * (sotay://ghichu/ mở mục Ghi chú). Trả về đoạn JavaScript cần chạy, hoặc null.
     */
    static String linkFromIntent(Intent i) {
        Uri u = i == null ? null : i.getData();
        if (u == null || !"sotay".equals(u.getScheme())) return null;
        String kind = u.getHost(), arg = u.getLastPathSegment();
        if ("ngay".equals(kind) && arg != null && arg.matches("\\d{4}-\\d{2}-\\d{2}"))
            return "window.sotayOpenDay&&window.sotayOpenDay('" + arg + "')";
        if ("ghichu".equals(kind))
            return "window.sotayOpenNote&&window.sotayOpenNote('" + (arg != null && arg.matches("[a-z0-9]{1,40}") ? arg : "") + "')";
        return null;
    }

    private void openPendingDay() {
        if (!pageReady || pendingDay == null) return;
        web.evaluateJavascript(pendingDay, null);
        pendingDay = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ghi chú có thể vừa được sửa từ tiện ích: nạp lại dữ liệu vào giao diện.
        long ext = Reminders.prefs(this).getLong("extWrite", 0);
        if (ext != seenExtWrite) {
            seenExtWrite = ext;
            if (pageReady) web.evaluateJavascript("window.sotayReload&&window.sotayReload()", null);
        }
    }

    private void sendDrive() {
        final String st = DriveSync.status(this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                web.evaluateJavascript("window.onDrive&&window.onDrive(" + st + ")", null);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String d = linkFromIntent(intent);
        if (d != null) {
            pendingDay = d;
            openPendingDay();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Khóa lại mục Chi tiêu khi app chạy nền.
        web.evaluateJavascript("window.sotayLock&&window.sotayLock()", null);
    }

    private void sendBiometricResult(boolean ok) {
        web.evaluateJavascript("window.onBiometric&&window.onBiometric(" + ok + ")", null);
    }

    @SuppressWarnings("deprecation")
    private boolean biometricAvailable() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                BiometricManager bm = getSystemService(BiometricManager.class);
                return bm != null && bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
            }
            if (Build.VERSION.SDK_INT == 29) {
                BiometricManager bm = getSystemService(BiometricManager.class);
                return bm != null && bm.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
            }
            if (Build.VERSION.SDK_INT == 28) {
                FingerprintManager fm = getSystemService(FingerprintManager.class);
                return fm != null && fm.isHardwareDetected() && fm.hasEnrolledFingerprints();
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /** Hộp thoại vân tay / khuôn mặt của hệ thống (Android 9 trở lên). */
    private void showBiometricPrompt() {
        if (Build.VERSION.SDK_INT < 28) {
            sendBiometricResult(false);
            return;
        }
        try {
            BiometricPrompt.Builder b = new BiometricPrompt.Builder(this)
                    .setTitle("Mở khóa Chi tiêu")
                    .setSubtitle("Dùng vân tay hoặc khuôn mặt")
                    .setNegativeButton("Dùng mã PIN", getMainExecutor(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sendBiometricResult(false);
                        }
                    });
            if (Build.VERSION.SDK_INT >= 30) b.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK);
            b.build().authenticate(new CancellationSignal(), getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    sendBiometricResult(true);
                }

                @Override
                public void onAuthenticationError(int code, CharSequence msg) {
                    sendBiometricResult(false);
                }
            });
        } catch (Exception e) {
            sendBiometricResult(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQ_DRIVE_CREATE || requestCode == REQ_DRIVE_OPEN) && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri u = data.getData();
            final boolean restore = requestCode == REQ_DRIVE_OPEN;
            DriveSync.IO.execute(new Runnable() {
                @Override
                public void run() {
                    if (restore) {
                        try {
                            final String text = DriveSync.read(MainActivity.this, u);
                            DriveSync.link(MainActivity.this, u);
                            final String quoted = org.json.JSONObject.quote(text);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    web.evaluateJavascript("window.onDriveRestore&&window.onDriveRestore(" + quoted + ")", null);
                                }
                            });
                        } catch (Exception e) {
                            Reminders.prefs(MainActivity.this).edit().putString("driveErr", "Không đọc được tệp: " + e.getMessage()).apply();
                        }
                    } else {
                        DriveSync.link(MainActivity.this, u);
                        DriveSync.writeNow(MainActivity.this, NoteStore.read(MainActivity.this));
                    }
                    sendDrive();
                }
            });
            return;
        }
        if (requestCode == REQ_FILE && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) result = new Uri[]{data.getData()};
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    /** Các hàm gọi được từ JavaScript qua window.AndroidBridge. */
    private class Bridge {
        @JavascriptInterface
        public String loadData() {
            return NoteStore.read(MainActivity.this);
        }

        @JavascriptInterface
        public void saveData(String json) {
            if (NoteStore.write(MainActivity.this, json)) NoteStore.changed(MainActivity.this, json, false);
        }

        @JavascriptInterface
        public String driveStatus() {
            return DriveSync.status(MainActivity.this);
        }

        /** Chọn nơi lưu (thường là Google Drive) qua trình chọn tệp của Android. */
        @JavascriptInterface
        public void driveLink() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("application/json").putExtra(Intent.EXTRA_TITLE, "SoTayLichViet-dulieu.json");
                    try { startActivityForResult(i, REQ_DRIVE_CREATE); } catch (Exception e) { sendDrive(); }
                }
            });
        }

        @JavascriptInterface
        public void driveRestore() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    try { startActivityForResult(i, REQ_DRIVE_OPEN); } catch (Exception e) { sendDrive(); }
                }
            });
        }

        @JavascriptInterface
        public void driveSaveNow() {
            DriveSync.IO.execute(new Runnable() {
                @Override
                public void run() {
                    DriveSync.writeNow(MainActivity.this, NoteStore.read(MainActivity.this));
                    sendDrive();
                }
            });
        }

        @JavascriptInterface
        public void driveUnlink() {
            DriveSync.unlink(MainActivity.this);
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
        public void setBarColor(final String css) {
            final Integer c = parseCssColor(css);
            if (c == null) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusSpacer.setBackgroundColor(c);
                    web.setBackgroundColor(c);
                }
            });
        }

        @JavascriptInterface
        public void setNavColor(final String css) {
            final Integer c = parseCssColor(css);
            if (c == null) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    navSpacer.setBackgroundColor(c);
                    setBarIcons(false, isLight(c));
                }
            });
        }

        @JavascriptInterface
        public void setWidgetData(String json) {
            Reminders.prefs(MainActivity.this).edit().putString("widget", json).apply();
            LichWidget.refreshAll(MainActivity.this);
        }

        @JavascriptInterface
        public void setCalendarData(String json) {
            Reminders.prefs(MainActivity.this).edit().putString("cal", json).apply();
            LichMonthWidget.refreshAll(MainActivity.this);
        }

        @JavascriptInterface
        public boolean canBiometric() {
            return biometricAvailable();
        }

        @JavascriptInterface
        public void biometricAuth() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showBiometricPrompt();
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
