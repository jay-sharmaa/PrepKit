package com.example.prepkit.MainNavigationScreen

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.prepkit.BluetoothDeviceListScreen
import com.example.prepkit.BluetoothDeviceScanner
import com.example.prepkit.BluetoothScreen
import com.example.prepkit.WifiViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ContactScreen(
    context: Context,
    viewModel: WifiViewModel,
    manager: WifiP2pManager,
    channel: WifiP2pManager.Channel,
) {
    var currValue = false
    var blinkJob by remember { mutableStateOf<Job?>(null) }
    var isBlinking by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }
    val discoveredDevices = remember { mutableStateListOf<BluetoothDevice>() }
    val scanner = remember {
        BluetoothDeviceScanner(context) { device ->
            if (discoveredDevices.none { it.address == device.address }) {
                discoveredDevices.add(device)
            }
        }
    }

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val wifiDevicesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        hasPermissions = fineLocationGranted && wifiDevicesGranted
        delay(1000)
        val packageManager = context.packageManager
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        Log.d("WiFiDirect", "Wi-Fi Enabled: ${wifiManager.isWifiEnabled}")
        Log.d(
            "WiFiDirect",
            "Has FEATURE_WIFI_DIRECT: ${packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)}"
        )
        Log.d(
            "WiFiDirect", "Has ACCESS_FINE_LOCATION: ${
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            }"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(
                "WiFiDirect", "Has NEARBY_WIFI_DEVICES: ${
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    ) == PackageManager.PERMISSION_GRANTED
                }"
            )
        }

        if (hasPermissions) {
            val p2pSupported =
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)
            if (!p2pSupported) {
                Log.e("WiFiDirect", "Wi-Fi Direct not supported on this device")
                return@LaunchedEffect
            }

            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {

                }

                override fun onFailure(reason: Int) {
                    Log.e("WiFiDirect", "Discovery failed: $reason")
                }
            })
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
        Text("Connect with wifi direct")
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        PeerListScreen(viewModel = viewModel) { device ->
            viewModel.connectToPeer(device, manager, channel)
        }
        HorizontalDivider(modifier = Modifier, thickness =  1.dp, color = Color.Black)
        Text("Connect with Bluetooth")
        BluetoothScreen()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (isBlinking) {
                        blinkJob?.cancel()
                        toggleFlashlight(context, false)
                        isBlinking = false
                    } else {
                        blinkJob = CoroutineScope(Dispatchers.Default).launch {
                            repeat(100) {
                                currValue = !currValue
                                toggleFlashlight(context, currValue)
                                delay(500L)
                            }
                        }
                        isBlinking = true
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(if (isBlinking) "Stop" else "Start Flashing")
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

@Composable
fun PeerListScreen(viewModel: WifiViewModel, onConnect: (WifiP2pDevice) -> Unit) {
    val peers = viewModel.peerList

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        items(peers) { peer ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConnect(peer) }
                    .padding(8.dp)
            ) {
                Text("Name: ${peer.deviceName}")
                Text("Address: ${peer.deviceAddress}")
            }
        }
    }
}
