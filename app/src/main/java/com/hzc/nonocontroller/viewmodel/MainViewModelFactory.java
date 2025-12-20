package com.hzc.nonocontroller.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.app.Application; // Import Application

import com.hzc.nonocontroller.ble.NonoBleManager;

public class MainViewModelFactory implements ViewModelProvider.Factory {

    private final NonoBleManager nonoBleManager;
    private final Application application;

    public MainViewModelFactory(Application application, NonoBleManager nonoBleManager) {
        this.application = application;
        this.nonoBleManager = nonoBleManager;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(application, nonoBleManager);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}