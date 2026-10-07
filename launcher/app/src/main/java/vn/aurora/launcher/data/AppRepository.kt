package vn.aurora.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import android.provider.MediaStore
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import kotlin.math.roundToInt

/** Icons are rasterised a bit larger than they are shown so scale animations stay crisp. */
private const val ICON_RENDER_SIZE_DP = 72

class AppRepository(private val context: Context) {

    private val launcherApps = checkNotNull(context.getSystemService(LauncherApps::class.java))
    private val userManager = checkNotNull(context.getSystemService(UserManager::class.java))
    private val collator = Collator.getInstance(Locale.forLanguageTag("vi"))

    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val metrics = context.resources.displayMetrics
        val iconPx = (ICON_RENDER_SIZE_DP * metrics.density).roundToInt()
        userManager.userProfiles
            .flatMap { user -> launcherApps.getActivityList(null, user) }
            .filter { it.componentName.packageName != context.packageName }
            .map { info ->
                AppInfo(
                    label = info.label.toString(),
                    component = info.componentName,
                    user = info.user,
                    icon = info.getBadgedIcon(metrics.densityDpi)
                        .toBitmap(iconPx, iconPx)
                        .asImageBitmap(),
                )
            }
            .sortedWith(compareBy(collator) { it.label })
    }

    /** Emits whenever an app is installed, removed or updated. */
    fun packageChanges(): Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageAdded(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackageChanged(packageName: String, user: UserHandle) {
                trySend(Unit)
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) {
                trySend(Unit)
            }

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) {
                trySend(Unit)
            }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    /** Packages of the default phone, messages, browser and camera apps, in dock order. */
    @Suppress("DEPRECATION")
    suspend fun defaultDockPackages(): List<String> = withContext(Dispatchers.IO) {
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )
        intents.mapNotNull { intent ->
            context.packageManager
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
                // "android" is the chooser shown when no default is set.
                ?.takeIf { it != "android" }
        }
    }

    fun launch(app: AppInfo, sourceBounds: Rect?, options: Bundle?) {
        launcherApps.startMainActivity(app.component, app.user, sourceBounds, options)
    }

    fun openAppInfo(app: AppInfo, sourceBounds: Rect?) {
        launcherApps.startAppDetailsActivity(app.component, app.user, sourceBounds, null)
    }

    fun uninstall(app: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
