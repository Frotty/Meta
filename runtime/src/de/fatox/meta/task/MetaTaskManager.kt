package de.fatox.meta.task

import com.badlogic.gdx.utils.Array

class MetaTaskManager {
	private val taskHistoryStack = Array<MetaTask>()
	private var currentIndex = -1

	fun runTask(metaTask: MetaTask) {
		if (currentIndex < taskHistoryStack.size - 1) {
			taskHistoryStack.setSize(currentIndex + 1)
		}
		metaTask.run()
		taskHistoryStack.add(metaTask)
		currentIndex = taskHistoryStack.size - 1
	}

	fun undoLastTask() {
		if (taskHistoryStack.size > 0 && currentIndex >= 0) {
			taskHistoryStack[currentIndex].undo()
			currentIndex--
		}
	}

	fun redoNextTask() {
		if (currentIndex < taskHistoryStack.size - 1) {
			currentIndex++
			taskHistoryStack[currentIndex].run()
		}
	}

	fun reset() {
		taskHistoryStack.clear()
		currentIndex = -1
	}
}