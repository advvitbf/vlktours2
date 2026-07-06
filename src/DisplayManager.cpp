#include "DisplayManager.h"

#if CUBIE_USE_OLED
#include <Fonts/FreeSans9pt7b.h>
#include "../final_eye_pack/mochi_final_eye_pack_split.h"
#endif

#if CUBIE_USE_OLED
DisplayManager::DisplayManager()
    : _display(CubieConfig::OledWidth, CubieConfig::OledHeight, &Wire, CubieConfig::OledResetPin)
{
}
#endif

void DisplayManager::begin()
{
    Serial.println("[Display] Serial backend ready");

#if CUBIE_USE_OLED
    _oledReady = _display.begin(CubieConfig::OledAddress, true);
    if (_oledReady)
    {
        _display.clearDisplay();
        _display.setTextColor(SH110X_WHITE);
        _display.display();
        Serial.println("[Display] OLED backend ready");
    }
    else
    {
        Serial.println("[Display] OLED init failed; Serial backend only");
    }
#endif
}

void DisplayManager::render(const DisplayFrame &frame)
{
    renderSerial(frame);

    if (_oledReady)
    {
        renderOled(frame);
    }
}

void DisplayManager::setBrightness(CubieBrightness brightness)
{
#if CUBIE_USE_OLED
    if (!_oledReady)
    {
        return;
    }

    uint8_t contrast = 0x7F;
    switch (brightness)
    {
    case CubieBrightness::Low:
        contrast = 0x20;
        break;
    case CubieBrightness::Medium:
        contrast = 0x7F;
        break;
    case CubieBrightness::High:
        contrast = 0xFF;
        break;
    }

    _display.setContrast(contrast);
#else
    (void)brightness;
#endif
}

void DisplayManager::playEyesSequence(bool restart)
{
#if CUBIE_USE_OLED
    playEyesSequence(restart, 0, (1ULL << CubieConfig::FacePackCount) - 1);
#else
    (void)restart;
#endif
}

void DisplayManager::playEyesSequence(bool restart, uint8_t startAnimation, uint64_t mask)
{
#if CUBIE_USE_OLED
    if (mask == 0)
    {
        mask = 1ULL;
    }

    startAnimation %= CubieConfig::FacePackCount;
    uint8_t selectedStart = nextEnabledFacePack(startAnimation, mask);
    const MochiFinalEyeAnimation &animation = mochiFinalEyeAnimations[_eyesSequenceAnimation];
    bool finished = !_eyesSequencePlaying &&
                    _eyesSequenceFrame + 1 >= animation.frameCount;
    bool changedSelection = _eyesSequenceMask != mask || _eyesSequenceStartAnimation != selectedStart;

    if (restart || finished || changedSelection)
    {
        _eyesSequenceFrame = 0;
        _eyesSequenceAnimation = selectedStart;
        _eyesSequenceStartAnimation = selectedStart;
        _eyesSequenceMask = mask;
    }
    _eyesSequencePlaying = true;
    _eyesSequenceLoop = false;
#else
    (void)restart;
    (void)startAnimation;
    (void)mask;
#endif
}

bool DisplayManager::isEyesSequencePlaying() const
{
    return _eyesSequencePlaying;
}

namespace
{
const char *brightnessLabel(CubieBrightness brightness)
{
    switch (brightness)
    {
    case CubieBrightness::Low:
        return "Bright LOW";
    case CubieBrightness::Medium:
        return "Bright MED";
    case CubieBrightness::High:
        return "Bright HIGH";
    }

    return "Bright MED";
}
}

