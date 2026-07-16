package de.fatox.meta.task

import com.badlogic.gdx.utils.Array
import de.fatox.meta.reactive.ReactiveValue
import de.fatox.meta.reactive.batch
import de.fatox.meta.reactive.signal

class MetaTaskManager {
	private val taskHistoryStack = Array<MetaTask>()
	private var currentIndex = -1
	private val canUndoSignal = signal(false)
	private val canRedoSignal = signal(false)

	/** Reactive history availability for menus, toolbar buttons, and keyboard action state. */
	val canUndo: ReactiveValue<Boolean> = canUndoSignal
	val canRedo: ReactiveValue<Boolean> = canRedoSignal

	fun runTask(metaTask: MetaTask) {
		metaTask.run()
		if (currentIndex < taskHistoryStack.size - 1) {
			taskHistoryStack.setSize(currentIndex + 1)
		}
		taskHistoryStack.add(metaTask)
		currentIndex = taskHistoryStack.size - 1
		updateAvailability()
	}

	fun undoLastTask() {
		if (taskHistoryStack.size > 0 && currentIndex >= 0) {
			taskHistoryStack[currentIndex].undo()
			currentIndex--
			updateAvailability()
		}
	}

	fun redoNextTask() {
		if (currentIndex < taskHistoryStack.size - 1) {
			currentIndex++
			taskHistoryStack[currentIndex].run()
			updateAvailability()
		}
	}

	fun reset() {
		taskHistoryStack.clear()
		currentIndex = -1
		updateAvailability()
	}

	private fun updateAvailability() {
		batch {
			canUndoSignal.value = currentIndex >= 0
			canRedoSignal.value = currentIndex < taskHistoryStack.size - 1
		}
	}
}
