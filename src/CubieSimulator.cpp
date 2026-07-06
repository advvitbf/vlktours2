#include "CubieSimulator.h"

void CubieSimulator::begin()
{
    _step = 0;
    scheduleNext(1500);
    Serial.println("[Simulator] Enabled");
}

void CubieSimulator::loop(CubieApp &app, DisplayManager &display)
{
    if (millis() >= _nextEventAt)
    {
        runStep(app, display);
    }
}

void CubieSimulator::runStep(CubieApp &app, DisplayManager &display)
{
    switch (_step)
    {
    case 0:
        Serial.println("[Simulator] BLE connected");
        app.onConnectionChanged(true);
        scheduleNext(2500);
        break;
    case 1:
        Serial.println("[Simulator] Notification on eyes screen");
        app.simulateNotification("WhatsApp", "Aarav", "Reached the cafe?");
        scheduleNext(5000);
        break;
    case 2:
        Serial.println("[Simulator] Navigation starts");
        app.simulateNavigation(true, "120 m", "Turn right", "2.4 km", "", "");
        scheduleNext(3000);
        break;
    case 3:
        Serial.println("[Simulator] Message arrives during navigation");
        app.simulateNotification("Gmail", "Order update", "Your package is out for delivery");
        scheduleNext(3000);
        break;
    case 4:
        Serial.println("[Simulator] Call arrives during navigation");
        app.simulateIncomingCall("Mom", true);
        scheduleNext(3000);
        break;
    case 5:
        Serial.println("[Simulator] Call stops, navigation still active");
        app.simulateIncomingCall("Mom", false);
        scheduleNext(3000);
        break;
    case 6:
        Serial.println("[Simulator] Navigation ends");
        app.simulateNavigation(false, "", "", "", "", "");
        scheduleNext(2500);
        break;
    case 7:
        Serial.println("[Simulator] Incoming call outside navigation");
        app.simulateIncomingCall("Mom", true);
        scheduleNext(4000);
        break;
    case 8:
        Serial.println("[Simulator] Call dismissed");
        app.simulateIncomingCall("Mom", false);
        scheduleNext(2500);
        break;
    case 9:
        Serial.println("[Simulator] Cubie battery drops to 10%");
        app.simulateCubieBattery(10, false);
        scheduleNext(4000);
        break;
    case 10:
        Serial.println("[Simulator] Battery restored for next loop");
        app.simulateCubieBattery(80, false);
        scheduleNext(2500);
        break;
    case 11:
        Serial.println("[Simulator] Touch single tap");
        app.onTouch(TouchEvent::SingleTap, display);
        scheduleNext(2500);
        break;
    case 12:
        Serial.println("[Simulator] Touch single tap");
        app.onTouch(TouchEvent::SingleTap, display);
        scheduleNext(2500);
        break;
    case 13:
        Serial.println("[Simulator] Touch double tap");
        app.onTouch(TouchEvent::DoubleTap, display);
        scheduleNext(2500);
        break;
    case 14:
        Serial.println("[Simulator] Touch triple tap exits to eyes");
        app.onTouch(TouchEvent::TripleTap, display);
        scheduleNext(2500);
        break;
    case 15:
        Serial.println("[Simulator] Navigation starts for touch lock test");
        app.simulateNavigation(true, "120 m", "Turn right", "2.4 km", "", "");
        scheduleNext(2000);
        break;
    case 16:
        Serial.println("[Simulator] Touch during navigation");
        app.onTouch(TouchEvent::SingleTap, display);
        scheduleNext(2500);
        break;
    case 17:
        Serial.println("[Simulator] Navigation ends after touch lock test");
        app.simulateNavigation(false, "", "", "", "", "");
        scheduleNext(2500);
        break;
    case 18:
        Serial.println("[Simulator] Long press enters sleep");
        app.onTouch(TouchEvent::LongPress, display);
        scheduleNext(2500);
        break;
    case 19:
        Serial.println("[Simulator] Any tap wakes from sleep");
        app.onTouch(TouchEvent::SingleTap, display);
        scheduleNext(3000);
        break;
    default:
        _step = 0;
        scheduleNext(1500);
        return;
    }

    _step++;
}

void CubieSimulator::scheduleNext(uint32_t delayMs)
{
    _nextEventAt = millis() + delayMs;
}
