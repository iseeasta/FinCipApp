# Build FinCip APK on Fedora

1. Install Android Studio from the official Android developer site and complete its setup wizard.
2. In Android Studio → Settings → Android SDK, install Android SDK Platform 37 and Build-Tools (the matching version offered by SDK Manager).
3. In Settings → Build Tools → Gradle, set Gradle JDK to JDK 17 (Android Studio's bundled JBR is suitable if it is 17).
4. Open this project directory and allow Gradle sync to complete.
5. Connect an Android phone with USB debugging enabled, or create an emulator.
6. To build a debug APK from the terminal:

```bash
./gradlew assembleDebug
```

APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Copy the APK to the phone and open it using Files / Downloads. Android may ask you to allow installation from that file manager. Enable that permission only for the installer you are using.

For a shareable release, use Android Studio's **Build → Generate Signed Bundle / APK** and create your own signing key. Keep the keystore and its password backed up securely; FinCip's application signing key is separate from your encrypted finance backup password.
