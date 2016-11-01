package de.fatox.meta.task;

import com.badlogic.gdx.utils.Array;

public class MetaTaskManager {

    private Array<MetaTask> taskHistoryStack = new Array<>();

    public void runTask(MetaTask metaTask) {
        metaTask.run();
        taskHistoryStack.add(metaTask);
    }

    public void reverseLastTask() {
        taskHistoryStack.pop().undo();
    }

}
