package com.hmdm.launcher.ui.Admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;

public class ModifyDeviceFragment extends Fragment {
    private static final String TAG = "ModifyDeviceFragment";
    private EditText editDeviceName, editDeviceNumber, editConfigurationId;
    private Button saveButton;
    private ProgressBar progressBar;
    private ServerApi serverService;
    private SettingsHelper settingsHelper;
    private int deviceId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modify_device, container, false);
        serverService = new ServerServiceImpl(requireContext());
        settingsHelper = SettingsHelper.getInstance(requireContext());

        editDeviceName = view.findViewById(R.id.edit_device_name);
        editDeviceNumber = view.findViewById(R.id.edit_device_number);
        editConfigurationId = view.findViewById(R.id.edit_configuration_id);
        saveButton = view.findViewById(R.id.save_button);
        progressBar = view.findViewById(R.id.progress_bar);

        Bundle args = getArguments();
        if (args != null) {
            deviceId = args.getInt("deviceId", -1);
            if (deviceId == -1) {
                Toast.makeText(requireContext(), "ID de l'appareil non spécifié", Toast.LENGTH_LONG).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        } else {
            Toast.makeText(requireContext(), "Arguments manquants", Toast.LENGTH_LONG).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        }

        saveButton.setOnClickListener(v -> modifyDevice());

        return view;
    }

    private void modifyDevice() {
        String name = editDeviceName.getText().toString().trim();
        String number = editDeviceNumber.getText().toString().trim();
        String configIdStr = editConfigurationId.getText().toString().trim();

        if (name.isEmpty() || number.isEmpty() || configIdStr.isEmpty()) {
            Toast.makeText(requireContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        int configurationId;
        try {
            configurationId = Integer.parseInt(configIdStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "L'ID de configuration doit être un nombre", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        serverService.modifyDevice(
                deviceId,
                name,
                number,
                configurationId,
                message -> {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    });
                },
                error -> {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        saveButton.setEnabled(true);
                        Toast.makeText(requireContext(), "Erreur lors de la modification: " + error, Toast.LENGTH_LONG).show();
                    });
                }
        );
    }
}