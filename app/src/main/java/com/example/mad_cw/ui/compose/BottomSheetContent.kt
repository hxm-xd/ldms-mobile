package com.example.mad_cw.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mad_cw.data.model.SensorData

@Composable
fun BottomSheetSensorContent(sensor: SensorData?) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Drag handle
            Box(modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(color = MaterialTheme.colors.primary))

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = sensor?.nodeName ?: "Sensor Node", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Lat: ${sensor?.latitude ?: 0.0}, Lon: ${sensor?.longitude ?: 0.0}")

            Spacer(modifier = Modifier.height(12.dp))
            Card(elevation = 4.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Accel: X=${sensor?.accelX ?: 0.0}, Y=${sensor?.accelY ?: 0.0}, Z=${sensor?.accelZ ?: 0.0}")
                    Text(text = "Tilt: ${sensor?.tilt ?: 0.0}°")
                    Text(text = "Rain: ${sensor?.rain ?: 0.0} mm")
                    Text(text = "Soil Moisture: ${sensor?.soilMoisture ?: 0.0} %")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            val threat = sensor?.let { s ->
                val tilt = s.tilt ?: 0.0
                val soil = s.soilMoisture ?: 0.0
                when {
                    tilt > 15 || soil > 70 -> "High"
                    tilt > 10 || soil > 50 -> "Medium"
                    else -> "Low"
                }
            } ?: "Unknown"
            Text(text = "Threat Level: $threat", color = MaterialTheme.colors.primary)
        }
    }
}
