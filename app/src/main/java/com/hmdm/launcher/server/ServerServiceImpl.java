package com.hmdm.launcher.server;

import static org.apache.commons.io.filefilter.FileFileFilter.FILE;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.ui.Admin.AddDeviceFragment;
import com.hmdm.launcher.ui.Admin.AdminLoginActivity;
import com.hmdm.launcher.ui.Admin.ApplicationListFragment;
import com.hmdm.launcher.ui.Admin.AuditFragment;
import com.hmdm.launcher.ui.Admin.Configuration;
import com.hmdm.launcher.ui.Admin.ConfigurationListFragment;

import com.hmdm.launcher.ui.Admin.DeviceListFragment;
import com.hmdm.launcher.ui.Admin.GroupFragment;
import com.hmdm.launcher.ui.Admin.SendMessageFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Implementation of ServerApi for communication with the Headwind MDM server.
 */
public class ServerServiceImpl implements ServerApi {
    private static final String TAG = "ServerServiceImpl";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType FILE = MediaType.get("application/vnd.android.package-archive");
    private final Context context;
    private final OkHttpClient client;
    private final SettingsHelper settingsHelper;

    public ServerServiceImpl(Context context) {
        this.context = context;
        this.settingsHelper = SettingsHelper.getInstance(context);
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(context, AdminLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase(); // <-- Majuscules obligatoires
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void adminLogin(String username, String password, ServerApi.TokenCallback successCallback, ServerApi.ErrorCallback errorCallback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("login", username);
            jsonBody.put("password", md5(password));

            String url = settingsHelper.getBaseUrl() + "/rest/public/jwt/login";

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(JSON, jsonBody.toString()))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la tentative de connexion", e);
                    errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String token = jsonResponse.getString("id_token");
                            settingsHelper.setAdminAuthToken(token);
                            settingsHelper.setAdminUsername(username);
                            successCallback.onToken(token);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            errorCallback.onError("Format de réponse invalide");
                        }
                    } else {
                        Log.e(TAG, "Échec de l'authentification: " + response.code());
                        errorCallback.onError("Identifiants invalides");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void adminLogout() {
        settingsHelper.setAdminAuthToken(null);
        settingsHelper.setAdminUsername(null);
        Log.i(TAG, "Déconnexion administrateur effectuée");
        redirectToLogin();
    }

    @Override
    public boolean isAdminLoggedIn() {
        String authToken = settingsHelper.getAdminAuthToken();
        return authToken != null && !authToken.isEmpty();
    }

    private String getStatusFromCode(String code) {
        switch (code) {
            case "green":
                return "En ligne";
            case "yellow":
                return "Activité récente";
            case "red":
                return "Hors ligne";
            default:
                return "Inconnu";
        }
    }

    private String convertTimestamp(long timestamp) {
        if (timestamp == 0) return "Jamais";

        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    @Override
    public void getDevices(DeviceListCallback successCallback, ErrorCallback errorCallback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/private/devices/search";

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("pageSize", 1000);
            jsonBody.put("pageNum", 1);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + settingsHelper.getAdminAuthToken())
                    .post(RequestBody.create(JSON, jsonBody.toString()))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la récupération des appareils", e);
                    errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 401) {
                        redirectToLogin();
                        errorCallback.onError("Session expirée");
                        return;
                    }

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Réponse brute: " + responseBody);
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.optString("status", "");
                            if (!"OK".equals(status)) {
                                errorCallback.onError("Réponse non OK: " + jsonResponse.optString("message", "Erreur inconnue"));
                                return;
                            }

                            JSONObject data = jsonResponse.optJSONObject("data");
                            if (data == null) {
                                errorCallback.onError("Données absentes dans la réponse");
                                return;
                            }

                            JSONObject devices = data.optJSONObject("devices");
                            if (devices == null) {
                                errorCallback.onError("Liste des appareils absente");
                                return;
                            }

                            JSONArray items = devices.optJSONArray("items");
                            if (items == null) {
                                errorCallback.onError("Tableau des appareils absent");
                                return;
                            }

                            List<DeviceListFragment.Device> deviceList = new ArrayList<>();
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.optJSONObject(i);
                                if (item == null) continue;

                                DeviceListFragment.Device device = new DeviceListFragment.Device();
                                device.setId(item.optInt("id", -1));
                                device.setName(item.optString("description", "Sans nom"));
                                device.setNumber(item.optString("number", "Inconnu"));
                                device.setStatus(getStatusFromCode(item.optString("statusCode")));
                                device.setLastOnline(convertTimestamp(item.optLong("lastUpdate")));
                                device.setConfigurationId(item.optInt("configurationId", -1));

                                JSONObject info = item.optJSONObject("info");
                                device.setModel(info != null ? info.optString("model", "Inconnu") : "Inconnu");

                                // Parsing des groupes
                                JSONArray groupsArray = item.optJSONArray("groups");
                                if (groupsArray != null) {
                                    List<DeviceListFragment.Device.Group> groups = new ArrayList<>();
                                    for (int j = 0; j < groupsArray.length(); j++) {
                                        JSONObject groupObj = groupsArray.optJSONObject(j);
                                        if (groupObj != null) {
                                            int groupId = groupObj.optInt("id", -1);
                                            String groupName = groupObj.optString("name", "Inconnu");
                                            groups.add(new DeviceListFragment.Device.Group(groupId, groupName));
                                        }
                                    }
                                    device.setGroups(groups);
                                }

                                deviceList.add(device);
                            }

