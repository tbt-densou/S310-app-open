# 製作概要

TeamBirdmanTrial（**TBT**）S310 暁 機体 電装設計 [norahshion](https://github.com/norahshion) です。
本リポジトリは、**TBT S310 電装班**で開発した機体計測・通信システムの一般公開用です。

本システムの目的は、飛行中のパイロットに機体状態をリアルタイムで可視化し、安全かつ効率的な飛行を支援することです。

## 🚀 システムコンセプト

本システムは、各計測基板に搭載された**ESP32**がセンサー取得および通信制御を担当し、Androidアプリを通じて**リアルタイム表示**と**データ保存**を行う構成となっています。
<!--
もっとわかりやすく書く？
ESP32で制御している旨が伝わりにくいかも
-->

1.  **データ集約**: テールビーム（TB）基板とメイン基板で計測した高度・機速・姿勢角・舵角情報を、**BLE通信**でコックピット内のメイン基板に集約します。
2.  **リアルタイム表示**: メイン基板から、**BluetoothClassic通信**を用いて前部パイロットのAndroid端末にデータを送信し、画面にリアルタイムで表示します。
3.  **フライト記録と制御**:
    * データは**Firebase RealtimeDatabase**の一時ディレクトリに常時保存されます。
    * 後部パイロットの端末からフライトの**開始/停止フラグ**をFirebase経由で制御します。
    * フラグが「開始」になると、前部パイロットの端末が**すべてのデータを最終保存ディレクトリとスプレッドシートに記録**します。

より詳細な通信プロトコルやFirebaseのデータ構造については、[AndroidアプリのデータフローとFirebase連携](./docs/data_flow.md)を参照してください。

---

### 主な使用技術

| 技術 | 用途 |
|------|------|
| ![Arduino](https://img.shields.io/badge/-Arduino-00979D?logo=arduino&logoColor=white) | ファームウェア開発 |
| ![Android Studio](https://img.shields.io/badge/-Android%20Studio-3DDC84?logo=android-studio&logoColor=white) | Androidアプリ開発 |
| ![Kotlin](https://img.shields.io/badge/-Kotlin-7F52FF?logo=kotlin&logoColor=white) | アプリ言語 |
| ![Firebase](https://img.shields.io/badge/-Firebase-FFCA28?logo=firebase&logoColor=black) | クラウドデータ保存 |
<!--TODOアーキテクチャも加えるか検討-->

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
<!--TODO動作動画も加えるか検討-->

---

## 📘 詳細ドキュメント

システムの設計・仕様に関する詳細は、以下のドキュメントを参照してください。

* **システム構成**: [全体構成図、システムアーキテクチャ](./docs/architecture.md)
* **データ連携**: [AndroidアプリのデータフローとFirebase連携](./docs/data_flow.md)
* **ハードウェア仕様**: [ハードウェア設計・実装構成](./docs/hardware_spec.md)
* **開発ガイド**: [開発環境構築・ビルド・運用ルール](./docs/development_guide.md)
<!--
TODO書くべきことがほかにないか検討
開発ガイド、ファームウェアだけでなく、APIキーのことや、レポジトリへの追記方法も書きたい
-->

---

## 👥 開発メンバー

| 名前 / GitHub ID | 担当分野 | 主な役割 |
|------------------|-----------|-----------|
| [norahshion](https://github.com/norahshion) | システム全体設計・Android | 原案設計、ESP32通信制御、Androidバックエンド開発、Firebase連携設計 |
| [echo125338](https://github.com/echo125338) | Android | Androidフロントエンド開発、UI設計 |
| [tttt10231023](https://github.com/tttt10231023) | ファームウェア | 測定機器の製作、基板および基板BOXの設計・製作 |
| 他6名 | ファームウェア | GitHubアカウント確認後に追記予定 |
<!--TODO希望者がいたらメンバー追加-->

> ※ 本プロジェクトは TeamBirdmanTrial S310 電装班によって共同開発されました。

S310電装班の活動紹介は[製作実績.pdf](https://drive.google.com/file/d/1oXdjaeTpDY14w4dTRbWsmmrn15XHOLD9/view?usp=sharing)をご参照ください。
