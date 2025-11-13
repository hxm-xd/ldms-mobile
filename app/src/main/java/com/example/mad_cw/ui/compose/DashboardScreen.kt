package com.example.mad_cw.ui.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
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
    onToggleFavorite: (String) -> Unit,
    showMyLocation: Boolean = false,
    lastKnownLocation: LatLng? = null,
    onRefreshLocation: (() -> Unit)? = null
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
                Card(modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth(), elevation = 8.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = selectedSensor.nodeName ?: "Sensor",
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tilt: ${String.format("%.2f", selectedSensor.tilt ?: 0.0)}°",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Soil Moisture: ${String.format("%.1f", selectedSensor.soilMoisture ?: 0.0)} %",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        selectedSensor.rain?.let {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Rainfall: ${String.format("%.1f", it)} mm",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = { onSensorSelected(selectedSensor) }) { Text("View Details") }
                            Spacer(modifier = Modifier.width(8.dp))
                            val nodeName = selectedSensor.nodeName ?: ""
                            val isFav = favorites.contains(nodeName)
                            Button(onClick = { onToggleFavorite(nodeName) }) { Text(if (isFav) "★ Favorited" else "☆ Favorite") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onSelectedChange(null) }) { Text("Close") }
                    }
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
                    gmap.uiSettings.isMyLocationButtonEnabled = showMyLocation
                    if (showMyLocation) {
                        try {
                            gmap.isMyLocationEnabled = true
                        } catch (_: SecurityException) { /* ignore if permission missing */ }
                    }
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
                        // left: summary counts with icons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SummaryItem(icon = Icons.Filled.Info, label = "Total", value = "$total")
                            Spacer(modifier = Modifier.width(8.dp))
                            SummaryItem(icon = Icons.Filled.Warning, label = "High Risk", value = "$highRisk")
                            Spacer(modifier = Modifier.width(8.dp))
                            SummaryItem(icon = Icons.Filled.Notifications, label = "Alerts", value = "$activeAlerts")
                        }

                        // right: filter dropdown
                        var expanded by remember { mutableStateOf(false) }
                        val levels = listOf("All", "Low", "Medium", "High")
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(text = currentFilter)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                levels.forEach { level ->
                                    DropdownMenuItem(onClick = {
                                        expanded = false
                                        if (level != currentFilter) onFilterChanged(level)
                                    }) {
                                        Text(level)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Update markers when sensors change. Use toList() so changes in content retrigger.
            LaunchedEffect(sensors.toList(), currentFilter, googleMapState.value, showMyLocation) {
                val gmap = googleMapState.value
                if (gmap == null) return@LaunchedEffect
                withContext(Dispatchers.Main) {
                    try {
                        // keep my location layer in sync with permission state
                        gmap.uiSettings.isMyLocationButtonEnabled = showMyLocation
                        if (showMyLocation) {
                            try { gmap.isMyLocationEnabled = true } catch (_: SecurityException) {}
                        }
                        gmap.clear()
                        val filtered =
                            if (currentFilter == "All") sensors else sensors.filter { levelFor(it) == currentFilter }
                        for (s in filtered) {
                            val lat = s.latitude
                            val lng = s.longitude
                            if (lat != null && lng != null) {
                                val pos = LatLng(lat, lng)
                                val marker = gmap.addMarker(
                                    MarkerOptions().position(pos).title(s.nodeName ?: "Sensor")
                                        .icon(iconFor(context, s))
                                )
                                marker?.tag = s
                            }
                        }
                        if (filtered.isNotEmpty()) {
                            val first = filtered.first()
                            val fLat = first.latitude
                            val fLng = first.longitude
                            if (fLat != null && fLng != null) {
                                gmap.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(
                                            fLat,
                                            fLng
                                        ), 10f
                                    )
                                )
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
                    } catch (_: Exception) {
                    }
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

            // Recenter and Refresh FABs (bottom-end above bottom nav)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                androidx.compose.material.FloatingActionButton(onClick = {
                    // Refresh last known location (activity handles fetching)
                    onRefreshLocation?.invoke()
                }, backgroundColor = MaterialTheme.colors.primary) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh location", tint = MaterialTheme.colors.onPrimary)
                }
                androidx.compose.material.FloatingActionButton(onClick = {
                    val gmap = googleMapState.value
                    val target = lastKnownLocation
                    if (gmap != null && target != null) {
                        gmap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(target, 14f)
                        )
                    }
                }, backgroundColor = MaterialTheme.colors.primary) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Recenter", tint = MaterialTheme.colors.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(modifier = Modifier.padding(end = 8.dp), horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colors.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label)
        }
        Text(text = value, style = MaterialTheme.typography.h6)
    }
}

// Replaced chip filters with a dropdown for compact selection

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun DashboardPreview() {
    // Simple preview that doesn't render the MapView — shows summary, filters, and bottom nav
    val sampleSensors = listOf(
        com.example.mad_cw.data.model.SensorData(
            nodeName = "node_1",
            tilt = 5.0,
            soilMoisture = 30.0,
            status = "ok"
        ),
        com.example.mad_cw.data.model.SensorData(
            nodeName = "node_2",
            tilt = 20.0,
            soilMoisture = 75.0,
            status = "alert"
        )
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.padding(end = 16.dp)) {
                            Text(text = "Total")
                            Text(
                                text = "${sampleSensors.size}",
                                style = MaterialTheme.typography.h6
                            )
                        }
                        Column(modifier = Modifier.padding(end = 16.dp)) {
                            Text(text = "High Risk")
                            Text(
                                text = "${sampleSensors.count { levelFor(it) == "High" }}",
                                style = MaterialTheme.typography.h6
                            )
                        }
                        Column {
                            Text(text = "Active")
                            Text(
                                text = "${
                                    sampleSensors.count {
                                        (it.status ?: "").contains(
                                            "alert",
                                            ignoreCase = true
                                        )
                                    }
                                }", style = MaterialTheme.typography.h6
                            )
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
                BottomNavigationItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Dashboard") })
                BottomNavigationItem(
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null) },
                    label = { Text("Profile") })
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
    // Keep alert severity colors the same: red (High), orange (Medium), green (Low)
    val hex = when (key) {
        "High" -> "#D32F2F"   // red 700
        "Medium" -> "#F57C00" // orange 700
        else -> "#388E3C"      // green 700
    }
    val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor(hex)
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
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                mapView.onStart()
            }

            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                mapView.onResume()
            }

            override fun onPause(owner: androidx.lifecycle.LifecycleOwner) {
                mapView.onPause()
            }

            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                mapView.onStop()
            }

            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                mapView.onDestroy()
            }
        }
        lifecycle?.addObserver(lifecycleObserver)
        onDispose {
            lifecycle?.removeObserver(lifecycleObserver)
            mapView.onDestroy()
        }
    }
    return mapView
}
