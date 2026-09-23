# Prism

High-performance Android video player powered by libmpv. Clean UI, universal audio codecs.

## Overview

Prism is an ad-free, high-performance Android video player combining the power of [libmpv](https://github.com/mpv-player/mpv) and FFmpeg with the intuitive, streamlined UI and UX of MX Player.

Package: `com.ahmedtrooper.prism`

## Key Features

* **MX Player Style Interface**:
  - Clean edge-to-edge top bar and bottom transport bar with gradient scrims.
  - Dedicated hardware decoder switcher badge (`HW+`, `HW`, `SW`) on the top bar.
  - Aspect ratio switcher (Fit, Stretch, Crop, 16:9, 4:3, 21:9).
  - Picture-in-Picture and background audio playback.

* **Advanced Media Library Browser**:
  - MX Player folder view with automatic video count badges.
  - Video list view with high-performance asynchronous thumbnail caching.
  - Video duration pills, resolution badges (4K, 1080p, 720p, 480p), and file size indicators.
  - Interactive search filtering and view mode switching (Folders / All Videos).
  - Quick network stream loader ("Open URL").

* **Gesture Engine 2.0**:
  - **Left vertical swipe**: Smooth screen brightness control with percentage HUD.
  - **Right vertical swipe**: System volume control plus signature **200% software audio boost** with orange indicator.
  - **Horizontal swipe**: Precision time scrub with center preview card (target time and delta).
  - **Double-tap left/right**: Instant ±10s skip with animated ripple indicators.
  - **Two-finger pinch-to-zoom & pan**: Smooth video zoom from 50% to 400% with real-time percentage badge and two-finger frame panning.
  - **Single-tap**: Responsive control overlay toggle.

* **Universal Codec & Subtitle Support**:
  - Complete audio codec support including AC3, E-AC3, DTS, DTS-HD, TrueHD, and AAC.
  - Advanced ASS/SSA stylized subtitle rendering via `libass`.
  - Dual / secondary subtitle track support with precision ±0.1s sync delay adjustment.

## Building from source

See [`buildscripts/README.md`](buildscripts/README.md) for compiling native libmpv dependencies and building release APKs.
