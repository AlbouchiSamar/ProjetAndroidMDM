package com.hmdm.launcher.ui.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;
import com.hmdm.launcher.ui.Admin.adapter.DeviceListAdapter;
import java.util.ArrayList;
import java.util.List;

public class DeviceListFragment extends Fragment implements DeviceListAdapter.OnDeviceClickListener {
    private static final String TAG = "DeviceListFragment";
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;
    private SearchView searchView;
    private DeviceListAdapter adapter;
    private List<Device> allDevices = new ArrayList<>();
    private List<Device> devices = new ArrayList<>();
    private ServerApi serverService;
    private SettingsHelper settingsHelper;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView appelé");
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);
        serverService = new ServerServiceImpl(requireContext());
        settingsHelper = SettingsHelper.getInstance(requireContext());
        initViews(view);
        setupRecyclerView();
        setupSearchView();
        loadDevices();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        searchView = view.findViewById(R.id.search_view);
        Log.d(TAG, "Vues initialisées: searchView=" + (searchView != null) + ", recyclerView=" + (recyclerView != null) + ", progressBar=" + (progressBar != null));
        swipeRefreshLayout.setOnRefreshListener(this::loadDevices);
    }

    private void setupRecyclerView() {
        adapter = new DeviceListAdapter(devices, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "RecyclerView initialisé: Adapter=" + (adapter != null) + ", LayoutManager=" + (recyclerView.getLayoutManager() != null));
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Recherche soumise: " + query);
                filterDevices(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "Recherche modifiée: " + newText);
                filterDevices(newText);
                return true;
            }
        });
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            Log.d(TAG, "SearchView focus changé: " + hasFocus);
        });
        searchView.requestFocus();
    }

    private void loadDevices() {
        if (isLoading) return;
        isLoading = true;
        showProgress(true);
        serverService.getDevices(
                deviceList -> {
                    requireActivity().runOnUiThread(() -> {
                        allDevices.clear();
                        allDevices.addAll(deviceList);
                        Log.d(TAG, "Appareils chargés: " + allDevices.size());
                        for (Device device : allDevices) {
                            Log.d(TAG, "Appareil: ID=" + device.getId() + ", Number=" + device.getNumber() + ", Name=" + device.getName());
                        }
                        adapter.updateDevices(allDevices);
                        showProgress(false);
                        updateEmptyView();
                        isLoading = false;
                    });
                },
                error -> {
                    requireActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(requireContext(), "Erreur lors du chargement des appareils: " + error, Toast.LENGTH_LONG).show();
                        updateEmptyView();
                        isLoading = false;
                    });
                }
        );
    }

    private void filterDevices(String query) {
        Log.d(TAG, "Filtrage avec query: " + query);
        List<Device> filteredList = new ArrayList<>();
        for (Device device : allDevices) {
            String id = String.valueOf(device.getId());
            if (TextUtils.isEmpty(query) || id.contains(query)) {
                filteredList.add(device);
                Log.d(TAG, "Appareil inclus: ID=" + device.getId() + ", Number=" + device.getNumber() + ", Name=" + device.getName());
            }
        }
        Log.d(TAG, "Résultats filtrés: " + filteredList.size());
        adapter.updateDevices(filteredList);
        updateEmptyView();
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
        if (devices.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDeviceClick(Device device) {
        if (!isAdded() || getActivity() == null) {
            Log.w(TAG, "Fragment détaché, dialogue ignoré");
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Options pour " + device.getName())
                .setItems(new String[]{"Modifier", "Supprimer"}, (dialog, which) -> {
                    if (which == 0) {
                        // Modifier: Naviguer vers ModifyDeviceFragment
                        Fragment fragment = new ModifyDeviceFragment();
                        Bundle args = new Bundle();
                        args.putInt("deviceId", device.getId());
                        fragment.setArguments(args);
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();
                        Toast.makeText(requireContext(), "Navigation vers la modification de l'appareil ID: " + device.getId(), Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        // Supprimer: Afficher la boîte de dialogue de confirmation
                        new AlertDialog.Builder(requireContext())
                                .setTitle("Confirmer la suppression")
                                .setMessage("Voulez-vous vraiment supprimer " + device.getName() + " (ID: " + device.getId() + ") ?")
                                .setPositiveButton("Oui", (dialog1, which1) -> {
                                    progressBar.setVisibility(View.VISIBLE);
                                    serverService.deleteDevice(
                                            String.valueOf(device.getId()),
                                            settingsHelper.getAdminAuthToken(),
                                            message -> {
                                                requireActivity().runOnUiThread(() -> {
                                                    progressBar.setVisibility(View.GONE);
                                                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                                                    Log.d(TAG, "Suppression réussie: " + message);
                                                    loadDevices();
                                                });
                                            },
                                            error -> {
                                                requireActivity().runOnUiThread(() -> {
                                                    progressBar.setVisibility(View.GONE);
                                                    Toast.makeText(requireContext(), "Erreur de suppression: " + error, Toast.LENGTH_LONG).show();
                                                    Log.e(TAG, "Erreur de suppression: " + error);
                                                });
                                            }
                                    );
                                })
                                .setNegativeButton("Non", null)
                                .show();
                    }
                })
                .show();
    }

    public static class Device {
        private int id;
        private String number;
        private String name;
        private String status;
        private String lastOnline;
        private String model;
        private int configurationId;

        public Device() {
        }

        public Device(int id, String number, String name, String status, String lastOnline, String model, int configurationId) {
            this.id = id;
            this.number = number;
            this.name = name;
            this.status = status;
            this.lastOnline = lastOnline;
            this.model = model;
            this.configurationId = configurationId;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLastOnline() { return lastOnline; }
        public void setLastOnline(String lastOnline) { this.lastOnline = lastOnline; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getConfigurationId() { return configurationId; }
        public void setConfigurationId(int configurationId) { this.configurationId = configurationId; }
    }
}