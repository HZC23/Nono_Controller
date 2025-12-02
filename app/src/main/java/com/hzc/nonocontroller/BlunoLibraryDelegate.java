package com.hzc.nonocontroller;

import android.bluetooth.BluetoothDevice;
import com.hzc.nonocontroller.BlunoLibrary; // Assuming this BlunoLibrary will be concrete now.

import java.util.List;

public interface BlunoLibraryDelegate {
    void onConectionStateChange(BlunoLibrary.connectionStateEnum theConnectionState);
    void onSerialReceived(String theString);
    void onScanDialogRequested(); // Added missing method
    void onDeviceDiscovered(BluetoothDevice device); // Added missing method
}