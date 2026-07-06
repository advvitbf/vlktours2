# Software Setup

This guide sets up the computer so Cubie firmware can be built, tested, and later uploaded to the ESP32-C3 Super Mini.

Current local status:

- `pio` is not installed on this PC
- `python` is not available on PATH
- `py` is not available on PATH
- The firmware files exist, but they have not been compiled locally yet

## What You Are Installing

You need these tools:

- VS Code: the code editor
- PlatformIO: the ESP32 build/upload tool inside VS Code
- Git for Windows: used to download and manage code
- USB serial driver: only needed if Windows does not detect the ESP32-C3 board

Official links:

- VS Code: https://code.visualstudio.com/download
- PlatformIO for VS Code: https://docs.platformio.org/en/latest/integration/ide/vscode.html
- PlatformIO Marketplace page: https://marketplace.visualstudio.com/items?itemName=platformio.platformio-ide
- Git for Windows: https://git-scm.com/install/windows
- CP210x USB driver: https://www.silabs.com/software-and-tools/usb-to-uart-bridge-vcp-drivers

## Step 1: Install VS Code

1. Open this link:

```text
https://code.visualstudio.com/download
```

2. Click `Windows`.
3. Download the `User Installer x64`.
4. Open the downloaded installer.
5. Keep the default options.
6. On the installer screen with checkboxes, enable these if shown:

```text
Add "Open with Code" action to Windows Explorer file context menu
Add "Open with Code" action to Windows Explorer directory context menu
Add to PATH
```

7. Finish the install.
8. Open VS Code once.

## Step 2: Install Git For Windows

1. Open this link:

```text
https://git-scm.com/install/windows
```

2. Download the latest `Git for Windows/x64 Setup`.
3. Open the installer.
4. You can keep almost all default options.
5. When asked about the default editor, choose `Use Visual Studio Code as Git's default editor` if available.
6. When asked about PATH, choose:

```text
Git from the command line and also from 3rd-party software
```

7. Finish the install.
8. Close and reopen PowerShell.
9. Test Git:

```powershell
git --version
```

Expected result:

```text
git version ...
```

Any version number is fine.

## Step 3: Install PlatformIO In VS Code

1. Open VS Code.
2. Click the Extensions icon on the left sidebar.
3. Search:

```text
PlatformIO IDE
```

4. Install the extension named `PlatformIO IDE`.
5. Publisher should be `PlatformIO`.
6. Wait for installation to finish.
7. Restart VS Code if it asks.
8. After restart, wait a few minutes. PlatformIO installs its own Python and tools in the background.
9. You should see an alien/head icon on the left sidebar. That is PlatformIO.

If the extension search is confusing, open this link:

```text
https://marketplace.visualstudio.com/items?itemName=platformio.platformio-ide
```

Then click `Install`.

## Step 4: Open The Cubie Project

1. Open VS Code.
2. Click:

```text
File > Open Folder
```

3. Select this folder:

```text
C:\Users\dell\OneDrive\Documents\New project
```

4. Click `Select Folder`.
5. If VS Code asks whether you trust the authors, click:

```text
Yes, I trust the authors
```

6. Wait for PlatformIO to notice `platformio.ini`.
7. It may start installing ESP32 tools. Let it finish.

This can take several minutes the first time.

## Step 5: Build The Firmware

This checks if the code compiles. You do not need hardware for this step.

Option A, using buttons:

1. Click the PlatformIO alien/head icon on the left.
2. Open:

```text
PROJECT TASKS > cubie_c3 > General
```

3. Click:

```text
Build
```

Option B, using the bottom bar:

1. Look at the bottom blue bar in VS Code.
2. Click the checkmark icon.

Expected result:

```text
SUCCESS
```

If it fails, copy the red error text and save it. That error tells us exactly what to fix.

## Step 6: Install USB Driver If Needed

You only need this once you have the ESP32-C3 Super Mini connected by USB.

Many ESP32-C3 Super Mini boards use a USB serial chip such as CP210x. If Windows does not show the board as a COM port, install the driver.

