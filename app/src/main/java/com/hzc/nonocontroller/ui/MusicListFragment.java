package com.hzc.nonocontroller.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hzc.nonocontroller.R;
import com.hzc.nonocontroller.viewmodel.MainViewModel;
import com.hzc.nonocontroller.Constants;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MusicListFragment extends Fragment implements MusicListAdapter.OnMusicClickListener {

    private MainViewModel viewModel;
    private MusicListAdapter adapter;
    private RecyclerView recyclerView;
    private Button listMusicButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_music_list, container, false);

        listMusicButton = root.findViewById(R.id.button_list_music);
        recyclerView = root.findViewById(R.id.recycler_view_music_files);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MusicListAdapter(new LinkedList<>(), this); // Pass 'this' as the listener
        recyclerView.setAdapter(adapter);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        listMusicButton.setOnClickListener(v -> {
            viewModel.clearMusicFiles(); // Clear previous list
            viewModel.sendCommand(Constants.buildCommand(Constants.ACTION_LIST_MUSIC));
        });

        viewModel.musicFiles.observe(getViewLifecycleOwner(), musicFiles -> {
            if (musicFiles != null) {
                adapter.setMusicFiles(musicFiles);
            }
        });
    }

    @Override
    public void onMusicClick(String filename) {
        // Send command to play the selected music file
        viewModel.sendCommand(Constants.buildCommand(Constants.ACTION_PLAY_MUSIC, filename));
    }
}
