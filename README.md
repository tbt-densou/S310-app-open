# 製作概要

TeamBirdmanTrial（**TBT**）S310 電装設計の [norahshion](https://github.com/norahshion) です。
本リポジトリは、S310 電装班で製作したシステムの一般公開用です。

本システムの最大目標は、飛行中のパイロットに機体の状態をリアルタイムに伝えることです。

## システムコンセプト

このシステムは、**ESP32**を用いた複数の計測基板からデータを集約し、Androidアプリを通じて**リアルタイム表示**と**データ保存**を行うことを核としています。

1.  **データ集約**: テールビーム（TB）基板とメイン基板で計測した高度・機速・姿勢角・舵角情報を、**BLE通信**でコックピット内のメイン基板に集約します。
2.  **リアルタイム表示**: メイン基板から、**BT Classic通信**を用いて前部パイロットのAndroid端末にデータを送信し、画面にリアルタイムで表示します。
3.  **フライト記録と制御**:
    * データは**Firebase RealtimeDatabase**の一時ディレクトリに常時保存されます。
    * 後部パイロットの端末からフライトの**開始/停止フラグ**をFirebase経由で制御します。
    * フラグが「開始」になると、前部パイロットの端末が**すべてのデータを最終保存ディレクトリとスプレッドシートに記録**します。

より詳細な通信プロトコルやFirebaseのデータ構造については、[データ連携ドキュメント](./docs/data_flow.md)を参照してください。

---

### 主な使用技術

| 技術 | 用途 |
|------|------|
| ![Arduino](https://img.shields.io/badge/-Arduino-00979D?logo=arduino&logoColor=white) | ファームウェア開発 |
| ![Android Studio](https://img.shields.io/badge/-Android%20Studio-3DDC84?logo=android-studio&logoColor=white) | Androidアプリ開発 |
| ![Kotlin](https://img.shields.io/badge/-Kotlin-7F52FF?logo=kotlin&logoColor=white) | アプリ言語 |
| ![Firebase](https://img.shields.io/badge/-Firebase-FFCA28?logo=firebase&logoColor=black) | クラウドデータ保存 |


---
## 📁 本プロジェクトのディレクトリ構成と役割

本リポジトリは、以下の表に示す主要なコンポーネント（ディレクトリ）で構成されています。各コンポーネントの役割と詳細を記します。

| ディレクトリ | 役割 | 詳細 |
| :--- | :--- | :--- |
| **`esp32/AKATUKI_MAIN.ino`** | **ESP32ファームウェア** | メイン基板の制御プログラム |
| **`esp32/AKATUKI_TB.ino`** | **ESP32ファームウェア** | TB基板の制御プログラム |
| **`androidApps/FrontP/`** | **Androidアプリ** | 前部パイロット用アプリ |
| **`androidApps/BackP/`** | **Androidアプリ** | 後部パイロット用アプリ |
| **`firebaseFunctions/services_helloworld_1748496253.231000/`** | **Firebase Functions** | 認証コードのチェック機能 |
| **`firebaseFunctions/services_sendpasswordresetifuserexists_1748518185.584000/`** | **Firebase Functions** | 登録状態のチェック機能 |
| **`spredsheetGas/saveData.gs`** | **データ保存スクリプト (GAS)** | Googleスプレッドシートへのデータ保存処理 |
| **`docs/`** | **ドキュメント (Markdown)** | 本プロジェクトの詳細ドキュメント |

---

## 📘 詳細ドキュメント

システムの設計・仕様に関する詳細は、以下のドキュメントを参照してください。

* **システム構成**: [全体構成図、システムアーキテクチャ](./docs/architecture.md)
* **データ連携**: [AndroidアプリのデータフローとFirebase連携](./docs/data_flow.md)
* **ハードウェア仕様**: [計測機器一覧と詳細仕様](./docs/hardware_spec.md)
* **実装・配置**: [機体への機器設置場所、基板写真](./docs/deployment.md)
* **開発ガイド**: [ファームウェアのビルド・書き込み手順](./docs/firmware_guide.md)