void DisplayManager::renderSerial(const DisplayFrame &frame)
{
    Serial.println();
    Serial.print("[Cubie] Screen: ");
    Serial.println(screenName(frame.screen));

    switch (frame.screen)
    {
    case CubieScreen::Eyes:
        Serial.println(frame.eyesReacting ? "    -      -" : "    O      O");
        Serial.println();
        Serial.println("      ____");
        break;
    case CubieScreen::Menu:
        Serial.println("# Menu");
        Serial.println(frame.menuIndex == 0 ? "> Eyes" : "  Eyes");
        Serial.println(frame.menuIndex == 1 ? "> Apps" : "  Apps");
        Serial.println(frame.menuIndex == 2 ? "> Notifications" : "  Notifications");
        Serial.println(frame.menuIndex == 3 ? "> Status" : "  Status");
        Serial.println(frame.menuIndex == 4 ? "> Settings" : "  Settings");
        break;
    case CubieScreen::Apps:
        Serial.println("# Apps");
        Serial.println(frame.appsIndex == 0 ? "> Stopwatch" : "  Stopwatch");
        Serial.println(frame.appsIndex == 1 ? "> Torch" : "  Torch");
        break;
    case CubieScreen::Stopwatch:
        Serial.println("# Stopwatch");
        Serial.print(frame.stopwatch.elapsedMs);
        Serial.println(" ms");
        Serial.println(frame.stopwatch.running ? "RUNNING" : "STOPPED");
        break;
    case CubieScreen::Torch:
        Serial.println("# Torch");
        Serial.println(frame.torch.on ? "ON" : "OFF");
        break;
    case CubieScreen::Navigation:
        Serial.print("Distance/title: ");
        Serial.println(frame.navigation.title);
        Serial.print("Directions: ");
        Serial.println(frame.navigation.directions);
        Serial.print("Remaining: ");
        Serial.println(frame.navigation.distance);
        Serial.print("Time: ");
        Serial.println(frame.navigation.eta);
        Serial.print("Speed: ");
        Serial.println(frame.navigation.speed);
        if (frame.missedCallCount > 0)
        {
            Serial.print("Missed calls: ");
            Serial.println(frame.missedCallCount);
        }
        if (frame.touchLocked)
        {
            Serial.println("Touch locked during navigation");
        }
        break;
    case CubieScreen::NotificationPreview:
        Serial.println(frame.notification.app);
        Serial.print(frame.notification.title);
        Serial.println(":");
        Serial.println(frame.notification.message);
        break;
    case CubieScreen::Notifications:
        Serial.println("# Notifications");
        if (frame.notificationCount > 0)
        {
            Serial.print(frame.notificationIndex + 1);
            Serial.print("/");
            Serial.println(frame.notificationCount);
        }
        Serial.println(frame.notification.app.length() > 0 ? frame.notification.app : "No messages");
        Serial.print(frame.notification.title);
        if (frame.notification.title.length() > 0)
        {
            Serial.println(":");
        }
        Serial.println(frame.notification.message);
        break;
    case CubieScreen::IncomingCall:
        Serial.println("Incoming");
        Serial.println(frame.callerName.length() > 0 ? frame.callerName : "Unknown caller");
        Serial.println("Phone icon");
        break;
    case CubieScreen::Status:
        Serial.print("BLE: ");
        Serial.println(frame.bleConnected ? "Connected" : "Disconnected");
        Serial.print("Phone: ");
        Serial.print(frame.battery.phonePercent);
        Serial.println("%");
        Serial.print("Cubie: ");
        Serial.print(frame.battery.cubiePercent);
        Serial.println("%");
        break;
    case CubieScreen::BatteryAlert:
        Serial.println("I AM HUNGRY :[");
        Serial.print(frame.battery.cubiePercent);
        Serial.println("%");
        Serial.println("FEED ME PLEASE");
        break;
    case CubieScreen::ChargeComplete:
        Serial.println("100%");
        Serial.println("I AM FULL");
        Serial.println("DON'T OVERFEED");
        Serial.println("ME");
        break;
    case CubieScreen::BleConnected:
        Serial.println("BLE CONNECTED");
        break;
    case CubieScreen::BleDisconnected:
        Serial.println("BLE DISCONNECTED");
        break;
    case CubieScreen::Sleep:
        Serial.println("[blank]");
        break;
    case CubieScreen::Settings:
        Serial.println("# Settings");
        Serial.println(frame.settingsIndex == 0 ? String("> ") + brightnessLabel(frame.brightness) : String("  ") + brightnessLabel(frame.brightness));
        Serial.println(frame.settingsIndex == 1 ? (frame.sleepEnabled ? "> Sleep ON" : "> Sleep OFF") : (frame.sleepEnabled ? "  Sleep ON" : "  Sleep OFF"));
        Serial.println(frame.settingsIndex == 2 ? (frame.buzzerEnabled ? "> Buzzer ON" : "> Buzzer OFF") : (frame.buzzerEnabled ? "  Buzzer ON" : "  Buzzer OFF"));
        break;
    }
}

