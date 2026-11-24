plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.marbellup.fitin"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.marbellup.fitin"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"
    }

    // Clave de firma FIJA para las compilaciones de prueba: así cada APK nuevo se instala
    // encima del anterior sin tener que desinstalar. Solo para la demo, no para Google Play.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Avisos periódicos en segundo plano (la IA revisa actividades nuevas aunque la app esté cerrada).
    implementation(libs.androidx.work.runtime.ktx)
    // Simulación que demuestra que el modelo aprende (app/src/test).
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
