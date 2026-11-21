# Build FinCip APK in the cloud (no Android Studio needed)

This project includes a GitHub Actions workflow at `.github/workflows/android-apk.yml`.
GitHub's hosted runner installs JDK 17 and Android SDK Platform 37 / Build Tools 36.0.0,
then runs the Gradle build and uploads `app-debug.apk` as a downloadable workflow artifact.
Your ThinkPad only needs to upload the source and download the finished APK; it does not
need Android Studio, an Android SDK, an emulator, or a local Gradle installation.

## 1. Download and extract the project

Extract `FinCip-Native-Android.zip` on Fedora. Open a terminal in the extracted
`FinCip-Native-Android` folder (the folder containing `settings.gradle.kts`).

## 2. Install the small GitHub upload tools

```bash
sudo dnf install git gh unzip
```

These are lightweight command-line tools, not Android Studio.

## 3. Sign in to GitHub

```bash
gh auth login
```

Choose **GitHub.com**, **HTTPS**, and **Login with a web browser**. Follow the one-time
code prompt in your browser.

## 4. Create a private repository and upload the project

From the project folder:

```bash
git init -b main
git config user.name "FinCip Android Build"
git config user.email "YOUR_GITHUB_EMAIL"
git add .
git commit -m "Add FinCip native Android project"
gh repo create fincip-native-android --private --source=. --remote=origin --push
```

Replace `YOUR_GITHUB_EMAIL` with the email on your GitHub account. Keep the repository
**Private**: the source is your app code. Do not commit finance backups, API keys,
Supabase service-role keys, signing keystores, passwords, or personal transaction data.

## 5. Wait for GitHub to build the APK

Pushing the `main` branch automatically starts the workflow:

1. Open the new `fincip-native-android` repository on GitHub.
2. Open the **Actions** tab.
3. Select **Build FinCip Android APK**.
4. Open the newest run and wait for the green check mark.
5. Under **Artifacts**, download **FinCip-debug-APK**.
6. Extract the downloaded artifact ZIP. It contains `app-debug.apk`.

If the build fails, open the run, select the failed `Build FinCip debug APK` step, and copy
the first meaningful error from its log. Do not assume an APK exists after a failed run.

## 6. Install on your Android phone

Move `app-debug.apk` to your phone using USB, Nearby Share / Quick Share, KDE Connect,
or download it from GitHub in your phone browser (sign in to the same private repository).
Open the APK with Files / Downloads. Android may ask you to allow that installer to install
unknown apps; enable it for that installer only, then tap **Install**.

This debug APK is suitable for personal testing. It is not a Play Store release and is
signed with GitHub Actions' temporary debug signing key. Keep using the same debug
application identity for in-place updates. For long-term public distribution, create and
secure your own release signing key; never upload that private key to a public repository.

## Build details

- Workflow: `.github/workflows/android-apk.yml`
- Task: `:app:assembleDebug`
- Artifact: `FinCip-debug-APK`
- APK path in build output: `app/build/outputs/apk/debug/app-debug.apk`
- Artifact retention: 14 days; download it before expiry.

GitHub Actions standard hosted runners are free for public repositories. Private repositories
use the included Actions minutes / storage allowance for the GitHub plan; check your account's
Actions billing page if you need to confirm remaining quota.
