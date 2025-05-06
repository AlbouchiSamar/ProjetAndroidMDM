package com.hmdm.launcher.ui.Admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hmdm.launcher.R;
import com.hmdm.launcher.ui.Admin.ConfigurationListFragment;

import java.util.List;

public class ConfigurationListAdapter extends RecyclerView.Adapter<ConfigurationListAdapter.ConfigurationViewHolder> {

    private List<ConfigurationListFragment.Configuration> configurations;
    private OnConfigurationClickListener listener;

    public ConfigurationListAdapter(List<ConfigurationListFragment.Configuration> configurations, OnConfigurationClickListener listener) {
        this.configurations = configurations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConfigurationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_configuration, parent, false);
        return new ConfigurationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConfigurationViewHolder holder, int position) {
        ConfigurationListFragment.Configuration configuration = configurations.get(position);
        holder.bind(configuration, listener);
    }

    @Override
    public int getItemCount() {
        return configurations.size();
    }

    public void updateConfigurations(List<ConfigurationListFragment.Configuration> newConfigurations) {
        this.configurations.clear();
        this.configurations.addAll(newConfigurations);
        notifyDataSetChanged();
    }

    public interface OnConfigurationClickListener {
        void onConfigurationClick(ConfigurationListFragment.Configuration configuration);
    }

    static class ConfigurationViewHolder extends RecyclerView.ViewHolder {
        private TextView configNameText;
        private TextView configDescriptionText;

        public ConfigurationViewHolder(@NonNull View itemView) {
            super(itemView);
            configNameText = itemView.findViewById(R.id.text_config_name);
            configDescriptionText = itemView.findViewById(R.id.text_config_description);
        }

        public void bind(final ConfigurationListFragment.Configuration configuration, final OnConfigurationClickListener listener) {
            configNameText.setText(configuration.getName());
            configDescriptionText.setText(configuration.getDescription());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConfigurationClick(configuration);
                }
            });
        }
    }
}
