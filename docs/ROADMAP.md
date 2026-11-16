# Playora — Roadmap

How Playora grows, phase by phase. Each phase ends with a working APK from CI.

## Phase 0 — Skeleton ✅ (v0.1.0)

- [x] Repo, README, MIT license, `.gitignore`
- [x] Gradle setup (Kotlin 2.0, Compose, Media3, Coil, Navigation)
- [x] Bottom navigation: Video / Music / Photos
- [x] Material 3 theme (light + dark) and launcher icon
- [x] `MediaStoreRepository` — reads device videos, music, photos
- [x] `PlayerManager` — Media3 ExoPlayer wrapper
- [x] `PhotoEditor` — rotate, flip, crop, brightness, contrast
- [x] CI builds a debug APK on every push to `main`

## Phase 1 — Browse + play video ✅ (v0.2.0)

- [x] Permission screen (media permissions, Android 13+ style)
- [x] Video list: thumbnails (Coil), title, duration, date — newest first
- [x] Video player screen: PlayerView, play/pause, seek bar, fullscreen, double-tap seek
- [x] Music list: title, artist, duration
- [x] Remember last played position per video

## Phase 2 — Music like a real player ✅ (v0.2.0)

- [x] Background playback with `MediaSessionService`
- [x] Notification controls (play/pause/next/previous)
- [x] Playlists: create, add songs, delete
- [x] Favorites (heart)
- [x] Queue screen
- [x] Sleep timer

## Phase 3 — Photos ✅ (v0.2.0)

- [x] Photo grid with thumbnails
- [x] Full-screen viewer: pinch zoom, swipe between photos
- [x] Editor screen: crop (square), rotate, flip
- [x] Sliders: brightness, contrast, saturation
- [x] Filters: grayscale, sepia, warm, cool
- [x] Save edited copy to gallery (keeps the original)

## Phase 4 — Polish ✅ (v0.2.0, except release signing)

- [x] Equalizer (Media3 audio effects) + presets
- [x] Video gestures: brightness/volume swipe
- [x] Search across video / music / photos
- [x] Dark theme polish, app icon final, Bengali language support
- [ ] Release signing (release keystore + GitHub secrets, same as MyCloud/ProStrem)
- [ ] v1.0.0 release on GitHub

## Decisions still open (for Yasin)

1. **Name** — `Playora` is the working name. Easy to change before v1.0.
2. **Online streaming?** — Phase 4 at the earliest; local files first.
3. **Photo editor scope** — basic edits in Phase 3; stickers/text later if wanted.
4. **Release signing** — create a Playora release keystore in Termux when v1.0 is near (same flow as MyCloud).
