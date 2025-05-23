package com.hmdm.launcher.ui.Admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;
import com.hmdm.launcher.ui.Admin.DeviceListFragment;
import com.hmdm.launcher.ui.Admin.adapter.DeviceListAdapter;

import java.util.ArrayList;
import java.util.List;

public class DeviceDeleteFragment extends Fragment {
    private static final String TAG = "DeviceDeleteFragment";
    private SearchView searchView;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private DeviceListAdapter adapter;
    private List<DeviceListFragment.Device> allDevices;
    private ServerApi serverService;
    private SettingsHelper settingsHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_delete, container, false);

        searchView = view.findViewById(R.id.search_view);
        recyclerView = view.findViewById(R.id.recycler_view_devices);
        progressBar = view.findViewById(R.id.progress_bar);

        settingsHelper = SettingsHelper.getInstance(getContext());
        serverService = new ServerServiceImpl(requireContext());
        allDevices = new ArrayList<>();

        // Configurer le RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DeviceListAdapter(allDevices, this::onDeviceSelected);
        recyclerView.setAdapter(adapter);

        // Configurer la barre de recherche
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterDevices(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterDevices(newText);
                return true;
            }
        });

        // Charger les appareils
        loadDevices();

        return view;
    }

    private void loadDevices() {
        progressBar.setVisibility(View.VISIBLE);
        serverService.getDevices(
                devices -> {
                    allDevices.clear();
                    allDevices.addAll(devices);
                    adapter.updateDevices(allDevices);
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur lors du chargement des appareils : " + error);
                }
        );
    }

    private void filterDevices(String query) {
        List<DeviceListFragment.Device> filteredList = new ArrayList<>();
        for (DeviceListFragment.Device device : allDevices) {
            if (TextUtils.isEmpty(query) || device.getNumber().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(device);
            }
        }
        adapter.updateDevices(filteredList);
    }

    private void onDeviceSelected(DeviceListFragment.Device device) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirmer la suppression")
                .setMessage("Voulez-vous vraiment supprimer " + device.getName() + " (ID: " + device.getId() + ") ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    serverService.deleteDevice(
                            String.valueOf(device.getId()),
                            settingsHelper.getAdminAuthToken(),
                            message -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                                Log.d(TAG, "Suppression réussie : " + message);
                                loadDevices(); // Recharger la liste
                            },
                            error -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Erreur de suppression : " + error);
                            }
                    );
                })
                .setNegativeButton("Non", null)
                .show();
    }
}