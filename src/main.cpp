#include <Arduino.h>
#include <ChronosESP32.h>
#include "Buzzer.h"
#include "CubieApp.h"
#include "CubieConfig.h"
#include "DisplayManager.h"
#include "GlyphManager.h"
#include "TouchInput.h"
#if CUBIE_SIMULATOR
#include "CubieSimulator.h"
#endif

ChronosESP32 chronos(CubieConfig::DeviceName, CF_ESP32_240x240);
CubieApp cubie;
DisplayManager display;
GlyphManager glyph;
TouchInput touchInput;
Buzzer buzzer;
uint32_t restartAt = 0;
bool phoneCallActive = false;
uint32_t nextPhoneCallBeepAt = 0;

void sendPhoneCallState()
{
    uint8_t response[] = {0xCB, 0x8A, static_cast<uint8_t>(phoneCallActive ? 1 : 0)};
    chronos.sendCommand(response, sizeof(response));
}
#if CUBIE_SIMULATOR
CubieSimulator simulator;
#endif

void connectionCallback(bool connected)
{
    glyph.setConnected(connected);
    cubie.onConnectionChanged(connected);
}

void notificationCallback(Notification notification)
{
    glyph.showNotification();
    cubie.onNotification(notification);
}

void ringerCallback(String caller, bool active)
{
    glyph.setCall(active);
    cubie.onRinger(caller, active);
}

void configurationCallback(Config config, uint32_t a, uint32_t b)
{
    cubie.onConfiguration(chronos, config, a, b);
}

uint64_t readMask24(const uint8_t *data, int offset)
{
    return static_cast<uint64_t>(data[offset]) |
           (static_cast<uint64_t>(data[offset + 1]) << 8) |
           (static_cast<uint64_t>(data[offset + 2]) << 16);
}

uint64_t readMask64(const uint8_t *data, int offset)
{
    uint64_t mask = 0;
    for (uint8_t i = 0; i < 8; i++)
    {
        mask |= static_cast<uint64_t>(data[offset + i]) << (i * 8);
    }
    return mask;
}

void rawDataCallback(uint8_t *data, int length)
{
    if (length >= 3 && data[0] == 0xCB && data[1] == 0x01)
    {
        cubie.setFacePack(data[2]);
    }
    else if (length >= 10 && data[0] == 0xCB && data[1] == 0x04)
    {
        uint64_t mask = readMask64(data, 2);
        cubie.setFacePackMask(mask);
    }
    else if (length >= 5 && data[0] == 0xCB && data[1] == 0x04)
    {
        uint64_t mask = readMask24(data, 2);
        cubie.setFacePackMask(mask);
    }
    else if (length >= 3 && data[0] == 0xCB && data[1] == 0x05)
    {
        cubie.setPettingFacePack(data[2]);
    }
    else if (length >= 2 && data[0] == 0xCB && data[1] == 0x06)
    {
        Serial.println("[Cubie] Restart requested by app");
        restartAt = millis() + 350;
    }
    else if (length >= 11 && data[0] == 0xCB && data[1] == 0x07)
    {
        uint8_t pettingIndex = data[2];
        uint64_t mask = readMask64(data, 3);
        cubie.setFaceSettings(mask, pettingIndex);
        Serial.println("[Cubie] Restart requested after face settings save");
        restartAt = millis() + 500;
    }
    else if (length >= 6 && data[0] == 0xCB && data[1] == 0x07)
    {
        uint8_t pettingIndex = data[2];
        uint64_t mask = readMask24(data, 3);
        cubie.setFaceSettings(mask, pettingIndex);
        Serial.println("[Cubie] Restart requested after face settings save");
        restartAt = millis() + 500;
    }
    else if (length >= 5 && data[0] == 0xCB && data[1] == 0x08)
    {
        bool sleepEnabled = data[2] == 1;
        bool buzzerEnabled = data[3] == 1;
        uint8_t brightnessValue = data[4];
        uint8_t sleepTimerMinutes = length >= 6 ? data[5] : 0;
        if (brightnessValue > static_cast<uint8_t>(CubieBrightness::High))
        {
            brightnessValue = static_cast<uint8_t>(CubieBrightness::Medium);
        }
        cubie.setDeviceSettings(sleepEnabled, buzzerEnabled, static_cast<CubieBrightness>(brightnessValue), sleepTimerMinutes);
    }
    else if (length >= 4 && data[0] == 0xCB && data[1] == 0x09)
    {
        uint8_t response[] = {0xCB, 0x89, data[2], data[3]};
        chronos.sendCommand(response, sizeof(response));
    }
    else if (length >= 3 && data[0] == 0xCB && data[1] == 0x0A)
    {
        phoneCallActive = data[2] == 1;
        nextPhoneCallBeepAt = 0;
        glyph.setCall(phoneCallActive);
        buzzer.setEnabled(phoneCallActive ? true : cubie.buzzerEnabled());
        Serial.println(phoneCallActive ? "[Cubie] Phone call alert started" : "[Cubie] Phone call alert stopped");
        sendPhoneCallState();
    }
    else if (length >= 8 && data[0] == 0xCB && data[1] == 0x0B)
    {
        bool active = data[2] == 1;
        uint8_t titleLen = data[3];
        uint8_t directionsLen = data[4];
        uint8_t distanceLen = data[5];
        uint8_t etaLen = data[6];
        uint8_t speedLen = data[7];
        int expected = 8 + titleLen + directionsLen + distanceLen + etaLen + speedLen;
        if (length >= expected)
        {
            String title;
            String directions;
            String distance;
            String eta;
            String speed;
            int cursor = 8;
            for (uint8_t i = 0; i < titleLen; i++) title += (char)data[cursor++];
            for (uint8_t i = 0; i < directionsLen; i++) directions += (char)data[cursor++];
            for (uint8_t i = 0; i < distanceLen; i++) distance += (char)data[cursor++];
            for (uint8_t i = 0; i < etaLen; i++) eta += (char)data[cursor++];
            for (uint8_t i = 0; i < speedLen; i++) speed += (char)data[cursor++];
            glyph.setNavigation(active, directions);
            cubie.simulateNavigation(active, title, directions, distance, eta, speed);
        }
    }
    else if (length >= 5 && data[0] == 0xCB && data[1] == 0x02)
    {
        uint8_t appLen = data[2];
        uint8_t titleLen = data[3];
        uint8_t messageLen = data[4];
        int expected = 5 + appLen + titleLen + messageLen;
        if (length >= expected)
        {
            String app;
            String title;
            String message;
            int cursor = 5;
            for (uint8_t i = 0; i < appLen; i++) app += (char)data[cursor++];
            for (uint8_t i = 0; i < titleLen; i++) title += (char)data[cursor++];
            for (uint8_t i = 0; i < messageLen; i++) message += (char)data[cursor++];
            cubie.simulateNotification(app, title, message);
        }
    }
    else if (length >= 4 && data[0] == 0xCB && data[1] == 0x03)
    {
        bool active = data[2] == 1;
        uint8_t callerLen = data[3];
        if (length >= 4 + callerLen)
        {
            String caller;
            for (uint8_t i = 0; i < callerLen; i++) caller += (char)data[4 + i];
            cubie.simulateIncomingCall(caller, active);
        }
    }
}

