#include "CubieApp.h"

namespace
{
uint64_t validFaceMask()
{
    if (CubieConfig::FacePackCount >= 64)
    {
        return 0xFFFFFFFFFFFFFFFFULL;
    }
    return (1ULL << CubieConfig::FacePackCount) - 1;
}

NavigationIcon classifyNavigationIcon(const String &title, const String &directions)
{
    String text = title + " " + directions;
    text.toLowerCase();

    const bool hasBridge = text.indexOf("bridge") >= 0;
    const bool hasLeft = text.indexOf("left") >= 0;
    const bool hasRight = text.indexOf("right") >= 0;
    const bool hasUTurn = text.indexOf("u-turn") >= 0 || text.indexOf("uturn") >= 0 || text.indexOf("make a u") >= 0;

    if (text.indexOf("arriv") >= 0 || text.indexOf("destination reached") >= 0)
    {
        return NavigationIcon::Arrived;
    }

    if (text.indexOf("roundabout") >= 0)
    {
        return NavigationIcon::Roundabout;
    }

    if (text.indexOf("keep left") >= 0 || text.indexOf("keep right") >= 0)
    {
        return NavigationIcon::Straight;
    }

    if (hasBridge)
    {
        if (hasUTurn)
        {
            return NavigationIcon::BridgeUTurn;
        }
        if (hasLeft)
        {
            return NavigationIcon::BridgeLeft;
        }
        if (hasRight)
        {
            return NavigationIcon::BridgeRight;
        }
        return NavigationIcon::BridgeStraight;
    }

    if (hasUTurn)
    {
        return hasRight ? NavigationIcon::UTurnRight : NavigationIcon::UTurnLeft;
    }

    if (text.indexOf("slight left") >= 0)
    {
        return NavigationIcon::SlightLeft;
    }
    if (text.indexOf("slight right") >= 0)
    {
        return NavigationIcon::SlightRight;
    }

    if (hasLeft)
    {
        return NavigationIcon::Left;
    }
    if (hasRight)
    {
        return NavigationIcon::Right;
    }

    return NavigationIcon::Straight;
}
}

void CubieApp::begin(ChronosESP32 &chronos, DisplayManager &display)
{
    _battery.cubiePercent = CubieConfig::InitialBatteryPercent;
    loadSavedSettings();
    _lastInteractionAt = millis();
    chronos.setBattery(_battery.cubiePercent, _battery.cubieCharging);
    chronos.setNotifyBattery(true);
    updateScreen();
    display.render(makeDisplayFrame(chronos));
}

void CubieApp::loop(ChronosESP32 &chronos, DisplayManager &display)
{
    if (_sleepEnabled && _sleepTimerMinutes > 0 && !_sleeping && !_navigationActive && _screen == CubieScreen::Eyes)
    {
        uint32_t idleMs = static_cast<uint32_t>(_sleepTimerMinutes) * 60000UL;
        if (millis() - _lastInteractionAt >= idleMs)
        {
            enterSleep();
        }
    }

    if (_eyesAnimationUntil > 0 && millis() > _eyesAnimationUntil)
    {
        _eyesAnimationUntil = 0;
        _lastRenderAt = 0;
    }

    if (_bleSplashUntil > 0 && millis() > _bleSplashUntil)
    {
        _bleSplashUntil = 0;
        _manualScreenActive = false;
        updateScreen();
    }

    if (_screen == CubieScreen::NotificationPreview && millis() > _notificationPreviewUntil)
    {
        _notificationPreviewUntil = 0;
        _manualScreenActive = false;
        updateScreen();
    }

    uint16_t renderIntervalMs = 500;
    if (_screen == CubieScreen::Eyes)
    {
        renderIntervalMs = 125;
    }
    else if (_screen == CubieScreen::Stopwatch && _stopwatchRunning)
    {
        renderIntervalMs = 31;
    }
    if (millis() - _lastRenderAt > renderIntervalMs)
    {
        _lastRenderAt = millis();
        display.render(makeDisplayFrame(chronos));
    }
}

void CubieApp::onConnectionChanged(bool connected)
{
    _bleConnected = connected;
    if (!connected)
    {
        _screen = CubieScreen::BleDisconnected;
        _manualScreenActive = true;
        _bleSplashUntil = millis() + 2000;
        return;
    }

    if (!_navigationActive)
    {
        _screen = CubieScreen::BleConnected;
        _manualScreenActive = true;
        _bleSplashUntil = millis() + 2000;
        return;
    }
    updateScreen();
}

