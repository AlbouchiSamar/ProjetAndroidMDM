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
                Toast.makeText(this, "Veuillez saisir un nom d'utilisateur et un mot de passe", Toast.LENGTH_SHORT).show();
                return;
            }

            attemptLogin(username, password);
        });
    }

    private void attemptLogin(String username, String password) {
        if (username.length() < 3 || password.length() < 5
        ) {
            Toast.makeText(this, "Le nom d'utilisateur doit avoir au moins 3 caractères et le mot de passe 6", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgress(true);

        serverService.adminLogin(username, password, token -> {
            // Stocker le token d'authentification
            settingsHelper.setAdminAuthToken(token);

            runOnUiThread(() -> {
                showProgress(false);
                // Rediriger vers l'écran principal d'administration
                Intent intent = new Intent(AdminLoginActivity.this, AdminMainActivity.class);
                startActivity(intent);
                finish();
            });
        }, error -> {
            runOnUiThread(() -> {
                showProgress(false);
                Toast.makeText(this, "Échec de l'authentification: " + error, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
    }
}
