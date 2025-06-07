package com.hmdm.launcher.ui.Admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.hmdm.launcher.R;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;


import java.util.ArrayList;
import java.util.List;

public class AddDeviceFragment extends Fragment {

    private static final String TAG = "AddDeviceFragment";
    private EditText editNumber, editDescription, editImei, editPhone;
    private Spinner spinnerConfiguration;
    private Button btnAddDevice, btnChooseGroups;
    private TextView textSelectedGroups;
    private ServerApi serverService;
    private List<Configuration> configurationsList = new ArrayList<>();
    private List<GroupFragment.Group> groupsList = new ArrayList<>();
    private List<GroupFragment.Group> selectedGroups = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_device, container, false);

        // Initialisation des vues
        editNumber = view.findViewById(R.id.edit_number);
        editDescription = view.findViewById(R.id.edit_description);
        editImei = view.findViewById(R.id.edit_imei);
        editPhone = view.findViewById(R.id.edit_phone);
        spinnerConfiguration = view.findViewById(R.id.spinner_configuration);
        textSelectedGroups = view.findViewById(R.id.text_selected_groups);
        btnAddDevice = view.findViewById(R.id.btn_add_device);
        btnChooseGroups = view.findViewById(R.id.btn_choose_groups);

        // Initialisation du service
        serverService = new ServerServiceImpl(requireContext());

        // Charger les configurations et les groupes
        loadConfigurations();
        loadGroups();

        // Actions des boutons
        btnChooseGroups.setOnClickListener(v -> showChooseGroupsDialog());
        btnAddDevice.setOnClickListener(v -> addDevice());

        return view;
    }

    private void loadConfigurations() {
        serverService.getConfigurationsDevice(new ServerApi.GetConfigurationsCallback() {
            @Override
            public void onConfigurationList(List<Configuration> configurations) {
                requireActivity().runOnUiThread(() -> {
                    configurationsList.clear();
                    configurationsList.addAll(configurations);
                    List<String> configNames = new ArrayList<>();
                    for (Configuration config : configurationsList) {
                        configNames.add(config.getName() + " (ID: " + config.getId() + ")");
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_item, configNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerConfiguration.setAdapter(adapter);
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Erreur chargement configurations: " + error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Erreur chargement configurations: " + error);
                });
            }
        });
    }

    private void loadGroups() {
        serverService.getGroups(new ServerApi.GetGroupsCallback() {
            @Override
            public void onGroupList(List<GroupFragment.Group> groups) {
                requireActivity().runOnUiThread(() -> {
                    groupsList.clear();
                    groupsList.addAll(groups);
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Erreur chargement groupes: " + error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Erreur chargement groupes: " + error);
                });
            }
        });
    }

    private void showChooseGroupsDialog() {
        if (groupsList.isEmpty()) {
            Toast.makeText(requireContext(), "Aucun groupe disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        // Préparer les noms des groupes pour le dialogue
        String[] groupNames = new String[groupsList.size()];
        boolean[] checkedItems = new boolean[groupsList.size()];
        for (int i = 0; i < groupsList.size(); i++) {
            groupNames[i] = groupsList.get(i).getName() + " (ID: " + groupsList.get(i).getId() + ")";
            checkedItems[i] = selectedGroups.contains(groupsList.get(i)); // Pré-cocher les groupes déjà sélectionnés
        }

        // Afficher le dialogue de sélection multiple
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sélectionner les groupes")
                .setMultiChoiceItems(groupNames, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Valider", (dialog, which) -> {
                    selectedGroups.clear();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedGroups.add(groupsList.get(i));
                        }
                    }
                    // Mettre à jour le TextView avec les groupes sélectionnés
                    StringBuilder selectedText = new StringBuilder("Groupes sélectionnés : ");
                    if (selectedGroups.isEmpty()) {
                        selectedText.append("Aucun");
                    } else {
                        for (int i = 0; i < selectedGroups.size(); i++) {
                            selectedText.append(selectedGroups.get(i).getName());
                            if (i < selectedGroups.size() - 1) {
                                selectedText.append(", ");
                            }
                        }
                    }
                    textSelectedGroups.setText(selectedText.toString());
                })
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void addDevice() {
        String number = editNumber.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String imei = editImei.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        int configurationPosition = spinnerConfiguration.getSelectedItemPosition();
        int configurationId = (configurationPosition >= 0 && configurationPosition < configurationsList.size()) ?
                configurationsList.get(configurationPosition).getId() : 0;

        // Validation
        if (number.isEmpty() || description.isEmpty() || imei.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Tous les champs sont requis", Toast.LENGTH_SHORT).show();
            return;
        }
        if (imei.length() != 15 || !imei.matches("\\d+")) {
            Toast.makeText(requireContext(), "L'IMEI doit comporter exactement 15 chiffres", Toast.LENGTH_SHORT).show();
            return;
        }
        if (configurationId == 0) {
            Toast.makeText(requireContext(), "Veuillez sélectionner une configuration", Toast.LENGTH_SHORT).show();
            return;
        }

        // Appeler le service avec les groupes sélectionnés
        serverService.addDevice(number, description, configurationId, imei, phone, selectedGroups,
                new ServerApi.AddDeviceCallback() {
                    @Override
                    public void onSuccess() {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Appareil ajouté avec succès", Toast.LENGTH_SHORT).show();
                            clearFields();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Erreur ajout appareil: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Erreur ajout appareil: " + error);
                        });
                    }
                });
    }

    private void clearFields() {
        editNumber.setText("");
        editDescription.setText("");
        editImei.setText("");
        editPhone.setText("");
        spinnerConfiguration.setSelection(0);
        selectedGroups.clear();
        textSelectedGroups.setText("Groupes sélectionnés : Aucun");
    }

    // Classe interne pour représenter une configuration
    public static class Configuration {
        private int id;
        private String name;

        public Configuration(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}