                            successCallback.onDeviceList(deviceList);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse: " + responseBody, e);
                            errorCallback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, "Échec de la récupération des appareils: " + errorMessage);
                        errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }


    @Override
    public void modifyDevice(int deviceId, String name, String number, int configurationId, ModifyDeviceCallback successCallback, ErrorCallback errorCallback) {
        try {
            String endpoint = settingsHelper.getBaseUrl() + "/rest/private/devices/";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                errorCallback.onError("Token d'authentification manquant");
                return;
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("id", deviceId);
            jsonBody.put("description", name);
            jsonBody.put("number", number);
            jsonBody.put("configurationId", configurationId);

            Request request = new Request.Builder()
                    .url(endpoint)
                    .put(RequestBody.create(JSON, jsonBody.toString()))
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la modification de l'appareil", e);
                    errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 401) {
                        redirectToLogin();
                        errorCallback.onError("Session expirée");
                        return;
                    }

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.optString("status", "");
                            String message = jsonResponse.optString("message", "Appareil modifié avec succès");
                            if ("OK".equals(status)) {
                                successCallback.onModifyDeviceSuccess(message);
                            } else {
                                errorCallback.onError("Échec de la modification: " + message);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            errorCallback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, "Échec de la modification de l'appareil: " + errorMessage);
                        errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }


    @Override
    public void deleteDevice(String deviceId, String token, ServerApi.DeleteDeviceCallback successCallback, ServerApi.ErrorCallback errorCallback) {
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token d'authentification manquant");
            errorCallback.onError("Token d'authentification manquant");
            return;
        }

        try {
            String url = settingsHelper.getBaseUrl() + "/rest/private/devices/" + deviceId;
            Log.d(TAG, "URL de la requête : " + url);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .delete()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion", e);
                    errorCallback.onError("Erreur de connexion : " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Réponse JSON : " + responseBody);

                    if (response.code() == 404) {
                        Log.e(TAG, "Erreur 404 : Endpoint ou appareil non trouvé : " + url);
                        errorCallback.onError("Erreur 404 : Appareil ou endpoint non trouvé. Vérifiez l'ID ou la configuration serveur.");
                        return;
                    }

                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            if (!json.optString("status").equals("OK")) {
                                errorCallback.onError("Réponse non valide : status = " + json.optString("status"));
                                return;
                            }
                            String message = json.optString("message", "Appareil supprimé avec succès");
                            successCallback.onDeleteSuccess(message);
                        } catch (Exception e) {
                            errorCallback.onError("Erreur de parsing : " + e.getMessage());
                        }
                    } else {
                        errorCallback.onError("Erreur " + response.code() + " : " + responseBody);
                    }
                }
            });
        } catch (Exception e) {
            errorCallback.onError("Erreur interne : " + e.getMessage());
        }
    }

    @Override
    public void getApplications(ServerApi.ApplicationListCallback successCallback, ServerApi.ErrorCallback errorCallback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/private/applications/search";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                errorCallback.onError("Token d'authentification manquant");
                return;
            }

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la récupération des applications", e);
                    errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 401) {
                        redirectToLogin();
                        errorCallback.onError("Session expirée");
                        return;
                    }

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray data = jsonResponse.getJSONArray("data");

                            List<ApplicationListFragment.Application> applicationList = new ArrayList<>();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject item = data.getJSONObject(i);
                                ApplicationListFragment.Application application = new ApplicationListFragment.Application();
                                application.setId(item.getInt("id"));
                                application.setName(item.optString("name", "Sans nom"));
                                application.setPkg(item.optString("pkg", "Inconnu"));
                                applicationList.add(application);
                            }
                            successCallback.onApplicationList(applicationList);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            errorCallback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage;
                        if (response.code() == 404) {
                            errorMessage = "Applications non trouvées";
                        } else if (response.code() == 500) {
                            errorMessage = "Erreur serveur interne";
                        } else {
                            errorMessage = "Erreur serveur: " + response.code();
                        }
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, "Échec de la récupération des applications: " + errorMessage);
                        errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }
    @Override
    public void searchApplicationsByName(String url, ApplicationCallback successCallback, ErrorCallback errorCallback) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + settingsHelper.getAdminAuthToken())
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray items = jsonResponse.getJSONArray("data");
                            List<ApplicationListFragment.Application> applications = new ArrayList<>();
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                ApplicationListFragment.Application app = new ApplicationListFragment.Application();
                                app.setId(item.getInt("id"));
                                app.setName(item.getString("name"));
                                app.setPkg(item.getString("pkg"));
                                applications.add(app);
                            }
                            successCallback.onSuccess(applications);
                        } catch (JSONException e) {
                            errorCallback.onError("Erreur de parsing: " + e.getMessage());
                        }
                    } else {
                        errorCallback.onError("Erreur serveur: " + response.code());
                    }
                }
            });
        } catch (Exception e) {
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void getDeviceDetails(String deviceNumber, ServerApi.DeviceCallback successCallback, ServerApi.ErrorCallback errorCallback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/private/devices/number/" + deviceNumber;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + settingsHelper.getAdminAuthToken())
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la récupération des détails de l'appareil", e);
                    errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 401) {
                        redirectToLogin();
                        errorCallback.onError("Session expirée");
                        return;
                    }
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONObject data = jsonResponse.getJSONObject("data");

                            DeviceListFragment.Device device = new DeviceListFragment.Device();
                            device.setId(data.getInt("id"));
                            device.setNumber(data.getString("number"));
                            device.setName(data.optString("name", "Sans nom"));
                            device.setStatus(data.optBoolean("online", false) ? "En ligne" : "Hors ligne");
                            device.setLastOnline(data.optString("lastUpdate", "Jamais"));
                            device.setModel(data.optString("configName", "Aucune"));

                            successCallback.onDevice(device);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            errorCallback.onError("Format de réponse invalide");
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, "Échec de la récupération des détails de l'appareil: " + errorMessage);
                        errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void getConfigurations(ServerApi.ConfigurationListCallback successCallback, ServerApi.ErrorCallback errorCallback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/private/configurations/search";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                errorCallback.onError("Token d'authentification manquant");
                return;
            }

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la récupération des configurations", e);
                    errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 401) {
                        redirectToLogin();
                        errorCallback.onError("Session expirée");
                        return;
                    }

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.getString("status");
                            if ("OK".equals(status)) {
                                JSONArray data = jsonResponse.getJSONArray("data");
                                List<ConfigurationListFragment.Configuration> configurations = new ArrayList<>();
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject item = data.getJSONObject(i);
                                    ConfigurationListFragment.Configuration config = new ConfigurationListFragment.Configuration();
                                    config.setId(item.getInt("id"));
                                    config.setName(item.optString("name", "Sans nom"));
                                    config.setPassword(item.optString("password", "Non défini"));
                                    config.setDescription(item.optString("description", "Sans description"));
                                    configurations.add(config);
                                }
                                successCallback.onConfigurationList(configurations);
                            } else {
                                errorCallback.onError("Échec de la récupération: " + jsonResponse.optString("message", "Erreur inconnue"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            errorCallback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage;
                        if (response.code() == 404) {
                            errorMessage = "Configurations non trouvées";
                        } else if (response.code() == 500) {
                            errorMessage = "Erreur serveur interne";
                        } else {
                            errorMessage = "Erreur serveur: " + response.code();
                        }
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, "Échec de la récupération des configurations: " + errorMessage);
                        errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void uninstallApp(String deviceNumber, String packageName, ServerApi.SuccessCallback successCallback, ServerApi.ErrorCallback errorCallback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/private/devices/" + deviceNumber + "/command";

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("command", "UNINSTALL");
            jsonBody.put("package", packageName);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + settingsHelper.getAdminAuthToken())
                    .post(RequestBody.create(JSON, jsonBody.toString()))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la demande de désinstallation", e);
                    errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 401) {
                        redirectToLogin();
                        errorCallback.onError("Session expirée");
                        return;
                    }
                    if (response.isSuccessful()) {
                        successCallback.onSuccess();
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, "Échec de la désinstallation: " + errorMessage);
                        errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void getDeviceStats(String token, ServerApi.DeviceStatsCallback successCallback, ServerApi.ErrorCallback errorCallback) {
        if (token == null || token.isEmpty()) {
            errorCallback.onError("Token d'authentification manquant");
            return;
        }

        String url = settingsHelper.getBaseUrl() + "/rest/private/summary/devices";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Erreur de connexion lors de la récupération des stats", e);
                errorCallback.onError("Erreur de connexion: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        JSONObject data = json.getJSONObject("data");
                        int totalDevices = data.optInt("devicesTotal", 0);
                        int enrolledDevices = data.optInt("devicesEnrolled", 0);
                        int lastMonthEnrolled = data.optInt("devicesEnrolledLastMonth", 0);

                        successCallback.onStats(totalDevices, enrolledDevices, lastMonthEnrolled);
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors du parsing des stats", e);
                        errorCallback.onError("Erreur de parsing: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Échec de la récupération des stats: " + response.code());
                    errorCallback.onError("Erreur " + response.code() + ": " + responseBody);
                }
            }
        });
    }

    @Override
    public void deleteApplication(int applicationId, ServerApi.SuccessCallback successCallback, ServerApi.ErrorCallback errorCallback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/private/applications/" + applicationId;
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                errorCallback.onError("Token d'authentification manquant");
                return;
            }

            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la suppression de l'application", e);
                    errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 401) {
                        redirectToLogin();
                        errorCallback.onError("Session expirée");
                        return;
                    }

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.getString("status");
                            String message = jsonResponse.getString("message");

                            if ("OK".equals(status)) {
                                successCallback.onSuccess();
                            } else {
                                errorCallback.onError("Échec de la suppression: " + message);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            errorCallback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage;
                        if (response.code() == 404) {
                            errorMessage = "Application non trouvée";
                        } else if (response.code() == 500) {
                            errorMessage = "Erreur serveur interne";
                        } else {
                            errorMessage = "Erreur serveur: " + response.code();
                        }
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, "Échec de la suppression de l'application: " + errorMessage);
                        errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête de suppression", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }


    @Override
    public void getApplicationById(int applicationId, ServerApi.GetApplicationIdCallback callback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/private/applications/" + applicationId;
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                callback.onError("Token d'authentification manquant");
                return;
            }

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur connexion pour getApplicationById: " + e.getMessage());
                    callback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Réponse getApplicationById: " + responseBody);

                    if (response.code() == 401) {
                        redirectToLogin();
                        callback.onError("Session expirée");
                        return;
                    }

                    if (!response.isSuccessful()) {
                        callback.onError("Erreur serveur: " + response.code());
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String status = json.optString("status", "");
                        if ("OK".equals(status)) {
                            JSONObject data = json.getJSONObject("data");
                            ApplicationListFragment.Application app = new ApplicationListFragment.Application();
                            app.setId(data.optInt("id"));
                            app.setName(data.optString("name", "Sans nom"));
                            app.setPkg(data.optString("pkg", "Inconnu"));
                            callback.onSuccess(app);
                        } else {
                            callback.onError("Erreur API: " + json.optString("message", "Inconnu"));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Erreur parsing JSON: " + e.getMessage());
                        callback.onError("Format réponse invalide");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur construction requête: " + e.getMessage());
            callback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void uploadApplicationFile(File apkFile, FileUploadSuccessCallback successCallback, FileUploadErrorCallback errorCallback) {
        try {
            String endpoint = settingsHelper.getBaseUrl() + "/rest/private/web-ui-files";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                if (errorCallback != null)
                    errorCallback.onError("Token d'authentification manquant");
                return;
            }

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", apkFile.getName(), RequestBody.create(MediaType.parse("application/vnd.android.package-archive"), apkFile))
                    .build();

            Request request = new Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur upload fichier", e);
                    if (errorCallback != null)
                        errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String serverPath = jsonResponse.getJSONObject("data").getString("serverPath");
                            JSONObject fileDetails = jsonResponse.getJSONObject("data").getJSONObject("fileDetails");
                            String pkg = fileDetails.getString("pkg");
                            String name = fileDetails.getString("name");
                            String version = fileDetails.getString("version");
                            if (successCallback != null)
                                successCallback.onSuccess(serverPath, pkg, name, version);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur parsing upload", e);
                            if (errorCallback != null)
                                errorCallback.onError("Format invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        if (errorCallback != null) errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation upload", e);
            if (errorCallback != null) errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void validatePackage(String name, String pkg, String version, int versionCode, String arch, String filePath,
                                ValidatePackageCallback callback) {
        try {
            String endpoint = settingsHelper.getBaseUrl() + "/rest/private/applications/validatePkg";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                callback.onError("Token d'authentification manquant");
                return;
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("name", name);
            jsonBody.put("pkg", pkg);
            jsonBody.put("version", version);
            jsonBody.put("versionCode", versionCode);
            jsonBody.put("arch", arch);
            jsonBody.put("filePath", filePath);
            jsonBody.put("type", "app");

            RequestBody body = RequestBody.create(JSON, jsonBody.toString());
            Request request = new Request.Builder()
                    .url(endpoint)
                    .put(body)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur validation package", e);
                    callback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            boolean isUnique = jsonResponse.getJSONArray("data").length() == 0;
                            callback.onValidatePackage(isUnique, pkg);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur parsing validation", e);
                            callback.onError("Format invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation validation", e);
            callback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void createOrUpdateApp(int id, String requestBody, CreateAppCallback callback) {
        try {
            String endpoint = settingsHelper.getBaseUrl() + "/rest/private/applications/android";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                callback.onError("Token d'authentification manquant");
                return;
            }

            RequestBody body = RequestBody.create(JSON, requestBody);
            Request request = new Request.Builder()
                    .url(endpoint)
                    .put(body) // Always use PUT, id is not included in request body for creation
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur création application", e);
                    callback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONObject data = jsonResponse.getJSONObject("data");
                            int applicationId = data.getInt("id");
                            String message = jsonResponse.optString("message", "Succès");
                            callback.onCreateAppSuccess(message, applicationId);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur parsing création", e);
                            callback.onError("Format invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation création", e);
            callback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void getApplicationConfigurations(int applicationId, GetAvailableConfigurationsCallback callback) {
        try {
            String endpoint = settingsHelper.getBaseUrl() + "/rest/private/applications/configurations/" + applicationId;
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                callback.onError("Token d'authentification manquant");
                return;
            }

            Request request = new Request.Builder()
                    .url(endpoint)
                    .get()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur récupération configurations", e);
                    callback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray dataArray = jsonResponse.getJSONArray("data");
                            List<Configuration> configurations = new ArrayList<>();
                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject configJson = dataArray.getJSONObject(i);
                                configurations.add(new Configuration(
                                        configJson.isNull("id") ? null : Integer.valueOf(configJson.optInt("id")), // Integer
                                        configJson.optInt("customerId", 0), // int, default 0 if missing
                                        configJson.optInt("configurationId", 0), // int, default 0 if missing
                                        configJson.optString("configurationName", ""), // String, default empty if missing
                                        configJson.isNull("applicationId") ? null : Integer.valueOf(configJson.optInt("applicationId")), // Integer
                                        configJson.optString("applicationName", ""), // String, default empty if missing
                                        configJson.optInt("action", 0), // int, default 0
                                        configJson.optBoolean("showIcon", true), // boolean, default true
                                        configJson.optBoolean("remove", false), // boolean, default false (added)
                                        configJson.optBoolean("outdated", false), // boolean, default false
                                        configJson.optString("latestVersionText", ""), // String, default empty
                                        configJson.optString("currentVersionText", ""), // String, default empty
                                        configJson.optBoolean("notify", false), // boolean, default false
                                        configJson.optBoolean("common", false) // boolean, default false
                                ));
                            }
                            callback.onSuccess(configurations);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur parsing configurations", e);
                            callback.onError("Format invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation récupération configurations", e);
            callback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void getAvailableConfigurations(ServerApi.AvailableConfigurationListCallback callback) {
        try {
            String endpoint = settingsHelper.getBaseUrl() + "/rest/private/configurations/search";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                if (callback != null) callback.onError("Token d'authentification manquant");
                return;
            }

            Request request = new Request.Builder()
                    .url(endpoint)
                    .get()
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Log.d(TAG, "Request URL: " + endpoint);
            Log.d(TAG, "Request Headers: Authorization=Bearer [token], Content-Type=application/json");

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la récupération des configurations", e);
                    if (callback != null)
                        callback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response Code: " + response.code());
                    Log.d(TAG, "Response Body: " + responseBody);

                    if (response.code() == 401) {
                        redirectToLogin();
                        if (callback != null) callback.onError("Session expirée");
                        return;
                    }

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.getString("status");
                            if ("OK".equals(status)) {
                                JSONArray data = jsonResponse.getJSONArray("data");
                                List<Configuration> configurations = new ArrayList<>();
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject item = data.getJSONObject(i);
                                    Configuration config = new Configuration(
                                            item.optInt("id", 0),
                                            0, // customerId not present in response, set to 0
                                            item.optInt("id", 0), // configurationId same as id
                                            item.optString("name", "Sans nom"),
                                            0, // applicationId not relevant here
                                            "", // applicationName not relevant
                                            0, // action not relevant
                                            true, // showIcon default
                                            false, // remove default
                                            false, // outdated default
                                            "", // latestVersionText default
                                            "", // currentVersionText default
                                            false, // notify default
                                            false // common default
                                    );
                                    configurations.add(config);
                                }
                                Log.d(TAG, "Parsed configurations: " + configurations.toString());
                                if (callback != null) callback.onSuccess(configurations);
                            } else {
                                String errorMessage = "Échec de la récupération: " + jsonResponse.optString("message", "Erreur inconnue");
                                Log.e(TAG, errorMessage);
                                if (callback != null) callback.onError(errorMessage);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            if (callback != null)
                                callback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage;
                        if (response.code() == 404) {
                            errorMessage = "Configurations non trouvées";
                        } else if (response.code() == 500) {
                            errorMessage = "Erreur serveur interne";
                        } else {
                            errorMessage = "Erreur serveur: " + response.code();
                        }
                        errorMessage += ", Détails: " + responseBody;
                        Log.e(TAG, "Échec de la récupération des configurations: " + errorMessage);
                        if (callback != null) callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            if (callback != null) callback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void updateApplicationConfiguration(int applicationId, int configurationId, ConfigurationUpdateRequest request,
                                               UpdateConfigurationCallback callback, String applicationName) {
        try {
            String endpoint = settingsHelper.getBaseUrl() + "/rest/private/applications/configurations";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                callback.onError("Token d'authentification manquant");
                return;
            }

            // Construct the configuration object (single configuration as per Postman)
            JSONObject configJson = new JSONObject();
            configJson.put("id", JSONObject.NULL);
            configJson.put("customerId", 1);
            configJson.put("configurationId", configurationId);
            configJson.put("configurationName", request.getConfigurationName());
            configJson.put("applicationId", applicationId);
            configJson.put("applicationName", applicationName);
            configJson.put("action", request.getAction());
            configJson.put("showIcon", request.isShowIcon());
            configJson.put("remove", false);
            configJson.put("outdated", false);
            configJson.put("latestVersionText", "0.23.1"); // Replace with actual version if needed
            configJson.put("currentVersionText", null);
            configJson.put("notify", request.isNotify());
            configJson.put("common", false);

            // Wrap the configuration object in a "configurations" array
            JSONArray configurationsArray = new JSONArray();
            configurationsArray.put(configJson);

            // Construct the root JSON body
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("applicationId", applicationId);
            jsonBody.put("configurations", configurationsArray);

            Log.d(TAG, "Final Request Payload: " + jsonBody.toString());
            Log.d(TAG, "Used Token: " + token.substring(0, 10) + "...");

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonBody.toString());
            Request requestHttp = new Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(requestHttp).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur mise à jour configuration", e);
                    callback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response Code: " + response.code());
                    Log.d(TAG, "Full Response: " + responseBody);
                    Log.d(TAG, "Headers: " + response.headers().toString());

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.getString("status");
                            String message = jsonResponse.optString("message", "Aucune information");
                            Log.d(TAG, "Response Status: " + status);
                            Log.d(TAG, "Response Message: " + message);

                            if ("OK".equals(status)) {
                                verifyApplicationAssociation(applicationId, configurationId, callback);
                            } else {
                                String errorMessage = "Échec de la mise à jour: " + jsonResponse.optString("message", "Erreur inconnue");
                                Log.e(TAG, errorMessage);
                                callback.onError(errorMessage);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur parsing réponse", e);
                            callback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code() + ", Détails: " + responseBody;
                        Log.e(TAG, errorMessage);
                        callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation mise à jour configuration", e);
            callback.onError("Erreur interne: " + e.getMessage());
        }
    }


    public void verifyApplicationAssociation(int applicationId, int configurationId, UpdateConfigurationCallback callback) {
        String endpoint = settingsHelper.getBaseUrl() + "/rest/private/applications/configurations/" + applicationId;
        String token = settingsHelper.getAdminAuthToken();

        Request request = new Request.Builder()
                .url(endpoint)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Erreur vérification association", e);
                callback.onError("Erreur vérification association: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Verify Association Response: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String status = jsonResponse.getString("status");
                        if ("OK".equals(status)) {
                            JSONArray configurations = jsonResponse.getJSONArray("data");
                            boolean isAssociated = false;
                            for (int i = 0; i < configurations.length(); i++) {
                                JSONObject config = configurations.getJSONObject(i);
                                if (config.getInt("configurationId") == configurationId) {
                                    int action = config.getInt("action");
                                    Log.d(TAG, "Verification Result - configurationId: " + configurationId + ", action: " + action);
                                    if (action == 1) {
                                        isAssociated = true;
                                        break;
                                    }
                                }
                            }
                            if (isAssociated) {
                                Log.d(TAG, "Association verified successfully");
                                callback.onSuccess();
                            } else {
                                Log.e(TAG, "Association not verified - action not 1");
                                callback.onError("Application non associée à la configuration (action non défini à 1)");
                            }
                        } else {
                            String errorMessage = "Échec vérification: " + jsonResponse.optString("message", "Erreur inconnue");
                            Log.e(TAG, errorMessage);
                            callback.onError(errorMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur parsing vérification", e);
                        callback.onError("Erreur parsing vérification: " + e.getMessage());
                    }
                } else {
                    String errorMessage = "Erreur serveur vérification: " + response.code() + ", Détails: " + responseBody;
                    Log.e(TAG, errorMessage);
                    callback.onError(errorMessage);
                }
            }
        });
    }


    @Override
    public void updateConfiguration(String requestBody, UpdateConfigurationCallback callback) {
        try {
            String endpoint = settingsHelper.getBaseUrl() + "/rest/private/configurations";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                if (callback != null) callback.onError("Token d'authentification manquant");
                return;
            }

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestBody);
            Request request = new Request.Builder()
                    .url(endpoint)
                    .put(body)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur mise à jour configuration", e);
                    if (callback != null)
                        callback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        if (callback != null) callback.onSuccess();
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        if (callback != null) callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation mise à jour configuration", e);
            if (callback != null) callback.onError("Erreur interne: " + e.getMessage());
        }
    }




    //configuration
    @Override
    public void copyConfiguration(int configurationId, String newName, String newDescription, CopyConfigurationCallback callback) {
        try {
            String endpoint = settingsHelper.getBaseUrl() + "/rest/private/configurations/copy";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                callback.onError("Token d'authentification manquant");
                return;
            }

            // Créer le corps de la requête avec nom et description
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("id", configurationId);
            jsonBody.put("name", newName);
            jsonBody.put("description", newDescription); // Ajout de la description

            Log.d(TAG, "Requête PUT envoyée à l'endpoint : " + endpoint);
            Log.d(TAG, "Corps de la requête : " + jsonBody.toString());

            Request request = new Request.Builder()
                    .url(endpoint)
                    .put(RequestBody.create(JSON, jsonBody.toString()))
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la copie de la configuration", e);
                    callback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Réponse brute du serveur : " + responseBody);

                    if (response.code() == 401) {
                        redirectToLogin();
                        callback.onError("Session expirée");
                        return;
                    }

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.optString("status", "");
                            String message = jsonResponse.optString("message", "Configuration copiée avec succès");
                            if ("OK".equals(status)) {
                                callback.onSuccess(message);
                            } else {
                                Log.e(TAG, "Statut non OK dans la réponse : " + status);
                                callback.onError("Échec de la copie: " + message);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            callback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code() + ", Détails: " + responseBody;
                        Log.e(TAG, "Échec de la copie de la configuration: " + errorMessage);
                        callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            callback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void deleteConfiguration(int configurationId, DeleteConfigurationCallback callback) {
        try {
            String endpoint = settingsHelper.getBaseUrl() + "/rest/private/configurations/" + configurationId;
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                callback.onError("Token d'authentification manquant");
                return;
            }

            Log.d(TAG, "Requête DELETE envoyée à l'endpoint : " + endpoint);

            Request request = new Request.Builder()
                    .url(endpoint)
                    .delete()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la suppression de la configuration", e);
                    callback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Réponse brute du serveur : " + responseBody);

                    if (response.code() == 401) {
                        redirectToLogin();
                        callback.onError("Session expirée");
                        return;
                    }

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.optString("status", "");
                            String message = jsonResponse.optString("message", "Configuration supprimée avec succès");
                            if ("OK".equals(status)) {
                                callback.onSuccess(message);
                            } else {
                                Log.e(TAG, "Statut non OK dans la réponse : " + status);
                                callback.onError("Échec de la suppression: " + message);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            callback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code() + ", Détails: " + responseBody;
                        Log.e(TAG, "Échec de la suppression de la configuration: " + errorMessage);
                        callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            callback.onError("Erreur interne: " + e.getMessage());
        }
    }


    @Override
    public void addDevice(String number, String description, int configurationId, String imei, String phone,
                          List<GroupFragment.Group> groups, AddDeviceCallback callback) {
        String endpoint = settingsHelper.getBaseUrl() + "/rest/private/devices";
        String token = settingsHelper.getAdminAuthToken();

        JSONObject deviceJson = new JSONObject();
        try {
            deviceJson.put("number", number);
            deviceJson.put("description", description);
            deviceJson.put("configurationId", configurationId);
            deviceJson.put("imei", imei);
            deviceJson.put("phone", phone);

            JSONArray groupsArray = new JSONArray();
            for (GroupFragment.Group group : groups) {
                JSONObject groupJson = new JSONObject();
                groupJson.put("id", group.getId());
                groupsArray.put(groupJson);
            }
            deviceJson.put("groups", groupsArray);
        } catch (Exception e) {
            callback.onError("Erreur création payload: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(deviceJson.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(endpoint)
                .put(body) // Utilisation de PUT (ou POST si nécessaire, ajustez selon votre API)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Erreur ajout appareil: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    String error = response.body().string();
                    callback.onError("Erreur serveur: " + response.code() + " - " + error);
                }
            }
        });
    }

    @Override
    public void getConfigurationsDevice(GetConfigurationsCallback callback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/private/configurations/search";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                callback.onError("Token d'authentification manquant");
                return;
            }

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la récupération des configurations", e);
                    callback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 401) {
                        redirectToLogin();
                        callback.onError("Session expirée");
                        return;
                    }

                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.getString("status");
                            if ("OK".equals(status)) {
                                JSONArray data = jsonResponse.getJSONArray("data");
                                List<AddDeviceFragment.Configuration> configurations = new ArrayList<>();
                                for (int i = 0; i < data.length(); i++) {
                                    JSONObject item = data.getJSONObject(i);
                                    int id = item.getInt("id");
                                    String name = item.optString("name", "Sans nom");
                                    configurations.add(new AddDeviceFragment.Configuration(id, name));
                                }
                                callback.onConfigurationList(configurations);
                            } else {
                                callback.onError("Échec de la récupération: " + jsonResponse.optString("message", "Erreur inconnue"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            callback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage;
                        if (response.code() == 404) {
                            errorMessage = "Configurations non trouvées";
                        } else if (response.code() == 500) {
                            errorMessage = "Erreur serveur interne";
                        } else {
                            errorMessage = "Erreur serveur: " + response.code();
                        }
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, "Échec de la récupération des configurations: " + errorMessage);
                        callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            callback.onError("Erreur interne: " + e.getMessage());
        }
    }


    //Groupe
    @Override
    public void getGroups(GetGroupsCallback callback) {
        String endpoint = settingsHelper.getBaseUrl() + "/rest/private/groups/search";
        String token = settingsHelper.getAdminAuthToken();

        Request request = new Request.Builder()
                .url(endpoint)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Erreur récupération groupes: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String status = jsonResponse.getString("status");
                        if ("OK".equals(status)) {
                            JSONArray data = jsonResponse.getJSONArray("data");
                            List<GroupFragment.Group> groups = new ArrayList<>();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject item = data.getJSONObject(i);
                                int id = item.getInt("id");
                                String name = item.getString("name");
                                int customerId = item.getInt("customerId");
                                boolean common = item.getBoolean("common");
                                groups.add(new GroupFragment.Group(id, name, customerId, common));
                            }
                            callback.onGroupList(groups);
                        } else {
                            callback.onError("Échec récupération groupes: " + jsonResponse.optString("message"));
                        }
                    } catch (Exception e) {
                        callback.onError("Erreur parsing groupes: " + e.getMessage());
                    }
                } else {
                    callback.onError("Erreur serveur: " + response.code());
                }
            }
        });
    }

    @Override
    public void searchGroups(String query, GetGroupsCallback callback) {
        String endpoint = settingsHelper.getBaseUrl() + "/rest/private/groups/search/" + query;
        String token = settingsHelper.getAdminAuthToken();

        Request request = new Request.Builder()
                .url(endpoint)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Erreur recherche groupes: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String status = jsonResponse.getString("status");
                        if ("OK".equals(status)) {
                            JSONArray data = jsonResponse.getJSONArray("data");
                            List<GroupFragment.Group> groups = new ArrayList<>();
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject item = data.getJSONObject(i);
                                int id = item.getInt("id");
                                String name = item.getString("name");
                                int customerId = item.getInt("customerId");
                                boolean common = item.getBoolean("common");
                                groups.add(new GroupFragment.Group(id, name, customerId, common));
                            }
                            callback.onGroupList(groups);
                        } else {
                            callback.onError("Échec recherche groupes: " + jsonResponse.optString("message"));
                        }
                    } catch (Exception e) {
                        callback.onError("Erreur parsing recherche: " + e.getMessage());
                    }
                } else {
                    callback.onError("Erreur serveur: " + response.code());
                }
            }
        });
    }

    @Override
    public void addGroup(String name, AddGroupCallback callback) {
        String endpoint = settingsHelper.getBaseUrl() + "/rest/private/groups";
        String token = settingsHelper.getAdminAuthToken();

        JSONObject groupJson = new JSONObject();
        try {
            groupJson.put("name", name);
        } catch (Exception e) {
            callback.onError("Erreur création payload: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(groupJson.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(endpoint)
                .put(body) // Utilisation de PUT comme spécifié
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Erreur ajout groupe: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Erreur serveur: " + response.code());
                }
            }
        });
    }

    @Override
    public void updateGroup(int id, String name, int customerId, boolean common, AddGroupCallback callback) {
        String endpoint = settingsHelper.getBaseUrl() + "/rest/private/groups";
        String token = settingsHelper.getAdminAuthToken();

        JSONObject groupJson = new JSONObject();
        try {
            groupJson.put("id", id);
            groupJson.put("name", name);
            groupJson.put("customerId", customerId);
            groupJson.put("common", common);
        } catch (Exception e) {
            callback.onError("Erreur création payload: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(groupJson.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(endpoint)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Erreur modification groupe: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Aucun détail fourni";
                    callback.onError("Erreur serveur: " + response.code() + " - " + errorBody);
                }
            }
        });
    }

    @Override
    public void deleteGroup(int id, DeleteGroupCallback callback) {
        String endpoint = settingsHelper.getBaseUrl() + "/rest/private/groups/" + id;
        String token = settingsHelper.getAdminAuthToken();

        Request request = new Request.Builder()
                .url(endpoint)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Erreur suppression groupe: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Erreur serveur: " + response.code());
                }
            }
        });
    }


    //Message
    @Override
    public void sendMessage(String scope, String deviceNumber, int groupId, int configurationId, String message, SendMessageCallback callback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/plugins/messaging/private/send";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                callback.onError("Token d'authentification manquant");
                return;
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("scope", scope);
            jsonBody.put("deviceNumber", deviceNumber);
            jsonBody.put("groupId", groupId);
            jsonBody.put("configurationId", configurationId);
            jsonBody.put("message", message);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(JSON, jsonBody.toString()))
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur envoi message", e);
                    callback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        callback.onSuccess("Message envoyé avec succès");
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, errorMessage);
                        callback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation requête", e);
            callback.onError("Erreur interne: " + e.getMessage());
        }
    }

    @Override
    public void getMessageHistory(MessageHistoryCallback successCallback, ErrorCallback errorCallback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/plugins/messaging/private/search";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                errorCallback.onError("Token d'authentification manquant");
                return;
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("pageNum", 1);
            jsonBody.put("pageSize", 50);
            jsonBody.put("deviceFilter", "");
            jsonBody.put("messageFilter", "");
            jsonBody.put("status", -1);
            jsonBody.put("sortValue", "createTime");

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(JSON, jsonBody.toString()))
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur chargement historique", e);
                    errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String responseStatus = jsonResponse.optString("status", ""); // Renommé pour éviter conflit
                            if ("OK".equals(responseStatus)) {
                                JSONObject data = jsonResponse.optJSONObject("data");
                                if (data != null) {
                                    JSONArray items = data.optJSONArray("items");
                                    List<SendMessageFragment.Message> messageList = new ArrayList<>();
                                    if (items != null) {
                                        for (int i = 0; i < items.length(); i++) {
                                            JSONObject item = items.optJSONObject(i);
                                            if (item != null) {
                                                int id = item.optInt("id", -1);
                                                String deviceNumber = item.optString("deviceNumber", "");
                                                long timestamp = item.optLong("ts", 0);
                                                String messageText = item.optString("message", "");
                                                int messageStatus = item.optInt("status", -1); // Renommé pour éviter conflit
                                                messageList.add(new SendMessageFragment.Message(id, deviceNumber, timestamp, messageText, messageStatus));
                                            }
                                        }
                                    }
                                    successCallback.onMessageList(messageList);
                                } else {
                                    errorCallback.onError("Données absentes dans la réponse");
                                }
                            } else {
                                errorCallback.onError("Statut non OK: " + jsonResponse.optString("message"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur parsing réponse", e);
                            errorCallback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, errorMessage);
                        errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation requête", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }




    //journeaux
    @Override
    public void searchAuditLogs(int pageNum, int pageSize, Integer customerId, String messageFilter,
                                Long dateFrom, Long dateTo, AuditCallback successCallback, ErrorCallback errorCallback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/plugins/audit/private/log/search";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                errorCallback.onError("Token d'authentification manquant");
                return;
            }

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("pageNum", pageNum);
            jsonBody.put("pageSize", pageSize);
            if (customerId != null) jsonBody.put("customerId", customerId);
            jsonBody.put("messageFilter", messageFilter != null ? messageFilter : "");
            if (dateFrom != null) jsonBody.put("dateFrom", dateFrom);
            if (dateTo != null) jsonBody.put("dateTo", dateTo);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(JSON, jsonBody.toString()))
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur recherche audit", e);
                    errorCallback.onError("Erreur de connexion: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.getString("status");
                            if ("OK".equals(status)) {
                                JSONObject data = jsonResponse.getJSONObject("data");
                                List<AuditFragment.AuditLog> auditLogs = new ArrayList<>();
                                if (data.has("items")) {
                                    JSONArray items = data.getJSONArray("items");
                                    for (int i = 0; i < items.length(); i++) {
                                        JSONObject item = items.getJSONObject(i);
                                        auditLogs.add(new AuditFragment.AuditLog(
                                                item.getLong("createTime"),
                                                item.getString("login"),
                                                item.getString("ipAddress"),
                                                item.getString("action")
                                        ));
                                    }
                                }
                                successCallback.onSuccess(auditLogs);
                            } else {
                                errorCallback.onError("Statut non OK: " + jsonResponse.optString("message"));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Erreur parsing réponse", e);
                            errorCallback.onError("Erreur parsing JSON: " + e.getMessage());
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, errorMessage);
                        errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur préparation requête audit", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }
}