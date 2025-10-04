## 📄 目次

- [1. 設計思想と目標](#1-設計思想と目標)
- [2. 全体構成図（ESP32 ⇔ Android）](#2-全体構成図esp32--android)
- [3. 通信アーキテクチャとプロトコル](#3-通信アーキテクチャとプロトコル)
- [4. アプリケーションアーキテクチャ](#4-アプリケーションアーキテクチャ)
    - [4.1. Androidアプリ構造図（データフロー）](#41-androidアプリ構造図データフロー)
    - [4.2. Androidアーキテクチャ詳細（技術選定）](#42-androidアーキテクチャ詳細技術選定)
- [5. 設計上の課題と解決策](#5-設計上の課題と解決策)


# 1. 設計思想と目標


# 全体構成図（ESP32 ⇔ Android）

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
### Androidアプリ構造図
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
# Androidアーキテクチャ
|記述方法|
|---|
|MVVM|
|Jetpack Compose|

|使用バージョン|  |
|---|---|
|Android Gradle Plugin|8.4.1|
|Kotlin Gradle Plugin|2.2.0|
|Google Services Plugin|4.4.2|
|Kotlin Language/API|2.2.0|
|Compose BOM|2024.09.00|
|Core KTX|1.16.0|
|Lifecycle Runtime KTX|2.9.0|
|Activity Compose|1.10.1|
|ConstraintLayout|2.2.1|
※その他詳細なバージョンについては、`libs.versions.toml`及び`build.gradle`を参照ください。


