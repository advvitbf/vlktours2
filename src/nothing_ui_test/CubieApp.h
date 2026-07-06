#ifndef CUBIE_APP_H
#define CUBIE_APP_H

#include <Arduino.h>
#include <ChronosESP32.h>
#include <Preferences.h>
#include "CubieConfig.h"
#include "CubieModels.h"
#include "DisplayManager.h"

class CubieApp
{
public:
    void begin(ChronosESP32 &chronos, DisplayManager &display);
    void loop(ChronosESP32 &chronos, DisplayManager &display);

    void onConnectionChanged(bool connected);
    void onNotification(const Notification &notification);
    void onRinger(const String &caller, bool active);
    void onConfiguration(ChronosESP32 &chronos, Config config, uint32_t a, uint32_t b);
    void onTouch(TouchEvent event, DisplayManager &display);
    bool buzzerEnabled() const;
    void setFacePack(uint8_t index);
    void setFacePackMask(uint64_t mask);
    void setPettingFacePack(uint8_t index);
    void setFaceSettings(uint64_t mask, uint8_t pettingIndex);
    void setDeviceSettings(bool sleepEnabled, bool buzzerEnabled, CubieBrightness brightness, uint8_t sleepTimerMinutes);

    void simulateNavigation(bool active, const String &title, const String &directions, const String &distance, const String &eta, const String &speed);
    void simulateNotification(const String &app, const String &title, const String &message);
    void simulateIncomingCall(const String &caller, bool active);
    void simulateCubieBattery(uint8_t percent, bool charging);

private:
    CubieScreen _screen = CubieScreen::Eyes;
    CubieBattery _battery;
    CubieNotification _notifications[CubieConfig::NotificationHistorySize];
    uint8_t _notificationCount = 0;
    uint8_t _nextNotificationSlot = 0;

    bool _bleConnected = false;
    bool _navigationActive = false;
    bool _incomingCallActive = false;
    bool _simulatedNavigationData = false;
    bool _manualScreenActive = false;
    bool _sleepEnabled = true;
    bool _buzzerEnabled = true;
    bool _sleeping = false;
    uint8_t _sleepTimerMinutes = 0;
    uint8_t _menuIndex = 0;
    uint8_t _appsIndex = 0;
    uint8_t _notificationViewIndex = 0;
    uint8_t _settingsIndex = 0;
    CubieBrightness _brightness = CubieBrightness::Medium;
    uint8_t _missedCallCount = 0;
    String _callerName;
    Navigation _simulatedNavigation;
    uint32_t _notificationPreviewUntil = 0;
    uint32_t _settingsSelectedFlashUntil = 0;
    uint32_t _eyesAnimationUntil = 0;
    uint8_t _facePackIndex = 0;
    uint64_t _facePackMask = (1ULL << CubieConfig::FacePackCount) - 1;
    uint8_t _pettingFacePackIndex = CubieConfig::DefaultPettingFacePackIndex;
    uint32_t _bleSplashUntil = 0;
    uint32_t _lastRenderAt = 0;
    uint32_t _lastInteractionAt = 0;
    uint32_t _stopwatchStartedAt = 0;
    uint32_t _stopwatchStoredMs = 0;
    bool _stopwatchRunning = false;
    bool _torchOn = false;
    Preferences _preferences;
    bool _preferencesReady = false;

    void loadSavedSettings();
    void saveFaceSettings();
    void saveDeviceSettings();
    void pushNotification(const Notification &notification);
    bool isMusicNotification(const CubieNotification &notification) const;
    void updateScreen();
    uint32_t stopwatchElapsedMs() const;
    void toggleStopwatch();
    void resetStopwatch();
    void toggleTorch();
    void moveNextScreen();
    void selectCurrentScreen();
    void openMenu();
    void backToMenu();
    void backToApps();
    void saveAndExitToEyes();
    void enterSleep();
    void wakeToEyes();
    void triggerEyesAnimation(DisplayManager &display, bool restart);
    void cycleBrightness();
    DisplayFrame makeDisplayFrame(ChronosESP32 &chronos);
};

#endif
