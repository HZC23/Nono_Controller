import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;
import android.widget.SeekBar;
import android.view.View;
import android.os.Handler;
import android.os.Looper;
import android.app.Application;
import android.content.Context;
import android.os.Vibrator;
import android.os.Build;
import android.os.VibrationEffect;
import android.bluetooth.BluetoothDevice;

import com.hzc.nonocontroller.data.TelemetryData;
import androidx.lifecycle.Observer;

import java.util.LinkedList;
import java.util.Queue;

import static com.hzc.nonocontroller.Constants.*;

import com.hzc.nonocontroller.ble.NonoBleManager;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

public class MainViewModel extends ViewModel implements ConnectionObserver {

    private final NonoBleManager bleManager;
    private final Observer<TelemetryData> telemetryObserver;
    private final Application application; // Storing application context
    private final Vibrator vibrator;

    // Command Queueing
    private final Queue<String> commandQueue = new LinkedList<>();
    private final Handler sendingHandler = new Handler(Looper.getMainLooper());
    private static final long SEND_DELAY_MS = 100; // Delay between sending commands

    // LiveData for UI state
    private final MutableLiveData<TelemetryData> _telemetry = new MutableLiveData<>(new TelemetryData());
    public final LiveData<TelemetryData> telemetry = _telemetry;

    private final LinkedList<String> serialLogBuffer = new LinkedList<>();
    private final MutableLiveData<String> _serialMonitor = new MutableLiveData<>("");
    public final LiveData<String> serialMonitor = _serialMonitor;

    private final MutableLiveData<Integer> _connectionState = new MutableLiveData<>();
    public final LiveData<Integer> connectionState = _connectionState;

    private final MutableLiveData<Integer> _speed = new MutableLiveData<>(150);
    public final LiveData<Integer> speed = _speed;

    private final MutableLiveData<LinkedList<String>> _musicFiles = new MutableLiveData<>(new LinkedList<>());
    public final LiveData<LinkedList<String>> musicFiles = _musicFiles;

    private final MutableLiveData<Integer> _heading = new MutableLiveData<>(0);
    public final LiveData<Integer> heading = _heading;

    private final MutableLiveData<Boolean> _isCalibrating = new MutableLiveData<>(false);
    public final LiveData<Boolean> isCalibrating = _isCalibrating;

    public MainViewModel(Application application, NonoBleManager bleManager) {
        super();
        this.application = application;
        this.bleManager = bleManager;
        this.vibrator = (Vibrator) application.getSystemService(Context.VIBRATOR_SERVICE);
        // Observe the telemetry data to update UI visibility
        telemetryObserver = this::updateUIVisibilityFromState;
        _telemetry.observeForever(telemetryObserver);

        // Observe connection state to manage command sending
        _connectionState.observeForever(s -> {
            if (bleManager.isConnected()) {
                attemptToSendNextCommand(); // Start sending any queued commands
            }
        });
    }

    public NonoBleManager getBleManager() {
        return bleManager;
    }

    public void connect(BluetoothDevice device) {
        bleManager.connect(device)
                .retry(3, 100)
                .useAutoConnect(false)
                .enqueue();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        _telemetry.removeObserver(telemetryObserver);
        sendingHandler.removeCallbacksAndMessages(null); // Stop any pending sends
        bleManager.disconnect().enqueue();
    }

    // --- UI Visibility LiveData ---
    private final MutableLiveData<Integer> _autonomousModesVisibility = new MutableLiveData<>(View.VISIBLE);
    public final LiveData<Integer> autonomousModesVisibility = _autonomousModesVisibility;

    private final MutableLiveData<Integer> _activeAutonomousControlsVisibility = new MutableLiveData<>(View.GONE);
    public final LiveData<Integer> activeAutonomousControlsVisibility = _activeAutonomousControlsVisibility;

    private final MutableLiveData<Integer> _systemControlsVisibility = new MutableLiveData<>(View.GONE);
    public final LiveData<Integer> systemControlsVisibility = _systemControlsVisibility;


    // --- LiveData Updaters ---
    public void updateTelemetry(TelemetryData newTelemetry) {
        _telemetry.postValue(newTelemetry);
    }

    private void updateUIVisibilityFromState(TelemetryData telemetryData) {
        if (telemetryData == null || telemetryData.getState() == null) {
            setIdleModeVisibility();
            return;
        }

        String state = telemetryData.getState();
        if (state.contains("FOLLOW_HEADING") || state.contains("SMART_AVOIDANCE")) {
            setActiveAutonomousModeVisibility();
        } else { // Covers IDLE, MANUAL_CONTROL, etc.
            setIdleModeVisibility();
        }
    }

    private void setIdleModeVisibility() {
        _autonomousModesVisibility.postValue(View.VISIBLE);
        _activeAutonomousControlsVisibility.postValue(View.GONE);
    }

    private void setActiveAutonomousModeVisibility() {
        _autonomousModesVisibility.postValue(View.GONE);
        _activeAutonomousControlsVisibility.postValue(View.VISIBLE);
    }

    public void updateSerialMonitor(String newText) {
        serialLogBuffer.add(newText.trim()); // Add new line, trimmed

        // Remove oldest lines if buffer exceeds limit
        final int MAX_SERIAL_MONITOR_LINES = 100;
        while (serialLogBuffer.size() > MAX_SERIAL_MONITOR_LINES) {
            serialLogBuffer.removeFirst();
        }

        // Combine lines for display
        StringBuilder sb = new StringBuilder();
        for (String line : serialLogBuffer) {
            sb.append(line).append("\n");
        }
        _serialMonitor.postValue(sb.toString());
    }

