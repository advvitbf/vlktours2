#ifndef CUBIE_MODELS_H
#define CUBIE_MODELS_H

#include <Arduino.h>
#include <ChronosESP32.h>

enum class CubieScreen
{
    Eyes,
    Menu,
    Navigation,
    NotificationPreview,
    Notifications,
    Apps,
    Stopwatch,
    Torch,
    IncomingCall,
    Status,
    Settings,
    BatteryAlert,
    ChargeComplete,
    BleConnected,
    BleDisconnected,
    Sleep
};

enum class NavigationIcon
{
    Straight,
    Left,
    Right,
    SlightLeft,
    SlightRight,
    UTurnLeft,
    UTurnRight,
    Roundabout,
    Arrived,
    BridgeStraight,
    BridgeLeft,
    BridgeRight,
    BridgeUTurn
};

enum class TouchEvent
{
    SingleTap,
    DoubleTap,
    TripleTap,
    LongPress
};

enum class CubieBrightness : uint8_t
{
    Low,
    Medium,
    High
};

struct CubieNotification
{
    String app;
    String title;
    String message;
    String time;
    int icon = 0;
};

struct CubieBattery
{
    uint8_t cubiePercent = 0;
    bool cubieCharging = false;
    uint8_t phonePercent = 0;
    bool phoneCharging = false;
};

struct CubieNavigationView
{
    String title;
    String directions;
    String distance;
    String eta;
    String speed;
    NavigationIcon icon = NavigationIcon::Straight;
};

struct CubieStopwatchView
{
    uint32_t elapsedMs = 0;
    bool running = false;
};

struct CubieTorchView
{
    bool on = false;
};

struct DisplayFrame
{
    CubieScreen screen = CubieScreen::Eyes;
    CubieBattery battery;
    CubieNotification notification;
    CubieNavigationView navigation;
    String callerName;
    uint8_t missedCallCount = 0;
    uint8_t menuIndex = 0;
    uint8_t appsIndex = 0;
    uint8_t notificationIndex = 0;
    uint8_t notificationCount = 0;
    uint8_t settingsIndex = 0;
    bool settingsSelectedFlash = false;
    bool eyesReacting = false;
    uint8_t facePackIndex = 0;
    uint64_t facePackMask = 0x1FFFFFFFFFFFFFFFULL;
    bool sleepEnabled = true;
    bool buzzerEnabled = true;
    CubieBrightness brightness = CubieBrightness::Medium;
    CubieStopwatchView stopwatch;
    CubieTorchView torch;
    bool bleConnected = false;
    bool touchLocked = false;
};

#endif
