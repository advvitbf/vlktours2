#include "Buzzer.h"
#include "CubieConfig.h"

void Buzzer::begin(uint8_t pin)
{
    _pin = pin;
    pinMode(_pin, OUTPUT);
    setActive(false);
}

void Buzzer::update()
{
    if (_remainingBeeps == 0 && !_active)
    {
        return;
    }

    uint32_t now = millis();
    if (now < _nextChangeAt)
    {
        return;
    }

    if (_active)
    {
        setActive(false);
        _nextChangeAt = now + _offMs;
        return;
    }

    if (_remainingBeeps > 0)
    {
        _remainingBeeps--;
        setActive(true);
        _nextChangeAt = now + _onMs;
    }
}

void Buzzer::beep(uint8_t count, uint16_t onMs, uint16_t offMs)
{
    if (!_enabled)
    {
        return;
    }

    _remainingBeeps = count;
    _onMs = onMs;
    _offMs = offMs;
    _nextChangeAt = 0;
}

void Buzzer::setEnabled(bool enabled)
{
    _enabled = enabled;
    if (!_enabled)
    {
        _remainingBeeps = 0;
        setActive(false);
    }
}

bool Buzzer::enabled() const
{
    return _enabled;
}

void Buzzer::setActive(bool active)
{
    _active = active;
    digitalWrite(_pin, active ? CubieConfig::BuzzerOnLevel : CubieConfig::BuzzerOffLevel);
}
