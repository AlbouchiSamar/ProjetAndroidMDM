package com.hmdm.launcher.ui.Admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddApplicationFragment extends Fragment {
    private static final String TAG = "AddApplicationFragment";

    private EditText editName, editPkg, editVersion, editVersionCode;
    private CheckBox checkShowIcon;
    private Button btnUploadApk, btnValidatePkg, btnCreateApp, btnSelectConfig, btnInstall;
    private ProgressBar progressBar;
    private TextView textUrl;
    private Spinner spinnerConfig;
    private ServerApi serverService;
    private SettingsHelper settingsHelper;
    private String serverPath;
    private ActivityResultLauncher<String> filePickerLauncher;
    private List<Configuration> configurationsList;
    private Map<String, Integer> configNameToIdMap;
    private Integer selectedConfigId;
    private int applicationId;
    private  Configuration selectedConfiguration = null; // au début de ta classe, comme champ


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsHelper = SettingsHelper.getInstance(requireContext());
        serverService = new ServerServiceImpl(requireContext());
        configurationsList = new ArrayList<>();
        configNameToIdMap = new HashMap<>();

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) handleApkUpload(uri);
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_application, container, false);

        editName = view.findViewById(R.id.edit_name);
        editPkg = view.findViewById(R.id.edit_pkg);
        editVersion = view.findViewById(R.id.edit_version);
        editVersionCode = view.findViewById(R.id.edit_version_code);
        spinnerConfig = view.findViewById(R.id.spinner_config);
        checkShowIcon = view.findViewById(R.id.check_show_icon);
        btnUploadApk = view.findViewById(R.id.btn_upload_apk);
        btnValidatePkg = view.findViewById(R.id.btn_validate_pkg);
        btnCreateApp = view.findViewById(R.id.btn_create_app);
        btnSelectConfig = view.findViewById(R.id.btn_select_config);
        btnInstall = view.findViewById(R.id.btn_install);
        progressBar = view.findViewById(R.id.progress_bar);
        textUrl = view.findViewById(R.id.text_url);

        btnUploadApk.setOnClickListener(v -> filePickerLauncher.launch("application/vnd.android.package-archive"));
        btnValidatePkg.setOnClickListener(v -> validatePackage());
        btnCreateApp.setOnClickListener(v -> createApplication());
        btnSelectConfig.setOnClickListener(v -> selectConfiguration());
        btnInstall.setOnClickListener(v -> installApplication());

        btnValidatePkg.setEnabled(false);
        btnCreateApp.setEnabled(false);
        btnSelectConfig.setEnabled(false);
        btnInstall.setEnabled(false);

        fetchAvailableConfigurations();

        return view;
    }

    private void handleApkUpload(Uri uri) {
        progressBar.setVisibility(View.VISIBLE);
        try {
            File file = createTempFileFromUri(uri);
            serverService.uploadApplicationFile(file,
                    (serverPath, pkg, name, version) -> requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        this.serverPath = serverPath;
                        textUrl.setText("URL: " + serverPath);
                        editPkg.setText(pkg);
                        editVersion.setText(version);
                        editName.setText(name);
                        btnValidatePkg.setEnabled(true);
                        Toast.makeText(getContext(), "Fichier APK téléversé avec succès", Toast.LENGTH_SHORT).show();
                    }),
                    error -> requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Erreur lors du téléversement: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Erreur upload APK: " + error);
                    }));
        } catch (IOException e) {
            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Erreur conversion fichier: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Erreur conversion fichier: " + e.getMessage());
            });
        }
    }

    private File createTempFileFromUri(Uri uri) throws IOException {
        File file = File.createTempFile("temp_apk", ".apk", requireContext().getCacheDir());
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        return file;
    }

    private void validatePackage() {
        if (serverPath == null || editPkg.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Téléversez un APK et entrez un nom de package", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        serverService.validatePackage(
                editName.getText().toString().trim(),
                editPkg.getText().toString().trim(),
                editVersion.getText().toString().trim(),
                Integer.parseInt(editVersionCode.getText().toString().trim()),
                null,
                serverPath,
                new ServerApi.ValidatePackageCallback() {
                    @Override
                    public void onValidatePackage(boolean isUnique, String validatedPkg) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            if (isUnique) {
                                Toast.makeText(getContext(), "Package valide et unique", Toast.LENGTH_SHORT).show();
                                btnCreateApp.setEnabled(true);
                            } else {
                                Toast.makeText(getContext(), "Package déjà existant: " + validatedPkg, Toast.LENGTH_LONG).show();
                                btnCreateApp.setEnabled(false);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Erreur validation: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Erreur validation package: " + error);
                        });
                    }
                });
    }

    private void createApplication() {
        if (serverPath == null || editPkg.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Téléversez un APK et validez le package", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("name", editName.getText().toString().trim());
            jsonBody.put("pkg", editPkg.getText().toString().trim());
            jsonBody.put("version", editVersion.getText().toString().trim());
            jsonBody.put("versionCode", Integer.parseInt(editVersionCode.getText().toString().trim()));
            jsonBody.put("arch", null);
            jsonBody.put("filePath", serverPath);
            jsonBody.put("showIcon", checkShowIcon.isChecked());
            jsonBody.put("type", "app");

            serverService.createOrUpdateApp(
                    0,
                    jsonBody.toString(),
                    new ServerApi.CreateAppCallback() {
                        @Override
                        public void onCreateAppSuccess(String message, int applicationId) {
                            requireActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Application créée avec succès: " + message, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Application ID: " + applicationId);
                                AddApplicationFragment.this.applicationId = applicationId;
                                btnSelectConfig.setEnabled(true);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            requireActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Erreur création: " + error, Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Erreur création application: " + error);
                            });
                        }
                    });
        } catch (Exception e) {
            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Erreur données: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Erreur parsing JSON: " + e.getMessage());
            });
        }
    }

    private void selectConfiguration() {

        if (applicationId == 0) {
            Toast.makeText(getContext(), "Créez d'abord une application", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        serverService.getApplicationConfigurations(
                applicationId,
                new ServerApi.GetAvailableConfigurationsCallback() {
                    @Override
                    public void onSuccess(List<Configuration> configurations) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            configurationsList.clear();
                            configurationsList.addAll(configurations);
                            List<String> configNames = new ArrayList<>();
                            configNames.add("Sélectionnez une configuration");
                            for (Configuration config : configurations) {
                                configNames.add(config.getConfigurationName());
                                configNameToIdMap.put(config.getConfigurationName(), config.getConfigurationId());
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                    android.R.layout.simple_spinner_item, configNames);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerConfig.setAdapter(adapter);

                            spinnerConfig.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    if (position > 0) {
                                        String selectedName = (String) parent.getItemAtPosition(position);
                                        selectedConfigId = configNameToIdMap.get(selectedName);

                                        // Met à jour aussi selectedConfiguration ici :
                                        for (Configuration config : configurationsList) {
                                            if (config.getConfigurationId() == selectedConfigId) {
                                                selectedConfiguration = config;
                                                break;
                                            }
                                        }

                                        btnInstall.setEnabled(true);
                                    } else {
                                        selectedConfigId = null;
                                        selectedConfiguration = null;
                                        btnInstall.setEnabled(false);
                                    }

                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                    selectedConfigId = null;
                                    selectedConfiguration = null;
                                    btnInstall.setEnabled(false);
                                }
                            });
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Erreur récupération configurations: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Erreur fetch configurations: " + error);
                        });
                    }
                });
    }

    private void installApplication() {
        Log.d(TAG, "Starting installApplication");
        if (applicationId == 0 || selectedConfigId == null) {
            Toast.makeText(getContext(), "Sélectionnez une configuration et créez une application", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Configuration selectedConfig = null;
        for (Configuration config : configurationsList) {
            if (config.getConfigurationId() == selectedConfigId) {
                selectedConfig = config;
                break;
            }
        }
        if (selectedConfig != null) {
            Log.d(TAG, "Calling serverService.updateApplicationConfiguration for appId: " + applicationId + ", configId: " + selectedConfigId);
            serverService.updateApplicationConfiguration(
                    applicationId,
                    selectedConfigId,
                    new ServerApi.ConfigurationUpdateRequest(
                            selectedConfig.getCustomerId(),
                            selectedConfig.getConfigurationId(),
                            selectedConfig.getConfigurationName(),
                            1, // action = 1 for automatic installation
                            selectedConfig.isShowIcon(),
                            true // notify = true to inform users
                    ),
                    new ServerApi.UpdateConfigurationCallback() {
                        @Override
                        public void onSuccess() {
                            requireActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Configuration mise à jour, application installée", Toast.LENGTH_SHORT).show();
                                btnInstall.setEnabled(false);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            requireActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Erreur mise à jour: " + error, Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Erreur update configuration: " + error);
                            });
                        }
                    },
                    editName.getText().toString().trim() // applicationName
            );
        } else {
            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Configuration non trouvée", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void fetchAvailableConfigurations() {
        progressBar.setVisibility(View.VISIBLE);
        serverService.getAvailableConfigurations(
                new ServerApi.AvailableConfigurationListCallback() {
                    @Override
                    public void onSuccess(List<Configuration> configurations) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            configurationsList.clear();
                            configurationsList.addAll(configurations);
                            List<String> configNames = new ArrayList<>();
                            configNames.add("Sélectionnez une configuration");
                            for (Configuration config : configurations) {
                                configNames.add(config.getConfigurationName());
                                configNameToIdMap.put(config.getConfigurationName(), config.getConfigurationId());
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                    android.R.layout.simple_spinner_item, configNames);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerConfig.setAdapter(adapter);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Erreur récupération configurations: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Erreur fetch configurations: " + error);
                        });
                    }
                });
    }
}