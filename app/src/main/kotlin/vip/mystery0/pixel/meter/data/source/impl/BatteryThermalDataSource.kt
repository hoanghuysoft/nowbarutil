package com.kakao.taxi.data.source.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.kakao.taxi.data.source.ThermalData

class BatteryThermalDataSource(
    private val context: Context
) {
    private val _thermalData = MutableStateFlow(ThermalData(0f))
    val thermalData: StateFlow<ThermalData> = _thermalData.asStateFlow()

    private var isRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                // Battery temperature is mostly reported in tenths of a degree Celsius
                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                val tempCelsius = tempTenths / 10.0f
                _thermalData.value = ThermalData(temperatureCelsius = tempCelsius)
            }
        }
    }

    fun startMonitoring() {
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryReceiver, filter)
            isRegistered = true
            
            // To get immediate value (registerReceiver returns the sticky intent)
            val stickyIntent = context.registerReceiver(null, filter)
            if (stickyIntent != null) {
                batteryReceiver.onReceive(context, stickyIntent)
            }
        }
    }

    fun stopMonitoring() {
        if (isRegistered) {
            context.unregisterReceiver(batteryReceiver)
            isRegistered = false
        }
    }
}
