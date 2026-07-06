#include "GlyphManager.h"

void GlyphManager::begin(uint8_t pin, uint8_t count)
{
    _pin = pin;
    _count = count;
#if CUBIE_USE_GLYPH
    _pixels.updateType(NEO_GRB + NEO_KHZ800);
    _pixels.updateLength(count);
    _pixels.setPin(pin);
    _pixels.begin();
    _pixels.setBrightness(CubieConfig::GlyphBrightness);
    _pixels.clear();
    _pixels.show();
    _ready = true;
    setPattern(GlyphPattern::Idle);
#else
    (void)pin;
    (void)count;
    _ready = false;
#endif
}

void GlyphManager::update()
{
    if (!_ready || millis() - _lastFrameAt < 33)
    {
        return;
    }
    _lastFrameAt = millis();

    if (_pattern == GlyphPattern::Notification && millis() - _patternStartedAt > 1400)
    {
        setPattern(GlyphPattern::Idle);
    }
    else if ((_pattern == GlyphPattern::Connected || _pattern == GlyphPattern::Disconnected) && millis() - _patternStartedAt > 2200)
    {
        setPattern(GlyphPattern::Idle);
    }

    switch (_pattern)
    {
    case GlyphPattern::Connected:
        renderPulse(color(0, 120, 255), 4, 24, 900);
        break;
    case GlyphPattern::Disconnected:
        renderBlink(color(255, 20, 0), 700);
        break;
    case GlyphPattern::Notification:
        renderPulse(color(255, 220, 120), 6, 34, 360);
        break;
    case GlyphPattern::Call:
        renderPulse(color(0, 180, 255), 8, 42, 620);
        break;
    case GlyphPattern::NavigationLeft:
        renderNavigation(color(255, 150, 0), 5, 7);
        break;
    case GlyphPattern::NavigationRight:
        renderNavigation(color(255, 150, 0), 1, 3);
        break;
    case GlyphPattern::NavigationUTurn:
        renderNavigation(color(255, 150, 0), 4, 7);
        break;
    case GlyphPattern::NavigationRoundabout:
        renderPulse(color(255, 150, 0), 5, 28, 800);
        break;
    case GlyphPattern::NavigationStraight:
        renderNavigation(color(255, 150, 0), 0, 2);
        break;
    case GlyphPattern::Idle:
    default:
        renderSolid(color(0, 0, 0));
        break;
    }
}

void GlyphManager::setConnected(bool connected)
{
    setPattern(connected ? GlyphPattern::Connected : GlyphPattern::Disconnected);
}

void GlyphManager::showNotification()
{
    if (_pattern != GlyphPattern::Call)
    {
        setPattern(GlyphPattern::Notification);
    }
}

void GlyphManager::setCall(bool active)
{
    setPattern(active ? GlyphPattern::Call : GlyphPattern::Idle);
}

void GlyphManager::setNavigation(bool active, const String &directions)
{
    if (!active)
    {
        setPattern(GlyphPattern::Idle);
        return;
    }

    String text = lowerText(directions);
    if (text.indexOf("roundabout") >= 0)
    {
        setPattern(GlyphPattern::NavigationRoundabout);
    }
    else if (text.indexOf("u-turn") >= 0 || text.indexOf("uturn") >= 0)
    {
        setPattern(GlyphPattern::NavigationUTurn);
    }
    else if (text.indexOf("left") >= 0)
    {
        setPattern(GlyphPattern::NavigationLeft);
    }
    else if (text.indexOf("right") >= 0)
    {
        setPattern(GlyphPattern::NavigationRight);
    }
    else
    {
        setPattern(GlyphPattern::NavigationStraight);
    }
}

void GlyphManager::clear()
{
    setPattern(GlyphPattern::Idle);
}

void GlyphManager::setPattern(GlyphPattern pattern)
{
    if (_pattern == pattern)
    {
        return;
    }
    _pattern = pattern;
    _patternStartedAt = millis();
    _lastFrameAt = 0;
}

void GlyphManager::renderSolid(uint32_t ledColor)
{
#if CUBIE_USE_GLYPH
    for (uint8_t i = 0; i < _count; i++)
    {
        _pixels.setPixelColor(i, ledColor);
    }
    _pixels.show();
#else
    (void)ledColor;
#endif
}

void GlyphManager::renderPulse(uint32_t ledColor, uint8_t minBrightness, uint8_t maxBrightness, uint16_t periodMs)
{
#if CUBIE_USE_GLYPH
    uint16_t phase = (millis() - _patternStartedAt) % periodMs;
    uint16_t half = periodMs / 2;
    uint8_t brightness = phase < half
                             ? map(phase, 0, half, minBrightness, maxBrightness)
                             : map(phase - half, 0, half, maxBrightness, minBrightness);
    _pixels.setBrightness(brightness);
    renderSolid(ledColor);
#else
    (void)ledColor;
    (void)minBrightness;
    (void)maxBrightness;
    (void)periodMs;
#endif
}

void GlyphManager::renderBlink(uint32_t ledColor, uint16_t periodMs)
{
    renderSolid(((millis() - _patternStartedAt) % periodMs) < (periodMs / 2) ? ledColor : color(0, 0, 0));
}

void GlyphManager::renderNavigation(uint32_t ledColor, int8_t start, int8_t end)
{
#if CUBIE_USE_GLYPH
    _pixels.setBrightness(CubieConfig::GlyphBrightness);
    _pixels.clear();
    for (int8_t i = start; i <= end; i++)
    {
        setPixel(static_cast<uint8_t>(i), ledColor);
    }
    _pixels.show();
#else
    (void)ledColor;
    (void)start;
    (void)end;
#endif
}

void GlyphManager::setPixel(uint8_t index, uint32_t ledColor)
{
#if CUBIE_USE_GLYPH
    if (index < _count)
    {
        _pixels.setPixelColor(index, ledColor);
    }
#else
    (void)index;
    (void)ledColor;
#endif
}

uint32_t GlyphManager::color(uint8_t red, uint8_t green, uint8_t blue)
{
#if CUBIE_USE_GLYPH
    return _pixels.Color(red, green, blue);
#else
    (void)red;
    (void)green;
    (void)blue;
    return 0;
#endif
}

String GlyphManager::lowerText(const String &value) const
{
    String lowered = value;
    lowered.toLowerCase();
    return lowered;
}
