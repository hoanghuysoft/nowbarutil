package com.kakao.taxi.data.source.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.kakao.taxi.data.source.ThermalData

/**
 * Hybrid battery temperature data source.
 *
 * Uses two mechanisms to ensure real-time temperature updates:
 * 1. **BroadcastReceiver** for [Intent.ACTION_BATTERY_CHANGED] — picks up system-driven
 *    updates whenever the battery state changes (charging, level, temperature).
 * 2. **Polling loop** — actively reads from the sticky intent at a fixed interval to ensure
 *    the UI stays fresh even when the system doesn't broadcast (battery temp changes slowly).
 *
 * This dual approach guarantees that the Samsung Now Bar pill always shows a current value.
 */
class BatteryThermalDataSource(
    private val context: Context
) {
    companion object {
        private const val TAG = "BatteryThermalDataSource"

        /** Default polling interval in milliseconds. */
        private const val DEFAULT_POLL_INTERVAL_MS = 3000L
    }

    private val _thermalData = MutableStateFlow(ThermalData(0f))
    val thermalData: StateFlow<ThermalData> = _thermalData.asStateFlow()

    private var isRegistered = false
    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                updateFromIntent(intent)
            }
        }
    }

    fun startMonitoring() {
        if (isRegistered) return

        // Register receiver to catch system-driven battery change events
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
        isRegistered = true

        // Read the sticky intent immediately for an initial value
        val stickyIntent = context.registerReceiver(null, filter)
        if (stickyIntent != null) {
            updateFromIntent(stickyIntent)
        }

        // Start a polling loop to ensure continuous updates.
        // ACTION_BATTERY_CHANGED can be infrequent when temperature is stable,
        // so we actively re-read the sticky intent at a fixed interval.
        pollingJob = scope.launch {
            while (true) {
                delay(DEFAULT_POLL_INTERVAL_MS)
                try {
                    val latestIntent = context.registerReceiver(null, filter)
                    if (latestIntent != null) {
                        updateFromIntent(latestIntent)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read battery sticky intent", e)
                }
            }
        }

        Log.i(TAG, "Monitoring started (broadcast + ${DEFAULT_POLL_INTERVAL_MS}ms polling)")
    }

    fun stopMonitoring() {
        pollingJob?.cancel()
        pollingJob = null

        if (isRegistered) {
            context.unregisterReceiver(batteryReceiver)
            isRegistered = false
        }

        Log.i(TAG, "Monitoring stopped")
    }

    /**
     * Extracts battery temperature from the [Intent.ACTION_BATTERY_CHANGED] intent.
     * The system reports temperature in tenths of a degree Celsius (e.g., 315 = 31.5°C).
     */
    private fun updateFromIntent(intent: Intent) {
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempCelsius = tempTenths / 10.0f
        _thermalData.value = ThermalData(temperatureCelsius = tempCelsius)
    }
}
