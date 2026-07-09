package de.fatox.meta.ui.components

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.api.ui.showWindow
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.Signal
import de.fatox.meta.reactive.signal
import de.fatox.meta.ui.windows.AssetDiscovererWindow

/**
 * Created by Frotty on 04.07.2016.
 */
class AssetSelectButton {
	val table = MetaTable()
	private lateinit var selectAssetButton: MetaTextButton
	private lateinit var assetNameLabel: MetaTextField
	private var name: String? = null
	val fileValue: Signal<FileHandle?> = signal(null)
	var file: FileHandle? = null
		private set(value) {
			field = value
			fileValue.value = value
		}
	private var selectListener: AssetDiscovererWindow.SelectListener? = null

	private val uiManager: UIManager by lazyInject()

	constructor(selectedAsset: FileHandle) {
		this.name = selectedAsset.name()
		this.file = selectedAsset
		setup()
		assetNameLabel.text = name
	}

	constructor(name: String) {
		this.name = name
		setup()
		assetNameLabel.text = "Select " + name
	}

	private fun setup() {
		selectAssetButton = MetaTextButton("...")
		selectAssetButton.addListener(object : ClickListener() {
			override fun clicked(event: InputEvent, x: Float, y: Float) {
				uiManager.showWindow<AssetDiscovererWindow> {
					enableSelectionMode { selected: FileHandle? ->
						this@AssetSelectButton.file = selected
						selectListener?.onSelect(selected)
						assetNameLabel.text = if (selected != null) {
							name + ": " + selected.name()
						} else {
							"Select " + name
						}
						// Bring window to Front
						var table: Group? = this@AssetSelectButton.table
						while (table != null && table !is Window) {
							table = table.parent
						}
						if (table != null) {
							table.toFront()
						}

					}
				}
			}
		})
		assetNameLabel = MetaTextField()
		assetNameLabel.isDisabled = true
		assetNameLabel.isFocusBorderEnabled = false
		table.add(assetNameLabel).growX()
		table.add(selectAssetButton).padLeft(2f)
	}

	fun hasFile(): Boolean {
		return fileValue()?.exists() == true
	}

	fun setSelectListener(selectListener: AssetDiscovererWindow.SelectListener) {
		this.selectListener = selectListener
	}
}
