# FinCip Native for Android

A **native Android application** built with Kotlin and Jetpack Compose. It does not embed the FinCip website in a WebView and it is not a browser shortcut.

## Included modules

- Overview dashboard, transaction list and editing, tags, people, projects, payment accounts, split allocations, privacy masking
- CSV and OFX/QFX-style statement parsing with duplicate checking; CSV export and printable PDF report
- Recurring bills and income, subscriptions, due-date notifications, mark-paid workflow
- Monthly money calendar with income, expense, bills and savings-goal markers
- Category budgets, goals, goal contributions, emergency fund and debt payoff estimator (avalanche / snowball)
- Net worth assets and liabilities; what-if savings simulator; cash-flow forecast; spending insights and categorization rules
- Multi-currency converter and preferences for INR, USD, EUR, GBP, JPY, CNY, CHF, CAD, AUD and SGD. You can refresh published reference rates from Frankfurter when online or edit them manually; they are not live trading or settlement quotes.
- India savings products (SIP, mutual funds, EPF, PPF, NPS, FD, RD, gold) with manual balances, contributions, XIRR and CAGR estimates
- Global tax planning: simplified India slab estimate; manually configured effective-rate estimates for other jurisdictions. This is not a tax filing engine.
- Investment portfolio, receipt vault, encrypted backups, plain JSON backups, household budgets and optional Supabase workspace sync
- Android wallpaper-based Material You colors on Android 12+, plus Y2K Acid, Cozy Lo-Fi and Receipt Paper themes
- Customizable mascot with character, name and accent colour. It celebrates completed savings goals / healthy saving behaviour and switches to ramen mode when the takeout category exceeds its budget.
- Neo-Brutalist Android components, with native hard-border cards, offset shadows, and customizable dashboard summary-card order.
- Android voice recognition, notification reminders, native file selection, native share/export flows and installable APK.

## Requirements

- Android Studio with a JDK 17 toolchain
- Android SDK Platform 37 (Android Studio can install it from SDK Manager)
- Internet access for the first Gradle sync, because Gradle and Maven dependencies need to be downloaded
- Android 8.0 (API 26) or newer for installation; Android 12+ for Material You wallpaper dynamic color

The project currently targets API 37, uses Android Gradle Plugin 9.5.2, Gradle 9.6.0 and Compose BOM 2026.09.00. The APK has not been built in this environment: Android SDK Platform 37 is not installed here and network access to the Gradle distribution host was unavailable. Build the APK locally using the steps below. See the Android release notes for compatibility details:

- https://developer.android.com/build/releases/agp-9-5-0-release-notes
- https://developer.android.com/develop/ui/compose/bom


## Build the APK without Android Studio (recommended for low-spec laptops)

You can build FinCip on GitHub's hosted runner and download the APK without installing
Android Studio or the Android SDK on your laptop. This repository includes
`.github/workflows/android-apk.yml`, which builds a debug APK and uploads it as a
workflow artifact whenever you push to `main`. Follow
[BUILD-APK-WITH-GITHUB-ACTIONS.md](BUILD-APK-WITH-GITHUB-ACTIONS.md).

## Open in Android Studio

1. Extract the ZIP archive.
2. In Android Studio choose **Open** and select the extracted `FinCip-Native-Android` folder (the folder containing `settings.gradle.kts`).
3. Allow Gradle sync and install Android SDK Platform 37 if prompted.
4. Choose an Android device or emulator and press **Run**.

## Build a debug APK

On Fedora / Linux:

```bash
cd FinCip-Native-Android
./gradlew assembleDebug
```

The custom `gradlew` bootstrap downloads the pinned Gradle distribution if it is not already cached. Then the Android Gradle plugin downloads its dependencies from Google Maven and Maven Central.

The debug APK will be written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

On Windows, open the project in Android Studio and choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**. The included `gradlew.bat` expects Gradle 9.6.0 to be cached under the standard user Gradle wrapper directory.

For a distributable signed release APK, use **Build → Generate Signed Bundle / APK** in Android Studio and create your own signing key. No signing key is included in this source package.

