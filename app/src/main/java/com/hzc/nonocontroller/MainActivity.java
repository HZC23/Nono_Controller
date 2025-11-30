package com.hzc.nonocontroller;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hzc.nonocontroller.data.TelemetryData;
import com.hzc.nonocontroller.util.SettingsManager;
import com.hzc.nonocontroller.viewmodel.MainViewModel;
import com.hzc.nonocontroller.viewmodel.MainViewModelFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import com.hzc.nonocontroller.util.SettingsManager;

public class MainActivity extends AppCompatActivity implements BlunoLibraryDelegate {
    private MainViewModel viewModel;
    private BlunoLibrary blunoLibrary;
    private SettingsManager settingsManager;
    private TextView connectionStatusTextView; // Declare TextView
    private LeDeviceListAdapter mLeDeviceListAdapter; // Declare LeDeviceListAdapter
    private AlertDialog mScanDeviceDialog; // Declare AlertDialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingsManager = new SettingsManager(this);
        if (settingsManager.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_main);

        connectionStatusTextView = findViewById(R.id.text_connection_status); // Initialize TextView

        BleManager.initialize(this);
        blunoLibrary = BleManager.getInstance();
        MainViewModelFactory factory = new MainViewModelFactory(getApplication(), blunoLibrary, settingsManager);
        viewModel = new ViewModelProvider(this, factory).get(MainViewModel.class);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavigationUI.setupWithNavController(navView, navController);

        // Observe connection state
        viewModel.connectionState.observe(this, connectionStateEnum -> {
            String statusText;
            switch (connectionStateEnum) {
                case isConnected:
                    statusText = "Connected";
                    break;
                case isConnecting:
                    statusText = "Connecting...";
                    break;
                case isScanning:
                    statusText = "Scanning...";
                    break;
                case isToScan:
                    statusText = "Disconnected";
                    break;
                case isDisconnecting:
                    statusText = "Disconnecting...";
                    break;
                default:
                    statusText = "Unknown";
                    break;
            }
            connectionStatusTextView.setText(statusText);
        });

