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
import com.hmdm.launcher.server.ServerServiceImpl;
import com.hmdm.launcher.ui.Admin.adapter.ConfigurationListAdapter;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationListFragment extends Fragment implements ConfigurationListAdapter.OnConfigurationClickListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;

    private ConfigurationListAdapter adapter;
    private List<Configuration> configurations = new ArrayList<>();

    private ServerApi serverService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configuration_list, container, false);

        serverService = new ServerServiceImpl(requireContext());

        initViews(view);
        setupRecyclerView();

        loadConfigurations();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        swipeRefreshLayout.setOnRefreshListener(this::loadConfigurations);
    }

    private void setupRecyclerView() {
        adapter = new ConfigurationListAdapter(configurations, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadConfigurations() {
        showProgress(true);

        serverService.getConfigurations(configList -> {
            configurations.clear();
            configurations.addAll(configList);

            requireActivity().runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                showProgress(false);
                updateEmptyView();
            });
        }, error -> {
            requireActivity().runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(requireContext(), "Erreur lors du chargement des configurations: " + error, Toast.LENGTH_LONG).show();
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
        if (configurations.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConfigurationClick(Configuration configuration) {
        // Action à effectuer lors du clic sur une configuration
        // Par exemple, ouvrir un écran de détails ou d'édition
        Toast.makeText(requireContext(), "Configuration cliquée: " + configuration.getName(), Toast.LENGTH_SHORT).show();
    }

    // Classe interne pour représenter une configuration
    public static class Configuration {
        private String id;
        private String name;
        private String description;

        public Configuration() {
        }
        // Nouveau constructeur pour id et name
        public Configuration(String id, String name) {
            this.id = id;
            this.name = name;
            this.description = null; // Ou "" si une chaîne vide est préférable
        }
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
