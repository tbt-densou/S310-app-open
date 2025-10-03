package com.example.frontp.screens.views

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import org.json.JSONArray
import com.example.frontp.dataclass.FlightData



object GoogleSheetsHelper {
    private const val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxtyMzDHha4voVkNxIFCDssaFpGR5pK9O7GsWltk2QNoxnokQey0fyGoNucvVDWkRc/exec"

    fun writeToSheet(rowData: List<Any?>) {
        Thread {
            try {
                val url = URL(SCRIPT_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                val jsonPayload = JSONObject()
                jsonPayload.put("value", rowData.joinToString(","))

                val outputStream = connection.outputStream
                outputStream.write(jsonPayload.toString().toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                Log.d("GoogleSheetsHelper", "Response: $responseCode")
            } catch (e: Exception) {
                Log.e("GoogleSheetsHelper", "Error: ${e.message}", e)
                e.printStackTrace()
            }
        }.start()
    }
}

object GoogleSheetsHelper2 {
    private const val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbyTyrUFpcikkP-F2Cde5Sab634p-aoDVfQ_lpILvkzjdO3Yab532Ool70IRwJO2s50/exec"
    private val dataBuffer = mutableListOf<List<Any?>>()

    init {
        startPeriodicSend()
    }

    // ✅ 呼び出し時点でタイムスタンプをつける
    fun addDataRow(data: List<Any?>) {
        val timestamp = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val rowWithTimestamp = listOf(timestamp) + data
        synchronized(dataBuffer) {
            dataBuffer.add(rowWithTimestamp)
        }
    }

    private fun startPeriodicSend() {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                val toSend: List<List<Any?>>
                synchronized(dataBuffer) {
                    if (dataBuffer.isEmpty()) return
                    toSend = dataBuffer.toList()
                    dataBuffer.clear()
                }
                sendData(toSend)
            }
        }, 1000, 1000)
    }

    private fun sendData(rows: List<List<Any?>>) {
        Thread {
            try {
                val jsonPayload = JSONObject()
                jsonPayload.put("rows", JSONArray(rows.map { JSONArray(it) }))

                val url = URL(SCRIPT_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                connection.outputStream.use { it.write(jsonPayload.toString().toByteArray()) }

                val responseCode = connection.responseCode
                Log.d("GoogleSheetsHelper", "Response: $responseCode")
                Log.d("GoogleSheetsHelper", "$rows")
            } catch (e: Exception) {
                Log.e("GoogleSheetsHelper", "Error: ${e.message}", e)
            }
        }.start()
    }
}


object FirebaseHelper {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val dataRef: DatabaseReference = database.getReference("sensorData")

    fun writeToFirebase(rowData: List<Any?>) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            if (rowData.any { it != null }) {
                val dataMap = mutableMapOf<String, Any?>()

                rowData.getOrNull(1)?.let { dataMap["speed"] = it }
                rowData.getOrNull(2)?.let { dataMap["height"] = it }
                rowData.getOrNull(3)?.let { dataMap["roll"] = it }
                rowData.getOrNull(4)?.let { dataMap["pitch"] = it }
                rowData.getOrNull(5)?.let { dataMap["yaw"] = it }
                rowData.getOrNull(6)?.let { dataMap["eAngle"] = it }
                rowData.getOrNull(7)?.let { dataMap["rAngle"] = it }
                rowData.getOrNull(8)?.let { dataMap["latitude"] = it }
                rowData.getOrNull(9)?.let { dataMap["longitude"] = it }

                dataMap["timestamp"] = System.currentTimeMillis()

                // データ数をチェックして10件超えていたら古い順に削除
                dataRef.orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val children = snapshot.children.toList()
                        if (children.size >= 20) {
                            val numToDelete = children.size - 19 // 最新の9件を残す
                            children.take(numToDelete).forEach { child ->
                                child.ref.removeValue()
                            }
                        }

                        // データ追加
                        dataRef.push().setValue(dataMap)
                            .addOnSuccessListener {
                                Log.d("FirebaseHelper", "🔥 Firebase にデータ送信成功！")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseHelper", "⚠️ Firebase 送信エラー: ${e.message}")
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FirebaseHelper", "⚠️ データ取得キャンセル: ${error.message}")
                    }
                })
            } else {
                Log.e("FirebaseHelper", "⚠️ 有効なデータがありません。保存をスキップします。")
            }
        } else {
            Log.e("FirebaseHelper", "⚠️ ユーザーが認証されていません。")
        }
    }
}


object FirebaseSelect {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    fun writeToFirebase(rowData: List<Any?>, selectedValue: String?, currentSelectedItem:String?) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            if (rowData.any { it != null }) {
                val dataMap = mutableMapOf<String, Any?>()

                rowData.getOrNull(1)?.let { dataMap["speed"] = it }
                rowData.getOrNull(2)?.let { dataMap["height"] = it }
                rowData.getOrNull(3)?.let { dataMap["roll"] = it }
                rowData.getOrNull(4)?.let { dataMap["pitch"] = it }
                rowData.getOrNull(5)?.let { dataMap["yaw"] = it }
                rowData.getOrNull(6)?.let { dataMap["eAngle"] = it }
                rowData.getOrNull(7)?.let { dataMap["rAngle"] = it }
                rowData.getOrNull(8)?.let { dataMap["latitude"] = it }
                rowData.getOrNull(9)?.let { dataMap["longitude"] = it }

