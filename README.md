# NClicker

NClicker is an Android vehicle-plate collection game with Material 3 Compose UI, a Java game-model/persistence layer, and a Rust native game engine.

## Game features

- Ten country catalogues with localized regions and generated plate patterns.
- Seven rarity tiers, rarity-specific rewards, reveal animations and sound signals.
- Civilian, taxi, police, fictional government-themed, black-series, diplomatic and retro plate categories.
- Local balance, upgrades, sound preferences and the latest 250 collection items are saved on device.
- Kotlin / Compose handles presentation, Java owns the plate model and persistence, and Rust via JNI performs rarity rolls, number generation, country/region selection, category selection and reward calculation.
- Java fallback keeps the game playable in IDE runs where the native library is unavailable.

All plate numbers and special series are generated for the game. They are fictional and do not identify real vehicles, people, registrations or government vehicles.

## Build APK manually

The workflow does not run on each push. To build when you choose:

1. Open the repository's Actions tab: https://github.com/cuuw619-maker/NClicker/actions
2. Select Android APK.
3. Click Run workflow and choose the main branch.
4. Wait for the run to complete successfully.
5. Download the NClicker-debug-apk artifact and extract app-debug.apk.

The workflow installs Java 17, Android SDK 35 and NDK 27.2; runs Rust unit tests; compiles the JNI library for arm64-v8a; runs Gradle assembleDebug; and uploads the APK. Minimum Android version is Android 8.0 / API 26.

## Local development

Open the project in Android Studio with JDK 17 and Android SDK 35. The Java engine falls back if the Rust native library is not available in an IDE run.

To build the Rust JNI library locally on Linux with the Android NDK:

    rustup target add aarch64-linux-android
    export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/27.2.12479018
    export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang
    cargo test --manifest-path rust/Cargo.toml
    cargo build --target aarch64-linux-android --release --manifest-path rust/Cargo.toml
    mkdir -p app/src/main/jniLibs/arm64-v8a
    cp rust/target/aarch64-linux-android/release/libplate_engine.so app/src/main/jniLibs/arm64-v8a/
    gradle --no-daemon assembleDebug

## Technology

- Kotlin 2.0.21 / Jetpack Compose / Material 3: presentation layer.
- Java 17: game model, game-engine facade and local collection persistence.
- Rust with JNI: weighted rarity, plate generation, country/region selection, special categories and reward calculation.
- Android Gradle Plugin 8.7.3.
