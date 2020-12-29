package de.fatox.meta.ui.components

import de.fatox.meta.ui.windows.MetaWindow.contentTable
import de.fatox.meta.ui.windows.MetaWindow.close
import de.fatox.meta.Meta.Companion.inject
import de.fatox.meta.api.graphics.FontProvider.getFont
import de.fatox.meta.api.ui.UIRenderer.addActor
import de.fatox.meta.api.PosModifier.modify
import de.fatox.meta.api.ui.UIRenderer.resize
import de.fatox.meta.api.model.MetaWindowData.displayed
import de.fatox.meta.api.model.MetaWindowData.set
import de.fatox.meta.api.model.MetaWindowData.setFrom
import de.fatox.meta.api.lang.LanguageBundle.format
import de.fatox.meta.api.model.MetaAudioVideoData.masterVolume
import de.fatox.meta.api.model.MetaAudioVideoData.musicVolume
import de.fatox.meta.api.AssetProvider.get
import de.fatox.meta.api.model.MetaAudioVideoData.soundVolume
import de.fatox.meta.assets.MetaAssetProvider.get
import de.fatox.meta.api.ui.UIRenderer.getCamera
import de.fatox.meta.ui.windows.MetaWindow
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisLabel
import de.fatox.meta.ui.windows.MetaDialog.DialogListener
import de.fatox.meta.ui.components.MetaClickListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.kotcrab.vis.ui.widget.VisImageButton
import com.badlogic.gdx.utils.Align
import de.fatox.meta.ui.components.MetaTextButton
import kotlin.jvm.JvmOverloads
import com.badlogic.gdx.math.Vector2
import de.fatox.meta.injection.Inject
import de.fatox.meta.api.graphics.FontProvider
import de.fatox.meta.ui.components.MetaLabel
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import de.fatox.meta.Meta
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisTextButton.VisTextButtonStyle
import de.fatox.meta.util.GoldenRatio
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.util.InputValidator
import de.fatox.meta.error.MetaErrorHandler
import com.kotcrab.vis.ui.widget.VisValidatableTextField
import de.fatox.meta.ui.components.MetaInputValidator
import de.fatox.meta.injection.Singleton
import de.fatox.meta.api.ui.UIRenderer
import de.fatox.meta.input.MetaInput
import com.badlogic.gdx.scenes.scene2d.ui.Table
import de.fatox.meta.api.PosModifier
import de.fatox.meta.api.DummyPosModifier
import de.fatox.meta.api.model.MetaWindowData
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.reflect.ClassReflection
import de.fatox.meta.ui.MetaUiManager
import de.fatox.meta.ui.windows.MetaDialog
import java.lang.InstantiationException
import java.lang.IllegalAccessException
import com.badlogic.gdx.utils.TimeUtils
import kotlin.jvm.Synchronized
import de.fatox.meta.task.TaskListener
import de.fatox.meta.task.MetaTask
import com.badlogic.gdx.Game
import de.fatox.meta.api.lang.LanguageBundle
import de.fatox.meta.error.MetaError
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.utils.IntMap
import de.fatox.meta.camera.ArcCamControl
import com.badlogic.gdx.controllers.Controllers
import de.fatox.meta.input.MetaControllerListener
import com.badlogic.gdx.controllers.ControllerListener
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.PovDirection
import com.badlogic.gdx.math.Vector3
import de.fatox.meta.assets.MetaAssetProvider
import de.fatox.meta.api.AssetProvider
import com.badlogic.gdx.utils.ObjectMap
import de.fatox.meta.api.model.MetaAudioVideoData
import de.fatox.meta.sound.MetaSoundDefinition
import com.badlogic.gdx.math.MathUtils
import de.fatox.meta.sound.MetaSoundHandle
import de.fatox.meta.sound.MetaSoundPlayer
import java.lang.NoSuchMethodException
import java.lang.reflect.InvocationTargetException
import de.fatox.meta.assets.MetaData.CacheObj
import com.badlogic.gdx.utils.Json
import java.nio.channels.ReadableByteChannel
import de.fatox.meta.assets.HashUtils
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.lang.RuntimeException
import java.io.IOException
import java.math.BigInteger
import java.nio.channels.SeekableByteChannel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Throws
import de.fatox.meta.assets.XPKByteChannel
import java.util.Arrays
import java.nio.channels.ClosedChannelException
import de.fatox.meta.entity.Meta3DEntity
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.Input.Buttons
import de.fatox.meta.api.entity.EntityManager
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import de.fatox.meta.entity.LightEntity
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import de.fatox.meta.graphics.buffer.MultisampleFBO
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import java.nio.IntBuffer
import java.lang.Exception
import com.badlogic.gdx.Application.ApplicationType
import com.badlogic.gdx.graphics.Color
import java.nio.ByteOrder
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer
import java.lang.IllegalStateException
import com.badlogic.gdx.graphics.Pixmap
import java.util.HashMap
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.StringBuilder
import de.fatox.meta.injection.Qualifier
import de.fatox.meta.injection.MetastasisException
import de.fatox.meta.injection.Metastasis
import java.lang.reflect.ParameterizedType
import java.util.LinkedHashSet
import java.util.HashSet
import de.fatox.meta.injection.Provides

