package com.example.backp.screens.views

import android.Manifest
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import com.google.accompanist.permissions.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.backp.viewmodel.DeviceViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import java.text.DecimalFormat
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.backp.screens.views.FirebaseSelect



//import androidx.compose.material3.Button
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.draw.drawBehind
var lastData: List<Any?> = listOf(null, null, null, null, null, null, null, null, null, null) // 👈 ここを変更
const val chara = 23//表示文字の大きさ
const val war = 20//警告の文字の大きさ


@Composable
fun ReceiveDataView(
    speedD: String?, heiD: String?, rpmD: String?, rollD: String?,
    pitchD: String?, yawD: String?, sw1D: String?, sw2D: String?,
    sw3D: String?, eAngD: String?, rAngD: String?,
    saveData: Boolean, // ← フラグ追加
    selectedValue: String?,
    viewModel: DeviceViewModel,
    navController: NavHostController
) {
    // State として定義 (表示用)
    var speed by remember { mutableStateOf<String?>(speedD) }
    var hei by remember { mutableStateOf<String?>(heiD) }
    var rpm by remember { mutableStateOf<String?>(rpmD) }
    var roll by remember { mutableStateOf<String?>(rollD) }
    var pitch by remember { mutableStateOf<String?>(pitchD) }
    var yaw by remember { mutableStateOf<String?>(yawD) }
    var eAng by remember { mutableStateOf<String?>(pitchD) }
    var rAng by remember { mutableStateOf<String?>(yawD) }
    // State として定義
    var lastSp by remember { mutableStateOf<String?>(null) }
    var lastHei by remember { mutableStateOf<String?>(null) }
    var lastRpm by remember { mutableStateOf<String?>(null) }
    var lastRoll by remember { mutableStateOf<String?>(null) }
    var lastPitch by remember { mutableStateOf<String?>(null) }
    var lastYaw by remember { mutableStateOf<String?>(null) }
    var lastEAng by remember { mutableStateOf<String?>(null) }
    var lastRAng by remember { mutableStateOf<String?>(null) }

    var currentLat by remember { mutableStateOf<Double?>(null) }
    var currentLng by remember { mutableStateOf<Double?>(null) }

    //var baseRoll by remember { mutableStateOf<String?>(null) }
    //var basePitch by remember { mutableStateOf<String?>(null) }
    //var baseYaw by remember { mutableStateOf<String?>(null) }

    val first=mutableListOf(1,2)
    for(i in 2 until 50){
        val next_num=first[i-2]+first[i-1]-1
        first.add(next_num)
    }
    Log.d("first", "$first")

    // rollD（String型）をDouble型に変換し、小数点以下2桁の文字列として整形
    val roll_f: String = remember(rollD) {
        rollD?.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00" // nullの場合は"0.00"をデフォルト値とする
    }
    // 同様にpitchDとyawDも処理
    val pitch_f: String = remember(pitchD) {
        pitchD?.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00"
    }
    val yaw_f: String = remember(yawD) {
        yawD?.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00"
    }

    Log.d("Formatted","rool: $roll_f, pitch: $pitch_f, yaw: $yaw_f")

    var lastTime by remember { mutableLongStateOf(0L) }
    // 位置情報が更新されたときに State を更新 (GoogleMapView からコールバックされる)
    val updateLocation = { lat: Double?, lng: Double? ->
        currentLat = lat
        currentLng = lng

    }

    //Log.d("ReceiveDataView", "ボタン$selectedValue")

    // isPlaying の初期状態を saveData に基づいて設定
    var isPlaying by remember(saveData) { mutableStateOf(saveData) } // saveData が true なら false (停止)、false なら true (再生)
    var selectedIndex by remember { mutableIntStateOf(0) } // 初期選択は0番目（"1本目"）
    val spinnerItems = remember { listOf("1本目", "2本目", "3本目", "4本目", "5本目", "6本目", "7本目", "8本目", "9本目", "10本目", "11本目", "12本目", "13本目", "14本目", "15本目") }
    var currentSelectedItem by remember { mutableStateOf(spinnerItems.firstOrNull() ?: "") } // 現在の選択肢のテキスト

    val context = LocalContext.current
    // ReceiveDataScreenから呼び出された場合のみデータ保存実行

    // 経過時間を追跡するための State
    var elapsedTime by remember { mutableLongStateOf(0L) }
    //Log.d("ReceiveDataView", "再生: $isPlaying")

    LaunchedEffect(Unit) {
        while (isActive) {
            if (!isPlaying) {
                FirebaseFlag.writeFlagValue(0, selectedValue, currentSelectedItem)// 再生していない間は定期的に0を書き込む
            } else {
                FirebaseFlag.writeFlagValue(1, selectedValue, currentSelectedItem)// 再生中なら1を書き込む
            }
            delay(500)
        }
    }


    // 再生状態を監視し、経過時間を更新する
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                elapsedTime = System.currentTimeMillis() - startTime
                lastTime = elapsedTime
                delay(10) // 1/100秒ごとに更新
                //基準値決定、ESP32からデータ受信するスマホでやる必要あるので、再考慮
                /*if (savay(100)
                    baseRoll = rollD
                    basePitch = pitchD
                    baseYaw = yawD
                    Log.d("基準値", "baseRoll: $baseRoll, basePitch: $basePitch, baseYaw: $baseYaw")
                }*/
            }
        } else {
            elapsedTime = lastTime // 停止したらリセット
        }
    }

    // 経過時間をフォーマット (例: 12.34 秒)
    val formattedTime = remember(elapsedTime) {
        DecimalFormat("#0.00").format(elapsedTime / 1000.0)
    }

    LaunchedEffect(speedD, heiD, rpmD, rollD, pitchD, yawD, eAngD, rAngD, currentLat, currentLng) {
        if (isPlaying) {
            // 緯度経度がnullのとき、再取得を試みる
            var lat = currentLat
            var lng = currentLng
            var retries = 0
            while ((lat == null || lng == null) && retries < 5) {
                delay(500) // 少し待つ
                lat = currentLat
                lng = currentLng
                retries++
            }

            saveToSheetIfUpdated(
                context,
                speedD,
                heiD,
                rpmD,
                rollD,
                pitchD,
                yawD,
                eAngD,
                rAngD,
                sw1D,
                selectedValue,
                currentSelectedItem,
                lat,
                lng,
                isPlaying
            )
            Log.d("ReceiveDataView", "$speedD , $heiD , $rpmD , $rollD , $pitchD , $yawD , $eAngD , $rAngD")
        }
    }


    if(!isPlaying){
        speed = lastSp
        hei = lastHei
        rpm = lastRpm
        roll = lastRoll
        pitch = lastPitch
        yaw = lastYaw
        eAng = lastEAng
        rAng = lastRAng
        /*baseRoll = null
        basePitch = null
        baseYaw = null*/
        //Log.d("ReceiveDataView", "baseRoll: $baseRoll, basePitch: $basePitch, baseYaw: $baseYaw")
    }else{
        /*
        if(saveData){ // Firebaseのデータ読み取り中は補正必要なし(これがないとクラッシュ)
            viewModel.rollPitchYaw(baseRoll, basePitch, baseYaw, rollD, pitchD, yawD) // 基準値と現在の値から補正値を取得
            roll = viewModel.mRoll.collectAsState().value.toString()
            pitch = viewModel.mPitch.collectAsState().value.toString()
            yaw = viewModel.mYaw.collectAsState().value.toString()
        } else {
            roll = rollD
            pitch = pitchD
            yaw = yawD
        }

         */
        roll = rollD
        pitch = pitchD
        yaw = yawD
        speed = speedD
        hei = heiD
        rpm = rpmD
        eAng = eAngD
        rAng = rAngD
    }
    // 高度を Float に変換（null やエラー時は 0m）
    val altitude = hei?.toFloatOrNull() ?: 0f
    val normalizedAltitude = (altitude / 10f).coerceIn(0.00001f, 1f) // 0.00001 〜 1.0 の範囲に制限(widthの値が0だとエラー出る)

    // 高度に応じて青 (240°) から赤 (0°) への色変化
    val hue = 240f * (1 - normalizedAltitude)  // 高度が高いほど 240 → 0 に変化
    val color = Color.hsv(hue, 1f, 1f) // HSV で指定



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    // dragAmount < 0 → 上方向のスワイプ
                    if (dragAmount < -30) { // 上方向にある程度動いたら
                        navController.popBackStack("main", false)
                    }
                }
            }
    ) {
        // 画像表示を上部に追加
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f)
                .border(1.dp, Color.Black)
        ){
            GoogleMapView(onLocationChange = updateLocation)//マップ表示
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly // 👈 水平方向に均等に配置
        ) {
            Text(text = "E角度: $eAng °", style = TextStyle(fontSize = 30.sp))
            Text(text = "R角度: $rAng °", style = TextStyle(fontSize = 30.sp))
            lastEAng = eAng
            lastRAng = rAng
        }
        Spacer(modifier = Modifier.height(16.dp))


        // 高度
        Text(text = "高度: $hei m", style = TextStyle(fontSize = chara.sp))
        lastHei = hei
        // 高度バー
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Black)
                .size(30.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = size.width * normalizedAltitude
                drawRect(color, size = Size(barWidth, size.height))
            }
        }

        // **対気速度と回転数、Spinnerとボタンを横並びにする Row**
        if (!saveData) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically, // 縦方向中央揃え
                // horizontalArrangement は weight を使う場合はあまり効果がないため削除または調整
            ) {
                // 左側の要素 (対気速度と回転数)
                Column(
                    modifier = Modifier.weight(4f), // 👈 weight を設定 (右側の2倍の比率)
                    horizontalAlignment = Alignment.Start // 左寄せ
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "機速: $speed m/s", style = TextStyle(fontSize = chara.sp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "タイム: $formattedTime 秒", style = TextStyle(fontSize = chara.sp))
                    Spacer(modifier = Modifier.height(16.dp))

                    lastSp = speed
                    lastRpm = rpm
                }

                // 右側の要素 (Spinnerとボタン)
                Column(
                    modifier = Modifier.weight(3f), // 👈 weight を設定
                    horizontalAlignment = Alignment.End // 右寄せ
                ) {
                    // Spinner
                    AndroidView(
                        factory = {
                            Spinner(context).apply {
                                adapter = ArrayAdapter(
                                    context,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    spinnerItems
                                )
                                onItemSelectedListener =
                                    object : AdapterView.OnItemSelectedListener {
                                        override fun onItemSelected(
                                            parent: AdapterView<*>?,
                                            view: android.view.View?,
                                            position: Int,
                                            id: Long
                                        ) {
                                            selectedIndex = position
                                            currentSelectedItem = spinnerItems[position]
                                        }

                                        override fun onNothingSelected(parent: AdapterView<*>?) {
                                            // 特に何もしない
                                        }
                                    }
                            }
                        },
                        update = { spinner ->
                            // インデックスが変更されたときに、プログラムからSpinnerの選択状態を更新
                            if (spinner.selectedItemPosition != selectedIndex && spinnerItems.indices.contains(
                                    selectedIndex
                                )
                            ) {
                                spinner.setSelection(selectedIndex)
                            }
                        },
                        modifier = Modifier.fillMaxWidth() // Spinnerを右側のColumn内でできるだけ広げる
                    )

                    // 再生/一時停止ボタン
                    Button(
                        onClick = {
                            // ボタンの状態を切り替え
                            isPlaying = !isPlaying
                            // ボタンが「一時停止」になった瞬間にSpinnerの値を更新
                            if (!isPlaying) {
                                selectedIndex++
                                if (selectedIndex >= spinnerItems.size) {
                                    selectedIndex = 0 // 最後の項目を超えたら最初に戻るなどの処理
                                }
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) Color.Red else Color(0xFF388E3C), // より濃い緑色
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth() // 👈 ここに追加
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "停止" else "再生",
                            tint = Color.White
                        )
                        androidx.compose.material3.Text(
                            if (isPlaying) "停止" else "再生",
                            color = Color.White
                        )
                    }
                }
            }
        }else{
            Spacer(modifier = Modifier.height(16.dp))

            // 対気速度
            Text(text = "対気速度: $speed m/s", style = TextStyle(fontSize = chara.sp))
            Spacer(modifier = Modifier.height(16.dp))


        }

        //Spacer(modifier = Modifier.height(16.dp))
        //androidx.compose.material3.Text("選択中の項目: $currentSelectedItem")

        // 下1/4エリア
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // 🔹 残りのスペースを均等に分配
                .border(1.dp, Color.Black)

        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // **左のスペース**
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, Color.Black)
                ) {
                    MiniModelView1(pitch)
                    lastPitch = pitch
                }

                // **右のスペース**
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, Color.Black)
                ) {
                    MiniModelView2(roll)
                    lastRoll = roll
                }
            }
        }
    }
}

