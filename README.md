# Cubie Software

Cubie is a small ESP32-C3 companion device with an OLED face, BLE phone connection, Google Maps navigation display, app/call notifications, music lyric sync, sleep controls, touch input, buzzer feedback, and an 8-LED WS2812-style glyph ring.

This repository contains both sides of the software:

- `src/` - ESP32-C3 PlatformIO firmware for the Cubie device.
- `cubie_app/` - Android companion app for BLE setup, notifications, calls, faces, petting animation, navigation, and lyrics.
- `chronos_source/` - local Chronos ESP32 library source used by the firmware.
- `scripts/` - helper scripts for the firmware toolchain.
- `navigation_icon_simulator/` - helper project for navigation icon/layout testing.
- `*.md` docs - wiring, setup, navigation layouts, product notes, and software planning.

## Firmware

Open this folder with PlatformIO and build the default environment:

```bash
pio run -e cubie_c3
```

The default firmware configuration is in `platformio.ini`.

Important hardware assumptions:

- ESP32-C3 board
- SH110X OLED display
- touch input
- buzzer
- optional WS2812/NeoPixel 8-LED glyph ring

See `connections.md` for wiring notes.

## Android App

The companion app lives in `cubie_app/`.

Build the debug APK with:

```bash
cd cubie_app
./gradlew assembleDebug
```

On Windows, use:

```powershell
cd cubie_app
.\gradlew.bat assembleDebug
```

The app manages:

- BLE connection to Cubie
- notification allowlist
- blocked callers
- OLED face/emotion selection
- petting animation selection
- Google Maps notification parsing for navigation
- music/lyrics sync
- sleep timer and device controls

## Navigation Display

Cubie shows Google Maps navigation data on the OLED:

- top right: next turn distance
- bottom left: remaining trip time
- bottom right: total remaining distance

Speed is intentionally not shown on the Cubie navigation screen.

## Glyph Ring

V1 supports an 8-LED circular RGB ring. The firmware uses the glyph ring for simple device states such as BLE connection, notifications, calls, and navigation activity.

## Notes

Generated build folders, Android local SDK paths, logs, APKs, PlatformIO output, and tool downloads are intentionally ignored.
