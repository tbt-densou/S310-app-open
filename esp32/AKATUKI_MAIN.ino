#include <Wire.h>
#include <ICM20948_WE.h>
#include <LiquidCrystal_I2C.h>
#include <NimBLEDevice.h>

#define ICM20948_ADDR 0x68
#define BUTTON_PIN 18  // スイッチ1姿勢各初期化用
#define HeightAddress 0x70
#define RangeCommand 0x51
#define LcdAddress 0x27
#define PERIPHERAL_1_ADDR "AC:15:18:E9:72:92" // ペリフェラル1のMACアドレス
#define SERVICE_UUID      "12345678-1234-1234-1234-123456789abc"  // サービスUUID
#define CHARACTERISTIC_UUID "abcdefab-cdef-abcd-efab-cdef12345678" // キャラクタリスティックUUID

//LCD
LiquidCrystal_I2C lcd(0x27, 16, 2);

//Bluetooth
// NimBLEClientのポインタ
NimBLEClient* pClient1 = nullptr; // ペリフェラル1用のクライアント

// 接続状態を管理するフラグ
bool connected1 = false;  // ペリフェラル1に接続しているか
bool reconnecting1 = false; // ペリフェラル1再接続中か

// ペリフェラルのアドレスを指定
// NimBLEAddressを修正
NimBLEAddress peripheralAddress1(std::string(PERIPHERAL_1_ADDR), 0);

// NimBLEClientCallbacksクラスを拡張して、接続・切断時の動作を定義
class MyClientCallback : public NimBLEClientCallbacks {
  void onConnect(NimBLEClient* pClient) {
    Serial.println("Client connected"); // 接続成功のメッセージを表示
    // 接続したクライアントに応じて接続状態を更新
    if (pClient == pClient1) {
      connected1 = true; // ペリフェラル1が接続
      reconnecting1 = false; // 再接続フラグをリセット
    }
  }

  void onDisconnect(NimBLEClient* pClient) {
    Serial.println("Client disconnected"); // 切断時のメッセージを表示
    // 切断したクライアントに応じて接続状態を更新
    if (pClient == pClient1) {
      connected1 = false; // ペリフェラル1が切断
      pClient1->disconnect(); // 切断処理
      pClient1 = nullptr;  // クライアントを解放
    }
  }
};

// ペリフェラル1に接続する関数
void connectToPeripheral1() {
  // pClient1がnullptrのときだけ新たにクライアントを作成
  if (pClient1 == nullptr) {
    pClient1 = NimBLEDevice::createClient(); // 新しいクライアントを作成
    pClient1->setClientCallbacks(new MyClientCallback(), false); // コールバックを設定
  }

  Serial.print("Connecting to Peripheral 1 at ");
  Serial.println(PERIPHERAL_1_ADDR); // 接続先のアドレスを表示

  // 接続を試みる
  if (pClient1->connect(peripheralAddress1)) {
    Serial.println("Connected to Peripheral 1"); // 接続成功のメッセージを表示
    connected1 = true; // 接続状態を更新
    reconnecting1 = false; // 再接続フラグをリセット
    readDataFromPeripheral(pClient1, "Peripheral 1"); // 接続後にデータを読み取る
  } else {
    Serial.println("Failed to connect to Peripheral 1."); // 接続失敗のメッセージを表示
    vTaskDelay(500 / portTICK_PERIOD_MS); // 500ms待機
  }
}

// ペリフェラルからデータを読み取る関数
String readDataFromPeripheral(NimBLEClient* pClient, const char* peripheralName) {
  NimBLERemoteService* pService = pClient->getService(SERVICE_UUID); // サービスを取得
  if (pService) {
    NimBLERemoteCharacteristic* pCharacteristic = pService->getCharacteristic(CHARACTERISTIC_UUID); // キャラクタリスティックを取得
    if (pCharacteristic) {
      String value = pCharacteristic->readValue(); // 値を読み取る
      return value.c_str();
    }
  }
}

// ペリフェラル1の再接続タスク
void reconnectTask1(void* parameter) {
  while (!connected1) { // 接続されるまでループ
    Serial.println("connecting");
    connectToPeripheral1(); // 接続を試みる
    vTaskDelay(500 / portTICK_PERIOD_MS); // 500msごとに再接続を試行
  }
  reconnecting1 = false; // 再接続が成功したのでフラグをリセット
  vTaskDelete(NULL); // タスクを削除
}

//姿勢角
ICM20948_WE myIMU = ICM20948_WE(ICM20948_ADDR);

float roll = 0.0, pitch = 0.0, yaw = 0.0;
unsigned long lastUpdateTime = 0;
const float alpha = 0.85; // Complementary filter rate（少し小さくして応答を速くする）
bool imuReady = true;  // 初期状態では正常と仮定

