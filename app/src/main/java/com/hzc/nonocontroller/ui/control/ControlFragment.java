package com.hzc.nonocontroller.ui.control;

import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.hzc.nonocontroller.Constants;
import com.hzc.nonocontroller.R;
import com.hzc.nonocontroller.databinding.FragmentControlBinding;
import com.hzc.nonocontroller.viewmodel.MainViewModel;

public class ControlFragment extends Fragment {

    private FragmentControlBinding binding;
    private MainViewModel viewModel;

    // Threshold for joystick "flat area" to ignore minor movements
    private static final float JOYSTICK_DEAD_ZONE = 0.1f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_control, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get ViewModel from parent activity to share data (e.g., BlunoLibrary)
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        setupListeners(binding);

        // Request focus to receive gamepad events
        view.setFocusableInTouchMode(true);
        view.requestFocus();

        // Set OnGenericMotionListener for joystick input
        view.setOnGenericMotionListener((v, event) -> {
            // Check if the event comes from a game controller
            if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
                    event.getAction() == MotionEvent.ACTION_MOVE) {

                // Get X and Y axis values for the left joystick
                float xAxis = getCenteredAxis(event, 18);
                float yAxis = getCenteredAxis(event, 19);

                // Determine direction based on joystick input
                if (Math.abs(yAxis) > Math.abs(xAxis)) { // Prioritize vertical movement
                    if (yAxis < -JOYSTICK_DEAD_ZONE) { // Up
                        viewModel.onDirectionalButton(Constants.DIRECTION_UP);
                    } else if (yAxis > JOYSTICK_DEAD_ZONE) { // Down
                        viewModel.onDirectionalButton(Constants.DIRECTION_DOWN);
                    } else { // Centered vertically
                        viewModel.onDirectionalButtonReleased();
                    }
                } else { // Prioritize horizontal movement
                    if (xAxis < -JOYSTICK_DEAD_ZONE) { // Left
                        viewModel.onDirectionalButton(Constants.DIRECTION_LEFT);
                    } else if (xAxis > JOYSTICK_DEAD_ZONE) { // Right
                        viewModel.onDirectionalButton(Constants.DIRECTION_RIGHT);
                    } else { // Centered horizontally
                        viewModel.onDirectionalButtonReleased();
                    }
                }
                return true; // Event handled
            }
            return false;
        });

        // Set OnKeyListener for gamepad button input
        view.setOnKeyListener((v, keyCode, event) -> {
            // Check if the event comes from a game controller
            if ((event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (event.getSource() & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) {

                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) { // Only handle initial press
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_BUTTON_A:
                            viewModel.onStopButtonClicked(); // Map A button to STOP
                            return true;
                        case KeyEvent.KEYCODE_BUTTON_B:
                            viewModel.onSmartAvoidanceClicked(); // Map B button to Smart Avoidance
                            return true;
                        case KeyEvent.KEYCODE_DPAD_UP:
                            viewModel.onDirectionalButton(Constants.DIRECTION_UP);
                            return true;
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            viewModel.onDirectionalButton(Constants.DIRECTION_DOWN);
                            return true;
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                            viewModel.onDirectionalButton(Constants.DIRECTION_LEFT);
                            return true;
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            viewModel.onDirectionalButton(Constants.DIRECTION_RIGHT);
                            return true;
                    }
                } else if (event.getAction() == KeyEvent.ACTION_UP) { // Handle key up for directional buttons
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_UP:
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                        case KeyEvent.KEYCODE_DPAD_LEFT:
                        case KeyEvent.KEYCODE_DPAD_RIGHT:
                            viewModel.onDirectionalButtonReleased();
                            return true;
                    }
                }
            }
            return false;
        });
    }

    private void setupListeners(FragmentControlBinding binding) {
        // D-Pad from controls_panel.xml (now directly in fragment_control.xml)
        setupDirectionalButton(binding.buttonUp, Constants.DIRECTION_UP);
        setupDirectionalButton(binding.buttonDown, Constants.DIRECTION_DOWN);
        setupDirectionalButton(binding.buttonLeft, Constants.DIRECTION_LEFT);
        setupDirectionalButton(binding.buttonRight, Constants.DIRECTION_RIGHT);

        // LCD message send button
        binding.mainSendLcdMessageButton.setOnClickListener(v -> {
            EditText lcdMessageInput = binding.mainLcdMessageInput;
            String message = lcdMessageInput.getText().toString();
            if (!message.isEmpty()) {
                viewModel.onSendLcdMessageClicked(message);
                lcdMessageInput.setText(""); // Clear input
            } else {
                Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDirectionalButton(ImageButton button, final String direction) {
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        viewModel.onDirectionalButton(direction);
                        return true; // Consume the event
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        viewModel.onDirectionalButtonReleased();
                        return true; // Consume the event
                }
                return false;
            }
        });
    }

    private float getCenteredAxis(MotionEvent event, int axis) {
        final InputDevice.MotionRange range = event.getDevice().getMotionRange(axis, event.getSource());
        if (range != null) {
            final float flat = range.getFlat();
            final float value = event.getAxisValue(axis);
            // Apply dead zone
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }
}
