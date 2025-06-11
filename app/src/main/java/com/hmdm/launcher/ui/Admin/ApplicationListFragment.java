package com.hmdm.launcher.ui.Admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private Button btnUninstallApp, btnInstallApp;
    private View view; // Initialisée dans onCreateView

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
        try {
            view = inflater.inflate(R.layout.fragment_application_list, container, false);
            Log.d(TAG, "View inflatée avec succès");

            // Initialisation des vues
            recyclerView = view.findViewById(R.id.recycler_view_applications);
            progressBar = view.findViewById(R.id.progress_bar);
            emptyView = view.findViewById(R.id.empty_view);
            swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
            btnUninstallApp = view.findViewById(R.id.btn_uninstall_app);
            btnInstallApp = view.findViewById(R.id.btn_install_app);

            // Vérification des vues
            if (recyclerView == null) Log.e(TAG, "recyclerView est null");
            if (progressBar == null) Log.e(TAG, "progressBar est null");
            if (emptyView == null) Log.e(TAG, "emptyView est null");
            if (swipeRefreshLayout == null) Log.e(TAG, "swipeRefreshLayout est null");
            if (btnUninstallApp == null) Log.e(TAG, "btnUninstallApp est null");
            if (btnInstallApp == null) Log.e(TAG, "btnInstallApp est null");

            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                adapter = new ApplicationAdapter(new ArrayList<>());
                recyclerView.setAdapter(adapter);
                Log.d(TAG, "RecyclerView configuré avec adapter");
            } else {
                Log.e(TAG, "Impossible de configurer RecyclerView car il est null");
            }

            settingsHelper = SettingsHelper.getInstance(getContext());
            serverService = new ServerServiceImpl(requireContext());
            if (settingsHelper == null) Log.e(TAG, "settingsHelper est null");
            if (serverService == null) Log.e(TAG, "serverService est null");

            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setOnRefreshListener(this::fetchApplications);
                Log.d(TAG, "SwipeRefreshLayout configuré");
            } else {
                Log.e(TAG, "SwipeRefreshLayout est null, onRefreshListener non configuré");
            }

            setupSearchView();
            setupButtons();
            fetchApplications();

            return view;
        } catch (Exception e) {
            Log.e(TAG, "Erreur dans onCreateView: ", e);
            Toast.makeText(getContext(), "Erreur lors du chargement du fragment: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return null; // Retourne null en cas d'erreur pour éviter un crash
        }
    }

    private void fetchApplications() {
        if (getContext() == null) {
            Log.e(TAG, "Contexte null, impossible de charger les applications");
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        else Log.e(TAG, "progressBar est null, impossible de montrer la progression");
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        else Log.e(TAG, "recyclerView est null, impossible de masquer");
        if (emptyView != null) emptyView.setVisibility(View.GONE);
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        serverService.getApplications(
                applications -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                    if (applications == null) {
                        Log.e(TAG, "Liste d'applications retournée est null");
                        return;
                    }

                    if (applications.isEmpty()) {
                        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                        if (emptyView != null) {
                            emptyView.setVisibility(View.VISIBLE);
                            emptyView.setText("Aucune application trouvée");
                        }
                    } else {
                        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                        if (emptyView != null) emptyView.setVisibility(View.GONE);
                        if (adapter != null) adapter.updateApplications(applications);
                        else Log.e(TAG, "adapter est null, impossible de mettre à jour les applications");
                    }

                    Toast.makeText(getContext(), "Applications chargées", Toast.LENGTH_SHORT).show();
                }),
                error -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                    if (emptyView != null) emptyView.setVisibility(View.GONE);
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur lors du chargement des applications : " + error);
                })
        );
    }

    private void setupSearchView() {
        if (view == null) {
            Log.e(TAG, "view est null dans setupSearchView");
            return;
        }
        androidx.appcompat.widget.SearchView searchView = view.findViewById(R.id.search_view);
        if (searchView != null) {
            searchView.setQueryHint("Search by Name");
            searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    Log.d(TAG, "Recherche soumise: " + query);
                    searchApplications(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    Log.d(TAG, "Recherche modifiée: " + newText);
                    if (newText.isEmpty()) {
                        fetchApplications();
                    } else {
                        searchApplications(newText);
                    }
                    return true;
                }
            });
        } else {
            Log.e(TAG, "searchView est null dans setupSearchView");
        }
    }

    private void searchApplications(String query) {
        if (getContext() == null) {
            Log.e(TAG, "Contexte null, impossible de rechercher les applications");
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyView != null) emptyView.setVisibility(View.GONE);

        String url = settingsHelper.getBaseUrl() + "/rest/private/applications/search/" + query;
        serverService.searchApplicationsByName(url,
                applications -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    if (applications == null) {
                        Log.e(TAG, "Liste d'applications retournée est null pour la recherche");
                        return;
                    }

                    if (applications.isEmpty()) {
                        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                        if (emptyView != null) {
                            emptyView.setVisibility(View.VISIBLE);
                            emptyView.setText("Aucune application trouvée pour '" + query + "'");
                        }
                    } else {
                        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                        if (emptyView != null) emptyView.setVisibility(View.GONE);
                        if (adapter != null) adapter.updateApplications(applications);
                        else Log.e(TAG, "adapter est null, impossible de mettre à jour les applications");
                    }
                }),
                error -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                    if (emptyView != null) emptyView.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Erreur de recherche : " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur lors de la recherche des applications : " + error);
                })
        );
    }

    private void setupButtons() {
        if (btnInstallApp != null) {
            btnInstallApp.setOnClickListener(v -> {
                Log.d(TAG, "Bouton INSTALL APP cliqué");
                Fragment fragment = new AddApplicationFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });
        } else {
            Log.e(TAG, "btnInstallApp est null dans setupButtons");
        }

        if (btnUninstallApp != null) {
            btnUninstallApp.setOnClickListener(v -> {
                Log.d(TAG, "Bouton UNINSTALL APP cliqué");
                Fragment fragment = new DeleteAppFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });
        } else {
            Log.e(TAG, "btnUninstallApp est null dans setupButtons");
        }
    }
}