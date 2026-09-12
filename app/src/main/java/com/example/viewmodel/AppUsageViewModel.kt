package com.example.viewmodel

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val totalTimeInForegroundMs: Long
)

data class AppUsageState(
    val usageList: List<AppUsageItem> = emptyList(),
    val isLoading: Boolean = true,
    val hasPermission: Boolean = false
)

class AppUsageViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(AppUsageState())
    val uiState: StateFlow<AppUsageState> = _uiState.asStateFlow()

    init {
        loadUsage()
    }

    fun loadUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val context = getApplication<Application>()
            
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            val hasPermission = mode == android.app.AppOpsManager.MODE_ALLOWED

            if (!hasPermission) {
                _uiState.value = _uiState.value.copy(isLoading = false, hasPermission = false)
                return@launch
            }

            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val pm = context.packageManager
            
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (24 * 60 * 60 * 1000) // 24 hours
            
            val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            
            val appUsageItems = mutableListOf<AppUsageItem>()
            
            if (usageStatsList != null) {
                for (usageStats in usageStatsList) {
                    if (usageStats.totalTimeInForeground > 0) {
                        val packageName = usageStats.packageName
                        var appName = packageName
                        try {
                            val appInfo = pm.getApplicationInfo(packageName, 0)
                            appName = pm.getApplicationLabel(appInfo).toString()
                        } catch (e: PackageManager.NameNotFoundException) {
                            // Keep package name
                        }
                        
                        appUsageItems.add(
                            AppUsageItem(
                                packageName = packageName,
                                appName = appName,
                                totalTimeInForegroundMs = usageStats.totalTimeInForeground
                            )
                        )
                    }
                }
            }
            
            // Group by package name and sum
            val grouped = appUsageItems.groupBy { it.packageName }.map { (pkg, list) ->
                AppUsageItem(
                    packageName = pkg,
                    appName = list.first().appName,
                    totalTimeInForegroundMs = list.sumOf { it.totalTimeInForegroundMs }
                )
            }.sortedByDescending { it.totalTimeInForegroundMs }
            
            _uiState.value = _uiState.value.copy(
                usageList = grouped,
                isLoading = false,
                hasPermission = true
            )
        }
    }
}
