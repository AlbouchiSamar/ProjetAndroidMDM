package com.hmdm.launcher.ui.Admin.adapter;



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
        this.devices.clear();
        this.devices.addAll(newDevices);
        notifyDataSetChanged();
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

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceIdText = itemView.findViewById(R.id.text_device_id);
            lastupdate = itemView.findViewById(R.id.text_lastupdate);
            deviceNumberText = itemView.findViewById(R.id.text_device_number);
            deviceNameText = itemView.findViewById(R.id.text_device_name);
            statusText = itemView.findViewById(R.id.text_status);
            configNameText = itemView.findViewById(R.id.text_config_name);
        }

        public void bind(final DeviceListFragment.Device device, final OnDeviceClickListener listener) {
            deviceIdText.setText(String.valueOf(device.getId()));
            lastupdate.setText(device.getLastOnline());
            deviceNumberText.setText(device.getNumber());
            deviceNameText.setText(device.getName());
            statusText.setText(device.getStatus());
            configNameText.setText(device.getModel());

            // Définir la couleur du statut en fonction de l'état de l'appareil
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