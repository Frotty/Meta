package de.fatox.meta.error;

public abstract class MetaError {
	private final String errorName;
	private final String errorDescription;

	public MetaError(String errorName, String errorDescription) {
		this.errorName = errorName;
		this.errorDescription = errorDescription;
	}

	public abstract void gotoError();

	public String getName() {
		return errorName;
	}
}
