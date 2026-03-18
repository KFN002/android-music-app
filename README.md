# Minimal Music (Android)

A minimalistic local music player for Android with smooth animations, queue control, random play, shuffle, playlists, album/folder browsing, and local-file playback.

## Features

- 🎵 Plays local audio files using ExoPlayer.
- 📁 Scans phone storage with MediaStore and extracts songs marked as music.
- 💿 Supported formats (common): **MP3, FLAC, WAV, OGG, M4A, AAC**.
- 🌍 Basic text-encoding normalization for metadata (Russian, English, and mixed encodings).
- ✨ Minimal dark UI with animated title pulse and song-change transitions.
- 🎧 Custom neon launcher logo for a standalone app look.
- 📱 Responsive Compose layout.
- 🔀 Shuffle mode + instant random-song play.
- 🧾 Queue management (add, remove, clear, play from queue).
- 📚 Browsing tabs for **All Songs**, **Albums**, **Folders (directories)**, **Playlists**, and **Queue**.
- ❤️ Favorites support and auto playlists (Favorites / Recently Added / Long Mix).
- 🎚️ One-tap launch to Android system equalizer panel.
- ⏮️ Skip previous / next and restart current song from beginning.

## Project structure

- `app/src/main/java/com/example/minimalmusic/MainActivity.kt` — UI, tabs, controls, permission flow, animations.
- `app/src/main/java/com/example/minimalmusic/data/MusicRepository.kt` — storage scan, song extraction, folder detection.
- `app/src/main/java/com/example/minimalmusic/player/MusicViewModel.kt` — playback, queue, shuffle/random, playlists/favorites state.
- `app/src/main/java/com/example/minimalmusic/util/TextEncodingUtils.kt` — encoding cleanup helpers.

---

## Install and run on an Android phone

### 1) Requirements

- Android Studio (latest stable recommended)
- Android SDK installed
- Android phone with Android 8.0+ (API 26+)
- USB cable (or wireless debugging)

### 2) Open the project

1. Open Android Studio.
2. Click **Open** and select this repository folder (`android-music-app`).
3. Wait for Gradle sync to finish.

### 3) Enable phone developer options

On your Android phone:

1. Open **Settings → About phone**.
2. Tap **Build number** 7 times to enable Developer Options.
3. Go to **Settings → Developer options**.
4. Enable **USB debugging**.

### 4) Connect device

1. Connect your phone via USB.
2. Accept the RSA debugging prompt on the phone ("Allow USB debugging").
3. In Android Studio, choose your device from the run target dropdown.

### 5) Build and install

- Press **Run ▶** in Android Studio.
- Select the `app` configuration.
- The app will install and open on your phone.

### 6) First launch permissions

- On first launch, grant **music/storage access** permission.
- The app scans local storage and builds song/albums/folders/playlists views.

---

## Manual APK install (optional)

If you want to build an APK and install manually:

1. In Android Studio: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
2. Locate generated APK in `app/build/outputs/apk/debug/`.
3. Transfer APK to phone and install (enable "Install unknown apps" for your file manager if required).

---

## Notes

- The app plays files indexed by Android MediaStore. If songs do not appear, open a file manager/music app once so Android media scanner refreshes indexing.
- On Android 13+ the app requests `READ_MEDIA_AUDIO`; on Android 12 and lower it requests `READ_EXTERNAL_STORAGE`.
- Equalizer availability depends on vendor ROM/device support.
