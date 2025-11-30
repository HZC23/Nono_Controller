package com.hzc.nonocontroller.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hzc.nonocontroller.R;

import java.util.LinkedList;
import java.util.List;

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.MusicViewHolder> {

    public interface OnMusicClickListener {
        void onMusicClick(String filename);
    }

    private LinkedList<String> musicFiles;
    private OnMusicClickListener listener;

    public MusicListAdapter(LinkedList<String> musicFiles, OnMusicClickListener listener) {
        this.musicFiles = musicFiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_list_item, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        String filename = musicFiles.get(position);
        holder.bind(filename);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMusicClick(filename);
            }
        });
    }

    @Override
    public int getItemCount() {
        return musicFiles.size();
    }

    public void setMusicFiles(LinkedList<String> newMusicFiles) {
        this.musicFiles.clear();
        this.musicFiles.addAll(newMusicFiles);
        notifyDataSetChanged();
    }

    static class MusicViewHolder extends RecyclerView.ViewHolder {
        TextView musicFileNameTextView;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            musicFileNameTextView = itemView.findViewById(R.id.text_music_file_name);
        }

        public void bind(String filename) {
            musicFileNameTextView.setText(filename);
        }
    }
}
