package de.fatox.meta.task

import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class MetaTaskManagerTest {
	private val taskManager: MetaTaskManager = MetaTaskManager()
	private val state: MutableList<String> = mutableListOf()

	@Test
	fun `execute 1 Task successfully`() {
		assertEquals(emptyList(), state)

		taskManager.runTask(AddStringTask(state, "value_00"))
		assertEquals(listOf("value_00"), state)
	}

	@Test
	fun `execute then undo 1 Task successfully`() {
		assertEquals(emptyList(), state)

		taskManager.runTask(AddStringTask(state, "value_00"))
		taskManager.undoLastTask()
		assertEquals(emptyList(), state)
	}

	@Test
	fun `execute then undo then redo 1 Task successfully`() {
		assertEquals(emptyList(), state)

		taskManager.runTask(AddStringTask(state, "value_00"))
		taskManager.undoLastTask()
		taskManager.redoNextTask()
		assertEquals(listOf("value_00"), state)
	}

	@Test
	fun `undoLastTask can be execute any number of times then redoNextTask still works successfully`() {
		assertEquals(emptyList(), state)

		repeat(5) { taskManager.runTask(AddStringTask(state, "value_0$it")) }
		repeat(10) { taskManager.undoLastTask() }
		taskManager.redoNextTask()
		assertEquals(listOf("value_00"), state)
	}

	@Test
	fun `undoLastTask can be executed without any tasks successfully`() {
		assertEquals(emptyList(), state)

		repeat(10) { taskManager.undoLastTask() }
		assertEquals(emptyList(), state)
	}

	@Test
	fun `redoNextTask can be executed without any tasks successfully`() {
		assertEquals(emptyList(), state)

		repeat(10) { taskManager.redoNextTask() }
		assertEquals(emptyList(), state)
	}

	@Test
	fun `run 10 Tasks then undo 4 then redo 3 successfully`() {
		assertEquals(emptyList(), state)

		repeat(10) { taskManager.runTask(AddStringTask(state, "value_0$it")) }
		assertEquals(List(10) { "value_0$it" }, state)
		repeat(4) { taskManager.undoLastTask() }
		assertEquals(List(6) { "value_0$it" }, state)
		repeat(3) { taskManager.redoNextTask() }
		assertEquals(List(9) { "value_0$it" }, state)
	}

	@Test
	fun `task before and after reset works successfully`() {
		assertEquals(emptyList(), state)

		taskManager.runTask(AddStringTask(state, "value_00"))
		assertEquals(List(1) { "value_0$it" }, state)
		taskManager.reset()
		taskManager.runTask(AddStringTask(state, "value_01"))
		assertEquals(List(2) { "value_0$it" }, state)
	}

	@Test
	fun `undo after reset does not work`() {
		assertEquals(emptyList(), state)

		taskManager.runTask(AddStringTask(state, "value_00"))
		assertEquals(List(1) { "value_0$it" }, state)
		taskManager.reset()
		taskManager.undoLastTask()
		assertEquals(List(1) { "value_0$it" }, state)
	}

	@Test
	fun `runTask clears redo future successfully`() {
		assertEquals(emptyList(), state)

		repeat(10) { taskManager.runTask(AddStringTask(state, "value_0$it")) }
		assertEquals(List(10) { "value_0$it" }, state)
		taskManager.undoLastTask()
		assertEquals(List(9) { "value_0$it" }, state)
		taskManager.runTask(AddStringTask(state, "value_0X"))
		assertEquals(MutableList(9) { "value_0$it" }.also { it.add("value_0X") }, state)
		taskManager.redoNextTask()
		assertEquals(MutableList(9) { "value_0$it" }.also { it.add("value_0X") }, state)
	}

}

private class AddStringTask(private val state: MutableList<String>, private val newValue: String) :
	MetaTask("AddStringTask") {

	override fun execute() {
		state.add(newValue)
	}

	override fun undo() {
		state.removeLast()
	}
}