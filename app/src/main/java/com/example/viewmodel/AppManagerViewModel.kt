package com.example.viewmodel

import android.app.Application
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AppItem(
    val name: String,
    val packageName: String,
    val size: Long,
    val isSystemApp: Boolean,
    val permissions: List<String> = emptyList()
)

data class AppManagerState(
    val apps: List<AppItem> = emptyList(),
    val isLoading: Boolean = true,
    val hasUsageStatsPermission: Boolean = false
)

class AppManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(AppManagerState())
    val uiState: StateFlow<AppManagerState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val context = getApplication<Application>()
            val pm = context.packageManager
            val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            val hasPermission = mode == android.app.AppOpsManager.MODE_ALLOWED

            if (!hasPermission) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasUsageStatsPermission = false
                )
                return@launch
            }

            val installedPackages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            
            val appItems = mutableListOf<AppItem>()

            for (packageInfo in installedPackages) {
                val appInfo = packageInfo.applicationInfo ?: continue
                var size = 0L
                try {
                    val stats = storageStatsManager.queryStatsForUid(appInfo.storageUuid, appInfo.uid)
                    size = stats.appBytes + stats.dataBytes + stats.cacheBytes
                } catch (e: Exception) {
                    // Ignore
                }

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val appName = pm.getApplicationLabel(appInfo).toString()
                val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
                
                appItems.add(
                    AppItem(
                        name = appName,
                        packageName = appInfo.packageName,
                        size = size,
                        isSystemApp = isSystem,
                        permissions = requestedPermissions
                    )
                )
            }

            // Sort by size descending
            appItems.sortByDescending { it.size }

            _uiState.value = _uiState.value.copy(
                apps = appItems,
                isLoading = false,
                hasUsageStatsPermission = hasPermission
            )
        }
    }
}