1. Open this link:

```text
https://www.silabs.com/software-and-tools/usb-to-uart-bridge-vcp-drivers
```

2. Download the Windows CP210x VCP driver.
3. Install it.
4. Unplug and reconnect the ESP32-C3.
5. Open Windows Device Manager.
6. Look under:

```text
Ports (COM & LPT)
```

7. You should see something like:

```text
Silicon Labs CP210x USB to UART Bridge (COM3)
```

The COM number may be different.

Note: some ESP32-C3 boards use native USB and may not need this driver.

## Step 7: Upload Later When Hardware Arrives

Do this only after the ESP32-C3 is plugged in.

1. Open VS Code.
2. Open the Cubie project folder.
3. Click the PlatformIO alien/head icon.
4. Open:

```text
PROJECT TASKS > cubie_c3 > General
```

5. Click:

```text
Upload
```

If upload fails, hold the board `BOOT` button, click upload again, and release `BOOT` when upload starts.

Some boards may need this button sequence:

1. Hold `BOOT`.
2. Tap `RESET`.
3. Release `BOOT`.
4. Click `Upload`.

## Step 8: Open Serial Monitor

Serial Monitor lets you see Cubie debug text.

1. Click the PlatformIO alien/head icon.
2. Open:

```text
PROJECT TASKS > cubie_c3 > General
```

3. Click:

```text
Monitor
```

Expected speed:

```text
115200
```

Expected startup text:

```text
Cubie firmware started
BLE address: ...
```

For now, Cubie renders screens to Serial instead of OLED because hardware is not connected yet.

## Step 8A: Run Simulator Mode

Simulator mode is currently enabled in `platformio.ini`:

```ini
build_flags =
    -D CUBIE_SIMULATOR=1
```

This lets Cubie fake events without needing the OLED, touch sensor, or a real Chronos phone connection.

When simulator mode is enabled, physical GPIO4 touch polling is disabled so a floating touch pin cannot create random touch events.

To use it:

1. Build the project.
2. Upload to the ESP32-C3 when hardware is available.
3. Open Serial Monitor.
4. Watch the fake event sequence.

Expected simulator sequence:

```text
[Simulator] Enabled
[Simulator] BLE connected
[Simulator] Notification on eyes screen
[Simulator] Navigation starts
[Simulator] Message arrives during navigation
[Cubie] Notification stored; navigation stays on screen
[Simulator] Call arrives during navigation
[Cubie] Call event stored/ignored; navigation stays on screen
[Simulator] Navigation ends
[Simulator] Incoming call outside navigation
[Simulator] Cubie battery drops to 10%
[Simulator] Touch single tap
[Simulator] Touch double tap
[Simulator] Touch triple tap exits to eyes
[Simulator] Touch during navigation
[Simulator] Long press enters sleep
[Simulator] Any tap wakes from sleep
```

Rules this tests:

- Eyes are the default screen.
- Messages can show on the eyes screen.
- Navigation takes over when active.
- Messages cannot interrupt navigation.
- Calls cannot interrupt navigation.
- Calls can show when navigation is inactive.
- Low battery warning can take over when nothing higher priority is active.
- Single tap moves through manual screens.
- Double tap selects.
- Triple tap exits to eyes.
- Touch is ignored during navigation.
- Long press blanks the screen when sleep is active.
- Any tap wakes from sleep.

To disable simulator mode later, change:

```ini
-D CUBIE_SIMULATOR=1
```

to:

```ini
-D CUBIE_SIMULATOR=0
```

## Step 9: Pair With Chronos App Later

After firmware is uploaded:

1. Open the Chronos app on the phone.
2. Go to the watches/devices area.
3. Choose pair new device.
4. Look for:

```text
Cubie
```

5. Pair with it.
6. Start Google Maps navigation.
7. Cubie should receive navigation data through Chronos.

Important:

- Do not pair the ESP32 from the normal Android Bluetooth settings first unless Chronos specifically asks.
- Pair from inside the Chronos app.

