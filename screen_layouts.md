# Cubie Screen Layout Approval

This file is used to approve Cubie's 128x64 OLED screen layouts before they are coded into the firmware.

Overall approval:

```text
Visual layout approval: Approved
OLED simulator approval: Approved
Ready for DisplayManager implementation: Yes
```

## Approval Flow

1. Create rough 128x64 layout variants in this file.
2. Review the variants and choose what feels right.
3. Mark chosen variants as approved or request changes.
4. Convert approved variants into display code.
5. Build the firmware and check Serial output.
6. Generate standalone OLED layout code for each approved screen variant so it can be tested in an OLED display simulator website.
7. Review the simulator output and approve again before final OLED implementation.

## Screen List

- Eyes
- Navigation
- Notification preview
- Incoming call
- Status
- Battery alert
- Charge complete
- Settings

## Layout Rules

- Target display: 128x64 OLED
- Color: monochrome
- Keep text short
- Use large readable symbols for navigation
- Navigation cannot be interrupted by messages or calls
- Eyes are the default screen
- No animations for V1
- Message preview lasts 3 seconds
- Incoming call screen appears only when navigation is inactive

## Approval Status Values

Use one of these:

```text
Pending
Approved
Change Needed
Rejected
```

---

# 1. Eyes Screen

Priority:

- Default screen

Status:

```text
Approved
```

Purpose:

- Cute idle state
- Shows Cubie personality most of the time
- Reacts to touch later

Variant A:

```text
+----------------+
|                |
|    O      O    |
|                |
|                |
|      ____      |
|                |
|                |
+----------------+
```

Notes:

- Approved.
- Based loosely on the Mochi-style eyes idea
- Static for V1
- Keep only the eyes. No battery, BLE, or other text on this screen.

OLED simulator code:

```text
oled_simulator_preview/oled_simulator_preview.ino
```

Simulator approval:

```text
Approved
```

---

# 2. Navigation Screen

Priority:

- Highest

Status:

```text
Approved
```

Purpose:

- Show next maneuver clearly while riding/walking
- Block calls, messages, and settings while active

Variant A:

```text
+----------------+
|120m          C1|
|                |
|       ->       |
|                |
|28km/h       1h8m|
+----------------+
```

Variant B:

```text
+----------------+
|      120m      |
|                |
|       ->       |
|                |
|28km/h      8m |
+----------------+
```

Notes:

- Approved: Variant A.
- Use Variant A structure, but only keep missed call indicator.
- Remove message/notification indicator from navigation altogether.
- Street names are not included yet.

OLED simulator code:

```text
oled_simulator_preview/oled_simulator_preview.ino
```

Simulator approval:

```text
Approved
```

---

# 3. Notification Preview

Priority:

- Shows only when navigation and incoming call are inactive

Status:

```text
Approved
```

Purpose:

- Show app name, sender/title, and short preview for 3 seconds

Variant A:

```text
+----------------+
| WhatsApp       |
| Aarav:         |
| Reached the    |
| cafe?          |
|                |
+----------------+
```

Variant B:

```text
+----------------+
| WhatsApp       |
|----------------|
| Aarav          |
| Reached cafe?  |
|                |
+----------------+
```

Notes:

- Approved: Variant A.
- Long messages must be clipped or wrapped.
- Only last 5 messages stay in history.

OLED simulator code:

```text
oled_simulator_preview/oled_simulator_preview.ino
```

Simulator approval:

```text
Approved
```

---

# 4. Incoming Call

Priority:

- Only shown when navigation is inactive

Status:

```text
Approved
```

Purpose:

- Show caller name and incoming state

Variant A:

```text
+----------------+
|    Incoming    |
|                |
|      Mom       |
|                |
|      [TEL]     |
+----------------+
```

Variant B:

```text
+----------------+
| [TEL] Incoming |
|                |
|      Mom       |
|                |
|                |
+----------------+
```

Notes:

- Approved: Variant A.
- During navigation, this screen does not appear.
- Missed call indicator appears on navigation screen instead.

OLED simulator code:

```text
oled_simulator_preview/oled_simulator_preview.ino
```

Simulator approval:

```text
Approved
```

---

# 5. Status Screen

Priority:

- Manual/status screen

Status:

```text
Approved
```

Purpose:

- Show BLE, phone battery, Cubie battery, and firmware version

Variant A:

```text
+----------------+
| BLE Connected  |
| Phone      72% |
| Cubie      80% |
| FW        1.0  |
|                |
+----------------+
```

Notes:

- Approved: Variant A.
- Useful during testing and debugging.

OLED simulator code:

```text
oled_simulator_preview/oled_simulator_preview.ino
```

Simulator approval:

```text
Approved
```

---

# 6. Battery Alert

Priority:

- Shown when battery reaches 10% or 5%, unless navigation is active

Status:

```text
Approved
```

Purpose:

- Warn user visually and with buzzer

Variant A:

```text
+----------------+
| I AM HUNGRY :[ |
|                |
|      10%       |
|                |
| FEED ME PLEASE |
+----------------+
```

Variant B:

```text
+----------------+
|      10%       |
|                |
|  I AM HUNGRY :[|
|                |
|                |
+----------------+
```

Notes:

- Approved: Variant A.
- 10% and 5% both beep and show visual warning.

OLED simulator code:

```text
oled_simulator_preview/oled_simulator_preview.ino
```

Simulator approval:

```text
Approved
```

---

# 7. Charge Complete

Priority:

- Shown when full charge is detected

Status:

```text
Approved
```

Purpose:

- Tell user to unplug charger without overcharging/overfeeding Cubie

Variant A:

```text
+----------------+
|      100%      |
|                |
|    I AM FULL   |
|                |
| DON'T OVERFEED |
|       ME       |
|                |
+----------------+
```

Notes:

- Approved: Variant A.
- Text changed and center aligned.
- Keep more vertical space between `I AM FULL` and `DON'T OVERFEED` than between `DON'T OVERFEED` and `ME`.
- Plays one long beep.
- Remains until charger is unplugged.
- Returns to eyes screen after unplug.

OLED simulator code:

```text
oled_simulator_preview/oled_simulator_preview.ino
```

Simulator approval:

```text
Approved
```

---

# 8. Settings Screen

Priority:

- Manual screen

Status:

```text
Approved
```

Purpose:

- Configure brightness, sleep timer, buzzer, reset

Variant A:

```text
+----------------+
| # Settings     |
| > Brightness   |
|   Sleep Timer  |
|   Buzzer       |
|   Reset        |
+----------------+
```

Notes:

- Approved: Variant A.
- Only page name uses `#`; option rows reserve one leading space for the arrow.
- Single tap moves next.
- Double tap selects.
- Triple tap saves and exits to eyes.
- Long press saves and exits to eyes.

OLED simulator code:

```text
oled_simulator_preview/oled_simulator_preview.ino
```

Simulator approval:

```text
Approved
```
