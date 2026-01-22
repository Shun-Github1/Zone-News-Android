package com.searcher.zonenews.ui.test

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.searcher.zonenews.R
import com.searcher.zonenews.base.BaseActivity
import com.searcher.zonenews.databinding.ActivityNotificationTestBinding
import com.searcher.zonenews.utils.NotificationManager as AppNotificationManager

/**
 * Test Activity for sending local notifications
 * This helps test the notification system without needing FCM
 */
class NotificationTestActivity : BaseActivity() {
    
    private lateinit var mViewBinding: ActivityNotificationTestBinding
    private lateinit var notificationManager: NotificationManager
    
    companion object {
        private const val CHANNEL_ID = "test_notifications"
        private const val CHANNEL_NAME = "Test Notifications"
        private const val CHANNEL_DESCRIPTION = "Test notifications for development"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityNotificationTestBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        // Create test notification channel
        createTestNotificationChannel()
        
        initView()
    }
    
    private fun createTestNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun initView() {
        // Setup toolbar
        mViewBinding.toolbar.setNavigationOnClickListener { finish() }
        mViewBinding.toolbar.title = "Notification Test"
        
        // Test general notification
        mViewBinding.btnGeneralNotification.setOnClickListener {
            sendTestNotification(
                title = "Test General Notification",
                body = "This is a test general notification",
                type = "general"
            )
        }
        
        // Test news notification
        mViewBinding.btnNewsNotification.setOnClickListener {
            sendTestNotification(
                title = "Breaking News",
                body = "Check out the latest updates from Zone News",
                type = "news",
                articleId = "12345"
            )
        }
        
        // Test custom notification
        mViewBinding.btnCustomNotification.setOnClickListener {
            sendTestNotification(
                title = "Custom Notification",
                body = "This is a custom notification with special data",
                type = "custom",
                customData = mapOf(
                    "custom_field" to "custom_value",
                    "timestamp" to System.currentTimeMillis().toString()
                )
            )
        }
        
        // Test notification with action
        mViewBinding.btnActionNotification.setOnClickListener {
            sendNotificationWithAction()
        }
    }
    
    private fun sendTestNotification(
        title: String,
        body: String,
        type: String,
        articleId: String? = null,
        customData: Map<String, String> = emptyMap()
    ) {
        val notificationId = System.currentTimeMillis().toInt()
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_24)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        
        // Add custom data
        val data = mutableMapOf("type" to type)
        if (articleId != null) data["article_id"] = articleId
        data.putAll(customData)
        
        // Set content intent based on type
        val intent = when (type) {
            "news" -> {
                android.content.Intent(this, com.searcher.zonenews.ui.MainActivity::class.java).apply {
                    putExtra("article_id", articleId)
                    putExtra("open_article", true)
                    flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            }
            else -> {
                android.content.Intent(this, com.searcher.zonenews.ui.MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            }
        }
        
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, notificationId, intent,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        notificationBuilder.setContentIntent(pendingIntent)
        
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        Toast.makeText(this, "Test notification sent!", Toast.LENGTH_SHORT).show()
    }
    
    private fun sendNotificationWithAction() {
        val notificationId = System.currentTimeMillis().toInt()
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_24)
            .setContentTitle("Notification with Action")
            .setContentText("Tap to open or dismiss")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        
        // Main intent
        val mainIntent = android.content.Intent(this, com.searcher.zonenews.ui.MainActivity::class.java)
        val mainPendingIntent = android.app.PendingIntent.getActivity(
            this, notificationId, mainIntent,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        // Dismiss intent
        val dismissIntent = android.content.Intent(this, NotificationTestActivity::class.java).apply {
            action = "DISMISS"
        }
        val dismissPendingIntent = android.app.PendingIntent.getActivity(
            this, notificationId + 1, dismissIntent,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        notificationBuilder
            .setContentIntent(mainPendingIntent)
            .addAction(
                R.drawable.ic_notifications_24,
                "Open App",
                mainPendingIntent
            )
            .addAction(
                R.drawable.ic_notifications_24,
                "Dismiss",
                dismissPendingIntent
            )
        
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        Toast.makeText(this, "Action notification sent!", Toast.LENGTH_SHORT).show()
    }
}
