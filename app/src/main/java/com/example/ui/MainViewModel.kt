package com.example.ui

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.RnaApplication
import com.example.RnaHomeInterceptorService
import com.example.data.TvAppCustomEntity
import com.example.data.model.RnaApp
import com.example.util.RnaPackageEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val KEY_SHOW_RECENT_APPS_ROW = "show_recent_apps_row_v2"
        const val KEY_SINGLE_ROW_HOME_APPS = "single_row_home_apps_v2"
        const val KEY_RECENT_APPS_TIMESTAMPS_JSON = "recent_apps_timestamps_json"
    }

    private val repository = (application as RnaApplication).repository
    private val prefs = application.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)

    val installedApps: StateFlow<List<RnaApp>> = repository.allApps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tvCustomizations: StateFlow<List<TvAppCustomEntity>> = repository.allTvCustomizations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 0: Home, 1: Create, 2: Manage, 3: More
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isTvLauncherMode = MutableStateFlow(detectIsTelevision(application))
    val isTvLauncherMode: StateFlow<Boolean> = _isTvLauncherMode.asStateFlow()

    private val _showCreationWizard = MutableStateFlow(false)
    val showCreationWizard: StateFlow<Boolean> = _showCreationWizard.asStateFlow()

    private val _pendingInstallerApp = MutableStateFlow<RnaApp?>(null)
    val pendingInstallerApp: StateFlow<RnaApp?> = _pendingInstallerApp.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Toggle for showing/hiding the 'Recent Apps' row on the main launcher screen (Disabled by default)
    private val _showRecentAppsRow = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_RECENT_APPS_ROW, false)
    )
    val showRecentAppsRow: StateFlow<Boolean> = _showRecentAppsRow.asStateFlow()

    // Toggle for displaying 'Your apps' on the Home tab as a Single Row (true by default) vs 2 Rows (false)
    private val _singleRowHomeApps = MutableStateFlow(
        prefs.getBoolean(KEY_SINGLE_ROW_HOME_APPS, true)
    )
    val singleRowHomeApps: StateFlow<Boolean> = _singleRowHomeApps.asStateFlow()

    // Map of packageName -> lastUsedTimestampMillis for both RNA web apps and normal native apps
    private val _recentAppTimestamps = MutableStateFlow(loadRecentTimestampsFromPrefs())
    val recentAppTimestamps: StateFlow<Map<String, Long>> = _recentAppTimestamps.asStateFlow()

    private fun detectIsTelevision(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    private fun loadRecentTimestampsFromPrefs(): Map<String, Long> {
        val raw = prefs.getString(KEY_RECENT_APPS_TIMESTAMPS_JSON, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            val map = mutableMapOf<String, Long>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val ts = json.optLong(key, 0L)
                if (ts > 0L) {
                    map[key] = ts
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveRecentTimestampsToPrefs(map: Map<String, Long>) {
        try {
            val json = JSONObject()
            map.forEach { (pkg, ts) ->
                json.put(pkg, ts)
            }
            prefs.edit().putString(KEY_RECENT_APPS_TIMESTAMPS_JSON, json.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun setShowRecentAppsRow(enabled: Boolean) {
        _showRecentAppsRow.value = enabled
        prefs.edit().putBoolean(KEY_SHOW_RECENT_APPS_ROW, enabled).apply()
    }

    fun toggleShowRecentAppsRow() {
        setShowRecentAppsRow(!_showRecentAppsRow.value)
    }

    fun setSingleRowHomeApps(singleRow: Boolean) {
        _singleRowHomeApps.value = singleRow
        prefs.edit().putBoolean(KEY_SINGLE_ROW_HOME_APPS, singleRow).apply()
    }

    fun recordAppLaunched(packageName: String, rnaAppId: String? = null) {
        val now = System.currentTimeMillis()
        val updated = _recentAppTimestamps.value.toMutableMap().apply {
            this[packageName] = now
        }
        _recentAppTimestamps.value = updated
        saveRecentTimestampsToPrefs(updated)

        if (!rnaAppId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.markAppLaunched(rnaAppId)
            }
        }
    }

    fun removeRecentApp(packageName: String) {
        val updated = _recentAppTimestamps.value.toMutableMap().apply {
            remove(packageName)
        }
        _recentAppTimestamps.value = updated
        saveRecentTimestampsToPrefs(updated)
    }

    fun clearRecentApps() {
        _recentAppTimestamps.value = emptyMap()
        prefs.edit().remove(KEY_RECENT_APPS_TIMESTAMPS_JSON).apply()
        Toast.makeText(getApplication(), "Cleared Recent Apps history", Toast.LENGTH_SHORT).show()
    }

    fun setTvLauncherMode(enabled: Boolean) {
        _isTvLauncherMode.value = enabled
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun openCreationWizard() {
        _showCreationWizard.value = true
    }

    fun closeCreationWizard() {
        _showCreationWizard.value = false
    }

    fun openInstallerWithApp(app: RnaApp) {
        _pendingInstallerApp.value = app
    }

    fun closeInstaller() {
        _pendingInstallerApp.value = null
    }

    fun toggleDarkTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun saveAppToManageOnly(app: RnaApp) {
        viewModelScope.launch {
            repository.saveAppOnly(app)
            _showCreationWizard.value = false
            _isTvLauncherMode.value = false
            _selectedTab.value = 2
            Toast.makeText(
                getApplication(),
                "Saved '${app.title}' to Manage tab. Click Install to launch Package Installer!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun installApp(app: RnaApp, useSlotAlias: Boolean, pinShortcut: Boolean, onComplete: (RnaApp) -> Unit) {
        viewModelScope.launch {
            val installed = repository.installApp(app, useSlotAlias, pinShortcut)
            recordAppLaunched(installed.packageName, installed.id)
            Toast.makeText(
                getApplication(),
                "Installed '${installed.title}' to RNA. TV",
                Toast.LENGTH_SHORT
            ).show()
            onComplete(installed)
        }
    }

    fun updateApp(app: RnaApp, oldSlot: Int?, enableSlot: Boolean) {
        viewModelScope.launch {
            repository.updateApp(app, oldSlot, enableSlot)
            Toast.makeText(getApplication(), "Updated ${app.title}", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateAppDirectly(app: RnaApp) {
        viewModelScope.launch {
            repository.updateAppDirectly(app)
        }
    }

    fun saveTvCustomization(entity: TvAppCustomEntity) {
        viewModelScope.launch {
            repository.upsertTvCustomization(entity)
        }
    }

    fun uninstallApp(app: RnaApp) {
        viewModelScope.launch {
            removeRecentApp(app.packageName)
            repository.deleteApp(app)
            Toast.makeText(getApplication(), "Uninstalled ${app.title}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportApp(app: RnaApp, triggerShare: Boolean = true) {
        viewModelScope.launch {
            val exportedFile = repository.exportApp(app)
            if (exportedFile != null) {
                Toast.makeText(
                    getApplication(),
                    "Exported ${exportedFile.name} to Downloads",
                    Toast.LENGTH_LONG
                ).show()

                if (triggerShare) {
                    try {
                        val shareIntent = RnaPackageEngine.createShareRnaIntent(
                            context = getApplication(),
                            exportedFile = exportedFile,
                            appTitle = app.title
                        )
                        val chooser = Intent.createChooser(shareIntent, "Share or Export ${app.title} .rna package").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        getApplication<Application>().startActivity(chooser)
                    } catch (_: Exception) {}
                }
            } else {
                Toast.makeText(getApplication(), "Failed to export package to Downloads", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleImportedRnaFile(uri: Uri) {
        val app = RnaPackageEngine.parseRnaPackage(getApplication(), uri)
        if (app != null) {
            _pendingInstallerApp.value = app
        } else {
            Toast.makeText(getApplication(), "Invalid .rna container format", Toast.LENGTH_SHORT).show()
        }
    }
}
