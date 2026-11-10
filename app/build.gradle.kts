import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val v16BannerBase64File = layout.projectDirectory.file("banner-assets/rs_tv_banner_v16.webp.base64")
val generatedV16BannerResDir = layout.buildDirectory.dir("generated/res/v16Banner")
val generateV16BannerResource = tasks.register("generateV16BannerResource") {
    inputs.file(v16BannerBase64File)
    val outputFile = generatedV16BannerResDir.map {
        it.file("drawable-nodpi/rs_tv_banner_v16.webp")
    }
    outputs.file(outputFile)
    doLast {
        val encoded = v16BannerBase64File.asFile.readText().filterNot { it.isWhitespace() }
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeBytes(Base64.getDecoder().decode(encoded))
    }
}

val releaseStoreFile = System.getenv("RS_TV_KEYSTORE_FILE")
val releaseStorePassword = System.getenv("RS_TV_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("RS_TV_KEY_ALIAS")
val releaseKeyPassword = System.getenv("RS_TV_KEY_PASSWORD")
val hasReleaseSigning = !releaseStoreFile.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.sinjartv.rstv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sinjartv.rstv"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            // Keep the experimental build installable beside the production app.
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
        }
        getByName("release") {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures { compose = true }
    sourceSets {
        getByName("main").assets.srcDir(rootProject.file("catalog/generated"))
        getByName("main").res.srcDir(generatedV16BannerResDir)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

tasks.named("preBuild").configure {
    dependsOn(generateV16BannerResource)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
