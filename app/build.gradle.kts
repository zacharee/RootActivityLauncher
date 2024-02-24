plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.atomicfu)
    alias(libs.plugins.bugsnag.android)
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "tk.zwander.rootactivitylauncher"
        namespace = "tk.zwander.rootactivitylauncher"
        minSdk = 21
        targetSdk = 34
        versionCode = 31
        versionName = versionCode.toString()

        extensions.getByType(BasePluginExtension::class.java).archivesName.set("RootActivityLauncher_${versionCode}")
    }

    buildTypes {
        release {
            signingConfig = signingConfigs["debug"]
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
    }
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.atomicfu)

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.preference.ktx)

    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.runtime)
    implementation(libs.compose.material)
    implementation(libs.activity.compose)
    implementation(libs.compose.theme.adapter3)
    implementation(libs.accompanist.themeadapter.material3)
    implementation(libs.datastore.preferences)

    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.composetooltip)

    implementation(libs.google.material)
    implementation(libs.gson)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.hiddenapibypass)
    implementation(libs.patreonSupportersRetrieval)
    implementation(libs.taskerpluginlibrary)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.libsuperuser)
    implementation(libs.progressCircula)

    implementation(libs.bugsnag.android)
    implementation(libs.dhizuku.api)
}