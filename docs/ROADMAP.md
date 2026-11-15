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

## Phase 1 — Browse + play video

- [ ] Permission screen (media permissions, Android 13+ style)
- [ ] Video list: thumbnails (Coil), title, duration, date — newest first
- [ ] Video player screen: PlayerView, play/pause, seek bar, fullscreen, double-tap seek
- [ ] Music list: title, artist, duration
- [ ] Remember last played position per video

## Phase 2 — Music like a real player

- [ ] Background playback with `MediaSessionService`
- [ ] Notification controls (play/pause/next/previous)
- [ ] Playlists: create, add songs, reorder, delete
- [ ] Favorites (heart) + Recently played
- [ ] Queue screen + shuffle / repeat modes
- [ ] Sleep timer

## Phase 3 — Photos

- [ ] Photo grid with thumbnails, albums by folder/date
- [ ] Full-screen viewer: pinch zoom, swipe between photos
- [ ] Editor screen: crop (free + square), rotate, flip
- [ ] Sliders: brightness, contrast, saturation
- [ ] Filters: grayscale, sepia, warm, cool
- [ ] Save edited copy to gallery (keeps the original)

## Phase 4 — Polish

- [ ] Equalizer (Media3 audio effects) + presets
- [ ] Video gestures: brightness/volume swipe, playback speed
- [ ] Search across video / music / photos
- [ ] Dark theme polish, app icon final, Bengali language support
- [ ] Release signing (release keystore + GitHub secrets, same as MyCloud/ProStrem)
- [ ] v1.0.0 release on GitHub

## Decisions still open (for Yasin)

1. **Name** — `Playora` is the working name. Easy to change before v1.0.
2. **Online streaming?** — Phase 4 at the earliest; local files first.
3. **Photo editor scope** — basic edits in Phase 3; stickers/text later if wanted.
4. **Release signing** — create a Playora release keystore in Termux when v1.0 is near (same flow as MyCloud).
