# Chronos Feature Slice For Cubie

## Keep

- BLE pairing and connection state
- Google Maps navigation data
- Navigation icon data
- Notification previews
- Incoming call/ringer events
- Missed call indicator support
- Phone battery reporting
- Basic app/device info

## Ignore For V1

- Heart rate
- Blood oxygen
- Blood pressure
- Step records
- Sleep records
- Temperature records
- Weather
- Alarms
- Music controls
- Camera controls
- QR codes
- SOS contacts
- Remote touch from app
- Watchface-related features

## Current Implementation Choice

For the first software pass, Cubie does not physically delete unused Chronos library features.

Instead, Cubie firmware only attaches callbacks and state handling for the features it needs:

- `setConnectionCallback`
- `setNotificationCallback`
- `setRingerCallback`
- `setConfigurationCallback`
- `getNavigation`
- `getPhoneBattery`
- `setBattery`
- `setNotifyBattery`

This keeps the BLE protocol stable while the hardware is not available. If flash, RAM, or behavior becomes a problem later, the next step is to create a physically trimmed `CubieChronos` library.

## Cubie Rules Implemented In Firmware Scaffold

- Eyes are the default screen.
- Navigation has top priority.
- Touch is considered locked during navigation.
- Notifications are stored during navigation but never interrupt the navigation screen.
- Notifications show as previews only when navigation and calls are inactive.
- Notification previews expire after 3 seconds.
- Only the last 5 notifications are stored.
- Incoming calls show a caller screen only when navigation is inactive.
- Phone battery updates are accepted from Chronos.
