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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mad_cw.data.model.SensorData
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@Composable
fun SensorDetailScreen(initial: SensorData, updates: State<SensorData?>, onBack: () -> Unit) {
    val current = updates.value ?: initial
    val primary = MaterialTheme.colors.primary
    val secondary = MaterialTheme.colors.secondary
    val onSurface = MaterialTheme.colors.onSurface

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
                    .verticalScroll(rememberScrollState())
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

                // Accumulate session history as updates arrive (simple in-app history)
                val tiltValues = remember { mutableStateListOf<Float>() }
                val soilValues = remember { mutableStateListOf<Float>() }
                val rainValues = remember { mutableStateListOf<Float>() }

                LaunchedEffect(Unit) {
                    // seed with initial
                    tiltValues.add((initial.tilt ?: 0.0).toFloat())
                    soilValues.add((initial.soilMoisture ?: 0.0).toFloat())
                    rainValues.add((initial.rain ?: 0.0).toFloat())
                }
                LaunchedEffect(current) {
                    tiltValues.add((current.tilt ?: 0.0).toFloat())
                    soilValues.add((current.soilMoisture ?: 0.0).toFloat())
                    rainValues.add((current.rain ?: 0.0).toFloat())
                    val maxPoints = 240
                    if (tiltValues.size > maxPoints) tiltValues.removeAt(0)
                    if (soilValues.size > maxPoints) soilValues.removeAt(0)
                    if (rainValues.size > maxPoints) rainValues.removeAt(0)
                }

                ChartCard(
                    title = "Tilt History (live)",
                    values = tiltValues,
                    lineColor = primary.toArgb(),
                    axisTextColor = onSurface.toArgb()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ChartCard(
                    title = "Soil Moisture History (live)",
                    values = soilValues,
                    lineColor = secondary.toArgb(),
                    axisTextColor = onSurface.toArgb()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ChartCard(
                    title = "Rainfall History (live)",
                    values = rainValues,
                    lineColor = MaterialTheme.colors.primaryVariant.toArgb(),
                    axisTextColor = onSurface.toArgb()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    values: List<Float>,
    lineColor: Int,
    axisTextColor: Int
) {
    Card(
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.subtitle1)
            Spacer(modifier = Modifier.height(8.dp))
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        setTouchEnabled(true)
                        isDragEnabled = true
                        setScaleEnabled(true)
                        setPinchZoom(true)
                        axisRight.isEnabled = false
                        axisLeft.textColor = axisTextColor
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.textColor = axisTextColor
                        xAxis.setDrawGridLines(false)
                        axisLeft.setDrawGridLines(false)
                        setNoDataText("No data yet")
                    }
                },
                update = { chart ->
                    val entries = values.mapIndexed { index, v -> Entry(index.toFloat(), v) }
                    val dataSet = LineDataSet(entries, "").apply {
                        color = lineColor
                        lineWidth = 2f
                        setDrawCircles(false)
                        setDrawValues(false)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    }
                    chart.data = LineData(dataSet)
                    chart.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
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
