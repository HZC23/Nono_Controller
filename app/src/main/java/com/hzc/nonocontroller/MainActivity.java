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
        MainViewModelFactory factory = new MainViewModelFactory(blunoLibrary);
        viewModel = new ViewModelProvider(this, factory).get(MainViewModel.class);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

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
        // Update the serial monitor first
        String currentLog = viewModel.serialMonitor.getValue() != null ? viewModel.serialMonitor.getValue() : "";
        viewModel.updateSerialMonitor(currentLog + theString.trim() + "\n");

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
            if (json.has("battery")) newTelemetry.setBattery(json.getInt("battery"));
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
}