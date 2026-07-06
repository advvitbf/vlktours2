#ifndef BUZZER_H
#define BUZZER_H

#include <Arduino.h>

class Buzzer
{
public:
    void begin(uint8_t pin);
    void update();
    void beep(uint8_t count = 1, uint16_t onMs = 55, uint16_t offMs = 70);
    void setEnabled(bool enabled);
    bool enabled() const;

private:
    uint8_t _pin = 0;
    uint8_t _remainingBeeps = 0;
    uint16_t _onMs = 55;
    uint16_t _offMs = 70;
    uint32_t _nextChangeAt = 0;
    bool _active = false;
    bool _enabled = true;

    void setActive(bool active);
};

#endif
