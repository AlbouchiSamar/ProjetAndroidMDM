package com.hmdm.launcher.ui.Admin.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hmdm.launcher.R;

import java.util.List;

public class PackageListAdapter extends ArrayAdapter<String> {

    private List<String> packages;
    private LayoutInflater inflater;

    public PackageListAdapter(Context context, List<String> packages) {
        super(context, R.layout.item_package, packages);
        this.packages = packages;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_package, parent, false);
            holder = new ViewHolder();
            holder.packageName = convertView.findViewById(R.id.text_package_name);
            holder.removeButton = convertView.findViewById(R.id.btn_remove_package);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String packageName = packages.get(position);
        holder.packageName.setText(packageName);

        holder.removeButton.setOnClickListener(v -> {
            packages.remove(position);
            notifyDataSetChanged();
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView packageName;
        ImageButton removeButton;
    }
}
