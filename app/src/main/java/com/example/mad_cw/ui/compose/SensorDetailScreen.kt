package com.example.mad_cw.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Water
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mad_cw.data.model.SensorData

@Composable
fun SensorDetailScreen(initial: SensorData, updates: State<SensorData?>, onBack: () -> Unit) {
    val current = updates.value ?: initial

    Scaffold(topBar = {
        TopAppBar(modifier = Modifier.statusBarsPadding(), title = { Text(current.nodeName ?: "Sensor") }, navigationIcon = {
            androidx.compose.material.IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        })
    }) { padding ->
        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ShowChart, contentDescription = null, tint = MaterialTheme.colors.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Tilt: ${String.format("%.2f", current.tilt ?: 0.0)}°", style = MaterialTheme.typography.h6)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Water, contentDescription = null, tint = MaterialTheme.colors.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Soil Moisture: ${String.format("%.1f", current.soilMoisture ?: 0.0)} %")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.InvertColors, contentDescription = null, tint = MaterialTheme.colors.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Rainfall: ${String.format("%.1f", current.rain ?: 0.0)} mm")
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Simple sparkline for tilt (placeholder visualization)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp), contentAlignment = Alignment.Center
                ) {
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
}

@Preview(showBackground = true)
@Composable
fun SensorDetailPreview() {
    val sample = SensorData(nodeName = "node_1", tilt = 12.34, soilMoisture = 45.0, rain = 2.5)
    val state = remember { mutableStateOf(sample) }
    SensorDetailScreen(initial = sample, updates = state, onBack = {})
}
