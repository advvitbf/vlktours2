# Open Questions

## Display

- The selected display is the Robocraze 1.3" I2C OLED Display Module 4-pin Blue Color.
- Is this exact module SSD1306 or SH1106?
- What is the OLED I2C address?
- Does the display need any X/Y offset correction?

## Navigation

- Should street names be shown later, or should V1 stay icon/distance/speed/ETA only?
- What exact Google Maps data does Chronos expose reliably?

## Power

- Can the selected TP4056 module safely power Cubie while charging?
- Should a load-sharing/power-path charging module be used instead?
- Which ESP32-C3 Super Mini pin should receive power from the battery/charger output?
- Which ADC pin should be used for battery voltage measurement?

## Battery

- What voltage divider resistor values should be used?
- How should full charge be detected?
- Can the protected 18650 cells and TP4056 protection circuit coexist safely in the final wiring?

## Firmware

- Confirm PlatformIO as the development environment.
- Confirm Arduino framework inside PlatformIO.
- Confirm whether Chronos is used as a library, direct fork, or reference implementation.
