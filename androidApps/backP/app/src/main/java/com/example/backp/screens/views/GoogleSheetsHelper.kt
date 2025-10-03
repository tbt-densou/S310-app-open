package com.example.backp.screens.views

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

object FirebaseHelper {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val dataRef: DatabaseReference = database.getReference("sensorData")

    fun writeToFirebase(rowData: List<Any?>) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            if (rowData.any { it != null }) {
                val dataMap = mutableMapOf<String, Any?>()

                rowData.getOrNull(0)?.let { dataMap["sw1"] = it }
                rowData.getOrNull(1)?.let { dataMap["speed"] = it }
                rowData.getOrNull(2)?.let { dataMap["height"] = it }
                rowData.getOrNull(3)?.let { dataMap["rpm"] = it }
                rowData.getOrNull(4)?.let { dataMap["roll"] = it }
                rowData.getOrNull(5)?.let { dataMap["pitch"] = it }
                rowData.getOrNull(6)?.let { dataMap["yaw"] = it }
                rowData.getOrNull(7)?.let { dataMap["eAngle"] = it }
                rowData.getOrNull(8)?.let { dataMap["rAngle"] = it }
                rowData.getOrNull(9)?.let { dataMap["latitude"] = it }
                rowData.getOrNull(10)?.let { dataMap["longitude"] = it }

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

                rowData.getOrNull(0)?.let { dataMap["sw1"] = it }
                rowData.getOrNull(1)?.let { dataMap["speed"] = it }
                rowData.getOrNull(2)?.let { dataMap["height"] = it }
                rowData.getOrNull(3)?.let { dataMap["rpm"] = it }
                rowData.getOrNull(4)?.let { dataMap["roll"] = it }
                rowData.getOrNull(5)?.let { dataMap["pitch"] = it }
                rowData.getOrNull(6)?.let { dataMap["yaw"] = it }
                rowData.getOrNull(7)?.let { dataMap["eAngle"] = it }
                rowData.getOrNull(8)?.let { dataMap["rAngle"] = it }
                rowData.getOrNull(9)?.let { dataMap["latitude"] = it }
                rowData.getOrNull(10)?.let { dataMap["longitude"] = it }

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
/*
            // 追加部分：Flightディレクトリに selectedValue と currentSelectedItem を保存（データは常に1つだけ）

            val flightDirRef = database.getReference("Flight")

            // 既存の全データを削除してから、新しいデータを追加
            flightDirRef.get().addOnSuccessListener { snapshot ->
                // 既存データを削除
                snapshot.children.forEach { it.ref.removeValue() }

                // 新しいデータの作成
                val flightData = mapOf(
                    "Flight" to selectedValue,
                    "TF" to currentSelectedItem
                )

                // ランダムキーで保存
                flightDirRef.push().setValue(flightData)
                    .addOnSuccessListener {
                        Log.d("FirebaseHelper", "✅ Flightディレクトリにデータ保存成功")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseHelper", "⚠️ Flight保存失敗: ${e.message}")
                    }
            }.addOnFailureListener { e ->
                Log.e("FirebaseHelper", "⚠️ Flightディレクトリの取得失敗: ${e.message}")
            }*/


        } else {
            Log.e("FirebaseHelper", "⚠️ ユーザーが認証されていません。")
        }
    }
}

object FirebaseFlag {
    fun writeFlagValue(
        value: Int,
        selectedValue: String?, // ← 追加
        currentSelectedItem: String? // ← 追加
    ) {
        val database = FirebaseDatabase.getInstance()

        val flagRef = database.getReference("Flag")
        val flightDirRef = database.getReference("Flight")

        // Flag の処理
        flagRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 既存の Flag データを削除
                snapshot.children.forEach { it.ref.removeValue() }

                // 新しい値を書き込む
                flagRef.push().setValue(value)
                    .addOnSuccessListener {
                        Log.d("FirebaseFlag", "✅ Flag に $value を書き込みました")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseFlag", "⚠️ Flag 書き込みエラー: ${e.message}")
                    }

                // Flight データもこの中で扱う（Flag 読み込み成功後）
                flightDirRef.get().addOnSuccessListener { flightSnapshot ->
                    // 既存データを削除
                    flightSnapshot.children.forEach { it.ref.removeValue() }

                    // 新しいデータを追加
                    val flightData = mapOf(
                        "Flight" to currentSelectedItem,
                        "TF" to selectedValue
                    )

                    flightDirRef.push().setValue(flightData)
                        .addOnSuccessListener {
                            Log.d("FirebaseFlag", "✅ Flight ディレクトリにデータ保存成功")
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseFlag", "⚠️ Flight 保存失敗: ${e.message}")
                        }

                }.addOnFailureListener { e ->
                    Log.e("FirebaseFlag", "⚠️ Flight ディレクトリ取得失敗: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseFlag", "⚠️ Firebase アクセスエラー: ${error.message}")
            }
        })
    }
}
