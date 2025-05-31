package com.hmdm.launcher.ui.Admin;

import android.app.AlertDialog;
import android.os.Bundle;
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
import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;
import java.util.ArrayList;
import java.util.List;

public class DeleteApplicationFragment extends Fragment {
    private static final String TAG = "DeleteApplicationFragment";
    private EditText editApplicationId;
    private TextView textApplicationName;
    private TextView textConfigurations;
    private Button btnSearch;
    private Button btnDelete;
    private ProgressBar progressBar;
    private int applicationId;
    private String applicationName;
    private ServerApi serverService;
    private SettingsHelper settingsHelper;
    private boolean isOperationInProgress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsHelper = SettingsHelper.getInstance(requireContext());
        serverService = new ServerServiceImpl(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView appelé");
        View view = inflater.inflate(R.layout.fragment_delete_application, container, false);

        editApplicationId = view.findViewById(R.id.edit_application_id);
        textApplicationName = view.findViewById(R.id.text_application_name);
        textConfigurations = view.findViewById(R.id.text_configurations);
        btnSearch = view.findViewById(R.id.btn_search);
        btnDelete = view.findViewById(R.id.btn_delete);
        progressBar = view.findViewById(R.id.progress_bar);

        resetForm(); // Réinitialisation initiale
        btnDelete.setEnabled(false);
        progressBar.setVisibility(View.GONE);
        isOperationInProgress = false;

        btnSearch.setOnClickListener(v -> searchApplication());
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());

