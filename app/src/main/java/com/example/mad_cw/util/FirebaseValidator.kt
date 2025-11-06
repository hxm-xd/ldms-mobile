package com.example.mad_cw.util

import android.util.Log
import com.example.mad_cw.data.model.SensorData
import com.google.firebase.database.*

/**
 * Utility class to validate Firebase connection and data structure
 */
object FirebaseValidator {
    
    private const val TAG = "FirebaseValidator"
    
    /**
     * Test Firebase connection and validate data structure
     */
    fun validateConnection(callback: (Boolean, String) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference
        
        Log.d(TAG, "Starting Firebase validation...")
        
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val results = StringBuilder()
                results.append("✅ Firebase Connection Successful!\n\n")
                
                // Check for node_* children
                val nodeChildren = snapshot.children.filter { it.key?.startsWith("node_") == true }
                results.append("📊 Found ${nodeChildren.count()} sensor nodes:\n")
                
                var allValid = true
                
                for (child in nodeChildren) {
                    val nodeKey = child.key ?: continue
                    results.append("\n[$nodeKey]:\n")
                    
                    // Try to parse as SensorData
                    val sensorData = child.getValue(SensorData::class.java)
                    
                    if (sensorData == null) {
                        results.append("  ❌ Failed to parse as SensorData\n")
                        allValid = false
                        continue
                    }
                    
                    // Validate required fields
                    val validations = mutableListOf<Pair<String, Boolean>>()
                    validations.add("name" to (sensorData.nodeName != null))
                    validations.add("latitude" to (sensorData.latitude != null))
                    validations.add("longitude" to (sensorData.longitude != null))
                    validations.add("tilt" to (sensorData.tilt != null))
                    validations.add("rain" to (sensorData.rain != null))
                    validations.add("soilMoisture" to (sensorData.soilMoisture != null))
                    
                    // Display validation results
                    for ((field, isValid) in validations) {
                        val icon = if (isValid) "✅" else "❌"
                        val value = when (field) {
                            "name" -> sensorData.nodeName
                            "latitude" -> sensorData.latitude
                            "longitude" -> sensorData.longitude
                            "tilt" -> sensorData.tilt
                            "rain" -> sensorData.rain
                            "soilMoisture" -> sensorData.soilMoisture
                            else -> null
                        }
                        results.append("  $icon $field: $value\n")
                        
                        if (!isValid) allValid = false
                    }
                    
                    // Show raw Firebase data for debugging
                    results.append("  📋 Raw data snapshot:\n")
                    child.children.forEach { field ->
                        results.append("    - ${field.key}: ${field.value}\n")
                    }
                }
                
                // Check for users structure
                val usersSnapshot = snapshot.child("users")
                if (usersSnapshot.exists()) {
                    results.append("\n✅ Users structure exists\n")
                    results.append("  Found ${usersSnapshot.childrenCount} users\n")
                } else {
                    results.append("\n⚠️ Users structure not found (optional)\n")
                }
                
                // Final verdict
                results.append("\n" + "=".repeat(40) + "\n")
                if (allValid && nodeChildren.count() > 0) {
                    results.append("✅ VALIDATION PASSED\n")
                    results.append("All sensor nodes have required fields!\n")
                } else {
                    results.append("❌ VALIDATION FAILED\n")
                    results.append("Some required fields are missing.\n")
                    results.append("Please update your Firebase database.\n")
                }
                
                Log.d(TAG, results.toString())
                callback(allValid && nodeChildren.count() > 0, results.toString())
            }
            
            override fun onCancelled(error: DatabaseError) {
                val errorMsg = "❌ Firebase Error: ${error.message}\n" +
                        "Code: ${error.code}\n" +
                        "Details: ${error.details}"
                Log.e(TAG, errorMsg)
                callback(false, errorMsg)
            }
        })
    }
    
    /**
     * Validate a specific sensor node
     */
    fun validateNode(nodeKey: String, callback: (Boolean, String) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference.child(nodeKey)
        
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val results = StringBuilder()
                results.append("Validating node: $nodeKey\n\n")
                
                if (!snapshot.exists()) {
                    val msg = "❌ Node does not exist in Firebase"
                    results.append(msg)
                    callback(false, results.toString())
                    return
                }
                
                val sensorData = snapshot.getValue(SensorData::class.java)
                
                if (sensorData == null) {
                    val msg = "❌ Failed to parse node as SensorData"
                    results.append(msg)
                    callback(false, results.toString())
                    return
                }
                
                results.append("✅ Successfully parsed as SensorData\n\n")
                results.append("Field Values:\n")
                results.append("  name: ${sensorData.nodeName}\n")
                results.append("  latitude: ${sensorData.latitude}\n")
                results.append("  longitude: ${sensorData.longitude}\n")
                results.append("  tilt: ${sensorData.tilt}\n")
                results.append("  rain: ${sensorData.rain}\n")
                results.append("  soilMoisture: ${sensorData.soilMoisture}\n")
                results.append("  light: ${sensorData.light}\n")
                results.append("  accelX/Y/Z: ${sensorData.accelX}, ${sensorData.accelY}, ${sensorData.accelZ}\n")
                
                val allRequiredPresent = listOf(
                    sensorData.nodeName,
                    sensorData.latitude,
                    sensorData.longitude,
                    sensorData.tilt,
                    sensorData.rain,
                    sensorData.soilMoisture
                ).all { it != null }
                
                if (allRequiredPresent) {
                    results.append("\n✅ All required fields present!")
                    callback(true, results.toString())
                } else {
                    results.append("\n❌ Some required fields are missing!")
                    callback(false, results.toString())
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                callback(false, "❌ Error: ${error.message}")
            }
        })
    }
}
