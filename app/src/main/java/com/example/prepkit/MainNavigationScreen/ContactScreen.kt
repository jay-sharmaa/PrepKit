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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.prepkit.BluetoothScreen
import com.example.prepkit.R
import com.example.prepkit.WifiViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ContactScreen(
    context: Context,
    viewModel: WifiViewModel,
    manager: WifiP2pManager,
    channel: WifiP2pManager.Channel,
) {
    var blinkJob by remember { mutableStateOf<Job?>(null) }
    var isBlinking by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        Color.Black,
                        Color.Black
                    )
                )
            ),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.wifi),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "WiFi Direct Connection",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Discover and connect to nearby devices",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        )
                    )
                }

                PeerListScreen(
                    viewModel = viewModel,
                    isVisible = isVisible,
                    onSearch = {
                        isVisible = !isVisible
                    }
                ) { device ->
                    if (isVisible) {
                        viewModel.connectToPeer(device, manager, channel)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.bluetooth),
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "Bluetooth Devices",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E3A8A)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 250.dp, max = 500.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        BluetoothScreen()
                    }

                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.flashlight),
                            contentDescription = null,
                            tint = if (isBlinking) Color(0xFFEF4444) else Color(0xFF3B82F6),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "SOS Morse Code",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E3A8A)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Send emergency SOS signal using flashlight",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (isBlinking) {
                                blinkJob?.cancel()
                                toggleFlashlight(context, false)
                                isBlinking = false
                            } else {
                                blinkJob = CoroutineScope(Dispatchers.Default).launch {
                                    val dotDuration = 250L
                                    val dashDuration = dotDuration * 3
                                    val gapBetweenElements = dotDuration
                                    val gapBetweenLetters = dotDuration * 3
                                    val gapBetweenWords = dotDuration * 7

                                    suspend fun flashSignal(duration: Long) {
                                        toggleFlashlight(context, true)
                                        delay(duration)
                                        toggleFlashlight(context, false)
                                        delay(gapBetweenElements)
                                    }

                                    repeat(5) {
                                        flashSignal(dotDuration)
                                        flashSignal(dotDuration)
                                        flashSignal(dotDuration)
                                        delay(gapBetweenLetters - gapBetweenElements)

                                        flashSignal(dashDuration)
                                        flashSignal(dashDuration)
                                        flashSignal(dashDuration)
                                        delay(gapBetweenLetters - gapBetweenElements)

                                        flashSignal(dotDuration)
                                        flashSignal(dotDuration)
                                        flashSignal(dotDuration)

                                        if (it < 4) {
                                            delay(gapBetweenWords)
                                        }
                                    }

                                    toggleFlashlight(context, false)
                                    isBlinking = false
                                }
                                isBlinking = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBlinking) Color(0xFFEF4444) else Color(0xFF3B82F6),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = (if (isBlinking) painterResource(id = R.drawable.stopbutton) else painterResource(
                                    id = R.drawable.flashlight
                                )),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                if (isBlinking) "Stop SOS Signal" else "Start SOS Signal",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }

        item {

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
fun PeerListScreen(
    viewModel: WifiViewModel,
    isVisible: Boolean,
    onSearch: () -> Unit,
    onConnect: (WifiP2pDevice) -> Unit,
) {
    val peers = viewModel.peerList

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (!isVisible) {
            Button(
                onClick = onSearch,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("Search Peers", textAlign = TextAlign.Center)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Button(
                    onClick = onSearch,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Hide Peers", textAlign = TextAlign.Center)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(top = 8.dp)
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
        }
    }
}
