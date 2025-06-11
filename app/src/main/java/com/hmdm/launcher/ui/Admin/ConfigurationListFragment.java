package com.hmdm.launcher.ui.Admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;
import com.hmdm.launcher.ui.Admin.adapter.ConfigurationListAdapter;

import java.util.ArrayList;
import java.util.List;

public class ConfigurationListFragment extends Fragment {
    private static final String TAG = "ConfigurationListFragment";

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private ServerApi serverService;
    private ConfigurationListAdapter adapter;

    public static class Configuration {
        private int id;
        private String name;
        private String password;
        private String description;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getPassword() { return password; }
        public String getDescription() { return description; }

        public void setId(int id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setPassword(String password) { this.password = password; }
        public void setDescription(String description) { this.description = description; }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverService = new ServerServiceImpl(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configuration_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_configurations);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConfigurationListAdapter(new ArrayList<>(), this::onConfigurationClicked);
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Refresh triggered");
            fetchConfigurations();
        });

        fetchConfigurations();

        return view;
    }

    private void onConfigurationClicked(ConfigurationListFragment.Configuration configuration) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_custom_alert, null);
        TextView title = dialogView.findViewById(R.id.alert_title);
        TextView message = dialogView.findViewById(R.id.alert_message);
        Button btnNegative = dialogView.findViewById(R.id.btn_negative);
        Button btnPositive = dialogView.findViewById(R.id.btn_positive);

        title.setText("Options for " + configuration.getName());
        message.setText("Choose an action:");
        btnNegative.setOnClickListener(v -> ((AlertDialog) btnNegative.getTag()).dismiss());
        btnPositive.setVisibility(View.GONE); // Hide positive button initially

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        btnNegative.setTag(dialog);

        // Custom list items
        String[] options = {"Copy", "Delete"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setItems(options, (dialog1, which) -> {
                    if (which == 0) {
                        promptCopyConfiguration(configuration, dialogView);
                    } else if (which == 1) {
                        confirmDeleteConfiguration(configuration, dialogView);
                    }
                    dialog.dismiss();
                });
        builder.show();
    }

    private void promptCopyConfiguration(ConfigurationListFragment.Configuration configuration, View dialogView) {
        TextView title = dialogView.findViewById(R.id.alert_title);
        TextView message = dialogView.findViewById(R.id.alert_message);
        EditText editName = dialogView.findViewById(R.id.edit_new_name);
        EditText editDescription = dialogView.findViewById(R.id.edit_new_description);
        Button btnNegative = dialogView.findViewById(R.id.btn_negative);
        Button btnPositive = dialogView.findViewById(R.id.btn_positive);

        title.setText("Copy Configuration");
        message.setText("Enter the new name and description for the copy of " + configuration.getName());
        editName.setVisibility(View.VISIBLE);
        editDescription.setVisibility(View.VISIBLE);
        editName.setInputType(InputType.TYPE_CLASS_TEXT);
        editDescription.setInputType(InputType.TYPE_CLASS_TEXT);
        btnPositive.setVisibility(View.VISIBLE);
        btnPositive.setText("Copy");
        btnPositive.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_green_light)); // Temp green
        btnPositive.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newDescription = editDescription.getText().toString().trim();
            if (newName.isEmpty() || newDescription.isEmpty()) {
                Toast.makeText(getContext(), "Name and description cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            copyConfiguration(configuration, newName, newDescription);
            ((AlertDialog) btnNegative.getTag()).dismiss();
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        btnNegative.setTag(dialog);
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void confirmDeleteConfiguration(ConfigurationListFragment.Configuration configuration, View dialogView) {
        TextView title = dialogView.findViewById(R.id.alert_title);
        TextView message = dialogView.findViewById(R.id.alert_message);
        Button btnNegative = dialogView.findViewById(R.id.btn_negative);
        Button btnPositive = dialogView.findViewById(R.id.btn_positive);

        title.setText("Delete Configuration");
        message.setText("Are you sure you want to delete " + configuration.getName() + "?");
        btnPositive.setVisibility(View.VISIBLE);
        btnPositive.setText("Yes");
        btnPositive.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_red_light)); // Temp red
        btnPositive.setOnClickListener(v -> {
            deleteConfiguration(configuration);
            ((AlertDialog) btnNegative.getTag()).dismiss();
        });

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        btnNegative.setTag(dialog);
        btnNegative.setText("No");
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void copyConfiguration(ConfigurationListFragment.Configuration configuration, String newName, String newDescription) {
        progressBar.setVisibility(View.VISIBLE);
        serverService.copyConfiguration(configuration.getId(), newName, newDescription, new ServerApi.CopyConfigurationCallback() {
            @Override
            public void onSuccess(String message) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Configuration copied successfully", Toast.LENGTH_SHORT).show();
                    fetchConfigurations();
                });
            }

            @Override
            public void onError(String error) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error copying configuration: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error copying configuration: " + error);
                });
            }
        });
    }

    private void deleteConfiguration(ConfigurationListFragment.Configuration configuration) {
        progressBar.setVisibility(View.VISIBLE);
        serverService.deleteConfiguration(configuration.getId(), new ServerApi.DeleteConfigurationCallback() {
            @Override
            public void onSuccess(String message) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Configuration deleted successfully", Toast.LENGTH_SHORT).show();
                    fetchConfigurations();
                });
            }

            @Override
            public void onError(String error) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error deleting configuration: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error deleting configuration: " + error);
                });
            }
        });
    }

    private void fetchConfigurations() {
        if (getContext() == null) {
            Log.e(TAG, "Context null, cannot load configurations");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);

        serverService.getConfigurations(
                configurations -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    if (configurations.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("No configurations found");
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                        adapter.updateConfigurations(configurations);
                    }
                    Toast.makeText(getContext(), "Configurations loaded", Toast.LENGTH_SHORT).show();
                }),
                error -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error loading configurations: " + error);
                })
        );
    }
}