        return view;
    }

    private void resetForm() {
        editApplicationId.setText("");
        textApplicationName.setText("Aucune application sélectionnée");
        textConfigurations.setText("Aucune configuration associée");
        btnDelete.setEnabled(false);
        applicationId = 0;
        applicationName = null;
    }

    private void searchApplication() {
        if (isOperationInProgress) {
            Log.d(TAG, "Opération en cours, recherche ignorée");
            Toast.makeText(requireContext(), "Opération en cours, veuillez attendre", Toast.LENGTH_SHORT).show();
            return;
        }

        String idText = editApplicationId.getText().toString().trim();

        if (idText.isEmpty()) {
            Toast.makeText(requireContext(), "Veuillez entrer un ID d'application", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            applicationId = Integer.parseInt(idText);
            if (applicationId <= 0) {
                Toast.makeText(requireContext(), "L'ID d'application doit être un nombre positif", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "ID d'application invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        isOperationInProgress = true;
        btnSearch.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        textApplicationName.setText("Recherche en cours...");
        textConfigurations.setText("Aucune configuration associée");
        Toast.makeText(requireContext(), "Recherche de l'application ID: " + applicationId, Toast.LENGTH_SHORT).show();

        serverService.getApplicationById(
                applicationId,
                new ServerApi.GetApplicationIdCallback() {
                    @Override
                    public void onSuccess(ApplicationListFragment.Application application) {
                        if (!isAdded() || getActivity() == null) {
                            Log.w(TAG, "Fragment détaché, callback ignoré");
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            applicationName = application.getName();
                            textApplicationName.setText("Application : " + applicationName + " (ID: " + applicationId + ")");
                            btnDelete.setEnabled(true);
                            btnSearch.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                            isOperationInProgress = false;
                            Toast.makeText(requireContext(), "Application trouvée: " + applicationName, Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Application trouvée: " + applicationName);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || getActivity() == null) {
                            Log.w(TAG, "Fragment détaché, callback ignoré");
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            textApplicationName.setText("Application non trouvée (ID: " + applicationId + ")");
                            btnDelete.setEnabled(false);
                            btnSearch.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                            isOperationInProgress = false;
                            Toast.makeText(requireContext(), "Erreur lors de la recherche: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Erreur recherche application: " + error);
                        });
                    }
                }
        );
    }

    private void showDeleteConfirmationDialog() {
        if (isOperationInProgress) {
            Log.d(TAG, "Opération en cours, suppression ignorée");
            Toast.makeText(requireContext(), "Opération en cours, veuillez attendre", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmation de suppression")
                .setMessage("Voulez-vous supprimer l'application " + applicationName + " ? Cette action est irréversible.")
                .setPositiveButton("Oui", (dialog, which) -> checkAndDeleteApplication())
                .setNegativeButton("Non", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void checkAndDeleteApplication() {
        if (isOperationInProgress) {
            Log.d(TAG, "Opération en cours, vérification ignorée");
            Toast.makeText(requireContext(), "Opération en cours, veuillez attendre", Toast.LENGTH_SHORT).show();
            return;
        }
        isOperationInProgress = true;
        btnDelete.setEnabled(false);
        btnSearch.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(requireContext(), "Vérification des configurations associées...", Toast.LENGTH_SHORT).show();

        serverService.getApplicationConfigurationsDelet(
                applicationId,
                configurations -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, callback ignoré");
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        if (configurations.isEmpty()) {
                            textConfigurations.setText("Aucune configuration associée");
                            Toast.makeText(requireContext(), "Aucune configuration associée, suppression en cours...", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Aucune configuration associée, suppression directe");
                            deleteApplication();
                        } else {
                            StringBuilder configNames = new StringBuilder("Configurations associées : ");
                            for (ApplicationConfiguration config : configurations) {
                                configNames.append(config.getConfigurationName()).append(", ");
                            }
                            String configText = configNames.toString();
                            textConfigurations.setText(configText.substring(0, configText.length() - 2));
                            Toast.makeText(requireContext(), "Configurations associées trouvées: " + configurations.size(), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Configurations associées trouvées: " + configurations.size());
                            updateConfigurations(configurations);
                        }
                    });
                },
                error -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, callback ignoré");
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        btnDelete.setEnabled(true);
                        btnSearch.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        isOperationInProgress = false;
                        Toast.makeText(requireContext(), "Erreur lors de la vérification des configurations: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Erreur vérification configurations: " + error);
                        // Ajout d'une option pour réessayer
                        showRetryDialog("Erreur lors de la vérification des configurations", this::checkAndDeleteApplication);
                    });
                }
        );
    }

    private void updateConfigurations(List<ApplicationConfiguration> configurations) {
        List<ApplicationConfiguration> updatedConfigs = new ArrayList<>();
        for (ApplicationConfiguration config : configurations) {
            updatedConfigs.add(new ApplicationConfiguration(config.getId(), config.getConfigurationId(), config.getConfigurationName(), true));
        }

        ServerApi.ApplicationConfigurationsUpdateRequest request = new ServerApi.ApplicationConfigurationsUpdateRequest(applicationId, updatedConfigs);

        Toast.makeText(requireContext(), "Mise à jour des configurations (marquer pour suppression)...", Toast.LENGTH_SHORT).show();
        serverService.updateApplicationConfigurations(
                request,
                () -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, callback ignoré");
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Configurations mises à jour avec succès", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Configurations mises à jour, suppression de l'application");
                        deleteApplication();
                    });
                },
                error -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, callback ignoré");
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        btnDelete.setEnabled(true);
                        btnSearch.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        isOperationInProgress = false;
                        Toast.makeText(requireContext(), "Erreur lors de la mise à jour des configurations: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Erreur mise à jour configurations: " + error);
                        // Ajout d'une option pour réessayer
                        showRetryDialog("Erreur lors de la mise à jour des configurations", () -> updateConfigurations(configurations));
                    });
                }
        );
    }

    private void deleteApplication() {
        Toast.makeText(requireContext(), "Suppression de l'application ID: " + applicationId, Toast.LENGTH_SHORT).show();
        serverService.deleteApplication(
                applicationId,
                () -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, callback ignoré");
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Application supprimée et désinstallation en cours...", Toast.LENGTH_LONG).show();
                        resetForm(); // Réinitialisation au lieu de popBackStack
                        progressBar.setVisibility(View.GONE);
                        isOperationInProgress = false;
                        Log.d(TAG, "Suppression réussie: ID=" + applicationId);
                    });
                },
                error -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, callback ignoré");
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        btnDelete.setEnabled(true);
                        btnSearch.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        isOperationInProgress = false;
                        Toast.makeText(requireContext(), "Erreur lors de la suppression: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Erreur suppression application: " + error);
                        // Ajout d'une option pour réessayer
                        showRetryDialog("Erreur lors de la suppression de l'application", this::deleteApplication);
                    });
                }
        );
    }

    private void showRetryDialog(String message, Runnable retryAction) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Erreur")
                .setMessage(message + ". Voulez-vous réessayer ?")
                .setPositiveButton("Réessayer", (dialog, which) -> retryAction.run())
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public static class ApplicationConfiguration {
        private int id;
        private int configurationId;
        private String configurationName;
        private boolean remove;

        public ApplicationConfiguration(int id, int configurationId, String configurationName, boolean remove) {
            this.id = id;
            this.configurationId = configurationId;
            this.configurationName = configurationName;
            this.remove = remove;
        }

        public ApplicationConfiguration(int id, int configurationId, String configurationName) {
            this(id, configurationId, configurationName, false);
        }

        public int getId() { return id; }
        public int getConfigurationId() { return configurationId; }
        public String getConfigurationName() { return configurationName; }
        public boolean isRemove() { return remove; }
    }
}