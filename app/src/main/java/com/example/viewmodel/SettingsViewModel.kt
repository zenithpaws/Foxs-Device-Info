package com.example.viewmodel

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.os.Environment
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class SettingsState(
    val useFahrenheit: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val hasAllFilesPermission: Boolean = false,
    val hasTelephonyPermission: Boolean = false,
    val themeMode: String = "System", // "System", "Light", "Dark"
    val batteryRefreshInterval: Int = 3 // seconds (1 to 5)
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        val useF = prefs.getBoolean("use_fahrenheit", false)
        val theme = prefs.getString("theme_mode", "System") ?: "System"
        val batteryRefresh = prefs.getInt("battery_refresh_interval", 3)
        
        _uiState.value = _uiState.value.copy(
            useFahrenheit = useF,
            themeMode = theme,
            batteryRefreshInterval = batteryRefresh
        )
        checkPermissions()
    }

    fun toggleTemperatureUnit(useFahrenheit: Boolean) {
        prefs.edit().putBoolean("use_fahrenheit", useFahrenheit).apply()
        _uiState.value = _uiState.value.copy(useFahrenheit = useFahrenheit)
    }
    
    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun setBatteryRefreshInterval(interval: Int) {
        prefs.edit().putInt("battery_refresh_interval", interval).apply()
        _uiState.value = _uiState.value.copy(batteryRefreshInterval = interval)
    }

    fun checkPermissions() {
        val context = getApplication<Application>()
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        val hasUsageStats = mode == AppOpsManager.MODE_ALLOWED
        val hasAllFiles = Environment.isExternalStorageManager()
        val hasTelPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

        _uiState.value = _uiState.value.copy(
            hasUsageStatsPermission = hasUsageStats,
            hasAllFilesPermission = hasAllFiles,
            hasTelephonyPermission = hasTelPerm
        )
    }
}
