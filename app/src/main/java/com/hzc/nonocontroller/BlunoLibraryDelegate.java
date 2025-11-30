package com.hzc.nonocontroller;

public interface BlunoLibraryDelegate {
    void onConectionStateChange(BlunoLibrary.connectionStateEnum theConnectionState);
    void onSerialReceived(String theString);
    void onScanDialogRequested();
    void onDeviceDiscovered(BluetoothDevice device);
}
