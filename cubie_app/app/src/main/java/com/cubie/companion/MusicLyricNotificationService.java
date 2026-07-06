package com.cubie.companion;

import android.app.Notification;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.content.pm.PackageManager;

public final class MusicLyricNotificationService extends NotificationListenerService {
    static final String ACTION_MUSIC_TEXT = "com.cubie.companion.MUSIC_TEXT";
    static final String ACTION_NAVIGATION_EVENT = "com.cubie.companion.NAVIGATION_EVENT";
    static final String ACTION_PHONE_NOTIFICATION = "com.cubie.companion.PHONE_NOTIFICATION";
    static final String EXTRA_PACKAGE = "package";
    static final String EXTRA_APP = "app";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_TEXT = "text";
    static final String EXTRA_BIG_TEXT = "bigText";
    static final String EXTRA_SUB_TEXT = "subText";
    static final String EXTRA_ACTIVE = "active";
    static final String EXTRA_DURATION_MS = "durationMs";
    static final String EXTRA_POSITION_MS = "positionMs";
    static final String EXTRA_PLAYING = "playing";
    private static final String GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps";
    private static final String[] KNOWN_MUSIC_PACKAGES = {
            "com.spotify.music",
            "com.apple.android.music",
            "com.google.android.apps.youtube.music",
            "com.amazon.mp3"
    };

    private String lastPackage = "";
    private String lastTitle = "";
    private String lastText = "";
    private long lastSentAt = 0;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification == null || notification.extras == null) {
            return;
        }
        Bundle extras = notification.extras;
        String packageName = sbn.getPackageName();
        String titleText = firstText(
                extras.getCharSequence(Notification.EXTRA_TITLE),
                extras.getCharSequence(Notification.EXTRA_TITLE_BIG));
        String bodyText = firstText(
                extras.getCharSequence(Notification.EXTRA_TEXT),
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        String bigText = firstText(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        String subText = firstText(extras.getCharSequence(Notification.EXTRA_SUB_TEXT));

        if (GOOGLE_MAPS_PACKAGE.equals(packageName)) {
            sendNavigationBroadcast(true, packageName, titleText, bodyText, bigText, subText);
            return;
        }

        MediaSnapshot media = readMediaSnapshot(notification);
        String title = firstText(
                media.title,
                extras.getCharSequence(Notification.EXTRA_TITLE),
                extras.getCharSequence(Notification.EXTRA_TITLE_BIG));
        String text = firstText(
                media.artist,
                extras.getCharSequence(Notification.EXTRA_TEXT),
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
                extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
        if (isBlank(title) || isBlank(text) || !looksLikeMusic(notification, packageName, title, text)) {
            sendNotificationBroadcast(packageName, titleText, bodyText, bigText, subText);
            return;
        }

        long now = System.currentTimeMillis();
        if (sbn.getPackageName().equals(lastPackage)
                && title.equals(lastTitle)
                && text.equals(lastText)
                && now - lastSentAt < 1500) {
            return;
        }
        lastPackage = sbn.getPackageName();
        lastTitle = title;
        lastText = text;
        lastSentAt = now;

        Intent intent = new Intent(ACTION_MUSIC_TEXT);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_PACKAGE, sbn.getPackageName());
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_TEXT, text);
        intent.putExtra(EXTRA_DURATION_MS, media.durationMs);
        intent.putExtra(EXTRA_POSITION_MS, media.positionMs);
        intent.putExtra(EXTRA_PLAYING, media.playing);
        sendBroadcast(intent);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (GOOGLE_MAPS_PACKAGE.equals(sbn.getPackageName())) {
            sendNavigationBroadcast(false, sbn.getPackageName(), "", "", "", "");
        }
    }

    private void sendNavigationBroadcast(boolean active, String packageName, String title, String text, String bigText, String subText) {
        Intent intent = new Intent(ACTION_NAVIGATION_EVENT);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_ACTIVE, active);
        intent.putExtra(EXTRA_PACKAGE, packageName);
        intent.putExtra(EXTRA_APP, appLabel(packageName));
        intent.putExtra(EXTRA_TITLE, safe(title));
        intent.putExtra(EXTRA_TEXT, safe(text));
        intent.putExtra(EXTRA_BIG_TEXT, safe(bigText));
        intent.putExtra(EXTRA_SUB_TEXT, safe(subText));
        sendBroadcast(intent);
    }

    private void sendNotificationBroadcast(String packageName, String title, String text, String bigText, String subText) {
        if (isBlank(title) && isBlank(text) && isBlank(bigText)) {
            return;
        }
        Intent intent = new Intent(ACTION_PHONE_NOTIFICATION);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_PACKAGE, packageName);
        intent.putExtra(EXTRA_APP, appLabel(packageName));
        intent.putExtra(EXTRA_TITLE, safe(title));
        intent.putExtra(EXTRA_TEXT, safe(text));
        intent.putExtra(EXTRA_BIG_TEXT, safe(bigText));
        intent.putExtra(EXTRA_SUB_TEXT, safe(subText));
        sendBroadcast(intent);
    }

    private MediaSnapshot readMediaSnapshot(Notification notification) {
        MediaSnapshot snapshot = new MediaSnapshot();
        Parcelable rawToken = notification.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
        if (!(rawToken instanceof MediaSession.Token)) {
            return snapshot;
        }

        MediaController controller = new MediaController(this, (MediaSession.Token) rawToken);
        MediaMetadata metadata = controller.getMetadata();
        if (metadata != null) {
            snapshot.title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            snapshot.artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            if (isBlank(snapshot.artist)) {
                snapshot.artist = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
            }
            snapshot.durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        }

        PlaybackState state = controller.getPlaybackState();
        if (state != null) {
            snapshot.playing = state.getState() == PlaybackState.STATE_PLAYING;
            long position = state.getPosition();
            if (snapshot.playing && position >= 0) {
                long elapsed = SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime();
                position += Math.max(0, elapsed);
            }
            snapshot.positionMs = Math.max(0, position);
        }
        return snapshot;
    }

    private boolean looksLikeMusic(Notification notification, String packageName, String title, String text) {
        if (notification.category != null && Notification.CATEGORY_TRANSPORT.equals(notification.category)) {
            return true;
        }
        if (isKnownMusicPackage(packageName)) {
            return true;
        }
        String haystack = (packageName + " " + title + " " + text).toLowerCase();
        return haystack.contains("music")
                || haystack.contains("spotify")
                || haystack.contains("youtube")
                || haystack.contains("amazon")
                || haystack.contains("apple")
                || haystack.contains("lyrics")
                || haystack.contains("musixmatch")
                || haystack.contains("wynk")
                || haystack.contains("jiosaavn")
                || haystack.contains("gaana");
    }

    private boolean isKnownMusicPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        for (String knownPackage : KNOWN_MUSIC_PACKAGES) {
            if (knownPackage.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private String firstText(CharSequence... values) {
        for (CharSequence value : values) {
            if (value != null && !isBlank(value.toString())) {
                return value.toString().trim();
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String appLabel(String packageName) {
        try {
            PackageManager packageManager = getPackageManager();
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString();
        } catch (Exception ignored) {
            return packageName == null ? "App" : packageName;
        }
    }

    private static final class MediaSnapshot {
        String title = "";
        String artist = "";
        long durationMs = 0;
        long positionMs = 0;
        boolean playing = true;
    }
}
