package com.hmdm.launcher.ui.Admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.json.WipeDataCommandProcessor;
import com.hmdm.launcher.server.ServerService;
import com.hmdm.launcher.ui.Admin.adapter.PackageListAdapter;

import java.util.ArrayList;
import java.util.List;

public class WipeDataActivity extends AppCompatActivity {

    private RadioGroup wipeTypeGroup;
    private RadioButton factoryResetRadio;
    private RadioButton selectiveWipeRadio;
    private RadioButton userDataWipeRadio;

    private View selectiveWipeContainer;
    private ListView packageListView;
    private Button addPackageButton;
    private EditText packageNameEdit;

    private Button executeButton;
    private Button cancelButton;

    private Spinner deviceSpinner;
    private ArrayAdapter<String> deviceAdapter;
    private List<String> deviceNumbers = new ArrayList<>();
    private List<String> deviceNames = new ArrayList<>();

    private PackageListAdapter packageListAdapter;
    private List<String> selectedPackages = new ArrayList<>();

    private ServerApi serverService;
    private SettingsHelper settingsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wipe_data);

        settingsHelper = SettingsHelper.getInstance(this);
        serverService = new ServerService(this);

        initViews();
        setupListeners();
        loadDevices();
    }

    private void initViews() {
        wipeTypeGroup = findViewById(R.id.wipe_type_group);
        factoryResetRadio = findViewById(R.id.radio_factory_reset);
        selectiveWipeRadio = findViewById(R.id.radio_selective_wipe);
        userDataWipeRadio = findViewById(R.id.radio_user_data_wipe);

        selectiveWipeContainer = findViewById(R.id.selective_wipe_container);
        packageListView = findViewById(R.id.package_list);
        addPackageButton = findViewById(R.id.btn_add_package);
        packageNameEdit = findViewById(R.id.edit_package_name);

        executeButton = findViewById(R.id.btn_execute);
        cancelButton = findViewById(R.id.btn_cancel);

        deviceSpinner = findViewById(R.id.device_spinner);

        // Initialiser l'adaptateur pour la liste des packages
        packageListAdapter = new PackageListAdapter(this, selectedPackages);
        packageListView.setAdapter(packageListAdapter);

        // Initialiser l'adaptateur pour la liste des appareils
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deviceNames);
        deviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceSpinner.setAdapter(deviceAdapter);
    }

    private void setupListeners() {
        wipeTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_selective_wipe) {
                selectiveWipeContainer.setVisibility(View.VISIBLE);
            } else {
                selectiveWipeContainer.setVisibility(View.GONE);
            }
        });

        addPackageButton.setOnClickListener(v -> {
            String packageName = packageNameEdit.getText().toString().trim();
            if (!packageName.isEmpty() && !selectedPackages.contains(packageName)) {
                selectedPackages.add(packageName);
                packageListAdapter.notifyDataSetChanged();
                packageNameEdit.setText("");
            }
        });

        executeButton.setOnClickListener(v -> {
            confirmAndExecuteWipe();
        });

        cancelButton.setOnClickListener(v -> {
            finish();
        });
    }

    private void loadDevices() {
        // Dans une application réelle, vous chargeriez la liste des appareils depuis le serveur
        // Pour cet exemple, nous utilisons des données fictives
        serverService.getDevices(devices -> {
            deviceNumbers.clear();
            deviceNames.clear();

            for (Device device : devices) {
                deviceNumbers.add(device.getNumber());
                deviceNames.add(device.getName() + " (" + device.getNumber() + ")");
            }

            runOnUiThread(() -> {
                deviceAdapter.notifyDataSetChanged();
            });
        }, error -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Erreur lors du chargement des appareils: " + error, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void confirmAndExecuteWipe() {
        int selectedPosition = deviceSpinner.getSelectedItemPosition();
        if (selectedPosition < 0 || selectedPosition >= deviceNumbers.size()) {
            Toast.makeText(this, "Veuillez sélectionner un appareil", Toast.LENGTH_SHORT).show();
            return;
        }

        String deviceNumber = deviceNumbers.get(selectedPosition);
        String deviceName = deviceNames.get(selectedPosition);

        int checkedRadioId = wipeTypeGroup.getCheckedRadioButtonId();
        String wipeType;
        String confirmMessage;

        if (checkedRadioId == R.id.radio_factory_reset) {
            wipeType = WipeDataCommandProcessor.TYPE_FACTORY_RESET;
            confirmMessage = "Êtes-vous sûr de vouloir effectuer un factory reset sur l'appareil " + deviceName + "?\n\nCette action est irréversible et supprimera toutes les données de l'appareil.";
        } else if (checkedRadioId == R.id.radio_selective_wipe) {
            if (selectedPackages.isEmpty()) {
                Toast.makeText(this, "Veuillez ajouter au moins un package à effacer", Toast.LENGTH_SHORT).show();
                return;
            }
            wipeType = WipeDataCommandProcessor.TYPE_SELECTIVE;
            confirmMessage = "Êtes-vous sûr de vouloir effacer les données des applications sélectionnées sur l'appareil " + deviceName + "?";
        } else if (checkedRadioId == R.id.radio_user_data_wipe) {
            wipeType = WipeDataCommandProcessor.TYPE_USER_DATA;
            confirmMessage = "Êtes-vous sûr de vouloir effacer toutes les données utilisateur sur l'appareil " + deviceName + "?";
        } else {
            Toast.makeText(this, "Veuillez sélectionner un type d'effacement", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage(confirmMessage)
                .setPositiveButton("Oui", (dialog, which) -> {
                    executeWipe(deviceNumber, wipeType);
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void executeWipe(String deviceNumber, String wipeType) {
        // Créer la requête d'effacement
        WipeDataRequest request = new WipeDataRequest();
        request.setWipeType(wipeType);

        if (WipeDataCommandProcessor.TYPE_SELECTIVE.equals(wipeType)) {
            request.setPackages(new ArrayList<>(selectedPackages));
        }

        // Envoyer la requête au serveur
        serverService.wipeDeviceData(deviceNumber, request, () -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Commande d'effacement envoyée avec succès", Toast.LENGTH_LONG).show();
                finish();
            });
        }, error -> {
            runOnUiThread(() -> {
                Toast.makeText(this, "Erreur lors de l'envoi de la commande: " + error, Toast.LENGTH_LONG).show();
            });
        });
    }

    // Classe interne pour représenter un appareil
    private static class Device {
        private String number;
        private String name;

        public Device(String number, String name) {
            this.number = number;
            this.name = name;
        }

        public String getNumber() {
            return number;
        }

        public String getName() {
            return name;
        }
    }

    // Classe interne pour la requête d'effacement
    public static class WipeDataRequest {
        private String wipeType;
        private List<String> packages;

        public String getWipeType() {
            return wipeType;
        }

        public void setWipeType(String wipeType) {
            this.wipeType = wipeType;
        }

        public List<String> getPackages() {
            return packages;
        }

        public void setPackages(List<String> packages) {
            this.packages = packages;
        }
    }
}
