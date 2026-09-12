package com.example.viewmodel

import android.app.ActivityManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.Display
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExternalStorageInfo(
    val name: String,
    val totalBytes: Long,
    val availableBytes: Long
)

data class DashboardState(
    val ramTotal: Long = 0,
    val ramAvailable: Long = 0,
    val swapTotal: Long = 0,
    val swapAvailable: Long = 0,
    val storageTotal: Long = 0,
    val storageAvailable: Long = 0,
    val externalStorages: List<ExternalStorageInfo> = emptyList(),
    val cpuCores: Int = 0,
    val cpuFreqMhz: Int = -1,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val refreshRate: Float = 0f,
    val batteryLevel: Int = 0,
    val batteryTempC: Float = 0f,
    val batteryCurrentNow: Int = 0, // Microamperes
    val batteryChargeCounter: Int = 0, // Microampere-hours
    val batteryCycleCount: Int = -1,
    val isCharging: Boolean = false,
    val batteryVoltageMv: Int = 0,
    val timeRemainingMs: Long = -1L,
    val plugType: Int = -1
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                
                val cycleCount = if (Build.VERSION.SDK_INT >= 34) {
                    intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
                } else {
                    -1
                }
                
                val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else 0
                val batteryTempCelsius = temp / 10f

                val batteryManager = context?.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val currentNow = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
                val chargeCounter = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                
                val timeRemaining = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    batteryManager?.computeChargeTimeRemaining() ?: -1L
                } else {
                    -1L
                }

                _uiState.value = _uiState.value.copy(
                    batteryLevel = batteryPct,
                    batteryTempC = batteryTempCelsius,
                    batteryCurrentNow = currentNow,
                    batteryChargeCounter = chargeCounter,
                    batteryCycleCount = cycleCount,
                    isCharging = isCharging,
                    batteryVoltageMv = voltage,
                    timeRemainingMs = timeRemaining,
                    plugType = plugged
                )
            }
        }
    }

    init {
        updateStats()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        application.registerReceiver(batteryReceiver, filter)
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = getApplication<Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            while (true) {
                val interval = prefs.getInt("battery_refresh_interval", 3)
                delay(interval * 1000L)
                updateStats()
                
                // Manually poll battery properties that don't need a broadcast receiver
                val context = getApplication<Application>()
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                if (batteryManager != null) {
                    val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    val chargeCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                    val timeRemaining = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        batteryManager.computeChargeTimeRemaining()
                    } else -1L
                    
                    _uiState.value = _uiState.value.copy(
                        batteryCurrentNow = currentNow,
                        batteryChargeCounter = chargeCounter,
                        timeRemainingMs = timeRemaining
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(batteryReceiver)
    }

    fun updateStats() {
        val context = getApplication<Application>()
        
        // RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        // Storage
        val statFs = StatFs(Environment.getDataDirectory().path)
        val storageTotal = statFs.totalBytes
        val storageAvailable = statFs.availableBytes
        
        // External Storage
        val externalStoragesList = mutableListOf<ExternalStorageInfo>()
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as android.os.storage.StorageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val volumes = storageManager.storageVolumes
            for (volume in volumes) {
                if (!volume.isPrimary && volume.state == Environment.MEDIA_MOUNTED) {
                    val dir = volume.directory
                    if (dir != null) {
                        try {
                            val extStatFs = StatFs(dir.path)
                            val desc = volume.getDescription(context)
                            externalStoragesList.add(
                                ExternalStorageInfo(
                                    name = desc ?: "External Storage",
                                    totalBytes = extStatFs.totalBytes,
                                    availableBytes = extStatFs.availableBytes
                                )
                            )
                        } catch (e: Exception) {}
                    }
                }
            }
        } else {
            val dirs = context.getExternalFilesDirs(null)
            for (dir in dirs) {
                if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                    try {
                        val extStatFs = StatFs(dir.path)
                        externalStoragesList.add(
                            ExternalStorageInfo(
                                name = "SD Card",
                                totalBytes = extStatFs.totalBytes,
                                availableBytes = extStatFs.availableBytes
                            )
                        )
                    } catch (e: Exception) {}
                }
            }
        }

        // CPU
        var cpuFreq = -1
        try {
            val freqFile = java.io.File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
            if (freqFile.exists()) {
                val freqKHz = freqFile.readText().trim().toInt()
                cpuFreq = freqKHz / 1000 // Convert to MHz
            }
        } catch (e: Exception) {}
        
        val cpuCores = Runtime.getRuntime().availableProcessors()

        // Swap (from /proc/meminfo)
        var swapTotal = 0L
        var swapFree = 0L
        try {
            val meminfo = java.io.File("/proc/meminfo")
            if (meminfo.exists()) {
                meminfo.forEachLine { line ->
                    if (line.startsWith("SwapTotal:")) {
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 2) swapTotal = parts[1].toLong() * 1024L
                    } else if (line.startsWith("SwapFree:")) {
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 2) swapFree = parts[1].toLong() * 1024L
                    }
                }
            }
        } catch (e: Exception) {}
        
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val displayMetrics = DisplayMetrics()
        
        var sWidth = 0
        var sHeight = 0
        var sRefreshRate = 0f
        
        if (display != null) {
            display.getRealMetrics(displayMetrics)
            sWidth = displayMetrics.widthPixels
            sHeight = displayMetrics.heightPixels
            sRefreshRate = display.refreshRate
        }

        _uiState.value = _uiState.value.copy(
            ramTotal = memoryInfo.totalMem,
            ramAvailable = memoryInfo.availMem,
            swapTotal = swapTotal,
            swapAvailable = swapFree,
            storageTotal = storageTotal,
            storageAvailable = storageAvailable,
            externalStorages = externalStoragesList,
            cpuCores = cpuCores,
            cpuFreqMhz = cpuFreq,
            screenWidth = sWidth,
            screenHeight = sHeight,
            refreshRate = sRefreshRate
        )
    }
}
