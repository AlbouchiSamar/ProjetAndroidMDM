package com.hmdm.launcher.ui.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.hmdm.launcher.R;
import com.hmdm.launcher.server.ServerService;

public class DeviceDetailActivity extends AppCompatActivity {

    private TextView deviceNumberText;
    private TextView deviceNameText;
    private TextView statusText;
    private TextView lastOnlineText;
    private TextView configNameText;
    private Button lockButton;
    private Button unlockButton;
    private Button rebootButton;
    private Button wipeDataButton;
    private ProgressBar progressBar;

    private String deviceNumber;
    private DeviceListFragment.Device device;

    private ServerService serverService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        serverService = new ServerService(this);

        // Récupérer le numéro de l'appareil depuis l'intent
        deviceNumber = getIntent().getStringExtra("device_number");
        if (deviceNumber == null) {
            Toast.makeText(this, "Numéro d'appareil non spécifié", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupButtons();

        loadDeviceDetails();
    }

    private void initViews() {
        deviceNumberText = findViewById(R.id.text_device_number);
        deviceNameText = findViewById(R.id.text_device_name);
        statusText = findViewById(R.id.text_status);
        lastOnlineText = findViewById(R.id.text_last_online);
        configNameText = findViewById(R.id.text_config_name);
        lockButton = findViewById(R.id.btn_lock);
        unlockButton = findViewById(R.id.btn_unlock);
        rebootButton = findViewById(R.id.btn_reboot);
        wipeDataButton = findViewById(R.id.btn_wipe_data);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Détails de l'appareil");
    }

    private void setupButtons() {
        lockButton.setOnClickListener(v -> {
            confirmAndExecuteAction("Verrouiller", "Êtes-vous sûr de vouloir verrouiller cet appareil ?", this::lockDevice);
        });

        unlockButton.setOnClickListener(v -> {
            confirmAndExecuteAction("Déverrouiller", "Êtes-vous sûr de vouloir déverrouiller cet appareil ?", this::unlockDevice);
        });

        rebootButton.setOnClickListener(v -> {
            confirmAndExecuteAction("Redémarrer", "Êtes-vous sûr de vouloir redémarrer cet appareil ?", this::rebootDevice);
        });

        wipeDataButton.setOnClickListener(v -> {
            // Ouvrir l'écran de suppression des données avec l'appareil pré-sélectionné
            Intent intent = new Intent(this, WipeDataActivity.class);
            intent.putExtra("device_number", deviceNumber);
            startActivity(intent);
        });
    }

    private void loadDeviceDetails() {
        showProgress(true);

        serverService.getDeviceDetails(deviceNumber, deviceDetails -> {
            this.device = deviceDetails;

            runOnUiThread(() -> {
                updateDeviceInfo();
                showProgress(false);
            });
        }, error -> {
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "Erreur lors du chargement des détails de l'appareil: " + error, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void updateDeviceInfo() {
        if (device != null) {
            deviceNumberText.setText(device.getNumber());
            deviceNameText.setText(device.getName());
            statusText.setText(device.getStatus());
            lastOnlineText.setText(device.getLastOnline());
            configNameText.setText(device.getConfigName());

            // Mettre à jour l'état des boutons en fonction du statut de l'appareil
            boolean isOnline = "En ligne".equals(device.getStatus());
            lockButton.setEnabled(isOnline);
            unlockButton.setEnabled(isOnline);
            rebootButton.setEnabled(isOnline);
        }
    }

    private void confirmAndExecuteAction(String actionName, String message, Runnable action) {
        new AlertDialog.Builder(this)
                .setTitle(actionName)
                .setMessage(message)
                .setPositiveButton("Oui", (dialog, which) -> action.run())
                .setNegativeButton("Non", null)
                .show();
    }

    private void lockDevice() {
        showProgress(true);

        serverService.lockDevice(deviceNumber, () -> {
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "Appareil verrouillé avec succès", Toast.LENGTH_SHORT).show();
            });
        }, error -> {
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "Erreur lors du verrouillage de l'appareil: " + error, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void unlockDevice() {
        showProgress(true);

        serverService.unlockDevice(deviceNumber, () -> {
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "Appareil déverrouillé avec succès", Toast.LENGTH_SHORT).show();
            });
        }, error -> {
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "Erreur lors du déverrouillage de l'appareil: " + error, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void rebootDevice() {
        showProgress(true);

        serverService.rebootDevice(deviceNumber, () -> {
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "Commande de redémarrage envoyée avec succès", Toast.LENGTH_SHORT).show();
            });
        }, error -> {
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "Erreur lors de l'envoi de la commande de redémarrage: " + error, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