        // Initialize BlunoLibrary
        blunoLibrary.request(1000, new BlunoLibrary.OnPermissionsResult() {
            @Override
            public void OnSuccess() {
                Toast.makeText(MainActivity.this, "Permissions granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void OnFail(List<String> noPermissions) {
                Toast.makeText(MainActivity.this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // BlunoLibrary lifecycle methods
    @Override
    protected void onResume() {
        super.onResume();
        BleManager.registerDelegate(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BleManager.unregisterDelegate(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // No longer call blunoLibrary.onStopProcess();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // No longer call blunoLibrary.onDestroyProcess();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        blunoLibrary.onRequestPermissionsResultProcess(requestCode, permissions, grantResults);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        blunoLibrary.onActivityResultProcess(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    public BlunoLibrary getBlunoLibrary() {
        return blunoLibrary;
    }

    // BlunoLibraryDelegate methods
    @Override
    public void onConectionStateChange(BlunoLibrary.connectionStateEnum theConnectionState) {
        viewModel.setConnectionState(theConnectionState);
        // Removed toasts to avoid spamming the user
    }

    @Override
    public void onSerialReceived(String theString) {
        Log.d("MainActivity", "Serial received: " + theString);
        // Update the serial monitor first
        viewModel.updateSerialMonitor(theString);

        // Parse music file responses
        if (theString.startsWith(Constants.RSP_MUSIC_FILE_PREFIX)) {
            String filename = theString.substring(Constants.RSP_MUSIC_FILE_PREFIX.length()).trim();
            viewModel.addMusicFile(filename);
            return; // Consume the message, it's not JSON telemetry
        }

        // Parse the JSON telemetry data
        try {
            JSONObject json = new JSONObject(theString.trim());

            // Create a new object for the update to ensure LiveData triggers reliably.
            TelemetryData newTelemetry = new TelemetryData();

            // Populate the new object directly from the JSON data.
            if (json.has("state")) newTelemetry.setState(json.getString("state"));
            if (json.has("heading")) newTelemetry.setHeading(json.getInt("heading"));
            if (json.has("distance")) newTelemetry.setDistance(json.getInt("distance"));
            if (json.has("distanceLaser")) newTelemetry.setDistanceLaser(json.getInt("distanceLaser"));
            if (json.has("battery")) {
                int batteryLevel = json.getInt("battery");
                newTelemetry.setBattery(batteryLevel);
                if (batteryLevel == 0) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Robot battery empty. Please recharge.", Toast.LENGTH_LONG).show());
                }
            }
            if (json.has("speedTarget")) newTelemetry.setSpeedTarget(json.getInt("speedTarget"));
            // speedCurrent is not sent by the robot, so we don't parse it.

            // Update the ViewModel with the new data object.
            viewModel.updateTelemetry(newTelemetry);
            Log.d("MainActivity", "Telemetry updated: " + newTelemetry.getState());

        } catch (JSONException e) {
            Log.e("MainActivity", "Failed to parse JSON: " + theString, e);
            // This is expected if the serial string is not a JSON object (e.g., a debug message)
            // Do nothing with it for telemetry.
        }
    }

    @Override
    public void onScanDialogRequested() {
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        // Initializes and show the scan Device Dialog
        mScanDeviceDialog = new AlertDialog.Builder(this) // Use 'this' (Activity context)
                .setTitle("BLE Device Scan...")
                .setAdapter(mLeDeviceListAdapter, new DialogInterface.OnClickListener() {

                    @SuppressLint("MissingPermission")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(which);
                        if (device == null)
                            return;

                        blunoLibrary.scanLeDevice(false); // Stop scanning

                        if(device.getName()==null || device.getAddress()==null)
                        {
                            blunoLibrary.mConnectionState= BlunoLibrary.connectionStateEnum.isToScan;
                            blunoLibrary.delegate.onConectionStateChange(blunoLibrary.mConnectionState);
                        }
                        else{
                            Log.i("MainActivity", "onListItemClick " + device.getName());
                            Log.i("MainActivity", "Device Name:"+device.getName() + "   " + "Device Address:" + device.getAddress());

                            // These are now handled by BlunoLibrary.connect
                            // blunoLibrary.mDeviceName=device.getName();
                            // blunoLibrary.mDeviceAddress=device.getAddress();

                            if (blunoLibrary.connect(device.getAddress())) { // Pass address to connect
                                Log.d("MainActivity", "Connect request success");
                                blunoLibrary.mConnectionState= BlunoLibrary.connectionStateEnum.isConnecting;
                                blunoLibrary.delegate.onConectionStateChange(blunoLibrary.mConnectionState);
                                // mHandler.postDelayed(mConnectingOverTimeRunnable, 10000); // Handled internally by BlunoLibrary if needed
                            }
                            else {
                                Log.d("MainActivity", "Connect request fail");
                                blunoLibrary.mConnectionState= BlunoLibrary.connectionStateEnum.isToScan;
                                blunoLibrary.delegate.onConectionStateChange(blunoLibrary.mConnectionState);
                            }
                        }
                        mScanDeviceDialog.dismiss();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @SuppressLint("MissingPermission")
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        Log.i("MainActivity", "mBluetoothAdapter.stopLeScan");

                        blunoLibrary.mConnectionState = BlunoLibrary.connectionStateEnum.isToScan;
                        blunoLibrary.delegate.onConectionStateChange(blunoLibrary.mConnectionState);
                        mScanDeviceDialog.dismiss();

                        blunoLibrary.scanLeDevice(false); // Stop scanning
                    }
                }).create();
        mScanDeviceDialog.show();
        blunoLibrary.scanLeDevice(true); // Start scanning
    }


    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @SuppressLint("MissingPermission")
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view
                        .findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view
                        .findViewById(R.id.device_name);
                Log.i("MainActivity", "mInflator.inflate  getView");
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    @Override
    public void onDeviceDiscovered(final BluetoothDevice device) {
        runOnUiThread(() -> {
            if (mLeDeviceListAdapter != null) {
                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        });
    }
}