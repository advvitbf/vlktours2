#ifndef GLYPH_MANAGER_H
#define GLYPH_MANAGER_H

#include <Arduino.h>
#include "CubieConfig.h"

#if CUBIE_USE_GLYPH
#include <Adafruit_NeoPixel.h>
#endif

enum class GlyphPattern
{
    Idle,
    Connected,
    Disconnected,
    Notification,
    Call,
    NavigationStraight,
    NavigationLeft,
    NavigationRight,
    NavigationUTurn,
    NavigationRoundabout
};

class GlyphManager
{
public:
    void begin(uint8_t pin, uint8_t count);
    void update();
    void setConnected(bool connected);
    void showNotification();
    void setCall(bool active);
    void setNavigation(bool active, const String &directions);
    void clear();

private:
    GlyphPattern _pattern = GlyphPattern::Idle;
    uint32_t _patternStartedAt = 0;
    uint32_t _lastFrameAt = 0;
    uint8_t _pin = 0;
    uint8_t _count = 0;
    bool _ready = false;

#if CUBIE_USE_GLYPH
    Adafruit_NeoPixel _pixels;
#endif

    void setPattern(GlyphPattern pattern);
    void renderSolid(uint32_t color);
    void renderPulse(uint32_t color, uint8_t minBrightness, uint8_t maxBrightness, uint16_t periodMs);
    void renderBlink(uint32_t color, uint16_t periodMs);
    void renderNavigation(uint32_t color, int8_t start, int8_t end);
    void setPixel(uint8_t index, uint32_t color);
    uint32_t color(uint8_t red, uint8_t green, uint8_t blue);
    String lowerText(const String &value) const;
};

#endif
