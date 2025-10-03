package com.example.backp.screens.views

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.backp.viewmodel.FirebaseViewModel
import com.example.backp.viewmodel.DeviceViewModel
import android.util.Log
import androidx.navigation.NavHostController
import com.example.backp.screens.views.ReceiveDataView

@Composable
fun FirebaseReader(viewModel: FirebaseViewModel, deviceViewModel: DeviceViewModel, navController: NavHostController) {
    val latestData by viewModel.latestData.collectAsState()
    val selectedValue by viewModel.selectedValue.collectAsState()

    // デバッグ用: latestData の値をログに出力
    LaunchedEffect(latestData) {
        Log.d("FirebaseReader", "Latest Data: $latestData")
    }


    latestData?.let { data ->
        val speedD=data.speed?:null
        val heiD=data.height?:null
        val rpmD=data.rpm?:null
        val rollD=data.roll?:null
        val pitchD=data.pitch?:null
        val yawD=data.yaw?:null
        val eAngD=data.eAngle?:null
        val rAngD=data.rAngle?:null
        val sw1D=data.sw1?:null
        val sw2D=data.sw1?:null
        val sw3D=data.sw1?:null

        // 最新データを表示するビューにデータを渡す
        ReceiveDataView(
            speedD = speedD.toString(),
            rpmD = rpmD.toString(),
            heiD = heiD.toString(),

            rollD = rollD.toString(),
            pitchD = pitchD.toString(),
            yawD = yawD.toString(),

            eAngD = eAngD.toString(),
            rAngD = rAngD.toString(),

            sw1D = sw1D.toString(),
            sw2D = sw2D.toString(),
            sw3D = sw3D.toString(),
            saveData = false,
            selectedValue = selectedValue,
            viewModel = deviceViewModel,
            navController = navController,
        )
    } ?: Text("No data available.")

}
