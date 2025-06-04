package com.hmdm.launcher.ui.Admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;

import org.json.JSONArray;
import org.json.JSONObject;

public class DeleteAppFragment extends Fragment {
    private static final String TAG = "DeleteAppFragment";

    private EditText editConfigurationId, editApplicationId;
    private Button btnUninstallApp;
    private ProgressBar progressBar;
    private ServerApi serverService;
    private SettingsHelper settingsHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsHelper = SettingsHelper.getInstance(requireContext());
        serverService = new ServerServiceImpl(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delete_app, container, false);

        editConfigurationId = view.findViewById(R.id.edit_configuration_id);
        editApplicationId = view.findViewById(R.id.edit_application_id);
        btnUninstallApp = view.findViewById(R.id.btn_uninstall_app);
        progressBar = view.findViewById(R.id.progress_bar);

        btnUninstallApp.setOnClickListener(v -> uninstallApplication());

        return view;
    }

    private void uninstallApplication() {
        String configIdStr = editConfigurationId.getText().toString().trim();
        String appIdStr = editApplicationId.getText().toString().trim();

        if (configIdStr.isEmpty() || appIdStr.isEmpty()) {
            Toast.makeText(getContext(), "Entrez l'ID de la configuration et de l'application", Toast.LENGTH_SHORT).show();
            return;
        }

        int configurationId;
        int applicationId;
        try {
            configurationId = Integer.parseInt(configIdStr);
            applicationId = Integer.parseInt(appIdStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "IDs doivent être des nombres valides", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        sendUninstallRequest(configurationId, applicationId);
    }

    private void sendUninstallRequest(int configurationId, int applicationId) {
        try {
            JSONObject configJson = buildMinimalConfigurationJson(configurationId, applicationId);
            updateConfiguration(configJson);
        } catch (Exception e) {
            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Erreur préparation données: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Erreur préparation JSON: " + e.getMessage());
            });
        }
    }

    private JSONObject buildMinimalConfigurationJson(int configurationId, int applicationId) throws Exception {
        JSONObject configJson = new JSONObject();
        configJson.put("id", configurationId);

        JSONArray applicationsArray = new JSONArray();
        JSONObject appJson = new JSONObject();
        appJson.put("id", applicationId);
        appJson.put("action", 2);
        appJson.put("actionChanged", true);
        applicationsArray.put(appJson);

        configJson.put("applications", applicationsArray);

        return configJson;
    }

    private void updateConfiguration(JSONObject configJson) {
        serverService.updateConfiguration(configJson.toString(), new ServerApi.UpdateConfigurationCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Application désinstallée avec succès", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Erreur désinstallation: " + error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur update configuration: " + error);
                });
            }
        });
    }
}