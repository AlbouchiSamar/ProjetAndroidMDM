package com.hmdm.launcher.ui.Admin.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hmdm.launcher.R;
import com.hmdm.launcher.ui.Admin.DeviceListFragment;
import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    private List<DeviceListFragment.Device> devices;
    private OnDeviceClickListener listener;

    public DeviceListAdapter(List<DeviceListFragment.Device> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceListFragment.Device device = devices.get(position);
        holder.bind(device, listener);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void updateDevices(List<DeviceListFragment.Device> newDevices) {
        Log.d("DeviceListAdapter", "Mise à jour des appareils: " + newDevices.size());
        this.devices.clear();
        this.devices.addAll(newDevices);
        notifyDataSetChanged();
        Log.d("DeviceListAdapter", "notifyDataSetChanged appelé");
    }

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceListFragment.Device device);
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView deviceIdText;
        private TextView lastupdate;
        private TextView deviceNumberText;
        private TextView deviceNameText;
        private TextView statusText;
        private TextView configNameText;
        private TextView configurationIdText;
        private TextView groupsText; // Ajout pour afficher les groupes

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceIdText = itemView.findViewById(R.id.text_device_id);
            lastupdate = itemView.findViewById(R.id.text_lastupdate);
            deviceNumberText = itemView.findViewById(R.id.text_device_number);
            deviceNameText = itemView.findViewById(R.id.text_device_name);
            statusText = itemView.findViewById(R.id.text_status);
            configNameText = itemView.findViewById(R.id.text_config_name);
            configurationIdText = itemView.findViewById(R.id.text_configuration_id);
            groupsText = itemView.findViewById(R.id.text_groups); // Ajout de l'ID pour les groupes
        }

        public void bind(final DeviceListFragment.Device device, final OnDeviceClickListener listener) {
            deviceIdText.setText(device.getId() != -1 ? String.valueOf(device.getId()) : "Inconnu");
            lastupdate.setText(device.getLastOnline() != null ? device.getLastOnline() : "Inconnu");
            deviceNumberText.setText(device.getNumber() != null ? device.getNumber() : "Inconnu");
            deviceNameText.setText(device.getName() != null ? device.getName() : "Sans nom");
            statusText.setText(device.getStatus() != null ? device.getStatus() : "Inconnu");
            configNameText.setText(device.getModel() != null ? device.getModel() : "Inconnu");
            configurationIdText.setText(device.getConfigurationId() != -1 ? String.valueOf(device.getConfigurationId()) : "Inconnu");

            // Affichage des groupes
            List<DeviceListFragment.Device.Group> groups = device.getGroups();
            if (groups != null && !groups.isEmpty()) {
                StringBuilder groupsTextContent = new StringBuilder("Groupes : ");
                for (int i = 0; i < groups.size(); i++) {
                    groupsTextContent.append(groups.get(i).getName()).append(" (ID: ").append(groups.get(i).getId()).append(")");
                    if (i < groups.size() - 1) {
                        groupsTextContent.append(", ");
                    }
                }
                groupsText.setText(groupsTextContent.toString());
            } else {
                groupsText.setText("Groupes : Aucun groupe");
            }

            if ("En ligne".equals(device.getStatus())) {
                statusText.setTextColor(itemView.getContext().getResources().getColor(R.color.colorOnline));
            } else {
                statusText.setTextColor(itemView.getContext().getResources().getColor(R.color.colorOffline));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });
        }
    }
}