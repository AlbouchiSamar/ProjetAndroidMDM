package com.hmdm.launcher.ui.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerService;
import com.hmdm.launcher.server.ServerServiceImpl;
import com.hmdm.launcher.ui.Admin.adapter.ConfigurationSpinnerAdapter;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class AddDeviceActivity extends AppCompatActivity {
    private static final String TAG = "AddDeviceActivity";
    private EditText deviceNumberEdit;
    private EditText deviceNameEdit;
    private Spinner configurationSpinner;
    private Button addButton;
    private ProgressBar progressBar;
    private ServerApi serverService;
    private SettingsHelper settingsHelper;
    private List<ConfigurationListFragment.Configuration> configurations = new ArrayList<>();
    private ConfigurationSpinnerAdapter configAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);
        serverService = new ServerServiceImpl(this);
        settingsHelper = SettingsHelper.getInstance(this);

        // Vérifier le token
     /*   if (settingsHelper.getToken() == null) {
            Toast.makeText(this, "Veuillez vous connecter", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupSpinner();
        loadConfigurations();
    }

    private void initViews() {
        deviceNumberEdit = findViewById(R.id.edit_device_number);
        deviceNameEdit = findViewById(R.id.edit_device_name);
        configurationSpinner = findViewById(R.id.spinner_configuration);
        addButton = findViewById(R.id.btn_add_device);
        progressBar = findViewById(R.id.progress_bar);
        addButton.setOnClickListener(v -> validateAndAddDevice());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.add_device);
    }

    private void setupSpinner() {
        configAdapter = new ConfigurationSpinnerAdapter(this, configurations);
        configurationSpinner.setAdapter(configAdapter);
    }

    private void loadConfigurations() {
        showProgress(true);
        serverService.getConfigurations(configList -> {
            configurations.clear();
            configurations.addAll(configList);
            runOnUiThread(() -> {
                configAdapter.notifyDataSetChanged();
                showProgress(false);
                if (configurations.isEmpty()) {
                    Toast.makeText(this, "Aucune configuration disponible", Toast.LENGTH_LONG).show();
                }
            });
        }, error -> runOnUiThread(() -> {
            showProgress(false);
            Toast.makeText(this, "Erreur: " + error, Toast.LENGTH_LONG).show();
            // Simulation pour tests
            configurations.add(new ConfigurationListFragment.Configuration("1", "Config par défaut"));
            configAdapter.notifyDataSetChanged();
        }));
    }

    private void validateAndAddDevice() {
        String deviceNumber = deviceNumberEdit.getText().toString().trim();
        String deviceName = deviceNameEdit.getText().toString().trim();

        if (deviceNumber.isEmpty()) {
            deviceNumberEdit.setError(getString(R.string.field_required));
            deviceNumberEdit.requestFocus();
            return;
        }
        if (!deviceNumber.matches("[a-zA-Z0-9_-]+")) {
            deviceNumberEdit.setError("ID invalide (lettres, chiffres, - ou _ uniquement)");
            deviceNumberEdit.requestFocus();
            return;
        }
        if (deviceName.isEmpty()) {
            deviceNameEdit.setError(getString(R.string.field_required));
            deviceNameEdit.requestFocus();
            return;
        }
        if (deviceName.length() > 100) {
            deviceNameEdit.setError("Nom trop long (max 100 caractères)");
            deviceNameEdit.requestFocus();
            return;
        }
        if (configurations.isEmpty() || configurationSpinner.getSelectedItemPosition() == -1) {
            Toast.makeText(this, R.string.select_configuration, Toast.LENGTH_SHORT).show();
            return;
        }

        ConfigurationListFragment.Configuration selectedConfig = configurations.get(configurationSpinner.getSelectedItemPosition());
        addDevice(deviceNumber, deviceName, selectedConfig.getId());
    }

    private void addDevice(String deviceNumber, String deviceName, String configId) {
        showProgress(true);
        try {
            JSONObject deviceData = new JSONObject();
            deviceData.put("number", deviceNumber);
            deviceData.put("name", deviceName);
            deviceData.put("configurationId", configId);
            serverService.addDevice(deviceData, () -> runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, R.string.device_added_successfully, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK); // Pour rafraîchir DeviceListFragment
                finish();
            }), error -> runOnUiThread(() -> {
                showProgress(false);
                String message = error;
                if (error.contains("401")) {
                    message = "Session expirée, veuillez vous reconnecter";
                    startActivity(new Intent(this, AdminLoginActivity.class));
                    finish();
                } else if (error.contains("409")) {
                    message = "L'appareil existe déjà";
                }
                Toast.makeText(this, "Erreur: " + message, Toast.LENGTH_LONG).show();
            }));
        } catch (Exception e) {
            showProgress(false);
            Toast.makeText(this, "Erreur interne: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        addButton.setEnabled(!show);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/
}
}