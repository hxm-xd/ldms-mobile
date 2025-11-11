package com.example.mad_cw.ui.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(
    sensors: List<SensorData>,
    currentFilter: String,
    onFilterChanged: (String) -> Unit,
    onSensorSelected: (SensorData) -> Unit,
    onNavigateToProfile: () -> Unit,
    onMapReady: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    val googleMapState = remember { mutableStateOf<GoogleMap?>(null) }

    LaunchedEffect(googleMapState.value) {
        onMapReady?.invoke()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { mapView ->
            mapView.getMapAsync { gmap ->
                googleMapState.value = gmap
                gmap.uiSettings.isZoomControlsEnabled = true
                // Move to a default location
                gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(7.29, 80.63), 8f))
            }
        }

        Column(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(12.dp)) {
            Card(elevation = 6.dp) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Total: ${sensors.size}")
                    Row {
                        TextButton(onClick = { onFilterChanged("All") }) { Text("All") }
                        TextButton(onClick = { onFilterChanged("Low") }) { Text("Low") }
                        TextButton(onClick = { onFilterChanged("Medium") }) { Text("Medium") }
                        TextButton(onClick = { onFilterChanged("High") }) { Text("High") }
                    }
                }
            }
        }

        // Bottom sheet area - simple card showing selected sensor when available
        var selected by remember { mutableStateOf<SensorData?>(null) }
        if (selected != null) {
            // display simple bottom card
            Card(modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .align(Alignment.BottomCenter), elevation = 12.dp) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = selected!!.nodeName ?: "Sensor")
                    Text(text = "Tilt: ${selected!!.tilt ?: 0.0}")
                    Text(text = "Soil: ${selected!!.soilMoisture ?: 0.0}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { /* open details */ onSensorSelected(selected!!) }) { Text("View Full Details") }
                }
            }
        }

        // Bottom navigation
        BottomNavigation(modifier = Modifier.align(Alignment.BottomCenter)) {
            BottomNavigationItem(
                selected = true,
                onClick = {},
                icon = { Text("") },
                label = { Text("Dashboard") }
            )
            BottomNavigationItem(
                selected = false,
                onClick = { onNavigateToProfile() },
                icon = { Text("") },
                label = { Text("Profile") }
            )
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
                            val marker = gmap.addMarker(MarkerOptions().position(pos).title(s.nodeName ?: "Sensor").icon(iconFor(context, s)))
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
                            selected = it
                        }
                        true
                    }
                } catch (_: Exception) {}
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