void CubieApp::onNotification(const Notification &notification)
{
    pushNotification(notification);

    if (_navigationActive)
    {
        Serial.println("[Cubie] Notification stored; navigation stays on screen");
        return;
    }

    if (!_navigationActive && !_incomingCallActive)
    {
        _manualScreenActive = false;
        CubieNotification latest;
        latest.app = notification.app;
        latest.title = notification.title;
        latest.message = notification.message;
        latest.time = notification.time;
        latest.icon = notification.icon;
        uint32_t previewMs = isMusicNotification(latest) ? CubieConfig::MusicPreviewMs : CubieConfig::NotificationPreviewMs;
        _notificationPreviewUntil = millis() + previewMs;
        _screen = CubieScreen::NotificationPreview;
    }
}

void CubieApp::onRinger(const String &caller, bool active)
{
    if (_navigationActive)
    {
        if (active && _missedCallCount < 99)
        {
            _missedCallCount++;
        }
        Serial.println("[Cubie] Call event stored/ignored; navigation stays on screen");
        return;
    }

    _incomingCallActive = active;
    _callerName = caller;
    updateScreen();
}

void CubieApp::onConfiguration(ChronosESP32 &chronos, Config config, uint32_t a, uint32_t b)
{
    (void)b;

    switch (config)
    {
    case CF_NAV_DATA:
        _navigationActive = a == 1;
        updateScreen();
        break;
    case CF_PBAT:
        _battery.phoneCharging = a == 1;
        _battery.phonePercent = chronos.getPhoneBattery();
        updateScreen();
        break;
    default:
        break;
    }
}

void CubieApp::onTouch(TouchEvent event, DisplayManager &display)
{
    _lastInteractionAt = millis();

    if (_sleeping)
    {
        wakeToEyes();
        return;
    }

    if (_navigationActive)
    {
        Serial.println("[Cubie] Touch ignored; navigation is locked");
        return;
    }

    switch (event)
    {
    case TouchEvent::SingleTap:
        if (_screen == CubieScreen::Stopwatch)
        {
            toggleStopwatch();
        }
        else if (_screen == CubieScreen::Eyes)
        {
            triggerEyesAnimation(display, false);
        }
        else
        {
            moveNextScreen();
        }
        break;
    case TouchEvent::DoubleTap:
        if (_screen == CubieScreen::Eyes)
        {
            openMenu();
        }
        else if (_screen == CubieScreen::Stopwatch)
        {
            if (_stopwatchRunning)
            {
                Serial.println("[Cubie] Double tap ignored on running stopwatch");
            }
            else
            {
                resetStopwatch();
            }
        }
        else
        {
            selectCurrentScreen();
        }
        break;
    case TouchEvent::TripleTap:
        if (_screen == CubieScreen::Eyes)
        {
            Serial.println("[Cubie] Triple tap ignored on eyes");
        }
        else if (_screen == CubieScreen::Menu)
        {
            saveAndExitToEyes();
        }
        else if (_screen == CubieScreen::Stopwatch || _screen == CubieScreen::Torch)
        {
            backToApps();
        }
        else
        {
            backToMenu();
        }
        break;
    case TouchEvent::LongPress:
        if (_screen == CubieScreen::Eyes && _sleepEnabled)
        {
            enterSleep();
        }
        else
        {
            saveAndExitToEyes();
        }
        break;
    }
}

bool CubieApp::buzzerEnabled() const
{
    return _buzzerEnabled;
}

void CubieApp::setFacePack(uint8_t index)
{
    _facePackIndex = index % CubieConfig::FacePackCount;
    if ((_facePackMask & (1ULL << _facePackIndex)) == 0)
    {
        _facePackMask |= 1ULL << _facePackIndex;
    }
    _lastRenderAt = 0;
    Serial.print("[Cubie] Face pack set by app: ");
    Serial.println(_facePackIndex);
}

void CubieApp::setFacePackMask(uint64_t mask)
{
    uint64_t allowedMask = validFaceMask();
    _facePackMask = mask & allowedMask;
    if (_facePackMask == 0)
    {
        _facePackMask = allowedMask;
    }

    if ((_facePackMask & (1ULL << _facePackIndex)) == 0)
    {
        for (uint8_t i = 0; i < CubieConfig::FacePackCount; i++)
        {
            if ((_facePackMask & (1ULL << i)) != 0)
            {
                _facePackIndex = i;
                break;
            }
        }
    }

    _lastRenderAt = 0;
    saveFaceSettings();
    Serial.println("[Cubie] Face pack mask set by app");
}