void DisplayManager::renderOled(const DisplayFrame &frame)
{
    setBrightness(frame.brightness);
    _display.clearDisplay();
    _display.setTextColor(SH110X_WHITE);
    _display.setFont();
    _display.setTextSize(1);

    switch (frame.screen)
    {
    case CubieScreen::Eyes:
        renderEyes(frame);
        break;
    case CubieScreen::Menu:
        renderMenu(frame);
        break;
    case CubieScreen::Apps:
        renderApps(frame);
        break;
    case CubieScreen::Stopwatch:
        renderStopwatch(frame);
        break;
    case CubieScreen::Torch:
        renderTorch(frame);
        break;
    case CubieScreen::Navigation:
        renderNavigation(frame);
        break;
    case CubieScreen::NotificationPreview:
        renderNotificationPreview(frame);
        break;
    case CubieScreen::Notifications:
        renderNotifications(frame);
        break;
    case CubieScreen::IncomingCall:
        renderIncomingCall(frame);
        break;
    case CubieScreen::Status:
        renderStatus(frame);
        break;
    case CubieScreen::BatteryAlert:
        renderBatteryAlert(frame);
        break;
    case CubieScreen::ChargeComplete:
        renderChargeComplete(frame);
        break;
    case CubieScreen::BleConnected:
        renderBleSplash("CONNECTED");
        break;
    case CubieScreen::BleDisconnected:
        renderBleSplash("DISCONNECTED");
        break;
    case CubieScreen::Settings:
        renderSettings(frame);
        break;
    case CubieScreen::Sleep:
        break;
    }

    _display.display();
}

void DisplayManager::renderEyes(const DisplayFrame &frame)
{
#if CUBIE_USE_OLED
    if (!_eyesSequencePlaying)
    {
        _eyesSequenceAnimation = nextEnabledFacePack(frame.facePackIndex, frame.facePackMask);
        _eyesSequenceFrame = 0;
        _eyesSequenceMask = frame.facePackMask;
        _eyesSequenceStartAnimation = _eyesSequenceAnimation;
        _eyesSequencePlaying = true;
        _eyesSequenceLoop = true;
    }
    else if (_eyesSequenceLoop && _eyesSequenceMask != frame.facePackMask)
    {
        _eyesSequenceAnimation = nextEnabledFacePack(frame.facePackIndex, frame.facePackMask);
        _eyesSequenceFrame = 0;
        _eyesSequenceMask = frame.facePackMask;
        _eyesSequenceStartAnimation = _eyesSequenceAnimation;
    }
#else
    (void)frame;
#endif
    renderPerfectedEyes(_eyesSequenceLoop);
}

void DisplayManager::renderPerfectedEyes(bool loop)
{
#if CUBIE_USE_OLED
    const MochiFinalEyeAnimation &animation = mochiFinalEyeAnimations[_eyesSequenceAnimation];
    _display.drawBitmap(0, 0, animation.frames[_eyesSequenceFrame], MOCHI_FINAL_EYE_WIDTH, MOCHI_FINAL_EYE_HEIGHT, SH110X_WHITE);
    if (!_eyesSequencePlaying)
    {
        return;
    }

    if (_eyesSequenceFrame + 1 < animation.frameCount)
    {
        _eyesSequenceFrame++;
        return;
    }

    uint8_t nextAnimation = nextEnabledFacePack(_eyesSequenceAnimation + 1, _eyesSequenceMask);
    if (nextAnimation == _eyesSequenceStartAnimation)
    {
        if (loop)
        {
            _eyesSequenceAnimation = _eyesSequenceStartAnimation;
            _eyesSequenceFrame = 0;
        }
        else
        {
            _eyesSequencePlaying = false;
            _eyesSequenceLoop = true;
            _eyesSequenceFrame = animation.frameCount - 1;
        }
        return;
    }

    _eyesSequenceAnimation = nextAnimation;
    _eyesSequenceFrame = 0;
#endif
}

void DisplayManager::renderMenu(const DisplayFrame &frame)
{
    const char *labels[] = {
        "Eyes",
        "Apps",
        "Notifications",
        "Status",
        "Settings"};

    _display.setTextSize(1);
    _display.setCursor(0, 0);
    _display.print("# Menu");

    for (uint8_t i = 0; i < 5; i++)
    {
        _display.setCursor(0, 12 + (i * 10));
        _display.print(i == frame.menuIndex ? "> " : "  ");
        _display.print(labels[i]);
    }
}

