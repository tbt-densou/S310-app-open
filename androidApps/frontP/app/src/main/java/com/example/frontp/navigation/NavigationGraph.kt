package com.example.frontp.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.frontp.screens.MainScreen
import com.example.frontp.screens.ReceiveDataScreen
import com.example.frontp.screens.views.FirebaseReader
import com.example.frontp.viewmodel.DeviceViewModel
import com.example.frontp.viewmodel.FirebaseViewModel


/**
 * アプリケーション内の画面遷移（ナビゲーション）を管理するComposable関数
 *
 * @param navController: NavHostController - 画面遷移を管理するコントローラー
 * @param openBluetoothSettings: () -> Unit - Bluetooth設定画面を開くためのコールバック関数
 */
@Composable
fun NavigationGraph(
    navController: NavHostController,
    deviceViewModel: DeviceViewModel,  // ViewModel を作成
    firebaseViewModel: FirebaseViewModel = viewModel(), // ここで ViewModel を作成
    openBluetoothSettings: () -> Unit
) {
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController, deviceViewModel, firebaseViewModel)
        }
        composable("firebase") {
            FirebaseReader(firebaseViewModel, deviceViewModel, navController) // 修正後の FirebaseReader に渡す
        }
        composable("espReader") {
            ReceiveDataScreen(deviceViewModel, navController)
        }
    }
}
