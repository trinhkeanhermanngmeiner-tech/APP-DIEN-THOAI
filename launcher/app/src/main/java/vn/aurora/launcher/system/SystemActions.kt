package vn.aurora.launcher.system

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object SystemActions {

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val self = ComponentName(context, AuroraAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == self }
    }

    /** Needs the accessibility service; returns false if it isn't running. */
    fun lockScreen(): Boolean =
        AuroraAccessibilityService.instance
            ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true

    /**
     * Pulls down the notification shade: first through the status bar service (works on
     * most phones without extra setup), then through the accessibility service.
     */
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context): Boolean {
        val viaStatusBar = runCatching {
            val statusBar = checkNotNull(context.getSystemService("statusbar"))
            statusBar.javaClass.getMethod("expandNotificationsPanel").invoke(statusBar)
        }.isSuccess
        if (viaStatusBar) return true
        return AuroraAccessibilityService.instance
            ?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) == true
    }

    fun openAccessibilitySettings(context: Context) {
        startFirst(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /** The system's "default home app" screen, falling back to more general pages. */
    fun openHomeSettings(context: Context) {
        startFirst(
            context,
            Intent(Settings.ACTION_HOME_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
    }

    fun openOwnAppInfo(context: Context) {
        startFirst(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )
    }

    private fun startFirst(context: Context, vararg intents: Intent) {
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next one.
            }
        }
    }
}
