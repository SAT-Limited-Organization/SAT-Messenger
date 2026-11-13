package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Manages Android system notifications posted to the device Notification Center.
 * Handles NotificationChannel registration (Android 8+), runtime POST_NOTIFICATIONS
 * permission checks (Android 13+), deep-link PendingIntents, and notification cancellation.
 */
class AppNotificationManager(private val context: Context) {

    init {
        ensureNotificationChannels()
    }

    fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES_ID,
                context.getString(R.string.notification_channel_messages_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_messages_desc)
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val updatesChannel = NotificationChannel(
                CHANNEL_UPDATES_ID,
                context.getString(R.string.notification_channel_updates_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_updates_desc)
                setShowBadge(true)
            }

            manager.createNotificationChannel(messagesChannel)
            manager.createNotificationChannel(updatesChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun areNotificationsEnabled(): Boolean {
        return hasNotificationPermission() &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showMessageNotification(
        chatId: String,
        chatTitle: String,
        senderName: String,
        messageText: String,
        isGroup: Boolean = false
    ): Boolean {
        if (!hasNotificationPermission()) return false
        ensureNotificationChannels()

        val title = if (isGroup && chatTitle.isNotBlank() && chatTitle != senderName) {
            "$senderName • $chatTitle"
        } else {
            senderName.ifBlank { chatTitle.ifBlank { context.getString(R.string.app_name) } }
        }
        val body = messageText.ifBlank { "New message" }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHAT_ID, chatId)
        }

        val requestCode = notificationIdForChat(chatId)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_MESSAGES)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(requestCode, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun showBroadcastNotification(
        title: String,
        body: String,
        notificationId: Int = NOTIFICATION_ID_TEST
    ): Boolean {
        if (!hasNotificationPermission()) return false
        ensureNotificationChannels()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_UPDATES_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun sendTestNotification(): Boolean {
        return showBroadcastNotification(
            title = context.getString(R.string.test_notification_title),
            body = context.getString(R.string.test_notification_body),
            notificationId = NOTIFICATION_ID_TEST
        )
    }

    fun cancelChatNotification(chatId: String) {
        NotificationManagerCompat.from(context).cancel(notificationIdForChat(chatId))
    }

    fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    companion object {
        const val CHANNEL_MESSAGES_ID = "sat_messages_channel"
        const val CHANNEL_UPDATES_ID = "sat_updates_channel"
        const val GROUP_MESSAGES = "sat_messenger_messages"
        const val EXTRA_CHAT_ID = "extra_chat_id"
        const val NOTIFICATION_ID_TEST = 9001

        fun notificationIdForChat(chatId: String): Int {
            return (chatId.hashCode() and 0x7FFFFFFF).coerceAtLeast(100)
        }
    }
}
