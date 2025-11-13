package com.example.mad_cw.ui.dashboard

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.MaterialTheme
import com.example.mad_cw.ui.theme.LDMSTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mad_cw.data.model.SensorData
import com.example.mad_cw.data.repository.AuthRepository
import com.example.mad_cw.ui.auth.LoginActivity
import com.example.mad_cw.ui.compose.DashboardScreen
import com.example.mad_cw.ui.sensor.SensorDetailActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Marker
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class DashboardActivity : AppCompatActivity() {

    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference
    private lateinit var usersDatabase: DatabaseReference
    private lateinit var favoritesDatabase: DatabaseReference

    // Legacy view fields removed - UI is now driven by Jetpack Compose

    // Legacy refresh loop removed; realtime listeners handle updates

    private var currentFilter: String = "All" // All, Low, Medium, High
    private var selectedSensor: SensorData? = null
    private var sensorMarkers = mutableMapOf<String, Marker>()
    private var allSensors = mutableListOf<SensorData>()
    private var favoriteSensors = mutableSetOf<String>()
    private var userAssignedSensors = mutableSetOf<String>()
    private var valueEventListener: ValueEventListener? = null
    private val authRepository = AuthRepository()
    private var userId: String = ""
    private val markerIconCache = mutableMapOf<String, BitmapDescriptor>()
    private var locationPermissionGrantedState by mutableStateOf(false)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocationState by mutableStateOf<LatLng?>(null)
    private var refreshingLocationState by mutableStateOf(false)

    companion object {
        private const val PERMISSION_REQUEST_LOCATION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize firebase refs and auth
        database = FirebaseDatabase.getInstance().reference
        usersDatabase = FirebaseDatabase.getInstance().getReference("users")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val user = authRepository.getCurrentUser()
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        userId = user.uid
        favoritesDatabase = FirebaseDatabase.getInstance().getReference("users/$userId/favorites")

        // Compose state
        val sensorsState = mutableStateListOf<SensorData>()
        var currentFilterState by mutableStateOf(currentFilter)
        var selectedSensorState by mutableStateOf<SensorData?>(null)
        var selectedSensorName: String? = null
        var favoritesState by mutableStateOf<Set<String>>(emptySet())

        // Functions to mutate favorites in Firebase and update Compose state
        fun toggleFavoriteCompose(sensorName: String) {
            if (favoritesState.contains(sensorName)) {
                favoritesDatabase.child(sensorName).removeValue()
            } else {
                favoritesDatabase.child(sensorName).setValue(true)
            }
        }

        ensureLocationPermission()

        setContent {
            LDMSTheme {
                DashboardScreen(
                sensors = sensorsState,
                currentFilter = currentFilterState,
                onFilterChanged = { f -> currentFilterState = f },
                onSensorSelected = { sensor ->
                    val intent = Intent(this@DashboardActivity, SensorDetailActivity::class.java)
                    intent.putExtra("sensorData", sensor)
                    startActivity(intent)
                },
                onNavigateToNearby = {
                    val loc = lastKnownLocationState
                    if (loc != null) {
                        val i = Intent(this@DashboardActivity, com.example.mad_cw.ui.nearby.NearbySensorsActivity::class.java)
                        i.putExtra("user_lat", loc.latitude)
                        i.putExtra("user_lng", loc.longitude)
                        startActivity(i)
                    } else {
                        showSnackbar("Location not available")
                    }
                },
                onNavigateToProfile = {
                    startActivity(
                        Intent(
                            this@DashboardActivity,
                            com.example.mad_cw.ui.profile.ProfileActivity::class.java
                        )
                    )
                },
                onMapReady = {
                    // once map is ready, load sensors
                    loadSensorsIntoState(sensorsState)
                },
                selectedSensor = selectedSensorState,
                onSelectedChange = { s ->
                    selectedSensorState = s
                    selectedSensorName = s?.nodeName
                },
                favorites = favoritesState,
                onToggleFavorite = { name -> toggleFavoriteCompose(name) },
                showMyLocation = locationPermissionGrantedState,
                lastKnownLocation = lastKnownLocationState,
                onRefreshLocation = { refreshLastKnownLocation() },
                isRefreshingLocation = refreshingLocationState
                )
            }
        }

        // start realtime listener for push updates
        setupRealtimeListenerForState(sensorsState) { updatedList ->
            // Keep the bottom sheet's selected sensor fresh
            val name = selectedSensorName
            if (name != null) {
                val refreshed = updatedList.find { it.nodeName == name }
                if (refreshed != null) {
                    selectedSensorState = refreshed
                }
            }
        }
        loadFavoriteSensorsCompose { newSet -> favoritesState = newSet }
    }

    // initViews, view-binding and click handlers removed - Compose manages the UI now

    // setupBottomSheet removed; Compose bottom sheet UI should be used instead

    // setupFilters removed; Compose provides filter UI

    private fun setupMenu() {
        Log.d("DashboardActivity", "setupMenu: Starting.")
        // Menu is now handled in Compose (top/right actions) - legacy PopupMenu removed
        Log.d("DashboardActivity", "setupMenu: Finished.")
    }

    // The rest of your code (loadSensors, updateMapMarkers, showSensorDetails, etc.) remains unchanged
    // Use a Toast for simple messages; Compose-level UI should handle user-facing notifications
    private fun showSnackbar(message: String) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("DashboardActivity", "showSnackbar failed: ${e.message}", e)
        }
    }

    // loadUserPreferences removed; filtering handled via Compose state

    private fun loadFavoriteSensors() {
        Log.d("DashboardActivity", "loadFavoriteSensors: Starting.")
        // Legacy: kept for non-Compose code paths - prefer loadFavoriteSensorsCompose
        favoritesDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("DashboardActivity", "loadFavoriteSensors: onDataChange.")
                favoriteSensors.clear()
                for (child in snapshot.children) {
                    child.key?.let { favoriteSensors.add(it) }
                }
                // legacy favorite button update removed; Compose handles favorite UI
                Log.d("DashboardActivity", "loadFavoriteSensors: Favorites updated.")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DashboardActivity", "loadFavoriteSensors: onCancelled: ${error.message}")
            }
        })
    }

    // Compose-friendly favorite loader: calls callback with the Set<String> of favorites
    private fun loadFavoriteSensorsCompose(onUpdate: (Set<String>) -> Unit) {
        favoritesDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val favs = mutableSetOf<String>()
                for (child in snapshot.children) {
                    child.key?.let { favs.add(it) }
                }
                onUpdate(favs)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "DashboardActivity",
                    "loadFavoriteSensorsCompose: onCancelled: ${error.message}"
                )
            }
        })
    }

    // Legacy favorite helpers removed; favorites are handled via Compose state and Firebase

    override fun onStart() {
        super.onStart()
        Log.d("DashboardActivity", "onStart: Activity starting.")
    }

    override fun onResume() {
        super.onResume()
        Log.d("DashboardActivity", "onResume: Activity resuming.")
        // Real-time updates handled by setupRealtimeListenerForState started in onCreate
        Log.d("DashboardActivity", "onResume: Resumed (realtime handled by Compose setup).")
    }

    override fun onPause() {
        super.onPause()
        Log.d("DashboardActivity", "onPause: Pausing activity.")
        // Detach legacy listener if present
        valueEventListener?.let { database.removeEventListener(it) }
        Log.d("DashboardActivity", "onPause: Paused.")
    }

    // onMapReady removed; Map is handled inside Compose DashboardScreen

    // loadSensors removed; Compose helpers load data directly

    // New helper to populate a Compose state list from Firebase
    private fun loadSensorsIntoState(stateList: MutableList<SensorData>) {
        try {
            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newList = mutableListOf<SensorData>()
                    for (child in snapshot.children) {
                        if (child.key?.startsWith("node_") == true) {
                            val node = child.getValue(SensorData::class.java)
                            if (node != null) {
                                if (userAssignedSensors.isEmpty() || userAssignedSensors.contains(node.nodeName)) {
                                    newList.add(node)
                                }
                            }
                        }
                    }
                    stateList.clear(); stateList.addAll(newList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DashboardActivity", "loadSensorsIntoState cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in loadSensorsIntoState: ${e.message}", e)
        }
    }

    private fun updateSensorsFromSnapshot(snapshot: DataSnapshot) {
        Log.d("DashboardActivity", "updateSensorsFromSnapshot: Updating sensors from snapshot")
        val previousHighRiskSensors =
            allSensors.filter { getThreatLevel(it) == "High" }.map { it.nodeName }.toSet()
        allSensors.clear()
        for (child in snapshot.children) {
            // Only process children that are sensor nodes (e.g., "node_1")
            if (child.key?.startsWith("node_") == true) {
                val node = child.getValue(SensorData::class.java)
                Log.d("DashboardActivity", "Parsed node from Firebase: $node")
                if (node == null) {
                    Log.e("DashboardActivity", "SensorData is null for child: ${child.key}")
                    continue
                }
                // Filter by user assigned sensors if available
                if (userAssignedSensors.isEmpty() || userAssignedSensors.contains(node.nodeName)) {
                    allSensors.add(node)
                }
            }
        }

        // Check for new high-risk sensors and trigger notifications
        val newHighRiskSensors = allSensors
            .filter { getThreatLevel(it) == "High" && !previousHighRiskSensors.contains(it.nodeName) }
            .map { it.nodeName }

        if (newHighRiskSensors.isNotEmpty()) {
            // In a real implementation, you would send FCM notifications here
            // For now, we'll just log it
            android.util.Log.d(
                "DashboardActivity",
                "New high-risk sensors detected: $newHighRiskSensors"
            )
        }

        // Summary card now driven by Compose; legacy map update removed

        // Update bottom sheet if sensor is selected
        selectedSensor?.let { sensor ->
            val updatedSensor = allSensors.find { it.nodeName == sensor.nodeName }
            updatedSensor?.let { showSensorDetails(it) }
        }
    }

    // Summary is driven by Compose; legacy updateSummaryCard removed.

    // updateMapMarkers removed; markers are rendered inside Compose DashboardScreen

    private fun showSensorDetails(sensor: SensorData) {
        // Convert legacy bottom-sheet population into internal selection state
        selectedSensor = sensor
        Log.d("DashboardActivity", "showSensorDetails: selected ${sensor.nodeName}")
    }

    // Marker icon cache and precaching removed; Compose provides marker icons

    private fun getThreatLevel(sensor: SensorData): String {
        val tilt = sensor.tilt ?: 0.0
        val soil = sensor.soilMoisture ?: 0.0
        return when {
            tilt > 15 || soil > 70 -> "High"
            tilt > 10 || soil > 50 -> "Medium"
            else -> "Low"
        }
    }

    // applyFilter removed; Compose handles filtering and map updates

    // Legacy realtime listener removed; using setupRealtimeListenerForState for Compose

    // New helper to set up realtime listener that updates a Compose state list
    private fun setupRealtimeListenerForState(
        stateList: MutableList<SensorData>,
        onUpdated: ((List<SensorData>) -> Unit)? = null
    ) {
        valueEventListener?.let { database.removeEventListener(it) }
        valueEventListener = database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newList = mutableListOf<SensorData>()
                for (child in snapshot.children) {
                    if (child.key?.startsWith("node_") == true) {
                        val node = child.getValue(SensorData::class.java)
                        if (node != null) {
                            if (userAssignedSensors.isEmpty() || userAssignedSensors.contains(node.nodeName)) {
                                newList.add(node)
                            }
                        }
                    }
                }
                stateList.clear(); stateList.addAll(newList)
                onUpdated?.invoke(newList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DashboardActivity", "Realtime listener cancelled: ${error.message}")
            }
        })
    }

    private fun ensureLocationPermission() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            locationPermissionGrantedState = true
            refreshLastKnownLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_LOCATION
            )
        }
    }

    private fun refreshLastKnownLocation() {
        if (!locationPermissionGrantedState) return
        try {
            refreshingLocationState = true
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    lastKnownLocationState = LatLng(loc.latitude, loc.longitude)
                    showSnackbar("Location updated")
                } else {
                    showSnackbar("Location unavailable")
                }
            }.addOnFailureListener {
                // no-op: keep lastKnownLocationState as is
                showSnackbar("Location refresh failed")
            }.addOnCompleteListener {
                refreshingLocationState = false
            }
        } catch (_: SecurityException) { }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            locationPermissionGrantedState =
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (locationPermissionGrantedState) refreshLastKnownLocation()
        }
    }
}
