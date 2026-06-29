package de.fatox.meta.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import de.fatox.meta.reactive.Disposable
import de.fatox.meta.reactive.ReactiveScope
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.effect
import de.fatox.meta.ui.components.MetaLabel
import de.fatox.meta.ui.components.MetaTextButton

/**
 * Reactive bindings for Meta's scene2d/VisUI widgets: bind a widget to a [ReactiveValue] (or any tracking lambda)
 * once and it keeps itself in sync - no manual re-setting, no "re-query and rebuild" glue. This is the fine-grained
 * complement to the coarse `someSignal.subscribe { rebuildEverything() }` pattern: use a binding when exactly one
 * property of one widget tracks one piece of state.
 *
 * These target the TTF Meta widgets ([MetaLabel], [MetaTextButton]) as well as generic scene2d ([Actor],
 * [Disableable]); they live in `de.fatox.meta.ui` (not in the dependency-free reactive core).
 *
 * Each binding is an [effect], so it runs immediately (the widget starts correct) and re-runs only on a real change,
 * and returns a [Disposable] you MUST tear down with the widget's owner - scene2d has no "removed" callback, so an
 * un-disposed binding leaks and fires on a dead widget. Register it in a [ReactiveScope] (see the scope-owned
 * overloads) and dispose the scope on screen/window teardown. All bindings must be created on the GL/render thread.
 */

// --- text -------------------------------------------------------------------------------------------------------

/** Keeps a [MetaLabel]'s text in sync with [text]. */
fun MetaLabel.bindText(text: () -> CharSequence): Disposable = effect("bindText") { setText(text()) }

/** Keeps a [MetaLabel]'s text in sync with a reactive value. */
fun MetaLabel.bindText(value: ReactiveValue<out CharSequence>): Disposable = bindText { value.value }

/** Keeps a [MetaTextButton]'s label in sync with [text]. */
fun MetaTextButton.bindText(text: () -> String): Disposable = effect("bindText") { setText(text()) }

/** Keeps a [MetaTextButton]'s label in sync with a reactive value. */
fun MetaTextButton.bindText(value: ReactiveValue<out String>): Disposable = bindText { value.value }

/** Keeps a plain scene2d [Label]'s text in sync with [text] (for non-Meta labels). */
fun Label.bindText(text: () -> CharSequence): Disposable = effect("bindText") { setText(text()) }

// --- visibility / color / disabled ------------------------------------------------------------------------------

/** Keeps an [Actor]'s visibility in sync with [visible]. */
fun Actor.bindVisible(visible: () -> Boolean): Disposable = effect("bindVisible") { isVisible = visible() }

/** Keeps an [Actor]'s tint in sync with [color] (copies the value in, so the source color is never aliased). */
fun Actor.bindColor(color: () -> Color): Disposable = effect("bindColor") { this@bindColor.color.set(color()) }

/** Keeps a [Disableable] widget's disabled state in sync with [disabled]. */
fun Disableable.bindDisabled(disabled: () -> Boolean): Disposable = effect("bindDisabled") { isDisabled = disabled() }

/** Keeps a table [Cell]'s actor's visibility in sync with [visible] (handy for show/hide-driven layouts). */
fun Cell<*>.bindVisible(visible: () -> Boolean): Disposable = effect("bindCellVisible") { actor?.isVisible = visible() }

// --- scope-owned convenience: `scope.bindText(label) { ... }` registers the binding for automatic teardown --------

fun ReactiveScope.bindText(label: MetaLabel, text: () -> CharSequence): Disposable = register(label.bindText(text))
fun ReactiveScope.bindText(button: MetaTextButton, text: () -> String): Disposable = register(button.bindText(text))
fun ReactiveScope.bindVisible(actor: Actor, visible: () -> Boolean): Disposable = register(actor.bindVisible(visible))
fun ReactiveScope.bindColor(actor: Actor, color: () -> Color): Disposable = register(actor.bindColor(color))
fun ReactiveScope.bindDisabled(widget: Disableable, disabled: () -> Boolean): Disposable =
	register(widget.bindDisabled(disabled))
