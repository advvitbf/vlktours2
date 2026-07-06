#ifndef TOUCH_INPUT_H
#define TOUCH_INPUT_H

#include <Arduino.h>
#include "CubieConfig.h"
#include "CubieModels.h"

class TouchInput
{
public:
    void begin(uint8_t pin);
    bool update(TouchEvent &event);
    bool consumePressEdge();

private:
    uint8_t _pin = 0;
    bool _lastRawState = false;
    bool _stableState = false;
    bool _longPressSent = false;
    uint8_t _tapCount = 0;
    uint32_t _lastRawChangeAt = 0;
    uint32_t _pressStartedAt = 0;
    uint32_t _lastReleaseAt = 0;
    bool _pressEdgePending = false;

    bool readPressed() const;
};

#endif