//初期位置
float initialRoll = 0.0, initialPitch = 0.0;

// --- 磁気センサキャリブレーション ---
float magMinX =  1000, magMaxX = -1000;
float magMinY =  1000, magMaxY = -1000;
bool calibrateMag = false;  // キャリブレーションフラグ

// --- 平均用バッファ ---
const int avgWindow = 5;
float accXBuf[avgWindow] = {0}, accYBuf[avgWindow] = {0}, accZBuf[avgWindow] = {0};
int avgIndex = 0;

bool waitForIMU(uint8_t address, uint16_t timeoutMs) {
  uint32_t start = millis();
  while (millis() - start < timeoutMs) {
    Wire.beginTransmission(address);
    if (Wire.endTransmission() == 0) {
      return true; // 応答あり
    }
    delay(100);
  }
  return false; // タイムアウト
}

void initializeIMU() {
  myIMU.setGyrRange(ICM20948_GYRO_RANGE_250);
  myIMU.setAccRange(ICM20948_ACC_RANGE_4G);
  myIMU.setGyrDLPF(ICM20948_DLPF_6);
  myIMU.setAccDLPF(ICM20948_DLPF_6);
  myIMU.initMagnetometer(); // 磁気センサ初期化
}


void resetIMU() {
  // センサ初期化処理
  myIMU.autoOffsets(); // センサのオフセットを再計算
  Serial.println("IMU Resetting...");
  delay(1000);
  
  // 初期角度を再設定
  myIMU.readSensor();
  xyzFloat accRaw;
  myIMU.getAccRawValues(&accRaw);
  accRaw.x = -accRaw.x;
  accRaw.y = -accRaw.y;
  accRaw.z = -accRaw.z;

  roll = atan2(accRaw.y, accRaw.z) * 180.0 / PI;
  pitch = atan2(-accRaw.x, sqrt(accRaw.y * accRaw.y + accRaw.z * accRaw.z)) * 180.0 / PI;
  yaw = 0.0;

  // 初期補正角度を再保存
  initialRoll = roll;
  initialPitch = pitch;
}

void setup() {
  //LCD
  lcd.init();
  lcd.noBacklight();
  delay(1000);

  //Bluetooth
  // BLEの初期化
  NimBLEDevice::init("ESP32_Central"); // デバイス名を設定
  NimBLEDevice::setPower(ESP_PWR_LVL_P9); // 出力電力を最大に設定

  // ペリフェラルへの接続を試みる
  connectToPeripheral1();

  //姿勢角
  Wire.begin(21, 22);
  Wire.setClock(100000);
  Serial.begin(115200);
  pinMode(BUTTON_PIN, INPUT_PULLUP);  // ボタンピンを入力プルアップに設定
  delay(500);

  lastUpdateTime = millis();

  myIMU.autoOffsets();
  myIMU.setGyrRange(ICM20948_GYRO_RANGE_250);
  myIMU.setAccRange(ICM20948_ACC_RANGE_4G);
  myIMU.setGyrDLPF(ICM20948_DLPF_6);
  myIMU.setAccDLPF(ICM20948_DLPF_6);
  myIMU.initMagnetometer(); // 磁気センサ初期化

  // 初期角度設定
  myIMU.readSensor();
  xyzFloat accRaw;
  myIMU.getAccRawValues(&accRaw);
  accRaw.x = -accRaw.x;
  accRaw.y = -accRaw.y;
  accRaw.z = -accRaw.z;

  roll = atan2(accRaw.y, accRaw.z) * 180.0 / PI;
  pitch = atan2(-accRaw.x, sqrt(accRaw.y * accRaw.y + accRaw.z * accRaw.z)) * 180.0 / PI;
  yaw = 0.0;

  // --- 初期補正角度を保存 ---
  initialRoll = roll;
  initialPitch = pitch;
}

int first = 1;