void DisplayManager::renderApps(const DisplayFrame &frame)
{
    const char *labels[] = {
        "Stopwatch",
        "Torch"};

    _display.setTextSize(1);
    _display.setCursor(0, 0);
    _display.print("# Apps");

    for (uint8_t i = 0; i < 2; i++)
    {
        _display.setCursor(0, 16 + (i * 12));
        _display.print(i == frame.appsIndex ? "> " : "  ");
        _display.print(labels[i]);
    }
}

void DisplayManager::renderStopwatch(const DisplayFrame &frame)
{
    uint32_t elapsed = frame.stopwatch.elapsedMs;
    uint16_t hours = elapsed / 3600000UL;
    uint8_t minutes = (elapsed / 60000UL) % 60;
    uint8_t seconds = (elapsed / 1000UL) % 60;

    char timeText[16];
    snprintf(timeText, sizeof(timeText), "%02u:%02u:%02u", hours, minutes, seconds);

    _display.setTextSize(1);
    _display.setCursor(0, 0);
    _display.print("# Stopwatch");
    _display.setCursor(0, 16);
    _display.print(frame.stopwatch.running ? "RUNNING" : "STOPPED");

    _display.setTextSize(2);
    int16_t x1;
    int16_t y1;
    uint16_t w;
    uint16_t h;
    _display.getTextBounds(timeText, 0, 34, &x1, &y1, &w, &h);
    _display.setCursor((CubieConfig::OledWidth - w) / 2, 34);
    _display.print(timeText);
}

void DisplayManager::renderTorch(const DisplayFrame &frame)
{
    if (frame.torch.on)
    {
        _display.fillRect(0, 0, CubieConfig::OledWidth, CubieConfig::OledHeight, SH110X_WHITE);
        _display.setTextColor(SH110X_BLACK);
        centerText("TORCH ON", 26);
        _display.setTextColor(SH110X_WHITE);
        return;
    }

    centerText("# Torch", 0);
    centerText("OFF", 28, 2);
}

void DisplayManager::renderNavigation(const DisplayFrame &frame)
{
    String turnDistance = frame.navigation.title;
    String totalDistance = shortText(frame.navigation.distance, 8);
    String eta = compactTimeText(frame.navigation.eta);

    printDefault(navigationLabel(frame.navigation.icon), 1, 0);
    printSlightlyBiggerRight(shortText(turnDistance, 6).c_str(), 121, 13);
    drawNavigationIcon(frame.navigation.icon);
    if (eta.length() > 0)
    {
        printDefault(eta.c_str(), 0, 56);
    }
    if (totalDistance.length() > 0)
    {
        printDefault(totalDistance.c_str(), 88, 56);
    }
}

void DisplayManager::renderNotificationPreview(const DisplayFrame &frame)
{
    _display.setTextSize(1);
    _display.setCursor(0, 0);
    _display.print(shortText(frame.notification.app, 16));
    _display.setCursor(0, 16);
    _display.print(shortText(frame.notification.title, 14));
    _display.print(":");
    _display.setCursor(0, 32);
    _display.print(shortText(frame.notification.message, 16));
}

void DisplayManager::renderNotifications(const DisplayFrame &frame)
{
    _display.setTextSize(1);
    _display.setCursor(0, 0);
    _display.print("# Notifications");
    if (frame.notificationCount > 0)
    {
        _display.setCursor(104, 0);
        _display.print(frame.notificationIndex + 1);
        _display.print("/");
        _display.print(frame.notificationCount);
    }

    if (frame.notification.app.length() == 0 && frame.notification.title.length() == 0 && frame.notification.message.length() == 0)
    {
        _display.setCursor(0, 24);
        _display.print("No messages");
        return;
    }

    _display.setCursor(0, 16);
    _display.print(shortText(frame.notification.app, 16));
    _display.setCursor(0, 30);
    _display.print(shortText(frame.notification.title, 14));
    if (frame.notification.title.length() > 0)
    {
        _display.print(":");
    }
    _display.setCursor(0, 44);
    _display.print(shortText(frame.notification.message, 16));
}

