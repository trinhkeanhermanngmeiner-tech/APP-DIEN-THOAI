package vn.aurora.launcher.data

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
data class LauncherSettings(
    // Navigation & gestures
    val hideNavBar: Boolean = true,
    val swipeDownNotifications: Boolean = true,
    val doubleTapLock: Boolean = false,
    // Effects
    val auroraBackground: Boolean = true,
    val fireflies: Boolean = true,
    val tiltParallax: Boolean = true,
    val cubePages: Boolean = true,
    val glassDock: Boolean = true,
    // Layout
    val showLabels: Boolean = true,
    val gridColumns: Int = 4,
    val iconSizeDp: Int = 58,
)

const val MIN_ICON_SIZE_DP = 44
const val MAX_ICON_SIZE_DP = 72

/** Settings kept in SharedPreferences and exposed as a flow so the UI reacts instantly. */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("launcher_settings", Context.MODE_PRIVATE)
    private val defaults = LauncherSettings()

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val updated = transform(_settings.value)
        write(updated)
        _settings.value = updated
    }

    private fun read() = LauncherSettings(
        hideNavBar = prefs.getBoolean("hide_nav_bar", defaults.hideNavBar),
        swipeDownNotifications = prefs.getBoolean("swipe_down_notifications", defaults.swipeDownNotifications),
        doubleTapLock = prefs.getBoolean("double_tap_lock", defaults.doubleTapLock),
        auroraBackground = prefs.getBoolean("aurora_background", defaults.auroraBackground),
        fireflies = prefs.getBoolean("fireflies", defaults.fireflies),
        tiltParallax = prefs.getBoolean("tilt_parallax", defaults.tiltParallax),
        cubePages = prefs.getBoolean("cube_pages", defaults.cubePages),
        glassDock = prefs.getBoolean("glass_dock", defaults.glassDock),
        showLabels = prefs.getBoolean("show_labels", defaults.showLabels),
        gridColumns = prefs.getInt("grid_columns", defaults.gridColumns),
        iconSizeDp = prefs.getInt("icon_size_dp", defaults.iconSizeDp)
            .coerceIn(MIN_ICON_SIZE_DP, MAX_ICON_SIZE_DP),
    )

    private fun write(settings: LauncherSettings) {
        prefs.edit()
            .putBoolean("hide_nav_bar", settings.hideNavBar)
            .putBoolean("swipe_down_notifications", settings.swipeDownNotifications)
            .putBoolean("double_tap_lock", settings.doubleTapLock)
            .putBoolean("aurora_background", settings.auroraBackground)
            .putBoolean("fireflies", settings.fireflies)
            .putBoolean("tilt_parallax", settings.tiltParallax)
            .putBoolean("cube_pages", settings.cubePages)
            .putBoolean("glass_dock", settings.glassDock)
            .putBoolean("show_labels", settings.showLabels)
            .putInt("grid_columns", settings.gridColumns)
            .putInt("icon_size_dp", settings.iconSizeDp)
            .apply()
    }
}
