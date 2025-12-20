package com.hzc.nonocontroller.ui.unified;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.hzc.nonocontroller.R;
import com.hzc.nonocontroller.ble.NonoBleManager;
import com.hzc.nonocontroller.databinding.FragmentMainUnifiedBinding;
import com.hzc.nonocontroller.viewmodel.MainViewModel;

public class UnifiedFragment extends Fragment {

    private MainViewModel viewModel;
    private NonoBleManager bleManager;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private AlertDialog mScanDeviceDialog;
    private FragmentMainUnifiedBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainUnifiedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        bleManager = viewModel.getBleManager();

        mLeDeviceListAdapter = new LeDeviceListAdapter(requireContext());

        binding.scanButton.setOnClickListener(v -> {
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();
            bleManager.startScan();
            showScanDialog();
        });
    }

    private void showScanDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Scan for devices");
        builder.setAdapter(mLeDeviceListAdapter, (dialog, which) -> {
            final BluetoothDevice device = mLeDeviceListAdapter.getDevice(which);
            if (device == null) return;
            bleManager.stopScan();
            viewModel.connect(device);
            mScanDeviceDialog.dismiss();
        });
        mScanDeviceDialog = builder.create();
        mScanDeviceDialog.show();

        bleManager.setScanListener(new NonoBleManager.ScanListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onDeviceFound(BluetoothDevice device) {
                if (device.getName() != null) {
                    requireActivity().runOnUiThread(() -> {
                        mLeDeviceListAdapter.addDevice(device);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    });
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(requireContext(), "Scan failed with error code: " + errorCode, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        bleManager.stopScan();
    }
}
