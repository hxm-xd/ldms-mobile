package com.example.mad_cw.ui.sensor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.example.mad_cw.data.model.SensorData
import com.example.mad_cw.ui.compose.SensorDetailScreen
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SensorDetailActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private var valueEventListener: ValueEventListener? = null
    // hold state so realtime updates can modify it from other methods
    private val state = mutableStateOf<SensorData?>(null)
    private var nodeName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        val sensorData = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("sensorData", SensorData::class.java)
        } else {
            intent.getSerializableExtra("sensorData") as? SensorData
        }
        if (sensorData == null) {
            finish(); return
        }

        database = FirebaseDatabase.getInstance().reference
        state.value = sensorData
        nodeName = sensorData.nodeName

        setContent {
            SensorDetailScreen(initial = sensorData, updates = state) {
                finish()
            }
        }

        // start realtime updates
        startRealtimeUpdates(nodeName)
    }

    private fun startRealtimeUpdates(node: String?) {
        if (node == null) return
        valueEventListener?.let { database.child(node).removeEventListener(it) }
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latest = snapshot.getValue(SensorData::class.java) ?: return
                lifecycleScope.launch(Dispatchers.Main) {
                    // update the class-level state
                    this@SensorDetailActivity.state.value = latest
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        database.child(node).addValueEventListener(valueEventListener as ValueEventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        // remove listener using the stored nodeName
        nodeName?.let { node ->
            valueEventListener?.let { database.child(node).removeEventListener(it) }
        }
    }
}

