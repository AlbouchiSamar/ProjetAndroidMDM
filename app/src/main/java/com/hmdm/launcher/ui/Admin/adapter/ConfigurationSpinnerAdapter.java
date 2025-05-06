package com.hmdm.launcher.ui.Admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hmdm.launcher.R;
import com.hmdm.launcher.ui.Admin.ConfigurationListFragment;

import java.util.List;

public class ConfigurationSpinnerAdapter extends ArrayAdapter<ConfigurationListFragment.Configuration> {

    private LayoutInflater inflater;

    public ConfigurationSpinnerAdapter(@NonNull Context context, @NonNull List<ConfigurationListFragment.Configuration> configurations) {
        super(context, 0, configurations);
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_spinner_configuration, parent, false);
        }

        TextView configNameText = convertView.findViewById(R.id.text_config_name);

        ConfigurationListFragment.Configuration configuration = getItem(position);
        if (configuration != null) {
            configNameText.setText(configuration.getName());
        }

        return convertView;
    }
}
