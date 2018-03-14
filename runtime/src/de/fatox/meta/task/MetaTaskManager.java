package de.fatox.meta.task;

import com.badlogic.gdx.utils.Array;

public class MetaTaskManager {
    private Array<MetaTask> taskHistoryStack = new Array<>();
    private int currentIndex = 0;

    public void runTask(MetaTask metaTask) {
        if (currentIndex < taskHistoryStack.size - 1) {
            taskHistoryStack.setSize(currentIndex + 1);
        }
        metaTask.run();
        taskHistoryStack.add(metaTask);
        currentIndex = taskHistoryStack.size - 1;
    }

    public void undoLastTask() {
        if (taskHistoryStack.size > 0 && currentIndex > 0) {
            taskHistoryStack.get(currentIndex).undo();
            currentIndex--;
        }
    }

    public void redoNextTask() {
        if (currentIndex < taskHistoryStack.size - 1) {
            currentIndex++;
            taskHistoryStack.get(currentIndex).execute();
        }
    }

}
