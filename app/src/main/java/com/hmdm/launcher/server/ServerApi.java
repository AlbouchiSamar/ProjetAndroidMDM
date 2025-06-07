package com.hmdm.launcher.server;


import com.hmdm.launcher.ui.Admin.AddDeviceFragment;
import com.hmdm.launcher.ui.Admin.ApplicationListFragment;
import com.hmdm.launcher.ui.Admin.Configuration;
import com.hmdm.launcher.ui.Admin.ConfigurationListFragment;
import com.hmdm.launcher.ui.Admin.DeleteAppFragment;
import com.hmdm.launcher.ui.Admin.DeviceListFragment;
import com.hmdm.launcher.ui.Admin.FileListFragment;
import com.hmdm.launcher.ui.Admin.GroupFragment;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

/**
 * Interface for communication with the Headwind MDM server.
 */
public interface ServerApi {

    /**
     * Authentifies an administrator with the server.
     * @param username Administrator username.
     * @param password Administrator password.
     * @param successCallback Callback called with the authentication token on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void adminLogin(String username, String password, TokenCallback successCallback, ErrorCallback errorCallback);

    /**
     * Logs out the administrator.
     */
    void adminLogout();

    /**
     * Checks if an administrator is logged in.
     * @return true if an administrator is logged in
     */
    boolean isAdminLoggedIn();

    /**
     * Retrieves the list of managed devices.
     * @param successCallback Callback called with the list of devices on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void getDevices(DeviceListCallback successCallback, ErrorCallback errorCallback);
    interface ModifyDeviceCallback {
        void onModifyDeviceSuccess(String message);
    }

    void modifyDevice(int deviceId, String name, String number, int configurationId, ModifyDeviceCallback successCallback, ErrorCallback errorCallback);
    /**
     * Retrieves details of a specific device.
     * @param deviceNumber Unique device number.
     * @param successCallback Callback called with the device details on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void getDeviceDetails(String deviceNumber, DeviceCallback successCallback, ErrorCallback errorCallback);



    /**
     * Retrieves logs (either for a specific device or general).
     * @param deviceNumber Device number (null for general logs).
     * @param successCallback Callback called with the list of log entries on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    // void getLogs(String deviceNumber, SuccessCallback<List<LogsFragment.LogEntry>> successCallback, ErrorCallback errorCallback);




    /**
     * Requests the uninstallation of an application on a device.
     * @param deviceNumber Unique device number.
     * @param packageName Package name of the application (e.g., "com.example.app").
     * @param successCallback Callback called on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void uninstallApp(String deviceNumber, String packageName, SuccessCallback successCallback, ErrorCallback errorCallback);

    /**
     * Interface for success callbacks without a return value.
     */
    interface SuccessCallback {
        void onSuccess();
    }



    /**
     * Interface for error callbacks.
     */
    interface ErrorCallback {
        void onError(String error);
    }

    /**
     * Interface for device list callbacks.
     */
    interface DeviceListCallback {
        void onDeviceList(List<DeviceListFragment.Device> devices);
    }

    /**
     * Interface for single device callbacks.
     */
    interface DeviceCallback {
        void onDevice(DeviceListFragment.Device device);
    }

    /**
     * Interface for authentication token callbacks.
     */
    interface TokenCallback {
        void onToken(String token);
    }


    interface DeviceStatsCallback {
        void onStats(int totalDevices, int enrolledDevices, int lastMonthEnrolled);
    }

    void getDeviceStats(String token, DeviceStatsCallback successCallback, ErrorCallback errorCallback);


    interface DeleteDeviceCallback {
        void onDeleteSuccess(String message);
    }
    void deleteDevice(String deviceId, String token, DeleteDeviceCallback successCallback, ErrorCallback errorCallback);
    interface ApplicationListCallback {
        void onApplicationList(List<ApplicationListFragment.Application> applications);
    }
    // Callback pour l'ajout d'un appareil
    interface AddDeviceCallback {
        void onSuccess();
        void onError(String error);
    }
    void addDevice(String number, String description, int configurationId, String imei, String phone,
                   List<GroupFragment.Group> groups, AddDeviceCallback callback);
    void getApplications(ApplicationListCallback successCallback, ErrorCallback errorCallback);

    /**
     * Interface for single application callbacks.
     */
    interface GetApplicationIdCallback {
        void onSuccess(ApplicationListFragment.Application application);
        void onError(String error);
    }
    void getApplicationById(int applicationId, GetApplicationIdCallback callback);




   // void getApplicationConfigurations(int applicationId, GetConfigurationsCallback callback);

    interface FileUploadSuccessCallback {
        void onSuccess(String serverPath, String pkg, String name, String version);
    }

    interface FileUploadErrorCallback {
        void onError(String error);
    }

