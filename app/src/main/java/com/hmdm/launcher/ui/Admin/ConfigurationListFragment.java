package com.hmdm.launcher.ui.Admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;
import com.hmdm.launcher.ui.Admin.adapter.ConfigurationListAdapter;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationListFragment extends Fragment {
    private static final String TAG = "ConfigurationListFragment";

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private ServerApi serverService;
    private ConfigurationListAdapter adapter;

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
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConfigurationListAdapter(new ArrayList<>(), this::onConfigurationClicked);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Rafraîchissement déclenché");
            fetchConfigurations();
        });

        fetchConfigurations();

        return view;
    }

    private void onConfigurationClicked(ConfigurationListFragment.Configuration configuration) {
        new AlertDialog.Builder(getContext())
                .setTitle("Options pour " + configuration.getName())
                .setItems(new String[]{"Copier", "Supprimer"}, (dialog, which) -> {
                    if (which == 0) {
                        promptCopyConfiguration(configuration);
                    } else if (which == 1) {
                        confirmDeleteConfiguration(configuration);
                    }
                })
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void promptCopyConfiguration(ConfigurationListFragment.Configuration configuration) {
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nouveau nom de la configuration");

        new AlertDialog.Builder(getContext())
                .setTitle("Copier la configuration")
                .setMessage("Entrez le nouveau nom pour la copie de " + configuration.getName())
                .setView(input)
                .setPositiveButton("Copier", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(getContext(), "Le nom ne peut pas être vide", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    copyConfiguration(configuration, newName);
                })
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void confirmDeleteConfiguration(ConfigurationListFragment.Configuration configuration) {
        new AlertDialog.Builder(getContext())
                .setTitle("Supprimer la configuration")
                .setMessage("Êtes-vous sûr de vouloir supprimer " + configuration.getName() + " ?")
                .setPositiveButton("Oui", (dialog, which) -> deleteConfiguration(configuration))
                .setNegativeButton("Non", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void copyConfiguration(ConfigurationListFragment.Configuration configuration, String newName) {
        progressBar.setVisibility(View.VISIBLE);
        serverService.copyConfiguration(configuration.getId(), newName, new ServerApi.CopyConfigurationCallback() {
            @Override
            public void onSuccess(String message) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Configuration copiée avec succès", Toast.LENGTH_SHORT).show();
                    fetchConfigurations();
                });
            }

            @Override
            public void onError(String error) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Erreur lors de la copie : " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur lors de la copie : " + error);
                });
            }
        });
    }

    private void deleteConfiguration(ConfigurationListFragment.Configuration configuration) {
        progressBar.setVisibility(View.VISIBLE);
        serverService.deleteConfiguration(configuration.getId(), new ServerApi.DeleteConfigurationCallback() {
            @Override
            public void onSuccess(String message) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Configuration supprimée avec succès", Toast.LENGTH_SHORT).show();
                    fetchConfigurations();
                });
            }

            @Override
            public void onError(String error) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Erreur lors de la suppression : " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur lors de la suppression : " + error);
                });
            }
        });
    }

    private void fetchConfigurations() {
        if (getContext() == null) {
            Log.e(TAG, "Contexte null, impossible de charger les configurations");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);

        serverService.getConfigurations(
                configurations -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
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
                    swipeRefreshLayout.setRefreshing(false);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur lors du chargement des configurations : " + error);
                })
        );
    }
}