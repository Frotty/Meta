package de.fatox.meta.task;

import com.badlogic.gdx.utils.Array;

/**
 * A MetaTask is any action that should be reversible and actions that are not instant
 */
public abstract class MetaTask {
    private Array<TaskListener> listeners = new Array<>();

    public void run() {
        for(TaskListener listener : listeners) {
            listener.onStart();
        }
        execute();
        for(TaskListener listener : listeners) {
            listener.onFinish();
        }
    }

    public abstract String getName();

    public abstract void execute();

    public abstract void undo();
}
