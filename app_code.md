# App Code Notes

## Strategy

Use Chronos as the starting point instead of building the phone/BLE stack from scratch.

Recommended path:

1. Fork the original Chronos ESP32 project or app-related repository as needed.
2. Keep the original project history.
3. Add Cubie-specific display commands and settings only where needed.
4. Keep compatibility with existing Chronos navigation and notification behavior as much as possible.

## How To Fork Chronos

Basic GitHub flow:

1. Open the source repository:

```text
https://github.com/fbiego/chronos-esp32
```

2. Click `Fork` on GitHub.
3. Choose your GitHub account.
4. Name the fork or keep the default name.
5. Clone your fork:

```bash
git clone https://github.com/YOUR_USERNAME/chronos-esp32.git
```

6. Add the original project as upstream:

```bash
git remote add upstream https://github.com/fbiego/chronos-esp32.git
```

7. Pull future upstream changes when needed:

```bash
git fetch upstream
git merge upstream/main
```

If the Cubie repository is the active firmware repository, Chronos can also be used as a library or reference instead of making the Cubie repo itself a fork.

## Data Cubie Needs

Navigation:

- Maneuver type
- Distance to next turn
- Speed
- ETA
- Navigation active/inactive

Notifications:

- App name
- App icon/category if available
- Message preview
- Timestamp

Calls:

- Incoming call active
- Caller name
- Missed call count/event

Battery:

- Phone battery percentage
- Charging state if available

Settings:

- Brightness
- Sleep timer
- Buzzer enabled/disabled

