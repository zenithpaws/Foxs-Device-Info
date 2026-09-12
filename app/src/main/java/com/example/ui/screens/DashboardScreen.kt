package com.example.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.DashboardViewModel
import com.example.viewmodel.SettingsViewModel

import androidx.compose.material.icons.filled.SdStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onNavigateToApps: () -> Unit = {},
    onNavigateToAppUsage: () -> Unit = {},
    onNavigateToLiveResources: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val tempStr = if (settingsState.useFahrenheit) {
        val f = (uiState.batteryTempC * 9/5) + 32
        String.format("%.1f°F", f)
    } else {
        String.format("%.1f°C", uiState.batteryTempC)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val ramUsed = uiState.ramTotal - uiState.ramAvailable
            val ramProgress = if (uiState.ramTotal > 0) ramUsed.toFloat() / uiState.ramTotal else 0f
            
            val swapUsed = uiState.swapTotal - uiState.swapAvailable
            val swapProgress = if (uiState.swapTotal > 0) swapUsed.toFloat() / uiState.swapTotal else 0f
            
            val storageUsed = uiState.storageTotal - uiState.storageAvailable
            val storageProgress = if (uiState.storageTotal > 0) storageUsed.toFloat() / uiState.storageTotal else 0f

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val cpuValue = if (uiState.cpuFreqMhz > 0) "${uiState.cpuCores} Cores @ ${uiState.cpuFreqMhz} MHz" else "${uiState.cpuCores} Cores"
                SmallStatCard(
                    title = "CPU",
                    icon = Icons.Default.DeveloperBoard,
                    value = cpuValue,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToLiveResources
                )
                SmallStatCard(
                    title = "Battery",
                    icon = Icons.Default.BatteryFull,
                    value = "${uiState.batteryLevel}% ($tempStr)",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallStatCard(
                    title = "Display",
                    icon = Icons.Default.Build,
                    value = "${uiState.screenWidth} x ${uiState.screenHeight}",
                    modifier = Modifier.weight(1f)
                )
                SmallStatCard(
                    title = "Refresh Rate",
                    icon = Icons.Default.Build,
                    value = String.format("%.1f Hz", uiState.refreshRate),
                    modifier = Modifier.weight(1f)
                )
            }

            // Battery Stats Card
            val currentMa = uiState.batteryCurrentNow / 1000f // Convert microamperes to milliamperes
            val capacityMah = uiState.batteryChargeCounter / 1000f
            val voltageV = uiState.batteryVoltageMv / 1000f // Convert millivolts to volts
            val currentA = uiState.batteryCurrentNow / 1000000f // Convert microamperes to Amperes
            val wattageW = Math.abs(currentA * voltageV)
            
            val plugTypeStr = when (uiState.plugType) {
                android.os.BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
                android.os.BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "None"
            }

            val timeStr = if (uiState.timeRemainingMs > 0) {
                val seconds = uiState.timeRemainingMs / 1000
                val mins = (seconds / 60) % 60
                val hrs = seconds / 3600
                if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
            } else {
                "Calculating..."
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Battery Usage Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Status: ${if (uiState.isCharging) "Charging" else "Discharging"}", style = MaterialTheme.typography.bodyLarge)
                    if (uiState.isCharging) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Connection: $plugTypeStr", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Time Remaining: $timeStr", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Voltage: ${String.format("%.2f", voltageV)} V", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current: ${String.format("%.1f", currentMa)} mA", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Power: ${String.format("%.2f", wattageW)} W", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Remaining Capacity: ${String.format("%.1f", capacityMah)} mAh", style = MaterialTheme.typography.bodyLarge)
                    if (uiState.batteryCycleCount >= 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Charge Cycles: ${uiState.batteryCycleCount}", style = MaterialTheme.typography.bodyLarge)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToAppUsage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Battery Usage by App")
                    }
                }
            }

            StatCard(
                title = "Memory (RAM)",
                icon = Icons.Default.Memory,
                value = "${Formatter.formatFileSize(context, ramUsed)} / ${Formatter.formatFileSize(context, uiState.ramTotal)}",
                progress = ramProgress
            )
            
            if (uiState.swapTotal > 0) {
                StatCard(
                    title = "System Swap",
                    icon = Icons.Default.Memory,
                    value = "${Formatter.formatFileSize(context, swapUsed)} / ${Formatter.formatFileSize(context, uiState.swapTotal)}",
                    progress = swapProgress
                )
            }

            StatCard(
                title = "Internal Storage",
                icon = Icons.Default.Storage,
                value = "${Formatter.formatFileSize(context, storageUsed)} / ${Formatter.formatFileSize(context, uiState.storageTotal)}",
                progress = storageProgress
            )
            
            uiState.externalStorages.forEach { extStorage ->
                val extUsed = extStorage.totalBytes - extStorage.availableBytes
                val extProgress = if (extStorage.totalBytes > 0) extUsed.toFloat() / extStorage.totalBytes else 0f
                StatCard(
                    title = extStorage.name,
                    icon = Icons.Default.SdStorage,
                    value = "${Formatter.formatFileSize(context, extUsed)} / ${Formatter.formatFileSize(context, extStorage.totalBytes)}",
                    progress = extProgress
                )
            }
        }
    }
}

@Composable
fun StatCard(title: String, icon: ImageVector, value: String, progress: Float) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallStatCard(title: String, icon: ImageVector, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val cardModifier = modifier
    if (onClick != null) {
        ElevatedCard(
            onClick = onClick,
            modifier = cardModifier,
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(value, style = MaterialTheme.typography.headlineSmall)
            }
        }
    } else {
        ElevatedCard(
            modifier = cardModifier,
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(value, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
