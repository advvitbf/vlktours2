# ESP32 Firmware Plan

## Recommended Development Environment

### Arduino IDE

Pros:

- Simple setup
- Easy for quick sketches
- Lots of beginner tutorials
- Good for testing one feature at a time

Cons:

- Gets messy as the project grows
- Weaker project structure
- Harder to manage multiple files cleanly
- Less convenient for larger firmware milestones

### PlatformIO In VS Code

Pros:

- Better for clean multi-file firmware
- Easier library and board management
- Better build configuration
- Better serial monitor workflow
- Better long-term project structure

Cons:

- Slightly more setup at the beginning
- Board configuration may need tuning
- More files to understand at first

Recommended:

- Use PlatformIO in VS Code for Cubie
- Keep Arduino framework inside PlatformIO

This gives the simplicity of Arduino code with a cleaner project structure.

## Firmware Stack

Framework:

- Arduino framework

Libraries:

- ChronosESP32
- Adafruit GFX
- Adafruit SH110X
- Adafruit NeoPixel

Display driver:

- The selected module is the Robocraze 1.3" I2C OLED Display Module 4-pin Blue Color.
- Hardware testing confirmed SH1106 works correctly.
- SSD1306 produced corrupted output on the tested display.

## Suggested File Structure

```text
src/
  main.cpp
  DisplayManager.cpp
  DisplayManager.h
  EyeScreen.cpp
  EyeScreen.h
  NavigationScreen.cpp
  NavigationScreen.h
  NotificationManager.cpp
  NotificationManager.h
  CallManager.cpp
  CallManager.h
  TouchInput.cpp
  TouchInput.h
  BatteryManager.cpp
  BatteryManager.h
  Buzzer.cpp
  Buzzer.h
  GlyphManager.cpp
  GlyphManager.h
  Settings.cpp
  Settings.h
  BleChronos.cpp
  BleChronos.h
```

## Screen Priority

Highest to lowest:

1. Navigation
2. Incoming call, only when not navigating
3. Battery critical warning
4. Notification preview
5. Settings
6. Eyes

Navigation locks touch controls except wake behavior.

## Display Architecture

Cubie firmware uses a display layer:

```text
CubieApp -> DisplayFrame -> DisplayManager -> Serial/OLED
```

`CubieApp` decides what should be shown. `DisplayManager` decides how it is drawn.

`DisplayManager` renders to Serial Monitor for debug and to the SH1106 OLED backend when enabled.

## Glyph Architecture

Cubie firmware uses a separate glyph layer:

```text
Phone/BLE events -> main.cpp callbacks -> GlyphManager -> WS2812 ring
```

`GlyphManager` controls the 8-LED circular WS2812 ring on GPIO6.

The glyph ring is for quick ambient feedback only. It does not replace OLED screens or hold menu state.

## First Build Order

1. OLED demo
2. Cubie eyes UI
3. Navigation display
4. Touch controls
5. Battery readout
6. BLE pairing

## Touch Timing Recommendation

Initial values:

```cpp
const uint16_t TOUCH_DEBOUNCE_MS = 50;
const uint16_t MULTI_TAP_WINDOW_MS = 450;
const uint16_t LONG_PRESS_MS = 900;
```

These should be tested on real hardware.

Implemented touch behavior:

- TTP223 OUT -> GPIO4
- Any tap wakes from sleep
- Single tap moves next when awake
- Double tap selects
- Triple tap saves and exits to eyes
- Long press blanks the screen when sleep is active; otherwise saves and exits to eyes
- Touch is ignored during navigation

## Battery Readout

Needed hardware:

- Voltage divider to ESP32 ADC pin

Firmware behavior:

- Read ADC
- Convert ADC reading to voltage
- Estimate percentage from Li-ion voltage curve
- Report in 5% brackets

Example bracket behavior:

```text
82% measured -> display 80%
87% measured -> display 85%
```

## Charging Behavior

Cubie should keep operating while charging.

When full charge is detected:

- Play 1 long beep
- Show `I AM FULL`, `DON'T OVERFEED`, and `ME`
- Remain displayed until charger is unplugged
- Return to eyes screen

Full charge detection depends on the final charging/power circuit.
