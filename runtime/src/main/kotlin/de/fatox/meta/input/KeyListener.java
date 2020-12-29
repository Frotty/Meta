package de.fatox.meta.input;

import com.badlogic.gdx.utils.Timer;

public abstract class KeyListener {
    private long requiredLengthMillis = 0;
    private Timer.Task task = null;

    public abstract void onEvent();

    public void onDown() {
        if (requiredLengthMillis > 0) {
            task = new Timer.Task() {
                @Override
                public void run() {
                    Timer.schedule(task, requiredLengthMillis / 1000f);
                    onEvent();
                }
            };
            Timer.schedule(task, requiredLengthMillis / 1000f);
        }
    }

    public void onUp() {
        if (task != null) {
            task.cancel();
        }
        if(requiredLengthMillis <= 0) {
            onEvent();
        }
    }

    public void resetDelay() {
        task.cancel();
        Timer.schedule(task, requiredLengthMillis / 1000f);
    }

    public void setRequiredLengthMillis(long requiredLengthMillis) {
        this.requiredLengthMillis = requiredLengthMillis;
    }

}
