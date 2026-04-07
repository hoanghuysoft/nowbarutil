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
import androidx.core.graphics.toColorInt
import com.kakao.taxi.MainActivity
import com.kakao.taxi.R
import kotlin.math.roundToInt

/**
 * Builds foreground service notifications for order tracking.
 *
 * Supports two display modes:
 * - **Live Update (Samsung Now Bar)**: Uses `setShortCriticalText` to push the latest
 *   order status directly into the One UI Now Bar pill.
 * - **Standard Bitmap Icon**: Renders a compact status abbreviation onto the notification
 *   small icon bitmap.
 */
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

            // IMPORTANCE_DEFAULT is required for Samsung Now Bar (Live Notification).
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows real-time order tracking status in status bar"
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
        textSize = size * 0.60f
    }

    private val unitPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textSize = size * 0.45f
    }

    /**
     * Builds a notification displaying the current order tracking status.
     *
     * @param statusText The latest status text of the tracked order (e.g. "Đang giao hàng").
     * @param orderCode The tracked order's express ID for context.
     * @param isLiveUpdate If true, pushes to Samsung Now Bar.
     * @param isNotificationEnabled Whether the notification should show live data.
     * @param textSize Relative text size for the bitmap icon (0.0–1.0).
     * @param unitSize Relative text size for the secondary line (0.0–1.0).
     * @param useCustomColor Whether to apply a custom accent color.
     * @param color The custom accent color (ARGB int).
     * @param isBlank If true, strips all content text for a minimalist icon.
     */
    fun buildNotification(
        statusText: String?,
        orderCode: String?,
        isLiveUpdate: Boolean,
        isNotificationEnabled: Boolean,
        textSize: Float = 0.60f,
        unitSize: Float = 0.45f,
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
            .setColor("#ff4600".toColorInt())

        if (useCustomColor) {
            builder.setColor(color)
        }

        if (isBlank) {
            val blankView = RemoteViews(context.packageName, R.layout.notification_blank)
            builder.setCustomContentView(blankView)
                .setCustomBigContentView(blankView)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        } else {
            builder.setContentTitle(context.getString(R.string.notification_content_title))
        }

        if (!isNotificationEnabled || statusText == null) {
            if (!isBlank) {
                builder.setContentText(context.getString(R.string.notification_content_text))
            }
            return builder.build()
        }

        // Compact status for Now Bar — truncate if too long
        val nowBarText = if (statusText.length > 30) {
            statusText.take(27) + "..."
        } else {
            statusText
        }

        if (isLiveUpdate) {
            // ── Samsung Now Bar (Live Notification) Mode ──
            if (!isBlank) {
                val displayText = if (orderCode != null) "📦 $statusText" else statusText
                builder.setContentText(displayText)
            }
            builder.setShortCriticalText(nowBarText)
                .setRequestPromotedOngoing(true)
        } else {
            // ── Standard Bitmap Icon Mode ──
            // Draw a short status abbreviation on the icon
            bitmap.eraseColor(Color.TRANSPARENT)
            val cx = size / 2f

            textPaint.textSize = size * textSize
            unitPaint.textSize = size * unitSize

            // Use first 2-3 chars as abbreviation for the icon
            val abbr = getStatusAbbreviation(statusText)
            canvas.drawText(abbr, cx, size * 0.55f, textPaint)
            canvas.drawText("📦", cx, size * 0.95f, unitPaint)

            val smallIcon = IconCompat.createWithBitmap(bitmap)

            if (!isBlank) {
                builder.setContentText("📦 $statusText")
            }
            builder.setSmallIcon(smallIcon)
        }

        return builder.build()
    }

    /**
     * Creates a short abbreviation from an order status for the bitmap icon.
     * Vietnamese statuses are common so we handle them specifically.
     */
    private fun getStatusAbbreviation(status: String): String {
        return when {
            status.contains("thành công", ignoreCase = true) || status.contains("成功", ignoreCase = true) -> "✓"
            status.contains("giao hàng", ignoreCase = true) || status.contains("派送", ignoreCase = true) || status.contains("派件", ignoreCase = true) -> "🚚"
            status.contains("trung chuyển", ignoreCase = true) || status.contains("运输", ignoreCase = true) || status.contains("中转", ignoreCase = true) -> "→"
            status.contains("kho", ignoreCase = true) || status.contains("仓库", ignoreCase = true) || status.contains("分拨", ignoreCase = true) -> "📦"
            status.contains("lấy hàng", ignoreCase = true) || status.contains("取件", ignoreCase = true) || status.contains("揽收", ignoreCase = true) -> "↑"
            status.contains("chuẩn bị", ignoreCase = true) || status.contains("准备", ignoreCase = true) -> "⏳"
            status.contains("delivered", ignoreCase = true) -> "✓"
            status.contains("transit", ignoreCase = true) -> "→"
            else -> status.take(2).uppercase()
        }
    }
}
