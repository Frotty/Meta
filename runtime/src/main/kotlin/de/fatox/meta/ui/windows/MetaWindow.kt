package de.fatox.meta.ui.windows

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import de.fatox.meta.api.AssetProvider
import de.fatox.meta.api.extensions.onChange
import de.fatox.meta.api.ui.UIManager
import de.fatox.meta.assets.MetaData
import de.fatox.meta.injection.MetaInject.Companion.lazyInject
import de.fatox.meta.reactive.ReactiveScope

/**
 * Created by Frotty on 08.05.2016.
 */
abstract class MetaWindow(
	title: String,
	resizable: Boolean = false,
	closeButton: Boolean = false,
) : VisWindow(title, if (resizable) "resizable" else "default") {
	protected val uiManager: UIManager by lazyInject()
	protected val assetProvider: AssetProvider by lazyInject()
	protected val metaData: MetaData by lazyInject()

	var contentTable: Table = VisTable()

	/**
	 * Reactive bindings owned by this window's current on-screen presentation. Create bindings/effects here inside
	 * [onShown] (e.g. `reactiveScope.bindText(label) { someSignal() }`); they are disposed automatically in
	 * [onRemovedFromStage], so a window can be shown, hidden and re-shown without leaking effects or firing them on a
	 * hidden widget. Do NOT use it from `init` - those run once at construction, before the window is on a stage.
	 */
	protected var reactiveScope: ReactiveScope = ReactiveScope()
		private set

	private var startDrag = false

	init {
		titleLabel.setAlignment(Align.left)

		titleTable.apply {
			left().padLeft(2f)
			if (closeButton) {
				val exitButton = VisImageButton("close-window").apply {
					setColor(1f, 1f, 1f, 0.2f)
					onChange { close() }
					addListener(object : ClickListener() {
						override fun touchDown(
							event: InputEvent,
							x: Float,
							y: Float,
							pointer: Int,
							button: Int
						): Boolean {
							event.cancel()
							return true
						}
					})
				}
				add(exitButton).padRight(-padRight + 0.7f)
			}

			// Separator
			top()
			row().height(2f)
			add(Separator()).growX().padTop(2f).colspan(if (closeButton) 2 else 1)
			padTop(2f)
		}

		if (resizable) {
			padBottom(6f)
			super.setResizable(true)
		}
		contentTable.top().pad(5f, 1f, 1f, 1f)
		add(contentTable).top().grow()
		row()
	}

	fun setDefaultSize(width: Float, height: Float) {
		setDefault(x, y, width, height)
	}

	fun setDefaultPos(x: Float, y: Float) {
		setDefault(x, y, width, height)
	}

	fun setDefault(x: Float, y: Float, width: Float, height: Float) {
		setPosition(x, y)
		setSize(width, height)
	}

	override fun draw(batch: Batch, parentAlpha: Float) {
		super.draw(batch, parentAlpha)
		if (isDragging) {
			startDrag = true
		} else if (startDrag) {
			startDrag = false
			uiManager.updateWindow(this)
		}
	}

	override fun setVisible(visible: Boolean) {
		super.setVisible(visible)
		setColor(1f, 1f, 1f, 0f)
		addAction(Actions.alpha(0.9f, 0.75f))
	}

	override fun setResizable(isResizable: Boolean) {
		super.setResizable(isResizable)
		if (VisUI.getSkin().has("resizeable", WindowStyle::class.java)) {
			style = VisUI.getSkin().get(if (isResizable) "resizeable" else "default", WindowStyle::class.java)
		}
	}

	public override fun close() {
		super.close()
		uiManager.closeWindow(this)
	}

	/**
	 * Drives the presentation lifecycle off scene2d's single detach/attach signal (every add/remove path goes through
	 * `setStage`). On attach we (re)open [reactiveScope] and call [onShown]; on detach we call [onRemovedFromStage]
	 * and dispose the scope, tearing down every binding created during this presentation.
	 */
	override fun setStage(stage: Stage?) {
		val wasOnStage = this.stage != null
		super.setStage(stage)
		if (stage != null && !wasOnStage) {
			if (reactiveScope.isDisposed) reactiveScope = ReactiveScope()
			onShown()
		} else if (wasOnStage && stage == null) {
			onRemovedFromStage()
			reactiveScope.dispose()
		}
	}

	/** Called when this window is attached to the stage (shown). Create reactive bindings in [reactiveScope] here. */
	protected open fun onShown() {}

	/** Called once when this window is detached from the stage by ANY path (close, screen change, direct remove). */
	protected open fun onRemovedFromStage() {}
}
