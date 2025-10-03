import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties") // プロジェクトルートからの相対パスで指定
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

android {
    namespace = "com.example.backp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.backp"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // local.properties から API キーを取得。見つからない場合は空文字列を設定
        val mapsApiKey = localProperties.getProperty("maps_api_key", "")

        // API キーを BuildConfig に追加
        buildConfigField("String", "MAPS_API_KEY", "\"${mapsApiKey}\"") // ここで BuildConfigField を使用

        // Manifest プレースホルダーも引き続き利用可能
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin { // kotlinOptions の代わりに kotlin DSL を使用
        jvmToolchain(21) // JavaVersion.VERSION_11 の代わりに数字を使用
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
            // その他、必要なコンパイラオプションがあればここに追加
        }
    }
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.2.0"
    }
    packaging {
        resources {
            excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES") // リスト形式で統合
            excludes += "META-INF/INDEX.LIST" // 競合しているファイルを排除
            excludes += "/META-INF/{AL2.0,LGPL2.1}" // 以前からあったものも維持
            excludes += "META-INF/DEPENDENCIES" // 以前からあったものも維持
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    implementation("com.google.firebase:firebase-auth")  // バージョン指定なしでfirebase-bomが管理
    implementation("com.google.firebase:firebase-database-ktx")


    implementation ("androidx.activity:activity-ktx:1.10.1")

    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.google.oauth-client:google-oauth-client:1.39.0")
    implementation("com.google.api-client:google-api-client:2.8.0")
    implementation("com.google.api-client:google-api-client-gson:2.8.0")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.5.3")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation(libs.androidx.constraintlayout)
    // Kotlin互換性の問題が発生しにくいfirebase-authライブラリを使用

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Google Sheets API
    implementation("com.google.apis:google-api-services-sheets:v4-rev614-1.18.0-rc")

    // Google認証
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // JSONライブラリの追加
    implementation("org.json:json:20240303")


    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.accompanist:accompanist-permissions:0.30.1")

    // 🔹 Google Maps Compose の追加
    implementation("com.google.maps.android:maps-compose:2.12.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    implementation("androidx.compose.material:material-icons-extended") // こちらも最新版を確認
    implementation("androidx.compose.material:material-icons-core") // または最新のバージョン
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0") // 最新の安定版

}