package com.example.smartwatch_app

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.TextView
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.*
import com.BluetoothManager
import com.HeartbeatManager
import com.SensorData
import com.example.smartwatch_app.databinding.ActivityMainBinding
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timerTask

class MainActivity : Activity() , SensorEventListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private lateinit var sensorData: SensorData
    private var lastSensorUpdateTime: Long = 0
    private lateinit var button:Button
    private lateinit var bluetoothManager: BluetoothManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorData = SensorData()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        button=findViewById(R.id.button)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val heartbeatManager = HeartbeatManager(this)
        val stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val heartRateDetector = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        val heartBeatDetector = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT)
        val pressureDetector = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, heartRateDetector, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, heartBeatDetector, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, pressureDetector, SensorManager.SENSOR_DELAY_NORMAL)
        heartbeatManager.startCollectingHeartRateData(object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
                if (availability is DataTypeAvailability) {
                    // Handle availability change.
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                val latestHeartRateDataPoint = data.getData(DataType.Companion.HEART_RATE_BPM).lastOrNull()

                // If the data point is not null, store the heart rate in the variable.
                if (latestHeartRateDataPoint != null) {
                    val heartRate = latestHeartRateDataPoint.value
                    this@MainActivity.sensorData.heartRate = heartRate

        }}})


        button.setOnClickListener {
            val sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
            val allEntries = sharedPreferences.all
            val allEntriesString = allEntries.toString()
            sendDataToPhone(allEntriesString)


//            bluetoothManager = BluetoothManager(this)
//            bluetoothManager.sendData(allEntriesString)
        }
    }
    override fun onDestroy() {
        bluetoothManager.close()
        super.onDestroy()
    }
    fun sendDataToPhone(data: String) {
        Wearable.getCapabilityClient(this)
            .getCapability("wearable_capability", CapabilityClient.FILTER_REACHABLE)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val nodes = task.result?.nodes
                    if (nodes?.isNotEmpty() == true) {
                        sendData(data)
                    } else {
                        button.text = "No Wearable Connected"
                        button.isEnabled = false
                        Handler().postDelayed({
                            button.text = "SYNCHRONIZE"
                            button.isEnabled = true
                        }, 5000)
                    }
                } else {
                    button.text = "Capability Check Failed"
                    button.isEnabled = false
                    Handler().postDelayed({
                        button.text = "SYNCHRONIZE"
                        button.isEnabled = true
                    }, 5000)
                }
            }
    }

    private fun sendData(data: String) {
        val dataMap = PutDataMapRequest.create("/data-path")
        dataMap.dataMap.putString("key_data", data)

        val request = dataMap.asPutDataRequest()
        val task = Wearable.getDataClient(this).putDataItem(request)

        // Optionally handle the result of the data transfer
        task.addOnSuccessListener {
            button.text = "sent"
            button.isEnabled = false
            Handler().postDelayed({
                button.text = "SYNCHRONIZE"
                button.isEnabled = true
            }, 5000)

        }.addOnFailureListener { exception ->
            button.text = "failed"
            button.isEnabled = false
            Handler().postDelayed({
                button.text = "SYNCHRONIZE"
                button.isEnabled = true
            }, 5000)
        }
    }


    override fun onSensorChanged(p0: SensorEvent?) {
        when (p0?.sensor?.type) {
            Sensor.TYPE_STEP_DETECTOR -> sensorData.stepCount++
            Sensor.TYPE_HEART_RATE -> sensorData.heartRate = p0.values[0].toDouble()
            Sensor.TYPE_HEART_BEAT -> sensorData.heartBeat = p0.values[0].toInt()
            Sensor.TYPE_PRESSURE -> sensorData.pressure = p0.values[0]
        }
        if (System.currentTimeMillis() - lastSensorUpdateTime > 4 * 60 * 1000) {
            lastSensorUpdateTime = System.currentTimeMillis()
            val json = JSONObject()
            json.put("step_count", sensorData.stepCount)
            json.put("heart_rate", sensorData.heartRate)
            json.put("heart_beat", sensorData.heartBeat)
            json.put("pressure", sensorData.pressure)
            val sharedPreferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().putString("sensor_data_${Date().time}", json.toString()).apply()
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        when (p1) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
            }
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
            }
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
            }
            SensorManager.SENSOR_STATUS_UNRELIABLE -> {
            }
        }
    }
}



//class MainActivity : AppCompatActivity(), DataClient.OnDataChangedListener {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        // Initialize DataClient
//        val dataClient = Wearable.getDataClient(this)
//        dataClient.addListener(this)
//    }
//
//    override fun onDataChanged(dataEvents: DataEventBuffer) {
//        // Handle incoming data from Wearable
//        for (event in dataEvents) {
//            if (event.type == DataEvent.TYPE_CHANGED) {
//                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
//                val receivedData = dataMap.getString("key_data")
//
//                // Process receivedData
//                Log.d("Received Data", receivedData ?: "No data received")
//            }
//        }
//
//        dataEvents.release()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//
//        // Remove the listener when the activity is destroyed
//        Wearable.getDataClient(this).removeListener(this)
//    }
//}
