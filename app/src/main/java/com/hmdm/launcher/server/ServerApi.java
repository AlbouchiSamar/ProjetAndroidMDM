package com.hmdm.launcher.server;

import android.graphics.Bitmap;

import com.hmdm.launcher.ui.Admin.ConfigurationListFragment;
import com.hmdm.launcher.ui.Admin.DeviceListFragment;
import com.hmdm.launcher.ui.Admin.LogsFragment;
import com.hmdm.launcher.ui.Admin.WipeDataActivity;

import org.json.JSONObject;

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

    /**
     * Retrieves details of a specific device.
     * @param deviceNumber Unique device number.
     * @param successCallback Callback called with the device details on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void getDeviceDetails(String deviceNumber, DeviceCallback successCallback, ErrorCallback errorCallback);

    /**
     * Retrieves the list of available configurations.
     * @param successCallback Callback called with the list of configurations on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void getConfigurations(ConfigurationListCallback successCallback, ErrorCallback errorCallback);

    /**
     * Retrieves a screenshot for a specific device.
     * @param deviceNumber Device number.
     * @param successCallback Callback called with the screenshot Bitmap on success.
     * @param errorCallback Callback called with an error message on failure.
     */
   // void getDeviceScreenshot(String deviceNumber, SuccessCallback<Bitmap> successCallback, ErrorCallback errorCallback);

    /**
     * Sends a remote control command to a specific device.
     * @param deviceNumber Device number.
     * @param command Command type (e.g., "tap", "swipe").
     * @param x X coordinate.
     * @param y Y coordinate.
     * @param successCallback Callback called on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void sendRemoteControlCommand(String deviceNumber, String command, int x, int y, SuccessCallback successCallback, ErrorCallback errorCallback);

    /**
     * Retrieves logs (either for a specific device or general).
     * @param deviceNumber Device number (null for general logs).
     * @param successCallback Callback called with the list of log entries on success.
     * @param errorCallback Callback called with an error message on failure.
     */
   // void getLogs(String deviceNumber, SuccessCallback<List<LogsFragment.LogEntry>> successCallback, ErrorCallback errorCallback);

    /**
     * Adds a new device.
     * @param deviceData Device data (JSON containing number, name, configurationId).
     * @param successCallback Callback called on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void addDevice(JSONObject deviceData, SuccessCallback successCallback, ErrorCallback errorCallback);

    /**
     * Locks a device remotely.
     * @param deviceNumber Unique device number.
     * @param successCallback Callback called on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void lockDevice(String deviceNumber, SuccessCallback successCallback, ErrorCallback errorCallback);

    /**
     * Unlocks a device remotely.
     * @param deviceNumber Unique device number.
     * @param successCallback Callback called on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void unlockDevice(String deviceNumber, SuccessCallback successCallback, ErrorCallback errorCallback);

    /**
     * Reboots a device remotely.
     * @param deviceNumber Unique device number.
     * @param successCallback Callback called on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void rebootDevice(String deviceNumber, SuccessCallback successCallback, ErrorCallback errorCallback);

    /**
     * Wipes data from a device remotely.
     * @param deviceNumber Unique device number.
     * @param request Parameters for the wipe request (wipe type, packages).
     * @param successCallback Callback called on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void wipeDeviceData(String deviceNumber, WipeDataActivity.WipeDataRequest request, SuccessCallback successCallback, ErrorCallback errorCallback);

    /**
     * Requests the uninstallation of an application on a device.
     * @param deviceNumber Unique device number.
     * @param packageName Package name of the application (e.g., "com.example.app").
     * @param successCallback Callback called on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void uninstallApp(String deviceNumber, String packageName, SuccessCallback successCallback, ErrorCallback errorCallback);

    /**
     * Sends a command to control device peripherals.
     * @param deviceId Device ID.
     * @param peripheral Peripheral type (WIFI, BLUETOOTH, GPS).
     * @param action Action to perform (ENABLE, DISABLE).
     * @param successCallback Callback called on success.
     * @param errorCallback Callback called with an error message on failure.
     */
    void sendPeripheralControlCommand(String deviceId, String peripheral, String action, SuccessCallback successCallback, ErrorCallback errorCallback);

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
}