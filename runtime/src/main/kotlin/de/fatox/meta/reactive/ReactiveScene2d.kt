package de.fatox.meta.reactive

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.Disableable

/**
 * Reactive bridges for non-reactive scene2d/VisUI widgets: instead of manually re-setting a label's text or a
 * button's disabled state every time some state changes, bind the widget to a [ReactiveValue] (or any tracking
 * lambda) once and let it keep itself in sync.
 *
 * Each binding is an [effect], so it:
 *  - runs immediately (the widget starts in the correct state), then re-runs only when something it read changed, and
 *  - returns a [Disposable] you MUST tear down with the widget's owner. Register it in a [ReactiveScope] (e.g. a
 *    screen/window scope) - scene2d gives no "removed from stage" callback, so an un-disposed binding leaks and keeps
 *    firing on a dead widget. For app-lifetime widgets on a DI singleton you may simply never dispose it.
 *
 * All bindings must be created on the GL/render thread (see [signal]'s threading note).
 */

/** Keeps [Label]'s text in sync with [text]. */
fun Label.bindText(text: () -> CharSequence): Disposable = effect("bindText") { setText(text()) }

/** Keeps [Label]'s text in sync with a reactive string value. */
fun Label.bindText(value: ReactiveValue<out CharSequence>): Disposable = bindText { value.value }

/** Keeps an [Actor]'s visibility in sync with [visible]. */
fun Actor.bindVisible(visible: () -> Boolean): Disposable = effect("bindVisible") { isVisible = visible() }

/** Keeps a [Disableable] widget's disabled state in sync with [disabled]. */
fun Disableable.bindDisabled(disabled: () -> Boolean): Disposable = effect("bindDisabled") { isDisabled = disabled() }

/** Keeps a table [Cell]'s actor's visibility in sync with [visible] (handy for show/hide-driven layouts). */
fun Cell<*>.bindVisible(visible: () -> Boolean): Disposable =
	effect("bindCellVisible") { actor?.isVisible = visible() }

// --- scope-owned convenience: `scope.bindText(label) { ... }` registers the binding for automatic teardown -------

/** [bindText] owned by this scope. */
fun ReactiveScope.bindText(label: Label, text: () -> CharSequence): Disposable = register(label.bindText(text))

/** [bindVisible] owned by this scope. */
fun ReactiveScope.bindVisible(actor: Actor, visible: () -> Boolean): Disposable = register(actor.bindVisible(visible))

/** [bindDisabled] owned by this scope. */
fun ReactiveScope.bindDisabled(widget: Disableable, disabled: () -> Boolean): Disposable =
	register(widget.bindDisabled(disabled))
