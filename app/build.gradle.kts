import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val copyStringsTemplate by tasks.registering(Copy::class) {
    from(layout.projectDirectory.file("src/main/res/values/strings.xml"))
    into(layout.buildDirectory.dir("generated/i18nAssets"))
    rename { "strings_template.xml" }
}

android {
    namespace = "com.savoo.scclient"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.savoo.scclient"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 17
        versionName = "2.0-oneui8"

        buildConfigField("String", "SC_CLIENT_ID_FALLBACK", "\"\"")
        buildConfigField("String", "TELEGRAM_API_ID", "\"${localProps.getProperty("TELEGRAM_API_ID", "0")}\"")
        buildConfigField("String", "TELEGRAM_API_HASH", "\"${localProps.getProperty("TELEGRAM_API_HASH", "")}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("release-key.jks")
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = "Resona"
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        val release by getting {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("canary") {
            initWith(release)
            applicationIdSuffix = ".canary"
            versionNameSuffix = "-canary"
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    kotlin {
        jvmToolchain(17)
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    sourceSets.getByName("main") {
        assets.srcDir(copyStringsTemplate)
    }
    sourceSets.getByName("debug") {
        assets.srcDir("$projectDir/schemas")
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

tasks.named("preBuild") {
    dependsOn(copyStringsTemplate)
}

configurations.implementation {
    exclude("androidx.core", "core")
    exclude("androidx.core", "core-ktx")
    exclude("androidx.customview", "customview")
    exclude("androidx.coordinatorlayout", "coordinatorlayout")
    exclude("androidx.drawerlayout", "drawerlayout")
    exclude("androidx.viewpager2", "viewpager2")
    exclude("androidx.viewpager", "viewpager")
    exclude("androidx.appcompat", "appcompat")
    exclude("androidx.fragment", "fragment")
    exclude("androidx.preference", "preference")
    exclude("androidx.recyclerview", "recyclerview")
    exclude("androidx.slidingpanelayout", "slidingpanelayout")
    exclude("androidx.swiperefreshlayout", "swiperefreshlayout")
    exclude("com.google.android.material", "material")
    exclude("androidx.fragment", "fragment-ktx")
}

dependencies {
    // SESL 8 - Jetpack
    implementation("sesl.androidx.core:core:1.19.0+1.0.7-sesl8+rev1")
    implementation("sesl.androidx.core:core-ktx:1.19.0+1.0.0-sesl8+rev1")
    implementation("sesl.androidx.appcompat:appcompat:1.8.0+1.0.21-sesl8+rev3")
    implementation("sesl.androidx.fragment:fragment:1.9.0+1.0.5-sesl8+rev1")
    implementation("sesl.androidx.recyclerview:recyclerview:1.4.0+1.0.12-sesl8+rev3")
    implementation("sesl.androidx.preference:preference:1.2.1+1.0.0-sesl8+rev1")

    // SESL 8 - Material
    implementation("sesl.com.google.android.material:material:1.12.0+1.0.32-sesl8+rev3")

    // Composants One UI 8
    implementation("io.github.tribalfs:oneui-design:0.9.19+oneui8")
    implementation("io.github.oneuiproject:icons:1.1.0")

    // Navigation and layout
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation(libs.lifecycle.runtime.ktx)

    // Image loading for XML views
    implementation("io.coil-kt:coil:2.7.0")

    // DI
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.jsoup)

    // Playback (Media3)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.session)
    implementation(libs.media3.common)

    // Room DB
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.room.paging)
    implementation(libs.paging.runtime)

    // Palette & Security & Storage
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    implementation(libs.biometric)

    // Background jobs
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.test.core)
}
