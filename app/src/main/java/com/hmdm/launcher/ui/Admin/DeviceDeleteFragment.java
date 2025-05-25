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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DeviceListAdapter(allDevices, this::onDeviceSelected);
        recyclerView.setAdapter(adapter);

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

        loadDevices();
        return view;
    }

    private void loadDevices() {
        Log.d(TAG, "Chargement des appareils (test statique)");
        allDevices.clear();
        DeviceListFragment.Device device1 = new DeviceListFragment.Device();
        device1.setId(123);
        device1.setNumber("device-123");
        device1.setName("Device A");
        device1.setStatus("En ligne");
        device1.setModel("Model X");
        allDevices.add(device1);
        DeviceListFragment.Device device2 = new DeviceListFragment.Device();
        device2.setId(456);
        device2.setNumber("device-456");
        device2.setName("Device B");
        device2.setStatus("Hors ligne");
        device2.setModel("Model Y");
        allDevices.add(device2);
        Log.d(TAG, "Appareils chargés (test): " + allDevices.size());
        requireActivity().runOnUiThread(() -> {
            Log.d(TAG, "Mise à jour UI: Adapter=" + (adapter != null) + ", RecyclerView=" + (recyclerView != null));
            adapter.updateDevices(allDevices);
            Log.d(TAG, "Appareils envoyés à l'adapter: " + allDevices.size());
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE); // Forcer la visibilité
        });
    }
    private void filterDevices(String query) {
        Log.d(TAG, "Filtrage avec query: " + query);
        List<DeviceListFragment.Device> filteredList = new ArrayList<>();
        for (DeviceListFragment.Device device : allDevices) {
            String number = device.getNumber() != null ? device.getNumber().toLowerCase() : "";
            String name = device.getName() != null ? device.getName().toLowerCase() : "";
            String id = String.valueOf(device.getId());
            if (TextUtils.isEmpty(query) ||
                    number.contains(query.toLowerCase()) ||
                    name.contains(query.toLowerCase()) ||
                    id.contains(query)) {
                filteredList.add(device);
                Log.d(TAG, "Appareil inclus: ID=" + device.getId() + ", Number=" + device.getNumber() + ", Name=" + device.getName());
            }
        }
        Log.d(TAG, "Résultats filtrés: " + filteredList.size());
        adapter.updateDevices(filteredList);
    }

    private void onDeviceSelected(DeviceListFragment.Device device) {
        if (!isAdded() || getActivity() == null) {
            Log.w(TAG, "Fragment détaché, dialogue ignoré");
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Confirmer la suppression")
                .setMessage("Voulez-vous vraiment supprimer " + device.getName() + " (ID: " + device.getId() + ") ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, suppression ignorée");
                        return;
                    }
                    progressBar.setVisibility(View.VISIBLE);
                    serverService.deleteDevice(
                            String.valueOf(device.getId()),
                            settingsHelper.getAdminAuthToken(),
                            message -> {
                                if (!isAdded() || getActivity() == null) {
                                    Log.w(TAG, "Fragment détaché, callback ignoré");
                                    return;
                                }
                                requireActivity().runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                                    Log.d(TAG, "Suppression réussie: " + message);
                                    loadDevices();
                                });
                            },
                            error -> {
                                if (!isAdded() || getActivity() == null) {
                                    Log.w(TAG, "Fragment détaché, callback ignoré");
                                    return;
                                }
                                requireActivity().runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Erreur de suppression: " + error);
                                });
                            }
                    );
                })
                .setNegativeButton("Non", null)
                .show();
    }
}