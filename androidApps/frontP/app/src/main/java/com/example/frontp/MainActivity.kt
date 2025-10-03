package com.example.frontp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.frontp.ui.theme.FrontPTheme
import com.example.frontp.viewmodel.DeviceViewModel
import com.google.firebase.FirebaseApp
import android.content.Intent
import android.net.Uri
import androidx.navigation.compose.rememberNavController
import com.example.frontp.navigation.NavigationGraph
import android.provider.Settings
import com.example.frontp.model.BTPermissionHandler
import com.example.frontp.screens.PermissionDeniedSnackbar
import android.content.Context
import android.os.Build
import com.example.frontp.model.BluetoothConnectionService
import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.example.frontp.util.BroadcastKeys
import android.content.pm.PackageManager // PackageManager をインポート
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.frontp.model.BTClientManager


class MainActivity : ComponentActivity() {

    private lateinit var btPermissionHandler: BTPermissionHandler
    // deviceViewModelはonCreateで初期化されるため、lateinit varではなくby lazyなどで遅延初期化するか、
    // onCreateでViewModelProviderを使ってインスタンス化するのがよりAndroidのベストプラクティスです。
    // 今回のクラッシュとは直接関係ありませんが、考慮する点です。
    private val deviceViewModel = DeviceViewModel()
    private lateinit var receiver: BroadcastReceiver

    // @SuppressLint("UnspecifiedRegisterReceiverFlag") は不要になるので削除するのが望ましい
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BroadcastReceiverを登録
        // 起動中、BTで送られてきたデータをViewModelに送信し続ける
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val rawData = intent?.getStringExtra(BroadcastKeys.EXTRA_RAW_DATA)
                rawData?.let {
                    Log.d("MainActivity", "受信したデータ: $it")
                    deviceViewModel.updateLatestData(it)
                }
            }
        }

        val filter = IntentFilter(BroadcastKeys.ACTION_RAW_DATA)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)

        // Android 12 (API Level 31) 以降の対応
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Build.VERSION_CODES.S は API Level 31 を表す
            // アプリ内部からのブロードキャストのみを受け取る場合 (推奨される安全な方法)
            // ACTION_RAW_DATA が他のアプリから送信される可能性が低い場合、こちらを選択
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
            // もし他のアプリからもこのブロードキャストを受け取らせたい場合は、以下を使用（セキュリティリスクを考慮）
            // registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            // Android 11 (API Level 30) 以前
            registerReceiver(receiver, filter)
        }

        //BT権限許可要求
        btPermissionHandler = BTPermissionHandler(
            activity = this,
            onPermissionGranted = { loadUI() },
            onPermissionDenied = {
                showPermissionDeniedView()
                loadUI()
            }
        )

        btPermissionHandler.setupPermissionLauncher()

        // Firebase 初期化
        if (FirebaseApp.initializeApp(this) == null) {
            Log.e("FirebaseInit", "FirebaseApp.initializeApp failed. Check your google-services.json")
        }
    }

    override fun onResume() {
        super.onResume()
        btPermissionHandler.requestBluetoothPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        // メモリリーク防止
    }

    //NavigationGraph で Compose 画面遷移を管理
    //deviceViewModel を渡すことで、受信データを UI に反映
    private fun loadUI() {
        setContent {
            val navController = rememberNavController()
            FrontPTheme {
                NavigationGraph(
                    navController = navController,
                    openBluetoothSettings = { openBluetoothSettings() },
                    deviceViewModel = deviceViewModel // ここで渡す
                )
            }
        }
    }

    private fun showPermissionDeniedView() {
        setContent {
            PermissionDeniedSnackbar(onSettingsClick = { openAppSettings() })
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        startActivity(intent)
    }
}


fun startBluetoothService(context: Context, deviceAddress: String) {
    Log.d("MainActivity", "startBluetoothService called with $deviceAddress")
    val intent = Intent(context, BluetoothConnectionService::class.java)
    intent.putExtra("device_address", deviceAddress)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

//| メソッド            | 何をする場所？                                    |
//| --------------- | ------------------------------------------ |
//| `onCreate`      | Activity が最初に作られる時に呼ばれる。UI の初期化やデータ初期化に使う。 |
//| `onStart`       | 画面がユーザーに見える直前。画面の準備段階。                     |
//| **`onResume`**  | **画面が完全に表示され、ユーザー操作可能になった直後**。一番アクティブな状態。  |
//| `onPause`       | 画面が他の画面で覆われる直前。処理停止や一時保存に使う。               |
//| `onStop`        | 画面が完全に隠れた時。重い処理の停止に使う。                     |
//| **`onDestroy`** | **Activity が完全に消される時**。メモリ解放やリソース解放に使う。    |
