package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.api.ApiClient
import com.example.data.api.model.DeviceRegistrationRequest
import com.example.data.local.TokenManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HrpFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "HrpFCM"
        const val CHANNEL_ID = "hrp_channel_notifications"
        const val CHANNEL_NAME = "Thông báo Nhân sự & Chấm công HRP"
        const val CHANNEL_DESC = "Nhận thông báo khi HR duyệt đơn, chi lương hoặc có thông báo ca làm việc"
    }

    /**
     * Called when a new Firebase token is generated.
     * Uploads the token to backend via POST /api/push/register-device
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (com.example.utils.AppConfig.isHttpLoggingEnabled) {
            Log.d(TAG, "New FCM Device Token received (length=${token.length})")
        }

        val tokenManager = TokenManager.getInstance(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                tokenManager.saveFcmToken(token)

                // If user is already logged in, register device token with backend
                if (tokenManager.isLoggedIn()) {
                    val api = ApiClient.getApiService(applicationContext)
                    api.registerDevice(
                        DeviceRegistrationRequest(
                            token = token,
                            deviceType = "ANDROID",
                            model = "${Build.MANUFACTURER} ${Build.MODEL}"
                        )
                    )
                    Log.i(TAG, "Device token successfully registered with backend server.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM device token with backend", e)
            }
        }
    }

    /**
     * Called when a push notification arrives while app is in foreground or background with data payload.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "HR Partner Thông Báo"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Bạn có cập nhật mới về chấm công hoặc phiếu lương."

        val type = remoteMessage.data["type"] // e.g. TICKET_APPROVED, ADVANCE_PAID, ATTENDANCE_ALERT
        val ticketId = remoteMessage.data["ticketId"]

        showNotification(title, body, type, ticketId)
    }

    private fun showNotification(title: String, body: String, type: String?, ticketId: String?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Android O+ Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
            putExtra("ticket_id", ticketId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.hrp_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
