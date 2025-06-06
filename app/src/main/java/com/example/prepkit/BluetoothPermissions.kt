package com.example.prepkit

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.IOException
import java.util.UUID

@Composable
fun BluetoothPermissionHandler(onGranted: () -> Unit) {
    val context = LocalContext.current
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) {
            onGranted()
        } else {
            Toast.makeText(context, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val allGranted = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            onGranted()
        } else {
            launcher.launch(permissions)
        }
    }
}

class BluetoothDeviceScanner(
    private val context: Context,
    private val onDeviceFound: (BluetoothDevice) -> Unit
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private var isReceiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { onDeviceFound(it) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d("BluetoothScanner", "Discovery finished")
                }
            }
        }
    }

    fun startDiscovery() {
        if (!hasBluetoothPermissions()) {
            Log.e("BluetoothScanner", "Missing Bluetooth permissions")
            return
        }

        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(receiver, filter)
            isReceiverRegistered = true
        }

        bluetoothAdapter?.let { adapter ->
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            adapter.startDiscovery()
        }
    }

    fun stopDiscovery() {
        if (!hasBluetoothPermissions()) return

        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(receiver)
                isReceiverRegistered = false
            }
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            Log.e("BluetoothScanner", "Error stopping discovery", e)
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}

class BluetoothConnection(private val context: Context) {
    companion object {
        private val UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    suspend fun connectToDevice(device: BluetoothDevice): BluetoothSocket? = withContext(Dispatchers.IO) {
        if (!hasBluetoothConnectPermission()) {
            Log.e("BluetoothConnection", "BLUETOOTH_CONNECT permission not granted")
            return@withContext null
        }

        try {
            val socket = device.createRfcommSocketToServiceRecord(UUID_SPP)
            socket.connect()
            socket
        } catch (e: IOException) {
            Log.e("BluetoothConnection", "Failed to connect to device", e)
            // Try fallback method
            try {
                val fallbackSocket = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    .invoke(device, 1) as BluetoothSocket
                fallbackSocket.connect()
                fallbackSocket
            } catch (fallbackE: Exception) {
                Log.e("BluetoothConnection", "Fallback connection also failed", fallbackE)
                null
            }
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

class BluetoothChatService(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    suspend fun listenForConnection(): BluetoothSocket? = withContext(Dispatchers.IO) {
        if (!hasBluetoothConnectPermission()) {
            Log.e("BluetoothChatService", "BLUETOOTH_CONNECT permission not granted")
            return@withContext null
        }

        val serverSocket: BluetoothServerSocket? = try {
            adapter?.listenUsingRfcommWithServiceRecord("BTChat", uuid)
        } catch (e: IOException) {
            Log.e("BluetoothChatService", "Failed to create server socket", e)
            null
        }

        try {
            serverSocket?.accept()
        } catch (e: IOException) {
            Log.e("BluetoothChatService", "Failed to accept connection", e)
            null
        } finally {
            serverSocket?.close()
        }
    }

    fun sendMessage(socket: BluetoothSocket, message: String): Boolean {
        return try {
            socket.outputStream.write(message.toByteArray())
            socket.outputStream.flush()
            true
        } catch (e: IOException) {
            Log.e("BluetoothChatService", "Failed to send message", e)
            false
        }
    }

    fun receiveMessage(socket: BluetoothSocket): String? {
        return try {
            val buffer = ByteArray(1024)
            val bytes = socket.inputStream.read(buffer)
            String(buffer, 0, bytes)
        } catch (e: IOException) {
            Log.e("BluetoothChatService", "Failed to receive message", e)
            null
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothDeviceListScreen(
    devices: List<BluetoothDevice>,
    onDeviceClick: (BluetoothDevice) -> Unit,
    isScanning: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (isScanning) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scanning for devices...")
            }
        }

        LazyColumn {
            items(devices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceClick(device) }
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = device.name ?: "Unknown Device",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = device.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Bond State: ${getBondStateString(device.bondState)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getBondStateString(bondState: Int): String {
    return when (bondState) {
        BluetoothDevice.BOND_NONE -> "Not Paired"
        BluetoothDevice.BOND_BONDING -> "Pairing..."
        BluetoothDevice.BOND_BONDED -> "Paired"
        else -> "Unknown"
    }
}