# NClicker

NClicker is an Android number-plate collection game. Jetpack Compose and Material 3 handle presentation, Java owns the game model and economy, and Rust generates plates through JNI.

## Gameplay

- Starting money: **$3,000**.
- Tapping the main button adds a generated plate to the inventory. It does not pay money and does not spend the wallet balance.
- Each item has a market value; an ordinary plate starts at about **$100**. Higher rarity and special categories change its valuation.
- Sell plates individually from the Inventory screen to add the sale amount to the wallet.
- Expand garage capacity from 30 to 150 items, upgrade odds for rare combinations, and improve collector connections to increase sale offers.
- Sales apply the current collector-connection bonus to both old and new plates.
- The display uses country-specific layouts and familiar number patterns for Russia, Ukraine, Germany, Japan, the USA, the UK, France, the UAE, Poland and Italy.
- Seven rarity tiers, category filters, rare-result animation and sound feedback are included. A rarity animation changes the plate card in place and does not open a modal.
- Balance, upgrades, sound settings and inventory are stored locally. Legacy save data is migrated to the dollar economy with $3,000 starting balance.

Generated combinations and special government-themed series are fictional and are not actual issued registrations.

## Build the APK manually

The GitHub Actions workflow is **manual only**; commits and pull requests do not start a build.

1. Open Actions: https://github.com/cuuw619-maker/NClicker/actions
2. Select **Android APK**.
3. Click **Run workflow** and choose `main`.
4. Wait for the run to complete successfully.
5. Download the `NClicker-debug-apk` artifact and extract `app-debug.apk`.

The workflow sets up Java 17, Android SDK 35 and NDK 27.2; runs Rust unit tests; compiles the ARM64 JNI library; runs `gradle assembleDebug`; and uploads the APK. Minimum Android version: Android 8.0 / API 26.

## Technology

- **Kotlin / Jetpack Compose / Material 3:** Android presentation, layout, transitions and navigation.
- **Java 17:** immutable plate model, generation bridge/fallback, wallet economics, sale-price calculations, upgrade costs and persistent inventory.
- **Rust / JNI:** weighted rarity, country and region selection, country-specific number patterns, categories and market-value generation.

To run the Rust tests locally: `cargo test --manifest-path rust/Cargo.toml`.
