package de.fatox.meta.api.ui

import de.fatox.meta.ui.MetaButtonTier

/** Semantic toast presentation and its default lifetime. A null lifetime means the toast remains until dismissed. */
enum class MetaToastType(val defaultAutoDismissSeconds: Float?) {
	NOTIFICATION(UIManager.DEFAULT_TOAST_SECONDS),
	PRIMARY(null),
	WARNING(UIManager.IMPORTANT_TOAST_SECONDS),
	ERROR(null),
	MUTED(UIManager.DEFAULT_TOAST_SECONDS),
}

class MetaToastAction @JvmOverloads constructor(
	val label: String,
	val tier: MetaButtonTier = MetaButtonTier.PRIMARY,
	val action: () -> Unit,
) {
	init {
		require(label.isNotBlank()) { "Toast action label must not be blank" }
	}
}

/**
 * Consumer-facing toast request. Prefer the factory methods for the common notification, error and invite flows.
 */
class MetaToastSpec @JvmOverloads constructor(
	val message: String,
	val type: MetaToastType = MetaToastType.NOTIFICATION,
	val autoDismissSeconds: Float? = type.defaultAutoDismissSeconds,
	val primaryAction: MetaToastAction? = null,
	val dismissLabel: String? = if (autoDismissSeconds == null) "Dismiss" else null,
	val onDismiss: () -> Unit = {},
) {
	init {
		require(message.isNotBlank()) { "Toast message must not be blank" }
		require(autoDismissSeconds == null || autoDismissSeconds >= 0f) {
			"Toast auto-dismiss duration must not be negative"
		}
		require(dismissLabel == null || dismissLabel.isNotBlank()) { "Toast dismiss label must not be blank" }
	}

	companion object {
		@JvmStatic
		@JvmOverloads
		fun notification(
			message: String,
			type: MetaToastType = MetaToastType.NOTIFICATION,
			duration: Float = type.defaultAutoDismissSeconds ?: UIManager.DEFAULT_TOAST_SECONDS,
		): MetaToastSpec = MetaToastSpec(message, type, duration, dismissLabel = null)

		@JvmStatic
		@JvmOverloads
		fun error(
			message: String,
			dismissLabel: String = "Dismiss",
			onDismiss: () -> Unit = {},
		): MetaToastSpec = MetaToastSpec(
			message = message,
			type = MetaToastType.ERROR,
			autoDismissSeconds = null,
			dismissLabel = dismissLabel,
			onDismiss = onDismiss,
		)

		@JvmStatic
		@JvmOverloads
		fun invite(
			message: String,
			acceptLabel: String = "Accept",
			dismissLabel: String = "Dismiss",
			onAccept: () -> Unit,
			onDismiss: () -> Unit = {},
		): MetaToastSpec = MetaToastSpec(
			message = message,
			type = MetaToastType.PRIMARY,
			autoDismissSeconds = null,
			primaryAction = MetaToastAction(acceptLabel, MetaButtonTier.PRIMARY, onAccept),
			dismissLabel = dismissLabel,
			onDismiss = onDismiss,
		)
	}
}
