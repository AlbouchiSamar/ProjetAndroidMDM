package com.hmdm.launcher.json;

import java.util.List;

// Nouvelle classe dans le package com.hmdm.rest.json
public class WipeDataRequest {
    // Types d'effacement : FACTORY_RESET, SELECTIVE, USER_DATA
    private String wipeType;

    // Liste des packages à effacer (pour le type SELECTIVE)
    private List<String> packages;

    // Getters et setters
    public String getWipeType() {
        return wipeType;
    }

    public void setWipeType(String wipeType) {
        this.wipeType = wipeType;
    }

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }
}
