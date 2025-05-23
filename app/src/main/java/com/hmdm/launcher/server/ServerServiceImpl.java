package com.hmdm.launcher.server;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hmdm.launcher.helper.SettingsHelper;
import com.hmdm.launcher.ui.Admin.AdminLoginActivity;
import com.hmdm.launcher.ui.Admin.ApplicationListFragment;
import com.hmdm.launcher.ui.Admin.ConfigurationListFragment;
import com.hmdm.launcher.ui.Admin.DeviceListFragment;

import org.json.JSONArray;
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
    public String getStatusFromCode(String code) {
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
    public String convertTimestamp(long timestamp) {
        if (timestamp == 0) return "Jamais";

        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    @Override
    public void getDevices(ServerApi.DeviceListCallback successCallback, ServerApi.ErrorCallback errorCallback) {
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
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONObject data = jsonResponse.getJSONObject("data");
                            JSONObject devices = data.getJSONObject("devices");
                            JSONArray items = devices.getJSONArray("items");

                            List<DeviceListFragment.Device> deviceList = new ArrayList<>();
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                DeviceListFragment.Device device = new DeviceListFragment.Device();
                                device.setId(data.getInt("id"));
                                device.setName(item.optString("description", "Sans nom"));
                                device.setNumber(item.optString("number", "Inconnu"));
                                device.setStatus(getStatusFromCode(item.optString("statusCode")));
                                device.setLastOnline(convertTimestamp(item.optLong("lastUpdate")));
                                device.setModel(item.optJSONObject("info").optString("model", "Inconnu"));
                                deviceList.add(device);
                            }
                            successCallback.onDeviceList(deviceList);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            errorCallback.onError("Format de réponse invalide");
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
    public void deleteDevice(String deviceId, String token, DeleteDeviceCallback successCallback, ErrorCallback errorCallback) {
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

            // Vérifier si le token est valide
            if (token == null || token.isEmpty()) {
                redirectToLogin();
                errorCallback.onError("Token d'authentification manquant");
                return;
            }

            // Construire une requête GET
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
            String url = settingsHelper.getBaseUrl() + "/rest/private/admin/configurations";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + settingsHelper.getAdminAuthToken())
                    .get()
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
                            JSONArray items = jsonResponse.getJSONArray("data");
                            List<ConfigurationListFragment.Configuration> configurations = new ArrayList<>();
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                ConfigurationListFragment.Configuration config = new ConfigurationListFragment.Configuration(
                                        item.getString("id"), item.getString("name"));
                                configurations.add(config);
                            }
                            successCallback.onConfigurationList(configurations);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            errorCallback.onError("Format de réponse invalide");
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
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



   /* @Override
    public void getLogs(String deviceNumber, ServerApi.SuccessCallback<List<LogsFragment.LogEntry>> successCallback, ServerApi.ErrorCallback errorCallback) {
        try {
            String url;
            if (deviceNumber != null) {
                url = settingsHelper.getBaseUrl() + "/rest/public/admin/devices/" + deviceNumber + "/logs";
            } else {
                url = settingsHelper.getBaseUrl() + "/rest/public/admin/logs";
            }

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + settingsHelper.getAdminAuthToken())
                    .get()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors de la récupération des logs", e);
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
                            JSONArray logsArray = jsonResponse.optJSONArray("logs");

                            List<LogsFragment.LogEntry> logList = new ArrayList<>();
                            if (logsArray != null) {
                                for (int i = 0; i < logsArray.length(); i++) {
                                    JSONObject logJson = logsArray.getJSONObject(i);
                                    LogsFragment.LogEntry logEntry = new LogsFragment.LogEntry();
                                    logEntry.setTimestamp(logJson.optLong("timestamp"));
                                    logEntry.setLevel(logJson.optString("level"));
                                    logEntry.setMessage(logJson.optString("message"));
                                    logList.add(logEntry);
                                }
                            }

                            successCallback.onSuccess(logList);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing des logs", e);
                            errorCallback.onError("Format de réponse invalide");
                        }
                    } else {
                        String errorMessage = "Erreur serveur: " + response.code();
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, "Échec de la récupération des logs: " + errorMessage);
                        errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }*/


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
    public void getDeviceStats(String token, DeviceStatsCallback successCallback, ErrorCallback errorCallback) {
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
    public void deleteApplication(int applicationId, SuccessCallback successCallback, ErrorCallback errorCallback) {
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
    public void uploadApplicationFile(File apkFile, FileUploadCallback successCallback, ErrorCallback errorCallback) {
        try {
            String url = settingsHelper.getBaseUrl() + "/rest/private/web-ui-files";
            String token = settingsHelper.getAdminAuthToken();

            if (token == null || token.isEmpty()) {
                redirectToLogin();
                errorCallback.onError("Token d'authentification manquant");
                return;
            }

            if (apkFile == null || !apkFile.exists()) {
                errorCallback.onError("Fichier APK invalide ou introuvable");
                return;
            }

            RequestBody fileBody = RequestBody.create(
                    apkFile,
                    MediaType.parse("application/vnd.android.package-archive")
            );

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", apkFile.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Erreur de connexion lors du téléversement du fichier", e);
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
                            String serverPath = jsonResponse.optString("serverPath", "Inconnu");
                            JSONObject fileDetails = jsonResponse.optJSONObject("fileDetails");
                            if (fileDetails == null) {
                                errorCallback.onError("Détails du fichier manquants dans la réponse");
                                return;
                            }

                            String pkg = fileDetails.optString("pkg", "Inconnu");
                            String name = fileDetails.optString("name", "Sans nom");
                            String version = fileDetails.optString("version", "Inconnue");

                            // Vérifier si les champs essentiels sont présents
                            if (pkg.equals("Inconnu") || name.equals("Sans nom")) {
                                Log.w(TAG, "Certains détails du fichier sont manquants dans la réponse");
                            }

                            successCallback.onFileUploaded(serverPath, pkg, name, version);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            errorCallback.onError("Format de réponse invalide: " + e.getMessage());
                        }
                    } else {
                        String errorMessage;
                        if (response.code() == 404) {
                            errorMessage = "Ressource non trouvée";
                        } else if (response.code() == 500) {
                            errorMessage = "Erreur serveur interne";
                        } else {
                            errorMessage = "Erreur serveur: " + response.code();
                        }
                        if (response.body() != null) {
                            errorMessage += ", Détails: " + response.body().string();
                        }
                        Log.e(TAG, "Échec du téléversement du fichier: " + errorMessage);
                        errorCallback.onError(errorMessage);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la requête de téléversement", e);
            errorCallback.onError("Erreur interne: " + e.getMessage());
        }
    }
}