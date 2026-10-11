# NClicker

NClicker is an Android number-plate collection clicker built with Kotlin, Jetpack Compose and Material 3. Each tap generates a fictional plate design from a country and region, awards in-game credits, and adds the result to the garage.

## Features

- Material 3 dark interface with bottom navigation.
- Generated plate designs inspired by Russia, Ukraine, Germany, Japan, the United States, the United Kingdom, France, the UAE, Poland and Italy.
- Seven rarity tiers, rarity-coloured presentation, animated reveals and rarity-specific tone feedback.
- Civilian, taxi, police, fictional FSB-themed, black-series, diplomatic and retro categories.
- Persistent local balance, upgrades, sound setting and a collection capped to the latest 250 finds.
- Luck and reward upgrades.
- Kotlin Android UI, a Java plate-format helper, and a Rust JNI ticket generator.
- GitHub Actions builds a debug APK and uploads it as the **NClicker-debug-apk** artifact.

All numbers and special-series designs are randomly generated for the game. They are fictional and do not identify real vehicles, people, registrations or government vehicles.

## Build with GitHub Actions

1. Open the **Actions** tab in this repository.
2. Select **Android APK**.
3. Choose **Run workflow**, or push a commit to main.
4. Open the completed run and download **NClicker-debug-apk** from **Artifacts**.
5. Extract the artifact and install app-debug.apk on an Android device (Android 8.0/API 26 or newer).

The workflow installs Java 17, Android SDK 35, NDK 27.2, Rust and the Android NDK linker, compiles the Rust library for arm64-v8a, then runs Gradle assembleDebug.

## Local development

Open this repository in a recent Android Studio with JDK 17 and Android SDK 35 installed. The app has a Kotlin fallback when the Rust native library has not been built locally; the supplied Actions workflow builds and packages it. To reproduce the full native build, install Rust with the aarch64-linux-android target and the Android NDK, then run the following commands from the repository root:

    export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/27.2.12479018"
    export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang"
    cargo build --target aarch64-linux-android --release --manifest-path rust/Cargo.toml
    mkdir -p app/src/main/jniLibs/arm64-v8a
    cp rust/target/aarch64-linux-android/release/libplate_engine.so app/src/main/jniLibs/arm64-v8a/

Then build the debug APK from Android Studio or with Gradle 8.10.2:

    gradle --no-daemon assembleDebug

## Technology

- Kotlin 2.0.21
- Java 17
- Rust (cdylib loaded through JNI)
- Android Gradle Plugin 8.7.3
- Jetpack Compose / Material 3