/**
 * A text label, with optional word wrapping.
 *
 *
 * The preferred size of the label is determined by the actual text bounds, unless [word wrap][.setWrap] is enabled.
 *
 * @author Nathan Sweet
 */
class MetaLabel @JvmOverloads constructor(
    text: CharSequence?,
    size: Int,
    color: Color? = Color.WHITE,
    monospace: Boolean = false
) : Widget() {
    val glyphLayout = GlyphLayout()
    private val prefSize = Vector2()
    private val text = StringBuilder()
    private var font: BitmapFont
    private var size: Float

    /** Allows subclasses to access the cache in [.draw].  */
    protected var bitmapFontCache: BitmapFontCache
        private set
    var labelAlign = Align.left
        private set
    var lineAlign = Align.left
        private set
    private var wrap = false
    private var lastPrefHeight = 0f
    private var prefSizeInvalid = true
    private var fontScaleX = 1f
    private var fontScaleY = 1f
    private var ellipsis: String? = null

    @Inject
    private val metaFontProvider: FontProvider? = null
    private val fontColor: Color?
    private val mono: Boolean

    /** @param newText May be null, "" will be used.
     */
    fun setText(newText: CharSequence?) {
        var newText = newText
        if (newText == null) newText = ""
        if (newText is StringBuilder) {
            if (text == newText) return
            text.setLength(0)
            text.append(newText as StringBuilder?)
        } else {
            if (textEquals(newText)) return
            text.setLength(0)
            text.append(newText)
        }
        invalidateHierarchy()
    }

    fun textEquals(other: CharSequence): Boolean {
        val length = text.length
        val chars = text.chars
        if (length != other.length) return false
        for (i in 0 until length) if (chars[i] != other[i]) return false
        return true
    }

    fun getText(): StringBuilder {
        return text
    }

    override fun invalidate() {
        super.invalidate()
        prefSizeInvalid = true
    }

    private fun scaleAndComputePrefSize() {
        val font = bitmapFontCache.font
        val oldScaleX = font.scaleX
        val oldScaleY = font.scaleY
        if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.data.setScale(fontScaleX, fontScaleY)
        computePrefSize()
        if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.data.setScale(oldScaleX, oldScaleY)
    }

    private fun computePrefSize() {
        prefSizeInvalid = false
        val prefSizeLayout = prefSizeLayout
        if (wrap && ellipsis == null) {
            val width = width
            prefSizeLayout.setText(bitmapFontCache.font, text, Color.WHITE, width, Align.left, true)
        } else prefSizeLayout.setText(bitmapFontCache.font, text)
        prefSize[prefSizeLayout.width] = prefSizeLayout.height
    }

    override fun layout() {
        val font = bitmapFontCache.font
        val oldScaleX = font.scaleX
        val oldScaleY = font.scaleY
        if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.data.setScale(fontScaleX, fontScaleY)
        val wrap = wrap && ellipsis == null
        if (wrap) {
            val prefHeight = prefHeight
            if (prefHeight != lastPrefHeight) {
                lastPrefHeight = prefHeight
                invalidateHierarchy()
            }
        }
        val width = width
        val height = height
        var x = 0f
        var y = 0f
        val layout = glyphLayout
        val textWidth: Float
        val textHeight: Float
        if (wrap || text.indexOf("\n") != -1) {
            // If the text can span multiple lines, determine the text's actual size so it can be aligned within the label.
            layout.setText(font, text, 0, text.length, Color.WHITE, width, lineAlign, wrap, ellipsis)
            textWidth = layout.width
            textHeight = layout.height
            if (labelAlign and Align.left == 0) {
                x += if (labelAlign and Align.right != 0) width - textWidth else (width - textWidth) / 2
            }
        } else {
            textWidth = width
            textHeight = font.data.capHeight
        }
        if (labelAlign and Align.top != 0) {
            y += if (bitmapFontCache.font.isFlipped) 0 else height - textHeight
            y += font.descent
        } else if (labelAlign and Align.bottom != 0) {
            y += if (bitmapFontCache.font.isFlipped) height - textHeight else 0
            y -= font.descent
        } else {
            y += (height - textHeight) / 2
        }
        if (!bitmapFontCache.font.isFlipped) y += textHeight
        layout.setText(font, text, 0, text.length, Color.WHITE, textWidth, lineAlign, wrap, ellipsis)
        bitmapFontCache.setText(layout, x, y)
        if (fontScaleX != oldScaleX || fontScaleY != oldScaleY) font.data.setScale(oldScaleX, oldScaleY)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        validate()
        val color = tempColor.set(color)
        color.a *= parentAlpha
        if (fontColor != null) color.mul(fontColor)
        bitmapFontCache.tint(color)
        bitmapFontCache.setPosition(x, y)
        bitmapFontCache.draw(batch)
    }

    override fun getPrefWidth(): Float {
        if (wrap) return 0
        if (prefSizeInvalid) scaleAndComputePrefSize()
        return prefSize.x
    }

    override fun getPrefHeight(): Float {
        if (prefSizeInvalid) scaleAndComputePrefSize()
        return prefSize.y - font.descent * fontScaleY * 2
    }

    /**
     * If false, the text will only wrap where it contains newlines (\n). The preferred size of the label will be the text bounds.
     * If true, the text will word wrap using the width of the label. The preferred width of the label will be 0, it is expected
     * that something external will set the width of the label. Wrapping will not occur when ellipsis is enabled. Default is false.
     *
     *
     * When wrap is enabled, the label's preferred height depends on the width of the label. In some cases the parent of the label
     * will need to layout twice: once to set the width of the label and a second time to adjust to the label's new preferred
     * height.
     */
    fun setWrap(wrap: Boolean) {
        this.wrap = wrap
        invalidateHierarchy()
    }

    /**
     * @param alignment Aligns all the text within the label (default left center) and each line of text horizontally (default
     * left).
     * @see Align
     */
    fun setAlignment(alignment: Int) {
        setAlignment(alignment, alignment)
    }

    /**
     * @param labelAlign Aligns all the text within the label (default left center).
     * @param lineAlign  Aligns each line of text horizontally (default left).
     * @see Align
     */
    fun setAlignment(labelAlign: Int, lineAlign: Int) {
        this.labelAlign = labelAlign
        if (lineAlign and Align.left != 0) this.lineAlign =
            Align.left else if (lineAlign and Align.right != 0) this.lineAlign = Align.right else this.lineAlign =
            Align.center
        invalidate()
    }

    fun setFontScale(fontScale: Float) {
        fontScaleX = fontScale
        fontScaleY = fontScale
        invalidateHierarchy()
    }

    fun setFontScale(fontScaleX: Float, fontScaleY: Float) {
        this.fontScaleX = fontScaleX
        this.fontScaleY = fontScaleY
        invalidateHierarchy()
    }

    fun getFontScaleX(): Float {
        return fontScaleX
    }

    fun setFontScaleX(fontScaleX: Float) {
        this.fontScaleX = fontScaleX
        invalidateHierarchy()
    }

    fun getFontScaleY(): Float {
        return fontScaleY
    }

    fun setFontScaleY(fontScaleY: Float) {
        this.fontScaleY = fontScaleY
        invalidateHierarchy()
    }

    /**
     * When non-null the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur
     * when ellipsis is enabled. Default is false.
     */
    fun setEllipsis(ellipsis: String?) {
        this.ellipsis = ellipsis
    }

    /**
     * When true the text will be truncated "..." if it does not fit within the width of the label. Wrapping will not occur when
     * ellipsis is true. Default is false.
     */
    fun setEllipsis(ellipsis: Boolean) {
        if (ellipsis) this.ellipsis = "..." else this.ellipsis = null
    }

    override fun toString(): String {
        return super.toString() + ": " + text
    }

    fun setMaxWidth(maxWidth: Int) {
        while (glyphLayout.width > maxWidth) {
            size *= 0.95f
            updateFont()
        }
    }

    private fun updateFont() {
        font = metaFontProvider!!.getFont(Math.round(size), mono)
        setText(text)
        bitmapFontCache = font.newFontCache()
        layout()
    }

    fun setFontSize(size: Int) {
        this.size = size.toFloat()
        updateFont()
    }

    /**
     * The style for a label, see [MetaLabel].
     *
     * @author Nathan Sweet
     */
    class LabelStyle {
        var font: BitmapFont? = null

        /** Optional.  */
        var fontColor: Color? = null

        /** Optional.  */
        var background: Drawable? = null

        constructor() {}
        constructor(font: BitmapFont?, fontColor: Color?) {
            this.font = font
            this.fontColor = fontColor
        }

        constructor(style: LabelStyle) {
            font = style.font
            if (style.fontColor != null) fontColor = Color(style.fontColor)
            background = style.background
        }
    }

    companion object {
        private val tempColor = Color()
        private val prefSizeLayout = GlyphLayout()
    }

    init {
        inject(this)
        mono = monospace
        this.size = size.toFloat()
        font = metaFontProvider!!.getFont(size, monospace)
        setAlignment(Align.center)
        fontColor = color
        setText(text)
        bitmapFontCache = font.newFontCache()
        layout()
    }
}