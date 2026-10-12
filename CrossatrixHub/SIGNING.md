# Signing Crossatrix Hub

Android only installs an update over an existing app if both APKs are signed with the same key.
Create the key once and keep the .jks file and its passwords safe forever.

## 1. Create the key

Run these from the CrossatrixHub folder (the one containing settings.gradle.kts).
Each command is one line. Do not split it across lines.

Windows (Command Prompt or PowerShell):

    keytool -genkeypair -v -keystore app\crossatrix-release.jks -alias crossatrix -keyalg RSA -keysize 2048 -validity 10000

Mac or Linux:

    keytool -genkeypair -v -keystore app/crossatrix-release.jks -alias crossatrix -keyalg RSA -keysize 2048 -validity 10000

keytool will ask for a keystore password, your name and organisation, and a key password.
Write these down. The key file ends up in the app folder.

If keytool is not found, use the one bundled with Android Studio:

    "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"

(replace the path with your Android Studio install location, and use the same single-line command after it).

## 2. Set up the passwords

Copy keystore.properties.example to keystore.properties (same folder as settings.gradle.kts)
and fill in the values you chose:

    storeFile=crossatrix-release.jks
    storePassword=your keystore password
    keyAlias=crossatrix
    keyPassword=your key password

## 3. Build a signed release APK

In Android Studio: Build > Generate Signed Bundle / APK > APK, then pick the release build type.
Or run in a terminal: gradlew assembleRelease
The APK is written to app/build/outputs/apk/release/.

## 4. Check the signature (optional)

    apksigner verify --print-certs <file>.apk

## If the update still fails

If the installed app was signed with a different key (for example a debug build from another computer),
Android will refuse the update. Uninstall the app once, then install the new release.
Options and the saved login are lost, so sign in again afterwards.
