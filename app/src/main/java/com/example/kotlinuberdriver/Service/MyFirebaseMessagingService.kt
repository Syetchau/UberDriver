package com.example.kotlinuberdriver.Service

import android.util.Log
import com.example.kotlinuberdriver.Common
import com.example.kotlinuberdriver.Model.EventBus.DriverRequestReceived
import com.example.kotlinuberdriver.Utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.random.Random

class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if(FirebaseAuth.getInstance().currentUser != null) {
            UserUtils.updateToken(this, token)
        }
    }

    override fun onMessageReceived(remoteMsg: RemoteMessage) {
        super.onMessageReceived(remoteMsg)
        val data = remoteMsg.data
        if (data != null) {
            if(data[Common.NOTIFICATION_TITLE].equals(Common.REQUEST_DRIVER_TITLE)) {
                EventBus.getDefault().postSticky(DriverRequestReceived(
                    data[Common.RIDER_KEY]!!,
                    data[Common.PICKUP_LOCATION]!!)
                )
            } else {
                Common.showNotification(this, Random.nextInt(),
                    data[Common.NOTIFICATION_TITLE],
                    data[Common.NOTIFICATION_BODY],
                    null)
            }
        }
    }
}