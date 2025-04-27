package com.hmdm.launcher.json;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.hmdm.launcher.AdminReceiver;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

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
                // Journaliser l'action avant de l'exécuter
                Log.i(TAG, "Exécution d'un factory reset");

                // Effectuer le factory reset
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

                // Effacer les données de l'application
                ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Pour Android 9.0 (API 28) et supérieur
                    am.clearApplicationUserData(packageName, null);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // Pour Android 4.4 à 8.1
                    am.clearApplicationUserData(packageName);
                }

                // Utiliser DevicePolicyManager pour des opérations supplémentaires si disponible
                if (dpm.isAdminActive(adminComponent) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    dpm.clearPackagePersistentPreferredActivities(adminComponent, packageName);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        // Effacer le cache de l'application (Android 9.0+)
                        dpm.clearPackageCache(adminComponent, packageName);
                    } else {
                        // Pour les versions antérieures, utiliser une alternative
                        try {
                            // Utiliser la méthode de nettoyage du cache via PackageManager
                            Method clearCacheMethod = PackageManager.class.getMethod("deleteApplicationCacheFiles",String.class, IPackageDataObserver.class);
                            clearCacheMethod.invoke(context.getPackageManager(), packageName, null);
                            Log.i(TAG, "Cache nettoyé pour " + packageName + " via reflection");
                        } catch (Exception e) {
                            Log.e(TAG, "Impossible de nettoyer le cache pour " + packageName, e);
                            // Continuer malgré l'erreur
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'effacement des données pour le package: " + packageName, e);
                success = false;
            }
        }

        return success;
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