void loop() {
  //Bluetooth
  // ペリフェラル1からのデータ受信
  if (connected1 && pClient1->isConnected()) {
    if(first){
      delay(500);
      lcd.backlight();
      delay(500);
      first = 0;
    }
    String receivedData = readDataFromPeripheral(pClient1, "Peripheral 1"); // データを読み取る

    int e_index = receivedData.indexOf('e');
    int r_index = receivedData.indexOf('r');
    int s_index = receivedData.indexOf('s');

    if (e_index != -1 && r_index != -1 && s_index != -1) {
      int e_value = receivedData.substring(0, e_index).toInt();
      int r_value = receivedData.substring(e_index + 1, r_index).toInt();
      float s_value = receivedData.substring(r_index + 1, s_index).toFloat();

      // LCDに表示
      lcd.clear();
      lcd.setCursor(0, 0);
      lcd.print("E:");
      lcd.print(e_value < 0 ? "-" : "+");
      lcd.print(abs(e_value), 1);
      lcd.print(" P:");
      lcd.print(r_value < 0 ? "-" : "+"); 
      lcd.print(abs(r_value), 1);
      lcd.print(" deg");
      lcd.setCursor(0, 1);
      lcd.print("Speed:");
      lcd.print(s_value, 2);
      lcd.print(" m/s");

      Serial.print("E:");
      Serial.print(e_value);
      Serial.print("deg ");
      Serial.print("R:");
      Serial.print(r_value);
      Serial.print("deg ");
      Serial.print("Speed:");
      Serial.print(s_value);
      Serial.print("m/s ");
    }
      //高度
    Wire.beginTransmission(HeightAddress);
    Wire.write(RangeCommand);
    Wire.endTransmission();
    delay(100); // 測定待機
    Wire.requestFrom(HeightAddress, 2);
    int range = Wire.read() << 8 | Wire.read();
    Serial.print("Height: ");
    Serial.print(range);
    Serial.print("cm ");

    //姿勢角
    // ボタンが押されたらIMUをリセット
    if (digitalRead(BUTTON_PIN) == LOW) {  // ボタンが押された（LOW）場合
      resetIMU();
      while (digitalRead(BUTTON_PIN) == LOW); // ボタンが離されるまで待機
    }

    myIMU.readSensor();

    xyzFloat accRaw, gyrRaw, magRaw;
    myIMU.getAccRawValues(&accRaw);
    myIMU.getGyrValues(&gyrRaw);
    myIMU.getMagValues(&magRaw);

    unsigned long currentTime = millis();
    float dt = (currentTime - lastUpdateTime) / 1000.0;
    lastUpdateTime = currentTime;

    // 逆さま補正
    accRaw.x = -accRaw.x;
    accRaw.y = -accRaw.y;
    accRaw.z = -accRaw.z;
    gyrRaw.x = -gyrRaw.x;
    gyrRaw.y = -gyrRaw.y;
    gyrRaw.z = -gyrRaw.z;

    // --- 磁気センサキャリブレーション（回転しながら実行） ---
    if (calibrateMag) {
      magMinX = min(magMinX, magRaw.x);
      magMaxX = max(magMaxX, magRaw.x);
      magMinY = min(magMinY, magRaw.y);
      magMaxY = max(magMaxY, magRaw.y);
    }

    // ハードアイアン補正
    float magOffsetX = (magMaxX + magMinX) / 2.0;
    float magOffsetY = (magMaxY + magMinY) / 2.0;
    float correctedMagX = magRaw.x - magOffsetX;
    float correctedMagY = magRaw.y - magOffsetY;

    // --- 地磁気でYaw計算 ---
    float pitchRad = pitch * PI / 180.0;
    float rollRad = roll * PI / 180.0;

    // チルト補正付き磁気方位角の計算
    float xh = magRaw.x * cos(pitchRad) + magRaw.z * sin(pitchRad);
    float yh = magRaw.x * sin(rollRad) * sin(pitchRad) + magRaw.y * cos(rollRad) - magRaw.z * sin(rollRad) * cos(pitchRad);

    float heading = atan2(yh, xh) * 180.0 / PI;
    if (heading < 0) heading += 360;  // 方角が負の場合は360度を加算

    // --- 地磁気に基づくyawの更新 ---
    yaw = 0.90 * yaw + 0.10 * heading;

    // --- ロール・ピッチ計算 ---
    float accRoll = atan2(accRaw.y, accRaw.z) * 180.0 / PI;
    float accPitch = atan2(-accRaw.x, sqrt(accRaw.y * accRaw.y + accRaw.z * accRaw.z)) * 180.0 / PI;

    roll  = alpha * roll  + (1.0 - alpha) * accRoll;
    pitch = alpha * pitch + (1.0 - alpha) * accPitch;

    // --- 補正後の値を出力 ---
    float correctedRoll = roll - initialRoll;
    float correctedPitch = pitch - initialPitch;

    // --- 出力 ---
    Serial.print("Roll: ");
    Serial.print(correctedRoll);  // 補正後のロール角
    Serial.print("deg ");

    Serial.print("Pitch: ");
    Serial.print(correctedPitch); // 補正後のピッチ角
    Serial.print("deg ");

    Serial.print("Yaw: ");
    Serial.print(yaw); // 地磁気に基づくヨー角
    Serial.println("deg");
  }else{
    connected1 = false;
    lcd.noBacklight();
    first = 1;
  }
  // 接続状況のチェックと再接続処理
  if (!connected1 && !reconnecting1) {
    reconnecting1 = true; // 再接続フラグをセット
    xTaskCreate(reconnectTask1, "Reconnect1", 4096, NULL, 1, NULL); // 再接続タスクを作成
  }

}
