package com.example.prepkit

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val _gpsPoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val gpsPoints: StateFlow<List<Pair<Double, Double>>> = _gpsPoints

    private val _bearing = MutableStateFlow(0f)
    val bearing: StateFlow<Float> = _bearing

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val sensorManager =
        application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let {
                _gpsPoints.update { list -> list + (it.latitude to it.longitude) }
            }
        }
    }

    init {
        startLocationUpdates()
        startCompassUpdates()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val request = LocationRequest.create().apply {
            interval = 1000L
            fastestInterval = 500L
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper())

    }

    private fun startCompassUpdates() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val sensorEventListener = object : SensorEventListener {
            @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> gravity.copyFrom(event.values)
                    Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic.copyFrom(event.values)
                }

                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    _bearing.value = (azimuth + 360) % 360
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorEventListener, accel, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(sensorEventListener, magnet, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onCleared() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onCleared()
    }

    private fun FloatArray.copyFrom(source: FloatArray) {
        for (i in indices) this[i] = source[i]
    }
}
