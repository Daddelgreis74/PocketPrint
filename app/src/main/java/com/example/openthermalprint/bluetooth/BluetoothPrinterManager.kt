package com.example.openthermalprint.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

data class PrinterDevice(
    val name: String,
    val address: String,
    val isBonded: Boolean
)

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data class Connecting(val deviceName: String) : ConnectionState()
    data class Connected(val deviceName: String, val address: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class BluetoothPrinterManager(private val context: Context) {

    companion object {
        // Standard SPP UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val CHUNK_SIZE = 512
        private const val CHUNK_DELAY_MS = 25L
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<PrinterDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var activeSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val scanReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val name = it.name ?: "Unbekanntes Gerät (${it.address})"
                        val isBonded = it.bondState == BluetoothDevice.BOND_BONDED
                        val printerDev = PrinterDevice(name, it.address, isBonded)

                        if (_discoveredDevices.value.none { d -> d.address == printerDev.address }) {
                            _discoveredDevices.value = _discoveredDevices.value + printerDev
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
            }
        }
    }

    init {
        loadBondedDevices()
    }

    @SuppressLint("MissingPermission")
    fun loadBondedDevices() {
        try {
            val bonded = bluetoothAdapter?.bondedDevices?.map {
                val devName = try { it.name ?: "Drucker" } catch (_: SecurityException) { "Drucker" }
                PrinterDevice(devName, it.address, true)
            } ?: emptyList()
            _discoveredDevices.value = bonded
        } catch (_: SecurityException) {
            // Wenn Berechtigungen noch nicht erteilt wurden (z.B. beim Erststart)
            _discoveredDevices.value = emptyList()
        } catch (_: Exception) {
            _discoveredDevices.value = emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter ?: return
        loadBondedDevices()
        if (adapter.isDiscovering) {
            adapter.cancelDiscovery()
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        try {
            context.registerReceiver(scanReceiver, filter)
            _isScanning.value = adapter.startDiscovery()
        } catch (_: Exception) {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            bluetoothAdapter?.cancelDiscovery()
            context.unregisterReceiver(scanReceiver)
        } catch (_: Exception) {
        }
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(deviceAddress: String): Boolean = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter ?: run {
            _connectionState.value = ConnectionState.Error("Bluetooth nicht verfügbar")
            return@withContext false
        }

        disconnect()

        val device = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Gerät nicht gefunden: ${e.localizedMessage}")
            return@withContext false
        }

        val deviceName = device.name ?: deviceAddress
        _connectionState.value = ConnectionState.Connecting(deviceName)

        if (adapter.isDiscovering) {
            adapter.cancelDiscovery()
        }

        try {
            // RFCOMM SPP Connection
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            activeSocket = socket
            outputStream = socket.outputStream
            _connectionState.value = ConnectionState.Connected(deviceName, deviceAddress)
            true
        } catch (e: Exception) {
            // Fallback: try fallback reflection method for older Bluetooth chips
            try {
                val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                val fallbackSocket = m.invoke(device, 1) as BluetoothSocket
                fallbackSocket.connect()
                activeSocket = fallbackSocket
                outputStream = fallbackSocket.outputStream
                _connectionState.value = ConnectionState.Connected(deviceName, deviceAddress)
                true
            } catch (e2: Exception) {
                disconnect()
                _connectionState.value = ConnectionState.Error("Verbindung fehlgeschlagen: ${e.message}")
                false
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            outputStream?.flush()
            outputStream?.close()
            activeSocket?.close()
        } catch (_: Exception) {
        } finally {
            outputStream = null
            activeSocket = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    /**
     * Send byte payload to the printer with packet pacing to prevent buffer overrun
     */
    suspend fun sendBytes(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val stream = outputStream ?: return@withContext Result.failure(
            IllegalStateException("Drucker ist nicht verbunden")
        )

        try {
            var offset = 0
            while (offset < bytes.size) {
                val chunkSize = (bytes.size - offset).coerceAtMost(CHUNK_SIZE)
                stream.write(bytes, offset, chunkSize)
                stream.flush()
                offset += chunkSize
                if (offset < bytes.size) {
                    kotlinx.coroutines.delay(CHUNK_DELAY_MS)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }
}
