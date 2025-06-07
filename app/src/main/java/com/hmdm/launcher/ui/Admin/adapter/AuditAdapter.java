package com.hmdm.launcher.ui.Admin.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hmdm.launcher.R;
import com.hmdm.launcher.ui.Admin.AuditFragment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AuditAdapter extends RecyclerView.Adapter<AuditAdapter.AuditViewHolder> {

    private final List<AuditFragment.AuditLog> auditLogs;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.getDefault());

    public AuditAdapter(List<AuditFragment.AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }

    @NonNull
    @Override
    public AuditViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.audit_item, parent, false);
        return new AuditViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AuditViewHolder holder, int position) {
        AuditFragment.AuditLog log = auditLogs.get(position);
        holder.tvDateTime.setText(formatDateTime(log.getCreateTime()));
        holder.tvUser.setText(log.getLogin());
        holder.tvIpAddress.setText(log.getIpAddress());
        holder.tvAction.setText(log.getAction());
    }

    @Override
    public int getItemCount() {
        return auditLogs.size();
    }

    public static class AuditViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime, tvUser, tvIpAddress, tvAction;

        public AuditViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);
            tvUser = itemView.findViewById(R.id.tv_user);
            tvIpAddress = itemView.findViewById(R.id.tv_ip_address);
            tvAction = itemView.findViewById(R.id.tv_action);
        }
    }

    private String formatDateTime(long millis) {
        return dateFormat.format(millis);
    }
}
