# Hardware And Connections

## Main Controller

Board:

- ESP32-C3 Super Mini

Important features:

- BLE
- Low power modes
- USB-C programming, depending on board variant
- Small size

## Display

Planned display:

- Robocraze 1.3" I2C OLED Display Module 4-pin Blue Color
- 128x64
- I2C/IIC
- Blue OLED
- 3.3V to 5V compatible

Product link:

```text
https://robocraze.com/products/1-3in-oled-display?variant=40192586842265
```

Important open question:

- Confirmed during hardware testing: this module works with the SH1106 driver

The Robocraze page confirms 1.3", 128x64, 4-pin I2C/IIC, and pins `GND`, `VCC`, `SCL`, `SDA`, but it does not clearly name the controller chip. Hardware testing showed SSD1306 produced corrupted output, while SH1106 rendered correctly.

## Touch Sensor

Sensor:

- TTP223 capacitive touch module

Used for:

- Wake
- Confirm
- Next screen
- Previous screen
- Settings control

## Glyph Ring

Selected V1 glyph hardware:

- 8 Bit WS2812 5050 RGB circular development board
- 8 individually addressable RGB LEDs
- 5V power
- One data pin from ESP32-C3

Role:

- Secondary light feedback, not a second screen
- BLE connect/disconnect indication
- Navigation direction hints
- Notification pulse
- Incoming call pulse

## Battery

Battery pack:

- 2x 18650 cells
- Parallel configuration
- Same brand
- Same capacity
- New cells
- Protected cells

Capacity:

```text
2500 mAh
+
2500 mAh
=
5000 mAh
```

Nominal voltage:

```text
3.7 V
```

## Charging Module

Planned module:

- TP4056 USB-C

Requirements:

- Charging
- Overcharge protection
- Battery protection
- Ability to run Cubie while charging

Important warning:

Standard TP4056 modules are not always ideal for load sharing. If Cubie must run while charging, use a module or circuit that supports power-path/load-sharing behavior, or test carefully before finalizing.

## Battery Measurement

Current status:

- No battery measurement circuit yet

Recommended approach:

- Add a voltage divider from battery positive to an ESP32 ADC pin
- Use high-value resistors to reduce drain
- Add firmware calibration
- Convert voltage to percentage using a Li-ion discharge curve

Accuracy target:

- Battery percentage within 5%

## Physical Power Switch

Decision:

- No physical power switch for V1

## OLED Sleep

Behavior:

- OLED should stay on if the setting is set to keep awake
- Sleep behavior should be configurable

## Optional Buzzer

Uses:

- Navigation prompts
- Low battery alerts
- Charge complete alert
- Find my Cubie, if added later

## Wiring

### OLED

```text
OLED VCC -> ESP32 3V3
OLED GND -> ESP32 GND
OLED SDA -> GPIO8
OLED SCL -> GPIO9
```

Robocraze pin labels:

```text
GND
VCC
SCL
SDA
```

### Touch Sensor

```text
TTP223 VCC -> 3V3
TTP223 GND -> GND
TTP223 OUT -> GPIO4
```

### Buzzer

```text
Buzzer VCC -> 3V3
Buzzer GND -> GND
Buzzer I/O -> GPIO5
```

### Glyph Ring

```text
Glyph 5V/VCC -> 5V
Glyph GND    -> ESP32 GND
Glyph DIN    -> GPIO6
```

Notes:

- Keep ESP32 GND and glyph GND connected.
- Do not power the glyph ring from an ESP32 GPIO pin.
- A 3.3V-to-5V data level shifter is recommended for reliable WS2812 data.
- Keep brightness low in firmware to reduce current draw and glare.

The tested buzzer module is active LOW:

```text
GPIO5 LOW  -> buzzer ON
GPIO5 HIGH -> buzzer OFF
```

Use a transistor driver if the buzzer needs more current than the ESP32 GPIO can safely provide.

## Battery Parallel Wiring

```text
Battery 1 +
            \
             +---- B+
            /
Battery 2 +

Battery 1 -
            \
             +---- B-
            /
Battery 2 -
```

## TP4056 Connections

```text
Battery Positive -> B+
Battery Negative -> B-

OUT+ -> ESP32 power input
OUT- -> ESP32 GND
```

Important open question:

- Confirm the safest ESP32-C3 Super Mini power input pin for the selected board variant.
