package com.example.mad_cw.data.model

import java.io.Serializable

class SensorData(
    val nodeName: String? = null,
    val accelX: Double? = null,
    val accelY: Double? = null,
    val accelZ: Double? = null,
    val gyroX: Double? = null,
    val gyroY: Double? = null,
    val gyroZ: Double? = null,
    val magX: Double? = null,
    val magY: Double? = null,
    val magZ: Double? = null,
    val tilt: Double? = null,
    val rain: Double? = null,
    val soilMoisture: Double? = null,
    val light: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: String? = null,
    val status: String? = null) : Serializable {


}