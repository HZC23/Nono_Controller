package com.hzc.nonocontroller.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.List;

public class BleManager {
    private final static String TAG = BleManager.class.getSimpleName();

    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner bluetoothLeScanner;
    private final Handler handler;

    private BluetoothGatt bluetoothGatt;

    private boolean isScanning = false;

    public BleManager(Context context) {
        this.context = context;
        this.handler = new Handler(context.getMainLooper());
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public void scanBleDevices(final boolean enable) {
        if (enable) {
            if (isScanning) {
                return;
            }
            handler.postDelayed(() -> {
                if (isScanning) {
                    stopScan();
                }
            }, 10000); // Stop scan after 10 seconds
            isScanning = true;
            bluetoothLeScanner.startScan(scanCallback);
            Log.d(TAG, "Started BLE scan.");
        } else {
            stopScan();
        }
    }

    private void stopScan() {
        if (isScanning && bluetoothAdapter.isEnabled()) {
            isScanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
            Log.d(TAG, "Stopped BLE scan.");
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            // TODO: forward the result to a listener
            Log.d(TAG, "Found device: " + result.getDevice().getName());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "Scan failed with error code: " + errorCode);
        }
    };

    public void connect(BluetoothDevice device) {
        if (device == null) {
            return;
        }
        // TODO: implement connection logic
        Log.d(TAG, "Connecting to " + device.getName());
    }



    public void disconnect() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    public void close() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}
