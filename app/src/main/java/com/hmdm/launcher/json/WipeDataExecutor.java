package com.hmdm.launcher.json;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.hmdm.launcher.AdminReceiver;
import com.hmdm.launcher.helper.SettingsHelper;

import java.io.File;
import java.util.List;

/**
 * Classe pour exécuter les opérations d'effacement des données
 */
public class WipeDataExecutor {
    private static final String TAG = "WipeDataExec";

    private Context context;
    private DevicePolicyManager dpm;
    private ComponentName adminComponent;

    public WipeDataExecutor(Context context) {
        this.context = context;
        this.dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.adminComponent = new ComponentName(context, AdminReceiver.class);
    }

    /**
     * Effectue un factory reset de l'appareil
     * @return true si la commande a été exécutée
     */
    public boolean performFactoryReset() {
        try {
            if (dpm.isAdminActive(adminComponent)) {
                Log.i(TAG, "Exécution d'un factory reset");
                dpm.wipeData(0);
                return true;
            } else {
                Log.e(TAG, "Impossible d'effectuer un factory reset: droits d'administrateur non actifs");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'exécution du factory reset: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Effectue un effacement sélectif des données pour les packages spécifiés
     * @param packages Liste des packages à effacer
     * @return true si la commande a été exécutée
     */
    public boolean performSelectiveWipe(List<String> packages) {
        boolean success = true;

        for (String packageName : packages) {
            try {
                Log.i(TAG, "Effacement des données pour le package: " + packageName);

                // Effacer les données de l'application via commande shell
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    success &= clearApplicationData(packageName);
                }

                // Utiliser DevicePolicyManager pour des opérations supplémentaires si disponible
                if (dpm.isAdminActive(adminComponent) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    dpm.clearPackagePersistentPreferredActivities(adminComponent, packageName);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'effacement des données pour le package: " + packageName, e);
                success = false;
            }
        }

        return success;
    }

    /**
     * Efface les données d'une application via commande shell
     * @param packageName Nom du package
     * @return true si l'opération a réussi
     */
    private boolean clearApplicationData(String packageName) {
        try {
            // Exécuter la commande shell "pm clear" pour effacer les données de l'application
            Process process = Runtime.getRuntime().exec("pm clear " + packageName);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                Log.i(TAG, "Données effacées avec succès pour le package: " + packageName);
                return true;
            } else {
                Log.e(TAG, "Échec de l'effacement des données pour le package: " + packageName);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'exécution de la commande pm clear pour " + packageName + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Effectue un effacement des données utilisateur
     * @return true si la commande a été exécutée
     */
    public boolean performUserDataWipe() {
        try {
            Log.i(TAG, "Effacement des données utilisateur");

            // Effacer les fichiers externes
            boolean externalSuccess = wipeExternalStorage();

            // Effacer les préférences partagées
            boolean prefsSuccess = wipeSharedPreferences();

            // Effacer les bases de données
            boolean dbSuccess = wipeDatabases();

            // Effacer le cache
            boolean cacheSuccess = wipeCache();

            return externalSuccess && prefsSuccess && dbSuccess && cacheSuccess;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'effacement des données utilisateur: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Efface les fichiers du stockage externe
     */
    private boolean wipeExternalStorage() {
        try {
            File externalDir = Environment.getExternalStorageDirectory();
            return deleteRecursive(externalDir, true);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'effacement du stockage externe: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Efface les préférences partagées
     */
    private boolean wipeSharedPreferences() {
        try {
            File prefsDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
            return deleteRecursive(prefsDir, false);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'effacement des préférences partagées: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Efface les bases de données
     */
    private boolean wipeDatabases() {
        try {
            File dbDir = new File(context.getApplicationInfo().dataDir, "databases");
            return deleteRecursive(dbDir, false);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'effacement des bases de données: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Efface le cache
     */
    private boolean wipeCache() {
        try {
            boolean success = deleteRecursive(context.getCacheDir(), false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                File externalCache = context.getExternalCacheDir();
                if (externalCache != null) {
                    success &= deleteRecursive(externalCache, false);
                }
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'effacement du cache: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Supprime récursivement les fichiers et dossiers
     * @param fileOrDirectory Fichier ou dossier à supprimer
     * @param preserveRoot Si true, préserve le dossier racine (supprime seulement son contenu)
     * @return true si l'opération a réussi
     */
    private boolean deleteRecursive(File fileOrDirectory, boolean preserveRoot) {
        boolean success = true;

        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    success &= deleteRecursive(child, false);
                }
            }
        }

        // Ne pas supprimer le répertoire racine si preserveRoot est true
        if (!preserveRoot || !fileOrDirectory.isDirectory()) {
            if (!fileOrDirectory.delete() && fileOrDirectory.exists()) {
                Log.w(TAG, "Impossible de supprimer: " + fileOrDirectory.getAbsolutePath());
                success = false;
            }
        }

        return success;
    }
}