void DisplayManager::renderIncomingCall(const DisplayFrame &frame)
{
    centerText("Incoming", 0);
    centerText(frame.callerName.length() > 0 ? frame.callerName : "Unknown", 28);
    centerText("[TEL]", 52);
}

void DisplayManager::renderStatus(const DisplayFrame &frame)
{
    _display.setTextSize(1);
    _display.setCursor(0, 0);
    _display.print(frame.bleConnected ? "BLE Connected" : "BLE Disconnected");
    _display.setCursor(0, 16);
    _display.print("Phone      ");
    _display.print(frame.battery.phonePercent);
    _display.print("%");
    _display.setCursor(0, 32);
    _display.print("Cubie      ");
    _display.print(frame.battery.cubiePercent);
    _display.print("%");
    _display.setCursor(0, 48);
    _display.print("FW        1.1");
}

void DisplayManager::renderBatteryAlert(const DisplayFrame &frame)
{
    centerText("I AM HUNGRY :[", 0);
    centerText(String(frame.battery.cubiePercent) + "%", 28);
    centerText("FEED ME PLEASE", 52);
}

void DisplayManager::renderChargeComplete(const DisplayFrame &frame)
{
    (void)frame;
    centerText("100%", 0);
    centerText("I AM FULL", 18);
    centerText("DON'T OVERFEED", 44);
    centerText("ME", 56);
}

void DisplayManager::renderBleSplash(const char *state)
{
    centerText("BLE", 6, 2);
    centerText(state, 34);
}

void DisplayManager::renderSettings(const DisplayFrame &frame)
{
    const char *labels[] = {
        brightnessLabel(frame.brightness),
        frame.sleepEnabled ? "Sleep ON" : "Sleep OFF",
        frame.buzzerEnabled ? "Buzzer ON" : "Buzzer OFF"};

    _display.setTextSize(1);
    _display.setCursor(0, 0);
    _display.print("# Settings");

    for (uint8_t i = 0; i < 3; i++)
    {
        _display.setCursor(0, 16 + (i * 12));
        if (i == frame.settingsIndex)
        {
            _display.print("> ");
        }
        else
        {
            _display.print("  ");
        }
        _display.print(labels[i]);
    }
}

void DisplayManager::drawNavigationIcon(NavigationIcon icon)
{
    switch (icon)
    {
    case NavigationIcon::Straight:
        drawStraight();
        break;
    case NavigationIcon::Left:
        drawLeft();
        break;
    case NavigationIcon::Right:
        drawRight();
        break;
    case NavigationIcon::SlightLeft:
        drawSlightLeft();
        break;
    case NavigationIcon::SlightRight:
        drawSlightRight();
        break;
    case NavigationIcon::UTurnLeft:
        drawUTurnLeft();
        break;
    case NavigationIcon::UTurnRight:
        drawUTurnRight();
        break;
    case NavigationIcon::Roundabout:
        drawRoundabout();
        break;
    case NavigationIcon::Arrived:
        drawArrived();
        break;
    case NavigationIcon::BridgeStraight:
        drawBridgeStraight();
        break;
    case NavigationIcon::BridgeLeft:
        drawBridgeLeft();
        break;
    case NavigationIcon::BridgeRight:
        drawBridgeRight();
        break;
    case NavigationIcon::BridgeUTurn:
        drawBridgeUTurn();
        break;
    }
}

void DisplayManager::drawBridgeBase()
{
    _display.drawLine(40, 44, 88, 44, SH110X_WHITE);
    _display.drawLine(44, 44, 52, 36, SH110X_WHITE);
    _display.drawLine(52, 36, 60, 44, SH110X_WHITE);
    _display.drawLine(60, 44, 68, 36, SH110X_WHITE);
    _display.drawLine(68, 36, 76, 44, SH110X_WHITE);
    _display.drawLine(76, 44, 84, 36, SH110X_WHITE);
}

void DisplayManager::drawStraight()
{
    thickLine(64, 46, 64, 22);
    arrowHead(64, 18, 0, -1);
}

void DisplayManager::drawLeft()
{
    thickLine(78, 42, 78, 28);
    thickLine(78, 28, 48, 28);
    arrowHead(44, 28, -1, 0);
}

void DisplayManager::drawRight()
{
    thickLine(50, 42, 50, 28);
    thickLine(50, 28, 80, 28);
    arrowHead(84, 28, 1, 0);
}

