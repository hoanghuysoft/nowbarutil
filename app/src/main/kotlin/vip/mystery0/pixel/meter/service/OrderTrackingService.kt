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
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import com.kakao.taxi.data.repository.OrderRepository

/**
 * Foreground service that polls the tracked order at a configurable interval
 * and updates the Samsung Now Bar / notification with the latest status.
 */
class OrderTrackingService : Service() {

    companion object {
        private const val TAG = "OrderTrackingService"
    }

    private val repository: OrderRepository by inject()
    private val notificationHelper: NotificationHelper by inject()
    private val notificationManager: NotificationManager by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var serviceJob: Job? = null

    /** Pauses updates when the screen has been off for > 2 minutes. */
    private var screenOff = false
    private var screenOffJob: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOffJob = scope.launch {
                        kotlinx.coroutines.delay(2 * 60 * 1000L)
                        screenOff = true
                        Log.d(TAG, "Screen off for 2 min, pausing updates")
                    }
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOffJob?.cancel()
                    if (screenOff) {
                        screenOff = false
                        Log.d(TAG, "Screen on, resuming updates")
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotif = notificationHelper.buildNotification(
            statusText = null,
            orderCode = null,
            isLiveUpdate = false,
            isNotificationEnabled = true
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
        repository.startTracking()

        serviceJob = scope.launch {
            repository.trackedOrder.collect { detail ->
                if (screenOff) return@collect

                val statusText = detail?.trackingHistory?.lastOrNull()?.status
                    ?: detail?.status
                val orderCode = detail?.expressId

                val notification = withContext(Dispatchers.Default) {
                    val isLiveUpdate = repository.isLiveUpdateEnabled.value
                    val isNotificationEnabled = repository.isNotificationEnabled.value
                    val textSize = repository.notificationTextSize.value
                    val unitSize = repository.notificationUnitSize.value
                    val useCustomColor = repository.notificationUseCustomColor.value
                    val color = repository.notificationColor.value
                    val isBlank = repository.isBlankNotificationEnabled.value

                    notificationHelper.buildNotification(
                        statusText, orderCode, isLiveUpdate, isNotificationEnabled,
                        textSize, unitSize, useCustomColor, color, isBlank
                    )
                }
                notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onDestroy() {
        serviceJob?.cancel()
        screenOffJob?.cancel()
        repository.stopTracking()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) { }
        super.onDestroy()
    }
}
