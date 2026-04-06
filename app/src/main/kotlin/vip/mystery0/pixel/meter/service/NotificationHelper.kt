package com.kakao.taxi.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.kakao.taxi.MainActivity
import com.kakao.taxi.R
import com.kakao.taxi.data.source.ThermalData
import java.util.Locale
import kotlin.math.roundToInt

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "net_monitor_silent"
        const val NOTIFICATION_ID = 1001

        fun createNotificationChannel(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val group = NotificationChannelGroup(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name)
            )
            notificationManager.createNotificationChannelGroup(group)

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time battery status in status bar"
                setShowBadge(false)
                setGroup(CHANNEL_ID)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private val size =
        (context.resources.displayMetrics.density * 24).roundToInt().coerceAtLeast(48)
    private val bitmap = createBitmap(size, size)
    private val canvas = Canvas(bitmap)

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textSize = size * 0.50f 
    }

    private val unitPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textSize = size * 0.35f 
    }

    fun buildNotification(
        thermalData: ThermalData,
        isNotificationEnabled: Boolean,
        textSize: Float = 0.50f,
        unitSize: Float = 0.35f,
        useCustomColor: Boolean = false,
        color: Int = 0,
        isBlank: Boolean = false
    ): Notification {
        val intent = Intent().apply {
            setClassName(context, MainActivity::class.java.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setSmallIcon(R.drawable.ic_speed)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (useCustomColor) {
            builder.setColor(color)
        }

        if (isBlank) {
            val blankView = RemoteViews(context.packageName, R.layout.notification_blank)
            builder.setCustomContentView(blankView)
                .setCustomBigContentView(blankView)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        } else {
            builder.setContentTitle("Battery Thermal Monitor")
        }

        if (!isNotificationEnabled) {
            if (!isBlank) {
                builder.setContentText("Monitoring is currently disabled.")
            }
            return builder.build()
        }

        // Format temp
        val tempString = String.format(Locale.US, "%.1f", thermalData.temperatureCelsius)
        val fullText = "$tempString°C"

        // Draw Bitmap
        bitmap.eraseColor(Color.TRANSPARENT)
        val cx = size / 2f
        val cyValue = size * 0.5f 
        val cyUnit = size * 0.95f

        textPaint.textSize = size * textSize
        unitPaint.textSize = size * unitSize

        canvas.drawText(tempString, cx, cyValue, textPaint)
        canvas.drawText("°C", cx, cyUnit, unitPaint)

        val smallIcon = IconCompat.createWithBitmap(bitmap)

        if (!isBlank) {
            builder.setContentText("Temperature: $fullText")
        }
        builder.setSmallIcon(smallIcon)

        return builder.build()
    }
}
