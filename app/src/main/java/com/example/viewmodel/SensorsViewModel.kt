package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SensorsState(
    val sensors: List<Sensor> = emptyList(),
    val sensorData: Map<Int, FloatArray> = emptyMap()
)

class SensorsViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val _uiState = MutableStateFlow(SensorsState())
    val uiState: StateFlow<SensorsState> = _uiState.asStateFlow()

    init {
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        _uiState.value = _uiState.value.copy(sensors = allSensors)
        
        allSensors.forEach { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val currentData = _uiState.value.sensorData.toMutableMap()
            currentData[it.sensor.type] = it.values.clone()
            _uiState.value = _uiState.value.copy(sensorData = currentData)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }
}
