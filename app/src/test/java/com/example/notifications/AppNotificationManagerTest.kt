package com.example.notifications

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppNotificationManagerTest {

    private lateinit var context: Application
    private lateinit var notificationManager: NotificationManager
    private lateinit var appNotificationManager: AppNotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        appNotificationManager = AppNotificationManager(context)
    }

    @Test
    fun createsNotificationChannelsOnInitialization() {
        val messagesChannel = notificationManager.getNotificationChannel(AppNotificationManager.CHANNEL_MESSAGES_ID)
        val updatesChannel = notificationManager.getNotificationChannel(AppNotificationManager.CHANNEL_UPDATES_ID)

        assertNotNull(messagesChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, messagesChannel?.importance)
        assertNotNull(updatesChannel)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, updatesChannel?.importance)
    }

    @Test
    fun showMessageNotification_whenPermissionDenied_returnsFalseAndPostsNothing() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val posted = appNotificationManager.showMessageNotification(
            chatId = "chat_denied",
            chatTitle = "Alice",
            senderName = "Alice",
            messageText = "Hello!"
        )

        assertFalse(posted)
        assertEquals(0, shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun showMessageNotification_whenPermissionGranted_postsToNotificationCenter() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val posted = appNotificationManager.showMessageNotification(
            chatId = "chat_123",
            chatTitle = "Engineering Room",
            senderName = "Simbarashe",
            messageText = "Deployment is live.",
            isGroup = true
        )

        assertTrue(posted)
        val notifications = shadowOf(notificationManager).allNotifications
        assertEquals(1, notifications.size)
        val postedNotification = notifications.first()
        assertEquals("Simbarashe • Engineering Room", postedNotification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Deployment is live.", postedNotification.extras.getString(Notification.EXTRA_TEXT))

        appNotificationManager.cancelChatNotification("chat_123")
        assertEquals(0, shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun sendTestNotification_whenPermissionGranted_postsBroadcastNotification() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val posted = appNotificationManager.sendTestNotification()

        assertTrue(posted)
        val notifications = shadowOf(notificationManager).allNotifications
        assertEquals(1, notifications.size)
    }
}
