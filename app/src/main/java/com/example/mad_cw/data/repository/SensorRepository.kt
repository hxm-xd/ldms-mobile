package com.example.mad_cw.data.repository

import android.util.Log
import com.example.mad_cw.data.model.SensorData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SensorRepository {
    private val database = FirebaseDatabase.getInstance().getReference("nodes")

    fun getSensorNodes(onResult: (List<SensorData>) -> Unit) {
        database.get().addOnSuccessListener { snapshot ->
            val sensorList = mutableListOf<SensorData>()
            for (nodeSnapshot in snapshot.children) {
                val data = nodeSnapshot.getValue(SensorData::class.java)
                if (data != null) sensorList.add(data)
            }
            onResult(sensorList)
        }
    }

    fun observeSensorChanges(onDataChange: (List<SensorData>) -> Unit) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sensorList = mutableListOf<SensorData>()
                for (nodeSnapshot in snapshot.children) {
                    val data = nodeSnapshot.getValue(SensorData::class.java)
                    if (data != null) sensorList.add(data)
                }
                onDataChange(sensorList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SensorRepository", "Firebase error ${error.message}")
            }
        })
    }
}