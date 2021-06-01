package com.example.kotlinuberdriver.Utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Toast
import com.example.kotlinuberdriver.Common
import com.example.kotlinuberdriver.Model.EventBus.NotifyRiderEvent
import com.example.kotlinuberdriver.Model.FCMSendData
import com.example.kotlinuberdriver.Model.Token
import com.example.kotlinuberdriver.R
import com.example.kotlinuberdriver.Remote.FCMService
import com.example.kotlinuberdriver.Remote.RetrofitFCMClient
import com.example.kotlinuberdriver.Service.MyFirebaseMessagingService
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder

object UserUtils {

    fun updateUser(view: View, data: Map<String, Any>) {
        FirebaseDatabase
            .getInstance()
            .getReference(Common.DRIVER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(data)
            .addOnFailureListener { e->
                Snackbar.make(view, e.message!!, Snackbar.LENGTH_SHORT).show()
            }
            .addOnSuccessListener{
                Snackbar.make(view, "Update success!", Snackbar.LENGTH_SHORT).show()
            }
    }

    fun updateToken(context: Context, token: String) {
        val tokenModel = Token()
        tokenModel.token = token

        FirebaseDatabase
            .getInstance()
            .getReference(Common.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(tokenModel)
            .addOnFailureListener { e->
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener {  }
    }

    fun sendDeclineRequest(view: View, activity: Activity?, key: String) {
        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCMClient.instance!!.create(FCMService::class.java)
        FirebaseDatabase    //Get token
            .getInstance()
            .getReference(Common.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val tokenModel = snapshot.getValue(Token::class.java)
                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData[Common.NOTIFICATION_TITLE] = Common.REQUEST_DRIVER_DECLINE
                        notificationData[Common.NOTIFICATION_BODY] = "This message represent for decline action from driver"
                        notificationData[Common.DRIVER_KEY] = FirebaseAuth.getInstance().currentUser!!.uid

                        val fcmData = FCMSendData(tokenModel!!.token, notificationData)
                        compositeDisposable.add(fcmService.sendNotification(fcmData)!!
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({response ->
                                if (response!!.success == 0){
                                    compositeDisposable.clear()
                                    Snackbar.make(view, activity!!.getString(R.string.decline_failed),
                                        Snackbar.LENGTH_LONG).show()
                                } else {
                                    Snackbar.make(view, activity!!.getString(R.string.decline_success),
                                        Snackbar.LENGTH_LONG).show()
                                }
                            }, {t: Throwable? ->
                                compositeDisposable.clear()
                                Snackbar.make(view,t!!.message!!, Snackbar.LENGTH_LONG).show()
                            }))
                    } else{
                        compositeDisposable.clear()
                        Snackbar.make(view, activity!!.getString(R.string.token_not_found),
                            Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                }
            })
    }

    fun sendAcceptRequestToRider(view: View?, context: Context, key: String, tripNumberId: String) {
        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCMClient.instance!!.create(FCMService::class.java)
        FirebaseDatabase    //Get token
            .getInstance()
            .getReference(Common.TOKEN_REFERENCE)
            .child(key)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val tokenModel = snapshot.getValue(Token::class.java)
                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData[Common.NOTIFICATION_TITLE] = Common.REQUEST_DRIVER_ACCEPT
                        notificationData[Common.NOTIFICATION_BODY] = "This message represent for accept action from driver"
                        notificationData[Common.DRIVER_KEY] = FirebaseAuth.getInstance().currentUser!!.uid
                        notificationData[Common.TRIP_KEY] = tripNumberId

                        val fcmData = FCMSendData(tokenModel!!.token, notificationData)
                        compositeDisposable.add(fcmService.sendNotification(fcmData)!!
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({response ->
                                if (response!!.success == 0){
                                    compositeDisposable.clear()
                                    Snackbar.make(view!!, context.getString(R.string.accept_failed),
                                        Snackbar.LENGTH_LONG).show()
                                }
                            }, {t: Throwable? ->
                                compositeDisposable.clear()
                                Snackbar.make(view!!,t!!.message!!, Snackbar.LENGTH_LONG).show()
                            }))
                    } else{
                        compositeDisposable.clear()
                        Snackbar.make(view!!, context.getString(R.string.token_not_found),
                            Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view!!, error.message, Snackbar.LENGTH_LONG).show()
                }
            })
    }

    fun sendNotifyToRider(context: Context, view: View, key: String?) {
        val compositeDisposable = CompositeDisposable()
        val fcmService = RetrofitFCMClient.instance!!.create(FCMService::class.java)
        FirebaseDatabase
            .getInstance()
            .getReference(Common.TOKEN_REFERENCE)
            .child(key!!)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val tokenModel = snapshot.getValue(Token::class.java)
                        val notificationData: MutableMap<String, String> = HashMap()
                        notificationData[Common.NOTIFICATION_TITLE] = context.getString(R.string.driver_arrived)
                        notificationData[Common.NOTIFICATION_BODY] = context.getString(R.string.your_driver_arrived)
                        notificationData[Common.DRIVER_KEY] = FirebaseAuth.getInstance().currentUser!!.uid
                        notificationData[Common.RIDER_KEY] = key

                        val fcmData = FCMSendData(tokenModel!!.token, notificationData)
                        compositeDisposable.add(fcmService.sendNotification(fcmData)!!
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({response ->
                                if (response!!.success == 0){
                                    compositeDisposable.clear()
                                    Snackbar.make(view, context.getString(R.string.accept_failed),
                                        Snackbar.LENGTH_LONG).show()
                                } else {
                                    EventBus.getDefault().postSticky(NotifyRiderEvent())
                                }
                            }, {t: Throwable? ->
                                compositeDisposable.clear()
                                Snackbar.make(view,t!!.message!!, Snackbar.LENGTH_LONG).show()
                            }))
                    } else{
                        compositeDisposable.clear()
                        Snackbar.make(view, context.getString(R.string.token_not_found),
                            Snackbar.LENGTH_LONG).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show()
                }
            })
    }
}