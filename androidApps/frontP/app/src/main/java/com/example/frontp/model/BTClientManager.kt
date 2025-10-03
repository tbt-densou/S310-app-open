package com.example.frontp.model

import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException


/**
 * デバイスとの接続を複数管理するクラス
 */
class BTClientManager {
    private val clients: MutableMap<String, BTClient> = mutableMapOf()
    private val socketMap = mutableMapOf<String, BluetoothSocket>()

    fun isConnected(deviceAddress: String): Boolean {
        return clients[deviceAddress]?.isConnected ?: false
    }
    fun connectToDevice(deviceAddress: String) {
        val client = BTClient()
        client.connectToDevice(deviceAddress)
        clients[deviceAddress] = client
    }

    fun deviceName(deviceAddress: String): String? {
        return clients[deviceAddress]?.deviceName ?: "N/A"
    }

    fun sendDataToDevice(deviceAddress: String, data: String) {
        val client = clients[deviceAddress]
        client?.sendData(data)
    }

    fun receiveDataFromDevice(deviceAddress: String): String? {
        val client = clients[deviceAddress]
        return client?.receiveData()
    }

    fun disconnectDevice(deviceAddress: String) {
        val client = clients[deviceAddress]
        client?.closeConnection()
        clients.remove(deviceAddress)
    }

    fun disconnectAllDevices() {
        clients.keys.toList().forEach { disconnectDevice(it) }
    }

    fun disconnectFromDevice(address: String) {
        try {
            socketMap[address]?.apply {
                close()
                Log.d("BTClientManager", "切断しました: $address")
            }
            socketMap.remove(address)
        } catch (e: IOException) {
            Log.e("BTClientManager", "切断エラー: ${e.message}")
        }
    }
}