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

public class DeleteApplicationFragment extends Fragment {
    private static final String TAG = "DeleteApplicationFragment";
    private EditText editDeviceNumber;
    private EditText editApplicationId;
    private TextView textApplicationName;
    private Button btnSearch;
    private Button btnUninstall;
    private Button btnDelete;
    private ProgressBar progressBar;
    private int applicationId;
    private String applicationName;
    private String packageName;
    private String deviceNumber;
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

        editDeviceNumber = view.findViewById(R.id.edit_device_number);
        editApplicationId = view.findViewById(R.id.edit_application_id);
        textApplicationName = view.findViewById(R.id.text_application_name);
        btnSearch = view.findViewById(R.id.btn_search);
        btnUninstall = view.findViewById(R.id.btn_uninstall);
        btnDelete = view.findViewById(R.id.btn_delete);
        progressBar = view.findViewById(R.id.progress_bar);

        textApplicationName.setText("Aucune application sélectionnée");
        btnUninstall.setEnabled(false);
        btnDelete.setEnabled(false);
        progressBar.setVisibility(View.GONE);
        isOperationInProgress = false;

        btnSearch.setOnClickListener(v -> searchApplication());
        btnUninstall.setOnClickListener(v -> showUninstallConfirmationDialog());
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());

        return view;
    }

    private void searchApplication() {
        if (isOperationInProgress) {
            Log.d(TAG, "Opération en cours, recherche ignorée");
            return;
        }

        deviceNumber = editDeviceNumber.getText().toString().trim();
        String idText = editApplicationId.getText().toString().trim();

        if (deviceNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Veuillez entrer un numéro d'appareil", Toast.LENGTH_SHORT).show();
            return;
        }
        if (idText.isEmpty()) {
            Toast.makeText(requireContext(), "Veuillez entrer un ID d'application", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            applicationId = Integer.parseInt(idText);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "ID d'application invalide", Toast.LENGTH_SHORT).show();
            return;
        }

        isOperationInProgress = true;
        btnSearch.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        textApplicationName.setText("Recherche en cours...");

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
                            packageName = application.getPkg();
                            textApplicationName.setText("Application : " + applicationName + " (ID: " + applicationId + ")");
                            btnUninstall.setEnabled(true);
                            btnDelete.setEnabled(true);
                            btnSearch.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                            isOperationInProgress = false;
                            Log.d(TAG, "Application trouvée: " + applicationName + ", Pkg: " + packageName);
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
                            btnUninstall.setEnabled(false);
                            btnDelete.setEnabled(false);
                            btnSearch.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                            isOperationInProgress = false;
                            Toast.makeText(requireContext(), "Erreur: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Erreur recherche application: " + error);
                        });
                    }
                }
        );
    }

    private void showUninstallConfirmationDialog() {
        if (isOperationInProgress) {
            Log.d(TAG, "Opération en cours, désinstallation ignorée");
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmation de désinstallation")
                .setMessage("Voulez-vous désinstaller " + applicationName + " de l'appareil " + deviceNumber + " ?")
                .setPositiveButton("Oui", (dialog, which) -> uninstallApplication())
                .setNegativeButton("Non", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void uninstallApplication() {
        if (isOperationInProgress) {
            Log.d(TAG, "Opération en cours, désinstallation ignorée");
            return;
        }
        isOperationInProgress = true;
        btnUninstall.setEnabled(false);
        btnDelete.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        serverService.uninstallApp(
                deviceNumber,
                packageName,
                () -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, callback ignoré");
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Application désinstallée avec succès", Toast.LENGTH_SHORT).show();
                        btnUninstall.setEnabled(true);
                        btnDelete.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        isOperationInProgress = false;
                        Log.d(TAG, "Désinstallation réussie: " + packageName + " sur " + deviceNumber);
                    });
                },
                error -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, callback ignoré");
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        btnUninstall.setEnabled(true);
                        btnDelete.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        isOperationInProgress = false;
                        Toast.makeText(requireContext(), "Erreur: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Erreur désinstallation: " + error);
                    });
                }
        );
    }

    private void showDeleteConfirmationDialog() {
        if (isOperationInProgress) {
            Log.d(TAG, "Opération en cours, suppression ignorée");
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmation de suppression")
                .setMessage("Voulez-vous supprimer l'application " + applicationName + " ? Cette action est irréversible.")
                .setPositiveButton("Oui", (dialog, which) -> deleteApplication())
                .setNegativeButton("Non", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteApplication() {
        if (isOperationInProgress) {
            Log.d(TAG, "Opération en cours, suppression ignorée");
            return;
        }
        isOperationInProgress = true;
        btnUninstall.setEnabled(false);
        btnDelete.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        serverService.deleteApplication(
                applicationId,
                () -> {
                    if (!isAdded() || getActivity() == null) {
                        Log.w(TAG, "Fragment détaché, callback ignoré");
                        return;
                    }
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Application supprimée avec succès", Toast.LENGTH_SHORT).show();
                        getActivity().getSupportFragmentManager().popBackStack();
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
                        btnUninstall.setEnabled(true);
                        btnDelete.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        isOperationInProgress = false;
                        Toast.makeText(requireContext(), "Erreur: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Erreur suppression application: " + error);
                    });
                }
        );
    }
}