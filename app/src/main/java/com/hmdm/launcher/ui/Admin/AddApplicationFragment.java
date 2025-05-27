package com.hmdm.launcher.ui.Admin;

import android.app.Activity;
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
import androidx.fragment.app.Fragment;
import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AddApplicationFragment extends Fragment {
    private static final String TAG = "AddApplicationFragment";
    private static final int DEFAULT_VERSION_CODE = 0;
    private static final String DEFAULT_ARCH = "unknown";
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

    private Button selectFileButton, uploadButton;
    private TextView statusTextView;
    private ProgressBar progressBar;
    private Uri selectedFileUri;
    private ServerApi serverApi;
    private SettingsHelper settingsHelper;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
                            if (APK_MIME_TYPE.equals(fileType)) {
                                selectedFileUri = uri;
                                statusTextView.setText("Fichier sélectionné : " + getFileNameFromUri(uri));
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        String fileName = uri.getLastPathSegment();
        if (fileName == null) {
            fileName = "fichier_apk_" + System.currentTimeMillis() + ".apk";
        }
        return fileName;
    }

    private File uriToFile(Uri uri) throws IOException {
        File tempFile = new File(requireContext().getCacheDir(), "temp_apk_" + System.currentTimeMillis() + ".apk");
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return tempFile;
    }

    private void uploadApk() {
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText("Téléversement en cours...");

        try {
            File apkFile = uriToFile(selectedFileUri);
            serverApi.uploadApplicationFile(apkFile, (serverPath, pkg, name, version) -> {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    statusTextView.setText("Upload réussi : " + serverPath);
                    validatePackage(pkg, name, version, DEFAULT_VERSION_CODE, DEFAULT_ARCH, serverPath);
                });
            }, error -> {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusTextView.setText("Erreur upload : " + error);
                });
            });
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            statusTextView.setText("Erreur lors de la conversion du fichier : " + e.getMessage());
            Log.e(TAG, "Erreur conversion Uri en File", e);
        }
    }

    private void validatePackage(String pkg, String name, String version, int versionCode, String arch, String url) {
        serverApi.validatePackage(pkg, name, version, versionCode, arch, url, new ServerApi.ValidatePackageCallback() {
            @Override
            public void onValidatePackage(boolean isUnique, String validatedPkg) {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (isUnique) {
                        statusTextView.append("\nPackage validé, aucun doublon.");
                        createOrUpdateApp(0, validatedPkg, name, version, versionCode, arch, url);
                    } else {
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
                    progressBar.setVisibility(View.GONE);
                    statusTextView.append("\nErreur validation : " + error);
                });
            }
        });
    }

    private void createOrUpdateApp(int id, String pkg, String name, String version, int versionCode, String arch, String url) {
        serverApi.createOrUpdateApp(id, pkg, name, version, versionCode, arch, url, true, false, false, new ServerApi.CreateAppCallback() {
            @Override
            public void onCreateAppSuccess(String message, int applicationId) {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
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
                    progressBar.setVisibility(View.GONE);
                    statusTextView.append("\nErreur création application : " + error);
                });
            }
        });
    }

    private void updateConfigurations(int applicationId, String configName) {
        serverApi.updateConfigurations(applicationId, configName, new ServerApi.UpdateConfigCallback() {
            @Override
            public void onUpdateConfigSuccess(String message) {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusTextView.append("\nConfiguration mise à jour : " + message);
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded() || getActivity() == null) {
                    Log.w(TAG, "Fragment détaché, callback ignoré");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusTextView.append("\nErreur configuration : " + error);
                });
            }
        });
    }
}