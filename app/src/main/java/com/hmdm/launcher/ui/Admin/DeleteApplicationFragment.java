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
    private EditText editApplicationId;
    private TextView textApplicationName;
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
        btnSearch = view.findViewById(R.id.btn_search);
        btnDelete = view.findViewById(R.id.btn_delete);
        progressBar = view.findViewById(R.id.progress_bar);

        textApplicationName.setText("Aucune application sélectionnée");
        btnDelete.setEnabled(false);
        progressBar.setVisibility(View.GONE);
        isOperationInProgress = false;

        btnSearch.setOnClickListener(v -> searchApplication());
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());

        return view;
    }

    private void searchApplication() {
        if (isOperationInProgress) {
            Log.d(TAG, "Opération en cours, recherche ignorée");
            return;
        }

        String idText = editApplicationId.getText().toString().trim();

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
                            textApplicationName.setText("Application : " + applicationName + " (ID: " + applicationId + ")");
                            btnDelete.setEnabled(true);
                            btnSearch.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                            isOperationInProgress = false;
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
                            Toast.makeText(requireContext(), "Erreur: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Erreur recherche application: " + error);
                        });
                    }
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
        btnDelete.setEnabled(false);
        btnSearch.setEnabled(false);
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
                        btnDelete.setEnabled(true);
                        btnSearch.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        isOperationInProgress = false;
                        Toast.makeText(requireContext(), "Erreur: " + error, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Erreur suppression application: " + error);
                    });
                }
        );
    }
}