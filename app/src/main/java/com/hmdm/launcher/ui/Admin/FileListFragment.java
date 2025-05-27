package com.hmdm.launcher.ui.Admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;
import java.util.ArrayList;
import java.util.List;

public class FileListFragment extends Fragment {
    private static final String TAG = "FileListFragment";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private FileAdapter adapter;
    private ServerApi serverService;
    private SettingsHelper settingsHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsHelper = SettingsHelper.getInstance(requireContext());
        serverService = new ServerServiceImpl(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView appelé");
        View view = inflater.inflate(R.layout.fragment_file_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_files);
        progressBar = view.findViewById(R.id.progress_bar);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FileAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        progressBar.setVisibility(View.VISIBLE);
        loadFiles();

        return view;
    }

    private void loadFiles() {
        serverService.getFiles(
                files -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, callback ignoré");
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        adapter.updateFiles(files);
                        progressBar.setVisibility(View.GONE);
                        Log.d(TAG, "Fichiers récupérés: " + files.size());
                    });
                },
                error -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, callback ignoré");
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Erreur: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Erreur récupération fichiers: " + error);
                    });
                }
        );
    }

    private static class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {
        private List<FileItem> files;

        FileAdapter(List<FileItem> files) {
            this.files = files;
        }

        void updateFiles(List<FileItem> newFiles) {
            this.files = newFiles;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
            FileItem file = files.get(position);
            holder.textFileName.setText("Nom: " + file.getName());
            holder.textFileUrl.setText("URL: " + file.getUrl());
            holder.textFileSize.setText("Taille: " + file.getSize() + " octets");
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        static class FileViewHolder extends RecyclerView.ViewHolder {
            TextView textFileName;
            TextView textFileUrl;
            TextView textFileSize;

            FileViewHolder(@NonNull View itemView) {
                super(itemView);
                textFileName = itemView.findViewById(R.id.text_file_name);
                textFileUrl = itemView.findViewById(R.id.text_file_url);
                textFileSize = itemView.findViewById(R.id.text_file_size);
            }
        }
    }

    public static class FileItem {
        private String name;
        private String url;
        private long size;

        public FileItem(String name, String url, long size) {
            this.name = name;
            this.url = url;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public long getSize() {
            return size;
        }
    }
}