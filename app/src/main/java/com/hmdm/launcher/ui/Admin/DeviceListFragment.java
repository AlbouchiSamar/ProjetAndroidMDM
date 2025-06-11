package com.hmdm.launcher.ui.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private Button btnAddDevice;
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
        setupAddDeviceButton();
        loadDevices();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        searchView = view.findViewById(R.id.search_view);
        btnAddDevice = view.findViewById(R.id.btn_add_device);
        Log.d(TAG, "Vues initialisées: searchView=" + (searchView != null) + ", recyclerView=" + (recyclerView != null) + ", progressBar=" + (progressBar != null) + ", btnAddDevice=" + (btnAddDevice != null));
        swipeRefreshLayout.setOnRefreshListener(this::loadDevices);
    }

    private void setupRecyclerView() {
        adapter = new DeviceListAdapter(devices, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "RecyclerView initialisé: Adapter=" + (adapter != null) + ", LayoutManager=" + (recyclerView.getLayoutManager() != null));
    }

    private void setupSearchView() {
        searchView.setQueryHint("Appareil à rechercher"); // Définir le hint initial
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
            if (hasFocus) {
                searchView.setQuery("", false); // Efface le texte si focus obtenu
                searchView.setQueryHint("Appareil à rechercher"); // Réassure le hint
            }
        });
        searchView.requestFocus();
    }

    private void setupAddDeviceButton() {
        btnAddDevice.setOnClickListener(v -> {
            Log.d(TAG, "Bouton Add Device cliqué");
            Fragment fragment = new AddDeviceFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
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
                        Toast.makeText(requireContext(), "Error loading devices: " + error, Toast.LENGTH_LONG).show();
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
            String number = device.getNumber() != null ? device.getNumber() : "";
            if (TextUtils.isEmpty(query) || id.contains(query) || number.contains(query)) {
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
            Log.w(TAG, "Fragment detached, dialog ignored");
            return;
        }

        // Initial dialog for options
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_alert, null);
        TextView title = dialogView.findViewById(R.id.alert_title);
        TextView message = dialogView.findViewById(R.id.alert_message);
        Button btnNegative = dialogView.findViewById(R.id.btn_negative);
        Button btnPositive = dialogView.findViewById(R.id.btn_positive);

        title.setText("Options for " + device.getName());
        message.setText("Choose an action:");
        btnNegative.setOnClickListener(v -> ((AlertDialog) btnNegative.getTag()).dismiss());
        btnPositive.setVisibility(View.GONE); // Hide positive button initially

        AlertDialog optionsDialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        btnNegative.setTag(optionsDialog);

        // Custom list items
        String[] options = {"Modify", "Delete"};
        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    optionsDialog.dismiss();
                    if (which == 0) {
                        // Modify: Navigate to ModifyDeviceFragment with device data
                        Fragment fragment = new ModifyDeviceFragment();
                        Bundle args = new Bundle();
                        args.putInt("deviceId", device.getId());
                        args.putString("deviceName", device.getName());
                        args.putString("deviceNumber", device.getNumber());
                        args.putInt("configurationId", device.getConfigurationId());
                        fragment.setArguments(args);
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();
                        Toast.makeText(requireContext(), "Navigating to modify device ID: " + device.getId(), Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        // Delete: Show confirmation dialog
                        View confirmDialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_alert, null);
                        TextView confirmTitle = confirmDialogView.findViewById(R.id.alert_title);
                        TextView confirmMessage = confirmDialogView.findViewById(R.id.alert_message);
                        Button confirmNegative = confirmDialogView.findViewById(R.id.btn_negative);
                        Button confirmPositive = confirmDialogView.findViewById(R.id.btn_positive);

                        confirmTitle.setText("Confirm Deletion");
                        confirmMessage.setText("Are you sure you want to delete " + device.getName() + " (ID: " + device.getId() + ")?");
                        confirmPositive.setVisibility(View.VISIBLE);
                        confirmPositive.setText("Yes");
                        confirmPositive.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_light));
                        confirmPositive.setOnClickListener(v -> {
                            progressBar.setVisibility(View.VISIBLE);
                            serverService.deleteDevice(
                                    String.valueOf(device.getId()),
                                    settingsHelper.getAdminAuthToken(),
                                    successMessage -> {
                                        requireActivity().runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(requireContext(), successMessage, Toast.LENGTH_LONG).show();
                                            Log.d(TAG, "Deletion successful: " + successMessage);
                                            loadDevices();
                                        });
                                    },
                                    error -> {
                                        requireActivity().runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(requireContext(), "Deletion error: " + error, Toast.LENGTH_LONG).show();
                                            Log.e(TAG, "Deletion error: " + error);
                                        });
                                    }
                            );
                            ((AlertDialog) confirmNegative.getTag()).dismiss();
                        });

                        AlertDialog confirmDialog = new AlertDialog.Builder(requireContext())
                                .setView(confirmDialogView)
                                .create();
                        confirmNegative.setTag(confirmDialog);
                        confirmNegative.setText("No");
                        confirmNegative.setOnClickListener(v -> confirmDialog.dismiss());
                        confirmDialog.show();
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
        private List<Group> groups = new ArrayList<>();

        public Device() {}

        public Device(int id, String number, String name, String status, String lastOnline, String model, int configurationId, List<Group> groups) {
            this.id = id;
            this.number = number;
            this.name = name;
            this.status = status;
            this.lastOnline = lastOnline;
            this.model = model;
            this.configurationId = configurationId;
            this.groups = groups != null ? groups : new ArrayList<>();
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
        public List<Group> getGroups() { return groups; }
        public void setGroups(List<Group> groups) { this.groups = groups != null ? groups : new ArrayList<>(); }

        public static class Group {
            private int id;
            private String name;

            public Group(int id, String name) {
                this.id = id;
                this.name = name;
            }

            public int getId() { return id; }
            public String getName() { return name; }
        }
    }
}