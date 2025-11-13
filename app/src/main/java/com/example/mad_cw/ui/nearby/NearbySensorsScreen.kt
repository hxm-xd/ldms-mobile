package com.example.mad_cw.ui.nearby

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mad_cw.data.model.SensorData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

@Composable
fun NearbySensorsScreen(
    userLocation: LatLng?,
    sensors: List<SensorData>,
    loading: Boolean,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Sensors (1km)") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 4.dp,
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { padding ->
        if (userLocation == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("User location unavailable", style = MaterialTheme.typography.body1)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(elevation = 6.dp, modifier = Modifier.fillMaxWidth().height(220.dp)) {
                val mapView = rememberMapViewWithLifecycle()
                val mapState = remember { mutableStateOf<GoogleMap?>(null) }
                AndroidView(factory = { mapView }) { mv ->
                    mv.getMapAsync { gmap ->
                        mapState.value = gmap
                        gmap.uiSettings.isZoomControlsEnabled = false
                        gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                        gmap.addCircle(
                            CircleOptions()
                                .center(userLocation)
                                .radius(1000.0)
                                .strokeColor(0x553888EC)
                                .fillColor(0x223888EC)
                        )
                        for (s in sensors) {
                            val lat = s.latitude
                            val lng = s.longitude
                            if (lat != null && lng != null) {
                                gmap.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(lat, lng))
                                        .title(s.nodeName ?: "Sensor")
                                )
                            }
                        }
                    }
                }
            }
            Card(elevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Sensors within 1km", style = MaterialTheme.typography.h6)
                    Spacer(Modifier.height(8.dp))
                    if (loading) {
                        // Use a non-animated static indicator to avoid runtime animation API mismatches
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(MaterialTheme.colors.primary.copy(alpha = 0.25f))
                        )
                    }
                    if (!loading && sensors.isEmpty()) {
                        Text(
                            text = "No sensors found in this radius.",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sensors) { sensor ->
                                SensorRow(sensor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorRow(sensor: SensorData) {
    Card(elevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(sensor.nodeName ?: "Sensor", style = MaterialTheme.typography.subtitle1)
            val tilt = sensor.tilt ?: 0.0
            val soil = sensor.soilMoisture ?: 0.0
            val rain = sensor.rain ?: 0.0
            Text("Tilt: %.2f°".format(tilt))
            Text("Soil: %.1f %%".format(soil))
            Text("Rain: %.1f mm".format(rain))
        }
    }
}

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = (context as? androidx.activity.ComponentActivity)?.lifecycle
    DisposableEffect(lifecycle, mapView) {
        mapView.onCreate(null)
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) { mapView.onStart() }
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) { mapView.onResume() }
            override fun onPause(owner: androidx.lifecycle.LifecycleOwner) { mapView.onPause() }
            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) { mapView.onStop() }
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    return mapView
}
