package com.example.kotlinuberdriver.Remote

import com.example.kotlinuberdriver.Model.FCMResponse
import com.example.kotlinuberdriver.Model.FCMSendData
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface FCMService {
    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAAqIj7A_I:APA91bHKqoSl2wkDd_Dj4z3sRflw3jtY3INKTCIDJVGY_yLdA7FEITGXu9ypYv6mgIO91sQzZt_sMsSleX7BVCucEwvqc3_0FrHcR4KxxFpNPGCatASpi1-oSiJQc9JyYNBi4uCCnhXS"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: FCMSendData?): Observable<FCMResponse?>?
}