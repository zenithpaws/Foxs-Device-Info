package com.example.viewmodel

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager

data class HardwareState(
    val modelNumber: String = "",
    val manufacturer: String = "",
    val device: String = "",
    val board: String = "",
    val hardware: String = "",
    val osVersion: String = "",
    val apiLevel: Int = 0,
    val ramSize: Long = 0,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val screenDensity: Float = 0f,
    val refreshRate: Float = 0f,
    val imei: String = "Restricted or Unavailable",
    val phoneNumber: String = "Restricted or Unavailable",
    val isRooted: Boolean = false,
    val hasTelephonyPermission: Boolean = false
)

class HardwareViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HardwareState())
    val uiState: StateFlow<HardwareState> = _uiState.asStateFlow()

    init {
        loadHardwareInfo()
    }

    fun loadHardwareInfo() {
        val context = getApplication<Application>()
        
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memoryInfo)

        var hasTelPerm = false
        var imeiStr = "Restricted or Unavailable"
        var phoneStr = "Restricted or Unavailable"

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            hasTelPerm = true
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    imeiStr = tm.imei ?: "Unknown"
                } else {
                    @Suppress("DEPRECATION")
                    imeiStr = tm.deviceId ?: "Unknown"
                }
                
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                    @Suppress("MissingPermission")
                    phoneStr = tm.line1Number ?: "Unknown"
                }
            } catch (e: SecurityException) {
                imeiStr = "Restricted (Android 10+ requires privileged access)"
                phoneStr = "Restricted"
            } catch (e: Exception) {
                imeiStr = "Error reading IMEI"
            }
        }

        // Basic root check
        val buildTags = Build.TAGS
        val check1 = buildTags != null && buildTags.contains("test-keys")
        val check2 = File("/system/app/Superuser.apk").exists()
        val check3 = arrayOf(
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        ).any { File(it).exists() }
        
        val isRooted = check1 || check2 || check3

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val displayMetrics = DisplayMetrics()
        
        var sWidth = 0
        var sHeight = 0
        var sDensity = 0f
        var sRefreshRate = 0f
        
        if (display != null) {
            display.getRealMetrics(displayMetrics)
            sWidth = displayMetrics.widthPixels
            sHeight = displayMetrics.heightPixels
            sDensity = displayMetrics.density
            sRefreshRate = display.refreshRate
        }

        _uiState.value = _uiState.value.copy(
            modelNumber = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            device = Build.DEVICE,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            osVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            ramSize = memoryInfo.totalMem,
            screenWidth = sWidth,
            screenHeight = sHeight,
            screenDensity = sDensity,
            refreshRate = sRefreshRate,
            imei = imeiStr,
            phoneNumber = phoneStr,
            isRooted = isRooted,
            hasTelephonyPermission = hasTelPerm
        )
    }
}
