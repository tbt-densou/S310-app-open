package com.example.frontp.screens

import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.frontp.ui.theme.FrontPTheme
import com.example.frontp.viewmodel.DeviceViewModel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation // PasswordVisualTransformation のインポートを追加
import androidx.compose.runtime.collectAsState // collectAsState のインポートを追加
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.StateFlow // StateFlow のインポートを確認
import kotlinx.coroutines.delay
import android.content.Context
import androidx.compose.ui.platform.LocalContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupScreen(
    viewModel: DeviceViewModel,
    onClose: () -> Unit
) {
    val TAG = "SimpleFunctionCallerUI"
    val resultText by viewModel.resultText.collectAsState() // 処理結果のメッセージ
    val errorMessage by viewModel.errorMessage.observeAsState(null) // 初期値を指定 (null)

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var registrationCode by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val loggedInEmail by viewModel.loggedInEmail.collectAsState()

    // パスワードリセットポップアップの表示状態
    var showResetPassPopup by remember { mutableStateOf(false) }
    var showAccountSetting by remember { mutableStateOf(false) }
    var showMacSetting by remember { mutableStateOf(false) }
    var showDeleteFirebase by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp)
            .clickable { keyboardController?.hide() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ログイン/新規登録",
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                viewModel.clearAll()
                onClose()
            }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "閉じる")
            }
        }
        // ログイン中のユーザーがいればメールアドレスを表示
        loggedInEmail?.let { email ->
            Text(
                text = "ログイン中: $email",
                color = Color.Green,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (loggedInEmail == null) {
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("メールアドレス") },
                modifier = Modifier.fillMaxWidth() // TextFieldの幅を広げる
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("パスワード") },
                visualTransformation = PasswordVisualTransformation(), // パスワードを隠す
                modifier = Modifier.fillMaxWidth() // TextFieldの幅を広げる
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = registrationCode,
                onValueChange = { registrationCode = it },
                label = { Text("登録コード") },
                modifier = Modifier.fillMaxWidth() // TextFieldの幅を広げる
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val enteredEmail = email
                    val enteredPassword = password

                    if (enteredEmail.isNotEmpty() && enteredPassword.isNotEmpty()) {
                        viewModel.signInWithEmailAndPassword(enteredEmail, enteredPassword)
                    } else {
                        viewModel.setErrorMessage("メールアドレスとパスワードを入力してください。")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ログイン")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val enteredEmail = email
                    val enteredPassword = password
                    val enteredCode = registrationCode

                    Log.d(TAG, "--- New Registration Attempt ---")
                    Log.d(TAG, "Email: \"$enteredEmail\" (Length: ${enteredEmail.length})")
                    Log.d(TAG, "Password: \"$enteredPassword\" (Length: ${enteredPassword.length})")
                    Log.d(TAG, "Code: \"$enteredCode\" (Length: ${enteredCode.length})")
                    Log.d(TAG, "Email Empty: ${enteredEmail.isEmpty()}, Password Empty: ${enteredPassword.isEmpty()}, Code Empty: ${enteredCode.isEmpty()}")

                    if (enteredEmail.isNotEmpty() && enteredPassword.isNotEmpty() && enteredCode.isNotEmpty()) {
                        // ViewModelに新規登録用の関数がある場合、それを呼び出す
                        viewModel.callHelloWorldWithName(enteredEmail, enteredPassword, enteredCode)
                        // 成功時はViewModel内でclearErrorMessage()が呼ばれる想定
                    } else {
                        viewModel.setErrorMessage("すべての項目を入力してください。")
                        Log.d(TAG, "Validation failed: Fields are empty.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("新規登録")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    showResetPassPopup = true
                },
                modifier = Modifier.fillMaxWidth()
            ){
                Text("パスワードをお忘れの場合")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    showMacSetting = true
                },
                modifier = Modifier.fillMaxWidth()
            ){
                Text("接続するデバイスの変更")
            }
        } else {
            Button(
                onClick = {
                    viewModel.signOut()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ログアウト")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.clearAll()
                    showAccountSetting = true
                },
                modifier = Modifier.fillMaxWidth()
            ){
                Text("アカウント設定")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    showMacSetting = true
                },
                modifier = Modifier.fillMaxWidth()
            ){
                Text("接続するデバイスの変更")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                showDeleteFirebase = true
            },
            modifier = Modifier.fillMaxWidth()
        ){
            Text("Firebaseデータ削除")
        }

        // ★エラーメッセージ表示の修正★
        // errorMessageがnullではない場合のみ表示
        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.Red, // エラーメッセージは赤色
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
        // ★結果メッセージ表示の修正★
        // resultTextがデフォルトのメッセージ（"結果がここに表示されます。"）でない場合のみ表示、かつ色をデフォルト（または緑など）に
        if (resultText != "結果がここに表示されます。") {
            Text(
                text = resultText,
                // エラーメッセージと区別するために色をデフォルト（または緑など）にする
                color = MaterialTheme.colorScheme.onSurface, // 例: デフォルトのテキスト色
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }

    // showResetPassPopupがtrueの場合のみResetPassポップアップを表示
    if (showResetPassPopup) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearAll()
                onClose()
            }, // ポップアップ外をタップで閉じる
            text = {
                ResetPass(
                    viewModel = viewModel,
                    onClose = { showResetPassPopup = false } // ResetPass内の閉じるボタンで状態をfalseに
                )
            },
            confirmButton = {
                // confirmButtonは必要なければ空にしてもOK
            }
        )
    }

    if (showAccountSetting) {
        AccountSetting(
            viewModel = viewModel,
            onClose = { showAccountSetting = false }
        )
    }
    if (showMacSetting) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearAll()
                onClose()
            }, // ポップアップ外をタップで閉じる
            text = {
                MacSetting(
                    viewModel = viewModel,
                    onClose = { showMacSetting = false } // MacSetting内の閉じるボタンで状態をfalseに
                )
            },
            confirmButton = {
                // confirmButtonは必要なければ空にしてもOK
            }
        )
    }

    if (showDeleteFirebase) {
        AlertDialog(
            onDismissRequest = {
                viewModel.clearAll()
                onClose()
            }, // ポップアップ外をタップで閉じる
            text = {
                FirebaseDelete(
                    viewModel = viewModel,
                    onClose = { showDeleteFirebase = false } // MacSetting内の閉じるボタンで状態をfalseに
                )
            },
            confirmButton = {
                // confirmButtonは必要なければ空にしてもOK
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSetting(viewModel: DeviceViewModel, onClose: () -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentEmail = currentUser?.email ?: "不明なユーザー"

    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var step by remember { mutableStateOf("reauth") }
    val resultText by viewModel.resultText.collectAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()
    var cloased by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "アカウント管理",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    viewModel.clearAll()
                    onClose()
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "閉じる")
                }

            }
        },
        text = {
            Column {
                if (step == "reauth") {
                    Text("登録済みのメールアドレスとパスワードで再認証してください")
                    Text("メール: $currentEmail", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("パスワード") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                } else if (step == "change") {
                    Text("新しいパスワードを入力してください")
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("新しいパスワード") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                } else if (step == "delete") {
                    Text("アカウントを本当に削除しますか？この操作は取り消せません。")
                }

                if (resultText.isNotBlank()) Text("✅ $resultText", color = Color.Green)
                if (errorMessage?.isNotBlank() == true) Text("⚠️ $errorMessage", color = Color.Red)
            }
        },
        confirmButton = {
            when (step) {
                "reauth" -> {
                    Column {
                        Button(onClick = {
                            if (password.isNotEmpty()) {
                                viewModel.reauthenticateAndRun(
                                    password,
                                    onSuccess = { step = "change" },
                                    onFailure = { viewModel.setErrorMessage(it) }
                                )
                            } else {
                                viewModel.setErrorMessage("パスワードを入力してください。")
                            }
                        },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("パスワード変更")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            if (password.isNotEmpty()) {
                                viewModel.reauthenticateAndRun(
                                    password,
                                    onSuccess = { step = "delete" },
                                    onFailure = { viewModel.setErrorMessage(it) }
                                )
                            } else {
                                viewModel.setErrorMessage("パスワードを入力してください。")
                            }
                        },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("アカウント削除")
                        }
                    }
                }
                "change" -> {
                    Button(onClick = {
                        viewModel.changePassword(newPassword) { success, error ->
                            if (success) {
                                viewModel.setResult("パスワードを変更しました")
                                cloased = true
                            } else {
                                viewModel.setErrorMessage(error ?: "エラー")
                            }
                        }
                    },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("変更する")
                    }
                }
                "delete" -> {
                    Button(onClick = {
                        viewModel.deleteUser { success, error ->
                            if (success) {
                                viewModel.setResult("アカウントを削除しました")
                                cloased = true
                            } else {
                                viewModel.setErrorMessage(error ?: "エラー")
                            }
                        }
                    },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("削除する")
                    }
                }
            }
        }
    )

    if (cloased) {
        LaunchedEffect(Unit) {
            viewModel.clearAll()
            onClose()
            cloased = false
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PopupScreenPreview() {
    FrontPTheme {
        PopupScreen(viewModel = DeviceViewModel(), onClose = {})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPass(
    viewModel: DeviceViewModel,
    onClose: () -> Unit // ポップアップを閉じるためのコールバック
) {
    var email by remember { mutableStateOf("") } // パスワードリセット用のメールアドレス
    var cloased by remember { mutableStateOf(false) } // ポップアップを閉じるためのフラグ
    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "パスワード再登録",
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                viewModel.clearAll()
                onClose()
            }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "閉じる")
            }
        }
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("登録済みメールアドレス") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                // ここでパスワードリセットのロジックを呼び出す
                // 例: viewModel.sendPasswordResetEmail(email)
                // ViewModelに実装されていない場合は、FirebaseAuthなどのAPIを直接利用
                if (email.isEmpty()) {
                    viewModel.setResetError("メールアドレスを入力してください。")
                } else {
                    viewModel.sendPasswordResetEmailWithCheck(email)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("メールを送信")
        }
        // ViewModelからのエラーメッセージ表示
        val errorMessage by viewModel.resetError.observeAsState(null)
        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        // ViewModelからの結果メッセージ表示 (成功時など)
        val resultText by viewModel.resetResult.collectAsState()
        if (resultText != "") {
            Text(
                text = resultText,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            LaunchedEffect(resultText) {
                delay(3000)
                if (resultText == "パスワードリセットメールを送信しました。") {
                    cloased = true
                }
            }
        }
    }
    if (cloased) {
        LaunchedEffect(Unit) {
            viewModel.clearAll()
            onClose()
            cloased = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacSetting(
    viewModel: DeviceViewModel,
    onClose: () -> Unit // ポップアップを閉じるためのコールバック
) {
    val mac1 = "78:42:1C:2D:15:66"
    val mac2 = "78:42:1C:2D:3C:FA"
    val mac3 = "EC:C9:FF:0F:80:FA"
    val mac4 = "78:42:1C:2E:E1:E2"
    var mac5 by remember { mutableStateOf("") }
    var cloased by remember { mutableStateOf(false) } // ポップアップを閉じるためのフラグ

    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "接続するデバイス",
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                viewModel.clearAll()
                onClose()
            }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "閉じる")
            }
        }

        Button(
            onClick = {
                viewModel.changeMac(mac1)
            },
            // ボタンの幅はコンテンツに合わせて調整されるため、weightは不要な場合が多い
            // 必要に応じてmodifierで固定幅を指定することも可能
        ) {
            Text("メイン $mac1")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.changeMac(mac2)
            },
            // ボタンの幅はコンテンツに合わせて調整されるため、weightは不要な場合が多い
            // 必要に応じてmodifierで固定幅を指定することも可能
        ) {
            Text("サブ $mac2")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.changeMac(mac4)
            },
            // ボタンの幅はコンテンツに合わせて調整されるため、weightは不要な場合が多い
            // 必要に応じてmodifierで固定幅を指定することも可能
        ) {
            Text("サブ2 $mac4")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.changeMac(mac3)
            },
            // ボタンの幅はコンテンツに合わせて調整されるため、weightは不要な場合が多い
            // 必要に応じてmodifierで固定幅を指定することも可能
        ) {
            Text("デバック $mac3")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(), // 親要素の幅いっぱいに広げる
            verticalAlignment = Alignment.CenterVertically // 垂直方向の中央揃え
        ) {
            TextField(
                value = mac5,
                onValueChange = { mac5 = it },
                label = { Text("接続するデバイス") },
                modifier = Modifier.weight(1f) // 利用可能なスペースを均等に割り当てる
            )
            Spacer(modifier = Modifier.width(16.dp)) // TextFieldとButtonの間にスペースを追加
            Button(
                onClick = {
                    if (mac5.isEmpty()) {
                        viewModel.setMacError("MACアドレスが入力されていません")
                    } else {
                        viewModel.changeMac(mac5)
                    }
                },
                // ボタンの幅はコンテンツに合わせて調整されるため、weightは不要な場合が多い
                // 必要に応じてmodifierで固定幅を指定することも可能
            ) {
                Text("→")
            }
        }
        // ViewModelからのエラーメッセージ表示
        val errorMessage by viewModel.macError.observeAsState(null)
        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        // ViewModelからの結果メッセージ表示 (成功時など)
        val macText by viewModel.macResult.collectAsState()
        if (macText != "") {
            Text(
                text = macText,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        val mac by viewModel.macAdd.collectAsState()
        Text(
            text = "現在のデバイス $mac",
        )
    }
    if (cloased) {
        LaunchedEffect(Unit) {
            viewModel.clearAll()
            onClose()
            cloased = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseDelete(
    viewModel: DeviceViewModel,
    onClose: () -> Unit // ポップアップを閉じるためのコールバック
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val spinnerItems = listOf("デバック", "全体接合", "1st走行", "2nd走行", "1stTF", "2ndTF", "3rdTF", "4thTF", "5thTF", "6thTF", "7thTF", "最終TF", "")
    var currentSelectedItem by remember { mutableStateOf(spinnerItems.firstOrNull() ?: "") }
    var test by remember { mutableStateOf("") }
    var cloased by remember { mutableStateOf(false) } // ポップアップを閉じるためのフラグ
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "削除する階層",
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                viewModel.clearAll()
                onClose()
            }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "閉じる")
            }
        }

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
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(), // 親要素の幅いっぱいに広げる
            verticalAlignment = Alignment.CenterVertically // 垂直方向の中央揃え
        ) {
            TextField(
                value = test,
                onValueChange = { test = it },
                label = { Text("削除する階層") },
                modifier = Modifier.weight(1f) // 利用可能なスペースを均等に割り当てる
            )
            Spacer(modifier = Modifier.width(16.dp)) // TextFieldとButtonの間にスペースを追加
            Button(
                onClick = {
                    if (test.isEmpty()) {
                        viewModel.deleteDirectory(currentSelectedItem)
                    } else {
                        viewModel.deleteDirectory(test)
                    }
                },
                // ボタンの幅はコンテンツに合わせて調整されるため、weightは不要な場合が多い
                // 必要に応じてmodifierで固定幅を指定することも可能
            ) {
                Text("→")
            }
        }
        // ViewModelからのエラーメッセージ表示
        val errorMessage by viewModel.delError.observeAsState(null)
        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        // ViewModelからの結果メッセージ表示 (成功時など)
        val delText by viewModel.delResult.collectAsState()
        if (delText != "") {
            Text(
                text = delText,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        if(test.isEmpty()){
            Text(
                text = "削除する階層 $currentSelectedItem",
            )
        } else {
            Text(
                text = "削除する階層 $test",
            )
        }

    }
    if (cloased) {
        LaunchedEffect(Unit) {
            viewModel.clearAll()
            onClose()
            cloased = false
        }
    }
}


