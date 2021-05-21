package com.example.kotlinuberdriver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.kotlinuberdriver.Model.DriverInfo
import java.lang.StringBuilder

object Common {
    var currentUser: DriverInfo?= null

    const val DRIVER_INFO_REFERENCE: String = "DriverInfo"
    const val DRIVERS_LOCATION_REFERENCE: String = "DriversLocation"
    const val TOKEN_REFERENCE: String = "Token"
    const val NOTIFICATION_TITLE: String = "title"
    const val NOTIFICATION_BODY: String = "body"

    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome, ")
            .append(currentUser!!.firstName)
            .append("")
            .append(currentUser!!.lastName)
            .toString()
    }

    fun showNotification(context: Context, id: Int, title: String?, body: String?, intent: Intent?) {
        var pendingIntent: PendingIntent?= null
        val notificationChannelId = "kotlin_uber_driver"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (intent != null) {
            pendingIntent = PendingIntent.getActivity(
                context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                notificationChannelId, "Uber Remake", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = "Uber Remake"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(context, notificationChannelId)
        builder
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_baseline_directions_car)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources,
                R.drawable.ic_baseline_directions_car))

        if(pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        val notification = builder.build()
        notificationManager.notify(id, notification)
    }
}