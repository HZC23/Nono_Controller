package com.hzc.nonocontroller.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.data.Data;

public class NonoBleManager extends BleManager {
    private static final String TAG = "NonoBleManager";

    // Nono Robot Service and Characteristics UUIDs
    public static final UUID NONO_SERVICE_UUID = UUID.fromString("0000dfb0-0000-1000-8000-00805f9b34fb");
    private static final UUID SERIAL_CHARACTERISTIC_UUID = UUID.fromString("0000dfb1-0000-1000-8000-00805f9b34fb");
    private static final UUID COMMAND_CHARACTERISTIC_UUID = UUID.fromString("0000dfb2-0000-1000-8000-00805f9b34fb");

    private BluetoothGattCharacteristic commandCharacteristic, serialCharacteristic;

    private DataReceivedCallback telemetryCallback;

    private final BluetoothLeScanner scanner;
    private boolean isScanning;
    private ScanListener scanListener;

    public interface ScanListener {
        void onDeviceFound(BluetoothDevice device);
        void onScanFailed(int errorCode);
    }

    public void setScanListener(ScanListener listener) {
        this.scanListener = listener;
    }

    public NonoBleManager(@NonNull final Context context) {
        super(context);
        this.scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
    }

    public void startScan() {
        if (isScanning) {
            return;
        }
        isScanning = true;
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();
        final List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(NONO_SERVICE_UUID)).build());
        scanner.startScan(filters, settings, scanCallback);
        Log.d(TAG, "Scan started");
    }

    public void stopScan() {
        if (isScanning) {
            isScanning = false;
            scanner.stopScan(scanCallback);
            Log.d(TAG, "Scan stopped");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (scanListener != null) {
                scanListener.onDeviceFound(result.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            if (scanListener != null) {
                scanListener.onScanFailed(errorCode);
            }
        }
    };

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new NonoGattCallback();
    }

    public void setTelemetryCallback(@NonNull final DataReceivedCallback callback) {
        this.telemetryCallback = callback;
    }

    public void sendCommand(@NonNull final String command) {
        if (commandCharacteristic != null) {
            writeCharacteristic(commandCharacteristic, command.getBytes())
                    .enqueue();
        }
    }
    
    private class NonoGattCallback extends BleManagerGattCallback {
        @Override
        protected void initialize() {
            setNotificationCallback(serialCharacteristic)
                    .with(telemetryCallback);
            enableNotifications(serialCharacteristic).enqueue();
        }

        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService service = gatt.getService(NONO_SERVICE_UUID);
            if (service != null) {
                commandCharacteristic = service.getCharacteristic(COMMAND_CHARACTERISTIC_UUID);
                serialCharacteristic = service.getCharacteristic(SERIAL_CHARACTERISTIC_UUID);
            }
            return commandCharacteristic != null && serialCharacteristic != null;
        }

        @Override
        protected void onServicesInvalidated() {
            commandCharacteristic = null;
            serialCharacteristic = null;
        }
    }
    
    @Override
    public void log(final int priority, @NonNull final String message) {
        Log.println(priority, TAG, message);
    }
}
