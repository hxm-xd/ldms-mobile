package com.example.mad_cw.ui.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import com.example.mad_cw.R
import com.example.mad_cw.ui.compose.BottomSheetSensorContent

class BottomSheetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BottomSheetSensorContent(sensor = null)
        }
    }
}