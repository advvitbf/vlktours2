package com.cubie.companion;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import android.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class MainActivity extends Activity implements BleCubieClient.Listener {
    private static final String TAG = "CubieCompanion";
    private static final int BG = Color.rgb(0, 0, 0);
    private static final int SURFACE = Color.rgb(13, 13, 13);
    private static final int SURFACE_2 = Color.rgb(23, 23, 23);
    private static final int TEXT = Color.rgb(246, 246, 241);
    private static final int MUTED = Color.rgb(142, 142, 134);
    private static final int ACCENT = Color.rgb(246, 246, 241);
    private static final int ORANGE = Color.rgb(255, 54, 48);
    private static final int RED = Color.rgb(255, 54, 48);
    private static final int BORDER = Color.rgb(38, 38, 34);
    private static final Typeface FONT_BODY = Typeface.create("sans-serif-light", Typeface.NORMAL);
    private static final Typeface FONT_MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private static final Typeface FONT_MONO = Typeface.create("monospace", Typeface.NORMAL);
    private Typeface fontBody = FONT_BODY;
    private Typeface fontMedium = FONT_MEDIUM;
    private Typeface fontMono = FONT_MONO;
    private Typeface fontDot = FONT_MONO;

    private static final int TAB_HOME = 0;
    private static final int TAB_DEVICE = 1;
    private static final int TAB_FACES = 2;
    private static final int TAB_SETTINGS = 3;
    private static final int FACE_MODE_EMOTIONS = 0;
    private static final int FACE_MODE_PETTING = 1;
    private static final int SETTINGS_ROOT = 0;
    private static final int SETTINGS_CUBIE = 1;
    private static final int SETTINGS_PHONE_EVENTS = 2;
    private static final int SETTINGS_NOTIFICATION_APPS = 3;
    private static final int SETTINGS_CALL_PEOPLE = 4;
    private static final int SETTINGS_PHONE_NOTIFICATIONS = 5;
    private static final int SETTINGS_PHONE_CALLS = 6;
    private static final int SETTINGS_MUSIC_LYRICS = 7;
    private static final String PREFS = "cubie_companion";
    private static final String PREF_PET_NAME = "petName";
    private static final String PREF_APP_LIST = "cachedApps";
    private static final String PREF_APP_REFRESH_AT = "appsRefreshAt";
    private static final String PREF_CONTACT_LIST = "cachedContacts";
    private static final String PREF_CONTACT_REFRESH_AT = "contactsRefreshAt";
    private static final long APP_REFRESH_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final long CONTACT_REFRESH_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final Pattern LRC_LINE = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)");
    private static final String SPOTIFY_REDIRECT_URI = "cubie://spotify-callback";
    private static final String SPOTIFY_AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SPOTIFY_NOW_PLAYING_URL = "https://api.spotify.com/v1/me/player/currently-playing";
    private static final String MUSIXMATCH_API_URL = "https://api.musixmatch.com/ws/1.1/";

    private static final String[] FACE_PACKS = FacePackData.NAMES;

    private final boolean[] selectedFaces = new boolean[FACE_PACKS.length];
    private LinearLayout contentRoot;
    private LinearLayout navRoot;
    private TextView latencyView;
    private BleCubieClient ble;
    private SharedPreferences prefs;
    private final Handler reconnectHandler = new Handler(Looper.getMainLooper());
    private String connectionText = "Disconnected";
    private boolean connected = false;
    private boolean autoConnectEnabled = false;
    private String petName = "Cubie";
    private String latencyText = "-- ms";
    private boolean petCallActive = false;
    private boolean sleepEnabled = true;
    private boolean buzzerEnabled = true;
    private int brightnessIndex = 1;
    private int sleepTimerMinutes = 0;
    private boolean notificationsEnabled = true;
    private boolean silentNotificationsEnabled = false;
    private boolean callsEnabled = true;
    private boolean navigationEnabled = true;
    private boolean musicLyricsEnabled = true;
    private int settingsPage = SETTINGS_ROOT;
    private final List<Integer> settingsBackStack = new ArrayList<>();
    private List<AppChoice> visibleAppsCache;
    private List<String> contactNamesCache = new ArrayList<>();
    private boolean appRefreshRunning = false;
    private boolean contactRefreshRunning = false;
    private String appSearchQuery = "";
    private String callerSearchQuery = "";
    private int activeTab = TAB_HOME;
    private int previewFace = 0;
    private int pettingFace = 0;
    private int faceMode = FACE_MODE_EMOTIONS;
    private String lastMusicTitle = "No music yet";
    private String lastMusicLine = "Play a song with lyrics enabled.";
    private String activeLyricsKey = "";
    private List<LrcLine> activeLyrics = new ArrayList<>();
    private boolean lyricsLookupRunning = false;
    private long playbackPositionMs = 0;
    private long playbackClockAtMs = 0;
    private boolean playbackRunning = false;
    private int lastSentLyricIndex = -1;
    private String spotifyClientId = "";
    private String spotifyAccessToken = "";
    private String spotifyRefreshToken = "";
    private long spotifyTokenExpiresAt = 0;
    private String spotifyStatus = "Not connected";
    private String spotifyCodeVerifier = "";
    private String spotifyAuthState = "";
    private String lastSpotifyTrackKey = "";
    private String musixmatchApiKey = "";
    private final Handler latencyHandler = new Handler(Looper.getMainLooper());
    private final Handler lyricsHandler = new Handler(Looper.getMainLooper());
    private final Handler spotifyHandler = new Handler(Looper.getMainLooper());
    private final Runnable latencyPoller = new Runnable() {
        @Override
        public void run() {
            if (connected) {
                ble.sendLatencyPing();
                latencyHandler.postDelayed(this, 500);
            }
        }
    };
    private final Runnable spotifyPoller = new Runnable() {
        @Override
        public void run() {
            pollSpotifyNowPlaying();
            spotifyHandler.postDelayed(this, 5000);
        }
    };
    private final Runnable lyricsTicker = new Runnable() {
        @Override
        public void run() {
            sendCurrentLyricLine();
            lyricsHandler.postDelayed(this, 500);
        }
    };
    private final BroadcastReceiver musicTextReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MusicLyricNotificationService.ACTION_MUSIC_TEXT.equals(action)) {
                String packageName = intent.getStringExtra(MusicLyricNotificationService.EXTRA_PACKAGE);
                String title = intent.getStringExtra(MusicLyricNotificationService.EXTRA_TITLE);
                String text = intent.getStringExtra(MusicLyricNotificationService.EXTRA_TEXT);
                long durationMs = intent.getLongExtra(MusicLyricNotificationService.EXTRA_DURATION_MS, 0);
                long positionMs = intent.getLongExtra(MusicLyricNotificationService.EXTRA_POSITION_MS, 0);
                boolean playing = intent.getBooleanExtra(MusicLyricNotificationService.EXTRA_PLAYING, true);
                handleMusicText(packageName, title, text, durationMs, positionMs, playing);
            } else if (MusicLyricNotificationService.ACTION_NAVIGATION_EVENT.equals(action)) {
                handleNavigationEvent(intent);
            } else if (MusicLyricNotificationService.ACTION_PHONE_NOTIFICATION.equals(action)) {
                handlePhoneNotificationEvent(intent);
            }
        }
    };
    private final Runnable reconnectRunnable = () -> {
        if (autoConnectEnabled && !connected) {
            connectionText = "Reconnecting...";
            showTab(activeTab);
            ble.scanAndConnect();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadNothingTypefaces();
        loadSavedListCaches();
        loadAppSettings();
        ble = new BleCubieClient(this, this);
        requestNeededPermissions();
        registerMusicTextReceiver();
        setContentView(buildUi());
        showTab(TAB_HOME);
        promptForPetNameIfNeeded();
    }

    private void loadNothingTypefaces() {
        fontBody = assetTypeface("fonts/NType82-Regular.ttf",
                assetTypeface("fonts/NType82.ttf", FONT_BODY));
        fontMedium = assetTypeface("fonts/NType82-Medium.ttf",
                assetTypeface("fonts/NType82-Regular.ttf", FONT_MEDIUM));
        fontMono = assetTypeface("fonts/NType82Mono-Regular.ttf",
                assetTypeface("fonts/LLLetteraMono-Regular.ttf", FONT_MONO));
        fontDot = assetTypeface("fonts/NDot57.ttf",
                assetTypeface("fonts/NDot55.ttf", FONT_MONO));
    }

    private Typeface assetTypeface(String path, Typeface fallback) {
        try {
            return Typeface.createFromAsset(getAssets(), path);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSpotifyRedirect(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleSpotifyRedirect(getIntent());
        if (!spotifyRefreshToken.isEmpty()) {
            spotifyHandler.removeCallbacks(spotifyPoller);
            spotifyHandler.post(spotifyPoller);
        }
    }

    @Override
    protected void onPause() {
        spotifyHandler.removeCallbacks(spotifyPoller);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        autoConnectEnabled = false;
        reconnectHandler.removeCallbacks(reconnectRunnable);
        latencyHandler.removeCallbacks(latencyPoller);
        spotifyHandler.removeCallbacks(spotifyPoller);
        lyricsHandler.removeCallbacks(lyricsTicker);
        unregisterReceiver(musicTextReceiver);
        ble.disconnect();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (activeTab == TAB_SETTINGS) {
            if (!settingsBackStack.isEmpty()) {
                settingsPage = settingsBackStack.remove(settingsBackStack.size() - 1);
                showTab(TAB_SETTINGS);
                return;
            }
            if (settingsPage != SETTINGS_ROOT) {
                settingsPage = SETTINGS_ROOT;
                showTab(TAB_SETTINGS);
                return;
            }
        }

        if (activeTab != TAB_HOME) {
            activeTab = TAB_HOME;
            showTab(TAB_HOME);
        }
    }

    @Override
    public void onState(String text, boolean isConnected) {
        runOnUiThread(() -> {
            connectionText = text;
            connected = isConnected;
            if (!isConnected) {
                latencyText = "-- ms";
                petCallActive = false;
                latencyHandler.removeCallbacks(latencyPoller);
            }
            showTab(activeTab);
            if (!isConnected && autoConnectEnabled && "Disconnected".equals(text)) {
                scheduleReconnect();
            } else if (isConnected) {
                reconnectHandler.removeCallbacks(reconnectRunnable);
                pushDeviceSettings();
                startLatencyPolling();
            }
        });
    }

    @Override
    public void onLatency(long latencyMs) {
        runOnUiThread(() -> {
            latencyText = latencyMs + " ms";
            refreshLatencyView();
        });
    }

    @Override
    public void onPetCallState(boolean active) {
        runOnUiThread(() -> {
            petCallActive = active;
            showTab(activeTab);
        });
    }

    private void startLatencyPolling() {
        latencyHandler.removeCallbacks(latencyPoller);
        latencyHandler.post(latencyPoller);
    }

    @Override
    public void onError(String text) {
        runOnUiThread(() -> {
            connectionText = text;
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            showTab(activeTab);
            if (autoConnectEnabled && !connected) {
                scheduleReconnect();
            }
        });
    }

    private View buildUi() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        contentRoot = new LinearLayout(this);
        contentRoot.setOrientation(LinearLayout.VERTICAL);
        contentRoot.setPadding(dp(18), statusBarHeight() + dp(18), dp(18), dp(22));
        scroll.addView(contentRoot);

        navRoot = new LinearLayout(this);
        shell.addView(navRoot, new LinearLayout.LayoutParams(-1, dp(64)));
        return shell;
    }

    private int statusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return dp(24);
    }

    private void showTab(int tab) {
        activeTab = tab;
        contentRoot.removeAllViews();
        contentRoot.addView(topBar(titleForTab(tab), subtitleForTab(tab)));

        if (tab == TAB_HOME) {
            renderHome();
        } else if (tab == TAB_DEVICE) {
            renderDevice();
        } else if (tab == TAB_FACES) {
            renderFaces();
        } else {
            renderSettings();
        }

        renderBottomNav();
    }

    private void renderHome() {
        contentRoot.addView(deviceHero());
        contentRoot.addView(sectionTitle("Device"));
        contentRoot.addView(deviceStatusCard());
        contentRoot.addView(actionCard(
                "Cubie connection",
                connectionText,
                autoConnectEnabled ? "Auto reconnect is on." : "Auto reconnect is off.",
                autoConnectEnabled ? "Connection ON" : "Connection OFF",
                v -> toggleConnection()));
    }

    private void renderDevice() {
        contentRoot.addView(deviceHero());
        contentRoot.addView(sectionTitle("Connection"));
        contentRoot.addView(deviceStatusCard());
        contentRoot.addView(actionCard(
                "Bluetooth",
                connectionText,
                autoConnectEnabled ? "Auto reconnect is on." : "Scan toggle is off.",
                autoConnectEnabled ? "Connection ON" : "Connection OFF",
                v -> toggleConnection()));
        contentRoot.addView(wideCard("Battery", "Cubie 80% / Phone unknown", "Battery values will sync from firmware later."));
        contentRoot.addView(wideCard("Firmware", "Cubie v1.1", "Main firmware is ready."));
    }

    private void renderFaces() {
        contentRoot.addView(sectionTitle("Phone preview"));
        contentRoot.addView(facePreviewCard());
        contentRoot.addView(faceModeSelector());
        contentRoot.addView(sectionTitle(faceMode == FACE_MODE_EMOTIONS ? "Emotions" : "Petting animation"));
        contentRoot.addView(faceMode == FACE_MODE_EMOTIONS ? faceGrid() : pettingGrid());
        contentRoot.addView(saveFacesButton());
    }

    private void renderSettings() {
        if (settingsPage == SETTINGS_CUBIE) {
            renderCubieSettingsPage();
            return;
        }
        if (settingsPage == SETTINGS_PHONE_EVENTS) {
            renderPhoneEventsPage();
            return;
        }
        if (settingsPage == SETTINGS_NOTIFICATION_APPS) {
            renderNotificationAppsPage();
            return;
        }
        if (settingsPage == SETTINGS_CALL_PEOPLE) {
            renderCallPeoplePage();
            return;
        }
        if (settingsPage == SETTINGS_PHONE_NOTIFICATIONS) {
            renderPhoneNotificationsPage();
            return;
        }
        if (settingsPage == SETTINGS_PHONE_CALLS) {
            renderPhoneCallsPage();
            return;
        }
        if (settingsPage == SETTINGS_MUSIC_LYRICS) {
            renderMusicLyricsPage();
            return;
        }

        contentRoot.addView(sectionTitle("Settings"));
        contentRoot.addView(actionCard(
                "Cubie",
                "Sleep, timer, buzzer, brightness",
                "Controls that are sent directly to the device.",
                "Open",
                v -> openSettingsPage(SETTINGS_CUBIE)));
        contentRoot.addView(actionCard(
                "Phone events",
                "Notifications, calls, navigation",
                "Choose what the phone is allowed to forward.",
                "Open",
                v -> openSettingsPage(SETTINGS_PHONE_EVENTS)));
    }

    private void renderCubieSettingsPage() {
        contentRoot.addView(sectionTitle("Cubie"));
        contentRoot.addView(actionCard(
                "Call " + petName,
                petCallActive ? "Calling" : "Ready",
                petCallActive ? petName + " is beeping until touch or stop." : "Make " + petName + " beep so you can find it.",
                petCallActive ? "Stop call" : "Call",
                v -> togglePetCall()));
        contentRoot.addView(actionCard(
                "Sleep",
                sleepEnabled ? "Enabled" : "Disabled",
                sleepEnabled ? "Long press can sleep from Eyes." : "Cubie will stay awake.",
                sleepEnabled ? "Sleep ON" : "Sleep OFF",
                v -> toggleSleep()));
        contentRoot.addView(actionCard(
                "Buzzer",
                buzzerEnabled ? "Enabled" : "Disabled",
                buzzerEnabled ? "Alerts and touch feedback are on." : "Buzzer feedback is muted.",
                buzzerEnabled ? "Buzzer ON" : "Buzzer OFF",
                v -> toggleBuzzer()));
        contentRoot.addView(actionCard(
                "Brightness",
                brightnessLabel(),
                "Tap to cycle OLED contrast.",
                "Change brightness",
                v -> cycleBrightnessSetting()));
        contentRoot.addView(actionCard(
                "Sleep timer",
                sleepTimerLabel(),
                sleepTimerMinutes == 0 ? "Cubie sleeps only by long press." : "Cubie sleeps after being idle.",
                "Change timer",
                v -> cycleSleepTimer()));
    }

    private void renderPhoneEventsPage() {
        contentRoot.addView(sectionTitle("Phone events"));
        contentRoot.addView(actionCard(
                "Notification access",
                isNotificationListenerEnabled() ? "Allowed" : "Needs permission",
                "Required for navigation, notifications, and lyrics.",
                isNotificationListenerEnabled() ? "Open settings" : "Allow access",
                v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))));
        contentRoot.addView(actionCard(
                "Notifications",
                notificationsEnabled ? "Enabled" : "Disabled",
                "Notification toggle and app selection.",
                "Open",
                v -> openSettingsPage(SETTINGS_PHONE_NOTIFICATIONS)));
        contentRoot.addView(actionCard(
                "Calls",
                callsEnabled ? "Enabled" : "Disabled",
                "Call toggle and blocked people.",
                "Open",
                v -> openSettingsPage(SETTINGS_PHONE_CALLS)));
        contentRoot.addView(actionCard(
                "Navigation",
                navigationEnabled ? "Enabled" : "Disabled",
                "Controls Google Maps direction forwarding.",
                navigationEnabled ? "Navigation ON" : "Navigation OFF",
                v -> togglePhoneEvent(2)));
        contentRoot.addView(actionCard(
                "Music lyrics",
                musicLyricsEnabled ? "Enabled" : "Disabled",
                "Shows live lyric lines when a music app exposes them.",
                "Open",
                v -> openSettingsPage(SETTINGS_MUSIC_LYRICS)));
    }

    private void renderPhoneNotificationsPage() {
        contentRoot.addView(sectionTitle("Notifications"));
        contentRoot.addView(actionCard(
                "Notifications",
                notificationsEnabled ? "Enabled" : "Disabled",
                "Controls whether message previews should be forwarded.",
                notificationsEnabled ? "Notifications ON" : "Notifications OFF",
                v -> togglePhoneEvent(0)));
        contentRoot.addView(actionCard(
                "Silent notifications",
                silentNotificationsEnabled ? "Enabled" : "Disabled",
                "Silent notifications are blocked by default.",
                silentNotificationsEnabled ? "Silent ON" : "Silent OFF",
                v -> toggleSilentNotifications()));
        contentRoot.addView(actionCard(
                "Notification apps",
                selectedNotificationAppCount() + " selected",
                "Choose which installed apps can send previews.",
                "Choose apps",
                v -> openSettingsPage(SETTINGS_NOTIFICATION_APPS)));
    }

    private void renderPhoneCallsPage() {
        contentRoot.addView(sectionTitle("Calls"));
        contentRoot.addView(actionCard(
                "Calls",
                callsEnabled ? "Enabled" : "Disabled",
                "Controls incoming call banners.",
                callsEnabled ? "Calls ON" : "Calls OFF",
                v -> togglePhoneEvent(1)));
        contentRoot.addView(actionCard(
                "People for calls",
                blockedCallerCount() + " blocked",
                "Choose which callers should be blocked.",
                "Block people",
                v -> openSettingsPage(SETTINGS_CALL_PEOPLE)));
    }

    private void renderMusicLyricsPage() {
        contentRoot.addView(sectionTitle("Music lyrics"));
        contentRoot.addView(actionCard(
                "Live lyrics",
                musicLyricsEnabled ? "Enabled" : "Disabled",
                "Reads currently playing music text from Android notification access.",
                musicLyricsEnabled ? "Lyrics ON" : "Lyrics OFF",
                v -> toggleMusicLyrics()));
        contentRoot.addView(actionCard(
                "Notification access",
                isNotificationListenerEnabled() ? "Allowed" : "Needs permission",
                "Allow Cubie so lyric and media notifications can be read.",
                "Open access",
                v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))));
        contentRoot.addView(spotifyClientCard());
        contentRoot.addView(actionCard(
                "Spotify API",
                spotifyStatus,
                spotifyRefreshToken.isEmpty() ? "Paste your Spotify Client ID, then connect." : "Uses Spotify currently-playing data for lyric lookup.",
                spotifyRefreshToken.isEmpty() ? "Connect Spotify" : "Reconnect",
                v -> startSpotifyLogin()));
        contentRoot.addView(musixmatchApiCard());
        contentRoot.addView(wideCard("Latest music text", lastMusicTitle, lastMusicLine));
    }

    private void renderNotificationAppsPage() {
        contentRoot.addView(sectionTitle("Notification apps"));
        contentRoot.addView(actionCard(
                "Saved app list",
                loadInstalledApps().size() + " apps",
                appRefreshRunning ? "Refreshing in background." : "Scans only when you tap Refresh.",
                "Refresh",
                v -> refreshAppCache(true)));
        contentRoot.addView(appSearchCard());
        Set<String> allowed = allowedNotificationApps();
        int visibleCount = 0;
        for (AppChoice app : loadInstalledApps()) {
            boolean selected = allowed.contains(app.key);
            boolean searchMatch = !appSearchQuery.trim().isEmpty() && appMatchesSearch(app.label);
            if (!selected && !searchMatch) {
                continue;
            }
            visibleCount++;
            contentRoot.addView(toggleRow(
                    app.label,
                    selected ? "Allowed" : "Tap to allow",
                    selected,
                    v -> toggleNotificationApp(app.key)));
        }
        if (visibleCount == 0) {
            contentRoot.addView(wideCard(loadInstalledApps().isEmpty() ? "No saved apps" : "No apps selected", appSearchQuery, "Search an app name and tap it to allow notifications."));
        }
    }

    private void renderCallPeoplePage() {
        contentRoot.addView(sectionTitle("Blocked callers"));
        contentRoot.addView(actionCard(
                "Saved contacts",
                contactNamesCache.size() + " contacts",
                contactRefreshRunning ? "Refreshing in background." : "Scans only when you tap Refresh.",
                "Refresh",
                v -> refreshContactCache(true)));
        contentRoot.addView(searchCard());
        List<String> callers = loadContactNames();
        if (callers.isEmpty()) {
            contentRoot.addView(wideCard("Contacts permission", "No contacts loaded", "Allow contacts permission, then reopen this page."));
            contentRoot.addView(actionCard(
                    "Permission",
                    "Contacts",
                    "Needed only for choosing allowed callers.",
                    "Allow contacts",
                    v -> requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 11)));
            return;
        }
        Set<String> blocked = blockedCallers();
        int visibleCount = 0;
        for (String caller : callers) {
            boolean selected = blocked.contains(caller);
            boolean searchMatch = !callerSearchQuery.trim().isEmpty() && callerMatchesSearch(caller);
            if (!selected && !searchMatch) {
                continue;
            }
            visibleCount++;
            contentRoot.addView(toggleRow(
                    caller,
                    selected ? "Blocked" : "Tap to block",
                    selected,
                    v -> toggleBlockedCaller(caller)));
        }
        if (visibleCount == 0) {
            contentRoot.addView(wideCard("No people selected", callerSearchQuery, "Search a contact name and tap it to block calls."));
        }
    }

    private View topBar(String titleText, String subtitleText) {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setPadding(0, 0, 0, dp(22));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.addView(dotLabel("CUBIE OS"));
        titleBlock.addView(label(titleText, 22, TEXT, false));
        titleBlock.addView(label(subtitleText, 11, MUTED, false));
        row.addView(titleBlock, new LinearLayout.LayoutParams(0, -2, 1));

        TextView latency = label(latencyText, 11, connected ? RED : MUTED, false);
        latency.setGravity(Gravity.CENTER);
        latency.setBackground(round(Color.TRANSPARENT, dp(2), connected ? RED : BORDER, dp(1)));
        latencyView = latency;
        row.addView(latency, new LinearLayout.LayoutParams(dp(68), dp(28)));
        bar.addView(row);
        bar.addView(glyphRail(), new LinearLayout.LayoutParams(-1, dp(18)));
        return bar;
    }

    private void refreshLatencyView() {
        if (latencyView == null) {
            return;
        }
        latencyView.setText(latencyText);
        latencyView.setTextColor(connected ? RED : MUTED);
        latencyView.setBackground(round(Color.TRANSPARENT, dp(2), connected ? RED : BORDER, dp(1)));
    }

    private String titleForTab(int tab) {
        if (tab == TAB_DEVICE) {
            return "Device";
        }
        if (tab == TAB_FACES) {
            return "Faces";
        }
        if (tab == TAB_SETTINGS) {
            if (settingsPage == SETTINGS_CUBIE) {
                return "Cubie";
            }
            if (settingsPage == SETTINGS_PHONE_EVENTS) {
                return "Phone events";
            }
            if (settingsPage == SETTINGS_NOTIFICATION_APPS) {
                return "Apps";
            }
            if (settingsPage == SETTINGS_CALL_PEOPLE) {
                return "People";
            }
            if (settingsPage == SETTINGS_PHONE_NOTIFICATIONS) {
                return "Notifications";
            }
            if (settingsPage == SETTINGS_PHONE_CALLS) {
                return "Calls";
            }
            if (settingsPage == SETTINGS_MUSIC_LYRICS) {
                return "Music";
            }
            return "Settings";
        }
        return petName;
    }

    private String subtitleForTab(int tab) {
        if (tab == TAB_DEVICE) {
            return "Cubie connection";
        }
        if (tab == TAB_FACES) {
            return "Preview, select, save";
        }
        if (tab == TAB_SETTINGS) {
            if (settingsPage == SETTINGS_CUBIE) {
                return "Device controls";
            }
            if (settingsPage == SETTINGS_PHONE_EVENTS) {
                return "Forwarding controls";
            }
            if (settingsPage == SETTINGS_NOTIFICATION_APPS) {
                return "Notification allow list";
            }
            if (settingsPage == SETTINGS_CALL_PEOPLE) {
                return "Call block list";
            }
            if (settingsPage == SETTINGS_PHONE_NOTIFICATIONS) {
                return "Toggle and app filter";
            }
            if (settingsPage == SETTINGS_PHONE_CALLS) {
                return "Toggle and blocked people";
            }
            if (settingsPage == SETTINGS_MUSIC_LYRICS) {
                return "Live lyric forwarding";
            }
            return "Cubie and phone events";
        }
        return "Cubie companion shell";
    }

    private View deviceHero() {
        LinearLayout card = card(SURFACE, 18);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(label("Cubie", 26, TEXT, false));
        copy.addView(label(connectionText, 13, MUTED, false));
        TextView scan = pill(autoConnectEnabled ? "Connection ON" : "Connection OFF");
        scan.setOnClickListener(v -> toggleConnection());
        copy.addView(scan);
        card.addView(copy);

        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, dp(126));
        previewParams.setMargins(0, dp(16), 0, 0);
        card.addView(cubiePreview(previewFace), previewParams);
        return withMargins(card, 0, 0, 0, 18);
    }

    private View cubiePreview(int faceIndex) {
        FrameLayout outer = new FrameLayout(this);
        outer.setBackground(round(Color.TRANSPARENT, dp(4), BORDER, dp(1)));

        OledPreviewView screen = new OledPreviewView(this);
        screen.setFacePack(faceIndex);
        screen.setBackground(round(Color.rgb(0, 0, 0), dp(4), ACCENT, dp(1)));
        FrameLayout.LayoutParams screenParams = new FrameLayout.LayoutParams(dp(256), dp(128), Gravity.CENTER);
        outer.addView(screen, screenParams);
        return outer;
    }

    private View deviceStatusCard() {
        LinearLayout card = card(SURFACE_2, 14);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.addView(label(connected ? "Cubie connected" : "No active Cubie", 18, TEXT, true));
        left.addView(label("Connect Cubie to sync faces and phone events.", 13, MUTED, false));
        row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));

        TextView dot = label(connected ? "ON" : "OFF", 11, connected ? TEXT : RED, false);
        dot.setGravity(Gravity.CENTER);
        dot.setBackground(round(Color.TRANSPARENT, dp(2), connected ? ACCENT : RED, dp(1)));
        row.addView(dot, new LinearLayout.LayoutParams(dp(58), dp(34)));
        card.addView(row);
        return withMargins(card, 0, 0, 0, 14);
    }

    private void toggleConnection() {
        autoConnectEnabled = !autoConnectEnabled;
        reconnectHandler.removeCallbacks(reconnectRunnable);
        if (autoConnectEnabled) {
            connectionText = connected ? "Connected" : "Scanning for Cubie...";
            showTab(activeTab);
            ble.scanAndConnect();
        } else {
            connectionText = "Disconnected";
            connected = false;
            petCallActive = false;
            ble.disconnect();
            showTab(activeTab);
        }
    }

    private void togglePetCall() {
        if (!connected) {
            Toast.makeText(this, "Connect Cubie first", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean nextState = !petCallActive;
        boolean ok = ble.sendPetCall(nextState);
        if (!ok) {
            Toast.makeText(this, "Connect Cubie first", Toast.LENGTH_SHORT).show();
            return;
        }
        petCallActive = nextState;
        showTab(TAB_HOME);
    }

    private void scheduleReconnect() {
        reconnectHandler.removeCallbacks(reconnectRunnable);
        reconnectHandler.postDelayed(reconnectRunnable, 2500);
    }

    private void toggleSleep() {
        sleepEnabled = !sleepEnabled;
        saveAppSettings();
        pushDeviceSettings();
        showTab(TAB_SETTINGS);
    }

    private void toggleBuzzer() {
        buzzerEnabled = !buzzerEnabled;
        saveAppSettings();
        pushDeviceSettings();
        showTab(TAB_SETTINGS);
    }

    private void cycleBrightnessSetting() {
        brightnessIndex = (brightnessIndex + 1) % 3;
        saveAppSettings();
        pushDeviceSettings();
        showTab(TAB_SETTINGS);
    }

    private void cycleSleepTimer() {
        if (sleepTimerMinutes == 0) {
            sleepTimerMinutes = 1;
        } else if (sleepTimerMinutes == 1) {
            sleepTimerMinutes = 3;
        } else if (sleepTimerMinutes == 3) {
            sleepTimerMinutes = 5;
        } else if (sleepTimerMinutes == 5) {
            sleepTimerMinutes = 10;
        } else {
            sleepTimerMinutes = 0;
        }
        saveAppSettings();
        pushDeviceSettings();
        showTab(TAB_SETTINGS);
    }

    private void pushDeviceSettings() {
        boolean ok = ble.sendDeviceSettings(sleepEnabled, buzzerEnabled, brightnessIndex, sleepTimerMinutes);
        if (!ok) {
            Toast.makeText(this, "Connect Cubie first", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSettingsPage(int page) {
        if (settingsPage != page) {
            settingsBackStack.add(settingsPage);
        }
        settingsPage = page;
        showTab(TAB_SETTINGS);
    }

    private void togglePhoneEvent(int event) {
        if (event == 0) {
            notificationsEnabled = !notificationsEnabled;
        } else if (event == 1) {
            callsEnabled = !callsEnabled;
        } else {
            navigationEnabled = !navigationEnabled;
        }
        saveAppSettings();
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        showTab(TAB_SETTINGS);
    }

    private void toggleMusicLyrics() {
        musicLyricsEnabled = !musicLyricsEnabled;
        saveAppSettings();
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        showTab(TAB_SETTINGS);
    }

    private void handleMusicText(String packageName, String title, String text, long durationMs, long positionMs, boolean playing) {
        if (title == null || title.trim().isEmpty() || text == null || text.trim().isEmpty()) {
            return;
        }
        lastMusicTitle = title.trim();
        lastMusicLine = text.trim();
        String artist = cleanArtist(lastMusicLine);
        String lyricsKey = lastMusicTitle.toLowerCase() + "|" + artist.toLowerCase();
        playbackPositionMs = Math.max(0, positionMs);
        playbackClockAtMs = SystemClock.elapsedRealtime();
        playbackRunning = playing;
        previewFace = Math.min(29, FACE_PACKS.length - 1);
        if (musicLyricsEnabled && connected) {
            ble.sendMusicLyric(musicAppLabel(packageName), lastMusicTitle, lastMusicLine);
        }
        if (musicLyricsEnabled && !lyricsKey.equals(activeLyricsKey)) {
            activeLyricsKey = lyricsKey;
            activeLyrics = new ArrayList<>();
            lastSentLyricIndex = -1;
            lookupLyrics(lastMusicTitle, artist, durationMs, musicAppLabel(packageName), lyricsKey);
        }
        if (musicLyricsEnabled && !activeLyrics.isEmpty()) {
            lyricsHandler.removeCallbacks(lyricsTicker);
            lyricsHandler.post(lyricsTicker);
        }
        if (activeTab == TAB_SETTINGS && settingsPage == SETTINGS_MUSIC_LYRICS) {
            showTab(TAB_SETTINGS);
        }
    }

    private void handleNavigationEvent(Intent intent) {
        if (!navigationEnabled || !connected) {
            return;
        }
        boolean active = intent.getBooleanExtra(MusicLyricNotificationService.EXTRA_ACTIVE, false);
        if (!active) {
            ble.sendNavigation(false, "", "", "", "", "");
            return;
        }

        String title = cleanEventText(intent.getStringExtra(MusicLyricNotificationService.EXTRA_TITLE));
        String text = cleanEventText(intent.getStringExtra(MusicLyricNotificationService.EXTRA_TEXT));
        String bigText = cleanEventText(intent.getStringExtra(MusicLyricNotificationService.EXTRA_BIG_TEXT));
        String subText = cleanEventText(intent.getStringExtra(MusicLyricNotificationService.EXTRA_SUB_TEXT));
        String tripSummary = firstNonEmpty(subText, tripSummaryFrom(title), tripSummaryFrom(bigText));
        String distance = nextTurnDistance(title, text, bigText, subText);
        String directions = firstNonEmpty(text, bigText, title);
        if (!distance.isEmpty()) {
            directions = removeDistanceFromText(directions, distance);
        }
        if (directions.isEmpty()) {
            String combined = joinNonEmpty(title, text, bigText);
            directions = combined.isEmpty() ? "Navigation" : combined;
        }
        String remainingDistance = remainingTripDistance(tripSummary, distance);
        String eta = etaText(tripSummary);
        String etaRemainingTime = remainingTimeFromEta(eta);
        String remainingTime = remainingTripTime(tripSummary);
        Log.d(TAG, "Maps nav raw title=" + title
                + " text=" + text
                + " bigText=" + bigText
                + " subText=" + subText
                + " parsedNext=" + distance
                + " parsedRemaining=" + remainingDistance
                + " parsedEta=" + eta
                + " parsedEtaRemaining=" + etaRemainingTime
                + " parsedTime=" + remainingTime);
        ble.sendNavigation(true, distance, directions, remainingDistance, firstNonEmpty(etaRemainingTime, remainingTime), "");
    }

    private void handlePhoneNotificationEvent(Intent intent) {
        if (!notificationsEnabled || !connected) {
            return;
        }
        String packageName = cleanEventText(intent.getStringExtra(MusicLyricNotificationService.EXTRA_PACKAGE));
        if (!isNotificationAppAllowed(packageName)) {
            return;
        }
        String app = cleanEventText(intent.getStringExtra(MusicLyricNotificationService.EXTRA_APP));
        String title = cleanEventText(intent.getStringExtra(MusicLyricNotificationService.EXTRA_TITLE));
        String text = cleanEventText(intent.getStringExtra(MusicLyricNotificationService.EXTRA_TEXT));
        String bigText = cleanEventText(intent.getStringExtra(MusicLyricNotificationService.EXTRA_BIG_TEXT));
        String message = firstNonEmpty(bigText, text);
        if (title.isEmpty() && message.isEmpty()) {
            return;
        }
        ble.sendPhoneNotification(app.isEmpty() ? "App" : app, title, message);
    }

    private String firstDistance(String value) {
        String[] parts = value.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            String token = parts[i].replace(",", "").trim();
            String lower = token.toLowerCase();
            if (lower.matches("\\d+(\\.\\d+)?(m|km|mi|ft)")) {
                return token;
            }
            if (lower.matches("\\d+(\\.\\d+)?") && i + 1 < parts.length) {
                String unit = parts[i + 1].replace(",", "").toLowerCase();
                if (unit.equals("m") || unit.equals("km") || unit.equals("mi") || unit.equals("ft")) {
                    return token + unit;
                }
            }
        }
        return "";
    }

    private String nextTurnDistance(String title, String text, String bigText, String subText) {
        String distance = firstDistance(joinNonEmpty(text, bigText));
        if (!distance.isEmpty()) {
            return distance;
        }
        if (!looksLikeTripSummary(title, subText)) {
            distance = firstDistance(title);
            if (!distance.isEmpty()) {
                return distance;
            }
        }
        return "";
    }

    private String remainingTripDistance(String tripSummary, String nextTurnDistance) {
        for (String distance : allDistances(tripSummary)) {
            if (!distance.equalsIgnoreCase(nextTurnDistance)) {
                return distance;
            }
        }
        return "";
    }

    private String removeDistanceFromText(String text, String distance) {
        if (text == null || text.trim().isEmpty() || distance == null || distance.trim().isEmpty()) {
            return text == null ? "" : text.trim();
        }
        Matcher matcher = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\s*(?:km|mi|ft|m)\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group().replaceAll("\\s+", "");
            if (candidate.equalsIgnoreCase(distance.replaceAll("\\s+", ""))) {
                return (text.substring(0, matcher.start()) + text.substring(matcher.end())).replaceAll("\\s+", " ").trim();
            }
        }
        return text.trim();
    }

    private String etaText(String value) {
        Matcher matcher = Pattern.compile("\\b([01]?\\d|2[0-3]):[0-5]\\d\\s*(AM|PM|am|pm)?\\b").matcher(value);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return "";
    }

    private String remainingTimeFromEta(String eta) {
        if (eta == null || eta.trim().isEmpty()) {
            return "";
        }
        Matcher matcher = Pattern.compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\s*(AM|PM|am|pm)?\\b").matcher(eta.trim());
        if (!matcher.find()) {
            return "";
        }

        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));
        String period = matcher.group(3);
        Calendar now = Calendar.getInstance();
        Calendar best = null;

        if (period != null && !period.trim().isEmpty()) {
            Calendar candidate = candidateEtaTime(now, hour, minute, period);
            best = nextEtaCandidate(now, candidate);
        } else {
            List<Calendar> candidates = new ArrayList<>();
            candidates.add(candidateEtaTime(now, hour, minute, null));
            if (hour >= 1 && hour <= 11) {
                candidates.add(candidateEtaTime(now, hour + 12, minute, null));
            }
            for (Calendar candidate : candidates) {
                Calendar next = nextEtaCandidate(now, candidate);
                if (best == null || next.getTimeInMillis() < best.getTimeInMillis()) {
                    best = next;
                }
            }
        }

        if (best == null) {
            return "";
        }
        long remainingMinutes = Math.max(1, (best.getTimeInMillis() - now.getTimeInMillis() + 59999) / 60000);
        return compactDurationText(remainingMinutes);
    }

    private Calendar candidateEtaTime(Calendar now, int hour, int minute, String period) {
        Calendar candidate = (Calendar) now.clone();
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);
        if (period != null && !period.trim().isEmpty()) {
            candidate.set(Calendar.HOUR, hour % 12);
            candidate.set(Calendar.AM_PM, period.equalsIgnoreCase("PM") ? Calendar.PM : Calendar.AM);
        } else {
            candidate.set(Calendar.HOUR_OF_DAY, hour);
        }
        candidate.set(Calendar.MINUTE, minute);
        return candidate;
    }

    private Calendar nextEtaCandidate(Calendar now, Calendar candidate) {
        Calendar next = (Calendar) candidate.clone();
        while (!next.after(now)) {
            next.add(Calendar.HOUR_OF_DAY, 24);
        }
        return next;
    }

    private String compactDurationText(long totalMinutes) {
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0 && minutes > 0) {
            return hours + "h" + minutes + "m";
        }
        if (hours > 0) {
            return hours + "h";
        }
        return totalMinutes + "m";
    }

    private String remainingTripTime(String value) {
        Matcher matcher = Pattern.compile("\\b\\d+\\s*(h|hr|hrs|hour|hours)\\s*\\d*\\s*(m|min|mins|minute|minutes)?\\b|\\b\\d+\\s*(min|mins|minute|minutes)\\b", Pattern.CASE_INSENSITIVE).matcher(value);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return "";
    }

    private String tripSummaryFrom(String value) {
        return looksLikeTripSummary(value, "") ? value : "";
    }

    private boolean looksLikeTripSummary(String value, String subText) {
        String text = value == null ? "" : value.toLowerCase();
        if (text.isEmpty()) {
            return false;
        }
        boolean hasTripDistance = !firstDistance(text).isEmpty();
        boolean hasTime = !etaText(text).isEmpty() || !remainingTripTime(text).isEmpty();
        boolean appearsInSubText = subText != null && !subText.isEmpty() && subText.equalsIgnoreCase(value);
        return appearsInSubText || (hasTripDistance && hasTime);
    }

    private List<String> allDistances(String value) {
        List<String> distances = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return distances;
        }
        Matcher attached = Pattern.compile("\\b\\d+(?:\\.\\d+)?\\s*(?:km|mi|ft|m)\\b", Pattern.CASE_INSENSITIVE).matcher(value);
        while (attached.find()) {
            distances.add(attached.group().replaceAll("\\s+", "").trim());
        }
        return distances;
    }

    private String cleanEventText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String joinNonEmpty(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(value.trim());
        }
        return builder.toString();
    }

    private void lookupLyrics(String title, String artist, long durationMs, String appLabel, String expectedKey) {
        lyricsLookupRunning = true;
        lastMusicLine = "Looking up synced lyrics...";
        if (connected) {
            ble.sendMusicLyric("Lyrics", title, "Searching lyrics...");
        }
        new Thread(() -> {
            List<LrcLine> lines = fetchSyncedLyrics(title, artist, durationMs);
            runOnUiThread(() -> {
                if (!expectedKey.equals(activeLyricsKey)) {
                    lyricsLookupRunning = false;
                    return;
                }
                lyricsLookupRunning = false;
                activeLyrics = lines;
                lastSentLyricIndex = -1;
                if (activeLyrics.isEmpty()) {
                    lastMusicLine = "No synced lyrics found yet.";
                    if (connected) {
                        ble.sendMusicLyric("Lyrics", title, "No lyrics found");
                    }
                    return;
                }
                lastMusicLine = "Synced lyrics ready";
                lyricsHandler.removeCallbacks(lyricsTicker);
                lyricsHandler.post(lyricsTicker);
            });
        }).start();
    }

    private List<LrcLine> fetchSyncedLyrics(String title, String artist, long durationMs) {
        try {
            List<String> titleCandidates = lyricTitleCandidates(title);
            List<String> artistCandidates = lyricArtistCandidates(artist);
            for (String candidateTitle : titleCandidates) {
                for (String candidateArtist : artistCandidates) {
                    List<LrcLine> musixmatch = fetchMusixmatchLyrics(candidateTitle, candidateArtist);
                    if (!musixmatch.isEmpty()) {
                        return musixmatch;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    private List<LrcLine> fetchMusixmatchLyrics(String title, String artist) {
        if (title == null || title.trim().isEmpty() || musixmatchApiKey.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            String directUrl = MUSIXMATCH_API_URL + "matcher.lyrics.get?q_track=" + urlEncode(title)
                    + "&q_artist=" + urlEncode(artist)
                    + "&apikey=" + urlEncode(musixmatchApiKey);
            List<LrcLine> direct = musixmatchLyricsFromObject(readJsonObject(directUrl));
            if (!direct.isEmpty()) {
                return direct;
            }

            String searchUrl = MUSIXMATCH_API_URL + "track.search?q_track=" + urlEncode(title)
                    + "&q_artist=" + urlEncode(artist)
                    + "&f_has_lyrics=1&page_size=1&s_track_rating=desc"
                    + "&apikey=" + urlEncode(musixmatchApiKey);
            JSONObject search = readJsonObject(searchUrl);
            JSONObject message = search == null ? null : search.optJSONObject("message");
            JSONObject body = message == null ? null : message.optJSONObject("body");
            JSONArray trackList = body == null ? null : body.optJSONArray("track_list");
            if (trackList == null || trackList.length() == 0) {
                return new ArrayList<>();
            }
            JSONObject track = trackList.optJSONObject(0) == null
                    ? null
                    : trackList.optJSONObject(0).optJSONObject("track");
            int trackId = track == null ? 0 : track.optInt("track_id", 0);
            if (trackId <= 0) {
                return new ArrayList<>();
            }
            String lyricsUrl = MUSIXMATCH_API_URL + "track.lyrics.get?track_id=" + trackId
                    + "&apikey=" + urlEncode(musixmatchApiKey);
            return musixmatchLyricsFromObject(readJsonObject(lyricsUrl));
        } catch (Exception ignored) {
        }
        return new ArrayList<>();
    }

    private List<LrcLine> musixmatchLyricsFromObject(JSONObject object) {
        JSONObject message = object == null ? null : object.optJSONObject("message");
        JSONObject body = message == null ? null : message.optJSONObject("body");
        JSONObject lyrics = body == null ? null : body.optJSONObject("lyrics");
        String text = lyrics == null ? "" : lyrics.optString("lyrics_body", "");
        text = cleanMusixmatchLyrics(text);
        return parsePlainLyrics(text);
    }

    private String cleanMusixmatchLyrics(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?s)\\*\\*\\*\\*\\*\\*\\*.*$", "")
                .replaceAll("(?i)\\n?\\.\\.\\.\\s*$", "")
                .trim();
    }

    private JSONObject readJsonObject(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "Cubie Companion Android");
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        connection.disconnect();
        return new JSONObject(builder.toString());
    }

    private List<LrcLine> parseSyncedLyrics(String raw) {
        List<LrcLine> lines = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return lines;
        }
        String[] rows = raw.split("\\r?\\n");
        for (String row : rows) {
            Matcher matcher = LRC_LINE.matcher(row);
            if (!matcher.matches()) {
                continue;
            }
            String text = matcher.group(4) == null ? "" : matcher.group(4).trim();
            if (text.isEmpty()) {
                continue;
            }
            long minutes = Long.parseLong(matcher.group(1));
            long seconds = Long.parseLong(matcher.group(2));
            String fraction = matcher.group(3) == null ? "" : matcher.group(3);
            while (fraction.length() < 3) {
                fraction += "0";
            }
            if (fraction.length() > 3) {
                fraction = fraction.substring(0, 3);
            }
            long millis = fraction.isEmpty() ? 0 : Long.parseLong(fraction);
            lines.add(new LrcLine(((minutes * 60) + seconds) * 1000 + millis, text));
        }
        Collections.sort(lines, (a, b) -> Long.compare(a.timeMs, b.timeMs));
        return lines;
    }

    private List<LrcLine> parsePlainLyrics(String raw) {
        List<LrcLine> lines = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return lines;
        }
        String[] rows = raw.split("\\r?\\n");
        long timeMs = 0;
        for (String row : rows) {
            String text = row.trim();
            if (text.isEmpty()) {
                timeMs += 2000;
                continue;
            }
            lines.add(new LrcLine(timeMs, text));
            timeMs += Math.max(2500, Math.min(6000, text.length() * 90L));
        }
        return lines;
    }

    private void sendCurrentLyricLine() {
        if (!musicLyricsEnabled || !connected || activeLyrics.isEmpty()) {
            lyricsHandler.removeCallbacks(lyricsTicker);
            return;
        }
        long position = playbackPositionMs;
        if (playbackRunning) {
            position += Math.max(0, SystemClock.elapsedRealtime() - playbackClockAtMs);
        }
        int index = lyricIndexForPosition(position);
        if (index < 0 || index == lastSentLyricIndex) {
            return;
        }
        lastSentLyricIndex = index;
        lastMusicLine = activeLyrics.get(index).text;
        ble.sendMusicLyric("Lyrics", lastMusicTitle, lastMusicLine);
        if (activeTab == TAB_SETTINGS && settingsPage == SETTINGS_MUSIC_LYRICS) {
            showTab(TAB_SETTINGS);
        }
    }

    private int lyricIndexForPosition(long positionMs) {
        int index = -1;
        for (int i = 0; i < activeLyrics.size(); i++) {
            if (activeLyrics.get(i).timeMs <= positionMs + 350) {
                index = i;
            } else {
                break;
            }
        }
        return index;
    }

    private String cleanArtist(String value) {
        String artist = value == null ? "" : value.trim();
        String[] separators = {" - ", " • ", " | ", "\n"};
        for (String separator : separators) {
            int index = artist.indexOf(separator);
            if (index > 0) {
                artist = artist.substring(0, index).trim();
            }
        }
        return artist;
    }

    private List<String> lyricTitleCandidates(String title) {
        List<String> values = new ArrayList<>();
        addUnique(values, title);
        String clean = stripLyricNoise(title);
        addUnique(values, clean);
        addUnique(values, clean.replaceAll("(?i)\\s+-\\s+.*$", "").trim());
        addUnique(values, clean.replaceAll("(?i)\\s*\\(.*?\\)", "").trim());
        addUnique(values, clean.replaceAll("(?i)\\s*\\[.*?\\]", "").trim());
        addUnique(values, clean.replaceAll("(?i)\\s+(feat\\.|featuring|ft\\.).*$", "").trim());
        return values;
    }

    private List<String> lyricArtistCandidates(String artist) {
        List<String> values = new ArrayList<>();
        addUnique(values, artist);
        addUnique(values, stripLyricNoise(artist));
        addUnique(values, artist == null ? "" : artist.replaceAll("(?i)\\s*,\\s*.*$", "").trim());
        addUnique(values, artist == null ? "" : artist.replaceAll("(?i)\\s+&\\s+.*$", "").trim());
        addUnique(values, artist == null ? "" : artist.replaceAll("(?i)\\s+(feat\\.|featuring|ft\\.).*$", "").trim());
        addUnique(values, "");
        return values;
    }

    private String stripLyricNoise(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("(?i)\\s*\\((official|audio|video|lyrics?|lyric video|visualizer).*?\\)", "")
                .replaceAll("(?i)\\s*\\[(official|audio|video|lyrics?|lyric video|visualizer).*?\\]", "")
                .replaceAll("(?i)\\s*-\\s*(remaster(ed)?|radio edit|single version|album version|mono|stereo).*$", "")
                .replaceAll("(?i)\\s*\\((remaster(ed)?|radio edit|single version|album version|mono|stereo).*?\\)", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void addUnique(List<String> values, String value) {
        String clean = value == null ? "" : value.trim();
        for (String existing : values) {
            if (existing.equalsIgnoreCase(clean)) {
                return;
            }
        }
        values.add(clean);
    }

    private String urlEncode(String value) throws Exception {
        return URLEncoder.encode(value == null ? "" : value, "UTF-8");
    }

    private void startSpotifyLogin() {
        if (spotifyClientId.trim().isEmpty()) {
            Toast.makeText(this, "Paste Spotify Client ID first", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            spotifyCodeVerifier = randomUrlSafe(64);
            spotifyAuthState = randomUrlSafe(24);
            String challenge = base64UrlNoPadding(MessageDigest.getInstance("SHA-256")
                    .digest(spotifyCodeVerifier.getBytes("US-ASCII")));
            prefs.edit()
                    .putString("spotifyCodeVerifier", spotifyCodeVerifier)
                    .putString("spotifyAuthState", spotifyAuthState)
                    .apply();
            String url = SPOTIFY_AUTH_URL
                    + "?response_type=code"
                    + "&client_id=" + urlEncode(spotifyClientId)
                    + "&scope=" + urlEncode("user-read-currently-playing user-read-playback-state")
                    + "&redirect_uri=" + urlEncode(SPOTIFY_REDIRECT_URI)
                    + "&state=" + urlEncode(spotifyAuthState)
                    + "&code_challenge_method=S256"
                    + "&code_challenge=" + urlEncode(challenge);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Spotify login failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSpotifyRedirect(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data == null || !"cubie".equals(data.getScheme()) || !"spotify-callback".equals(data.getHost())) {
            return;
        }
        String code = data.getQueryParameter("code");
        String state = data.getQueryParameter("state");
        setIntent(new Intent(this, MainActivity.class));
        if (code == null || !spotifyAuthState.equals(state)) {
            spotifyStatus = "Spotify login canceled";
            showTab(activeTab);
            return;
        }
        exchangeSpotifyCode(code);
    }

    private void exchangeSpotifyCode(String code) {
        spotifyStatus = "Connecting Spotify...";
        showTab(activeTab);
        new Thread(() -> {
            try {
                String body = "grant_type=authorization_code"
                        + "&code=" + urlEncode(code)
                        + "&redirect_uri=" + urlEncode(SPOTIFY_REDIRECT_URI)
                        + "&client_id=" + urlEncode(spotifyClientId)
                        + "&code_verifier=" + urlEncode(spotifyCodeVerifier);
                JSONObject token = postSpotifyToken(body);
                if (token == null) {
                    throw new Exception("No token");
                }
                saveSpotifyTokens(token);
                runOnUiThread(() -> {
                    spotifyStatus = "Connected";
                    spotifyHandler.removeCallbacks(spotifyPoller);
                    spotifyHandler.post(spotifyPoller);
                    showTab(TAB_SETTINGS);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    spotifyStatus = "Spotify login failed";
                    showTab(TAB_SETTINGS);
                });
            }
        }).start();
    }

    private void pollSpotifyNowPlaying() {
        if (spotifyRefreshToken.isEmpty()) {
            return;
        }
        new Thread(() -> {
            try {
                ensureSpotifyAccessToken();
                HttpURLConnection connection = (HttpURLConnection) new URL(SPOTIFY_NOW_PLAYING_URL).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Authorization", "Bearer " + spotifyAccessToken);
                int status = connection.getResponseCode();
                if (status == 204) {
                    connection.disconnect();
                    return;
                }
                if (status == 401) {
                    spotifyTokenExpiresAt = 0;
                    ensureSpotifyAccessToken();
                    connection.disconnect();
                    return;
                }
                if (status < 200 || status >= 300) {
                    connection.disconnect();
                    return;
                }
                JSONObject object = readJsonFromConnection(connection);
                JSONObject item = object.optJSONObject("item");
                if (item == null) {
                    return;
                }
                String title = item.optString("name", "");
                JSONArray artists = item.optJSONArray("artists");
                String artist = "";
                if (artists != null && artists.length() > 0) {
                    JSONObject first = artists.optJSONObject(0);
                    artist = first == null ? "" : first.optString("name", "");
                }
                long durationMs = item.optLong("duration_ms", 0);
                long progressMs = object.optLong("progress_ms", 0);
                boolean playing = object.optBoolean("is_playing", false);
                String trackKey = title.toLowerCase() + "|" + artist.toLowerCase();
                if (!trackKey.equals(lastSpotifyTrackKey) || Math.abs(progressMs - playbackPositionMs) > 3000) {
                    lastSpotifyTrackKey = trackKey;
                    final String currentTitle = title;
                    final String currentArtist = artist;
                    final long currentDurationMs = durationMs;
                    final long currentProgressMs = progressMs;
                    final boolean currentPlaying = playing;
                    runOnUiThread(() -> handleMusicText("com.spotify.music", currentTitle, currentArtist, currentDurationMs, currentProgressMs, currentPlaying));
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void ensureSpotifyAccessToken() throws Exception {
        if (!spotifyAccessToken.isEmpty() && System.currentTimeMillis() < spotifyTokenExpiresAt - 60000) {
            return;
        }
        String body = "grant_type=refresh_token"
                + "&refresh_token=" + urlEncode(spotifyRefreshToken)
                + "&client_id=" + urlEncode(spotifyClientId);
        JSONObject token = postSpotifyToken(body);
        if (token == null) {
            throw new Exception("Refresh failed");
        }
        saveSpotifyTokens(token);
    }

    private JSONObject postSpotifyToken(String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(SPOTIFY_TOKEN_URL).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        OutputStream output = connection.getOutputStream();
        output.write(body.getBytes("UTF-8"));
        output.close();
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            return null;
        }
        return readJsonFromConnection(connection);
    }

    private JSONObject readJsonFromConnection(HttpURLConnection connection) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        connection.disconnect();
        return new JSONObject(builder.toString());
    }

    private void saveSpotifyTokens(JSONObject token) {
        spotifyAccessToken = token.optString("access_token", spotifyAccessToken);
        spotifyRefreshToken = token.optString("refresh_token", spotifyRefreshToken);
        long expiresIn = token.optLong("expires_in", 3600);
        spotifyTokenExpiresAt = System.currentTimeMillis() + expiresIn * 1000L;
        prefs.edit()
                .putString("spotifyAccessToken", spotifyAccessToken)
                .putString("spotifyRefreshToken", spotifyRefreshToken)
                .putLong("spotifyTokenExpiresAt", spotifyTokenExpiresAt)
                .apply();
    }

    private String randomUrlSafe(int bytes) {
        byte[] data = new byte[bytes];
        new SecureRandom().nextBytes(data);
        return base64UrlNoPadding(data);
    }

    private String base64UrlNoPadding(byte[] data) {
        return Base64.encodeToString(data, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private String musicAppLabel(String packageName) {
        if ("com.spotify.music".equals(packageName)) {
            return "Spotify";
        }
        if ("com.apple.android.music".equals(packageName)) {
            return "Apple Music";
        }
        if ("com.google.android.apps.youtube.music".equals(packageName)) {
            return "YT Music";
        }
        if ("com.amazon.mp3".equals(packageName)) {
            return "Amazon Music";
        }
        return "Music";
    }

    private static final class LrcLine {
        final long timeMs;
        final String text;

        LrcLine(long timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
    }

    private void registerMusicTextReceiver() {
        IntentFilter filter = new IntentFilter(MusicLyricNotificationService.ACTION_MUSIC_TEXT);
        filter.addAction(MusicLyricNotificationService.ACTION_NAVIGATION_EVENT);
        filter.addAction(MusicLyricNotificationService.ACTION_PHONE_NOTIFICATION);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(musicTextReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(musicTextReceiver, filter);
        }
    }

    private boolean isNotificationListenerEnabled() {
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (enabledListeners == null || enabledListeners.trim().isEmpty()) {
            return false;
        }
        ComponentName service = new ComponentName(this, MusicLyricNotificationService.class);
        String serviceName = service.flattenToString();
        String shortName = service.flattenToShortString();
        String[] listeners = enabledListeners.split(":");
        for (String listener : listeners) {
            if (serviceName.equalsIgnoreCase(listener) || shortName.equalsIgnoreCase(listener)) {
                return true;
            }
        }
        return false;
    }

    private void toggleSilentNotifications() {
        silentNotificationsEnabled = !silentNotificationsEnabled;
        saveAppSettings();
        showTab(TAB_SETTINGS);
    }

    private void toggleNotificationApp(String key) {
        Set<String> allowed = new HashSet<>(allowedNotificationApps());
        if (allowed.contains(key)) {
            allowed.remove(key);
        } else {
            allowed.add(key);
        }
        prefs.edit().putStringSet("allowedApps", allowed).apply();
        showTab(TAB_SETTINGS);
    }

    private void toggleBlockedCaller(String name) {
        Set<String> blocked = new HashSet<>(blockedCallers());
        if (blocked.contains(name)) {
            blocked.remove(name);
        } else {
            blocked.add(name);
        }
        prefs.edit().putStringSet("blockedCallers", blocked).apply();
        showTab(TAB_SETTINGS);
    }

    private boolean isNotificationAppAllowed(String key) {
        return allowedNotificationApps().contains(key);
    }

    private boolean isCallerBlocked(String name) {
        return blockedCallers().contains(name);
    }

    private int selectedNotificationAppCount() {
        return allowedNotificationApps().size();
    }

    private int blockedCallerCount() {
        return blockedCallers().size();
    }

    private Set<String> allowedNotificationApps() {
        return prefs.getStringSet("allowedApps", new HashSet<>());
    }

    private Set<String> blockedCallers() {
        return prefs.getStringSet("blockedCallers", new HashSet<>());
    }

    private void loadAppSettings() {
        petName = prefs.getString(PREF_PET_NAME, petName).trim();
        if (petName.isEmpty()) {
            petName = "Cubie";
        }
        sleepEnabled = prefs.getBoolean("sleep", sleepEnabled);
        buzzerEnabled = prefs.getBoolean("buzzer", buzzerEnabled);
        brightnessIndex = prefs.getInt("brightness", brightnessIndex);
        if (brightnessIndex < 0 || brightnessIndex > 2) {
            brightnessIndex = 1;
        }
        sleepTimerMinutes = prefs.getInt("sleepTimer", sleepTimerMinutes);
        if (sleepTimerMinutes < 0 || sleepTimerMinutes > 60) {
            sleepTimerMinutes = 0;
        }
        notificationsEnabled = prefs.getBoolean("notifications", notificationsEnabled);
        silentNotificationsEnabled = prefs.getBoolean("silentNotifications", silentNotificationsEnabled);
        callsEnabled = prefs.getBoolean("calls", callsEnabled);
        navigationEnabled = prefs.getBoolean("navigation", navigationEnabled);
        musicLyricsEnabled = prefs.getBoolean("musicLyrics", musicLyricsEnabled);
        spotifyClientId = prefs.getString("spotifyClientId", spotifyClientId);
        spotifyAccessToken = prefs.getString("spotifyAccessToken", spotifyAccessToken);
        spotifyRefreshToken = prefs.getString("spotifyRefreshToken", spotifyRefreshToken);
        spotifyTokenExpiresAt = prefs.getLong("spotifyTokenExpiresAt", spotifyTokenExpiresAt);
        spotifyCodeVerifier = prefs.getString("spotifyCodeVerifier", spotifyCodeVerifier);
        spotifyAuthState = prefs.getString("spotifyAuthState", spotifyAuthState);
        musixmatchApiKey = prefs.getString("musixmatchApiKey", musixmatchApiKey);
        if (!spotifyRefreshToken.isEmpty()) {
            spotifyStatus = "Connected";
        }
        pettingFace = prefs.getInt("pettingFace", pettingFace);
        if (pettingFace < 0 || pettingFace >= FACE_PACKS.length) {
            pettingFace = 0;
        }
        previewFace = pettingFace;
        for (int i = 0; i < selectedFaces.length; i++) {
            selectedFaces[i] = prefs.getBoolean("face_" + i, true);
        }
        selectedFaces[pettingFace] = false;
        ensureNormalFaceSelection();
    }

    private void saveAppSettings() {
        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean("sleep", sleepEnabled)
                .putBoolean("buzzer", buzzerEnabled)
                .putInt("brightness", brightnessIndex)
                .putInt("sleepTimer", sleepTimerMinutes)
                .putBoolean("notifications", notificationsEnabled)
                .putBoolean("silentNotifications", silentNotificationsEnabled)
                .putBoolean("calls", callsEnabled)
                .putBoolean("navigation", navigationEnabled)
                .putBoolean("musicLyrics", musicLyricsEnabled)
                .putInt("pettingFace", pettingFace);
        for (int i = 0; i < selectedFaces.length; i++) {
            editor.putBoolean("face_" + i, selectedFaces[i]);
        }
        editor.apply();
    }

    private void promptForPetNameIfNeeded() {
        if (prefs.contains(PREF_PET_NAME)) {
            return;
        }

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(petName);
        input.setSelectAllOnFocus(true);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint("Cubie");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Name your pet")
                .setMessage("What do you want to call your Cubie?")
                .setView(input)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
                return;
            }
            petName = name;
            prefs.edit().putString(PREF_PET_NAME, petName).apply();
            dialog.dismiss();
            showTab(activeTab);
        }));
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private String brightnessLabel() {
        if (brightnessIndex == 0) {
            return "Low";
        }
        if (brightnessIndex == 2) {
            return "High";
        }
        return "Medium";
    }

    private String sleepTimerLabel() {
        if (sleepTimerMinutes == 0) {
            return "Off";
        }
        return sleepTimerMinutes + " min";
    }

    private View facePreviewCard() {
        LinearLayout card = card(SURFACE, 18);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.addView(cubiePreview(previewFace), new LinearLayout.LayoutParams(-1, dp(142)));
        TextView title = label(FACE_PACKS[previewFace], 18, TEXT, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title);
        TextView note = label("Tap an emotion below to preview. Save exports the selected pack to Cubie.", 13, MUTED, false);
        note.setGravity(Gravity.CENTER);
        card.addView(note);
        return withMargins(card, 0, 0, 0, 14);
    }

    private View pettingPreviewCard() {
        LinearLayout card = card(SURFACE, 18);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.addView(cubiePreview(pettingFace), new LinearLayout.LayoutParams(-1, dp(142)));
        TextView title = label(FACE_PACKS[pettingFace], 18, TEXT, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title);
        TextView note = label("Single tap on Cubie will play this animation.", 13, MUTED, false);
        note.setGravity(Gravity.CENTER);
        card.addView(note);
        return withMargins(card, 0, 0, 0, 14);
    }

    private View faceModeSelector() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.HORIZONTAL);
        wrap.setPadding(dp(4), dp(4), dp(4), dp(4));
        wrap.setBackground(round(Color.TRANSPARENT, dp(2), BORDER, dp(1)));
        wrap.addView(faceModeButton("EMOTIONS", FACE_MODE_EMOTIONS), new LinearLayout.LayoutParams(0, dp(38), 1));
        wrap.addView(faceModeButton("PETTING", FACE_MODE_PETTING), new LinearLayout.LayoutParams(0, dp(38), 1));
        return withMargins(wrap, 0, 0, 0, 16);
    }

    private TextView faceModeButton(String text, int mode) {
        boolean selected = faceMode == mode;
        TextView button = dotLabel(text);
        button.setTextColor(selected ? BG : MUTED);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setBackground(round(selected ? TEXT : Color.TRANSPARENT, dp(2), 0, 0));
        button.setOnClickListener(v -> {
            faceMode = mode;
            if (mode == FACE_MODE_PETTING) {
                previewFace = pettingFace;
            }
            showTab(TAB_FACES);
        });
        return button;
    }

    private View faceGrid() {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setUseDefaultMargins(false);
        for (int i = 0; i < FACE_PACKS.length; i++) {
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(54);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(0, 0, dp(8), dp(8));
            grid.addView(faceChoice(i), params);
        }
        return withMargins(grid, 0, 0, 0, 8);
    }

    private View pettingGrid() {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        grid.setUseDefaultMargins(false);
        for (int i = 0; i < FACE_PACKS.length; i++) {
            final int index = i;
            View item = pettingChoice(index);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = dp(54);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(0, 0, dp(8), dp(8));
            grid.addView(item, params);
        }
        return withMargins(grid, 0, 0, 0, 8);
    }

    private View faceSelectionGrid() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);

        GridLayout headers = new GridLayout(this);
        headers.setColumnCount(2);
        headers.setUseDefaultMargins(false);
        addFaceSelectionCell(headers, faceColumnHeader("Emotions"), dp(30), dp(8));
        addFaceSelectionCell(headers, faceColumnHeader("Petting"), dp(30), dp(8));
        wrap.addView(headers);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        grid.setUseDefaultMargins(false);
        for (int i = 0; i < FACE_PACKS.length; i++) {
            addFaceSelectionCell(grid, faceChoice(i), dp(64), dp(10));
            addFaceSelectionCell(grid, pettingChoice(i), dp(64), dp(10));
        }
        wrap.addView(grid);
        return withMargins(wrap, 0, 0, 0, 8);
    }

    private TextView faceColumnHeader(String text) {
        TextView header = label(text, 14, MUTED, true);
        header.setGravity(Gravity.CENTER);
        return header;
    }

    private void addFaceSelectionCell(GridLayout grid, View item, int height, int bottomMargin) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = height;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(0, 0, dp(10), bottomMargin);
        grid.addView(item, params);
    }

    private View faceChoice(int index) {
        if (index == pettingFace) {
            LinearLayout blocked = faceChip(FACE_PACKS[index], false, previewFace == index, true);
            blocked.setOnClickListener(v -> {
                previewFace = index;
                Toast.makeText(this, "This one is reserved for petting", Toast.LENGTH_SHORT).show();
                showTab(TAB_FACES);
            });
            return blocked;
        }

        boolean selected = selectedFaces[index];
        boolean preview = previewFace == index;
        LinearLayout item = faceChip(FACE_PACKS[index], selected, preview, false);
        item.setOnClickListener(v -> {
            previewFace = index;
            selectedFaces[index] = !selectedFaces[index];
            if (!hasAnyFaceSelected()) {
                selectedFaces[index] = true;
                Toast.makeText(this, "Keep at least one face selected", Toast.LENGTH_SHORT).show();
            }
            showTab(TAB_FACES);
        });
        return item;
    }

    private LinearLayout faceChip(String name, boolean enabled, boolean preview, boolean reserved) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(6), dp(5), dp(6), dp(5));
        int stroke = preview ? RED : (enabled ? TEXT : BORDER);
        chip.setBackground(round(Color.TRANSPARENT, dp(2), stroke, preview ? dp(2) : dp(1)));

        View dot = new View(this);
        int dotColor = reserved ? Color.rgb(42, 42, 42) : (enabled ? TEXT : Color.rgb(42, 42, 42));
        dot.setBackground(round(preview ? RED : dotColor, dp(5), 0, 0));
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(preview ? 18 : 8), dp(8));
        dotParams.setMargins(0, 0, 0, dp(6));
        chip.addView(dot, dotParams);

        TextView title = label(name.toUpperCase(), 10, reserved ? MUTED : (enabled ? TEXT : MUTED), enabled && !reserved);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        title.setIncludeFontPadding(false);
        chip.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView state = dotLabel(reserved ? "PET" : (enabled ? "ON" : "OFF"));
        state.setTextSize(8);
        state.setTextColor(preview ? RED : MUTED);
        state.setGravity(Gravity.CENTER);
        state.setIncludeFontPadding(false);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(-1, -2);
        stateParams.setMargins(0, dp(4), 0, 0);
        chip.addView(state, stateParams);
        return chip;
    }

    private LinearLayout faceRow(String name, String state, boolean enabled, boolean preview, boolean reserved) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), 0, dp(10), 0);
        int stroke = preview ? RED : (enabled ? TEXT : BORDER);
        row.setBackground(round(Color.TRANSPARENT, dp(2), stroke, preview ? dp(2) : dp(1)));

        View previewLine = new View(this);
        previewLine.setBackground(round(preview ? RED : Color.rgb(38, 38, 38), dp(1), 0, 0));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(dp(3), dp(28));
        lineParams.setMargins(0, 0, dp(12), 0);
        row.addView(previewLine, lineParams);

        TextView title = label(name, 15, reserved ? MUTED : (enabled ? TEXT : MUTED), enabled && !reserved);
        title.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(title, new LinearLayout.LayoutParams(0, -1, 1));

        TextView badge = dotLabel(state);
        badge.setTextSize(11);
        badge.setTextColor(reserved ? MUTED : (enabled ? BG : MUTED));
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(enabled && !reserved ? TEXT : Color.TRANSPARENT, dp(2), reserved ? BORDER : (enabled ? 0 : BORDER), enabled ? 0 : dp(1)));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(74), dp(30));
        row.addView(badge, badgeParams);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(58));
        rowParams.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowParams);
        return row;
    }

    private View pettingChoice(int index) {
        boolean selected = pettingFace == index;
        LinearLayout item = faceChip(FACE_PACKS[index], selected, selected, false);
        item.setOnClickListener(v -> {
            pettingFace = index;
            previewFace = index;
            selectedFaces[index] = false;
            ensureNormalFaceSelection();
            showTab(TAB_FACES);
        });
        return item;
    }

    private View saveFacesButton() {
        TextView save = label("Save to Cubie", 14, TEXT, false);
        save.setGravity(Gravity.CENTER);
        save.setBackground(round(Color.TRANSPARENT, dp(2), ACCENT, dp(1)));
        save.setOnClickListener(v -> {
            selectedFaces[pettingFace] = false;
            ensureNormalFaceSelection();
            saveAppSettings();
            boolean ok = ble.sendFaceSettingsAndRestart(selectedFaces, pettingFace);
            Toast.makeText(this, ok ? "Face pack exported. Cubie will reboot." : "Connect Cubie first", Toast.LENGTH_SHORT).show();
        });
        return withMargins(save, 0, 2, 0, 24);
    }

    private View savePettingButton() {
        TextView save = label("Save petting animation", 14, TEXT, false);
        save.setGravity(Gravity.CENTER);
        save.setBackground(round(Color.TRANSPARENT, dp(2), ACCENT, dp(1)));
        save.setOnClickListener(v -> {
            selectedFaces[pettingFace] = false;
            ensureNormalFaceSelection();
            saveAppSettings();
            boolean facesOk = ble.sendFaceSettingsAndRestart(selectedFaces, pettingFace);
            Toast.makeText(this, facesOk ? "Petting animation exported. Cubie will reboot." : "Connect Cubie first", Toast.LENGTH_SHORT).show();
        });
        return withMargins(save, 0, 2, 0, 24);
    }

    private boolean hasAnyFaceSelected() {
        for (boolean selected : selectedFaces) {
            if (selected) {
                return true;
            }
        }
        return false;
    }

    private int countSelectedFaces() {
        int count = 0;
        for (boolean selected : selectedFaces) {
            if (selected) {
                count++;
            }
        }
        return count;
    }

    private void ensureNormalFaceSelection() {
        if (hasAnyFaceSelected()) {
            return;
        }
        for (int i = 0; i < selectedFaces.length; i++) {
            if (i != pettingFace) {
                selectedFaces[i] = true;
                return;
            }
        }
    }

    private View toggleRow(String title, String value, boolean enabled, View.OnClickListener listener) {
        LinearLayout card = card(SURFACE, 14);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(label(title, 14, TEXT, false));
        copy.addView(label(value, 12, enabled ? ACCENT : MUTED, false));
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));

        TextView state = label(enabled ? "ON" : "OFF", 12, enabled ? BG : MUTED, true);
        state.setGravity(Gravity.CENTER);
        state.setTextColor(enabled ? RED : MUTED);
        state.setBackground(round(Color.TRANSPARENT, dp(2), enabled ? RED : BORDER, dp(1)));
        row.addView(state, new LinearLayout.LayoutParams(dp(52), dp(32)));

        card.addView(row);
        card.setOnClickListener(listener);
        return withMargins(card, 0, 0, 0, 8);
    }

    private View searchCard() {
        LinearLayout card = card(SURFACE, 14);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(dotLabel("SEARCH"));

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(callerSearchQuery);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint("Type a contact name");
        input.setTextSize(15);
        input.setBackground(round(SURFACE_2, dp(4), BORDER, dp(1)));
        input.setPadding(dp(12), 0, dp(12), 0);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                callerSearchQuery = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(46));
        inputParams.setMargins(0, dp(10), 0, 0);
        card.addView(input, inputParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView search = pill("Search");
        search.setOnClickListener(v -> showTab(TAB_SETTINGS));
        buttons.addView(search);
        card.addView(buttons);
        return withMargins(card, 0, 0, 0, 12);
    }

    private View appSearchCard() {
        LinearLayout card = card(SURFACE, 14);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(dotLabel("SEARCH"));

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(appSearchQuery);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint("Type an app name");
        input.setTextSize(15);
        input.setBackground(round(SURFACE_2, dp(4), BORDER, dp(1)));
        input.setPadding(dp(12), 0, dp(12), 0);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                appSearchQuery = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(46));
        inputParams.setMargins(0, dp(10), 0, 0);
        card.addView(input, inputParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView search = pill("Search");
        search.setOnClickListener(v -> showTab(TAB_SETTINGS));
        buttons.addView(search);
        card.addView(buttons);
        return withMargins(card, 0, 0, 0, 12);
    }

    private View spotifyClientCard() {
        LinearLayout card = card(SURFACE, 14);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(dotLabel("SPOTIFY CLIENT ID"));

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(spotifyClientId);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint("Paste Client ID");
        input.setTextSize(15);
        input.setBackground(round(SURFACE_2, dp(4), BORDER, dp(1)));
        input.setPadding(dp(12), 0, dp(12), 0);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                spotifyClientId = s.toString().trim();
                prefs.edit().putString("spotifyClientId", spotifyClientId).apply();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(46));
        inputParams.setMargins(0, dp(10), 0, 0);
        card.addView(input, inputParams);
        return withMargins(card, 0, 0, 0, 12);
    }

    private View musixmatchApiCard() {
        LinearLayout card = card(SURFACE, 14);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(dotLabel("MUSIXMATCH API KEY"));
        card.addView(label(
                musixmatchApiKey.trim().isEmpty()
                        ? "Required for API lyric lookup."
                        : "Saved. Musixmatch will be used for lyrics.",
                13,
                MUTED,
                false));

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(musixmatchApiKey);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setHint("Paste API key");
        input.setTextSize(15);
        input.setBackground(round(SURFACE_2, dp(4), BORDER, dp(1)));
        input.setPadding(dp(12), 0, dp(12), 0);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                musixmatchApiKey = s.toString().trim();
                prefs.edit().putString("musixmatchApiKey", musixmatchApiKey).apply();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(46));
        inputParams.setMargins(0, dp(10), 0, 0);
        card.addView(input, inputParams);
        return withMargins(card, 0, 0, 0, 12);
    }

    private boolean appMatchesSearch(String appName) {
        String query = appSearchQuery.trim();
        return query.isEmpty() || appName.toLowerCase().contains(query.toLowerCase());
    }

    private boolean callerMatchesSearch(String caller) {
        String query = callerSearchQuery.trim();
        return query.isEmpty() || caller.toLowerCase().contains(query.toLowerCase());
    }

    private List<AppChoice> loadInstalledApps() {
        return visibleAppsCache == null ? new ArrayList<>() : visibleAppsCache;
    }

    private List<AppChoice> scanVisibleApps() {
        List<AppChoice> apps = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();
        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> installed = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo info : installed) {
            String key = info.packageName;
            if (key == null || key.trim().isEmpty()) {
                continue;
            }

            Intent launchIntent = packageManager.getLaunchIntentForPackage(key);
            if (launchIntent == null) {
                continue;
            }

            boolean systemApp = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean updatedSystemApp = (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
            if (systemApp && !updatedSystemApp) {
                continue;
            }

            if (!seenPackages.add(key)) {
                continue;
            }
            String label = info.loadLabel(packageManager).toString();
            apps.add(new AppChoice(label, key));
        }
        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
        return apps;
    }

    private List<String> loadContactNames() {
        return contactNamesCache == null ? new ArrayList<>() : contactNamesCache;
    }

    private List<String> scanContactNames() {
        List<String> names = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return names;
        }

        Cursor cursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME_PRIMARY},
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC");
        if (cursor == null) {
            return names;
        }

        try {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty() && !names.contains(name)) {
                    names.add(name);
                }
            }
        } finally {
            cursor.close();
        }
        return names;
    }

    private void loadSavedListCaches() {
        visibleAppsCache = parseAppCache(prefs.getString(PREF_APP_LIST, "[]"));
        contactNamesCache = parseStringCache(prefs.getString(PREF_CONTACT_LIST, "[]"));
    }

    private void refreshAppCache(boolean force) {
        if (appRefreshRunning) {
            return;
        }
        if (!force) {
            return;
        }

        appRefreshRunning = true;
        new Thread(() -> {
            List<AppChoice> apps = scanVisibleApps();
            prefs.edit()
                    .putString(PREF_APP_LIST, serializeAppCache(apps))
                    .putLong(PREF_APP_REFRESH_AT, System.currentTimeMillis())
                    .apply();
            runOnUiThread(() -> {
                visibleAppsCache = apps;
                appRefreshRunning = false;
                if (activeTab == TAB_SETTINGS && (settingsPage == SETTINGS_NOTIFICATION_APPS || settingsPage == SETTINGS_PHONE_NOTIFICATIONS)) {
                    showTab(TAB_SETTINGS);
                }
            });
        }).start();
    }

    private void refreshContactCache(boolean force) {
        if (contactRefreshRunning) {
            return;
        }
        if (!force) {
            return;
        }

        contactRefreshRunning = true;
        new Thread(() -> {
            List<String> contacts = scanContactNames();
            prefs.edit()
                    .putString(PREF_CONTACT_LIST, serializeStringCache(contacts))
                    .putLong(PREF_CONTACT_REFRESH_AT, System.currentTimeMillis())
                    .apply();
            runOnUiThread(() -> {
                contactNamesCache = contacts;
                contactRefreshRunning = false;
                if (activeTab == TAB_SETTINGS && (settingsPage == SETTINGS_CALL_PEOPLE || settingsPage == SETTINGS_PHONE_CALLS)) {
                    showTab(TAB_SETTINGS);
                }
            });
        }).start();
    }

    private String serializeAppCache(List<AppChoice> apps) {
        JSONArray array = new JSONArray();
        for (AppChoice app : apps) {
            JSONObject object = new JSONObject();
            try {
                object.put("label", app.label);
                object.put("key", app.key);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }
        return array.toString();
    }

    private List<AppChoice> parseAppCache(String raw) {
        List<AppChoice> apps = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String label = object.optString("label", "");
                String key = object.optString("key", "");
                if (!label.isEmpty() && !key.isEmpty()) {
                    apps.add(new AppChoice(label, key));
                }
            }
        } catch (JSONException ignored) {
        }
        return apps;
    }

    private String serializeStringCache(List<String> values) {
        JSONArray array = new JSONArray();
        for (String value : values) {
            array.put(value);
        }
        return array.toString();
    }

    private List<String> parseStringCache(String raw) {
        List<String> values = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "");
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        } catch (JSONException ignored) {
        }
        return values;
    }

    private View actionCard(String title, String value, String note, String action, View.OnClickListener listener) {
        LinearLayout card = card(SURFACE, 14);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setClickable(true);
        card.setOnClickListener(listener);
        card.addView(label(title, 15, TEXT, false));
        card.addView(label(value, 13, RED, false));
        card.addView(label(note, 12, MUTED, false));
        TextView button = pill(action);
        button.setOnClickListener(listener);
        card.addView(button);
        return withMargins(card, 0, 0, 0, 10);
    }

    private static final class AppChoice {
        final String label;
        final String key;

        AppChoice(String label, String key) {
            this.label = label;
            this.key = key;
        }
    }

    private View wideCard(String title, String value, String note) {
        LinearLayout card = card(SURFACE, 14);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(label(title, 15, TEXT, false));
        card.addView(label(value, 13, TEXT, false));
        card.addView(label(note, 12, MUTED, false));
        return withMargins(card, 0, 0, 0, 8);
    }

    private void renderBottomNav() {
        navRoot.removeAllViews();
        navRoot.setOrientation(LinearLayout.HORIZONTAL);
        navRoot.setGravity(Gravity.CENTER);
        navRoot.setPadding(dp(10), dp(8), dp(10), dp(10));
        navRoot.setBackground(round(Color.rgb(4, 4, 4), 0, BORDER, dp(1)));
        navRoot.addView(navItem("Home", TAB_HOME), new LinearLayout.LayoutParams(0, -1, 1));
        navRoot.addView(navItem("Device", TAB_DEVICE), new LinearLayout.LayoutParams(0, -1, 1));
        navRoot.addView(navItem("Faces", TAB_FACES), new LinearLayout.LayoutParams(0, -1, 1));
        navRoot.addView(navItem("Settings", TAB_SETTINGS), new LinearLayout.LayoutParams(0, -1, 1));
    }

    private View navItem(String text, int tab) {
        boolean active = activeTab == tab;
        TextView item = label(text, 12, active ? RED : MUTED, false);
        item.setGravity(Gravity.CENTER);
        if (active) {
            item.setBackground(round(Color.TRANSPARENT, dp(2), RED, dp(1)));
        }
        item.setOnClickListener(v -> {
            if (tab == TAB_SETTINGS) {
                settingsPage = SETTINGS_ROOT;
                settingsBackStack.clear();
            }
            showTab(tab);
        });
        return item;
    }

    private View sectionTitle(String text) {
        TextView title = dotLabel(text.toUpperCase());
        title.setPadding(0, dp(10), 0, dp(8));
        return title;
    }

    private TextView dotLabel(String text) {
        TextView view = label(spacedText(text), 11, MUTED, true);
        view.setLetterSpacing(0.08f);
        view.setTypeface(fontDot, Typeface.NORMAL);
        return view;
    }

    private String spacedText(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(value.charAt(i));
        }
        return builder.toString();
    }

    private View glyphRail() {
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.HORIZONTAL);
        rail.setGravity(Gravity.CENTER_VERTICAL);
        rail.setPadding(0, dp(10), 0, 0);
        int[] colors = {
                RED, ACCENT, Color.rgb(74, 74, 68), Color.rgb(74, 74, 68),
                ACCENT, Color.rgb(74, 74, 68), RED, Color.rgb(74, 74, 68)
        };
        for (int i = 0; i < colors.length; i++) {
            View dot = new View(this);
            dot.setBackground(round(colors[i], dp(2), 0, 0));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(i == 1 || i == 4 ? 18 : 6), dp(4));
            params.setMargins(0, 0, dp(6), 0);
            rail.addView(dot, params);
        }
        return rail;
    }

    private LinearLayout card(int color, int radiusDp) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int fill = color == SURFACE ? Color.TRANSPARENT : color;
        card.setBackground(round(fill, dp(Math.min(radiusDp, 3)), BORDER, dp(1)));
        return card;
    }

    private TextView pill(String text) {
        TextView pill = label(text, 12, TEXT, false);
        pill.setGravity(Gravity.CENTER);
        pill.setBackground(round(Color.TRANSPARENT, dp(2), BORDER, dp(1)));
        pill.setPadding(dp(12), dp(7), dp(12), dp(7));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(0, dp(12), 0, 0);
        pill.setLayoutParams(params);
        return pill;
    }

    private TextView iconButton(String text) {
        TextView button = label(text, 18, TEXT, false);
        button.setGravity(Gravity.CENTER);
        button.setBackground(round(Color.TRANSPARENT, dp(2), BORDER, dp(1)));
        return button;
    }

    private TextView label(String value, int sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        view.setTypeface(bold ? fontMedium : fontBody, Typeface.NORMAL);
        return view;
    }

    private View withMargins(View view, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        view.setLayoutParams(params);
        return view;
    }

    private GradientDrawable round(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }
        return drawable;
    }

    private void requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, 10);
        } else if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 10);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
