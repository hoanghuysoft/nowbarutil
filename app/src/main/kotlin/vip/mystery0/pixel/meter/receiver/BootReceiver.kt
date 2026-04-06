package com.kakao.taxi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.kakao.taxi.data.repository.ThermalRepository
import com.kakao.taxi.service.ThermalMonitorService

class BootReceiver : BroadcastReceiver(), KoinComponent {
    companion object {
        private const val TAG = "BootReceiver"
    }

    private val repository: ThermalRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val isAutoStart = repository.isAutoStartServiceEnabled.value
        if (isAutoStart) {
            Log.i(TAG, "boot completed, starting service")
            try {
                val serviceIntent = Intent(context, ThermalMonitorService::class.java)
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "failed to start foreground service on boot", e)
            }
        } else {
            Log.i(TAG, "boot completed, but auto-start is disabled")
        }
    }
}
