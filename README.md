# 製作概要

TeamBirdmanTrial（**TBT**）S310 電装設計の [norahshion](https://github.com/norahshion) です。  
本リポジトリは、S310 電装班で製作したシステムの一般公開用です。  

本システムでは **ESP32** をメイン基板に採用し、テールビーム基板との **BLE通信**、および **Android端末とのBT Classic通信** を用いて、各種データの計測・表示・保存を行います。

---

### 主な使用技術

| 技術 | 用途 |
|------|------|
| ![Arduino](https://img.shields.io/badge/-Arduino-00979D?logo=arduino&logoColor=white) | ファームウェア開発 |
| ![Android Studio](https://img.shields.io/badge/-Android%20Studio-3DDC84?logo=android-studio&logoColor=white) | Androidアプリ開発 |
| ![Kotlin](https://img.shields.io/badge/-Kotlin-7F52FF?logo=kotlin&logoColor=white) | アプリ言語 |
| ![Firebase](https://img.shields.io/badge/-Firebase-FFCA28?logo=firebase&logoColor=black) | クラウドデータ保存 |


---

### 計測機器

| 計測項目 | センサー | 通信方式 | 設置場所 | 備考 |
|----------|----------|----------|----------|------|
| 高度 | [MB1242-000](https://akizukidenshi.com/catalog/g/g114709/) | I2C | メイン基板 | 測定範囲 20cm〜765cm |
| 機速 | [ピトー管](https://amzn.asia/d/ecEdyK8) + [SDP810-500Pa](https://www.mouser.jp/ProductDetail/Sensirion/SDP810-500PA) | I2C | テールビーム | 差圧方式 |
| 姿勢角 | [BNO080](https://www.amazon.co.jp/dp/B0DM98Q67M) | I2C | テールビーム | 9軸センサ |
| 舵角 | [CJMCU-103](https://amzn.asia/d/fbYAGlM) | アナログ | TB ER部 | 角度検出 |

---

### システム概要

### 全体構成図（ESP32 ⇔ Android）

```mermaid
graph TD
  subgraph "テールビーム TB 基板"
      A["機速 計測"] --> B{"BLE 送信"};
      C["姿勢角 計測"] --> B;
      D["舵角 計測"] --> B;
  end

  subgraph "メイン基板"
      E["高度 計測"] --> F{"BT Classic 送信"};
      B --> F;
  end

  F --> G["Android アプリ"];
  メイン基板 --> H["LCD1602 表示（有線）"];

  style A fill:#f9f,stroke:#333,stroke-width:2px,color:#000;
  style C fill:#f9f,stroke:#333,stroke-width:2px,color:#000;
  style D fill:#f9f,stroke:#333,stroke-width:2px,color:#000;
  style E fill:#ccf,stroke:#333,stroke-width:2px,color:#000;
  style G fill:#bbf,stroke:#333,stroke-width:2px,color:#000;
  style H fill:#ffc,stroke:#333,stroke-width:2px,color:#000;

  style B fill:#afa,stroke:#333,stroke-width:2px,color:#000;
  style F fill:#afa,stroke:#333,stroke-width:2px,color:#000;

```
---
### Androidアプリ  
前部パイロット用

```mermaid
flowchart TD
    A[ESP32] -->|BTClassic| B["Android (前部パイロット)"]

    subgraph Firebase
        C1["一時保存ディレクトリ"]
        C2["フラグディレクトリ"]
        C3["最終保存ディレクトリ"]
    end

    B -->|全データ保存| C1
    C2 -->|参照| B
    B -->|フラグがTrueなら保存| C3
    B -->|フラグTrueなら保存| D["GAS (スプレッドシート)"]
```

後部パイロット用

```mermaid
flowchart LR
    subgraph Firebase
        A["一時保存ディレクトリ"]
        B["フラグディレクトリ"]
    end

    C["Android(後部パイロット)"]

    A -->|受信、表示| C
    C -->|フラグを保存| B


```
---
# ESP32 実装・配置
- メイン基板：コックピット下部
- TB基板：ER付近  

基板製作、マイコン主担当：[tttt10231023](https://github.com/tttt10231023)<br><br>

|コックピット内機器設置場所|
|---|
|<img src="https://github.com/user-attachments/assets/c1e43768-2d11-4781-8030-5a3d245f5ef2" alt="コックピット" width="600">|

<br>実際の設置場所
| | |
|---|---|
|メイン基板|<img src="https://github.com/user-attachments/assets/c9511cec-04ee-4d2e-a01b-62e47ae4a66d" alt="メイン基板設置場所" width="600">|
|TB基板|<img src="https://github.com/user-attachments/assets/d9f75060-6250-4b48-b116-e1941ae798e5" alt="TB基板設置場所" width="600">|

<br>基盤と基板BOX
| |メイン基板|TB基板|
|---|---|---|
|基板|<img src="https://github.com/user-attachments/assets/06a95b7e-0ac5-4e8b-862f-901a45c4e0d8" alt="メイン基板" width="600">|<img src="https://github.com/user-attachments/assets/cd2abbba-d2d1-4de2-a08e-eb71223fb60e" alt="TB基板" width="600">|
|基板BOX|<img src="https://github.com/user-attachments/assets/55e7e6a4-a767-4f23-bd4b-c7771f573f33" alt="メイン基板BOX" width="600">|<img src="https://github.com/user-attachments/assets/878c554c-92f4-4891-8b04-863a76adb4ec" alt="TB基板BOX" width="600">|


