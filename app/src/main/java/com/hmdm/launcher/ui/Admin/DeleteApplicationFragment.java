package com.hmdm.launcher.ui.Admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private static final String ARG_APPLICATION_ID = "application_id";
    private static final String ARG_APPLICATION_NAME = "application_name";

    private TextView textApplicationName;
    private Button btnDelete;
    private int applicationId;
    private String applicationName;
    private ServerApi serverService;
    private SettingsHelper settingsHelper;

    public static DeleteApplicationFragment newInstance(int id, String name) {
        DeleteApplicationFragment fragment = new DeleteApplicationFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_APPLICATION_ID, id);
        args.putString(ARG_APPLICATION_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            applicationId = getArguments().getInt(ARG_APPLICATION_ID);
            applicationName = getArguments().getString(ARG_APPLICATION_NAME);
        }
        settingsHelper = SettingsHelper.getInstance(requireContext());
        serverService = new ServerServiceImpl(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delete_application, container, false);

        textApplicationName = view.findViewById(R.id.text_application_name);
        btnDelete = view.findViewById(R.id.btn_delete);

        if (applicationName != null) {
            textApplicationName.setText("Supprimer : " + applicationName + " (ID: " + applicationId + ")");
        } else {
            textApplicationName.setText("Application non trouvée (ID: " + applicationId + ")");
        }

        btnDelete.setOnClickListener(v -> showConfirmationDialog());

        return view;
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmation de suppression")
                .setMessage("Êtes-vous sûr de vouloir supprimer l'application " + applicationName + " ? Cette action est irréversible.")
                .setPositiveButton("Oui", (dialog, which) -> deleteApplication())
                .setNegativeButton("Non", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteApplication() {
        if (getContext() == null) {
            Log.e(TAG, "Contexte null, impossible de supprimer l'application");
            return;
        }

        serverService.deleteApplication(
                applicationId,
                () -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(),"Application supprimée avec succès", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack(); // Retour au fragment précédent
                    }
                }),
                error -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur lors de la suppression de l'application : " + error);
                })
        );
    }
}