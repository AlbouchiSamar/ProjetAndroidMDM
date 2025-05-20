package com.hmdm.launcher.ui.Admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hmdm.launcher.R;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerService;
import com.hmdm.launcher.server.ServerServiceImpl;

public class RemoteControlFragment extends Fragment {

    private ImageView screenView;
    private Button refreshButton;
    private Button sendTapButton;
    private ProgressBar progressBar;

    private ServerApi serverService;
    private String deviceNumber; // À définir lors de la création du fragment

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote_control, container, false);
/*
        serverService = new ServerServiceImpl(requireContext());

        // Récupérer le numéro de l'appareil depuis les arguments
        if (getArguments() != null) {
            deviceNumber = getArguments().getString("device_number");
        }

        if (deviceNumber == null) {
            Toast.makeText(requireContext(), "Numéro d'appareil non spécifié", Toast.LENGTH_SHORT).show();
            // Gérer l'erreur, peut-être fermer le fragment
        }

        initViews(view);
        setupListeners();

        // Charger la première capture d'écran
        loadScreenshot();

        return view;
    }

    private void initViews(View view) {
        screenView = view.findViewById(R.id.image_screen);
        refreshButton = view.findViewById(R.id.btn_refresh);
        sendTapButton = view.findViewById(R.id.btn_send_tap);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        refreshButton.setOnClickListener(v -> loadScreenshot());

        sendTapButton.setOnClickListener(v -> {
            // Implémenter l'envoi d'une commande de tapotement
            // Vous devrez déterminer les coordonnées du tapotement
            sendRemoteCommand("tap", 100, 200); // Exemple
        });

        // Ajouter un écouteur pour détecter les tapotements sur l'image
        screenView.setOnTouchListener((v, event) -> {
            // Récupérer les coordonnées du tapotement
            float x = event.getX();
            float y = event.getY();
            // Envoyer la commande de tapotement au serveur
            sendRemoteCommand("tap", (int) x, (int) y);
            return true; // Indiquer que l'événement a été géré
        });
    }

    private void loadScreenshot() {
        if (deviceNumber == null) return;

        showProgress(true);

        serverService.getDeviceScreenshot(deviceNumber, bitmap -> {
            requireActivity().runOnUiThread(() -> {
                if (bitmap != null) {
                    screenView.setImageBitmap(bitmap);
                } else {
                    Toast.makeText(requireContext(), "Impossible de récupérer la capture d'écran", Toast.LENGTH_SHORT).show();
                }
                showProgress(false);
            });
        }, error -> {
            requireActivity().runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(requireContext(), "Erreur lors du chargement de la capture d'écran: " + error, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void sendRemoteCommand(String command, int x, int y) {
        if (deviceNumber == null) return;

        // Envoyer la commande au serveur
        serverService.sendRemoteControlCommand(deviceNumber, command, x, y, () -> {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Commande '" + command + "' envoyée", Toast.LENGTH_SHORT).show();
                // Rafraîchir l'écran après un court délai
                screenView.postDelayed(this::loadScreenshot, 500);
            });
        }, error -> {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Erreur lors de l'envoi de la commande: " + error, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Méthode statique pour créer une instance du fragment avec le numéro d'appareil
    public static RemoteControlFragment newInstance(String deviceNumber) {
        RemoteControlFragment fragment = new RemoteControlFragment();
        Bundle args = new Bundle();
        args.putString("device_number", deviceNumber);
        fragment.setArguments(args);
        return fragment;
    }
    */
        return view;

    }
}
