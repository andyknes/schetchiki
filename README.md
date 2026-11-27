# СЧЕТЧИКИ — Android project

Prototype source project for Android 12+ (minSdk 31), Kotlin + Jetpack Compose.

## Features included
- Full-screen immersive portrait interface.
- Splash branding screen (1.7 seconds; custom generated artwork is included as `app/src/main/res/drawable/splash_art.png`, though the code currently uses a minimal animated text splash).
- Five numeric fields: ХВ, ГВ, ХК, ГК, ЭЛ.
- Phone-number entry screen.
- Direct SMS sending with Android `SEND_SMS` permission prompt.
- Sent-result callback, success screen, retry/error screen.
- Clear all numeric fields after pressing `ГОТОВО`.
- Adaptive Compose layout for different display sizes.

## Build
Open this folder in Android Studio (recent version with Android SDK 35 and JDK 17), let Gradle sync, then use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
The generated debug APK is typically at `app/build/outputs/apk/debug/app-debug.apk`.

## Important SMS note
The app sends SMS directly using `SmsManager`. Android's permission prompt is required. SMS sending may fail depending on SIM, network, carrier, account/balance, dual-SIM configuration, or device capabilities. The app's callback indicates the SMS was accepted by the system/carrier send path, not guaranteed delivery to the recipient. Direct SMS permissions may also make Google Play publishing subject to policy review.
