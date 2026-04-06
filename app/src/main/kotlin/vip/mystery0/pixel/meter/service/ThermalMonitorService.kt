package com.kakao.taxi.service

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import com.kakao.taxi.data.repository.ThermalRepository
import com.kakao.taxi.data.source.ThermalData
import com.kakao.taxi.ui.overlay.OverlayWindow

class ThermalMonitorService : Service() {
    companion object {
        private const val TAG = "ThermalMonitorService"
    }

    private val repository: ThermalRepository by inject()
    private val notificationHelper: NotificationHelper by inject()
    private val notificationManager: NotificationManager by inject()
    private val overlayWindow: OverlayWindow by inject()

    private var serviceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotif = notificationHelper.buildNotification(
            thermalData = ThermalData(0f),
            isNotificationEnabled = true,
            textSize = 0.50f,
            unitSize = 0.35f,
            useCustomColor = false,
            color = 0,
            isBlank = false
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID,
                    initialNotif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID,
                    initialNotif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand: start foreground error", e)
            stopSelf()
            return START_NOT_STICKY
        }

        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceJob?.cancel()

        repository.startMonitoring()

        serviceJob = scope.launch {
            repository.thermalData.collect { thermal ->
                withContext(Dispatchers.Main) {
                    try {
                        if (repository.isOverlayEnabled.value) {
                            if (Settings.canDrawOverlays(this@ThermalMonitorService)) {
                                overlayWindow.show()
                                overlayWindow.update(thermal)
                            } else {
                                Log.w(TAG, "overlay enabled but permission not granted, hiding overlay.")
                                overlayWindow.hide()
                            }
                        } else {
                            overlayWindow.hide()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "startMonitoring: overlay window error", e)
                    }
                }

                val notification = withContext(Dispatchers.Default) {
                    val isNotificationEnabled = repository.isNotificationEnabled.value
                    
                    val textSize = repository.notificationTextSize.value
                    val unitSize = repository.notificationUnitSize.value
                    val useCustomColor = repository.notificationUseCustomColor.value
                    val color = repository.notificationColor.value

                    notificationHelper.buildNotification(
                        thermal, isNotificationEnabled,
                        textSize, unitSize, useCustomColor, color, isBlank = false
                    )
                }
                notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "Screen OFF: Scheduling sleep in 2 minutes")
                    stopMonitoringJob?.cancel()
                    stopMonitoringJob = scope.launch {
                        delay(2 * 60 * 1000L) // 2 minutes
                        Log.d(TAG, "Screen OFF Timeout: Stopping monitoring to save power")
                        repository.stopMonitoring()
                    }
                }

                Intent.ACTION_SCREEN_ON -> {
                    Log.d(TAG, "Screen ON: Cancelling sleep timer")
                    stopMonitoringJob?.cancel()
                    if (!repository.isMonitoring.value) {
                        Log.d(TAG, "Screen ON: Resuming monitoring")
                        startMonitoring()
                    }
                }
            }
        }
    }

    private var stopMonitoringJob: Job? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        serviceJob?.cancel()
        stopMonitoringJob?.cancel()
        overlayWindow.hide()
        repository.stopMonitoring()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
