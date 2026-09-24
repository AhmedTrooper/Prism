# Prism

High-performance Android video player powered by libmpv. Clean UI, universal audio codecs.

## Overview

Prism is an ad-free, high-performance Android video player that brings together the playback power of [libmpv](https://github.com/mpv-player/mpv) and FFmpeg with a clean, streamlined mobile interface.

Stock video players and popular commercial apps often fail to play common audio formats like AC3, E-AC3, and DTS due to licensing limits or require messy external codec packs. Commercial players are also packed with intrusive ads and tracking. Prism solves both problems: it plays all common audio and video formats directly with full hardware acceleration, zero ads, and no tracking.

* **Application ID**: `com.ahmedtrooper.prism`
* **Target SDK**: Android 16 (API 36)
* **Minimum SDK**: Android 6.0 (API 23)

---

## Native Engine & APK Size

Prism bundles prebuilt native shared libraries (`.so`) compiled directly from libmpv, FFmpeg, and libass.

### Why the APK is ~33 MB instead of ~9 MB

A basic Android shell with only Kotlin code and AndroidX UI components is roughly 9 MB. However, a 9 MB APK has no native decoders or player engine. Attempting to play any media results in a runtime `UnsatisfiedLinkError` because `libmpv` is missing.

To play videos reliably with universal format support, Prism packages the complete FFmpeg and libmpv engine:

| Shared Library | Uncompressed Size | Purpose |
| :--- | :--- | :--- |
| `libavcodec.so` | 12.4 MB | Full audio & video decoders (AC3, E-AC3, DTS, TrueHD, H.264, HEVC, AV1, VP9) |
| `libmpv.so` | 8.3 MB | Core playback pipeline, frame presentation, and script bindings |
| `libavfilter.so` | 4.3 MB | Audio and video filtering graph |
| `libavformat.so` | 3.4 MB | Container demuxers (MKV, MP4, WebM, AVI, TS, FLV) |
| `libc++_shared.so` | 1.4 MB | LLVM C++ standard library runtime |
| `libswscale.so` | 1.2 MB | Video scaling and color conversion |
| `libavutil.so` | 740 KB | Core media utility functions |
| `libswresample.so` | 100 KB | Audio resampling and channel layout mapping |
| `libprism.so` | 21 KB | Direct JNI bridge connecting `com.ahmedtrooper.prism.PrismLib` to the playback engine |

### APK Size by Architecture (Release)

Thanks to Gradle ABI splits, users only download the native library built for their specific device architecture:

* **ARM64 (`arm64-v8a`)**: ~33 MB *(standard for modern Android phones)*
* **ARMv7 (`armeabi-v7a`)**: ~30 MB *(older 32-bit devices)*
* **x86_64**: ~39 MB *(64-bit emulators and Chromebooks)*
* **x86**: ~35 MB *(32-bit emulators)*
* **Universal APK**: ~130 MB *(includes all 4 architectures)*

---

## Storage Flavors & Distribution

Prism provides two product flavors to support different distribution models and Android storage requirements:

| Flavor | Minimum SDK | Storage Permission | Target Audience | Build Command |
| :--- | :--- | :--- | :--- | :--- |
| **`default`** | Android 6.0 (API 23) | Scoped Storage (`READ_MEDIA_VIDEO`) | Google Play Store | `./gradlew assembleDefaultRelease` |
| **`allstorage`** | Android 11 (API 30) | `MANAGE_EXTERNAL_STORAGE` | Direct Download / F-Droid / Sideload | `./gradlew assembleAllstorageRelease` |

* **`default`**: Follows modern Android scoped storage policies and is fully compliant with Google Play Store guidelines.
* **`allstorage`**: Replaces the obsolete legacy storage bypass (`api29`) with Android 11+'s `MANAGE_EXTERNAL_STORAGE` permission, granting unrestricted access to SD cards, USB OTG, hidden dotfiles, and external subtitles without downgrading API level.

---

## Features

### 1. Clean Media Interface
* **Branded Splash Screen**: Instant zero-delay window launch with signature geometric crystalline Prism emblem and bold typography, smoothly fading into the media library.
* **Cobalt Blue Header**: Solid blue Action Bar (`#007AFF`) and dark blue status bar (`#0066D6`).
* **Media Folder Browser**:
  * Clean folder rows showing folder title and video count in parentheses, such as `Camera (27)`.
  * Distinctive blue `NEW` badges for recently added items.
  * 72dp indented hairline dividers for clean folder separation.
* **Video List & Grid Views**:
  * 16:9 thumbnail previews with corner duration pills.
  * Resume progress bar along the bottom of thumbnails for partially watched videos.
  * Resolution tags (`1080p`, `4K`, `720p`, `480p`) alongside file size and date.
  * Toggle between vertical list view and 2-column grid view.
* **Instant Search**: Real-time filtering across folders and video files with proper case-insensitive matching.
* **Item Menu (3-dots)**: Play, Play from beginning, Share, and detailed file Properties (path, resolution, duration, size, date).
* **Polished Layout (Sep 2026)**: Unified 4dp spacing grid, 0.5dp hairline dividers, card borders with stroke, 48dp touch targets, focus/hover/ripple states for TV and mouse, and clean text truncation (NEW badges, counts, and resolution tags).

### 2. Player Controls & Decoder Switcher
* **Decoder Toggle**: Instant switching between `HW+` (Hardware Plus), `HW` (MediaCodec), and `SW` (Software FFmpeg) directly from the top bar.
* **Aspect Ratio Control**: Switch between Fit to Screen, Stretch, Crop / Zoom, 16:9, 4:3, and 21:9.
* **Audio & Subtitle Track Selectors**:
  * Switch between multiple embedded audio tracks.
  * Select embedded or external subtitles (`.srt`, `.ass`, `.vtt`).
  * Subtitle sync offset adjustment (±0.1s increments).
  * High-quality stylized subtitle rendering via `libass`.
* **Background & PiP**: Support for Picture-in-Picture windowing and background audio playback.
* **A-B Loop Repeat**: Tap the A-B loop button to set point A, point B, and repeat sections continuously, or long-press to reset.
* **Resume Playback**: Automatically stores and restores playback positions for every video.

### 3. Gesture Controls
* **Left Vertical Swipe**: Screen brightness control with percentage on-screen display.
* **Right Vertical Swipe**: Media volume control with **200% software audio boost** and bright orange indicator.
* **Horizontal Swipe**: Fast and smooth seeking with target position and time delta preview card (`[ +00:15 ]`).
* **Double-Tap**: 10-second skip forward (right side) or backward (left side).
* **Two-Finger Pinch-to-Zoom & Pan**: Zoom video from 50% to 400% with smooth two-finger frame repositioning.
* **Single Tap**: Toggle controls overlay on and off.

### 4. Codec & Format Compatibility
* **Audio**: AC3 (Dolby Digital), E-AC3 (Dolby Digital Plus), DTS, DTS-HD, TrueHD, AAC, MP3, FLAC, Opus, Vorbis.
* **Video**: H.264 / AVC, H.265 / HEVC (8-bit and 10-bit HDR), AV1, VP9, VP8, MPEG-4, VC-1.
* **Containers**: MKV, MP4, WebM, AVI, TS, M2TS, MOV, FLV, OGG.

---

## Building from Source

### Prerequisites
* Android Studio Ladybug or newer
* Android SDK 36 (Build Tools 36.0.0)
* Android NDK 27.2.12479018 (specified in `ndk.properties`)
* JDK 17 or JDK 21

### Build Debug APK
```bash
# Standard Google Play build (Scoped Storage)
./gradlew assembleDefaultDebug

# All Storage build (unrestricted filesystem access)
./gradlew assembleAllstorageDebug
```
Output APKs will be in `app/build/outputs/apk/<flavor>/debug/`:
* `app-default-arm64-v8a-debug.apk`
* `app-allstorage-arm64-v8a-debug.apk`

### Build Release APK
```bash
# Standard Google Play build
./gradlew assembleDefaultRelease

# All Storage build
./gradlew assembleAllstorageRelease
```
Output APKs will be in `app/build/outputs/apk/<flavor>/release/`:
* `app-default-arm64-v8a-release-unsigned.apk`
* `app-allstorage-arm64-v8a-release-unsigned.apk`

### Compiling Native Dependencies
To recompile `libmpv`, `ffmpeg`, and related libraries from source, follow the instructions in [`buildscripts/README.md`](buildscripts/README.md).

---

## License
Prism is licensed under the terms of the GNU General Public License v3 or later. See [LICENSE](LICENSE) for details.
