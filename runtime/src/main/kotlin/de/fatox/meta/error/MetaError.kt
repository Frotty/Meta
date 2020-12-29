package de.fatox.meta.error;

public abstract class MetaError {
    private String errorName;
    private String errorDescription;

    public MetaError(String errorName, String errorDescription) {
        this.errorName = errorName;
        this.errorDescription = errorDescription;
    }

    public abstract void gotoError();

    public String getName() {
        return errorName;
    }
}
