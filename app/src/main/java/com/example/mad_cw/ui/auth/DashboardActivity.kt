package com.example.mad_cw.ui.auth

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.example.mad_cw.R
import com.example.mad_cw.data.model.SensorData
import com.example.mad_cw.data.repository.AuthRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.android.material.bottomsheet.BottomSheetBehavior

class DashboardActivity : AppCompatActivity(), OnMapReadyCallback {

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
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Check authentication
        if (authRepository.getCurrentUser() == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Firebase references
        database = FirebaseDatabase.getInstance().getReference("nodes")
        usersDatabase = FirebaseDatabase.getInstance().getReference("users")
        favoritesDatabase = FirebaseDatabase.getInstance().getReference("users/${currentUserId}/favorites")

        initViews()
        setupBottomSheet()
        setupFilters()
        loadUserPreferences()
        loadFavoriteSensors()

        // Map setup
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        
        setupMenu()
    }
    
    private fun initViews() {
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
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
        
        btnFavorite.setOnClickListener {
            selectedSensor?.let { sensor ->
                toggleFavorite(sensor.nodeName ?: "")
            }
        }
    }
    
    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Handle state changes
            }
            
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Handle slide animations
            }
        })
    }
    
    private fun setupFilters() {
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
    }
    
    private fun setupMenu() {
        btnMenu.setOnClickListener {
            val popup = androidx.appcompat.widget.PopupMenu(this, btnMenu)
            popup.menuInflater.inflate(R.menu.dashboard_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_settings -> {
                        startActivity(Intent(this, SettingsActivity::class.java))
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
    }
    
    private fun loadUserPreferences() {
        currentUserId?.let { userId ->
            usersDatabase.child(userId).child("assignedSensors").addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        userAssignedSensors.clear()
                        for (child in snapshot.children) {
                            child.value?.toString()?.let { sensorName ->
                                userAssignedSensors.add(sensorName)
                            }
                        }
                        // If no assigned sensors, load all sensors
                        if (userAssignedSensors.isEmpty()) {
                            loadSensors()
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        // On error, load all sensors
                        loadSensors()
                    }
                }
            )
        } ?: run {
            // If no user ID, load all sensors
            loadSensors()
        }
    }
    
    private fun loadFavoriteSensors() {
        favoritesDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                favoriteSensors.clear()
                for (child in snapshot.children) {
                    child.key?.let { favoriteSensors.add(it) }
                }
                updateFavoriteButton()
            }
            
            override fun onCancelled(error: DatabaseError) {}
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

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
        setupRealtimeListener()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
        valueEventListener?.let { database.removeEventListener(it) }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isZoomGesturesEnabled = true
        loadSensors()
    }
    
    private fun setupRealtimeListener() {
        valueEventListener?.let { database.removeEventListener(it) }
        
        valueEventListener = database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateSensorsFromSnapshot(snapshot)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DashboardActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadSensors() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateSensorsFromSnapshot(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DashboardActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun updateSensorsFromSnapshot(snapshot: DataSnapshot) {
        val previousHighRiskSensors = allSensors.filter { getThreatLevel(it) == "High" }.map { it.nodeName }.toSet()
        
        allSensors.clear()
        for (child in snapshot.children) {
            val node = child.getValue(SensorData::class.java)
            node?.let {
                // Filter by user assigned sensors if available
                if (userAssignedSensors.isEmpty() || userAssignedSensors.contains(it.nodeName)) {
                    allSensors.add(it)
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
        mMap.clear()
        sensorMarkers.clear()
        
        val filteredSensors = if (currentFilter == "All") {
            allSensors
        } else {
            allSensors.filter { getThreatLevel(it) == currentFilter }
        }
        
        for (sensor in filteredSensors) {
            if (sensor.latitude != null && sensor.longitude != null) {
                val position = LatLng(sensor.latitude, sensor.longitude)
                val marker = mMap.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(sensor.nodeName ?: "Sensor")
                        .icon(BitmapDescriptorFactory.fromBitmap(getMarkerIcon(sensor)))
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
            if (firstSensor.latitude != null && firstSensor.longitude != null) {
                val bounds = LatLngBounds.Builder()
                filteredSensors.forEach { sensor ->
                    if (sensor.latitude != null && sensor.longitude != null) {
                        bounds.include(LatLng(sensor.latitude, sensor.longitude))
                    }
                }
                try {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
                } catch (e: Exception) {
                    // If bounds is too small, just zoom to first sensor
                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(firstSensor.latitude, firstSensor.longitude),
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
    }
    
    private fun applyFilter() {
        updateMapMarkers()
    }

    private fun showSensorDetails(node: SensorData) {
        selectedSensor = node
        sensorName.text = node.nodeName ?: "Unknown"
        sensorTilt.text = "${node.tilt ?: 0.0}°"
        sensorSoil.text = "${node.soilMoisture ?: 0.0}%"
        sensorRain.text = "${node.rain ?: 0.0} mm"
        
        val threatLevel = getThreatLevel(node)
        sensorThreat.text = threatLevel
        
        // Update threat color
        val threatColor = when (threatLevel) {
            "High" -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
            "Medium" -> ContextCompat.getColor(this, android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(this, R.color.primaryColor)
        }
        sensorThreat.setTextColor(threatColor)
        
        updateFavoriteButton()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun getThreatLevel(node: SensorData): String {
        return when {
            (node.tilt ?: 0.0) > 15 || (node.soilMoisture ?: 0.0) > 70 -> "High"
            (node.tilt ?: 0.0) > 10 || (node.soilMoisture ?: 0.0) > 50 -> "Medium"
            else -> "Low"
        }
    }

    private fun getMarkerIcon(node: SensorData): Bitmap {
        val drawableRes = when (getThreatLevel(node)) {
            "High" -> R.drawable.marker_high
            "Medium" -> R.drawable.marker_medium
            else -> R.drawable.marker_low
        }
        
        val drawable = ContextCompat.getDrawable(this, drawableRes) ?: 
            ContextCompat.getDrawable(this, R.drawable.marker_low)!!
        
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
