# Navigation Icon Approval

This file tracks the icon approval flow for Cubie's navigation directions.

Source direction list:

```text
navigation_directions.md
```

## Approval Flow

1. Create simple icon candidates for all 15 approved directions.
2. Preview them in a 128x64 OLED simulator.
3. Approve or request changes.
4. Move approved icons into `DisplayManager`.

## Approved Direction Set

Normal directions:

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

Bridge variants:

12. Bridge straight
13. Bridge left
14. Bridge right
15. Bridge U-turn

## Shared Navigation Layout

All icons are previewed inside the approved navigation layout:

```text
+----------------+
|120m          C1|
|                |
|      ICON      |
|                |
|28km/h       1h8m|
+----------------+
```

## Icon Rules

- Monochrome
- Bold and readable
- No tiny details
- Designed for 128x64 OLED
- Bridge variants use one bridge base mark plus a direction arrow
- Roundabout uses one generic icon

## Simulator Code

```text
navigation_icon_simulator/navigation_icon_simulator.ino
```

## Current Status

```text
Approved
```

Approval rule:

```text
All navigation icons approved after simulator review.
```

## Icon Approval Checklist

| ID | Direction | Status | Notes |
| --- | --- | --- | --- |
| `straight` | Straight | Approved | Forward arrow |
| `left` | Left | Approved | 90-degree left |
| `right` | Right | Approved | 90-degree right |
| `slight_left` | Slight left | Approved | Straight arrow rotated -45 degrees |
| `slight_right` | Slight right | Approved | Straight arrow rotated +45 degrees |
| `sharp_left` | Sharp left | Reuses left | No separate icon |
| `sharp_right` | Sharp right | Reuses right | No separate icon |
| `uturn_left` | U-turn left | Approved | Curved U left |
| `uturn_right` | U-turn right | Approved | Curved U right |
| `roundabout` | Roundabout | Approved | One generic circular arrow |
| `arrived` | Arrived | Approved | Target/arrival mark |
| `bridge_straight` | Bridge straight | Approved | Bridge + straight |
| `bridge_left` | Bridge left | Approved | Bridge + left |
| `bridge_right` | Bridge right | Approved | Bridge + right |
| `bridge_uturn` | Bridge U-turn | Approved | Bridge + U-turn |
