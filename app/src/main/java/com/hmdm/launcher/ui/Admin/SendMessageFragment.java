package com.hmdm.launcher.ui.Admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerServiceImpl;
import com.hmdm.launcher.ui.Admin.adapter.MessageListAdapter;

import java.util.ArrayList;
import java.util.List;

public class SendMessageFragment extends Fragment {
    private static final String TAG = "SendMessageFragment";
    private Spinner spinnerRecipientType;
    private EditText editRecipientId, editMessage;
    private Button btnSend;
    private ProgressBar progressBar;
    private RecyclerView recyclerViewMessages;
    private MessageListAdapter messageAdapter;
    private ServerApi serverService;
    private SettingsHelper settingsHelper;
    private List<Message> messages = new ArrayList<>();

    public static class Message {
        private int id;
        private String deviceNumber;
        private long timestamp;
        private String messageText;
        private int status;

        public Message(int id, String deviceNumber, long timestamp, String messageText, int status) {
            this.id = id;
            this.deviceNumber = deviceNumber;
            this.timestamp = timestamp;
            this.messageText = messageText;
            this.status = status;
        }

        public int getId() { return id; }
        public String getDeviceNumber() { return deviceNumber; }
        public long getTimestamp() { return timestamp; }
        public String getMessageText() { return messageText; }
        public int getStatus() { return status; }
        public String getStatusText() { return status == 0 ? "Sent" : "Read"; }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverService = new ServerServiceImpl(requireContext());
        settingsHelper = SettingsHelper.getInstance(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send_message, container, false);

        spinnerRecipientType = view.findViewById(R.id.spinner_recipient_type);
        editRecipientId = view.findViewById(R.id.edit_recipient_id);
        editMessage = view.findViewById(R.id.edit_message);
        btnSend = view.findViewById(R.id.btn_send);
        progressBar = view.findViewById(R.id.progress_bar);
        recyclerViewMessages = view.findViewById(R.id.recycler_view_messages);

        // Configuration du Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.recipient_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRecipientType.setAdapter(adapter);
        spinnerRecipientType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAutoCompleteSuggestions();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ne rien faire
            }
        });

        // Configuration du RecyclerView
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(requireContext()));
        messageAdapter = new MessageListAdapter(messages);
        recyclerViewMessages.setAdapter(messageAdapter);

        // Actions
        btnSend.setOnClickListener(v -> sendMessage());
        loadMessageHistory();

        return view;
    }

    private void updateAutoCompleteSuggestions() {
        String recipientType = spinnerRecipientType.getSelectedItem().toString();
        if ("Device".equals(recipientType)) {
            editRecipientId.setHint("Enter device number");
            editRecipientId.setEnabled(true);
            editRecipientId.setText("");
        } else if ("Group".equals(recipientType)) {
            editRecipientId.setHint("Enter group ID");
            editRecipientId.setEnabled(true);
            editRecipientId.setText("");
        } else if ("Configuration".equals(recipientType)) {
            editRecipientId.setHint("Enter configuration ID");
            editRecipientId.setEnabled(true);
            editRecipientId.setText("");
        } else if ("All Devices".equals(recipientType)) {
            editRecipientId.setHint("");
            editRecipientId.setEnabled(false);
            editRecipientId.setText("");
        }
        // Pas d'auto-complétion, donc pas besoin d'ArrayAdapter
    }

    private void sendMessage() {
        String recipientType = spinnerRecipientType.getSelectedItem().toString();
        String recipientId = editRecipientId.getText().toString().trim();
        String message = editMessage.getText().toString().trim();

        if (message.isEmpty()) {
            showAlert("Error", "The message cannot be empty");
            return;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        btnSend.setEnabled(false);

        String scope = "";
        String deviceNumber = "";
        int groupId = 0;
        int configurationId = 0;

        switch (recipientType) {
            case "Device":
                if (recipientId.isEmpty()) {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    btnSend.setEnabled(true);
                    showAlert("Error", "Enter a device number");
                    return;
                }
                scope = "device";
                deviceNumber = recipientId;
                break;
            case "Group":
                if (recipientId.isEmpty()) {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    btnSend.setEnabled(true);
                    showAlert("Error", "Enter a group ID");
                    return;
                }
                try {
                    groupId = Integer.parseInt(recipientId);
                    scope = "group";
                    deviceNumber = ""; // Not needed for group
                } catch (NumberFormatException e) {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    btnSend.setEnabled(true);
                    showAlert("Error", "Invalid group ID");
                    return;
                }
                break;
            case "Configuration":
                if (recipientId.isEmpty()) {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    btnSend.setEnabled(true);
                    showAlert("Error", "Enter a configuration ID");
                    return;
                }
                try {
                    configurationId = Integer.parseInt(recipientId);
                    scope = "configuration";
                    deviceNumber = ""; // Not needed for configuration
                } catch (NumberFormatException e) {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    btnSend.setEnabled(true);
                    showAlert("Error", "Invalid configuration ID");
                    return;
                }
                break;
            case "All Devices":
                scope = "all";
                deviceNumber = "";
                break;
        }

        serverService.sendMessage(scope, deviceNumber, groupId, configurationId, message,
                new ServerApi.SendMessageCallback() {
                    @Override
                    public void onSuccess(String message) {
                        requireActivity().runOnUiThread(() -> {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            btnSend.setEnabled(true);
                            Toast.makeText(requireContext(), "Message sent successfully", Toast.LENGTH_SHORT).show();
                            editMessage.setText("");
                            loadMessageHistory();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            btnSend.setEnabled(true);
                            showAlert("Error", "Failed to send message: " + error);
                            Log.e(TAG, "Error sending message: " + error);
                        });
                    }
                });
    }

    private void showAlert(String title, String message) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_custom_alertmessag, null);
        TextView alertTitle = dialogView.findViewById(R.id.alert_title);
        TextView alertMessage = dialogView.findViewById(R.id.alert_message);
        Button btnNegative = dialogView.findViewById(R.id.btn_negative);
        Button btnPositive = dialogView.findViewById(R.id.btn_positive);

        alertTitle.setText(title);
        alertMessage.setText(message);
        btnPositive.setText("OK");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        btnNegative.setTag(dialog);
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        btnPositive.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadMessageHistory() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        serverService.getMessageHistory(
                messages -> requireActivity().runOnUiThread(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    this.messages.clear();
                    this.messages.addAll(messages);
                    messageAdapter.updateMessages(messages);
                }),
                error -> requireActivity().runOnUiThread(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    showAlert("Error", "Failed to load message history: " + error);
                    Log.e(TAG, "Error loading message history: " + error);
                })
        );
    }
}