void DisplayManager::drawSlightLeft()
{
    thickLine(76, 46, 54, 24);
    _display.fillTriangle(50, 20, 54, 31, 61, 24, SH110X_WHITE);
}

void DisplayManager::drawSlightRight()
{
    thickLine(52, 46, 74, 24);
    _display.fillTriangle(78, 20, 67, 24, 74, 31, SH110X_WHITE);
}

void DisplayManager::drawUTurnLeft()
{
    _display.drawCircle(64, 34, 14, SH110X_WHITE);
    _display.drawCircle(64, 34, 13, SH110X_WHITE);
    _display.fillRect(64, 34, 18, 20, SH110X_BLACK);
    thickLine(78, 34, 78, 48);
    arrowHead(50, 34, -1, 0);
}

void DisplayManager::drawUTurnRight()
{
    _display.drawCircle(64, 34, 14, SH110X_WHITE);
    _display.drawCircle(64, 34, 13, SH110X_WHITE);
    _display.fillRect(46, 34, 18, 20, SH110X_BLACK);
    thickLine(50, 34, 50, 48);
    arrowHead(78, 34, 1, 0);
}

void DisplayManager::drawRoundabout()
{
    _display.drawCircle(64, 29, 15, SH110X_WHITE);
    _display.drawCircle(64, 29, 14, SH110X_WHITE);
    arrowHead(78, 20, 1, 0);
}

void DisplayManager::drawArrived()
{
    _display.drawCircle(64, 29, 16, SH110X_WHITE);
    _display.drawCircle(64, 29, 8, SH110X_WHITE);
    _display.fillCircle(64, 29, 3, SH110X_WHITE);
}

void DisplayManager::drawBridgeStraight()
{
    drawBridgeBase();
    thickLine(64, 34, 64, 18);
    arrowHead(64, 16, 0, -1);
}

void DisplayManager::drawBridgeLeft()
{
    drawBridgeBase();
    thickLine(78, 34, 54, 24);
    arrowHead(50, 22, -1, 0);
}

void DisplayManager::drawBridgeRight()
{
    drawBridgeBase();
    thickLine(50, 34, 74, 24);
    arrowHead(78, 22, 1, 0);
}

void DisplayManager::drawBridgeUTurn()
{
    drawBridgeBase();
    _display.drawCircle(64, 25, 10, SH110X_WHITE);
    _display.drawCircle(64, 25, 9, SH110X_WHITE);
    _display.fillRect(64, 25, 14, 12, SH110X_BLACK);
    arrowHead(54, 25, -1, 0);
}

void DisplayManager::centerText(const char *text, int16_t y, uint8_t size)
{
    int16_t x1;
    int16_t y1;
    uint16_t w;
    uint16_t h;
    _display.setTextSize(size);
    _display.getTextBounds(text, 0, y, &x1, &y1, &w, &h);
    _display.setCursor((CubieConfig::OledWidth - w) / 2, y);
    _display.print(text);
}

void DisplayManager::centerText(const String &text, int16_t y, uint8_t size)
{
    centerText(text.c_str(), y, size);
}

void DisplayManager::thickLine(int16_t x0, int16_t y0, int16_t x1, int16_t y1)
{
    _display.drawLine(x0, y0, x1, y1, SH110X_WHITE);
    _display.drawLine(x0, y0 + 1, x1, y1 + 1, SH110X_WHITE);
}

void DisplayManager::arrowHead(int16_t x, int16_t y, int8_t dx, int8_t dy)
{
    if (dx > 0)
    {
        _display.fillTriangle(x, y, x - 8, y - 5, x - 8, y + 5, SH110X_WHITE);
    }
    else if (dx < 0)
    {
        _display.fillTriangle(x, y, x + 8, y - 5, x + 8, y + 5, SH110X_WHITE);
    }
    else if (dy < 0)
    {
        _display.fillTriangle(x, y, x - 5, y + 8, x + 5, y + 8, SH110X_WHITE);
    }
    else
    {
        _display.fillTriangle(x, y, x - 5, y - 8, x + 5, y - 8, SH110X_WHITE);
    }
}

void DisplayManager::printDefault(const char *text, int16_t x, int16_t y, uint8_t size)
{
    _display.setFont();
    _display.setTextSize(size);
    _display.setCursor(x, y);
    _display.print(text);
}

