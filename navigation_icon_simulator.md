# Navigation Icon Simulator

Use this sketch to preview Cubie's approved navigation direction icon candidates.

## File

```text
navigation_icon_simulator/navigation_icon_simulator.ino
```

## What It Shows

The simulator cycles through 15 direction icons:

1. Straight
2. Left
3. Right
4. Slight left
5. Slight right
6. Sharp left
7. Sharp right
8. U-turn left
9. U-turn right
10. Roundabout
11. Arrived
12. Bridge straight
13. Bridge left
14. Bridge right
15. Bridge U-turn

Each icon is shown in the approved navigation screen layout:

```text
120m          C1

ICON

28km/h        1h8m
```

## Simulator Setup

Use:

```text
128x64 SSD1306 I2C OLED
I2C address 0x3C
Adafruit GFX
Adafruit SSD1306
```

## Approval Questions

For each icon, check:

- Is the direction clear?
- Is the arrow large enough?
- Does the label interfere with the icon?
- Are bridge icons understandable?
- Is the roundabout icon clear enough with only one generic symbol?
- Should any icon be thicker, simpler, or moved?

Sharp left reuses the normal left icon. Sharp right reuses the normal right icon.

Approval result:

```text
Approved
```

Next step: move the approved icons into `DisplayManager`.
