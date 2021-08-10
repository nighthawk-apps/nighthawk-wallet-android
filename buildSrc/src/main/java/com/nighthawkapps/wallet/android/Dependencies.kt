package com.nighthawkapps.wallet.android

object Deps {
    // For use in the top-level build.gradle which gives an error when provided
    // `Deps.Kotlin.version` directly
    const val kotlinVersion = "1.5.21"
    const val navigationVersion =   "2.3.5"
    const val compileSdkVersion = 30
    const val buildToolsVersion = "30.0.3"
    const val minSdkVersion = 23
    const val targetSdkVersion = 30
    const val versionName = "1.0.25"
    const val versionCode = 1_00_25_800 // last digits are alpha(0XX) beta(2XX) rc(4XX) release(8XX). Ex: 1_08_04_401 is an release candidate build of version 1.8.4 and 1_08_04_800 would be the final release.
    const val packageName = "com.nighthawkapps.wallet.android"

    object AndroidX {
        const val ANNOTATION = "androidx.annotation:annotation:1.2.0"
        const val APPCOMPAT = "androidx.appcompat:appcompat:1.3.1"
        const val BIOMETRICS = "androidx.biometric:biometric:1.2.0-alpha03"
        const val CONSTRAINT_LAYOUT = "androidx.constraintlayout:constraintlayout:2.1.0"
        const val CORE_KTX = "androidx.core:core-ktx:1.6.0"
        const val FRAGMENT_KTX = "androidx.fragment:fragment-ktx:1.3.6"
        const val LEGACY = "androidx.legacy:legacy-support-v4:1.0.0"
        const val MULTIDEX = "androidx.multidex:multidex:2.0.1"
        const val PAGING = "androidx.paging:paging-runtime-ktx:3.0.0"
        const val RECYCLER = "androidx.recyclerview:recyclerview:1.2.0"
        const val SECURITY = "androidx.security:security-crypto:1.1.0-alpha03"
        const val DESUGAR_JDK = "com.android.tools:desugar_jdk_libs:1.1.5"

        object CameraX : Version("1.0.1") {
            val CAMERA2 = "androidx.camera:camera-camera2:$version"
            val CORE = "androidx.camera:camera-core:$version"
            val LIFECYCLE = "androidx.camera:camera-lifecycle:$version"

            object View : Version("1.0.0-alpha27") {
                val EXT = "androidx.camera:camera-extensions:$version"
                val VIEW = "androidx.camera:camera-view:$version"
            }
        }

        object Lifecycle : Version("2.2.0") {
            val LIFECYCLE_RUNTIME_KTX = "androidx.lifecycle:lifecycle-runtime-ktx:2.4.0-alpha03"
        }

        object Navigation : Version("2.3.5") {
            val FRAGMENT_KTX = "androidx.navigation:navigation-fragment-ktx:$version"
            val UI_KTX = "androidx.navigation:navigation-ui-ktx:$version"
        }

        object Room : Version("2.3.0") {
            val ROOM_COMPILER = "androidx.room:room-compiler:$version"
            val ROOM_KTX = "androidx.room:room-ktx:$version"
        }
    }

    object Dagger : Version("2.33") {
        val ANDROID_SUPPORT = "com.google.dagger:dagger-android-support:$version"
        val ANDROID_PROCESSOR = "com.google.dagger:dagger-android-processor:$version"
        val COMPILER = "com.google.dagger:dagger-compiler:$version"
    }

    object Google {
        // solves error: Duplicate class com.google.common.util.concurrent.ListenableFuture found in modules jetified-guava-26.0-android.jar (com.google.guava:guava:26.0-android) and listenablefuture-1.0.jar (com.google.guava:listenablefuture:1.0)
        // per this recommendation from Chris Povirk, given guava's decision to split ListenableFuture away from Guava: https://groups.google.com/d/msg/guava-discuss/GghaKwusjcY/bCIAKfzOEwAJ
        const val GUAVA = "com.google.guava:guava:30.1.1-android"
        const val MATERIAL = "com.google.android.material:material:1.4.0"
        const val GSON = "com.google.code.gson:gson:2.8.7"
    }

    object Grpc : Version("1.39.0") {
        val ANDROID = "io.grpc:grpc-android:$version"
        val OKHTTP = "io.grpc:grpc-okhttp:$version"
        val PROTOBUG = "io.grpc:grpc-protobuf-lite:$version"
        val STUB = "io.grpc:grpc-stub:$version"
    }

    object JavaX {
        const val INJECT = "javax.inject:javax.inject:1"
        const val JAVA_ANNOTATION = "javax.annotation:javax.annotation-api:1.3.2"
    }

    object Kotlin : Version(kotlinVersion) {
        val STDLIB = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"

        object Coroutines : Version("1.5.1") {
            val ANDROID = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
            val CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
            val TEST = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
        }
    }

    object Zcash {
        const val ANDROID_WALLET_PLUGINS = "cash.z.ecc.android:zcash-android-wallet-plugins:1.0.0"
        const val KOTLIN_BIP39 = "cash.z.ecc.android:kotlin-bip39:1.0.1"
        const val SDK = "cash.z.ecc.android:zcash-android-sdk:1.3.0-beta18"
    }

    object Misc {
        const val I_TEXT_CORE = "com.itextpdf:itext7-core:7.1.13"
        const val LOTTIE = "com.airbnb.android:lottie:4.0.0"

        object Plugins {
            const val SECURE_STORAGE = "com.github.gmale:secure-storage-android:0.0.3"
            const val QR_SCANNER = "com.google.zxing:core:3.4.1"
        }
    }

    object Test {
        const val JUNIT = "junit:junit:4.13.2"
        const val MOKITO = "junit:junit:4.13.2"
        const val COROUTINES_TEST = "junit:junit:4.13.2"
        const val MOCKITO_KOTLIN = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"

        object Android {
            const val JUNIT = "androidx.test.ext:junit:1.1.1"
            const val ESPRESSO = "androidx.test.espresso:espresso-core:3.4.0"
        }
    }
}

open class Version(@JvmField val version: String)
