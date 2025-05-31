package com.hmdm.launcher.ui.Admin;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AddApplicationFragment extends Fragment {
    private static final String TAG = "AddApplicationFragment";
    private static final int DEFAULT_VERSION_CODE = 0;
    private static final String DEFAULT_ARCH = "unknown";
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
    private static final long MAX_APK_SIZE = 100 * 1024 * 1024; // Limite de 100 Mo

    private Button selectFileButton, uploadButton;
    private TextView statusTextView;
    private ProgressBar progressBar;
    private Uri selectedFileUri;
    private ServerApi serverApi;
    private SettingsHelper settingsHelper;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsHelper = SettingsHelper.getInstance(requireContext());
        serverApi = new ServerServiceImpl(requireContext());

        // Vérifier l'authentification
        String token = settingsHelper.getAdminAuthToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "Veuillez vous connecter en tant qu'administrateur", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(requireContext(), AdminLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            Log.d(TAG, "URI sélectionné : " + uri.toString());
                            String fileType = requireContext().getContentResolver().getType(uri);
                            String fileName = getFileNameFromUri(uri);
                            if ((fileType != null && fileType.equals(APK_MIME_TYPE)) || (fileName != null && fileName.toLowerCase().endsWith(".apk"))) {
                                selectedFileUri = uri;
                                long fileSize = getFileSize(uri);
                                if (fileSize > 0 && fileSize <= MAX_APK_SIZE) {
                                    statusTextView.setText("Fichier sélectionné : " + fileName + " (Taille : " + (fileSize / 1024) + " KB)");
                                    Log.d(TAG, "Taille du fichier : " + fileSize + " octets");
                                } else {
                                    Toast.makeText(requireContext(), "Fichier trop grand ou invalide (max 100 Mo)", Toast.LENGTH_SHORT).show();
                                    selectedFileUri = null;
                                    statusTextView.setText("Statut : Fichier invalide");
                                }
                            } else {
                                Toast.makeText(requireContext(), "Veuillez sélectionner un fichier APK valide", Toast.LENGTH_SHORT).show();
                                selectedFileUri = null;
                            }
                        } else {
                            Log.e(TAG, "URI est null après sélection");
                            Toast.makeText(requireContext(), "Aucun fichier sélectionné", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "Résultat de sélection annulé ou invalide");
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_application, container, false);

        selectFileButton = view.findViewById(R.id.select_file_button);
        uploadButton = view.findViewById(R.id.upload_button);
        statusTextView = view.findViewById(R.id.status_text);
        progressBar = view.findViewById(R.id.progress_bar);

        progressBar.setVisibility(View.GONE);

        selectFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(APK_MIME_TYPE);
            filePickerLauncher.launch(intent);
        });

        uploadButton.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                uploadApk();
            } else {
                Toast.makeText(requireContext(), "Veuillez sélectionner un fichier APK", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        try {
            if (uri.getScheme().equals("content")) {
                String[] projection = {android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME};
                Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME);
                    fileName = cursor.getString(columnIndex);
                    cursor.close();
                }
            } else if (uri.getScheme().equals("file")) {
                fileName = uri.getLastPathSegment();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'extraction du nom de fichier : " + e.getMessage());
        }
        if (fileName == null) {
            fileName = "fichier_apk_" + System.currentTimeMillis() + ".apk";
        }
        return fileName;
    }

    private long getFileSize(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return -1;
            long size = inputStream.available();
            inputStream.close();
            return size;
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la récupération de la taille du fichier : " + e.getMessage());
            return -1;
        }
    }

    private File uriToFile(Uri uri) throws IOException {
        Log.d(TAG, "Début de la conversion de l'URI en fichier : " + uri.toString());
        File tempFile = new File(requireContext().getCacheDir(), "temp_apk_" + System.currentTimeMillis() + ".apk");
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            if (inputStream == null) {
                Log.e(TAG, "InputStream est null pour l'URI : " + uri.toString());
                throw new IOException("Impossible d'ouvrir l'InputStream pour l'URI");
            }
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            Log.d(TAG, "Fichier temporaire créé : " + tempFile.getAbsolutePath() + ", Taille : " + tempFile.length() + " octets");
        }
        return tempFile;
    }

    private void uploadApk() {
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText("Téléversement en cours...");

        try {
            File apkFile = uriToFile(selectedFileUri);
            if (apkFile.length() < 1024) {
                throw new IOException("Fichier APK trop petit, probablement invalide");
            }
            serverApi.uploadApplicationFile(apkFile, (serverPath, pkg, name, version) -> {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    Log.d(TAG, "Upload réussi - ServerPath: " + serverPath + ", Package: " + pkg + ", Nom: " + name + ", Version: " + version);
                    statusTextView.setText("Upload réussi : " + serverPath);
                    validatePackage(pkg, name, version, DEFAULT_VERSION_CODE, DEFAULT_ARCH, serverPath);
                });
            }, error -> {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Erreur lors de l'upload : " + error);
                    progressBar.setVisibility(View.GONE);
                    statusTextView.setText("Erreur upload : " + error);
                });
            });
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la conversion de l'URI en fichier : " + e.getMessage());
            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusTextView.setText("Erreur lors de la conversion du fichier : " + e.getMessage());
                });
            }
        }
    }

    private void validatePackage(String pkg, String name, String version, int versionCode, String arch, String url) {
        Log.d(TAG, "Validation du package - Pkg: " + pkg + ", Nom: " + name + ", Version: " + version);
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText("Validation en cours...");
        serverApi.validatePackage(pkg, name, version, versionCode, arch, url, new ServerApi.ValidatePackageCallback() {
            @Override
            public void onValidatePackage(boolean isUnique, String validatedPkg) {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (isUnique) {
                        Log.d(TAG, "Package validé, aucun doublon - Pkg: " + validatedPkg);
                        statusTextView.append("\nPackage validé, aucun doublon.");
                        createOrUpdateApp(0, validatedPkg, name, version, versionCode, arch, url);
                    } else {
                        Log.w(TAG, "Erreur : Package existe déjà - Pkg: " + validatedPkg);
                        progressBar.setVisibility(View.GONE);
                        statusTextView.append("\nErreur : Le package " + validatedPkg + " existe déjà.");
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Erreur lors de la validation : " + error);
                    progressBar.setVisibility(View.GONE);
                    statusTextView.append("\nErreur validation : " + error);
                });
            }
        });
    }

    private void createOrUpdateApp(int id, String pkg, String name, String version, int versionCode, String arch, String url) {
        Log.d(TAG, "Création/Mise à jour de l'application - ID: " + id + ", Pkg: " + pkg);
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText("Création/Mise à jour en cours...");
        serverApi.createOrUpdateApp(id, pkg, name, version, versionCode, arch, url, true, false, false,
                new ServerApi.CreateAppCallback() {
                    @Override
                    public void onCreateAppSuccess(String message, int applicationId) {
                        if (!isAdded() || getActivity() == null) {
                            Log.w(TAG, "Fragment détaché, callback ignoré");
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            Log.d(TAG, "Application créée/mise à jour avec succès - ID: " + applicationId + ", Message: " + message);
                            statusTextView.append("\nApplication créée/mise à jour : " + message);
                            updateConfigurations(applicationId, name);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded() || getActivity() == null) {
                            Log.w(TAG, "Fragment détaché, callback ignoré");
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            Log.e(TAG, "Erreur lors de la création/mise à jour : " + error);
                            progressBar.setVisibility(View.GONE);
                            statusTextView.append("\nErreur création application : " + error);
                        });
                    }
                });
    }

    private void updateConfigurations(int applicationId, String configName) {
        Log.d(TAG, "Mise à jour des configurations - App ID: " + applicationId + ", Config: " + configName);
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText("Mise à jour des configurations en cours...");
        serverApi.getApplicationConfigurations(applicationId, new ServerApi.GetConfigurationsCallback() {
            @Override
            public void onSuccess(List<AppConfiguration> configurations) {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                AppConfiguration configToUpdate = null;
                // Replace stream with a for loop for API 23 compatibility
                for (AppConfiguration config : configurations) {
                    if (config.getName().equals(configName)) {
                        configToUpdate = config;
                        break;
                    }
                }
                if (configToUpdate != null) {
                    // Call the correct updateConfigurations method with AppConfiguration and showIcon
                    serverApi.updateConfigurations(applicationId, configToUpdate, true, new ServerApi.UpdateConfigCallback() {
                        @Override
                        public void onUpdateConfigSuccess(String message) {
                            requireActivity().runOnUiThread(() -> {
                                Log.d(TAG, "Configuration mise à jour avec succès - Message: " + message);
                                progressBar.setVisibility(View.GONE);
                                statusTextView.append("\nConfiguration mise à jour : " + message);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            requireActivity().runOnUiThread(() -> {
                                Log.e(TAG, "Erreur lors de la mise à jour des configurations : " + error);
                                progressBar.setVisibility(View.GONE);
                                statusTextView.append("\nErreur configuration : " + error);
                            });
                        }
                    });
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Log.w(TAG, "Aucune configuration trouvée pour : " + configName);
                        progressBar.setVisibility(View.GONE);
                        statusTextView.append("\nErreur : Aucune configuration trouvée");
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Erreur lors de la récupération des configurations : " + error);
                    progressBar.setVisibility(View.GONE);
                    statusTextView.append("\nErreur configuration : " + error);
                });
            }
        });
    }
}