package com.example.kotlinuberdriver.Model.EventBus

class DriverRequestReceived{
    var key: String?= null
    var pickupLocation: String?= null
    var pickupLocationString: String?= null
    var destinationLocation: String?= null
    var destinationLocationString: String?= null
    var distanceText: String?= ""
    var durationText: String?= ""
    var distanceValue: Int = 0
    var durationValue: Int = 0
    var totalFee: Double = 0.0
}