package com.hmdm.launcher.ui.Admin;

import android.os.Bundle;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hmdm.launcher.R;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerService;
import com.hmdm.launcher.server.ServerServiceImpl;
import com.hmdm.launcher.ui.Admin.adapter.LogListAdapter;

import java.util.ArrayList;
import java.util.List;

public class LogsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;

    private LogListAdapter adapter;
   // private List<LogEntry> logs = new ArrayList<>();

    private ServerApi serverService;
    private String deviceNumber; // À définir lors de la création du fragment

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logs, container, false);

        serverService = new ServerServiceImpl(requireContext());
/*
        // Récupérer le numéro de l'appareil depuis les arguments
        if (getArguments() != null) {
            deviceNumber = getArguments().getString("device_number");
        }

        initViews(view);
        setupRecyclerView();

        loadLogs();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        swipeRefreshLayout.setOnRefreshListener(this::loadLogs);
    }

    private void setupRecyclerView() {
        adapter = new LogListAdapter(logs);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadLogs() {
        showProgress(true);

        // Utiliser deviceNumber si on veut les logs d'un appareil spécifique
        // Sinon, passer null pour les logs généraux du serveur
        serverService.getLogs(deviceNumber, logList -> {
            logs.clear();
            logs.addAll(logList);

            requireActivity().runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                showProgress(false);
                updateEmptyView();
            });
        }, error -> {
            requireActivity().runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(requireContext(), "Erreur lors du chargement des logs: " + error, Toast.LENGTH_LONG).show();
                updateEmptyView();
            });
        });
    }

    private void showProgress(boolean show) {
        if (swipeRefreshLayout.isRefreshing()) {
            if (!show) {
                swipeRefreshLayout.setRefreshing(false);
            }
        } else {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void updateEmptyView() {
        if (logs.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Méthode statique pour créer une instance du fragment avec le numéro d'appareil (optionnel)
    public static LogsFragment newInstance(@Nullable String deviceNumber) {
        LogsFragment fragment = new LogsFragment();
        Bundle args = new Bundle();
        args.putString("device_number", deviceNumber);
        fragment.setArguments(args);
        return fragment;
    }

    // Classe interne pour représenter une entrée de log
    public static class LogEntry {
        private long timestamp;
        private String level;
        private String message;

        public LogEntry() {
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }*/
        return view;

    }
}
