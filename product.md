# Product Behavior

## Product Definition

Cubie is both:

- A motorcycle/walking navigation display
- A cute OLED companion device

The eyes screen is the default experience. Navigation, notifications, calls, battery warnings, and settings are secondary states.

## Default State

Default screen:

```text
Eyes
```

The eyes design will be based on:

```text
https://themochi.huykhong.com/
```

V1 eyes behavior:

- Eyes are the default screen
- Eyes react to touch
- No animation required for V1

## Glyph Ring

V1 adds a small circular glyph ring:

- 8 addressable RGB LEDs
- Used as a light-language layer
- Not used as a second display
- OLED remains the main information screen

V1 glyph behaviors:

- BLE connected: soft blue pulse
- BLE disconnected: red blink
- Navigation left/right/straight/U-turn/roundabout: amber directional pattern
- Notification: warm quick pulse
- Incoming call: blue breathing pulse

## Menu Map

Menu is an actual OLED screen opened from Eyes with a triple tap. Navigation is not shown as a selectable menu item; when BLE is connected and phone navigation starts, Cubie automatically switches to Navigation from anywhere.

```text
# Menu
|_ Eyes
|_ Notifications
|_ Status
|_ Settings
   |_ Brightness
   |_ Sleep
   |_ Buzzer
```

## Navigation

Navigation source:

- Google Maps only for V1
- Data comes through the Chronos app over BLE

Navigation priority:

- Navigation is the highest-priority active mode
- During navigation, other interactions are locked
- Incoming calls do not take over the screen during navigation
- Message previews do not interrupt navigation
- Missed calls are shown only as an indicator icon during navigation

Navigation fallback:

- When navigation ends, Cubie returns to the eyes screen

Supported maneuvers:

- Straight
- Left
- Right
- Slight left
- Slight right
- Sharp left
- Sharp right
- U-turn left
- U-turn right
- Roundabout
- Arrived

Possible navigation layout:

```text
120m

      ->

1h8m         2.4km
```

Street names:

- Not decided yet
- Decide during testing after seeing what fits on the OLED

Sound:

- No vibration
- Buzzer only

## Notifications

Notification behavior:

- Show message preview
- Preview appears for 3 seconds
- After 3 seconds, preview disappears
- Keep last 5 messages in history
- Delete older messages automatically

Supported notification examples:

- WhatsApp
- Telegram
- Discord
- Gmail
- Instagram
- Other apps

Notification history layout:

```text
Notifications

WhatsApp (2)
Discord (1)
Gmail (2)
```

## Calls

During navigation:

- Do not show full incoming call screen
- Do not show message previews over navigation
- If a call is missed, show missed call icon/count

When not navigating:

- Show caller name
- Show text: `Incoming`
- Show phone icon

Possible incoming call layout:

```text
Incoming

Mom

Phone Icon
```

## Battery Alerts

Battery alert levels:

- 10%
- 5%

Alert behavior:

- Buzzer beep
- Visual warning

Example:

```text
I AM HUNGRY :[

10%

FEED ME PLEASE
```

Battery accuracy goal:

- Within a 5% bracket

## Charge Complete Behavior

When battery reaches full:

```text
100%

I AM FULL

DON'T OVERFEED
ME
```

Behavior:

- Play 1 long beep
- Keep the full battery screen displayed
- When charger is unplugged, return to eyes screen
- No separate charging screen exists
- Eyes remain the default charging display

## Touch Controls

Hardware:

- One TTP223 capacitive touch module

Eyes controls:

- Single tap: trigger an eyes reaction
- Double tap: open `# Menu`
- Triple tap: no screen action
- Long press: blank the screen if sleep is active

Menu branch controls:

- Single tap: scroll
- Double tap: select
- Triple tap: back
- Long press: return to eyes, even if sleep is active

Settings and Notifications use the same menu branch controls.

Sleep behavior:

- If screen is asleep, any tap wakes Cubie
- The wake gesture does not also move to the next screen
- When sleep is active, long press makes the screen go blank

Navigation lock:

- During navigation, controls are locked
- Long press does not open or exit settings during navigation

## Tap Timing Options

Conservative timing:

- Tap window: 350 ms
- Double/triple tap detection window: 500 ms
- Long press: 900 ms
- Best for avoiding accidental input on motorcycle use

Responsive timing:

- Tap window: 250 ms
- Double/triple tap detection window: 400 ms
- Long press: 700 ms
- Feels faster, but can misread shaky taps

Recommended V1 timing:

- Tap debounce: 50 ms
- Multi-tap window: 450 ms
- Long press: 900 ms

This should be tested on the real enclosure because capacitive touch behavior changes with case material and mounting.