@Composable
fun MiniModelView1(latestDataD: String?) {

    val pitchAngle = latestDataD?.toFloatOrNull() ?: 0f

    Canvas(modifier = Modifier.fillMaxSize()) {
        rotate(pitchAngle, pivot = center) {  // ピッチ角分回転
            drawLine(
                Color.Black, start = Offset(10f, size.height / 2),
                end = Offset(size.width - 50f, size.height / 2), 12f
            )
            drawLine(
                Color.Black,
                start = Offset(60f, size.height / 2 + 50f),
                end = Offset(60f, size.height / 2 - 80f),
                8f
            )
            drawLine(
                Color.Black,
                start = Offset(size.width * 0.72f, size.height / 2),
                end = Offset(
                    size.width * 0.72f,
                    size.height / 2 - 80f
                ),
                8f
            )
            scale(scaleX = 1.5f, scaleY = 1f) {
                drawCircle(
                    Color.Gray, radius = 15.dp.toPx(),
                    center = Offset(
                        size.width * 0.65f,
                        size.height / 2 + 30f
                    )
                )
            }
        }
    }
    Text(text = "ピッチ: $latestDataD °", style = TextStyle(fontSize = chara.sp))
}

@Composable
fun MiniModelView2(latestDataE: String?) {
    val rollAngle = latestDataE?.toFloatOrNull() ?: 0f
    Canvas(modifier = Modifier.fillMaxSize()) {
        rotate(rollAngle, pivot = center) {  // ロール角分回転
            scale(scaleX = 0.8f, scaleY = 1.3f) {
                drawCircle(
                    Color.Gray, radius = 15.dp.toPx(),
                    center = Offset(
                        size.width / 2,
                        size.height / 2 * 1.1f
                    )
                )
            }
            drawLine(
                Color.Black,
                start = Offset(50f, size.height / 2),
                end = Offset(size.width - 50f, size.height / 2),
                strokeWidth = 12f
            )
            drawLine(
                Color.Black,
                start = Offset(size.width / 2, size.width / 2 - 90f),
                end = Offset(size.width / 2, size.height / 2 + 30f),
                strokeWidth = 8f
            )
        }
    }
    Text(text = "ロール: $latestDataE °", style = TextStyle(fontSize = chara.sp))
}

