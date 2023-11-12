package com

data class SensorData(
    var stepCount: Int,
    var heartRate: Double,
    var heartBeat: Int,
    var pressure: Float,){

    constructor() : this(0, 0.0, 0, 0.0f) }
