package de.fatox.meta.api.ui;

/**
 * Created by Frotty on 25.08.2016.
 */
public class AssetPromise<TYPE> {
    /** This will at first contain a placeholder which is then replaced by the requested drawable */
    private TYPE asset;

    public AssetPromise(TYPE asset) {
        this.asset = asset;
    }

    public synchronized TYPE get() {
        return asset;
    }

    public synchronized void set(TYPE asset) {
        this.asset = asset;
    }
}