void DisplayManager::printSlightlyBigger(const char *text, int16_t x, int16_t baseline)
{
#if CUBIE_USE_OLED
    _display.setFont(&FreeSans9pt7b);
    _display.setTextSize(1);
    _display.setCursor(x, baseline);
    _display.print(text);
    _display.setFont();
    _display.setTextSize(1);
#else
    (void)text;
    (void)x;
    (void)baseline;
#endif
}

void DisplayManager::printSlightlyBiggerRight(const char *text, int16_t rightEdge, int16_t baseline)
{
#if CUBIE_USE_OLED
    int16_t x1;
    int16_t y1;
    uint16_t w;
    uint16_t h;
    _display.setFont(&FreeSans9pt7b);
    _display.setTextSize(1);
    _display.getTextBounds(text, 0, baseline, &x1, &y1, &w, &h);
    _display.setCursor(rightEdge - w, baseline);
    _display.print(text);
    _display.setFont();
    _display.setTextSize(1);
#else
    (void)text;
    (void)rightEdge;
    (void)baseline;
#endif
}

String DisplayManager::shortText(const String &text, uint8_t maxChars) const
{
    if (text.length() <= maxChars)
    {
        return text;
    }
    return text.substring(0, maxChars);
}

String DisplayManager::compactTimeText(const String &text) const
{
    String compact = text;
    compact.toLowerCase();
    compact.replace("minutes", "m");
    compact.replace("minute", "m");
    compact.replace("mins", "m");
    compact.replace("min", "m");
    compact.replace("hours", "h");
    compact.replace("hour", "h");
    compact.replace("hrs", "h");
    compact.replace("hr", "h");
    compact.replace(" ", "");

    if (compact.length() > 4)
    {
        compact = compact.substring(0, 4);
    }

    return compact;
}

const char *DisplayManager::navigationLabel(NavigationIcon icon) const
{
    switch (icon)
    {
    case NavigationIcon::Straight:
        return "Straight";
    case NavigationIcon::Left:
        return "Left";
    case NavigationIcon::Right:
        return "Right";
    case NavigationIcon::SlightLeft:
        return "Slight L";
    case NavigationIcon::SlightRight:
        return "Slight R";
    case NavigationIcon::UTurnLeft:
        return "U Left";
    case NavigationIcon::UTurnRight:
        return "U Right";
    case NavigationIcon::Roundabout:
        return "Round";
    case NavigationIcon::Arrived:
        return "Arrive";
    case NavigationIcon::BridgeStraight:
        return "Bridge";
    case NavigationIcon::BridgeLeft:
        return "Brdg L";
    case NavigationIcon::BridgeRight:
        return "Brdg R";
    case NavigationIcon::BridgeUTurn:
        return "Brdg U";
    }

    return "Straight";
}

const char *DisplayManager::screenName(CubieScreen screen) const
{
    switch (screen)
    {
    case CubieScreen::Eyes:
        return "Eyes";
    case CubieScreen::Menu:
        return "Menu";
    case CubieScreen::Apps:
        return "Apps";
    case CubieScreen::Stopwatch:
        return "Stopwatch";
    case CubieScreen::Torch:
        return "Torch";
    case CubieScreen::Navigation:
        return "Navigation";
    case CubieScreen::NotificationPreview:
        return "NotificationPreview";
    case CubieScreen::Notifications:
        return "Notifications";
    case CubieScreen::IncomingCall:
        return "IncomingCall";
    case CubieScreen::Status:
        return "Status";
    case CubieScreen::Settings:
        return "Settings";
    case CubieScreen::BatteryAlert:
        return "BatteryAlert";
    case CubieScreen::ChargeComplete:
        return "ChargeComplete";
    case CubieScreen::BleConnected:
        return "BleConnected";
    case CubieScreen::BleDisconnected:
        return "BleDisconnected";
    case CubieScreen::Sleep:
        return "Sleep";
    }

    return "Unknown";
}

uint8_t DisplayManager::nextEnabledFacePack(uint8_t start, uint64_t mask) const
{
    for (uint8_t offset = 0; offset < CubieConfig::FacePackCount; offset++)
    {
        uint8_t index = (start + offset) % CubieConfig::FacePackCount;
        if ((mask & (1ULL << index)) != 0)
        {
            return index;
        }
    }

    return 0;
}