## Import data from the FinCip website version

1. In the existing FinCip website, go to **Settings & data → Full backup / Export JSON**. For a backup with receipt attachments, use **Encrypted backup** instead.
2. Copy the exported `.json` file to the Android device (Downloads is fine).
3. In the Android app open **Explore → Privacy & backups → Choose backup file**.
4. For encrypted backups, enter the same password used when exporting.

The Android app understands the regular FinCip JSON workspace shape and the `fincip-encrypted-backup` format (PBKDF2-SHA256, 310,000 iterations, AES-256-GCM). The encrypted backup can include receipt attachments. Keep the password: there is no recovery mechanism.

The native app stores its workspace in an AES-GCM encrypted SQLite payload using an Android Keystore key. `allowBackup` is disabled so the private database is not copied by the Android system backup service. Export a FinCip backup before changing phones or uninstalling.

## Bank statement import

Open **Money → Transactions → Import statement** and select a CSV or OFX file. The importer looks for common date, description, amount / debit / credit, and currency columns. It attempts to deduplicate rows against existing transactions. Imported rows use an `Other` category so you can recategorize them afterward.

Different banks use different statement schemas. Always review the resulting transactions and totals against the original statement before relying on them. QFX files that contain standard OFX `<STMTTRN>` blocks can be read by the same parser.

## Themes and mascot

Open **Explore → Settings & vibe**:

- **System / Material You** uses Android dynamic wallpaper-derived colors on Android 12+.
- **Y2K Acid** uses acid lime, hot pink and electric blue.
- **Cozy Lo-Fi** uses warm paper, clay and olive tones.
- **Receipt Paper** uses warm paper styling and monospace typography.

The mascot can be renamed and changed (cat, robot, sprout, fox, frog, panda or octopus) with a selectable accent. It responds to savings progress and takeout-budget status; it does not read health, location or other device sensors.

## Optional Supabase sync and shared budgets

Local mode works without a backend. To enable account sync / household budgets:

1. Create a Supabase project that you control.
2. Run `supabase/schema.sql` in its SQL Editor.
3. If you already ran an earlier FinCip schema, run `supabase/migrations/001_shared_budget_currencies.sql` to allow the ten supported currencies in shared budgets.
4. In the app open **Explore → Household & sync**, enter the Supabase project URL and public anon key, then sign in with Supabase Auth.

The app uses HTTPS, Supabase Auth and row-level security from the included schema. Workspace push/pull is manual, not end-to-end encrypted, and it replaces the corresponding cloud/local workspace snapshot. Shared budgets can be refreshed manually; this native build does not currently maintain a live WebSocket subscription. Receipt files stay in the device vault and encrypted local backups; they are not uploaded by the Supabase workspace sync.

Never use a Supabase service-role key in the Android app. Only use the public anon key, with the provided RLS policies enabled.

See [FEATURE-COVERAGE.md](FEATURE-COVERAGE.md) for a module-by-module map from the FinCip website version to the native Android screens and the features adapted for Android.

## Important limitations

- This is a local-first finance tracker, not a bank connection. CSV/OFX import is a user-selected file workflow; it does not ask for or store bank login credentials.
- FX reference rates are user-editable; the app does not promise real-time forex pricing.
- Investment prices and fund values are entered manually; no broker, exchange or AMC integration is configured.
- India tax estimate is simplified. Other countries use a user-provided effective tax rate and deduction amount rather than jurisdiction-specific tax tables. It is not tax advice or filing software.
- Household shared-budget refresh is manual in this native version; there is no real-time WebSocket feed yet.
- Receipt documents are stored in the app-private files directory. Android app sandbox protects that directory, but the raw receipt files are not individually encrypted at rest. Password-encrypted exports encrypt the included receipt files.
- Notification delivery is best-effort and subject to Android notification permission, battery optimisation and WorkManager scheduling behaviour.

## Package identity

- Application ID: `com.fincip.nativeapp`
- App label: `FinCip`
- Minimum Android: API 26
- Native UI: Kotlin + Jetpack Compose + Material 3