void CubieApp::setPettingFacePack(uint8_t index)
{
    _pettingFacePackIndex = index % CubieConfig::FacePackCount;
    _lastRenderAt = 0;
    saveFaceSettings();
    Serial.print("[Cubie] Petting face pack set by app: ");
    Serial.println(_pettingFacePackIndex);
}

void CubieApp::setFaceSettings(uint64_t mask, uint8_t pettingIndex)
{
    uint64_t allowedMask = validFaceMask();
    _pettingFacePackIndex = pettingIndex % CubieConfig::FacePackCount;
    _facePackMask = mask & allowedMask;
    _facePackMask &= ~(1ULL << _pettingFacePackIndex);
    if (_facePackMask == 0)
    {
        for (uint8_t i = 0; i < CubieConfig::FacePackCount; i++)
        {
            if (i != _pettingFacePackIndex)
            {
                _facePackMask = 1ULL << i;
                break;
            }
        }
    }

    if ((_facePackMask & (1ULL << _facePackIndex)) == 0)
    {
        for (uint8_t i = 0; i < CubieConfig::FacePackCount; i++)
        {
            if ((_facePackMask & (1ULL << i)) != 0)
            {
                _facePackIndex = i;
                break;
            }
        }
    }

    _lastRenderAt = 0;
    saveFaceSettings();
    Serial.print("[Cubie] Face settings saved petting=");
    Serial.println(_pettingFacePackIndex);
}

void CubieApp::setDeviceSettings(bool sleepEnabled, bool buzzerEnabled, CubieBrightness brightness, uint8_t sleepTimerMinutes)
{
    _sleepEnabled = sleepEnabled;
    _buzzerEnabled = buzzerEnabled;
    _brightness = brightness;
    _sleepTimerMinutes = sleepTimerMinutes;
    _lastInteractionAt = millis();
    _lastRenderAt = 0;
    saveDeviceSettings();
    Serial.print("[Cubie] Device settings saved sleep=");
    Serial.print(_sleepEnabled ? "ON" : "OFF");
    Serial.print(" buzzer=");
    Serial.print(_buzzerEnabled ? "ON" : "OFF");
    Serial.print(" brightness=");
    Serial.print(static_cast<uint8_t>(_brightness));
    Serial.print(" sleepTimer=");
    Serial.println(_sleepTimerMinutes);
}

void CubieApp::loadSavedSettings()
{
    uint64_t allowedMask = validFaceMask();
    _preferencesReady = _preferences.begin("cubie", false);
    if (!_preferencesReady)
    {
        Serial.println("[Cubie] Preferences unavailable; using default faces");
        return;
    }

    uint64_t savedMask = _preferences.getULong64("faceMask", _facePackMask) & allowedMask;
    uint8_t savedPetting = _preferences.getUChar("petFace", _pettingFacePackIndex) % CubieConfig::FacePackCount;

    if (savedMask == 0)
    {
        savedMask = allowedMask;
    }

    _facePackMask = savedMask;
    _pettingFacePackIndex = savedPetting;
    _sleepEnabled = _preferences.getBool("sleep", _sleepEnabled);
    _buzzerEnabled = _preferences.getBool("buzzer", _buzzerEnabled);
    _sleepTimerMinutes = _preferences.getUChar("sleepTm", _sleepTimerMinutes);
    uint8_t savedBrightness = _preferences.getUChar("bright", static_cast<uint8_t>(_brightness));
    if (savedBrightness <= static_cast<uint8_t>(CubieBrightness::High))
    {
        _brightness = static_cast<CubieBrightness>(savedBrightness);
    }

    if ((_facePackMask & (1ULL << _facePackIndex)) == 0)
    {
        for (uint8_t i = 0; i < CubieConfig::FacePackCount; i++)
        {
            if ((_facePackMask & (1ULL << i)) != 0)
            {
                _facePackIndex = i;
                break;
            }
        }
    }

    Serial.println("[Cubie] Loaded face mask");
    Serial.print("[Cubie] Loaded petting face: ");
    Serial.println(_pettingFacePackIndex);
    Serial.print("[Cubie] Loaded settings sleep=");
    Serial.print(_sleepEnabled ? "ON" : "OFF");
    Serial.print(" buzzer=");
    Serial.print(_buzzerEnabled ? "ON" : "OFF");
    Serial.print(" brightness=");
    Serial.print(static_cast<uint8_t>(_brightness));
    Serial.print(" sleepTimer=");
    Serial.println(_sleepTimerMinutes);
}

