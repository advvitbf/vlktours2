# Cubie Companion App

Native Android app for Cubie.

Current first slice:

- Scan for Cubie over BLE using the Chronos UART service UUID.
- Connect to Cubie.
- Multi-select OLED emotion packs from the phone app.
- Send notification and call events through Android notification access.
- Forward live music metadata to Cubie while the app is open and connected.
  The app also looks up synced lyrics from LRCLIB using the detected song and
  artist, then sends timestamped lyric lines when a match is available. Enable
  Android notification access from Settings > Phone events > Music lyrics.
- Spotify API support: create a Spotify Developer app, add redirect URI
  `cubie://spotify-callback`, paste its Client ID into Cubie Settings >
  Phone events > Music lyrics, then tap Connect Spotify. The app polls Spotify's
  currently-playing endpoint and uses that track data for lyrics.

Eye pack command:

```text
CB 01 00 = Angry
CB 01 01 = Angry 2
CB 01 02 = Confused 2
CB 01 03 = Content
CB 01 04 = Determined
CB 01 05 = Embarrassed
CB 01 06 = Excited 2
CB 01 07 = Frustrated
CB 01 08 = Happy
CB 01 09 = Happy 2
CB 01 0A = Intro
CB 01 0B = Laugh
CB 01 0C = Love
CB 01 0D = Music
CB 01 0E = Proud
CB 01 0F = Relaxed
CB 01 10 = Sleepy
CB 01 11 = Sleepy 3
CB 04 XX XX XX = selected emotion bitmask
CB 02 appLen titleLen msgLen app title msg = notification
CB 03 active callerLen caller = call state
Music metadata and lyric lines reuse CB 02. Metadata uses the player app name;
synced lyric lines use app = Lyrics, title = current song/title, msg = lyric line.
```

Open this folder in Android Studio:

```text
C:\Users\dell\OneDrive\Documents\New project\cubie_app
```

Then click **Sync Project**, then **Run**.
