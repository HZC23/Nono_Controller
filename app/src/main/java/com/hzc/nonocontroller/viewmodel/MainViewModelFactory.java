package com.hzc.nonocontroller.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.app.Application; // Import Application

import com.hzc.nonocontroller.BlunoLibrary;

public class MainViewModelFactory implements ViewModelProvider.Factory {

    private final BlunoLibrary blunoLibrary;
    private final Application application; // Add Application instance

    public MainViewModelFactory(Application application, BlunoLibrary blunoLibrary) {
        this.application = application;
        this.blunoLibrary = blunoLibrary;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(MainViewModel.class)) {
            return (T) new MainViewModel(application, blunoLibrary); // Pass application
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}