void CubieApp::saveFaceSettings()
{
    if (!_preferencesReady)
    {
        _preferencesReady = _preferences.begin("cubie", false);
    }
    if (!_preferencesReady)
    {
        Serial.println("[Cubie] Preferences unavailable; face settings not saved");
        return;
    }
    _preferences.putULong64("faceMask", _facePackMask);
    _preferences.putUChar("petFace", _pettingFacePackIndex);
}

void CubieApp::saveDeviceSettings()
{
    if (!_preferencesReady)
    {
        _preferencesReady = _preferences.begin("cubie", false);
    }
    if (!_preferencesReady)
    {
        Serial.println("[Cubie] Preferences unavailable; device settings not saved");
        return;
    }
    _preferences.putBool("sleep", _sleepEnabled);
    _preferences.putBool("buzzer", _buzzerEnabled);
    _preferences.putUChar("bright", static_cast<uint8_t>(_brightness));
    _preferences.putUChar("sleepTm", _sleepTimerMinutes);
}

uint32_t CubieApp::stopwatchElapsedMs() const
{
    if (_stopwatchRunning)
    {
        return _stopwatchStoredMs + (millis() - _stopwatchStartedAt);
    }

    return _stopwatchStoredMs;
}

void CubieApp::toggleStopwatch()
{
    if (_stopwatchRunning)
    {
        _stopwatchStoredMs += millis() - _stopwatchStartedAt;
        _stopwatchRunning = false;
        Serial.println("[Cubie] Stopwatch stopped");
    }
    else
    {
        _stopwatchStartedAt = millis();
        _stopwatchRunning = true;
        Serial.println("[Cubie] Stopwatch started");
    }

    _lastRenderAt = 0;
}

void CubieApp::resetStopwatch()
{
    _stopwatchStoredMs = 0;
    _stopwatchStartedAt = millis();
    _lastRenderAt = 0;
    Serial.println("[Cubie] Stopwatch reset");
}

void CubieApp::toggleTorch()
{
    _torchOn = !_torchOn;
    _lastRenderAt = 0;
    Serial.print("[Cubie] Torch ");
    Serial.println(_torchOn ? "ON" : "OFF");
}

void CubieApp::simulateNavigation(bool active, const String &title, const String &directions, const String &distance, const String &eta, const String &speed)
{
    _simulatedNavigationData = active;
    _navigationActive = active;
    _simulatedNavigation.active = active;
    _simulatedNavigation.isNavigation = active;
    _simulatedNavigation.title = title;
    _simulatedNavigation.directions = directions;
    _simulatedNavigation.distance = distance;
    _simulatedNavigation.eta = eta;
    _simulatedNavigation.speed = speed;
    updateScreen();
}

void CubieApp::simulateNotification(const String &app, const String &title, const String &message)
{
    Notification notification;
    notification.app = app;
    notification.title = title;
    notification.message = message;
    notification.time = "SIM";
    notification.icon = 0;
    onNotification(notification);
}

void CubieApp::simulateIncomingCall(const String &caller, bool active)
{
    onRinger(caller, active);
}

void CubieApp::simulateCubieBattery(uint8_t percent, bool charging)
{
    _battery.cubiePercent = percent;
    _battery.cubieCharging = charging;
    updateScreen();
}

void CubieApp::pushNotification(const Notification &notification)
{
    CubieNotification &slot = _notifications[_nextNotificationSlot];
    slot.app = notification.app;
    slot.title = notification.title;
    slot.message = notification.message;
    slot.time = notification.time;
    slot.icon = notification.icon;

    _nextNotificationSlot = (_nextNotificationSlot + 1) % CubieConfig::NotificationHistorySize;
    if (_notificationCount < CubieConfig::NotificationHistorySize)
    {
        _notificationCount++;
    }
}

