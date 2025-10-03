package com.example.frontp.model

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.frontp.util.BroadcastKeys
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothConnectionService : Service() {

    companion object {
        const val ACTION_STOP_AND_DISCONNECT = "com.example.frontp.STOP_AND_DISCONNECT"
    }

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var deviceAddress: String
    private val btClientManager = BTClientManager()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _counter = MutableStateFlow(0) // ここはMutableStateFlowでOK

    // 外部に読み取り専用のStateFlowとして公開
    val counter: StateFlow<Int> = _counter.asStateFlow()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BTService", "onStartCommand開始")

        if (intent?.action == ACTION_STOP_AND_DISCONNECT) {
            Log.d("BTService", "切断＆サービス停止命令を受信")
            serviceScope.launch {
                btClientManager.disconnectFromDevice(deviceAddress)
                stopForeground(true)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        deviceAddress = intent?.getStringExtra("device_address") ?: run {
            Log.e("BTService", "device_addressがnullです。サービスを停止します。")
            return START_NOT_STICKY
        }
        Log.d("BTService", "deviceAddress=$deviceAddress")

        deviceAddress = intent?.getStringExtra("device_address") ?: return START_NOT_STICKY

        val deviceName = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            "不明なデバイス"
        } else {
            BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(deviceAddress)?.name ?: "不明なデバイス"
        }

        // 通知チャンネルの登録を startForeground 前に
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bt_channel",
                "Bluetooth接続",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        startForeground(1, createNotification(deviceName))

        serviceScope.launch {
            while (isActive) {
                val connected = btClientManager.isConnected(deviceAddress)
                Log.d("BTService", "接続チェック: $connected")
                if (!connected) {
                    try {
                        btClientManager.connectToDevice(deviceAddress)
                        Log.d("BTService", "再接続成功")
                        startReceivingInService()
                    } catch (e: Exception) {
                        Log.e("BTService", "再接続失敗: ${e.message}")
                    }
                }
                delay(2000)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true) // 通知を削除
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(deviceName: String): Notification {
        return NotificationCompat.Builder(this, "bt_channel")
            .setContentTitle("Bluetooth接続中")
            .setContentText("$deviceName に接続しています")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .build()
    }

    private fun startReceivingInService() {
        serviceScope.launch {
            while (btClientManager.isConnected(deviceAddress)) {
                try {
                    val rawData = btClientManager.receiveDataFromDevice(deviceAddress)
                    Log.d("BTService", "受信データ: $rawData")

                    if (rawData == null) {
                        _counter.value++ // ここを修正: MutableStateFlowの値にアクセス
                        Log.d("BTService", "受信データがnull, カウンター: ${_counter.value}") // ロギング追加 (前回指摘分)
                    } else {
                        _counter.value = 0 // データが正常に受信されたらカウンターをリセット (前回指摘分)
                    }

                    if (_counter.value >= 5){ // ここを修正: StateFlowの値にアクセスして比較
                        Log.w("BTService", "受信データがnullのため切断と再接続を試みます")
                        btClientManager.disconnectFromDevice(deviceAddress)
                        _counter.value = 0 // ここを修正: MutableStateFlowの値にアクセスしてリセット
                        break  // 上位の再接続ループへ
                    }

                    broadcastReceivedData(rawData)

                } catch (e: IOException) {
                    Log.e("BTService", "受信失敗: ${e.message}")
                    btClientManager.disconnectFromDevice(deviceAddress)
                    break
                }
            }
        }
    }


    private fun broadcastReceivedData(message: String?) {
        val intent = Intent(BroadcastKeys.ACTION_RAW_DATA)
        intent.putExtra(BroadcastKeys.EXTRA_RAW_DATA, message)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        Log.d("BluetoothConnectionService", "Broadcast sent: $message")
    }

}

fun stopBluetoothService(context: Context, mac: String) {
    Log.d("BTService", "stopBluetoothService called")

    val intent = Intent(context, BluetoothConnectionService::class.java).apply {
        action = BluetoothConnectionService.ACTION_STOP_AND_DISCONNECT
        putExtra("device_address", mac) // disconnectに必要
    }

    context.startService(intent) // ForegroundではなくServiceでOK
}

