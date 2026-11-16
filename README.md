# Playora

**Playora** is a modern media player for Android — video, audio and photos in one clean app.

- 🎬 **Video player** — play local videos with a modern, gesture-friendly player
- 🎵 **Audio player** — music with background playback, playlists and queue
- 🖼️ **Photo viewer & editor** — open photos, crop, rotate, adjust brightness and contrast

Built with **Kotlin**, **Jetpack Compose** and **Media3 (ExoPlayer)**. No ads, no trackers, fully offline for local media.

## Status

✅ **v0.2.0 — video, music and photos all working.** See [docs/ROADMAP.md](docs/ROADMAP.md) for the full plan.

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.0 |
| UI | Jetpack Compose (Material 3) |
| Video / audio | Media3 ExoPlayer 1.4 |
| Images | Coil |
| Navigation | Navigation Compose |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

## Get the APK

Every push to `main` builds a debug APK in GitHub Actions — download it from the **Actions** tab → latest run → artifact `playora-debug-apk`. No Android Studio needed.

## Build locally

1. Open the project in Android Studio (Hedgehog or newer).
2. Let Gradle sync, then **Run ▶** on a device or emulator.

Or with the Gradle wrapper / a local Gradle 8.9 install:

```bash
gradle assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
app/src/main/java/com/yasin/playora/
├── MainActivity.kt            # App entry, bottom navigation (Video / Music / Photos)
├── ui/theme/                  # Material 3 theme (Color, Theme)
├── media/                     # LocalMedia model + MediaStore repository
├── player/                    # PlayerManager (Media3 ExoPlayer wrapper)
└── photo/                     # PhotoEditor (crop, rotate, brightness, contrast)
```

## Roadmap

- **Phase 1** — media browsing (all videos / music / photos from the device) + video playback
- **Phase 2** — audio background playback, notification controls, playlists, favorites
- **Phase 3** — photo viewer + basic editor (crop, rotate, filters)
- **Phase 4** — polish: equalizer, gestures, sleep timer, themes

Full plan: [docs/ROADMAP.md](docs/ROADMAP.md)

## License

MIT — see [LICENSE](LICENSE). Copyright (c) 2026 Yasin Adnan.
