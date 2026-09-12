package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

data class ProcessResourceInfo(
    val pid: String,
    val user: String,
    val cpuPercent: String,
    val memPercent: String,
    val time: String,
    val name: String
)

data class LiveResourcesState(
    val processes: List<ProcessResourceInfo> = emptyList(),
    val summary: String = "Loading...",
    val isLoading: Boolean = true
)

class LiveResourcesViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LiveResourcesState())
    val uiState: StateFlow<LiveResourcesState> = _uiState.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("top", "-b", "-n", "1"))
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    
                    val processesList = mutableListOf<ProcessResourceInfo>()
                    var summaryBuilder = StringBuilder()
                    
                    var isReadingProcesses = false
                    var pidIdx = -1
                    var userIdx = -1
                    var cpuIdx = -1
                    var memIdx = -1
                    var timeIdx = -1
                    var nameIdx = -1
                    
                    reader.forEachLine { line ->
                        val trimmedLine = line.trim()
                        if (trimmedLine.isEmpty()) return@forEachLine
                        
                        if (trimmedLine.startsWith("PID") && trimmedLine.contains("USER")) {
                            isReadingProcesses = true
                            val headers = trimmedLine.split("\\s+".toRegex())
                            pidIdx = headers.indexOf("PID")
                            userIdx = headers.indexOf("USER")
                            cpuIdx = headers.indexOfFirst { it.contains("CPU") }
                            memIdx = headers.indexOfFirst { it.contains("MEM") }
                            timeIdx = headers.indexOfFirst { it.contains("TIME") }
                            nameIdx = headers.indexOfFirst { it == "ARGS" || it == "CMD" || it == "COMMAND" || it == "NAME" }
                            return@forEachLine
                        }
                        
                        if (!isReadingProcesses) {
                            summaryBuilder.append(trimmedLine).append("\n")
                        } else {
                            val parts = trimmedLine.split("\\s+".toRegex())
                            if (pidIdx >= 0 && parts.size > pidIdx && nameIdx >= 0) {
                                val pid = parts.getOrNull(pidIdx) ?: ""
                                val user = parts.getOrNull(userIdx) ?: ""
                                val cpu = parts.getOrNull(cpuIdx) ?: ""
                                val mem = parts.getOrNull(memIdx) ?: ""
                                val time = parts.getOrNull(timeIdx) ?: ""
                                val name = if (parts.size > nameIdx) parts.subList(nameIdx, parts.size).joinToString(" ") else ""
                                
                                processesList.add(
                                    ProcessResourceInfo(
                                        pid = pid,
                                        user = user,
                                        cpuPercent = cpu,
                                        memPercent = mem,
                                        time = time,
                                        name = name
                                    )
                                )
                            }
                        }
                    }
                    
                    process.waitFor()
                    
                    _uiState.value = _uiState.value.copy(
                        processes = processesList,
                        summary = summaryBuilder.toString().trim(),
                        isLoading = false
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        summary = "Error loading live resources: ${e.message}",
                        isLoading = false
                    )
                }
                
                delay(3000) // Update every 3 seconds
            }
        }
    }
}
