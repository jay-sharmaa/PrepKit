package com.example.prepkit

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class WifiStateReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val activity: MainActivity,
    private val viewModel: WifiViewModel
) : BroadcastReceiver() {
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                activity.isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel) { peers ->
                    viewModel.updatePeerList(peers.deviceList.toList())
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo?.isConnected == true) {
                    manager.requestConnectionInfo(channel) { info ->
                        if (info.groupFormed && info.isGroupOwner) {
                            Log.d("ConnectionState", "Client")
                            Toast.makeText(activity, "Connected as Server", Toast.LENGTH_SHORT).show()
                            Thread {
                                WifiDirectSocketManager.startServer()
                            }
                        } else if (info.groupFormed) {
                            Log.d("ConnectionState", "Client")
                            Toast.makeText(activity, "Connected as Client", Toast.LENGTH_SHORT).show()
                            Thread {
                                WifiDirectSocketManager.startClient(info.groupOwnerAddress.hostAddress!!)
                            }
                        }
                    }
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                val device = intent.getParcelableExtra<WifiP2pDevice>(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
                )
                device?.let {
                    viewModel.updateThisDevice(it)
                }
            }
        }
    }
}

class WifiViewModel : ViewModel() {
    var thisDevice by mutableStateOf<WifiP2pDevice?>(null)
        private set

    var peerList by mutableStateOf<List<WifiP2pDevice>>(emptyList())
        private set

    fun updatePeerList(peers: List<WifiP2pDevice>) {
        peerList = peers
    }

    fun updateThisDevice(device: WifiP2pDevice) {
        thisDevice = device
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun connectToPeer(device: WifiP2pDevice, manager: WifiP2pManager, channel: WifiP2pManager.Channel) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("ConnectionState", "Success ${device.deviceName}")
            }

            override fun onFailure(reason: Int) {
                Log.d("ConnectionState", "Failure $reason")
            }
        })
    }
}

object WifiDirectSocketManager {
    private const val PORT = 8888

    fun startServer() {
        try {
            val serverSocket = java.net.ServerSocket(PORT)
            Log.d("Socket", "Server: Waiting for client...")
            val client = serverSocket.accept()
            Log.d("Socket", "Server: Client connected")

            val input = client.getInputStream()
            val output = client.getOutputStream()

            // Example: Send data to client
            output.write("Hello from server".toByteArray())

            // Example: Read data from client
            val buffer = ByteArray(1024)
            val bytesRead = input.read(buffer)
            val message = String(buffer, 0, bytesRead)
            Log.d("Socket", "Server received: $message")

            client.close()
            serverSocket.close()
        } catch (e: Exception) {
            Log.e("Socket", "Server error: ${e.message}")
        }
    }

    fun startClient(hostAddress: String) {
        try {
            val socket = java.net.Socket()
            socket.bind(null)
            socket.connect(java.net.InetSocketAddress(hostAddress, PORT), 5000)

            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            // Example: Send data to server
            output.write("Hello from client".toByteArray())

            // Example: Read data from server
            val buffer = ByteArray(1024)
            val bytesRead = input.read(buffer)
            val message = String(buffer, 0, bytesRead)
            Log.d("Socket", "Client received: $message")

            socket.close()
        } catch (e: Exception) {
            Log.e("Socket", "Client error: ${e.message}")
        }
    }
}