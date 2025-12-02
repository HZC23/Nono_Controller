package com.hzc.nonocontroller;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

public class BleManager {

    private static BlunoLibrary blunoLibrary;
    private static BlunoLibraryDelegate currentDelegate;
    private static BlunoLibrary.connectionStateEnum currentConnectionState = BlunoLibrary.connectionStateEnum.isNull;

    public static synchronized void initialize(Context context) {
        if (blunoLibrary == null) {
            Log.d("BleManager", "Initializing BlunoLibrary singleton.");
            blunoLibrary = BlunoLibrary.getInstance(context.getApplicationContext(), new BlunoLibraryDelegate() {
                @Override
                public void onConectionStateChange(BlunoLibrary.connectionStateEnum theConnectionState) {
                    currentConnectionState = theConnectionState; // Update internal state
                    if (currentDelegate != null) {
                        currentDelegate.onConectionStateChange(theConnectionState);
                    }
                }

                @Override
                public void onSerialReceived(String theString) {
                    if (currentDelegate != null) {
                        currentDelegate.onSerialReceived(theString);
                    }
                }

                @Override
                public void onScanDialogRequested() {
                    if (currentDelegate != null) {
                        currentDelegate.onScanDialogRequested();
                    }
                }

                @Override
                public void onDeviceDiscovered(BluetoothDevice device) {
                    if (currentDelegate != null) {
                        currentDelegate.onDeviceDiscovered(device);
                    }
                }
            });
            blunoLibrary.serialBegin(115200);
            blunoLibrary.onCreateProcess(); // This should be called once when the app starts.
        }
    }

    public static synchronized BlunoLibrary getInstance() {
        if (blunoLibrary == null) {
            throw new IllegalStateException("BleManager has not been initialized. Call initialize(context) first.");
        }
        return blunoLibrary;
    }

    public static synchronized void registerDelegate(BlunoLibraryDelegate delegate) {
        Log.d("BleManager", "Registering delegate: " + delegate.getClass().getSimpleName());
        currentDelegate = delegate;
        // Immediately notify the new delegate of the current connection state.
        if (blunoLibrary != null) {
            currentDelegate.onConectionStateChange(currentConnectionState);
        }
    }

    public static synchronized void unregisterDelegate(BlunoLibraryDelegate delegate) {
        if (currentDelegate == delegate) {
            Log.d("BleManager", "Unregistering delegate: " + delegate.getClass().getSimpleName());
            currentDelegate = null;
        }
    }
}
