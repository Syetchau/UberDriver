package com.example.kotlinuberdriver.Model

class FCMResponse {
    var multicastId: Long = 0
    var success = 0
    var failed = 0
    var canonicalIds = 0
    var results: List<FCMResult>?= null
    var messageId: Long = 0
}