@SuppressLint("MissingPermission")
fun startLocationUpdates(context: Context, onLocationUpdated: (Location?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).apply {
        setMinUpdateIntervalMillis(2000) // 最小更新間隔 2秒
    }.build()

    // 位置情報更新のコールバック
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            onLocationUpdated(location)
        }
    }

    // 位置情報更新のリクエストを開始
    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GoogleMapView(onLocationChange: (Double?, Double?) -> Unit) { // 👈 コールバックを受け取る
    val context = LocalContext.current

    // 位置情報パーミッションの状態を監視
    val locationPermissionState =
        rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // 現在地の初期値は "取得待ち" 状態を表す
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }

    // カメラ位置の初期値（仮置き）
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(35.329977, 136.189374), 11.8f)
    }

    // パーミッションのリクエストを実行
    LaunchedEffect(Unit) {
        locationPermissionState.launchPermissionRequest()
    }

    // パーミッションが許可された場合のみ位置情報を取得
    if (locationPermissionState.status.isGranted) {
        LaunchedEffect(Unit) {
            startLocationUpdates(context) { location ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    // 現在のズームレベルを取得し、位置のみ更新
                    val currentZoom = cameraPositionState.position.zoom
                    // 地図のカメラ位置を更新
                    val cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLocation!!, currentZoom)
                    cameraPositionState.move(cameraUpdate)
                    // 位置情報が更新されたらコールバックを呼び出す
                    onLocationChange(location.latitude, location.longitude)
                    Log.d("ReceiveDataView", "位置情報更新: $location")
                }
            }
        }
    }

    // Google Map の表示
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = locationPermissionState.status.isGranted
        )
    ) {
        Marker(
            state = rememberMarkerState(position = LatLng(35.682839, 139.759455)),
            title = "折り返し地点",
            snippet = "折り返し地点"
        )
    }

    // パーミッションが拒否された場合のエラーメッセージ
    if (!locationPermissionState.status.isGranted) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "位置情報のパーミッションが許可されていません。", color = Color.Red, fontSize = war.sp)
        }
    }

    // 位置情報の取得待ちのメッセージ
    if (currentLocation == null && locationPermissionState.status.isGranted) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "位置情報を取得中...", color = Color.Gray, fontSize = war.sp)
        }
    }
}

