package com.hzc.nonocontroller;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.hzc.nonocontroller.databinding.ActivityCalibrationBinding;
import com.hzc.nonocontroller.viewmodel.CalibrationViewModel;
import com.hzc.nonocontroller.viewmodel.CalibrationViewModelFactory;

import org.json.JSONException;
import org.json.JSONObject;

public class CalibrationActivity extends AppCompatActivity implements BlunoLibraryDelegate {

    private BlunoLibrary blunoLibrary;
    private CalibrationViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCalibrationBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_calibration);

        blunoLibrary = BlunoLibrary.getInstance(this, this);

        CalibrationViewModelFactory factory = new CalibrationViewModelFactory(blunoLibrary);
        viewModel = new ViewModelProvider(this, factory).get(CalibrationViewModel.class);

        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(this);

        binding.backButton.setOnClickListener(v -> finish());
    }

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
    protected void onDestroy() {
        super.onDestroy();
        blunoLibrary.onDestroyProcess();
    }

    @Override
    public void onConectionStateChange(BlunoLibrary.connectionStateEnum theConnectionState) {
        // Can be ignored in this activity, or show a toast
        if (theConnectionState == BlunoLibrary.connectionStateEnum.isToScan) {
            Toast.makeText(this, "Device disconnected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSerialReceived(String theString) {
        try {
            JSONObject json = new JSONObject(theString.trim());
            if (json.has("heading")) {
                viewModel.updateHeading(json.getInt("heading"));
            }
        } catch (JSONException e) {
            // Not a JSON string
        }
    }
}