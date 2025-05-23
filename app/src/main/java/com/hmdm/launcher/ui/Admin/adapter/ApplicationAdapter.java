package com.hmdm.launcher.ui.Admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.hmdm.launcher.R;
import com.hmdm.launcher.ui.Admin.ApplicationListFragment;

import java.util.ArrayList;
import java.util.List;

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ViewHolder> {
    private List<ApplicationListFragment.Application> applications;

    public ApplicationAdapter(List<ApplicationListFragment.Application> applications) {
        this.applications = applications != null ? applications : new ArrayList<>();
    }

    public void updateApplications(List<ApplicationListFragment.Application> newApplications) {
        List<ApplicationListFragment.Application> oldList = new ArrayList<>(this.applications);
        this.applications = newApplications != null ? new ArrayList<>(newApplications) : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(oldList, this.applications));
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationListFragment.Application application = applications.get(position);
        holder.textId.setText("ID: " + application.getId());
        holder.textName.setText(application.getName());
        holder.textPkg.setText("Package: " + application.getPkg());
    }

    @Override
    public int getItemCount() {
        return applications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textId;
        TextView textName;
        TextView textPkg;

        ViewHolder(View itemView) {
            super(itemView);
            textId = itemView.findViewById(R.id.text_id);
            textName = itemView.findViewById(R.id.text_name);
            textPkg = itemView.findViewById(R.id.text_pkg);
        }
    }

    private static class DiffCallback extends DiffUtil.Callback {
        private final List<ApplicationListFragment.Application> oldList;
        private final List<ApplicationListFragment.Application> newList;

        DiffCallback(List<ApplicationListFragment.Application> oldList, List<ApplicationListFragment.Application> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ApplicationListFragment.Application oldApp = oldList.get(oldItemPosition);
            ApplicationListFragment.Application newApp = newList.get(newItemPosition);
            return oldApp.getId() == newApp.getId() &&
                    oldApp.getName().equals(newApp.getName()) &&
                    oldApp.getPkg().equals(newApp.getPkg());
        }
    }
}