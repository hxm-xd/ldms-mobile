package com.example.mad_cw.ui.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mad_cw.data.model.SensorData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.Settings


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    sensors: List<SensorData>,
    currentFilter: String,
    onFilterChanged: (String) -> Unit,
    onSensorSelected: (SensorData) -> Unit,
    onNavigateToProfile: () -> Unit,
    onMapReady: (() -> Unit)? = null,
    selectedSensor: SensorData?,
    onSelectedChange: (SensorData?) -> Unit,
    favorites: Set<String>,
    onToggleFavorite: (String) -> Unit
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    val googleMapState = remember { mutableStateOf<GoogleMap?>(null) }
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            if (selectedSensor != null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = selectedSensor.nodeName ?: "Sensor")
                    Text(text = "Tilt: ${selectedSensor.tilt ?: 0.0}")
                    Text(text = "Soil: ${selectedSensor.soilMoisture ?: 0.0}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = { onSensorSelected(selectedSensor) }) { Text("View Full Details") }
                        Spacer(modifier = Modifier.width(8.dp))
                        val nodeName = selectedSensor.nodeName ?: ""
                        val isFav = favorites.contains(nodeName)
                        Button(onClick = { onToggleFavorite(nodeName) }) { Text(if (isFav) "★ Favorited" else "☆ Favorite") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { onSelectedChange(null) }) { Text("Close") }
                }
            } else {
                Box(Modifier.height(1.dp)) {}
            }
        }
    ) { padding ->
        // Compute summary values in scope
        val total = sensors.size
        val highRisk = sensors.count { levelFor(it) == "High" }
        val activeAlerts = sensors.count { (it.status ?: "").contains("alert", ignoreCase = true) }

        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { mv ->
                mv.getMapAsync { gmap ->
                    googleMapState.value = gmap
                    gmap.uiSettings.isZoomControlsEnabled = true
                    // Move to a default location
                    gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(7.29, 80.63), 8f))
                    onMapReady?.invoke()
                }
            }

            // Summary + filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(12.dp)
                    .statusBarsPadding()
            ) {
                Card(elevation = 6.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // left: summary counts
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.padding(end = 16.dp)) {
                                Text(text = "Total")
                                Text(text = "$total", style = MaterialTheme.typography.h6)
                            }
                            Column(modifier = Modifier.padding(end = 16.dp)) {
                                Text(text = "High Risk")
                                Text(text = "$highRisk", style = MaterialTheme.typography.h6)
                            }
                            Column {
                                Text(text = "Active Alerts")
                                Text(text = "$activeAlerts", style = MaterialTheme.typography.h6)
                            }
                        }

                        // right: filters
                        Row {
                            TextButton(onClick = { onFilterChanged("All") }) { Text("All") }
                            TextButton(onClick = { onFilterChanged("Low") }) { Text("Low") }
                            TextButton(onClick = { onFilterChanged("Medium") }) { Text("Medium") }
                            TextButton(onClick = { onFilterChanged("High") }) { Text("High") }
                        }
                    }
                }
            }

            // Update markers when sensors change
            LaunchedEffect(sensors, currentFilter, googleMapState.value) {
                val gmap = googleMapState.value
                if (gmap == null) return@LaunchedEffect
                withContext(Dispatchers.Main) {
                    try {
                        gmap.clear()
                        val filtered = if (currentFilter == "All") sensors else sensors.filter { levelFor(it) == currentFilter }
                        for (s in filtered) {
                            val lat = s.latitude
                            val lng = s.longitude
                            if (lat != null && lng != null) {
                                val pos = LatLng(lat, lng)
                                val marker = gmap.addMarker(
                                    MarkerOptions().position(pos).title(s.nodeName ?: "Sensor").icon(iconFor(context, s))
                                )
                                marker?.tag = s
                            }
                        }
                        if (filtered.isNotEmpty()) {
                            val first = filtered.first()
                            val fLat = first.latitude
                            val fLng = first.longitude
                            if (fLat != null && fLng != null) {
                                gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(fLat, fLng), 10f))
                            }
                        }

                        gmap.setOnMarkerClickListener { marker ->
                            val s = marker.tag as? SensorData
                            s?.let {
                                onSelectedChange(it)
                                scope.launch { scaffoldState.bottomSheetState.expand() }
                            }
                            true
                        }
                    } catch (_: Exception) {}
                }
            }
            // Bottom navigation overlaid at the bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                BottomNavigation {
                    BottomNavigationItem(
                        selected = true,
                        onClick = {},
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") }
                    )
                    BottomNavigationItem(
                        selected = false,
                        onClick = { onNavigateToProfile() },
                        icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun DashboardPreview() {
    // Simple preview that doesn't render the MapView — shows summary, filters, and bottom nav
    val sampleSensors = listOf(
        com.example.mad_cw.data.model.SensorData(nodeName = "node_1", tilt = 5.0, soilMoisture = 30.0, status = "ok"),
        com.example.mad_cw.data.model.SensorData(nodeName = "node_2", tilt = 20.0, soilMoisture = 75.0, status = "alert")
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .statusBarsPadding()) {
            Card(elevation = 6.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.padding(end = 16.dp)) {
                            Text(text = "Total")
                            Text(text = "${sampleSensors.size}", style = MaterialTheme.typography.h6)
                        }
                        Column(modifier = Modifier.padding(end = 16.dp)) {
                            Text(text = "High Risk")
                            Text(text = "${sampleSensors.count { levelFor(it) == "High" }}", style = MaterialTheme.typography.h6)
                        }
                        Column {
                            Text(text = "Active Alerts")
                            Text(text = "${sampleSensors.count { (it.status ?: "").contains("alert", ignoreCase = true) }}", style = MaterialTheme.typography.h6)
                        }
                    }

                    Row {
                        TextButton(onClick = {}) { Text("All") }
                        TextButton(onClick = {}) { Text("Low") }
                        TextButton(onClick = {}) { Text("Medium") }
                        TextButton(onClick = {}) { Text("High") }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            BottomNavigation(modifier = Modifier.navigationBarsPadding()) {
                BottomNavigationItem(selected = true, onClick = {}, icon = { Icon(Icons.Filled.Home, contentDescription = null) }, label = { Text("Dashboard") })
                BottomNavigationItem(selected = false, onClick = {}, icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) }, label = { Text("Profile") })
            }
        }
    }
}

private fun levelFor(s: SensorData): String {
    val tilt = s.tilt ?: 0.0
    val soil = s.soilMoisture ?: 0.0
    return when {
        tilt > 15 || soil > 70 -> "High"
        tilt > 10 || soil > 50 -> "Medium"
        else -> "Low"
    }
}

private fun iconFor(context: Context, sensor: SensorData): BitmapDescriptor {
    val key = levelFor(sensor)
    val color = when (key) {
        "High" -> android.R.color.holo_red_dark
        "Medium" -> android.R.color.holo_orange_dark
        else -> android.R.color.holo_green_dark
    }
    val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        this.color = androidx.core.content.ContextCompat.getColor(context, color)
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawCircle(24f, 24f, 24f, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = (context as? androidx.activity.ComponentActivity)?.lifecycle
    DisposableEffect(lifecycle, mapView) {
        mapView.onCreate(null)
        val lifecycleObserver = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) { mapView.onStart() }
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) { mapView.onResume() }
            override fun onPause(owner: androidx.lifecycle.LifecycleOwner) { mapView.onPause() }
            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) { mapView.onStop() }
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycle?.addObserver(lifecycleObserver)
        onDispose {
            lifecycle?.removeObserver(lifecycleObserver)
            mapView.onDestroy()
        }
    }
    return mapView
}
