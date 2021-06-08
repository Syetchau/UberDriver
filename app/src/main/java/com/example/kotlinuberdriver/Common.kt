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
import com.google.android.gms.maps.model.LatLng
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

object Common {
    var currentUser: DriverInfo?= null

    const val DRIVER_INFO_REFERENCE: String = "DriverInfo"
    const val DRIVERS_LOCATION_REFERENCE: String = "DriversLocation"
    const val TOKEN_REFERENCE: String = "Token"
    const val NOTIFICATION_TITLE: String = "title"
    const val NOTIFICATION_BODY: String = "body"
    const val PICKUP_LOCATION: String = "PickupLocation"
    const val PICKUP_LOCATION_STRING = "PickupLocationString"
    const val DESTINATION_LOCATION: String = "DestinationLocation"
    const val DESTINATION_LOCATION_STRING = "DestinationLocationString"
    const val RIDER_KEY: String = "RiderKey"
    const val RIDER_INFO: String = "RiderInfo"
    const val DRIVER_KEY: String = "DriverKey"
    const val REQUEST_DRIVER_TITLE: String = "RequestDriver"
    const val REQUEST_DRIVER_DECLINE: String = "Decline"
    const val REQUEST_DRIVER_ACCEPT: String = "Accept"
    const val TRIP: String = "Trips"
    const val TRIP_KEY: String = "TripKey"
    const val TRIP_PICKUP_REFERENCE: String = "TripPickupLocation"
    const val TRIP_DESTINATION_LOCATION_REFERENCE: String = "TripDestinationLocation"
    const val REQUEST_DRIVER_DECLINE_AND_REMOVE_TRIP: String = "DeclineAndRemoveTrip"
    const val RIDER_REQUEST_COMPLETE_TRIP: String = "RequestCompleteTripToRider"
    const val MIN_RANGE_PICKUP_IN_KM: Double = 0.05  //50m
    const val WAIT_TIME_IN_MIN: Int = 1

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

    fun decodePoly(encoded: String): ArrayList<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }

    fun createUniqueTripId(timeOffset: Long?): String? {
        var random = Random()
        var current = System.currentTimeMillis()+timeOffset!!
        var unique = current + random.nextLong()
        if (unique < 0) {
            unique *= -1
        }
        return unique.toString()
    }
}