## Firmware Project Files

```text
platformio.ini
src/main.cpp
src/CubieApp.cpp
src/CubieApp.h
src/CubieConfig.h
src/CubieModels.h
src/CubieSimulator.cpp
src/CubieSimulator.h
src/DisplayManager.cpp
src/DisplayManager.h
```

## Firmware Architecture

Current structure:

```text
ChronosESP32
  |
  | BLE events
  v
CubieApp
  |
  | DisplayFrame
  v
DisplayManager
  |
  | Serial output for now
  v
Serial Monitor
```

Responsibilities:

- `ChronosESP32`: receives BLE data from the Chronos app
- `CubieApp`: decides Cubie's state and priority rules
- `DisplayFrame`: carries screen data in a hardware-neutral format
- `DisplayManager`: renders the current screen
- `CubieSimulator`: creates fake events for software-only testing

OLED rendering now lives inside `DisplayManager`, while Serial output remains as a debug backend. CubieApp should not directly draw to the OLED.

## Current Firmware Behavior

For software-only testing, rendering is sent to Serial instead of the OLED.

Implemented:

- Chronos BLE startup
- BLE connection callback
- Notification callback
- Ringer/incoming call callback
- Chronos configuration callback
- Navigation active/inactive state
- Phone battery update
- Eyes default screen
- Navigation priority
- Messages cannot interrupt navigation
- Incoming calls cannot interrupt navigation
- Incoming call screen when not navigating
- 3-second notification preview
- 5-message notification history
- Low battery alert state
- SSD1306-compatible OLED rendering path in `DisplayManager`
- Approved screen layouts in OLED rendering path
- Approved navigation icons in OLED rendering path
- Touch input module on GPIO4
- Touch simulator events
- Sleep blank-screen state

## If Something Goes Wrong

If VS Code says PlatformIO is missing:

- Reinstall the PlatformIO IDE extension
- Restart VS Code
- Wait for the setup to finish

If build says board is unknown:

- Tell me the exact red error
- We may need to change the board in `platformio.ini`

If upload cannot find a port:

- Check the USB cable supports data, not only charging
- Try another USB port
- Install the CP210x driver
- Check Device Manager for a COM port

If upload starts but fails:

- Try the `BOOT` button method from Step 7

## If The C++ Language Server Crashes

You may see this message in VS Code:

```text
The language server crashed. Restarting...
Server process exited with code 3221226356.
Source: C/C++
```

This is usually the Microsoft C/C++ autocomplete engine crashing while PlatformIO is still setting up. It does not always mean the firmware code is broken.

First try:

1. Wait until the left PlatformIO panel stops saying:

```text
Initializing PlatformIO Core...
```

2. If it stays there longer than 10 minutes, close VS Code.
3. Reopen VS Code.
4. Open the Cubie project folder again.
5. Wait again for PlatformIO to finish.

If the C++ crash keeps happening:

1. Press `Ctrl + Shift + P`.
2. Search:

```text
Preferences: Open User Settings (JSON)
```

3. Press Enter.
4. Add this line inside the `{ }` settings object:

```json
"C_Cpp.intelliSenseEngine": "Tag Parser"
```

5. Save the file.
6. Restart VS Code.

If there are already settings inside the file, add a comma after the previous line before adding this setting.

Example:

```json
{
  "workbench.colorTheme": "Default Dark Modern",
  "C_Cpp.intelliSenseEngine": "Tag Parser"
}
```

This uses a simpler C++ parser. Autocomplete may be less smart, but it is usually more stable.

## Next Software Step

After PlatformIO builds successfully:

1. Add OLED rendering into `DisplayManager`.
2. Keep Serial rendering as a debug backend.
3. Start with SSD1306-compatible code because the simulator and approved preview used SSD1306.
4. Keep SH1106 as fallback if the real Robocraze 1.3" OLED is blank or horizontally shifted.
5. Add a touch input module with the selected timing values.
