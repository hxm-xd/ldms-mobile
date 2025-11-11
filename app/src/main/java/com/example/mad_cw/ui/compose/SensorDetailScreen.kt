package com.example.mad_cw.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.example.mad_cw.data.model.SensorData

@Composable
fun SensorDetailScreen(initial: SensorData, updates: State<SensorData?>, onBack: () -> Unit) {
    val current = updates.value ?: initial

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Text(text = current.nodeName ?: "Sensor", style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Tilt: ${String.format("%.2f", current.tilt ?: 0.0)}°")
            Text(text = "Soil Moisture: ${String.format("%.1f", current.soilMoisture ?: 0.0)} %")
            Text(text = "Rainfall: ${String.format("%.1f", current.rain ?: 0.0)} mm")
            Spacer(modifier = Modifier.height(16.dp))

            // Simple sparkline for tilt (placeholder visualization)
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(120.dp), contentAlignment = Alignment.Center) {
                val points = remember { mutableStateListOf<Float>() }
                LaunchedEffect(current) {
                    points.add((current.tilt ?: 0.0).toFloat())
                    if (points.size > 60) points.removeAt(0)
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    if (points.size >= 2) {
                        val step = w / (points.size - 1)
                        val max = (points.maxOrNull() ?: 1f)
                        val min = (points.minOrNull() ?: 0f)
                        val range = if (max - min == 0f) 1f else max - min
                        val path = Path()
                        points.forEachIndexed { i, v ->
                            val x = i * step
                            val y = h - ((v - min) / range) * h
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path = path, color = Color(0xFF2196F3))
                    }
                }
            }
        }
    }
}
