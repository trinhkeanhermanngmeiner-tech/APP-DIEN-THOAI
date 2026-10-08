package vn.aurora.launcher

import android.app.Application
import android.graphics.Rect
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import vn.aurora.launcher.data.AppInfo
import vn.aurora.launcher.data.AppRepository
import vn.aurora.launcher.data.LauncherSettings
import vn.aurora.launcher.data.SettingsRepository

const val DOCK_SIZE = 4

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    private val settingsRepository = SettingsRepository(application)

    val settings: StateFlow<LauncherSettings> = settingsRepository.settings

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _dock = MutableStateFlow<List<AppInfo>>(emptyList())
    val dock: StateFlow<List<AppInfo>> = _dock.asStateFlow()

    private val _homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homePresses: SharedFlow<Unit> = _homePresses.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.packageChanges()
                .onStart { emit(Unit) }
                .conflate()
                .collect { reload() }
        }
    }

    private suspend fun reload() {
        val apps = repository.loadApps()
        val preferred = repository.defaultDockPackages()
            .mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
            .distinctBy { it.packageName }
        _apps.value = apps
        _dock.value = (preferred + apps.filterNot { it in preferred }).take(DOCK_SIZE)
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        settingsRepository.update(transform)
    }

    fun onHomePressed() {
        _homePresses.tryEmit(Unit)
    }

    fun launch(app: AppInfo, bounds: Rect?, options: Bundle?): Boolean =
        runCatching { repository.launch(app, bounds, options) }.isSuccess

    fun openAppInfo(app: AppInfo, bounds: Rect?): Boolean =
        runCatching { repository.openAppInfo(app, bounds) }.isSuccess

    fun uninstall(app: AppInfo): Boolean =
        runCatching { repository.uninstall(app) }.isSuccess
}
