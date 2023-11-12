package com

import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.DataType
import androidx.health.services.client.unregisterMeasureCallback

class HeartbeatManager(context: Context) {

    private val healthClient = HealthServices.getClient(context)
    private val measureClient = healthClient.measureClient
    private var heartRateCallback: MeasureCallback? = null

    fun startCollectingHeartRateData(callback: MeasureCallback) {
        heartRateCallback = callback
        measureClient.registerMeasureCallback(
            DataType.Companion.HEART_RATE_BPM,
            heartRateCallback!!
        )
    }

    suspend fun stopCollectingHeartRateData() {
        measureClient.unregisterMeasureCallback(
            DataType.Companion.HEART_RATE_BPM,
            heartRateCallback!!
        )
    }
}
