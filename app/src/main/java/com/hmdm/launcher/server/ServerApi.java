package com.hmdm.launcher.server;

import com.hmdm.launcher.ui.Admin.ApplicationListFragment;
import com.hmdm.launcher.ui.Admin.ConfigurationListFragment;
import com.hmdm.launcher.ui.Admin.DeleteApplicationFragment;
import com.hmdm.launcher.ui.Admin.DeviceListFragment;
import com.hmdm.launcher.ui.Admin.FileListFragment;

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

    /**
     * Interface for configuration list callbacks.
     */
    interface ConfigurationListCallback {
        void onConfigurationList(List<ConfigurationListFragment.Configuration> configurations);
    }
    interface DeviceStatsCallback {
        void onStats(int totalDevices, int enrolledDevices, int lastMonthEnrolled);
    }

    void getDeviceStats(String token, DeviceStatsCallback successCallback, ErrorCallback errorCallback);
    // Callbacks pour la réinitialisation
    interface ResetDeviceCallback {
        void onResetSuccess(String message);
    }

   interface DeleteDeviceCallback {
        void onDeleteSuccess(String message);
    }
    void deleteDevice(String deviceId, String token, DeleteDeviceCallback successCallback, ErrorCallback errorCallback);
    interface ApplicationListCallback {
        void onApplicationList(List<ApplicationListFragment.Application> applications);
    }

    void getApplications(ApplicationListCallback successCallback, ErrorCallback errorCallback);
   void getConfigurations(ConfigurationListCallback successCallback, ErrorCallback errorCallback);

    /**
     * Interface for single application callbacks.
     */
    interface GetApplicationIdCallback {
        void onSuccess(ApplicationListFragment.Application application);
        void onError(String error);
    }
    void getApplicationById(int applicationId, GetApplicationIdCallback callback);






    interface FileUploadCallback {
        void onSuccess(String serverPath, String pkg, String name, String version);
    }
    void uploadApplicationFile(File apkFile, FileUploadCallback successCallback, ErrorCallback errorCallback);
    // Méthode pour valider un package
    interface ValidatePackageCallback {
        void onValidatePackage(boolean isUnique, String pkg);
        void onError(String error);
    }
    void validatePackage(String pkg, String name, String version, int versionCode, String arch, String url, ValidatePackageCallback callback);

    // Méthode pour créer ou mettre à jour une application
    interface CreateAppCallback {
        void onCreateAppSuccess(String message, int applicationId); // Ajout de l'applicationId
        void onError(String error);
    }
    void createOrUpdateApp(int id, String pkg, String name, String version, int versionCode, String arch, String url,
                           boolean showIcon, boolean useKiosk, boolean system, CreateAppCallback callback);

    // Méthode pour mettre à jour les configurations
    interface UpdateConfigCallback {
        void onUpdateConfigSuccess(String message);
        void onError(String error);
    }
    void updateConfigurations(int applicationId, String configName, UpdateConfigCallback callback);




    interface ApplicationConfigurationsCallback {
        void onApplicationConfigurations(List<DeleteApplicationFragment.ApplicationConfiguration> configurations);
    }

    class ApplicationConfigurationsUpdateRequest {
        private int applicationId;
        private List<DeleteApplicationFragment.ApplicationConfiguration> configurations;

        public ApplicationConfigurationsUpdateRequest(int applicationId, List<DeleteApplicationFragment.ApplicationConfiguration> configurations) {
            this.applicationId = applicationId;
            this.configurations = configurations;
        }

        public int getApplicationId() { return applicationId; }
        public List<DeleteApplicationFragment.ApplicationConfiguration> getConfigurations() { return configurations; }
    }
    void getApplicationConfigurations(int applicationId, ApplicationConfigurationsCallback successCallback, ErrorCallback errorCallback);
    void updateApplicationConfigurations(ApplicationConfigurationsUpdateRequest request, SuccessCallback successCallback, ErrorCallback errorCallback);
    void deleteApplication(int applicationId, SuccessCallback successCallback, ErrorCallback errorCallback);




}