    void uploadApplicationFile(File apkFile, FileUploadSuccessCallback successCallback, FileUploadErrorCallback errorCallback);    interface ValidatePackageCallback {
        void onValidatePackage(boolean isUnique, String validatedPkg);
        void onError(String error);
    }
    void validatePackage(String name, String pkg, String version, int versionCode, String arch, String url, ValidatePackageCallback callback);
    // Méthode pour créer ou mettre à jour une application
    interface CreateAppCallback {
        void onCreateAppSuccess(String message, int applicationId);
        void onError(String error);
    }
    void createOrUpdateApp(int id, String requestBody, CreateAppCallback callback);
    // Méthode pour mettre à jour les configurations
    interface UpdateConfigCallback {
        void onUpdateConfigSuccess(String message);
        void onError(String error);
    }

    interface UpdateConfigurationCallback {
        void onSuccess();
        void onError(String error);
    }
    interface GetAvailableConfigurationsCallback {
        void onSuccess(List<Configuration> configurations);
        void onError(String error);
    }
    interface AvailableConfigurationListCallback {
        void onSuccess(List<Configuration> configurations);
        void onError(String error);
    }


    void getAvailableConfigurations(AvailableConfigurationListCallback callback);
    void updateApplicationConfiguration(int applicationId, int configurationId, ConfigurationUpdateRequest request,
                                        UpdateConfigurationCallback callback, String applicationName);
    void verifyApplicationAssociation(int applicationId, int configurationId, UpdateConfigurationCallback callback);

    // Data Models
    class ApplicationConfiguration {
        private final int id;
        private final int configurationId;
        private final String configurationName;

        public ApplicationConfiguration(int id, int configurationId, String configurationName) {
            this.id = id;
            this.configurationId = configurationId;
            this.configurationName = configurationName;
        }

        public int getId() { return id; }
        public int getConfigurationId() { return configurationId; }
        public String getConfigurationName() { return configurationName; }
    }

    class ConfigurationUpdateRequest {
        private final int customerId;
        private final int configurationId;
        private final String configurationName;
        private final int action; // 0: No action, 1: Install
        private final boolean showIcon;
        private final boolean notify;

        public ConfigurationUpdateRequest(int customerId, int configurationId, String configurationName,
                                          int action, boolean showIcon, boolean notify) {
            this.customerId = customerId;
            this.configurationId = configurationId;
            this.configurationName = configurationName;
            this.action = action;
            this.showIcon = showIcon;
            this.notify = notify;
        }

        public int getCustomerId() { return customerId; }
        public int getConfigurationId() { return configurationId; }
        public String getConfigurationName() { return configurationName; }
        public int getAction() { return action; }
        public boolean isShowIcon() { return showIcon; }
        public boolean isNotify() { return notify; }
    }
    void getApplicationConfigurations(int applicationId, GetAvailableConfigurationsCallback callback);

    void updateConfiguration(String requestBody, UpdateConfigurationCallback callback);
   // void updateApplicationConfigurations(ApplicationConfigurationsUpdateRequest request, SuccessCallback successCallback, ErrorCallback errorCallback);
    void deleteApplication(int applicationId, SuccessCallback successCallback, ErrorCallback errorCallback);


    /**
     * Interface for file list callbacks.
     */
    interface FileListCallback {
        void onFileList(List<FileListFragment.FileItem> files);
    }

    /**
     * Retrieves the list of files from the server.
     * @param successCallback Callback called with the list of files on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void getFiles(FileListCallback successCallback, ErrorCallback errorCallback);




//configuration
    /**
     * Interface for configuration list callbacks.
     */
    interface ConfigurationListCallback {
        void onConfigurationList(List<ConfigurationListFragment.Configuration> configurations);
    }
    void getConfigurations(ConfigurationListCallback successCallback, ErrorCallback errorCallback);
    interface GetConfigurationsCallback {
        void onConfigurationList(List<AddDeviceFragment.Configuration> configurations);
        void onError(String error);
    }
    void getConfigurationsDevice(GetConfigurationsCallback callback);

    // Callback pour copyConfiguration
    interface CopyConfigurationCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    void copyConfiguration(int configurationId, String newName, String newDescription, CopyConfigurationCallback callback);
    // Callback pour deleteConfiguration
    interface DeleteConfigurationCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    void deleteConfiguration(int configurationId, DeleteConfigurationCallback callback);




//Groupe
    // Callback pour la récupération des groupes
    interface GetGroupsCallback {
        void onGroupList(List<GroupFragment.Group> groups);
        void onError(String error);
    }

    // Callback pour l'ajout/modification d'un groupe
    interface AddGroupCallback {
        void onSuccess();
        void onError(String error);
    }

    // Callback pour la suppression d'un groupe
    interface DeleteGroupCallback {
        void onSuccess();
        void onError(String error);
    }

    // Méthodes à implémenter dans ServerServiceImpl
    void getGroups(GetGroupsCallback callback);
    void searchGroups(String query, GetGroupsCallback callback);
    void addGroup(String name, AddGroupCallback callback);
    void updateGroup(int id, String name, int customerId, boolean common, AddGroupCallback callback);    void deleteGroup(int id, DeleteGroupCallback callback);
}