bool CubieApp::isMusicNotification(const CubieNotification &notification) const
{
    String app = notification.app;
    app.toLowerCase();
    return app.indexOf("spotify") >= 0 ||
           app.indexOf("apple music") >= 0 ||
           app.indexOf("yt music") >= 0 ||
           app.indexOf("youtube music") >= 0 ||
           app.indexOf("amazon music") >= 0 ||
           app == "music";
}

void CubieApp::updateScreen()
{
    if (_navigationActive)
    {
        _sleeping = false;
        _notificationPreviewUntil = 0;
        _screen = CubieScreen::Navigation;
        return;
    }

    if (_sleeping)
    {
        _screen = CubieScreen::Sleep;
        return;
    }

    if (_incomingCallActive)
    {
        _screen = CubieScreen::IncomingCall;
        return;
    }

    if (_battery.cubiePercent <= CubieConfig::LowBatteryWarningPercent)
    {
        _screen = CubieScreen::BatteryAlert;
        return;
    }

    if (_manualScreenActive)
    {
        return;
    }

    _screen = CubieScreen::Eyes;
}

void CubieApp::moveNextScreen()
{
    _manualScreenActive = true;

    switch (_screen)
    {
    case CubieScreen::Eyes:
        _screen = CubieScreen::Eyes;
        _manualScreenActive = false;
        break;
    case CubieScreen::Menu:
        _menuIndex = (_menuIndex + 1) % 5;
        break;
    case CubieScreen::Apps:
        _appsIndex = (_appsIndex + 1) % 2;
        break;
    case CubieScreen::Stopwatch:
        break;
    case CubieScreen::Torch:
        toggleTorch();
        break;
    case CubieScreen::Notifications:
        if (_notificationCount > 0)
        {
            _notificationViewIndex = (_notificationViewIndex + 1) % _notificationCount;
        }
        break;
    case CubieScreen::Settings:
        _settingsIndex = (_settingsIndex + 1) % 3;
        _settingsSelectedFlashUntil = 0;
        break;
    case CubieScreen::Status:
        break;
    default:
        openMenu();
        break;
    }

    Serial.println("[Cubie] Touch single tap: next screen");
}

void CubieApp::selectCurrentScreen()
{
    Serial.println("[Cubie] Touch double tap: select");

    if (_screen == CubieScreen::Menu)
    {
        switch (_menuIndex)
        {
        case 0:
            saveAndExitToEyes();
            break;
        case 1:
            _screen = CubieScreen::Apps;
            _appsIndex = 0;
            break;
        case 2:
            _screen = CubieScreen::Notifications;
            _notificationViewIndex = 0;
            break;
        case 3:
            _screen = CubieScreen::Status;
            break;
        case 4:
            _screen = CubieScreen::Settings;
            _settingsIndex = 0;
            break;
        }
    }
    else if (_screen == CubieScreen::Apps)
    {
        switch (_appsIndex)
        {
        case 0:
            _screen = CubieScreen::Stopwatch;
            break;
        case 1:
            _screen = CubieScreen::Torch;
            break;
        }
    }
    else if (_screen == CubieScreen::Torch)
    {
        Serial.println("[Cubie] Double tap ignored on torch");
    }
    else if (_screen == CubieScreen::Settings)
    {
        _settingsSelectedFlashUntil = millis() + 900;

        if (_settingsIndex == 0)
        {
            cycleBrightness();
            Serial.println("[Cubie] Brightness cycled");
        }
        else if (_settingsIndex == 1)
        {
            _sleepEnabled = !_sleepEnabled;
            saveDeviceSettings();
            Serial.print("[Cubie] Sleep setting: ");
            Serial.println(_sleepEnabled ? "ON" : "OFF");
        }
        else if (_settingsIndex == 2)
        {
            _buzzerEnabled = !_buzzerEnabled;
            saveDeviceSettings();
            Serial.print("[Cubie] Buzzer setting: ");
            Serial.println(_buzzerEnabled ? "ON" : "OFF");
        }
    }
    else if (_screen == CubieScreen::Status)
    {
        Serial.println("[Cubie] Select has no action on status");
    }
    else if (_screen == CubieScreen::Notifications)
    {
        Serial.println("[Cubie] Select has no action on this branch yet");
    }
}

void CubieApp::openMenu()
{
    Serial.println("[Cubie] Menu opened");
    _sleeping = false;
    _manualScreenActive = true;
    _notificationPreviewUntil = 0;
    _screen = CubieScreen::Menu;
}

