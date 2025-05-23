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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;
import com.hmdm.launcher.ui.Admin.adapter.ApplicationAdapter;

import java.util.ArrayList;

public class ApplicationListFragment extends Fragment {
    private static final String TAG = "ApplicationListFragment";
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ApplicationAdapter adapter;
    private ServerApi serverService;
    private SettingsHelper settingsHelper;

    // Classe interne rendue statique
    public static class Application {
        private int id;
        private String name;
        private String pkg;

        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public String getPkg() { return pkg; }

        // Setters
        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setPkg(String pkg) { this.pkg = pkg; }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_application_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_applications);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ApplicationAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        settingsHelper = SettingsHelper.getInstance(getContext());
        serverService = new ServerServiceImpl(requireContext());

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::fetchApplications);
        }

        fetchApplications();

        return view;
    }

    private void fetchApplications() {
        if (getContext() == null) {
            Log.e(TAG, "Contexte null, impossible de charger les applications");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        if (emptyView != null) emptyView.setVisibility(View.GONE);
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        serverService.getApplications(
                applications -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    if (applications.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        if (emptyView != null) {
                            emptyView.setVisibility(View.VISIBLE);
                            emptyView.setText("Aucune application trouvée");
                        }
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        if (emptyView != null) emptyView.setVisibility(View.GONE);
                        adapter.updateApplications(applications);
                    }

                    Toast.makeText(getContext(), "Applications chargées", Toast.LENGTH_SHORT).show();
                }),
                error -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    recyclerView.setVisibility(View.VISIBLE);
                    if (emptyView != null) emptyView.setVisibility(View.GONE);
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur lors du chargement des applications : " + error);
                })
        );
    }
}