#ifndef CUBIE_CONFIG_H
#define CUBIE_CONFIG_H

#include <Arduino.h>

namespace CubieConfig
{
constexpr const char *DeviceName = "Cubie";

constexpr uint8_t NotificationHistorySize = 5;
constexpr uint32_t NotificationPreviewMs = 3000;
constexpr uint32_t MusicPreviewMs = 30000;
constexpr uint8_t FacePackCount = 61;
constexpr uint8_t DefaultPettingFacePackIndex = 0;

constexpr uint8_t InitialBatteryPercent = 80;
constexpr uint8_t LowBatteryWarningPercent = 10;
constexpr uint8_t CriticalBatteryWarningPercent = 5;

constexpr uint16_t TouchDebounceMs = 50;
constexpr uint16_t MultiTapWindowMs = 450;
constexpr uint16_t LongPressMs = 900;

constexpr uint8_t TouchPin = 4;
constexpr uint8_t BuzzerPin = 5;
constexpr uint8_t GlyphPin = 6;
constexpr uint8_t GlyphLedCount = 8;
constexpr uint8_t GlyphBrightness = 28;
constexpr uint8_t BuzzerOnLevel = LOW;
constexpr uint8_t BuzzerOffLevel = HIGH;

#ifndef CUBIE_SIMULATOR
#define CUBIE_SIMULATOR 0
#endif

#ifndef CUBIE_USE_OLED
#define CUBIE_USE_OLED 0
#endif

#ifndef CUBIE_USE_GLYPH
#define CUBIE_USE_GLYPH 0
#endif

constexpr uint8_t OledWidth = 128;
constexpr uint8_t OledHeight = 64;
constexpr int8_t OledResetPin = -1;
constexpr uint8_t OledAddress = 0x3C;
}

#endif