                dataMap["timestamp"] = System.currentTimeMillis()

                // ここ！selectedValueがnullや空じゃなければ保存
                selectedValue?.takeIf { it.isNotEmpty() }?.let { key ->
                    val validKeys = listOf(
                        "デバック",
                        "全体接合",
                        "1st走行",
                        "2nd走行",
                        "1stTF",
                        "2ndTF",
                        "3rdTF",
                        "4thTF",
                        "5thTF",
                        "6thTF",
                        "7thTF",
                        "最終TF"
                    )

                    val safeKey = if (key in validKeys) key else "other"

                    currentSelectedItem?.takeIf { it.isNotEmpty() }?.let { key ->
                        val flightKeys = listOf(
                            "1本目",
                            "2本目",
                            "3本目",
                            "4本目",
                            "5本目",
                            "6本目",
                            "7本目",
                            "8本目",
                            "9本目",
                            "10本目",
                            "11本目",
                            "12本目",
                            "13本目",
                            "14本目",
                            "15本目"
                        )

                        val fliKey = if (key in flightKeys) key else "other"

                        val sortedRef = database.getReference(safeKey).child(fliKey)
                        sortedRef.push().setValue(dataMap)
                            .addOnSuccessListener {
                                Log.d("FirebaseHelper", "✅ sortedData/$safeKey に保存成功！")
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "FirebaseHelper",
                                    "⚠️ sortedData/$safeKey 保存失敗: ${e.message}"
                                )
                            }
                    }
                }

            } else {
                Log.e("FirebaseHelper", "⚠️ 有効なデータがありません。保存をスキップします。")
            }
        } else {
            Log.e("FirebaseHelper", "⚠️ ユーザーが認証されていません。")
        }
    }
}

object FirebaseFlag {
    private var flagListener: ValueEventListener? = null
    private var lastFlagUpdateTime = System.currentTimeMillis()
    private var lastValidValue: Int? = null
    private var nullCount = 0

    fun startMonitoringFlag(onValueChanged: (Int) -> Unit) {
        val flagRef = FirebaseDatabase.getInstance().getReference("Flag")

        flagListener?.let { flagRef.removeEventListener(it) }

        flagListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latest = snapshot.children.lastOrNull()
                val value = latest?.getValue(Int::class.java)

                Log.d("FirebaseFlag", "🚩 Flagの現在の値: $value")

                lastFlagUpdateTime = System.currentTimeMillis()

                if (value != null) {
                    lastValidValue = value
                    nullCount = 0
                    onValueChanged(value)
                } else {
                    nullCount++
                    if (nullCount >= 3) {
                        Log.d("FirebaseFlag", "⚠️ 連続3回nullだったので 0 とみなします")
                        lastValidValue = 0
                        nullCount = 0
                        onValueChanged(0)
                    } else {
                        Log.d("FirebaseFlag", "📭 フラグがnullです（無視, カウント: $nullCount）")
                        lastValidValue?.let { onValueChanged(it) }  // 直前の値を再送
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseFlag", "⚠️ Flag監視エラー: ${error.message}")
            }
        }

        flagRef.addValueEventListener(flagListener!!)
    }

    fun startFlagTimeoutMonitor() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastFlagUpdateTime > 3000) {
                    Log.w("FirebaseFlag", "🚨 3秒以上更新なし。0を書き込みます")
                    writeFlagValue(0)
                    lastFlagUpdateTime = currentTime // これで無限書き込み防止
                }
                delay(1000)
            }
        }
    }

    fun writeFlagValue(value: Int) {
        val flagRef = FirebaseDatabase.getInstance().getReference("Flag")

        flagRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 既存データを削除
                snapshot.children.forEach { it.ref.removeValue() }

                // 1つだけ新しい値を書き込む
                flagRef.push().setValue(value)
                    .addOnSuccessListener {
                        Log.d("FirebaseFlag", "✅ Flagに$value を書き込みました")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseFlag", "⚠️ 書き込みエラー: ${e.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseFlag", "⚠️ Firebaseアクセスエラー: ${error.message}")
            }
        })
    }

}

fun observeFlightData(
    onResult: (List<FlightData>) -> Unit,
    onError: (String) -> Unit = {}
): ValueEventListener {
    val database = FirebaseDatabase.getInstance()
    val flightRef = database.getReference("Flight")

    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val flightList = mutableListOf<FlightData>()
            for (child in snapshot.children) {
                val data = child.getValue(FlightData::class.java)
                data?.let { flightList.add(it) }
            }
            onResult(flightList)
        }

        override fun onCancelled(error: DatabaseError) {
            onError(error.message)
        }
    }

    flightRef.addValueEventListener(listener)
    return listener  // 後で removeListener できるように返しておく
}


