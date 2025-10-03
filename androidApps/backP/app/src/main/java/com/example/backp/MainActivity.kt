package com.example.backp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.backp.ui.theme.BackPTheme
import android.util.Log
import androidx.activity.compose.setContent
import com.example.backp.ui.theme.BackPTheme
import com.example.backp.viewmodel.DeviceViewModel
import com.google.firebase.FirebaseApp
import androidx.navigation.compose.rememberNavController
import com.example.backp.navigation.NavigationGraph

class MainActivity : ComponentActivity() {
    private val deviceViewModel = DeviceViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebaseの初期化は残します
        if (FirebaseApp.initializeApp(this) == null) {
            Log.e("FirebaseInit", "FirebaseApp.initializeApp failed. Check your google-services.json")
        }

        // 初期UIの読み込み
        loadUI()
    }
    override fun onResume() {
        super.onResume()
        // Bluetoothパーミッションリクエストを削除
        // btPermissionHandler.requestBluetoothPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Bluetooth関連のBroadcastReceiver登録解除を削除
        // LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    // UIの読み込みはそのまま残します
    private fun loadUI() {
        setContent {
            val navController = rememberNavController()
            BackPTheme {
                NavigationGraph(
                    navController = navController,
                    // Bluetooth関連のコールバックを削除
                    // openBluetoothSettings = { openBluetoothSettings() },
                    deviceViewModel = deviceViewModel
                )
            }
        }
    }
}