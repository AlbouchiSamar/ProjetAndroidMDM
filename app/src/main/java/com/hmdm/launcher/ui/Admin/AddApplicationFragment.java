package com.hmdm.launcher.ui.Admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class AddApplicationFragment extends Fragment {
    private static final String TAG = "AddApplicationFragment";

    private TextView textSelectedFile;
    private Button btnSelectFile;
    private Button btnAdd;
    private ProgressBar progressBar;
    private File selectedFile;
    private ServerApi serverService;
    private SettingsHelper settingsHelper;

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleFileSelection(uri);
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsHelper = SettingsHelper.getInstance(requireContext());
        serverService = new ServerServiceImpl(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_application, container, false);

        textSelectedFile = view.findViewById(R.id.text_selected_file);
        btnSelectFile = view.findViewById(R.id.btn_select_file);
        btnAdd = view.findViewById(R.id.btn_add);
        progressBar = view.findViewById(R.id.progress_bar);

        btnSelectFile.setOnClickListener(v -> openFilePicker());
        btnAdd.setOnClickListener(v -> addApplication());

        return view;
    }

    private void openFilePicker() {
        filePickerLauncher.launch("application/vnd.android.package-archive");
    }

    private File getFileFromUri(Uri uri) {
        try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri)) {
            File tempFile = File.createTempFile("upload", ".apk", getContext().getCacheDir());
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la conversion du URI en fichier", e);
            return null;
        }
    }

    private void handleFileSelection(Uri uri) {
        try {
            selectedFile = getFileFromUri(uri);
            if (selectedFile != null && selectedFile.exists()) {
                textSelectedFile.setText("Fichier sélectionné : " + selectedFile.getName());
            } else {
                selectedFile = null;
                textSelectedFile.setText("Aucun fichier sélectionné");
                Toast.makeText(getContext(), "Fichier introuvable", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la sélection du fichier", e);
            selectedFile = null;
            textSelectedFile.setText("Aucun fichier sélectionné");
            Toast.makeText(getContext(), "Erreur lors de la sélection du fichier", Toast.LENGTH_LONG).show();
        }
    }

    private void addApplication() {
        if (getContext() == null) {
            Log.e(TAG, "Contexte null, impossible d'ajouter l'application");
            return;
        }

        if (selectedFile == null) {
            Toast.makeText(getContext(), "Veuillez sélectionner un fichier APK", Toast.LENGTH_SHORT).show();
            return;
        }

        // Désactiver le bouton et afficher la barre de progression
        btnAdd.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        serverService.uploadApplicationFile(
                selectedFile,
                (serverPath, pkg, name, version) -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(), "Application ajoutée : " + name + " (" + pkg + ")", Toast.LENGTH_SHORT).show();
                    textSelectedFile.setText("Aucun fichier sélectionné");
                    if (selectedFile != null) {
                        selectedFile.delete(); // Supprimer le fichier temporaire
                        selectedFile = null;
                    }
                    btnAdd.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                }),
                error -> getActivity().runOnUiThread(() -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Erreur lors de l'ajout de l'application : " + error);
                    btnAdd.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                })
        );
    }
}