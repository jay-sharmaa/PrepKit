package com.example.prepkit

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BluetoothHeader() {
    Column{
        Text("Connect With Bluetooth", style = MaterialTheme.typography.headlineSmall)
        HorizontalDivider(thickness = 1.dp, color = Color.Black)
    }
}

@Composable
fun ConnectionStatusCard(isConnected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color.Green.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)
        )
    ) {
        Text(
            text = if (isConnected) "Connected" else "Not Connected",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ScanControls(
    isScanning: Boolean,
    isConnected: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = onStartScan, enabled = !isScanning && !isConnected) {
            Text("Start Scan")
        }
        Button(onClick = onStopScan, enabled = isScanning) {
            Text("Stop Scan")
        }
        Button(onClick = onDisconnect, enabled = isConnected) {
            Text("Disconnect")
        }
    }
}

@Composable
fun ChatSection(
    messages: List<String>,
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Chat", style = MaterialTheme.typography.headlineSmall)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                messages.reversed().forEach { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(vertical = 2.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    singleLine = true
                )
                Button(onClick = onSendClick, enabled = messageText.isNotBlank()) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun DeviceListSection(
    devices: List<BluetoothDevice>,
    isScanning: Boolean,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
    ) {
        items(devices) { device ->
            Text(
                text = device.name ?: device.address,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeviceClick(device) }
                    .padding(16.dp)
            )
            Divider()
        }
    }
}


@Composable
fun BluetoothScreen() {
    val context = LocalContext.current
    val discoveredDevices = remember { mutableStateListOf<BluetoothDevice>() }
    var isScanning by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }
    var connectedSocket by remember { mutableStateOf<BluetoothSocket?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var receivedMessages = remember { mutableStateListOf<String>() }

    val scanner = remember {
        BluetoothDeviceScanner(context) { device ->
            if (!discoveredDevices.any { it.address == device.address }) {
                discoveredDevices.add(device)
            }
        }
    }

    val bluetoothConnection = remember { BluetoothConnection(context) }
    val chatService = remember { BluetoothChatService(context) }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            val socket = chatService.listenForConnection()
            if (socket != null) {
                connectedSocket = socket
                isConnected = true
                while (socket.isConnected) {
                    val message = chatService.receiveMessage(socket)
                    if (message != null) {
                        receivedMessages.add("Received: $message")
                    }
                }
            }
        }
    }

    if (!hasPermissions) {
        BluetoothPermissionHandler {
            hasPermissions = true
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp)
        ) {
            BluetoothHeader()
            ConnectionStatusCard(isConnected)

            ScanControls(
                isScanning = isScanning,
                isConnected = isConnected,
                onStartScan = {
                    discoveredDevices.clear()
                    isScanning = true
                    scanner.startDiscovery()
                },
                onStopScan = {
                    isScanning = false
                    scanner.stopDiscovery()
                },
                onDisconnect = {
                    connectedSocket?.close()
                    connectedSocket = null
                    isConnected = false
                    receivedMessages.clear()
                }
            )

            if (isConnected) {
                Box(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    ChatSection(
                        messages = receivedMessages,
                        messageText = messageText,
                        onMessageTextChange = { messageText = it },
                        onSendClick = {
                            connectedSocket?.let {
                                val success = chatService.sendMessage(it, messageText)
                                if (success) {
                                    receivedMessages.add("Sent: $messageText")
                                    messageText = ""
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to send message",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
            else {
                Box(
                    modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())
                ) {
                    DeviceListSection(
                        devices = discoveredDevices,
                        isScanning = isScanning,
                        onDeviceClick = { device ->
                            scanner.stopDiscovery()
                            isScanning = false

                            CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    val socket = bluetoothConnection.connectToDevice(device)
                                    if (socket?.isConnected == true) {
                                        connectedSocket = socket
                                        isConnected = true
                                        Toast.makeText(
                                            context,
                                            "Connected to ${device.name ?: "Unknown"}",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        launch(Dispatchers.IO) {
                                            while (socket.isConnected) {
                                                val msg = chatService.receiveMessage(socket)
                                                if (msg != null) {
                                                    withContext(Dispatchers.Main) {
                                                        receivedMessages.add("Received: $msg")
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Connection failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Connection error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                scanner.stopDiscovery()
                connectedSocket?.close()
            }
        }
    }
}
