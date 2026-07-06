#ifndef DISPLAY_MANAGER_H
#define DISPLAY_MANAGER_H

#include <Arduino.h>
#include <Wire.h>
#include "CubieModels.h"
#include "CubieConfig.h"

#if CUBIE_USE_OLED
#include <Adafruit_GFX.h>
#include <Adafruit_SH110X.h>
#endif

class DisplayManager
{
public:
#if CUBIE_USE_OLED
    DisplayManager();
#endif
    void begin();
    void render(const DisplayFrame &frame);
    void setBrightness(CubieBrightness brightness);
    void playEyesSequence(bool restart);
    void playEyesSequence(bool restart, uint8_t startAnimation, uint64_t mask);
    bool isEyesSequencePlaying() const;

private:
    bool _oledReady = false;
    uint8_t _activeFacePack = 0;
    uint32_t _activeFaceMask = 0;
    uint16_t _faceFrame = 0;
    uint16_t _eyesSequenceFrame = 0;
    uint8_t _eyesSequenceAnimation = 0;
    uint8_t _eyesSequenceStartAnimation = 0;
    uint64_t _eyesSequenceMask = 1ULL;
    bool _eyesSequencePlaying = false;
    bool _eyesSequenceLoop = true;

#if CUBIE_USE_OLED
    Adafruit_SH1106G _display;
#endif

    void renderSerial(const DisplayFrame &frame);
    void renderOled(const DisplayFrame &frame);
    void renderEyes(const DisplayFrame &frame);
    void renderMenu(const DisplayFrame &frame);
    void renderApps(const DisplayFrame &frame);
    void renderStopwatch(const DisplayFrame &frame);
    void renderTorch(const DisplayFrame &frame);
    void renderNavigation(const DisplayFrame &frame);
    void renderNotificationPreview(const DisplayFrame &frame);
    void renderNotifications(const DisplayFrame &frame);
    void renderIncomingCall(const DisplayFrame &frame);
    void renderStatus(const DisplayFrame &frame);
    void renderBatteryAlert(const DisplayFrame &frame);
    void renderChargeComplete(const DisplayFrame &frame);
    void renderBleSplash(const char *state);
    void renderSettings(const DisplayFrame &frame);
    void renderPerfectedEyes(bool loop);
    void drawNavigationIcon(NavigationIcon icon);
    void drawBridgeBase();
    void drawStraight();
    void drawLeft();
    void drawRight();
    void drawSlightLeft();
    void drawSlightRight();
    void drawUTurnLeft();
    void drawUTurnRight();
    void drawRoundabout();
    void drawArrived();
    void drawBridgeStraight();
    void drawBridgeLeft();
    void drawBridgeRight();
    void drawBridgeUTurn();
    void centerText(const char *text, int16_t y, uint8_t size = 1);
    void centerText(const String &text, int16_t y, uint8_t size = 1);
    void thickLine(int16_t x0, int16_t y0, int16_t x1, int16_t y1);
    void arrowHead(int16_t x, int16_t y, int8_t dx, int8_t dy);
    void printDefault(const char *text, int16_t x, int16_t y, uint8_t size = 1);
    void printSlightlyBigger(const char *text, int16_t x, int16_t baseline);
    void printSlightlyBiggerRight(const char *text, int16_t rightEdge, int16_t baseline);
    String shortText(const String &text, uint8_t maxChars) const;
    String compactTimeText(const String &text) const;
    const char *navigationLabel(NavigationIcon icon) const;
    const char *screenName(CubieScreen screen) const;
    uint8_t nextEnabledFacePack(uint8_t start, uint64_t mask) const;
};

#endif