void setup()
{
    Serial.begin(115200);
    delay(200);

    chronos.setConnectionCallback(connectionCallback);
    chronos.setNotificationCallback(notificationCallback);
    chronos.setRingerCallback(ringerCallback);
    chronos.setConfigurationCallback(configurationCallback);
    chronos.setRawDataCallback(rawDataCallback);

    chronos.begin();
    display.begin();
    glyph.begin(CubieConfig::GlyphPin, CubieConfig::GlyphLedCount);
    touchInput.begin(CubieConfig::TouchPin);
    buzzer.begin(CubieConfig::BuzzerPin);
    buzzer.beep(2, 70, 90);
    cubie.begin(chronos, display);
#if CUBIE_SIMULATOR
    simulator.begin();
#endif

    Serial.println("Cubie firmware started");
    Serial.print("BLE address: ");
    Serial.println(chronos.getAddress());
}

void loop()
{
    chronos.loop();
    glyph.update();
    buzzer.update();
    if (phoneCallActive && millis() >= nextPhoneCallBeepAt)
    {
        buzzer.beep(1, 35, 35);
        nextPhoneCallBeepAt = millis() + 120;
    }
    if (restartAt > 0 && millis() >= restartAt)
    {
        ESP.restart();
    }
#if CUBIE_SIMULATOR
    simulator.loop(cubie, display);
#endif
#if !CUBIE_SIMULATOR
    TouchEvent touchEvent;
    if (touchInput.update(touchEvent))
    {
        if (phoneCallActive)
        {
            phoneCallActive = false;
            buzzer.beep(1, 90, 70);
            Serial.println("[Cubie] Phone call alert stopped by touch");
            sendPhoneCallState();
            buzzer.setEnabled(cubie.buzzerEnabled());
        }
        else
        {
            switch (touchEvent)
            {
            case TouchEvent::SingleTap:
                buzzer.beep(1);
                break;
            case TouchEvent::DoubleTap:
                buzzer.beep(2);
                break;
            case TouchEvent::TripleTap:
                buzzer.beep(3);
                break;
            case TouchEvent::LongPress:
                buzzer.beep(1, 420, 70);
                break;
            }
            cubie.onTouch(touchEvent, display);
        }
        buzzer.setEnabled(cubie.buzzerEnabled());
    }
#endif
    cubie.loop(chronos, display);
}
