# Cubie Milestones

## Milestone 1: OLED Demo

Goal:

- Confirm the ESP32 can draw reliably on the OLED.

Tasks:

- Confirm display driver: SSD1306 or SH1106
- Confirm I2C address
- Draw text
- Draw simple icons
- Test brightness

## Milestone 2: Cubie Eyes UI

Goal:

- Make the default eyes screen.

Tasks:

- Recreate static eyes based on the Mochi reference
- Approve the 128x64 layout in `screen_layouts.md`
- Test the approved layout in an OLED simulator website
- Add touch reaction
- Add charging/default state behavior
- Add sleep/keep-awake setting behavior

## Milestone 3: Navigation Display

Goal:

- Display Google Maps navigation data from Chronos.

Tasks:

- Approve the 128x64 navigation layout in `screen_layouts.md`
- Test the approved layout in an OLED simulator website
- Show turn icon
- Show distance
- Show speed
- Show ETA
- Show missed call indicator during navigation
- Lock touch controls during navigation
- Return to eyes after navigation ends

## Milestone 4: Touch Controls

Goal:

- Make one TTP223 sensor control the UI.

Tasks:

- Single tap moves next
- Any tap wakes from sleep if screen is blank
- Double tap selects
- Triple tap saves and exits to eyes
- Long press blanks the screen when sleep is active
- Wake-only behavior from sleep
- Settings navigation
- Navigation lock behavior

Status:

- Software touch event handling added
- GPIO4 touch input module added
- Simulator touch events added
- Sleep blank-screen state added
- Real TTP223 hardware test pending

## Milestone 5: Battery Readout

Goal:

- Display Cubie battery within 5% accuracy brackets.

Tasks:

- Add voltage divider circuit
- Pick ADC pin
- Calibrate ADC reading
- Convert voltage to percent
- Add 10% and 5% buzzer/visual warnings
- Add full battery behavior

## Milestone 6: BLE Pairing

Goal:

- Pair Cubie with the phone and receive live data.

Tasks:

- Integrate ChronosESP32
- Receive Google Maps navigation
- Receive notifications
- Receive call events
- Receive phone battery
- Test reconnect behavior

## Milestone 7: Hardware Assembly

Goal:

- Build physical Cubie prototype.

Tasks:

- Confirm safe charging circuit
- Confirm power input pin
- Mount OLED
- Mount touch sensor
- Mount buzzer
- Test battery runtime
- Test touch behavior inside enclosure

## Milestone 8: Enclosure

Goal:

- Create a usable enclosure for walking and motorcycle use.

Tasks:

- Decide mounting approach
- Decide charging port access
- Decide OLED window
- Test touch sensitivity through enclosure
- Test weather/dust protection requirements
