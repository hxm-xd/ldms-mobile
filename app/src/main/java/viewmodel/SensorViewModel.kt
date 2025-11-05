package viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mad_cw.data.model.SensorData
import com.example.mad_cw.data.repository.SensorRepository

class SensorViewModel : ViewModel(){

    private val repository = SensorRepository()

    private val _sensorData = MutableLiveData<List<SensorData>>()
    val sensorData: LiveData<List<SensorData>> = _sensorData

    fun loadSensorData(){
        repository.getSensorNodes { data ->
            _sensorData.value = data
        }
    }

    fun observeSensorData(){
        repository.observeSensorChanges { data ->
            _sensorData.value = data
        }
    }
}