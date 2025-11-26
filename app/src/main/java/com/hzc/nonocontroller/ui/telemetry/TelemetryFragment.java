package com.hzc.nonocontroller.ui.telemetry;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.hzc.nonocontroller.R;
import com.hzc.nonocontroller.databinding.FragmentTelemetryBinding;
import com.hzc.nonocontroller.viewmodel.MainViewModel;

public class TelemetryFragment extends Fragment {

    private FragmentTelemetryBinding binding;
    private MainViewModel viewModel;
    private TextView serialLogTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_telemetry, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        serialLogTextView = binding.serialLogTextView;
        serialLogTextView.setMovementMethod(new ScrollingMovementMethod());

        viewModel.serialMonitor.observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                // Scroll to the bottom when new text is added
                serialLogTextView.post(() -> serialLogTextView.scrollTo(0, serialLogTextView.getLayout().getLineTop(serialLogTextView.getLineCount()) - serialLogTextView.getHeight()));
            }
        });
    }
}