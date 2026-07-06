#ifndef CUBIE_SIMULATOR_H
#define CUBIE_SIMULATOR_H

#include <Arduino.h>
#include "CubieApp.h"

class CubieSimulator
{
public:
    void begin();
    void loop(CubieApp &app, DisplayManager &display);

private:
    uint8_t _step = 0;
    uint32_t _nextEventAt = 0;

    void runStep(CubieApp &app, DisplayManager &display);
    void scheduleNext(uint32_t delayMs);
};

#endif
