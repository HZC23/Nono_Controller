package com.hzc.nonocontroller.ui.unified;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.hzc.nonocontroller.BlunoLibrary;
import com.hzc.nonocontroller.MainActivity;
import com.hzc.nonocontroller.R;
import com.hzc.nonocontroller.databinding.FragmentMainUnifiedBinding;
import com.hzc.nonocontroller.util.SettingsManager;
import com.hzc.nonocontroller.viewmodel.MainViewModel;

public class UnifiedFragment extends Fragment {

    private FragmentMainUnifiedBinding binding;
    private MainViewModel mainViewModel;
    private BlunoLibrary blunoLibrary;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        mainViewModel =
                new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        binding = FragmentMainUnifiedBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(this);
        binding.setViewModel(mainViewModel);

        blunoLibrary = ((MainActivity) requireActivity()).getBlunoLibrary();

        binding.scanButton.setOnClickListener(v -> {
            mainViewModel.performHapticFeedback();
            if (blunoLibrary != null) {
                blunoLibrary.buttonScanOnClickProcess();
            }
        });

        binding.buttonUp.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                mainViewModel.onDirectionalButton("UP");
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                mainViewModel.onDirectionalButtonReleased();
            }
            return true;
        });

        binding.buttonDown.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                mainViewModel.onDirectionalButton("DOWN");
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                mainViewModel.onDirectionalButtonReleased();
            }
            return true;
        });

        binding.buttonLeft.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                mainViewModel.onDirectionalButton("LEFT");
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                mainViewModel.onDirectionalButtonReleased();
            }
            return true;
        });

        binding.buttonRight.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                mainViewModel.onDirectionalButton("RIGHT");
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                mainViewModel.onDirectionalButtonReleased();
            }
            return true;
        });
        
        binding.settingsButton.setOnClickListener(v -> {
            mainViewModel.performHapticFeedback();
            showSettingsDialog();
        });
        
        binding.mainSendLcdMessageButton.setOnClickListener(v -> {
            String message = binding.mainLcdMessageInput.getText().toString();
            if (!message.isEmpty()) {
                mainViewModel.onSendLcdMessageClicked(message);
                binding.mainLcdMessageInput.setText(""); // Clear input
            } else {
                Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        mainViewModel.telemetry.observe(getViewLifecycleOwner(), telemetryData -> {
            // This is a workaround to force the UI to update.
            // The data binding should handle this automatically, but for some reason it's not.
            binding.telemetryStateValue.setText(telemetryData.getState());
            binding.telemetryHeadingValue.setText(String.valueOf(telemetryData.getHeading()));
            binding.telemetryDistanceUsValue.setText(String.valueOf(telemetryData.getDistance()));
            binding.telemetryDistanceLaserValue.setText(String.valueOf(telemetryData.getDistanceLaser()));
            binding.telemetryBatteryValue.setText(String.valueOf(telemetryData.getBattery()));
            binding.telemetrySpeedTargetValue.setText(String.valueOf(telemetryData.getSpeedTarget()));
        });

        mainViewModel.connectionState.observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case isConnected:
                    binding.scanButton.setText("Disconnect");
                    break;
                case isConnecting:
                    binding.scanButton.setText("Connecting...");
                    break;
                case isScanning:
                    binding.scanButton.setText("Scanning...");
                    break;
                default:
                    binding.scanButton.setText("Scan");
                    break;
            }
        });

        SettingsManager settingsManager = new SettingsManager(requireContext());
        if (settingsManager.isInvertLayout()) {
            invertLayout();
        }

        return binding.getRoot();
    }

    private void invertLayout() {
        androidx.constraintlayout.widget.ConstraintLayout constraintLayout = binding.rootLayout;
        androidx.constraintlayout.widget.ConstraintSet constraintSet = new androidx.constraintlayout.widget.ConstraintSet();
        constraintSet.clone(constraintLayout);
        
        // Clear existing constraints for dpad_section and actions_section
        constraintSet.clear(R.id.dpad_section, androidx.constraintlayout.widget.ConstraintSet.START);
        constraintSet.clear(R.id.dpad_section, androidx.constraintlayout.widget.ConstraintSet.END);
        constraintSet.clear(R.id.actions_section, androidx.constraintlayout.widget.ConstraintSet.START);
        constraintSet.clear(R.id.actions_section, androidx.constraintlayout.widget.ConstraintSet.END);
        
        // Re-apply constraints to swap them
        constraintSet.connect(R.id.dpad_section, androidx.constraintlayout.widget.ConstraintSet.START, R.id.telemetry_section, androidx.constraintlayout.widget.ConstraintSet.END);
        constraintSet.connect(R.id.dpad_section, androidx.constraintlayout.widget.ConstraintSet.END, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.END);
        
        constraintSet.connect(R.id.actions_section, androidx.constraintlayout.widget.ConstraintSet.START, androidx.constraintlayout.widget.ConstraintSet.PARENT_ID, androidx.constraintlayout.widget.ConstraintSet.START);
        constraintSet.connect(R.id.actions_section, androidx.constraintlayout.widget.ConstraintSet.END, R.id.telemetry_section, androidx.constraintlayout.widget.ConstraintSet.START);

        constraintSet.applyTo(constraintLayout);
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView);

        TextView consoleButton = dialogView.findViewById(R.id.settings_console_button);
        com.google.android.material.switchmaterial.SwitchMaterial darkModeSwitch = dialogView.findViewById(R.id.settings_dark_mode_switch);
        com.google.android.material.switchmaterial.SwitchMaterial invertLayoutSwitch = dialogView.findViewById(R.id.settings_invert_layout_switch);

        SettingsManager settingsManager = new SettingsManager(requireContext());
        darkModeSwitch.setChecked(settingsManager.isDarkMode());
        invertLayoutSwitch.setChecked(settingsManager.isInvertLayout());


        consoleButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Console coming soon!", Toast.LENGTH_SHORT).show();
        });

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setDarkMode(isChecked);
            requireActivity().recreate();
        });

        invertLayoutSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setInvertLayout(isChecked);
            requireActivity().recreate();
        });


        builder.setPositiveButton("OK", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
