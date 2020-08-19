package de.fatox.meta.api.ui

/**
 * Created by Frotty on 25.08.2016.
 */
class AssetPromise<TYPE>(
	/** This will at first contain a placeholder which is then replaced by the requested drawable  */
	private var asset: TYPE
) {
	@Synchronized
	fun get(): TYPE {
		return asset
	}

	@Synchronized
	fun set(asset: TYPE) {
		this.asset = asset
	}
}