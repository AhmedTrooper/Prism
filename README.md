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
* **MX-Style Bottom Navigation**: Bottom bar with three tabs — Local (folder browser), Streams (online video library), and Me (settings, app info, library rescan). Tabs keep their state across orientation changes and back-stack restoration.
* **Branded Splash Screen**: Instant zero-delay window launch with signature geometric crystalline Prism emblem and bold typography, smoothly fading into the media library.
* **Cobalt Blue Header**: Solid blue Action Bar (`#007AFF`) and dark blue status bar (`#0066D6`).

### 1c. Global Material 3 Theme
* **Single Source of Truth**: One `Theme.Prism` style extends `Theme.Material3.DayNight.NoActionBar` and is consumed by every activity — `MainActivity` (Library), `PlayerActivity` (Player), `FilePickerActivity` (File Picker), `PreferenceActivity` (Settings), `AboutActivity` (About) — through dedicated variants (`Theme.Prism.Library`, `Theme.Prism.Player`, `Theme.Prism.FilePicker`, `Theme.Prism.Preferences`).
* **Brand Token Palette**: `prism_primary` (`#007AFF` light / `#409CFF` dark), `prism_surface`, `prism_surface_container`, `prism_on_surface`, `prism_on_surface_variant`, `prism_outline`, and matching `prism_*_container` family — every color resolves through the active DayNight palette with zero hard-coded hexes in layouts.
* **Dialog & Popup Migration**: Every `AlertDialog.Builder` was replaced with `MaterialAlertDialogBuilder`, so all dialogs (single-choice option lists, message dialogs, the Open URL prompt, advanced/slider/decimal/playlist/track selectors) pick up the Material 3 surface, corner radius, and cobalt blue text button accent for free.
* **Theme Overlays**: `materialAlertDialogTheme`, `alertDialogTheme`, `bottomSheetDialogTheme`, `popupMenuStyle`, `actionOverflowMenuStyle`, `materialButtonStyle`, `materialButtonOutlinedStyle`, `autoCompleteTextViewStyle`, and `textInputStyle` are all rebound to `Widget.Prism.*` styles so any future AppCompat or Material widget renders on-brand without per-call configuration.
* **Custom Drawables**: `prism_dialog_background.xml` and `prism_popup_background.xml` provide rounded 28dp surfaces with proper tonal elevation for non-Material surfaces that still need to match.
* **Runtime Selection**: Pick `System Default (Adaptive)`, `Light`, or `Dark` from the **App Theme** row in the Me tab. The choice is persisted via `AppCompatDelegate.setDefaultNightMode` and survives process death.
* **Media Folder Browser (Authentic MX Style)**:
  * Two-line folder hierarchy: bold folder title on top and item count below (`27 videos` / `1 video`).
  * Custom directory folder glyphs: embossed camera glyph for `Camera`/`DCIM`, viewfinder glyph for `Screenshots`, and download arrow for `Download`.
  * Corner red circular badge on folder icons indicating count of newly added videos.
  * 72dp indented hairline dividers matching text alignment.
  * Inside folder view cleanly displays the folder title alone without redundant counts in the top bar.
* **Video List & Grid Views**:
  * 16:9 thumbnail previews with corner duration pills.
  * Authentic red `NEW` pill badges (`#E53935`) on video thumbnails.
  * Resume progress bar along the bottom of thumbnails for partially watched videos.
  * Resolution tags (`1080p`, `4K`, `720p`, `480p`) alongside file size and date.
  * Toggle between vertical list view and 2-column grid view.
  * Massive folder performance optimizations: background thread filtering, dynamic heap-proportional LruCache, automatic thumbnail job cancellation on view recycling, and viewholder pre-caching.
* **Instant Search**: Real-time filtering across folders and video files with proper case-insensitive matching.
* **Branded Loading & Searching Indicator**: Features the animated crystalline Prism logo with smooth breathing luminescence during media library scans, folder transitions, and search queries, plus stylized Prism placeholders inside video cards while thumbnails load.
* **Item Menu & Structured Properties**: Play, Play from beginning, Share, and structured Properties card (File details with exact byte counts and Media specs).
* **Polished Layout (Sep 2026)**: Unified 4dp spacing grid, 0.5dp hairline dividers, card borders with stroke, 48dp touch targets, focus/hover/ripple states for TV and mouse, and clean text truncation.

### 1b. Online Streams Library
* **JSON-Backed Catalog**: Categories and videos are stored in app-private JSON via `OnlineStreamManager`, with thread-safe load/save and format detection from URL extension.
* **Category Browsing**: Tap a category in the Streams tab to open its videos. Long-press reveals delete; categories with subcategories require explicit confirm before descendant deletion.
* **Video Playback**: Tap a video to open it in `PlayerActivity`. Rescan, add, and delete operations are persisted on the same JSON file.
* **Unit Tested**: 13 JUnit tests cover load/save round-trips, CRUD on categories and videos, format detection from URLs, descendant deletion semantics, and persistence across re-reads.

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
* Gradle 9.8.0 (managed by the wrapper — `./gradlew` will fetch it on first run)

### Build Toolchain

| Component | Version | Purpose |
| :--- | :--- | :--- |
| Gradle | 9.8.0 | Build automation (`gradle/wrapper/gradle-wrapper.properties`) |
| Android Gradle Plugin | 9.4.1 | Android build pipeline (root `build.gradle`) |
| Kotlin | 2.4.20 | Source compilation and JVM target |
| compileSdk / targetSdk | 36 (Android 16) | Latest Android features and scoped storage |
| minSdk | 23 (Android 6.0 Marshmallow) | Broad device coverage while staying on a current security baseline |

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
Prism does **not** ship prebuilt native binaries in this repository — the `app/src/main/libs/` and `app/src/main/jniLibs/` directories are intentionally gitignored. The Gradle build picks them up automatically once they exist, but a fresh clone will not produce a working APK until the native libraries are compiled and copied into place.

To build `libmpv`, `ffmpeg`, and related libraries from source, follow the instructions in [`buildscripts/README.md`](buildscripts/README.md). The short version on Linux/macOS is:

```bash
cd buildscripts
./download.sh         # installs SDK, NDK, and source tarballs
./buildall.sh --arch arm64 mpv   # builds for arm64-v8a (add other archs as needed)
```

After that finishes, the `.so` files land in `app/src/main/libs/<abi>/` and `./gradlew assembleDefaultDebug` will produce a working APK.

### Run Unit Tests
```bash
./gradlew :app:testDefaultDebugUnitTest
```
Reports land in `app/build/reports/tests/testDefaultDebugUnitTest/index.html`. The current suite covers the online streams catalog, A-B loop math, media browser filtering, and shared utilities.

---

## License
* **Original wrapper code** (mpv-android by Ilya Zhuravlev and sfan5): [LICENSE](LICENSE) — MIT License, preserved verbatim with all original authors.
* **Prism original code** (Md. Ramjan Miah, https://github.com/removet-v): [LICENSE-APACHE](LICENSE-APACHE) — Apache License, Version 2.0.
* **Compiled Application (APK)**: Distributed under the terms of the GNU General Public License v3.0 or later because the built APK bundles GPLv3-licensed `libmpv` and FFmpeg shared libraries. The source code itself stays under MIT / Apache 2.0; only the compiled binary that links against libmpv and FFmpeg falls under GPLv3.
* See [LICENSE](LICENSE), [LICENSE-APACHE](LICENSE-APACHE), and [docs/licenses.html](docs/licenses.html) for complete component attribution and license text.