void CubieApp::backToMenu()
{
    Serial.println("[Cubie] Back to menu");
    _sleeping = false;
    _manualScreenActive = true;
    _notificationPreviewUntil = 0;
    _screen = CubieScreen::Menu;
}

void CubieApp::backToApps()
{
    Serial.println("[Cubie] Back to apps");
    _sleeping = false;
    _manualScreenActive = true;
    _notificationPreviewUntil = 0;
    _screen = CubieScreen::Apps;
}

void CubieApp::saveAndExitToEyes()
{
    Serial.println("[Cubie] Touch save/exit to eyes");
    _sleeping = false;
    _manualScreenActive = false;
    _incomingCallActive = false;
    _notificationPreviewUntil = 0;
    _screen = CubieScreen::Eyes;
}

void CubieApp::enterSleep()
{
    Serial.println("[Cubie] Sleep active; screen blanked");
    _sleeping = true;
    _manualScreenActive = false;
    _incomingCallActive = false;
    _notificationPreviewUntil = 0;
    _screen = CubieScreen::Sleep;
}

void CubieApp::wakeToEyes()
{
    Serial.println("[Cubie] Wake to eyes");
    _sleeping = false;
    _manualScreenActive = false;
    _lastInteractionAt = millis();
    _screen = CubieScreen::Eyes;
}

void CubieApp::triggerEyesAnimation(DisplayManager &display, bool restart)
{
    Serial.println(restart ? "[Cubie] Eyes animation restarted" : "[Cubie] Eyes animation started");
    uint64_t pettingMask = 1ULL << _pettingFacePackIndex;
    display.playEyesSequence(restart, _pettingFacePackIndex, pettingMask);
    _eyesAnimationUntil = 0;
    _lastInteractionAt = millis();
    _lastRenderAt = 0;
}

void CubieApp::cycleBrightness()
{
    switch (_brightness)
    {
    case CubieBrightness::Low:
        _brightness = CubieBrightness::Medium;
        break;
    case CubieBrightness::Medium:
        _brightness = CubieBrightness::High;
        break;
    case CubieBrightness::High:
        _brightness = CubieBrightness::Low;
        break;
    }
    saveDeviceSettings();
}

DisplayFrame CubieApp::makeDisplayFrame(ChronosESP32 &chronos)
{
    DisplayFrame frame;
    frame.screen = _screen;
    frame.battery = _battery;
    frame.bleConnected = _bleConnected;
    frame.callerName = _callerName;
    frame.missedCallCount = _missedCallCount;
    frame.menuIndex = _menuIndex;
    frame.appsIndex = _appsIndex;
    frame.settingsIndex = _settingsIndex;
    frame.settingsSelectedFlash = millis() < _settingsSelectedFlashUntil;
    frame.eyesReacting = _screen == CubieScreen::Eyes && millis() < _eyesAnimationUntil;
    frame.facePackIndex = frame.eyesReacting ? _pettingFacePackIndex : _facePackIndex;
    frame.facePackMask = frame.eyesReacting ? (1ULL << _pettingFacePackIndex) : _facePackMask;
    frame.sleepEnabled = _sleepEnabled;
    frame.buzzerEnabled = _buzzerEnabled;
    frame.brightness = _brightness;
    frame.stopwatch.elapsedMs = stopwatchElapsedMs();
    frame.stopwatch.running = _stopwatchRunning;
    frame.torch.on = _torchOn;
    frame.touchLocked = _navigationActive;

    if (_notificationCount > 0)
    {
        if (_notificationViewIndex >= _notificationCount)
        {
            _notificationViewIndex = 0;
        }

        uint8_t offset = _screen == CubieScreen::Notifications ? _notificationViewIndex : 0;
        uint8_t slot = (_nextNotificationSlot + CubieConfig::NotificationHistorySize - 1 - offset) % CubieConfig::NotificationHistorySize;
        frame.notification = _notifications[slot];
        frame.notificationIndex = offset;
        frame.notificationCount = _notificationCount;
    }
    
    Navigation nav = _simulatedNavigationData ? _simulatedNavigation : chronos.getNavigation();
    frame.navigation.title = nav.title;
    frame.navigation.directions = nav.directions;
    frame.navigation.distance = nav.distance;
    frame.navigation.eta = nav.eta;
    frame.navigation.speed = nav.speed;
    frame.navigation.icon = classifyNavigationIcon(nav.title, nav.directions);

    return frame;
}
