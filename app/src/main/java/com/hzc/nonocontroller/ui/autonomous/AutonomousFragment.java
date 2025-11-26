package com.hzc.nonocontroller.ui.autonomous;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hzc.nonocontroller.R;
import com.hzc.nonocontroller.databinding.FragmentAutonomousBinding;
import com.hzc.nonocontroller.viewmodel.MainViewModel;

public class AutonomousFragment extends Fragment {

    private FragmentAutonomousBinding binding;
    private MainViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_autonomous, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        setupListeners();
    }

    private void setupListeners() {
        binding.gotoHeadingButton.setOnClickListener(v -> showGoToHeadingDialog());
    }

    private void showGoToHeadingDialog() {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("0-359");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.goto_heading_title)
                .setMessage(R.string.goto_heading_message)
                .setView(input)
                .setPositiveButton(R.string.go_button, (dialog, which) -> {
                    String value = input.getText().toString();
                    if (!value.isEmpty()) {
                        try {
                            int heading = Integer.parseInt(value);
                            viewModel.onGoToHeadingClicked(heading);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), R.string.invalid_input, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }
}