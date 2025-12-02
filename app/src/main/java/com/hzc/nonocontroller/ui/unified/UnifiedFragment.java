package com.hzc.nonocontroller.ui.unified;

import android.app.AlertDialog;
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
            if (blunoLibrary != null) {
                blunoLibrary.buttonScanOnClickProcess();
            }
        });
        
        binding.settingsButton.setOnClickListener(v -> {
            showSettingsDialog();
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
        TextView calibrationButton = dialogView.findViewById(R.id.settings_calibration_button);
        com.google.android.material.switchmaterial.SwitchMaterial darkModeSwitch = dialogView.findViewById(R.id.settings_dark_mode_switch);
        com.google.android.material.switchmaterial.SwitchMaterial invertLayoutSwitch = dialogView.findViewById(R.id.settings_invert_layout_switch);

        SettingsManager settingsManager = new SettingsManager(requireContext());
        darkModeSwitch.setChecked(settingsManager.isDarkMode());
        invertLayoutSwitch.setChecked(settingsManager.isInvertLayout());


        consoleButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Console coming soon!", Toast.LENGTH_SHORT).show();
        });

        calibrationButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Calibration page coming soon!", Toast.LENGTH_SHORT).show();
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
