# Cubie Project

Cubie is a compact ESP32-C3 Super Mini based navigation, notification, and companion display device.

The device spends most of its time on the eyes screen. Navigation, calls, notifications, battery alerts, and settings temporarily interrupt or overlay that experience.

## Primary Goals

- Cute always-on companion eyes
- Motorcycle navigation
- Walking navigation
- Notification previews
- Missed call indicators during navigation
- Incoming call screen when not navigating
- Long battery life from a 5000 mAh battery pack
- Simple OLED interface
- No cloud dependency for V1
- No WiFi required for normal use

## Core Architecture

```text
Phone
  |
  | Google Maps / notifications / call state
  v
Chronos App
  |
  | BLE commands and data
  v
ESP32-C3 Super Mini
  |
  | I2C
  v
1.3" OLED Display
```

Cubie does not calculate routes. The phone app sends data and commands, and Cubie displays them.

## V1 Priority Balance

Cubie should feel balanced across the following:

- Eyes/personality: used most of the time
- Navigation: used less often, but must be reliable
- Notifications: used less often, but important
- Battery life: important, backed by a 5000 mAh pack

## Active Repository

```text
https://github.com/advvitbf/cubie
```

## Documentation Map

- `main.md`: project overview
- `product.md`: behavior, screens, priorities, UX rules
- `connections.md`: hardware, wiring, battery, power notes
- `app.md`: phone app and Chronos behavior
- `app_code.md`: app-side implementation notes
- `esp32_code.md`: ESP32 firmware structure and milestones
- `milestones.md`: build order and checklist
- `questions.md`: open questions to revisit

