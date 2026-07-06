# Phone App And Chronos

## Role Of The Phone

The phone handles:

- Google Maps navigation
- Notification access
- Call state
- Phone battery reporting
- Sending data to Cubie through BLE

Cubie handles:

- Displaying the received information
- Touch input
- Local settings
- Battery warnings
- Buzzer feedback

## Connectivity

Normal operation:

- BLE only
- No WiFi needed
- No cloud dependency for V1

## Chronos

Relevant projects:

```text
https://github.com/advvitbf/cubie
https://github.com/fbiego/chronos-esp32
```

Important Chronos features:

- BLE communication
- Navigation support
- Notification support
- Missed calls
- Phone battery reporting
- OTA framework
- Device configuration syncing

## Navigation Source

V1 app support:

- Google Maps only

Cubie should not calculate navigation. It should display navigation commands, distances, speed, ETA, and icons received from the phone.

## Notification Rules

- Send notification app name
- Send message preview
- Show preview on Cubie for 3 seconds
- Keep last 5 messages in Cubie history
- Delete older messages

## Call Rules

During navigation:

- Do not interrupt navigation
- Send missed call event if call was missed
- Cubie shows missed call indicator only

Outside navigation:

- Send incoming call event
- Include caller name if available
- Cubie shows incoming call screen

