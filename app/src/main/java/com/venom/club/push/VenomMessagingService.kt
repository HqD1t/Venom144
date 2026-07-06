package com.venom.club.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.venom.club.MainActivity
import com.venom.club.R

/** Пуши: подтверждение брони, ответ админа в чате, новые посты. */
class VenomMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Firebase.auth.currentUser?.uid?.let {
            Firebase.firestore.document("users/$it").update("fcmToken", token)
        }
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        val title = msg.notification?.title ?: msg.data["title"] ?: "VENOM Club"
        val body = msg.notification?.body ?: msg.data["body"] ?: return

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("venom", "VENOM Club", NotificationManager.IMPORTANCE_HIGH)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(
            System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(this, "venom")
                .setSmallIcon(R.drawable.venom_logo)
                .setContentTitle(title).setContentText(body)
                .setContentIntent(pi).setAutoCancel(true)
                .build()
        )
    }
}
