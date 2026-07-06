package com.cubie.companion;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

final class BleCubieClient {
    interface Listener {
        void onState(String text, boolean connected);
        void onError(String text);
        void onLatency(long latencyMs);
        void onPetCallState(boolean active);
    }

    private static final UUID CHRONOS_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CHRONOS_RX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CHRONOS_TX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final Context context;
    private final Listener listener;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic rx;
    private BluetoothGattCharacteristic tx;
    private final Queue<byte[]> writeQueue = new ArrayDeque<>();
    private boolean writeInProgress = false;
    private int pingSequence = 0;
    private int pendingPingSequence = -1;
    private long pingStartedAtMs = 0;

    BleCubieClient(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    void scanAndConnect() {
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            listener.onError("Turn on Bluetooth first");
            return;
        }
        if (!hasScanPermission() || !hasConnectPermission()) {
            listener.onError("Bluetooth permission needed");
            return;
        }

        disconnect();
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            listener.onError("BLE scanner unavailable");
            return;
        }

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(CHRONOS_SERVICE))
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        listener.onState("Scanning for Cubie...", false);
        scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
    }

    void disconnect() {
        if (scanner != null && hasScanPermission()) {
            scanner.stopScan(scanCallback);
        }
        scanner = null;
        if (gatt != null && hasConnectPermission()) {
            gatt.disconnect();
            gatt.close();
        }
        gatt = null;
        rx = null;
        tx = null;
        writeQueue.clear();
        writeInProgress = false;
        pendingPingSequence = -1;
        pingStartedAtMs = 0;
    }

    boolean sendFaceMask(boolean[] selectedFaces) {
        long mask = 0;
        for (int i = 0; i < selectedFaces.length && i < 64; i++) {
            if (selectedFaces[i]) {
                mask |= 1L << i;
            }
        }
        byte[] packet = new byte[10];
        packet[0] = (byte) 0xCB;
        packet[1] = 0x04;
        writeLongMask(packet, 2, mask);
        return write(packet);
    }

    boolean sendPettingFace(int index) {
        return write(new byte[]{
                (byte) 0xCB,
                0x05,
                (byte) index
        });
    }

    boolean sendRestart() {
        return write(new byte[]{
                (byte) 0xCB,
                0x06
        });
    }

    boolean sendFaceSettingsAndRestart(boolean[] selectedFaces, int pettingIndex) {
        long mask = 0;
        for (int i = 0; i < selectedFaces.length && i < 64; i++) {
            if (i != pettingIndex && selectedFaces[i]) {
                mask |= 1L << i;
            }
        }
        if (mask == 0) {
            for (int i = 0; i < selectedFaces.length && i < 64; i++) {
                if (i != pettingIndex) {
                    mask = 1L << i;
                    break;
                }
            }
        }
        byte[] packet = new byte[11];
        packet[0] = (byte) 0xCB;
        packet[1] = 0x07;
        packet[2] = (byte) pettingIndex;
        writeLongMask(packet, 3, mask);
        return write(packet);
    }

    private void writeLongMask(byte[] packet, int offset, long mask) {
        for (int i = 0; i < 8; i++) {
            packet[offset + i] = (byte) ((mask >> (8 * i)) & 0xFF);
        }
    }

    boolean sendDeviceSettings(boolean sleepEnabled, boolean buzzerEnabled, int brightnessIndex, int sleepTimerMinutes) {
        return write(new byte[]{
                (byte) 0xCB,
                0x08,
                (byte) (sleepEnabled ? 1 : 0),
                (byte) (buzzerEnabled ? 1 : 0),
                (byte) brightnessIndex,
                (byte) sleepTimerMinutes
        });
    }

    boolean sendPhoneNotification(String app, String title, String message) {
        byte[] appBytes = trimUtf8(app, 24);
        byte[] titleBytes = trimUtf8(title, 32);
        byte[] messageBytes = trimUtf8(message, 80);
        byte[] packet = new byte[5 + appBytes.length + titleBytes.length + messageBytes.length];
        packet[0] = (byte) 0xCB;
        packet[1] = 0x02;
        packet[2] = (byte) appBytes.length;
        packet[3] = (byte) titleBytes.length;
        packet[4] = (byte) messageBytes.length;
        int offset = 5;
        System.arraycopy(appBytes, 0, packet, offset, appBytes.length);
        offset += appBytes.length;
        System.arraycopy(titleBytes, 0, packet, offset, titleBytes.length);
        offset += titleBytes.length;
        System.arraycopy(messageBytes, 0, packet, offset, messageBytes.length);
        return write(packet);
    }

    boolean sendMusicLyric(String app, String title, String line) {
        return sendPhoneNotification(app, title, line);
    }

    boolean sendNavigation(boolean active, String title, String directions, String distance, String eta, String speed) {
        byte[] titleBytes = trimUtf8(title, 24);
        byte[] directionBytes = trimUtf8(directions, 64);
        byte[] distanceBytes = trimUtf8(distance, 12);
        byte[] etaBytes = trimUtf8(eta, 16);
        byte[] speedBytes = trimUtf8(speed, 12);
        byte[] packet = new byte[8 + titleBytes.length + directionBytes.length + distanceBytes.length + etaBytes.length + speedBytes.length];
        packet[0] = (byte) 0xCB;
        packet[1] = 0x0B;
        packet[2] = (byte) (active ? 1 : 0);
        packet[3] = (byte) titleBytes.length;
        packet[4] = (byte) directionBytes.length;
        packet[5] = (byte) distanceBytes.length;
        packet[6] = (byte) etaBytes.length;
        packet[7] = (byte) speedBytes.length;
        int offset = 8;
        System.arraycopy(titleBytes, 0, packet, offset, titleBytes.length);
        offset += titleBytes.length;
        System.arraycopy(directionBytes, 0, packet, offset, directionBytes.length);
        offset += directionBytes.length;
        System.arraycopy(distanceBytes, 0, packet, offset, distanceBytes.length);
        offset += distanceBytes.length;
        System.arraycopy(etaBytes, 0, packet, offset, etaBytes.length);
        offset += etaBytes.length;
        System.arraycopy(speedBytes, 0, packet, offset, speedBytes.length);
        return write(packet);
    }

    boolean sendLatencyPing() {
        if (pendingPingSequence >= 0 && android.os.SystemClock.elapsedRealtime() - pingStartedAtMs < 4000) {
            return true;
        }
        int sequence = pingSequence++ & 0xFFFF;
        pendingPingSequence = sequence;
        pingStartedAtMs = 0;
        return write(new byte[]{
                (byte) 0xCB,
                0x09,
                (byte) (sequence & 0xFF),
                (byte) ((sequence >> 8) & 0xFF)
        });
    }

    boolean sendPetCall(boolean active) {
        return write(new byte[]{
                (byte) 0xCB,
                0x0A,
                (byte) (active ? 1 : 0)
        });
    }

    private byte[] trimUtf8(String value, int maxBytes) {
        if (value == null) {
            return new byte[0];
        }
        String clean = value.trim();
        while (!clean.isEmpty()) {
            byte[] bytes = clean.getBytes(StandardCharsets.UTF_8);
            if (bytes.length <= maxBytes) {
                return bytes;
            }
            clean = clean.substring(0, clean.length() - 1);
        }
        return new byte[0];
    }

    private boolean write(byte[] data) {
        if (rx == null || gatt == null || !hasConnectPermission()) {
            listener.onError("Connect Cubie first");
            return false;
        }
        writeQueue.add(data);
        flushWriteQueue();
        return true;
    }

    private void flushWriteQueue() {
        if (writeInProgress || writeQueue.isEmpty()) {
            return;
        }
        if (rx == null || gatt == null || !hasConnectPermission()) {
            writeQueue.clear();
            writeInProgress = false;
            return;
        }
        byte[] data = writeQueue.peek();
        rx.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        rx.setValue(data);
        if (data.length >= 4 && data[0] == (byte) 0xCB && data[1] == 0x09) {
            int sequence = (data[2] & 0xFF) | ((data[3] & 0xFF) << 8);
            if (sequence == pendingPingSequence) {
                pingStartedAtMs = android.os.SystemClock.elapsedRealtime();
            }
        }
        boolean ok = gatt.writeCharacteristic(rx);
        if (!ok) {
            writeQueue.clear();
            writeInProgress = false;
            listener.onError("Write failed");
            return;
        }
        writeInProgress = true;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (!hasScanPermission() || !hasConnectPermission()) {
                listener.onError("Bluetooth permission needed");
                return;
            }
            if (scanner != null) {
                scanner.stopScan(this);
            }
            scanner = null;
            listener.onState("Connecting...", false);
            gatt = device.connectGatt(context, false, gattCallback);
        }

        @Override
        public void onScanFailed(int errorCode) {
            listener.onError("Scan failed: " + errorCode);
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (!hasConnectPermission()) {
                return;
            }
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                listener.onState("Discovering services...", false);
                gatt.discoverServices();
            } else {
                rx = null;
                tx = null;
                writeQueue.clear();
                writeInProgress = false;
                pendingPingSequence = -1;
                pingStartedAtMs = 0;
                listener.onState("Disconnected", false);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService service = gatt.getService(CHRONOS_SERVICE);
            rx = service == null ? null : service.getCharacteristic(CHRONOS_RX);
            tx = service == null ? null : service.getCharacteristic(CHRONOS_TX);
            if (rx == null || tx == null) {
                listener.onError("Cubie service not found");
                return;
            }
            if (!enableNotifications(gatt, tx)) {
                listener.onState("Connected", true);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            writeInProgress = false;
            if (!writeQueue.isEmpty()) {
                writeQueue.poll();
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                writeQueue.clear();
                listener.onError("Write failed: " + status);
                return;
            }
            flushWriteQueue();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            handleNotification(characteristic.getValue());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (CLIENT_CONFIG.equals(descriptor.getUuid())) {
                listener.onState("Connected", true);
            }
        }
    };

    private boolean enableNotifications(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!hasConnectPermission()) {
            return false;
        }
        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CONFIG);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            return gatt.writeDescriptor(descriptor);
        }
        return false;
    }

    private void handleNotification(byte[] data) {
        if (data == null || data.length < 2) {
            return;
        }
        if (data[0] == (byte) 0xCB && data[1] == (byte) 0x8A && data.length >= 3) {
            listener.onPetCallState(data[2] == 1);
            return;
        }
        if (data[0] != (byte) 0xCB || data[1] != (byte) 0x89) {
            return;
        }
        if (data.length < 4) {
            return;
        }
        int sequence = (data[2] & 0xFF) | ((data[3] & 0xFF) << 8);
        if (sequence != pendingPingSequence || pingStartedAtMs == 0) {
            return;
        }
        long latencyMs = android.os.SystemClock.elapsedRealtime() - pingStartedAtMs;
        pendingPingSequence = -1;
        pingStartedAtMs = 0;
        listener.onLatency(latencyMs);
    }

    private boolean hasScanPermission() {
        return Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasConnectPermission() {
        return Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }
}
