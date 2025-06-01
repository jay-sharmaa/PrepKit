package com.example.prepkit

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    // Listen for incoming connections
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            val socket = chatService.listenForConnection()
            if (socket != null) {
                connectedSocket = socket
                isConnected = true

                // Start receiving messages
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
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            HorizontalDivider(modifier = Modifier, thickness = 1.dp, color = Color.Black)
            Text("Connect with Bluetooth", style = MaterialTheme.typography.headlineSmall)

            // Connection Status
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

            // Scan Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        discoveredDevices.clear()
                        isScanning = true
                        scanner.startDiscovery()
                    },
                    enabled = !isScanning && !isConnected
                ) {
                    Text("Start Scan")
                }

                Button(
                    onClick = {
                        isScanning = false
                        scanner.stopDiscovery()
                    },
                    enabled = isScanning
                ) {
                    Text("Stop Scan")
                }

                Button(
                    onClick = {
                        connectedSocket?.close()
                        connectedSocket = null
                        isConnected = false
                        receivedMessages.clear()
                    },
                    enabled = isConnected
                ) {
                    Text("Disconnect")
                }
            }

            // Chat Interface (only show when connected)
            if (isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Chat", style = MaterialTheme.typography.headlineSmall)

                        // Message History
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 8.dp),
                            reverseLayout = true
                        ) {
                            items(receivedMessages.reversed()) { message ->
                                Text(
                                    text = message,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }

                        // Message Input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type a message...") },
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    if (messageText.isNotBlank() && connectedSocket != null) {
                                        val success = chatService.sendMessage(connectedSocket!!, messageText)
                                        if (success) {
                                            receivedMessages.add("Sent: $messageText")
                                            messageText = ""
                                        } else {
                                            Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = messageText.isNotBlank()
                            ) {
                                Text("Send")
                            }
                        }
                    }
                }
            }

            // Device List (only show when not connected)
            if (!isConnected) {
                BluetoothDeviceListScreen(
                    devices = discoveredDevices,
                    isScanning = isScanning,
                    onDeviceClick = { device ->
                        scanner.stopDiscovery()
                        isScanning = false

                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val socket = bluetoothConnection.connectToDevice(device)
                                if (socket != null && socket.isConnected) {
                                    connectedSocket = socket
                                    isConnected = true

                                    Toast.makeText(
                                        context,
                                        "Connected to ${device.name ?: "Unknown Device"}",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Start listening for messages in the background
                                    launch(Dispatchers.IO) {
                                        while (socket.isConnected) {
                                            val message = chatService.receiveMessage(socket)
                                            if (message != null) {
                                                launch(Dispatchers.Main) {
                                                    receivedMessages.add("Received: $message")
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

        DisposableEffect(Unit) {
            onDispose {
                scanner.stopDiscovery()
                connectedSocket?.close()
            }
        }
    }
}