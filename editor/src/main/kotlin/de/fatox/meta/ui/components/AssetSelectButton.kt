package de.fatox.meta.ui.components

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.VisWindow
import de.fatox.meta.Meta
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.injection.Inject
import de.fatox.meta.ui.windows.AssetDiscovererWindow

/**
 * Created by Frotty on 04.07.2016.
 */
class AssetSelectButton {
    val table = VisTable()
    private var selectAssetButton: VisTextButton? = null
    private var assetNameLabel: VisTextField? = null
    private var name: String? = null
    var file: FileHandle? = null
        private set
    private var selectListener: AssetDiscovererWindow.SelectListener? = null

    @Inject
    private val uiManager: UIManager? = null

    constructor(selectedAsset: FileHandle) {
        this.name = selectedAsset.name()
        setup()
        assetNameLabel!!.text = name
    }

    constructor(name: String) {
        Meta.inject(this)
        this.name = name
        setup()
        assetNameLabel!!.text = "Select " + name
    }

    private fun setup() {
        selectAssetButton = VisTextButton("...")
        selectAssetButton!!.addListener(object : MetaClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                var window = uiManager!!.getWindow(AssetDiscovererWindow::class.java)
                if (window == null) window = uiManager.showWindow(AssetDiscovererWindow::class.java)
                window?.enableSelectionMode { selected: FileHandle ->
                    this@AssetSelectButton.file = selected
                    if (selectListener != null) {
                        selectListener!!.onSelect(selected)
                    }
                    assetNameLabel!!.text = name + ": " + file!!.name()
                    // Bring window to Front
                    var table: Group? = this@AssetSelectButton.table
                    while (table != null && table !is Window) {
                        table = table.parent
                    }
                    if (table != null) {
                        val visWindow = table as VisWindow?
                        visWindow?.toFront()
                    }

                }
            }
        })
        assetNameLabel = VisTextField()
        assetNameLabel!!.isDisabled = true
        assetNameLabel!!.isFocusBorderEnabled = false
        table.add<VisTextField>(assetNameLabel).growX()
        table.add<VisTextButton>(selectAssetButton).padLeft(2f)
    }

    fun hasFile(): Boolean {
        return file != null && file!!.exists()
    }

    fun setSelectListener(selectListener: AssetDiscovererWindow.SelectListener) {
        this.selectListener = selectListener
    }
}
