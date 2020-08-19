package de.fatox.meta.task;

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.utils.Array;

/**
 * A MetaTask is any action that should be reversible and actions that are not instant
 */
public abstract class MetaTaskQueue extends MetaTask {
    private final ProgressBar progressBar;
    private MetaTask currentTask;
    private Array<MetaTask> tasks = new Array<>();

    public MetaTaskQueue(ProgressBar progressBar, MetaTask... tasks) {
        this.progressBar = progressBar;
        this.tasks.addAll(tasks);
    }

    public void add(MetaTask task, boolean startIfEmpty) {
        tasks.add(task);
        if (currentTask == null && startIfEmpty && tasks.size == 1) {
            start();
        }
    }

    public void start() {
        currentTask = tasks.pop();
        currentTask.execute();
    }

    public void onTaskFinished(MetaTask task) {

    }

}
