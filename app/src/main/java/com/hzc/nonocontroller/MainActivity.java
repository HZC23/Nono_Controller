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

import no.nordicsemi.android.ble.observer.ConnectionObserver;
import com.hzc.nonocontroller.ble.NonoBleManager;

public class MainActivity extends AppCompatActivity {
    private MainViewModel viewModel;
    private NonoBleManager bleManager;
    private SettingsManager settingsManager;
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

        bleManager = new NonoBleManager(this);
        MainViewModelFactory factory = new MainViewModelFactory(getApplication(), bleManager);
        viewModel = new ViewModelProvider(this, factory).get(MainViewModel.class);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        // Set up the connection observer
        bleManager.setConnectionObserver(viewModel);
        bleManager.setTelemetryCallback(new DataReceivedCallback() {
            @Override
            public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
                final String text = data.getStringValue(0);
                if (text == null) return;
                Log.d("MainActivity", "Serial received: " + text);
                
                // The data may contain multiple JSON objects separated by newlines.
                String[] jsonStrings = text.split("\n");
                for (String jsonString : jsonStrings) {
                    if (jsonString.trim().isEmpty()) continue;
                    
                    viewModel.updateSerialMonitor(jsonString);

                    try {
                        JSONObject json = new JSONObject(jsonString.trim());
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
                        Log.e("MainActivity", "Failed to parse JSON: " + jsonString, e);
                    }
                }
            }
        });
    }
}