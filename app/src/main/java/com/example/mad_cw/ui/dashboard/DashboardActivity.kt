package com.example.mad_cw.ui.dashboard

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.mad_cw.R
import com.example.mad_cw.data.model.SensorData
import com.example.mad_cw.data.repository.AuthRepository
import com.example.mad_cw.ui.auth.LoginActivity
import com.example.mad_cw.ui.sensor.SensorDetailActivity
import com.example.mad_cw.ui.settings.SettingsActivity
import com.example.mad_cw.util.FirebaseValidator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*

class DashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var rootCoordinator: CoordinatorLayout
    private lateinit var mMap: GoogleMap
    private lateinit var database: DatabaseReference
    private lateinit var usersDatabase: DatabaseReference
    private lateinit var favoritesDatabase: DatabaseReference

    private lateinit var bottomSheet: ConstraintLayout
    private lateinit var sensorName: TextView
    private lateinit var sensorTilt: TextView
    private lateinit var sensorSoil: TextView
    private lateinit var sensorRain: TextView
    private lateinit var sensorThreat: TextView
    private lateinit var btnViewDetails: Button
    private lateinit var btnFavorite: ImageButton
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private lateinit var totalSensors: TextView
    private lateinit var highRiskSensors: TextView
    private lateinit var activeAlerts: TextView

    private lateinit var filterAll: Chip
    private lateinit var filterLow: Chip
    private lateinit var filterMedium: Chip
    private lateinit var filterHigh: Chip

    private lateinit var btnMenu: ImageButton

    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 45000L // 45 seconds
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadSensors()
            handler.postDelayed(this, refreshInterval)
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize CoordinatorLayout for safe Snackbars
        rootCoordinator = findViewById(R.id.rootCoordinator)

        // Defensive map fragment initialization
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        if (mapFragment == null) {
            Toast.makeText(this, "Map could not be loaded", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        mapFragment.getMapAsync(this)

        // Check authentication
        val user = authRepository.getCurrentUser()
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        userId = user.uid
        if (userId.isEmpty()) {
            authRepository.logoutUser()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        try {
            // Firebase references
            database = FirebaseDatabase.getInstance().reference
            usersDatabase = FirebaseDatabase.getInstance().getReference("users")
            favoritesDatabase = FirebaseDatabase.getInstance().getReference("users/$userId/favorites")

            initViews()
            setupBottomSheet()
            setupFilters()
            setupMenu()

            // Initialize summary card with default values
            updateSummaryCard()

            // Load user preferences and favorites asynchronously
            loadUserPreferences()
            loadFavoriteSensors()
        } catch (e: Exception) {
            showSnackbar("Error initializing dashboard: ${e.message}")
        }
    }

    private fun initViews() {
        try {
            bottomSheet = findViewById(R.id.bottomSheet)
            sensorName = findViewById(R.id.sensorName)
            sensorTilt = findViewById(R.id.sensorTilt)
            sensorSoil = findViewById(R.id.sensorSoil)
            sensorRain = findViewById(R.id.sensorRain)
            sensorThreat = findViewById(R.id.sensorThreat)
            btnViewDetails = findViewById(R.id.btnViewDetails)
            btnFavorite = findViewById(R.id.btnFavorite)
            btnMenu = findViewById(R.id.btnMenu)

            totalSensors = findViewById(R.id.totalSensors)
            highRiskSensors = findViewById(R.id.highRiskSensors)
            activeAlerts = findViewById(R.id.activeAlerts)

            filterAll = findViewById(R.id.filterAll)
            filterLow = findViewById(R.id.filterLow)
            filterMedium = findViewById(R.id.filterMedium)
            filterHigh = findViewById(R.id.filterHigh)

            btnViewDetails.setOnClickListener {
                selectedSensor?.let { sensor ->
                    val intent = Intent(this, SensorDetailActivity::class.java)
                    intent.putExtra("sensorData", sensor)
                    startActivity(intent)
                    @Suppress("DEPRECATION")
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            }

            btnFavorite.setOnClickListener {
                selectedSensor?.let { sensor ->
                    toggleFavorite(sensor.nodeName ?: "")
                }
            }
            Log.d("DashboardActivity", "initViews: Finished.")
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in initViews: ${e.message}", e)
            showSnackbar("Error initializing views: ${e.message}")
        }
    }

    private fun setupBottomSheet() {
        Log.d("DashboardActivity", "setupBottomSheet: Starting.")
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        Log.d("DashboardActivity", "setupBottomSheet: Finished.")
    }

    private fun setupFilters() {
        Log.d("DashboardActivity", "setupFilters: Starting.")
        filterAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = "All"
                filterLow.isChecked = false
                filterMedium.isChecked = false
                filterHigh.isChecked = false
                applyFilter()
            }
        }

        filterLow.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = "Low"
                filterAll.isChecked = false
                filterMedium.isChecked = false
                filterHigh.isChecked = false
                applyFilter()
            }
        }

        filterMedium.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = "Medium"
                filterAll.isChecked = false
                filterLow.isChecked = false
                filterHigh.isChecked = false
                applyFilter()
            }
        }

        filterHigh.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                currentFilter = "High"
                filterAll.isChecked = false
                filterLow.isChecked = false
                filterMedium.isChecked = false
                applyFilter()
            }
        }
        Log.d("DashboardActivity", "setupFilters: Finished.")
    }

    private fun setupMenu() {
        Log.d("DashboardActivity", "setupMenu: Starting.")
        btnMenu.setOnClickListener {
            val popup = androidx.appcompat.widget.PopupMenu(this, btnMenu)
            popup.menuInflater.inflate(R.menu.dashboard_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_settings -> {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        @Suppress("DEPRECATION")
                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        true
                    }
                    R.id.menu_logout -> {
                        authRepository.logoutUser()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        Log.d("DashboardActivity", "setupMenu: Finished.")
    }

    // The rest of your code (loadSensors, updateMapMarkers, showSensorDetails, etc.) remains unchanged
    // Only change Snackbar.make() calls to use rootCoordinator for safety
    private fun showSnackbar(message: String) {
        if (::rootCoordinator.isInitialized) {
            Snackbar.make(rootCoordinator, message, Snackbar.LENGTH_SHORT).show()
        } else {
            Log.e("DashboardActivity", "Snackbar failed: rootCoordinator not initialized. Message: $message")
        }
    }

    private fun loadUserPreferences() {
        Log.d("DashboardActivity", "loadUserPreferences: Starting.")
        try {
            if (userId.isNotEmpty()) {
                usersDatabase.child(userId).child("assignedSensors").addListenerForSingleValueEvent(
                    object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            Log.d("DashboardActivity", "loadUserPreferences: onDataChange.")
                            try {
                                userAssignedSensors.clear()
                                for (child in snapshot.children) {
                                    child.value?.toString()?.let { sensorName ->
                                        userAssignedSensors.add(sensorName)
                                    }
                                }
                                // If no assigned sensors, load all sensors
                                if (userAssignedSensors.isEmpty()) {
                                    // Only load sensors if map is ready
                                    if (::mMap.isInitialized) {
                                        loadSensors()
                                    }
                                }
                                Log.d("DashboardActivity", "loadUserPreferences: Processed assigned sensors.")
                            } catch (e: Exception) {
                                Log.e("DashboardActivity", "Error processing user preferences: ${e.message}", e)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("DashboardActivity", "Error loading user preferences: ${error.message}")
                            // On error, load all sensors if map is ready
                            if (::mMap.isInitialized) {
                                loadSensors()
                            }
                        }
                    }
                )
            } else {
                Log.d("DashboardActivity", "loadUserPreferences: No user ID, loading all sensors.")
                // If no user ID, load all sensors if map is ready
                if (::mMap.isInitialized) {
                    loadSensors()
                }
            }
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in loadUserPreferences: ${e.message}", e)
            // On error, try to load sensors if map is ready
            if (::mMap.isInitialized) {
                loadSensors()
            }
        }
    }

    private fun loadFavoriteSensors() {
        Log.d("DashboardActivity", "loadFavoriteSensors: Starting.")
        favoritesDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("DashboardActivity", "loadFavoriteSensors: onDataChange.")
                favoriteSensors.clear()
                for (child in snapshot.children) {
                    child.key?.let { favoriteSensors.add(it) }
                }
                updateFavoriteButton()
                Log.d("DashboardActivity", "loadFavoriteSensors: Favorites updated.")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DashboardActivity", "loadFavoriteSensors: onCancelled: ${error.message}")
            }
        })
    }

    private fun toggleFavorite(sensorName: String) {
        if (favoriteSensors.contains(sensorName)) {
            favoritesDatabase.child(sensorName).removeValue()
        } else {
            favoritesDatabase.child(sensorName).setValue(true)
        }
    }

    private fun updateFavoriteButton() {
        selectedSensor?.nodeName?.let { name ->
            val isFavorite = favoriteSensors.contains(name)
            btnFavorite.setImageResource(
                if (isFavorite) android.R.drawable.star_big_on else android.R.drawable.star_big_off
            )
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("DashboardActivity", "onStart: Activity starting.")
    }

    override fun onResume() {
        super.onResume()
        Log.d("DashboardActivity", "onResume: Activity resuming.")
        handler.post(refreshRunnable)
        setupRealtimeListener()
        Log.d("DashboardActivity", "onResume: Refresh runnable and realtime listener started.")
    }

    override fun onPause() {
        super.onPause()
        Log.d("DashboardActivity", "onPause: Pausing activity.")
        handler.removeCallbacks(refreshRunnable)
        valueEventListener?.let { database.removeEventListener(it) }
        Log.d("DashboardActivity", "onPause: Refresh runnable and realtime listener stopped.")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("DashboardActivity", "onMapReady: Map is ready.")
        try {
            mMap = googleMap
            mMap.uiSettings.isZoomControlsEnabled = true
            mMap.uiSettings.isZoomGesturesEnabled = true

            // Set default camera position (Sri Lanka area)
            val defaultLocation = LatLng(7.2906, 80.6337)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 8f))

            // Validate Firebase structure before loading sensors
            Log.d("DashboardActivity", "onMapReady: Validating Firebase connection...")
            FirebaseValidator.validateConnection { isValid, message ->
                Log.d("DashboardActivity", "Firebase Validation Result:\n$message")
                runOnUiThread {
                    if (isValid) {
                        Toast.makeText(this, "✅ Firebase connection validated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "⚠️ Firebase validation issues - check Logcat", Toast.LENGTH_LONG).show()
                    }
                }
            }

            // Now that map is ready, load sensors
            Log.d("DashboardActivity", "onMapReady: Calling loadSensors.")
            loadSensors()
            // Pre-cache marker icons
            precacheMarkerIcons()
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in onMapReady: ${e.message}", e)
            showSnackbar("Error loading map: ${e.message}")
        }
    }

    private fun loadSensors() {
        if (!::mMap.isInitialized) {
            Log.w("DashboardActivity", "loadSensors: Map not ready yet, skipping")
            return
        }

        Log.d("DashboardActivity", "loadSensors: Loading sensors from Firebase.")
        try {
            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        Log.d("DashboardActivity", "loadSensors: onDataChange. Data received.")
                        updateSensorsFromSnapshot(snapshot)
                    } catch (e: Exception) {
                        Log.e("DashboardActivity", "Error processing sensor data: ${e.message}", e)
                        showSnackbar("Error processing sensor data")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DashboardActivity", "loadSensors: onCancelled - ${error.message}")
                    // Don't show toast if it's a permission error (expected if no data)
                    if (error.code != DatabaseError.PERMISSION_DENIED) {
                        showSnackbar("Error: ${error.message}")
                    } else {
                        Log.d("DashboardActivity", "Permission denied - this is normal if no data exists yet")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error in loadSensors: ${e.message}", e)
            showSnackbar("Error loading sensors: ${e.message}")
        }
    }

    private fun updateSensorsFromSnapshot(snapshot: DataSnapshot) {
        Log.d("DashboardActivity", "updateSensorsFromSnapshot: Updating sensors from snapshot")
        val previousHighRiskSensors = allSensors.filter { getThreatLevel(it) == "High" }.map { it.nodeName }.toSet()
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
            android.util.Log.d("DashboardActivity", "New high-risk sensors detected: $newHighRiskSensors")
        }

        updateSummaryCard()
        updateMapMarkers()

        // Update bottom sheet if sensor is selected
        selectedSensor?.let { sensor ->
            val updatedSensor = allSensors.find { it.nodeName == sensor.nodeName }
            updatedSensor?.let { showSensorDetails(it) }
        }
    }

    private fun updateSummaryCard() {
        totalSensors.text = allSensors.size.toString()

        val highRiskCount = allSensors.count { getThreatLevel(it) == "High" }
        highRiskSensors.text = highRiskCount.toString()

        val activeAlertsCount = allSensors.count {
            getThreatLevel(it) == "High" || getThreatLevel(it) == "Medium"
        }
        activeAlerts.text = activeAlertsCount.toString()
    }

    private fun updateMapMarkers() {
        if (!::mMap.isInitialized) {
            Log.w("DashboardActivity", "updateMapMarkers: Map not ready yet")
            return
        }

        try {
            mMap.clear()
            sensorMarkers.clear()

            val filteredSensors = if (currentFilter == "All") {
                allSensors
            } else {
                allSensors.filter { getThreatLevel(it) == currentFilter }
            }

            for (sensor in filteredSensors) {
                val lat = sensor.latitude
                val lng = sensor.longitude
                if (lat != null && lng != null) {
                    val position = LatLng(lat, lng)
                    val marker = mMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(sensor.nodeName ?: "Sensor")
                            .icon(getMarkerIcon(sensor))
                    )
                    marker?.tag = sensor
                    sensor.nodeName?.let { name ->
                        marker?.let { sensorMarkers[name] = it }
                    }
                }
            }

            // Move camera to show all markers
            if (filteredSensors.isNotEmpty()) {
                val firstSensor = filteredSensors.first()
                val firstLat = firstSensor.latitude
                val firstLng = firstSensor.longitude
                if (firstLat != null && firstLng != null) {
                    val bounds = LatLngBounds.Builder()
                    filteredSensors.forEach { sensor ->
                        val lat = sensor.latitude
                        val lng = sensor.longitude
                        if (lat != null && lng != null) {
                            bounds.include(LatLng(lat, lng))
                        }
                    }
                    try {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
                    } catch (e: Exception) {
                        // If bounds too small or only one point, just zoom to the first sensor
                        mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(firstLat, firstLng),
                                12f
                            )
                        )
                    }
                }
            }

            // Marker click listener
            mMap.setOnMarkerClickListener { marker ->
                val sensor = marker.tag as? SensorData
                sensor?.let { showSensorDetails(it) }
                true
            }
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error updating map markers: ${e.message}", e)
            showSnackbar("Error updating map markers")
        }
    }

    private fun showSensorDetails(sensor: SensorData) {
        selectedSensor = sensor
        try {
            sensorName.text = sensor.nodeName ?: "N/A"

            val tiltValue = sensor.tilt ?: 0.0
            val soilValue = sensor.soilMoisture ?: 0.0
            val rainValue = sensor.rain ?: 0.0

            sensorTilt.text = "${String.format("%.2f", tiltValue)}°"
            sensorSoil.text = "${String.format("%.1f", soilValue)}%"
            sensorRain.text = "${String.format("%.1f", rainValue)} mm"
            sensorThreat.text = getThreatLevel(sensor)

            updateFavoriteButton()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error showing sensor details: ${e.message}", e)
        }
    }

    private fun getMarkerIcon(sensor: SensorData): BitmapDescriptor {
        val key = getThreatLevel(sensor)
        return markerIconCache.getOrPut(key) {
            val colorRes = when (key) {
                "High" -> android.R.color.holo_red_dark
                "Medium" -> android.R.color.holo_orange_dark
                "Low" -> android.R.color.holo_green_dark
                else -> android.R.color.darker_gray
            }
            val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = ContextCompat.getColor(this@DashboardActivity, colorRes)
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle(24f, 24f, 24f, paint)
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    private fun precacheMarkerIcons() {
        // Pre-cache by threat levels to avoid missing resources and speed up marker creation
        listOf("Low", "Medium", "High", "Unknown").forEach { level ->
            if (!markerIconCache.containsKey(level)) {
                val dummy = SensorData(nodeName = level)
                // Use a minimal SensorData with implied threat level via key
                // We'll map to colors by the level below using a when in getMarkerIcon
                markerIconCache[level] = run {
                    val colorRes = when (level) {
                        "High" -> android.R.color.holo_red_dark
                        "Medium" -> android.R.color.holo_orange_dark
                        "Low" -> android.R.color.holo_green_dark
                        else -> android.R.color.darker_gray
                    }
                    val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = ContextCompat.getColor(this@DashboardActivity, colorRes)
                        style = android.graphics.Paint.Style.FILL
                    }
                    canvas.drawCircle(24f, 24f, 24f, paint)
                    BitmapDescriptorFactory.fromBitmap(bitmap)
                }
            }
        }
    }

    private fun getThreatLevel(sensor: SensorData): String {
        val tilt = sensor.tilt ?: 0.0
        val soil = sensor.soilMoisture ?: 0.0
        return when {
            tilt > 15 || soil > 70 -> "High"
            tilt > 10 || soil > 50 -> "Medium"
            else -> "Low"
        }
    }

    private fun applyFilter() {
        Log.d("DashboardActivity", "applyFilter: Applying filter - $currentFilter")
        updateMapMarkers()
    }

    private fun setupRealtimeListener() {
        Log.d("DashboardActivity", "setupRealtimeListener: Setting up realtime listener.")
        valueEventListener = database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("DashboardActivity", "Realtime data received.")
                updateSensorsFromSnapshot(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DashboardActivity", "Realtime listener cancelled: ${error.message}")
            }
        })
    }
}
