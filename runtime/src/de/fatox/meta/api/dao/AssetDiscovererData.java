package de.fatox.meta.api.dao;

/**
 * Created by Frotty on 07.05.2017.
 */
public class AssetDiscovererData {
    private String lastFolder;

    public AssetDiscovererData(String lastFolder) {
        this.lastFolder = lastFolder;
    }

    public String getLastFolder() {
        return lastFolder;
    }
}
