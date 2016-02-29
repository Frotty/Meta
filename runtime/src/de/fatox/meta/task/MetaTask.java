package de.fatox.meta.task;

/**
 * A MetaTask is any action that happens in the editor.
 * Everything has to be wrapped inside it so we can have undo/redo support
 */
public abstract class MetaTask {

    public abstract String getName();

    public abstract void execute();

    public abstract void reverse();
}
