package com.example.mad_cw.ui.sensor

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_cw.R
import com.example.mad_cw.data.model.SensorData
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class SensorDetailActivity : AppCompatActivity() {

    private lateinit var sensorName: TextView
    private lateinit var sensorTilt: TextView
    private lateinit var sensorSoil: TextView
    private lateinit var sensorRain: TextView
    private lateinit var sensorThreat: TextView
    private lateinit var alertMessage: TextView
    private lateinit var alertCard: MaterialCardView
    
    private lateinit var tiltChart: LineChart
    private lateinit var soilChart: LineChart
    private lateinit var rainChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_detail)
        
        @Suppress("DEPRECATION")
        val sensorData = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("sensorData", SensorData::class.java)
        } else {
            intent.getSerializableExtra("sensorData") as? SensorData
        }
        
        if (sensorData == null) {
            finish()
            return
        }
        
        initViews()
        displaySensorData(sensorData)
        setupCharts(sensorData)
    }
    
    private fun initViews() {
        sensorName = findViewById(R.id.sensorName)
        sensorTilt = findViewById(R.id.sensorTilt)
        sensorSoil = findViewById(R.id.sensorSoil)
        sensorRain = findViewById(R.id.sensorRain)
        sensorThreat = findViewById(R.id.sensorThreat)
        alertMessage = findViewById(R.id.alertMessage)
        alertCard = findViewById(R.id.alertCard)
        
        tiltChart = findViewById(R.id.tiltChart)
        soilChart = findViewById(R.id.soilChart)
        rainChart = findViewById(R.id.rainChart)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sensor Details"
    }
    
    private fun displaySensorData(sensor: SensorData) {
                sensorName.text = sensor.nodeName ?: "Unknown Sensor"
        sensorTilt.text = "Tilt: ${sensor.tilt ?: 0.0}°"
        sensorSoil.text = "Soil Moisture: ${sensor.soilMoisture ?: 0.0}%"
        sensorRain.text = "Rainfall: ${sensor.rain ?: 0.0} mm"
        
        val threatLevel = getThreatLevel(sensor)
        sensorThreat.text = "Threat Level: $threatLevel"
        
        // Check for threshold alerts
        val alerts = mutableListOf<String>()
        if ((sensor.tilt ?: 0.0) > 15) {
            alerts.add("⚠️ Tilt angle exceeds safe threshold (15°)")
        }
        if ((sensor.soilMoisture ?: 0.0) > 70) {
            alerts.add("⚠️ Soil moisture exceeds critical level (70%)")
        }
        if ((sensor.rain ?: 0.0) > 50) {
            alerts.add("⚠️ Heavy rainfall detected (>50mm)")
        }
        
        if (alerts.isNotEmpty()) {
            alertMessage.text = alerts.joinToString("\n")
            alertCard.visibility = android.view.View.VISIBLE
        } else {
            alertCard.visibility = android.view.View.GONE
        }
    }
    
    private fun getThreatLevel(sensor: SensorData): String {
        return when {
            (sensor.tilt ?: 0.0) > 15 || (sensor.soilMoisture ?: 0.0) > 70 -> "High"
            (sensor.tilt ?: 0.0) > 10 || (sensor.soilMoisture ?: 0.0) > 50 -> "Medium"
            else -> "Low"
        }
    }
    
    private fun setupCharts(sensor: SensorData) {
        // Generate sample historical data (in real app, fetch from Firebase)
        val entries = generateHistoricalData()
        
        setupLineChart(tiltChart, entries, "Tilt (°)", sensor.tilt?.toFloat() ?: 0f)
        setupLineChart(soilChart, entries, "Soil Moisture (%)", sensor.soilMoisture?.toFloat() ?: 0f)
        setupLineChart(rainChart, entries, "Rainfall (mm)", sensor.rain?.toFloat() ?: 0f)
    }
    
    private fun setupLineChart(chart: LineChart, entries: List<Entry>, label: String, currentValue: Float) {
        val dataSet = LineDataSet(entries, label)
        dataSet.color = getColor(R.color.primaryColor)
        dataSet.setCircleColor(getColor(R.color.primaryColor))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        
        val lineData = LineData(dataSet)
        chart.data = lineData
        
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setDragEnabled(true)
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR, -(23 - value.toInt()))
                return SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
            }
        }
        
        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.axisMinimum = 0f
        
        chart.axisRight.isEnabled = false
        chart.animateX(1000)
        chart.invalidate()
    }
    
    private fun generateHistoricalData(): List<Entry> {
        // Generate 24 hours of sample data
        val entries = mutableListOf<Entry>()
        val random = Random()
        for (i in 0..23) {
            entries.add(Entry(i.toFloat(), random.nextFloat() * 20 + 10))
        }
        return entries
    }
    
    override fun onSupportNavigateUp(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            finish()
        } else {
            @Suppress("DEPRECATION")
            onBackPressed()
        }
        return true
    }
}

