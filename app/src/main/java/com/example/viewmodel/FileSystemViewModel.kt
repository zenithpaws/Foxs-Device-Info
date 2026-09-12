package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)

data class FileSystemState(
    val currentPath: String = "",
    val files: List<FileItem> = emptyList(),
    val hasPermission: Boolean = false
)

class FileSystemViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(FileSystemState())
    val uiState: StateFlow<FileSystemState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        val hasPerm = Environment.isExternalStorageManager()
        if (hasPerm) {
            loadVolumes()
        } else {
            _uiState.value = _uiState.value.copy(hasPermission = false)
        }
    }

    fun loadVolumes() {
        val context = getApplication<Application>()
        val volumesList = mutableListOf<FileItem>()
        
        // Add Primary Internal Storage
        val root = Environment.getExternalStorageDirectory()
        volumesList.add(
            FileItem(
                name = "Internal Storage",
                path = root.absolutePath,
                isDirectory = true,
                size = 0,
                lastModified = 0
            )
        )

        // Add External Storages
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val volumes = storageManager.storageVolumes
            for (volume in volumes) {
                if (!volume.isPrimary && volume.state == Environment.MEDIA_MOUNTED) {
                    val dir = volume.directory
                    if (dir != null) {
                        val desc = volume.getDescription(context) ?: "SD Card"
                        volumesList.add(
                            FileItem(
                                name = desc,
                                path = dir.absolutePath,
                                isDirectory = true,
                                size = 0,
                                lastModified = 0
                            )
                        )
                    }
                }
            }
        } else {
            val dirs = context.getExternalFilesDirs(null)
            for (dir in dirs) {
                if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                    // Try to get the root of the removable storage by stripping Android/data...
                    val rootPath = dir.absolutePath.split("/Android")[0]
                    volumesList.add(
                        FileItem(
                            name = "SD Card",
                            path = rootPath,
                            isDirectory = true,
                            size = 0,
                            lastModified = 0
                        )
                    )
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            currentPath = "",
            files = volumesList,
            hasPermission = true
        )
    }

    fun loadDirectory(path: String) {
        if (path.isEmpty()) {
            loadVolumes()
            return
        }
        
        val dir = File(path)
        if (dir.exists() && dir.isDirectory) {
            val filesList = dir.listFiles()?.map {
                FileItem(
                    name = it.name,
                    path = it.absolutePath,
                    isDirectory = it.isDirectory,
                    size = it.length(),
                    lastModified = it.lastModified()
                )
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()

            _uiState.value = _uiState.value.copy(
                currentPath = path,
                files = filesList,
                hasPermission = true
            )
        }
    }

    fun navigateUp() {
        val current = File(_uiState.value.currentPath)
        val parent = current.parentFile
        
        if (parent == null || parent.absolutePath == "/" || parent.absolutePath == "/storage") {
            // Reached the top of the volume, go back to volumes list
            loadVolumes()
        } else {
            // Check if we are still inside one of the volumes
            val context = getApplication<Application>()
            var isInVolume = false
            
            val root = Environment.getExternalStorageDirectory()
            if (parent.absolutePath.startsWith(root.absolutePath)) isInVolume = true
            
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                for (volume in storageManager.storageVolumes) {
                    val dir = volume.directory
                    if (dir != null && parent.absolutePath.startsWith(dir.absolutePath)) {
                        isInVolume = true
                    }
                }
            }
            
            if (isInVolume) {
                loadDirectory(parent.absolutePath)
            } else {
                loadVolumes()
            }
        }
    }
}
