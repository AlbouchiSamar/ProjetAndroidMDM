/*
 * Headwind MDM: Open Source Android MDM Software
 * https://h-mdm.com
 *
 * Copyright (C) 2019 Headwind Solutions LLC (http://h-sms.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hmdm.launcher;

import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE;
import static android.content.Context.MODE_PRIVATE;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.json.DeviceEnrollOptions;
import com.hmdm.launcher.util.PreferenceLogger;

/**
 * Created by Ivan Lozenko on 21.02.2017.
 */

public class AdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "AdminReceiver";
    private static final String PREFS_NAME = "com.hmdm.launcher.prefs";
    @Override
    public void onEnabled(Context context, Intent intent) {
        // We come here after both successful provisioning and manual activation of the device owner
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences( Const.PREFERENCES, MODE_PRIVATE );
        PreferenceLogger.log(preferences, "Administrator enabled");
        preferences.edit().putInt(Const.PREFERENCES_ADMINISTRATOR, Const.PREFERENCES_ON).commit();
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences( Const.PREFERENCES, MODE_PRIVATE );
        PreferenceLogger.log(preferences, "Profile provisioning complete");

        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ) {
            // This function is never called on Android versions less than 5 (in fact, less than 7)
            return;
        }

        PersistableBundle bundle = intent.getParcelableExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);
        updateSettings(context, bundle);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void updateSettings(Context context, PersistableBundle bundle) {
        SharedPreferences preferences = context.getApplicationContext().getSharedPreferences( Const.PREFERENCES, MODE_PRIVATE );
        try {
            SettingsHelper settingsHelper = SettingsHelper.getInstance(context.getApplicationContext());
            String deviceId = null;
            PreferenceLogger.log(preferences, "Bundle != null: " + (bundle != null));
            if (bundle != null) {
                deviceId = bundle.getString(Const.QR_DEVICE_ID_ATTR, null);
                if (deviceId == null) {
                    // Also let's try legacy attribute
                    deviceId = bundle.getString(Const.QR_LEGACY_DEVICE_ID_ATTR, null);
                }
                if (deviceId == null) {
                    String deviceIdUse = bundle.getString(Const.QR_DEVICE_ID_USE_ATTR, null);
                    if (deviceIdUse != null) {
                        PreferenceLogger.log(preferences, "deviceIdUse: " + deviceIdUse);
                        // Save for further automatic choice of the device ID
                        settingsHelper.setDeviceIdUse(deviceIdUse);
                    }
                }
            }
            if (deviceId != null) {
                // Device ID is delivered in the QR code!
                // Added: "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {"com.hmdm.DEVICE_ID": "(device id)"}
                PreferenceLogger.log(preferences, "DeviceID: " + deviceId);
                settingsHelper.setDeviceId(deviceId);
            }

            String baseUrl = null;
            String secondaryBaseUrl = null;
            String serverProject = null;
            DeviceEnrollOptions createOptions = new DeviceEnrollOptions();
            if (bundle != null) {
                baseUrl = bundle.getString(Const.QR_BASE_URL_ATTR, null);
                secondaryBaseUrl = bundle.getString(Const.QR_SECONDARY_BASE_URL_ATTR, null);
                serverProject = bundle.getString(Const.QR_SERVER_PROJECT_ATTR, null);
                createOptions.setCustomer(bundle.getString(Const.QR_CUSTOMER_ATTR, null));
                createOptions.setConfiguration(bundle.getString(Const.QR_CONFIG_ATTR, null));
                createOptions.setGroups(bundle.getString(Const.QR_GROUP_ATTR, null));
                if (baseUrl != null) {
                    PreferenceLogger.log(preferences, "BaseURL: " + baseUrl);
                    settingsHelper.setBaseUrl(baseUrl);
                    // If we don't set the secondary base URL, it will point to app.h-mdm.com by default which is wrong
                    if (secondaryBaseUrl == null) {
                        secondaryBaseUrl = baseUrl;
                    }
                }
                if (secondaryBaseUrl != null) {
                    PreferenceLogger.log(preferences, "SecondaryBaseURL: " + secondaryBaseUrl);
                    settingsHelper.setSecondaryBaseUrl(secondaryBaseUrl);
                }
                if (serverProject != null) {
                    PreferenceLogger.log(preferences, "ServerPath: " + serverProject);
                    settingsHelper.setServerProject(serverProject);
                }
                if (createOptions.getCustomer() != null) {
                    PreferenceLogger.log(preferences, "Customer: " + createOptions.getCustomer());
                    settingsHelper.setEnrollOptionCustomer(createOptions.getCustomer());
                }
                if (createOptions.getConfiguration() != null) {
                    PreferenceLogger.log(preferences, "Configuration: " + createOptions.getConfiguration());
                    settingsHelper.setEnrollOptionConfigName(createOptions.getConfiguration());
                }
                if (createOptions.getGroups() != null) {
                    PreferenceLogger.log(preferences, "Groups: " + bundle.getString(Const.QR_GROUP_ATTR));
                    settingsHelper.setEnrollOptionGroup(createOptions.getGroupSet());
                }
                settingsHelper.setQrProvisioning(true);
            }
        } catch (Exception e) {
            // Ignored
            e.printStackTrace();
            PreferenceLogger.printStackTrace(preferences, e);
        }
    }

    public void handleWipeData(Context context, String wipeType) {
        try {
            // Validate wipe type
            if (wipeType == null || !wipeType.equals("FACTORY_RESET")) {
                Log.w(TAG, "Invalid wipe type: " + wipeType);
                return;
            }

            // Secure logging
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences preferences = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context.getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            preferences.edit()
                    .putString("last_wipe", "Effacement effectué à " + System.currentTimeMillis())
                    .apply();
            Log.i(TAG, "Wipe data triggered for type: " + wipeType);

            // Perform FACTORY_RESET
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(context, AdminReceiver.class);
            if (dpm.isDeviceOwnerApp(context.getPackageName())) {
                dpm.wipeData(0); // 0 for full FACTORY_RESET
            } else {
                Log.e(TAG, "Application is not Device Owner");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during wipe data", e);
        }
    }

}
