#include "TouchInput.h"

void TouchInput::begin(uint8_t pin)
{
    _pin = pin;
    pinMode(_pin, INPUT);
    _lastRawState = readPressed();
    _stableState = _lastRawState;
    _lastRawChangeAt = millis();
}

bool TouchInput::update(TouchEvent &event)
{
    bool rawState = readPressed();
    uint32_t now = millis();

    if (rawState != _lastRawState)
    {
        _lastRawState = rawState;
        _lastRawChangeAt = now;
    }

    if (now - _lastRawChangeAt < CubieConfig::TouchDebounceMs)
    {
        return false;
    }

    if (rawState != _stableState)
    {
        _stableState = rawState;

        if (_stableState)
        {
            _pressStartedAt = now;
            _longPressSent = false;
            _pressEdgePending = true;
        }
        else
        {
            if (!_longPressSent)
            {
                _tapCount++;
                _lastReleaseAt = now;
            }
        }
    }

    if (_stableState && !_longPressSent && now - _pressStartedAt >= CubieConfig::LongPressMs)
    {
        _longPressSent = true;
        _tapCount = 0;
        event = TouchEvent::LongPress;
        return true;
    }

    if (!_stableState && _tapCount > 0 && now - _lastReleaseAt >= CubieConfig::MultiTapWindowMs)
    {
        if (_tapCount == 1)
        {
            event = TouchEvent::SingleTap;
        }
        else if (_tapCount == 2)
        {
            event = TouchEvent::DoubleTap;
        }
        else
        {
            event = TouchEvent::TripleTap;
        }

        _tapCount = 0;
        return true;
    }

    return false;
}

bool TouchInput::readPressed() const
{
    return digitalRead(_pin) == HIGH;
}

bool TouchInput::consumePressEdge()
{
    if (!_pressEdgePending)
    {
        return false;
    }

    _pressEdgePending = false;
    return true;
}
