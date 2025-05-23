package com.hmdm.launcher.ui.Admin;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.hmdm.launcher.R;
import com.hmdm.launcher.helper.SettingsHelper;

public class AdminMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private TextView headerUsername;

    private SettingsHelper settingsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        settingsHelper = SettingsHelper.getInstance(this);

        initViews();
        setupNavigation();

        // Charger le fragment de liste des appareils par défaut
        if (savedInstanceState == null) {
            loadFragment(new DeviceStatsFragment());
            navigationView.setCheckedItem(R.id.nav_dashboard);
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Configurer l'en-tête de navigation
        headerUsername = navigationView.getHeaderView(0).findViewById(R.id.text_username);
        headerUsername.setText(settingsHelper.getAdminUsername());
    }

    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_devices) {
            loadFragment(new DeviceListFragment());
        } else if (id == R.id.nav_configurations) {
            loadFragment(new ConfigurationListFragment());
        }else if (id == R.id.nav_dashboard) {
            loadFragment(new DeviceStatsFragment());

        }
       else if (id == R.id.nav_logs) {
            loadFragment(new LogsFragment());
        } else if (id == R.id.nav_logout) {
            logout();
        }/*else if (id == R.id.nav_add_device) {
            Intent intent = new Intent(this, AddDeviceActivity.class);
            startActivity(intent);
        }*/


        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void logout() {
        // Effacer les informations d'authentification
        settingsHelper.clearAdminAuth();

        // Rediriger vers l'écran de connexion
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
