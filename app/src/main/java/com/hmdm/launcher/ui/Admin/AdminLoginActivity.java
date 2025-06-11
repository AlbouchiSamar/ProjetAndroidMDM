package com.hmdm.launcher.ui.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.server.ServerApi;
import com.hmdm.launcher.server.ServerService;
import com.hmdm.launcher.server.ServerServiceImpl;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText usernameEdit;
    private EditText passwordEdit;
    private Button loginButton;
    private ProgressBar progressBar;

    private ServerApi serverService;
    private SettingsHelper settingsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        settingsHelper = SettingsHelper.getInstance(this);
        serverService = new ServerServiceImpl(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        usernameEdit = findViewById(R.id.edit_username);
        passwordEdit = findViewById(R.id.edit_password);
        loginButton = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(v -> {
            String username = usernameEdit.getText().toString().trim();
            String password = passwordEdit.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter a username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            attemptLogin(username, password);
        });
    }

    private void attemptLogin(String username, String password) {
        if (username.length() < 3 || password.length() < 6) {
            Toast.makeText(this, "Username must be at least 3 characters and password 6", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress(true);

        serverService.adminLogin(username, password, token -> {
            settingsHelper.setAdminAuthToken(token);

            runOnUiThread(() -> {
                showProgress(false);
                Intent intent = new Intent(AdminLoginActivity.this, AdminMainActivity.class);
                startActivity(intent);
                finish();
            });
        }, error -> {
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "Authentication failed: " + error, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
    }
}