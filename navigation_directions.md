# Navigation Directions Approval

This file tracks all navigation maneuvers Cubie should support.

Source:

- Current project plan
- Google Maps navigation behavior through Chronos
- Cubie V1 OLED constraints

## Current Approved Scope

V1 navigation source:

```text
Google Maps only
```

Cubie does not calculate routes. Cubie displays navigation data received from the phone through Chronos.

Navigation priority:

- Navigation is highest priority.
- Messages cannot interrupt navigation.
- Incoming calls cannot interrupt navigation.
- Only missed-call count/indicator can appear during navigation.

## Directions Currently Listed In The Plan

Status:

```text
Approved
```

Current list:

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

## Recommended V1 Direction Set

These are the directions I recommend we support first.

| ID | Direction | Need Unique Icon? | Notes |
| --- | --- | --- | --- |
| `straight` | Straight | Yes | Forward arrow |
| `left` | Left | Yes | 90-degree left arrow |
| `right` | Right | Yes | 90-degree right arrow |
| `slight_left` | Slight left | Yes | Straight arrow rotated -45 degrees |
| `slight_right` | Slight right | Yes | Straight arrow rotated +45 degrees |
| `sharp_left` | Sharp left | Reuse left | Use normal left icon |
| `sharp_right` | Sharp right | Reuse right | Use normal right icon |
| `uturn_left` | U-turn left | Yes | Curved U arrow left |
| `uturn_right` | U-turn right | Yes | Curved U arrow right |
| `roundabout` | Roundabout | Yes | Circular arrow |
| `arrived` | Arrived | Yes | Flag/check/dot target |
| `bridge_straight` | Bridge straight | Uses bridge base icon | Bridge icon with forward arrow |
| `bridge_left` | Bridge left | Uses bridge base icon | Bridge icon with left arrow |
| `bridge_right` | Bridge right | Uses bridge base icon | Bridge icon with right arrow |
| `bridge_uturn` | Bridge U-turn | Uses bridge base icon | Bridge icon with U-turn arrow |

## Bridge Direction Variants

Bridge directions are special cases where the instruction mentions a bridge.

Example:

```text
Take the bridge on right turn
```

Cubie should use one bridge base icon and combine it with the direction arrow.

Approved bridge variants:

| ID | Meaning | Display Idea |
| --- | --- | --- |
| `bridge_straight` | Take bridge ahead | Bridge icon + straight arrow |
| `bridge_left` | Take bridge on left | Bridge icon + left arrow |
| `bridge_right` | Take bridge on right | Bridge icon + right arrow |
| `bridge_uturn` | Bridge route requires U-turn/return | Bridge icon + U-turn arrow |

## Extra Directions To Consider

Google Maps/Chronos may sometimes provide navigation text that does not map perfectly to the 11 directions above.

These can be handled later or mapped to existing icons.

| Possible Direction | V1 Handling Recommendation |
| --- | --- |
| Keep left | Reuse left |
| Keep right | Reuse right |
| Take exit | Reuse right, left, or roundabout based on text |
| Exit roundabout | Reuse roundabout |
| Take bridge straight | Use bridge straight |
| Take bridge on left | Use bridge left |
| Take bridge on right | Use bridge right |
| Bridge U-turn/return | Use bridge U-turn |
| Merge | Reuse slight left/right if direction is known |
| Continue | Reuse straight |
| Head north/south/east/west | Reuse straight |
| Ferry | Reuse straight or generic info |
| Destination on left | Reuse left |
| Destination on right | Reuse right |
| Destination reached | Reuse arrived |
| Rerouting | Generic info text, no unique icon |
| No navigation active | Return to eyes |

## Icon Design Requirements

Target:

```text
128x64 OLED
```

Navigation layout:

```text
120m          C1

       ->

28km/h        1h8m
```

Icon area:

- Center of screen
- Large and readable
- Monochrome
- No tiny details
- Must be readable while riding/walking

## Approval

Final decisions:

1. Approved: the expanded 15-direction V1 set is correct.
2. Approved: `keep left` reuses left, and `keep right` reuses right.
3. Approved: `destination on left` reuses left, `destination on right` reuses right, and `destination reached` reuses arrived.
4. Approved: roundabout uses one generic icon.
5. Approved: bridge uses one base bridge icon combined with direction arrows.
