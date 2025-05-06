package com.hmdm.launcher.server;

import com.hmdm.launcher.ui.Admin.ConfigurationListFragment;
import com.hmdm.launcher.ui.Admin.DeviceListFragment;
import com.hmdm.launcher.ui.Admin.WipeDataActivity;

import org.json.JSONObject;

import java.util.List;

public interface ServerApi {
    /**
     * Authentifie un administrateur auprès du serveur.
     * @param username Nom d'utilisateur de l'administrateur.
     * @param password Mot de passe de l'administrateur.
     * @param successCallback Callback appelé avec le jeton d'authentification en cas de succès.
     * @param errorCallback Callback appelé avec un message d'erreur en cas d'échec.
     */
    void adminLogin(String username, String password, TokenCallback successCallback, ErrorCallback errorCallback);
    /**
     * Récupère la liste des configurations disponibles.
     * @param successCallback Callback appelé avec la liste des configurations en cas de succès.
     * @param errorCallback Callback appelé avec un message d'erreur en cas d'échec.
     */
    void getConfigurations(ConfigurationListCallback successCallback, ErrorCallback errorCallback);

    /**
     * Ajoute un nouvel appareil.
     * @param deviceData Données de l'appareil (JSON contenant number, name, configurationId).
     * @param successCallback Callback appelé en cas de succès.
     * @param errorCallback Callback appelé avec un message d'erreur en cas d'échec.
     */
    void addDevice(JSONObject deviceData, SuccessCallback successCallback, ErrorCallback errorCallback);
    /**
     * Récupère la liste des appareils gérés.
     * @param successCallback Callback appelé avec la liste des appareils en cas de succès.
     * @param errorCallback Callback appelé avec un message d'erreur en cas d'échec.
     */
    void getDevices(DeviceListCallback successCallback, ErrorCallback errorCallback);

    /**
     * Récupère les détails d'un appareil spécifique.
     * @param deviceNumber Numéro unique de l'appareil.
     * @param successCallback Callback appelé avec les détails de l'appareil en cas de succès.
     * @param errorCallback Callback appelé avec un message d'erreur en cas d'échec.
     */
    void getDeviceDetails(String deviceNumber, DeviceCallback successCallback, ErrorCallback errorCallback);

    /**
     * Verrouille un appareil à distance.
     * @param deviceNumber Numéro unique de l'appareil.
     * @param successCallback Callback appelé en cas de succès.
     * @param errorCallback Callback appelé avec un message d'erreur en cas d'échec.
     */
    void lockDevice(String deviceNumber, SuccessCallback successCallback, ErrorCallback errorCallback);

    /**
     * Déverrouille un appareil à distance.
     * @param deviceNumber Numéro unique de l'appareil.
     * @param successCallback Callback appelé en cas de succès.
     * @param errorCallback Callback appelé avec un message d'erreur en cas d'échec.
     */
    void unlockDevice(String deviceNumber, SuccessCallback successCallback, ErrorCallback errorCallback);

    /**
     * Redémarre un appareil à distance.
     * @param deviceNumber Numéro unique de l'appareil.
     * @param successCallback Callback appelé en cas de succès.
     * @param errorCallback Callback appelé avec un message d'erreur en cas d'échec.
     */
    void rebootDevice(String deviceNumber, SuccessCallback successCallback, ErrorCallback errorCallback);

    /**
     * Supprime les données d'un appareil à distance.
     * @param deviceNumber Numéro unique de l'appareil.
     * @param request Paramètres de la demande de suppression (type de suppression, packages).
     * @param successCallback Callback appelé en cas de succès.
     * @param errorCallback Callback appelé avec un message d'erreur en cas d'échec.
     */
    void wipeDeviceData(String deviceNumber, WipeDataActivity.WipeDataRequest request,
                        SuccessCallback successCallback, ErrorCallback errorCallback);

    interface SuccessCallback {
        void onSuccess();
    }

    interface ErrorCallback {
        void onError(String error);
    }

    interface DeviceListCallback {
        void onDeviceList(List<DeviceListFragment.Device> devices);
    }

    interface DeviceCallback {
        void onDevice(DeviceListFragment.Device device);
    }

    interface TokenCallback {
        void onToken(String token);
    }
    interface ConfigurationListCallback {
        void onConfigurationList(List<ConfigurationListFragment.Configuration> configurations);
    }

}