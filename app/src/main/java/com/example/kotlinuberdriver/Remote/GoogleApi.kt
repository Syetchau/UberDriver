package com.example.kotlinuberdriver.Remote

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleApi {
    @GET("maps/api/directions/json")
    fun getDirection(
        @Query("mode") mode:String?,
        @Query("transit_routing_preference") transitRouting: String?,
        @Query("origin") origin: String?,
        @Query("destination") destination: String?,
        @Query("key") key:String
    ): Observable<String>
}