    // --- UI Event Handlers ---

    public void onTurretScanClicked(View view) {
        performHapticFeedback();
        sendCommand(buildCommand(ACTION_SCAN, VALUE_START));
    }

    public void onStopButtonClicked() {
        sendCommand(buildMovementCommand(0, 0));
    }

    // --- Manual Control ---
    public void onUpButtonClicked() { onDirectionalButton(DIRECTION_UP); }
    public void onDownButtonClicked() { onDirectionalButton(DIRECTION_DOWN); }
    public void onLeftButtonClicked() { onDirectionalButton(DIRECTION_LEFT); }
    public void onRightButtonClicked() { onDirectionalButton(DIRECTION_RIGHT); }

    public void onDirectionalButton(String direction) {
        int velocity = 0;
        int angular = 0;
        switch (direction) {
            case DIRECTION_UP:
                velocity = 100;
                break;
            case DIRECTION_DOWN:
                velocity = -100;
                break;
            case DIRECTION_LEFT:
                angular = -100;
                break;
            case DIRECTION_RIGHT:
                angular = 100;
                break;
        }
        sendCommand(buildMovementCommand(velocity, angular));
    }

    public void onDirectionalButtonReleased() {
        sendCommand(buildMovementCommand(0, 0)); // Stop all movement
    }


    // --- Autonomous Control ---
    public void onSmartAvoidanceClicked() {
        performHapticFeedback();
        sendCommand(buildCommand(ACTION_MODE, VALUE_AVOID));
    }


    public void onGoToHeadingClicked(int heading) {
        performHapticFeedback();
        sendCommand(buildCommand(ACTION_GOTO, heading));
    }

    public void onSentryModeClicked() {
        performHapticFeedback();
        sendCommand(buildCommand(ACTION_MODE, VALUE_SENTRY));
    }

    public void onPauseClicked() {
        performHapticFeedback();
        sendCommand(buildCommand(ACTION_PAUSE));
    }

    public void onResumeClicked() {
        performHapticFeedback();
        sendCommand(buildCommand(ACTION_RESUME));
    }

    // --- Settings ---
    public void onSettingsClicked() {
        performHapticFeedback();
        // Logic to show settings dialog will be in MainActivity
    }

    public void onToggleConsole(boolean show) {
        performHapticFeedback();
        // This will be used to update a LiveData for console visibility
    }

    public void onGoToCalibrationClicked() {
        performHapticFeedback();
        // This could navigate to a new Activity or show a specific dialog
    }

    // --- Accessories & System ---
    public void onLightSwitched(boolean isChecked) {
        performHapticFeedback();
        if (isChecked) {
            sendCommand(buildCommand(ACTION_LIGHT, VALUE_ON));
        } else {
            sendCommand(buildCommand(ACTION_LIGHT, VALUE_OFF));
        }
    }

    public final SeekBar.OnSeekBarChangeListener onSpeedChanged = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                _speed.setValue(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            performHapticFeedback();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            performHapticFeedback();
            sendCommand(buildCommand(ACTION_SPEED, seekBar.getProgress()));
        }
    };

    public void onCalibrateCompassClicked() {
        performHapticFeedback();
        sendCommand(buildCommand(ACTION_CALIBRATE, VALUE_COMPASS));
    }

    public void onStartCalibrationClicked() {
        sendCommand("CMD:CALIBRATE:COMPASS\n");
    }

    public void updateHeading(int newHeading) {
        _heading.postValue(newHeading);
    }

    public void setCalibrating(boolean isCalibrating) {
        _isCalibrating.postValue(isCalibrating);
    }


    public void onSetCompassOffsetClicked(float offset) {
        performHapticFeedback();
        sendCommand(buildCommand(ACTION_COMPASS_OFFSET, offset));
    }

    public void onSendLcdMessageClicked(String message) {
        performHapticFeedback();
        if (message != null && !message.isEmpty()) {
            sendCommand(buildCommand(ACTION_LCD, message));
        }
    }

    public void onAnimClicked(String anim) {
        performHapticFeedback();
        sendCommand(buildCommand(ACTION_ANIM, anim));
    }


    // --- Private Helper ---
    public void sendCommand(String command) {
        commandQueue.add(command);
        attemptToSendNextCommand();
    }

    private void attemptToSendNextCommand() {
        if (bleManager.isConnected() && !commandQueue.isEmpty()) {
            String command = commandQueue.poll();
            if (command != null) {
                bleManager.sendCommand(command);
                Log.d("MainViewModel", "Sent: " + command.trim());
            }
            // Schedule the next command sending
            sendingHandler.postDelayed(this::attemptToSendNextCommand, SEND_DELAY_MS);
        } else if (!bleManager.isConnected() && !commandQueue.isEmpty()) {
            Log.w("MainViewModel", "Not connected, commands queued: " + commandQueue.size());
        }
    }

    public void addMusicFile(String filename) {
        LinkedList<String> currentList = _musicFiles.getValue();
        if (currentList == null) {
            currentList = new LinkedList<>();
        }
        currentList.add(filename);
        _musicFiles.postValue(currentList);
    }

    public void clearMusicFiles() {
        _musicFiles.postValue(new LinkedList<>());
    }

    public void performHapticFeedback() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // Deprecated in API 26
                vibrator.vibrate(50);
            }
        }
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {
        _connectionState.postValue(0); // Using 0 for connecting, etc. Need real consts
    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        _connectionState.postValue(1);
    }

    @Override
    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
        _connectionState.postValue(-1);
    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {
        _connectionState.postValue(2);
    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
        _connectionState.postValue(3);
    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
        _connectionState.postValue(-1);
    }
}