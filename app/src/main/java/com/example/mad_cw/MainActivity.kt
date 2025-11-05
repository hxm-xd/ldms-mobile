package com.example.mad_cw

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import android.util.Log

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            val app = FirebaseApp.initializeApp(this)
            if (app == null) {
                Log.e("FirebaseStatus", "❌ Firebase not initialized! Check google-services.json or plugin.")
            } else {
                Log.d("FirebaseStatus", "✅ Firebase initialized successfully: ${app.name}")
                // Optional: write test data to database
                val db = FirebaseDatabase.getInstance().reference
                db.child("test_connection").setValue("Firebase is working!")
                    .addOnSuccessListener {
                        Log.d("FirebaseStatus", "✅ Data written successfully.")
                    }
                    .addOnFailureListener {
                        Log.e("FirebaseStatus", "❌ Failed to write data: ${it.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e("FirebaseStatus", "❌ Exception during Firebase init: ${e.message}")
        }
    }
}