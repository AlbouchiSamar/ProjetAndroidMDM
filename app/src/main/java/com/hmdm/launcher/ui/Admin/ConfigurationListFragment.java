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
import com.hmdm.launcher.ui.Admin.adapter.ConfigurationListAdapter;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationListFragment extends Fragment {
    private static final String TAG = "ConfigurationListFragment";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private ServerApi serverService;
    private ConfigurationListAdapter adapter;

    // Classe Configuration interne
    public static class Configuration {
        private int id;
        private String name;
        private String password;
        private String description;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getPassword() { return password; }
        public String getDescription() { return description; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setPassword(String password) { this.password = password; }
        public void setDescription(String description) { this.description = description; }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverService = new ServerServiceImpl(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configuration_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_configurations);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConfigurationListAdapter(new ArrayList<>(), this::onConfigurationClicked);
        recyclerView.setAdapter(adapter);

        fetchConfigurations();

        return view;
    }

    private void onConfigurationClicked(ConfigurationListFragment.Configuration configuration) {
        // Gérer le clic sur une configuration ici
        Toast.makeText(getContext(), "Configuration cliquée: " + configuration.getName(), Toast.LENGTH_SHORT).show();
    }

    private void fetchConfigurations() {
        if (getContext() == null) {
            Log.e(TAG, "Contexte null, impossible de charger les configurations");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        serverService.getConfigurations(
                configurations -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    progressBar.setVisibility(View.GONE);
                    if (configurations.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("Aucune configuration trouvée");
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                        adapter.updateConfigurations(configurations);
                    }
                    Toast.makeText(getContext(), "Configurations chargées", Toast.LENGTH_SHORT).show();
                }),
                error -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur lors du chargement des configurations : " + error);
                })
        );
    }
}