package com.example.prepkit.MainNavigationScreen

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.prepkit.MapLibreCompose
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

@Composable
fun HomeScreen(azimuth: State<Float>, latitude: State<Float>, longitude: State<Float>) {
    var selectedLocation by remember {
        mutableStateOf(
            LatLng(
                latitude.value.toDouble(),
                longitude.value.toDouble()
            )
        )
    }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    var lastCameraLocation by remember { mutableStateOf(selectedLocation) }

    MapLibreCompose(
        modifier = Modifier.fillMaxSize(),
        initialPosition = selectedLocation,
        initialZoom = 12.0,
        styleUrl = "https://demotiles.maplibre.org/style.json",
        onMapReady = { map ->
            mapInstance = map
            map.addOnMapClickListener { latLng ->
                selectedLocation = latLng
                lastCameraLocation = latLng
                true
            }
        }
    )

    LaunchedEffect(latitude.value, longitude.value) {
        val newLocation = LatLng(latitude.value.toDouble(), longitude.value.toDouble())

        val distance = calculateDistance(lastCameraLocation, newLocation)

        if (distance > 10.0) {
            selectedLocation = newLocation
            lastCameraLocation = newLocation

            mapInstance?.let { map ->
                try {
                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(newLocation, 12.0)
                    map.animateCamera(cameraUpdate, 1000)
                } catch (e: Exception) {
                }
            }
        }
    }
}

fun calculateDistance(point1: LatLng, point2: LatLng): Double {
    val earthRadius = 6371000.0

    val lat1Rad = Math.toRadians(point1.latitude)
    val lat2Rad = Math.toRadians(point2.latitude)
    val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
    val deltaLngRad = Math.toRadians(point2.longitude - point1.longitude)

    val a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
            Math.cos(lat1Rad) * Math.cos(lat2Rad) *
            Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2)

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

    return earthRadius * c
}