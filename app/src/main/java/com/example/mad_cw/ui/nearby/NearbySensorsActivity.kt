package com.example.mad_cw.ui.nearby

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.mad_cw.data.model.SensorData
import com.example.mad_cw.ui.theme.LDMSTheme
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.*

class NearbySensorsActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private var userLocation: LatLng? = null
    private val nearbySensorsState = mutableStateListOf<SensorData>()
    private var loadingState by mutableStateOf(true)
    private var valueEventListener: ValueEventListener? = null
    private var highlightedSensorName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lat = intent.getDoubleExtra("user_lat", Double.NaN)
        val lng = intent.getDoubleExtra("user_lng", Double.NaN)
        if (!lat.isNaN() && !lng.isNaN()) {
            userLocation = LatLng(lat, lng)
        }
        val sensorNameFromIntent = intent.getStringExtra("sensor_name")
        if (!sensorNameFromIntent.isNullOrBlank()) {
            highlightedSensorName = sensorNameFromIntent
        }
        database = FirebaseDatabase.getInstance().reference
        loadSensorsWithinRadius(1000.0)
        setContent {
            LDMSTheme {
                NearbySensorsScreen(
                    userLocation = userLocation,
                    sensors = nearbySensorsState,
                    loading = loadingState,
                    onBack = { finish() },
                    highlightedSensorName = highlightedSensorName
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        valueEventListener?.let { database.removeEventListener(it) }
    }

    private fun loadSensorsWithinRadius(radiusMeters: Double) {
        loadingState = true
        val center = userLocation
        if (center == null) {
            loadingState = false
            return
        }

        valueEventListener?.let { database.removeEventListener(it) }
        valueEventListener = database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val list = mutableListOf<SensorData>()
                    for (child in snapshot.children) {
                        if (child.key?.startsWith("node_") == true) {
                            val node = child.getValue(SensorData::class.java)
                            val lat = node?.latitude
                            val lng = node?.longitude
                            if (node != null && lat != null && lng != null) {
                                val d = distanceMeters(center.latitude, center.longitude, lat, lng)
                                if (d <= radiusMeters) list.add(node)
                            }
                        }
                    }
                    nearbySensorsState.clear()
                    nearbySensorsState.addAll(list)
                } catch (e: Exception) {
                    Log.e("NearbySensorsActivity", "Error filtering nearby sensors: ${e.message}", e)
                } finally {
                    loadingState = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                loadingState = false
                Log.e("NearbySensorsActivity", "Firebase cancelled: ${error.message}")
            }
        })
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }
}
