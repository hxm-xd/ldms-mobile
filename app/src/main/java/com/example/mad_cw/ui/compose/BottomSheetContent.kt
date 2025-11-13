package com.example.mad_cw.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mad_cw.data.model.SensorData
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Water

@Composable
fun BottomSheetSensorContent(sensor: SensorData?) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(color = MaterialTheme.colors.primary)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = sensor?.nodeName ?: "Sensor Node", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colors.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Lat: ${sensor?.latitude ?: 0.0}, Lon: ${sensor?.longitude ?: 0.0}")
            }

            Spacer(modifier = Modifier.height(12.dp))
            Card(elevation = 4.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ShowChart, contentDescription = null, tint = MaterialTheme.colors.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Tilt: ${String.format("%.2f", sensor?.tilt ?: 0.0)}°")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Water, contentDescription = null, tint = MaterialTheme.colors.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Soil Moisture: ${String.format("%.1f", sensor?.soilMoisture ?: 0.0)} %")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.InvertColors, contentDescription = null, tint = MaterialTheme.colors.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Rain: ${String.format("%.1f", sensor?.rain ?: 0.0)} mm")
                    }
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
