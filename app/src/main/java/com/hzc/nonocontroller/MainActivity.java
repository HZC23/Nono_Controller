package com.hzc.nonocontroller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

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

        blunoLibrary = BlunoLibrary.getInstance(this, this);
        MainViewModelFactory factory = new MainViewModelFactory(getApplication(), blunoLibrary);
        viewModel = new ViewModelProvider(this, factory).get(MainViewModel.class);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        // Initialize BlunoLibrary
        blunoLibrary.request(1000, new BlunoLibrary.OnPermissionsResult() {
            @Override
            public void OnSuccess() {
                // Permissions granted
            }

            @Override
            public void OnFail(List<String> noPermissions) {
                Toast.makeText(MainActivity.this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        });

        blunoLibrary.onCreateProcess();
        blunoLibrary.serialBegin(115200);
    }

    // BlunoLibrary lifecycle methods
    @Override
    protected void onResume() {
        super.onResume();
        blunoLibrary.setDelegate(this);
        blunoLibrary.onResumeProcess();
    }

    @Override
    protected void onPause() {
        super.onPause();
        blunoLibrary.onPauseProcess();
    }

    @Override
    protected void onStop() {
        super.onStop();
        blunoLibrary.onStopProcess();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        blunoLibrary.onDestroyProcess();
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
        viewModel.updateSerialMonitor(theString);

        try {
            JSONObject json = new JSONObject(theString.trim());
            Boolean isCalibrating = viewModel.isCalibrating.getValue();
            if (isCalibrating != null && isCalibrating) {
                if (json.has("heading")) {
                    viewModel.updateHeading(json.getInt("heading"));
                }
            } else {
                TelemetryData newTelemetry = new TelemetryData();
                if (json.has("state")) newTelemetry.setState(json.getString("state"));
                if (json.has("heading")) newTelemetry.setHeading(json.getInt("heading"));
                if (json.has("distance")) newTelemetry.setDistance(json.getInt("distance"));
                if (json.has("distanceLaser")) newTelemetry.setDistanceLaser(json.getInt("distanceLaser"));
                if (json.has("battery")) newTelemetry.setBattery(json.getInt("battery"));
                if (json.has("speedTarget")) newTelemetry.setSpeedTarget(json.getInt("speedTarget"));
                viewModel.updateTelemetry(newTelemetry);
                Log.d("MainActivity", "Telemetry updated: " + newTelemetry.getState());
            }
        } catch (JSONException e) {
            Log.e("MainActivity", "Failed to parse JSON: " + theString, e);
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
                            viewModel.setConnectionState(blunoLibrary.mConnectionState); // Let the BlunoLibrary manage its state and notify its delegate.
                        }
                        else{
                            Log.i("MainActivity", "onListItemClick " + device.getName());
                            Log.i("MainActivity", "Device Name:"+device.getName() + "   " + "Device Address:" + device.getAddress());

                            // These are now handled by BlunoLibrary.connect
                            // blunoLibrary.mDeviceName=device.getName();
                            // blunoLibrary.mDeviceAddress=device.getAddress();

                            if (blunoLibrary.connect(device.getAddress())) { // Pass address to connect
                                Log.d("MainActivity", "Connect request success");
                                viewModel.setConnectionState(blunoLibrary.mConnectionState); // Let the BlunoLibrary manage its state and notify its delegate.
                                // mHandler.postDelayed(mConnectingOverTimeRunnable, 10000); // Handled internally by BlunoLibrary if needed
                            }
                            else {
                                Log.d("MainActivity", "Connect request fail");
                                viewModel.setConnectionState(blunoLibrary.mConnectionState); // Let the BlunoLibrary manage its state and notify its delegate.
                            }
                        }
                        mScanDeviceDialog.dismiss();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @SuppressLint("MissingPermission")
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        Log.i("MainActivity", "Scan dialog cancelled");
                        blunoLibrary.scanLeDevice(false); // Stop scanning
                        viewModel.setConnectionState(BlunoLibrary.connectionStateEnum.isToScan); // Explicitly set state
                        mScanDeviceDialog.dismiss();
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