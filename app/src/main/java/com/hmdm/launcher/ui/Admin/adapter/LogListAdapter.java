package com.hmdm.launcher.ui.Admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hmdm.launcher.R;
import com.hmdm.launcher.ui.Admin.LogsFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.LogViewHolder> {

    private List<LogsFragment.LogEntry> logs;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault() );

    public LogListAdapter(List<LogsFragment.LogEntry> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogsFragment.LogEntry log = logs.get(position);
        holder.bind(log, dateFormat);
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public void updateLogs(List<LogsFragment.LogEntry> newLogs) {
        this.logs.clear();
        this.logs.addAll(newLogs);
        notifyDataSetChanged();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        private TextView timestampText;
        private TextView levelText;
        private TextView messageText;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            timestampText = itemView.findViewById(R.id.text_timestamp);
            levelText = itemView.findViewById(R.id.text_level);
            messageText = itemView.findViewById(R.id.text_message);
        }

        public void bind(final LogsFragment.LogEntry log, SimpleDateFormat dateFormat) {
            timestampText.setText(dateFormat.format(new Date(log.getTimestamp())));
            levelText.setText(log.getLevel());
            messageText.setText(log.getMessage());

            // Définir la couleur du niveau de log
            int colorResId;
            switch (log.getLevel()) {
                case "ERROR":
                    colorResId = R.color.colorLogError;
                    break;
                case "WARN":
                    colorResId = R.color.colorLogWarn;
                    break;
                case "INFO":
                    colorResId = R.color.colorLogInfo;
                    break;
                default:
                    colorResId = R.color.colorLogDebug;
                    break;
            }
            levelText.setTextColor(itemView.getContext().getResources().getColor(colorResId));
        }
    }
}