fun saveToSheetIfUpdated(context: Context,
                         speedD: String?,
                         heiD: String?,
                         rpmD: String?,
                         rollD: String?,
                         pitchD: String?,
                         yawD: String?,
                         eAngD:String?,
                         rAngD:String?,
                         sw1D: String?,
                         selectedValue: String?,
                         currentSelectedItem:String?,
                         latitude: Double?, // 👈 緯度を受け取る
                         longitude: Double?, // 👈 経度を受け取る
                         isPlaying: Boolean
) {
    val newData = listOf(speedD, heiD, rpmD, rollD, pitchD, yawD, eAngD, rAngD, latitude, longitude)
    if (newData != lastData) {

        // rowDataをList<Any>型で作成（nullもそのまま渡す）
        val rowData: List<Any?> = listOf(
            sw1D,  // ラップ
            speedD,     // speedD (nullがそのまま渡されます)
            heiD,       // heiD (nullがそのまま渡されます)
            rpmD,       // rpmD (nullがそのまま渡されます)
            rollD,      // rollD (nullがそのまま渡されます)
            pitchD,     // pitchD (nullがそのまま渡されます)
            yawD,       // yawD (nullがそのまま渡されます)
            eAngD,      // eAngD (nullがそのまま渡されます)
            rAngD,       // rAngD (nullがそのまま渡されます)
            latitude,  // 👈 緯度を追加
            longitude  // 👈 経度を追加
        )

        Log.d("ReceiveDataView", "データ$rowData")
/*
        // Firebase に送信
        if(isPlaying) {
            FirebaseSelect.writeToFirebase(rowData, selectedValue, currentSelectedItem)
            // GoogleSheetsHelper.writeToSheetにcontextとrowDataを渡す
            GoogleSheetsHelper.writeToSheet(rowData)
        }

 */
        lastData = newData
    }
}


