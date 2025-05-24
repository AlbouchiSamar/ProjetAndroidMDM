package com.hmdm.launcher.ui.Admin;

import android.content.Intent;
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
import com.hmdm.launcher.ui.Admin.adapter.DeviceListAdapter;

import java.util.ArrayList;
import java.util.List;

public class DeviceListFragment extends Fragment implements DeviceListAdapter.OnDeviceClickListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;

    private DeviceListAdapter adapter;
    private List<Device> devices = new ArrayList<>();

    private ServerApi serverService;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);

        serverService = new ServerServiceImpl(requireContext());

        initViews(view);
        setupRecyclerView();

        loadDevices();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        swipeRefreshLayout.setOnRefreshListener(this::loadDevices);
    }

    private void setupRecyclerView() {
        adapter = new DeviceListAdapter(devices, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }


    private void loadDevices() {
        if (isLoading) return; // Ignorer si un chargement est en cours
        isLoading = true;
        showProgress(true);

        serverService.getDevices(deviceList -> {
            requireActivity().runOnUiThread(() -> {
                adapter.updateDevices(deviceList); // Utiliser la méthode de l'adaptateur
                showProgress(false);
                updateEmptyView();
                isLoading = false;
            });
        }, error -> {
            requireActivity().runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(requireContext(), "Erreur lors du chargement des appareils: " + error, Toast.LENGTH_LONG).show();
                updateEmptyView();
                isLoading = false;
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
        // Ouvrir l'écran de détails de l'appareil
        Intent intent = new Intent(requireContext(), LogsFragment.class);
        intent.putExtra("device_number", device.getNumber());
        startActivity(intent);
    }



    // Classe interne pour représenter un appareil
    // Classe interne pour représenter un appareil
    public static class Device {
        private int id;
        private String number;
        private String name;
        private String status;
        private String lastOnline;
        private String model;

        public Device() {
        }

        public Device(int id, String number, String name, String status, String lastOnline, String model) {
            this.id = id;
            this.number = number;
            this.name = name;
            this.status = status;
            this.lastOnline = lastOnline;
            this.model = model;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getLastOnline() {
            return lastOnline;
        }

        public void setLastOnline(String lastOnline) {
            this.lastOnline = lastOnline;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

}
