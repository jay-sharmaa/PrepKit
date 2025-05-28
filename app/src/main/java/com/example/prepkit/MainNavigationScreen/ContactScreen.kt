package com.example.prepkit.MainNavigationScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ContactScreen(){
    var currValue = false
    var blinkJob by remember { mutableStateOf<Job?>(null) }
    var isBlinking by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mylist = listOf("jay", "sharma", "temp")
    Column(
        modifier = Modifier.fillMaxSize()
    ){
        LazyColumn(
            modifier = Modifier.weight(2f)
                .fillMaxWidth()
        ) {
            items(mylist) { it->
                Text(it)
            }
        }
        Box(
            modifier = Modifier.weight(1f)
                .fillMaxWidth()
        ){
            Button(
                onClick = {
                    if(isBlinking) {
                        blinkJob?.cancel()
                        toggleFlashlight(context = context, false)
                        isBlinking = false
                    }
                    else{
                        blinkJob = CoroutineScope(Dispatchers.Default).launch {
                            repeat(100) {
                                currValue = !currValue
                                toggleFlashlight(context = context, currValue)
                                delay(500L)
                            }
                        }
                        isBlinking = true
                    }
                },
            ) {
                Text("On")
            }
        }
    }
}



fun toggleFlashlight(context: Context, turnOn: Boolean) {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
        val cameraId = cameraManager.cameraIdList.firstOrNull {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val isBackFacing = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
            hasFlash && isBackFacing
        }

        cameraId?.let {
            cameraManager.setTorchMode(it, turnOn)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
