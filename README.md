# WhereUAt (BLE Proximity Friend Finder)

WhereUAt is a GPS-free Android app concept for finding friends in crowded spaces using **Bluetooth Low Energy proximity + direction estimation**.

## MVP implemented

- Anonymous login with Firebase Auth
- Create / join event session with Firestore
- BLE scan loop with battery-aware profiles (fast, balanced, saver)
- Optional Wi‑Fi RSSI adjustment for rough stability
- Radar UI with friend icons around the user center (no map, no coordinates)
- Guidance screen with heading-aware arrow-style turn direction and distance labels
- Dynamic friend clustering into `Group 1`, `Group 2`, ...

## Tech

- Kotlin + Jetpack Compose
- Simple MVVM + Coroutines
- Firebase Auth + Firestore

## Project structure

- `ui/` Compose screens
- `viewmodel/` app state + business flow
- `data/` Firebase repositories
- `model/` core entities and distance mapping
- `bluetooth/` BLE scanner
- `utils/` orientation and motion helpers

## Setup

1. Create a Firebase project and add an Android app with package `com.example.whereuat`.
2. Download `google-services.json` into `app/google-services.json`.
3. Enable **Anonymous Authentication** in Firebase Auth.
4. Create Firestore in native mode.
5. Open in Android Studio (JDK 17), sync Gradle, run on a BLE-capable device.
6. Grant Bluetooth runtime permissions on Android 12+.

## Notes

- The app intentionally avoids GPS, map SDKs, and coordinate display.
- Direction is estimated and approximate, designed for quick friend finding rather than exact positioning.
