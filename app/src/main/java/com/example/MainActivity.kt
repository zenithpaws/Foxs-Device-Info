package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DeviceMonitorApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val settingsViewModel: SettingsViewModel = viewModel()
      val settingsState by settingsViewModel.uiState.collectAsState()
      
      val isDarkTheme = when (settingsState.themeMode) {
          "Light" -> false
          "Dark" -> true
          else -> isSystemInDarkTheme()
      }

      MyApplicationTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          DeviceMonitorApp(settingsViewModel = settingsViewModel)
        }
      }
    }
  }
}

