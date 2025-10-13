# 🧭 ハードウェア設計・実装構成

本章では、**S310** 機体搭載用の計測システムにおけるハードウェア構成・実装方針をまとめる。  
計測システムは、**配線簡略化と信頼性向上**を最優先目標として設計されており、主要デバイスには**I²C 通信**を採用し、機器接続には**XH コネクタ**を統一的に使用することで、**振動耐性・防水性と高い整備性・交換性**を確保した。

## 1. 計測機器一覧と仕様

<!--TODO:センサー正しいか確認-->

S310 機体では、以下のセンサー群を搭載した。  
各計器の選定理由、通信方式、設置場所および主要仕様を表に示す。

| 計測項目     | センサー名                                                                                                           | 通信方式 | 設置位置         | 主な仕様・備考                                             |
| ------------ | -------------------------------------------------------------------------------------------------------------------- | -------- | ---------------- | ---------------------------------------------------------- |
| **高度計**   | [MB1242-000](https://akizukidenshi.com/catalog/g/g114709/)                                                           | I²C      | フェアリング下部 | 測距範囲 20–765cm。地面反射を利用した高度算出。            |
| **機速計**   | [ピトー管](https://amzn.asia/d/ecEdyK8) + [SDP810-500Pa](https://www.mouser.jp/ProductDetail/Sensirion/SDP810-500PA) | I²C      | テールビーム     | 差圧式。ベルヌーイの定理より動圧から速度換算。             |
| **姿勢角計** | [BNO080](https://www.amazon.co.jp/dp/B0DM98Q67M)                                                                     | I²C      | TB 基板上        | 9 軸（加速度・角速度・地磁気）統合センサ。四元数出力対応。 |
| **舵角計**   | [CJMCU-103](https://amzn.asia/d/fbYAGlM)                                                                             | アナログ | ER 機構部        | 可変抵抗方式。舵軸角度を電圧変換して出力。                 |

## 2. 設計方針と通信構成

- 通信バス設計  
  主要センサーは I²C 通信を採用し、配線を 4 本（VCC, GND, SDA, SCL）に統一。  
  これにより、配線量を削減し、機体内部のスペース効率および整備性を向上させた。  
  一部アナログ出力（舵角計）のみ、直接 ADC 入力として基板に接続。

- 電源系統  
  すべてのセンサーは 3.3V 系統で駆動。  
  ※ ESP32、表示機器 LCD1602 は 5V 電源使用

- 接続インタフェース  
  センサーと基板の接続には XH コネクタを使用。  
  極性防止と確実な保持を両立し、飛行中の振動環境下や雨天環境下でも安定動作を確保した。

## 3. 機器配置と実装

全体の計器配置図を以下に示す。  
<img src="https://github.com/user-attachments/assets/c1e43768-2d11-4781-8030-5a3d245f5ef2" alt="コックピット" width="600">

- 高度計（超音波センサー）  
  フェアリング下部に下向きに設置し、地面反射を利用して離陸直後から高度を検知可能。  
  測定レンジ外（x < 20 cm、765 cm < x）での精度低下を考慮し、補正ロジックをファームウェアに実装。

- 機速計（ピトー管 + 差圧センサー）  
  テールビーム先端にピトー管を配置し、静圧ポートとの差圧を[SDP810-500Pa](https://www.mouser.jp/ProductDetail/Sensirion/SDP810-500PA)で検出。  
  流線の乱れを最小化するため、プロペラや翼の影響が少ないテールビーム後部に設置。

- 姿勢角計（BNO080）  
  TB 基板上に実装し、機体座標系に対して固定。  
  9 軸統合センサー出力からクォータニオンを生成し、姿勢角（ピッチ・ロール・ヨー）をリアルタイム算出。

- 舵角計（可変抵抗）  
  ER 可動部の回転軸に連結。  
  可動角を電圧値に変換し、ESP32 の ADC に入力してデジタル値へ変換。<br><br>

|            | 基板設置場所                                                                                                                     |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------- |
| メイン基板 | <img src="https://github.com/user-attachments/assets/c9511cec-04ee-4d2e-a01b-62e47ae4a66d" alt="メイン基板設置場所" width="600"> |
| TB 基板    | <img src="https://github.com/user-attachments/assets/d9f75060-6250-4b48-b116-e1941ae798e5" alt="TB基板設置場所" width="600">     |

<br>基盤と基板 BOX
| |メイン基板|TB 基板|
|---|---|---|
|基板|<img src="https://github.com/user-attachments/assets/06a95b7e-0ac5-4e8b-862f-901a45c4e0d8" alt="メイン基板" width="600">|<img src="https://github.com/user-attachments/assets/cd2abbba-d2d1-4de2-a08e-eb71223fb60e" alt="TB基板" width="600">|
|基板 BOX|<img src="https://github.com/user-attachments/assets/55e7e6a4-a767-4f23-bd4b-c7771f573f33" alt="メイン基板BOX" width="600">|<img src="https://github.com/user-attachments/assets/878c554c-92f4-4891-8b04-863a76adb4ec" alt="TB基板BOX" width="600">|

## 4. データ取得と統合

各センサーからのデータは ESP32 を介して周期的に取得される。  
I²C 通信デバイスは同一バス上でアドレス分離され、姿勢・速度・高度・舵角の各データを同周期（約 10Hz）で統合。  
データは Android アプリおよび Firebase に送信され、飛行ログとして蓄積・可視化される。

## 5. センサーキャリブレーション方法

- 高度計  
  測定範囲が 20cm~765cm なので、地面から 20cm の地点に設置し、その値を 0 として測定

- 姿勢角計
  起動時に初期値を 0 として、そこからのずれ角度で計算
  時間がたつとズレが生じ、特にヨーのずれがひどかったため、リセット処理も追加しようとしたが未実装 - ヨー  
   補正式

      ```c++
      // --- 磁気センサキャリブレーション（回転しながら実行） ---
      if (calibrateMag) { /* ... 最大・最小値の更新 ... */ }

      // ハードアイアン補正
      float magOffsetX = (magMaxX + magMinX) / 2.0;
      float magOffsetY = (magMaxY + magMinY) / 2.0;
      float correctedMagX = magRaw.x - magOffsetX;
      float correctedMagY = magRaw.y - magOffsetY;
      ```
      センサーを回転させたときの地磁気の最大値・最小値を記録
      最大値と最小値の中央値をオフセットとして、現在の地磁気生データ（magRaw）から差し引くことで補正

      計算式
      ```c++
      // チルト補正付き磁気方位角の計算
      float xh = magRaw.x * cos(pitchRad) + magRaw.z * sin(pitchRad);
      float yh = magRaw.x * sin(rollRad) * sin(pitchRad) + magRaw.y * cos(rollRad) - magRaw.z * sin(rollRad) * cos(pitchRad);

      float heading = atan2(yh, xh) * 180.0 / PI;

      // --- 地磁気に基づくyawの更新 ---
      yaw = 0.90 * yaw + 0.10 * heading;
      ```
      機体が傾いている（ロールやピッチがある）場合、地磁気の値が歪むため、**ロール（rollRad）とピッチ（pitchRad）を使ってその影響を打ち消す（チルト補正）

      地磁気で計算した新しいheading（方位）と、以前のyaw（ヨー）を**重み付けして平均する（相補フィルターや簡易的なカルマンフィルターの考え方）**ことで、ヨー角をスムーズに更新

      - ロール、ピッチ
      初期値からのずれ角度を計算
      ```c++
      // --- ロール・ピッチ計算 ---
      float accRoll = atan2(accRaw.y, accRaw.z) * 180.0 / PI;
      float accPitch = atan2(-accRaw.x, sqrt(accRaw.y * accRaw.y + accRaw.z * accRaw.z)) * 180.0 / PI;

      roll  = alpha * roll  + (1.0 - alpha) * accRoll;
      pitch = alpha * pitch + (1.0 - alpha) * accPitch;
      ```

      前の角度（roll, pitch）に重みalpha（ジャイロの寄与）をかけ、加速度計の角度に重み(1.0 - alpha)をかけて加算する **相補フィルター（Complementary Filter）** 使用

## 6. まとめ

計測機器群は、軽量・低消費電力・高信頼性を指針として選定した。  
I²C 通信による共通化およびコネクタ統一設計により、再現性の高い配線構成とメンテナンス性を実現している。  
また、センサー配置を流体・構造設計と整合させることで、飛行時のデータ精度を確保した。

> 計器主担当：[tttt10231023](https://github.com/tttt10231023)  
> 著：[norahshion](https://github.com/norahshion)
