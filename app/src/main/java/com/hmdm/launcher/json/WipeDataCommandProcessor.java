package com.hmdm.launcher.json;
import static android.content.ContentValues.TAG;
import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
public class WipeDataCommandProcessor {
    public static final String TYPE_FACTORY_RESET = "FACTORY_RESET";
    public static final String TYPE_SELECTIVE = "SELECTIVE";
    public static final String TYPE_USER_DATA = "USER_DATA";

    private Context context;
    private WipeDataExecutor wipeDataExecutor;

    public WipeDataCommandProcessor(Context context, WipeDataExecutor wipeDataExecutor) {
        this.context = context;
        this.wipeDataExecutor = wipeDataExecutor;
    }
    /**
     * Traite une commande d'effacement des données
     * @param commandJson La commande JSON reçue
     * @return true si la commande a été traitée avec succès
     */
    public boolean processCommand(String commandJson) {
        try {
            JSONObject command = new JSONObject(commandJson);

            // Vérifier que c'est bien une commande WIPE_DATA
            if (!"WIPE_DATA".equals(command.optString("command"))) {
                return false;
            }

            String wipeType = command.optString("type");
            if (wipeType == null || wipeType.isEmpty()) {
                Log.e(TAG, "Type d'effacement non spécifié");
                return false;
            }

            switch (wipeType) {
                case TYPE_FACTORY_RESET:
                    return wipeDataExecutor.performFactoryReset();

                case TYPE_SELECTIVE:
                    JSONArray packagesArray = command.optJSONArray("packages");
                    if (packagesArray == null) {
                        Log.e(TAG, "Liste des packages non fournie pour l'effacement sélectif");
                        return false;
                    }

                    List<String> packages = new ArrayList<>();
                    for (int i = 0; i < packagesArray.length(); i++) {
                        packages.add(packagesArray.getString(i));
                    }

                    return wipeDataExecutor.performSelectiveWipe(packages);

                case TYPE_USER_DATA:
                    return wipeDataExecutor.performUserDataWipe();

                default:
                    Log.e(TAG, "Type d'effacement non reconnu: " + wipeType);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du traitement de la commande d'effacement: " + e.getMessage(), e);
            return false;
        }
    }
}
