package de.fatox.meta.input;

import com.badlogic.gdx.utils.Timer;

public abstract class KeyListener {
    private long requiredLengthMilis = 0;
    private Timer.Task task = null;

    public abstract void onEvent();

    public void onDown() {
        if (requiredLengthMilis > 0) {
            task = new Timer.Task() {
                @Override
                public void run() {
                    Timer.schedule(task, requiredLengthMilis / 1000f);
                    onEvent();
                }
            };
            Timer.schedule(task, requiredLengthMilis / 1000f);
        }
    }

    public void onUp() {
        if (task != null) {
            task.cancel();
        }
        if(requiredLengthMilis <= 0) {
            onEvent();
        }
    }

    public void resetDelay() {
        task.cancel();
        Timer.schedule(task, requiredLengthMilis / 1000f);
    }

    public void setRequiredLengthMilis(long requiredLengthMilis) {
        this.requiredLengthMilis = requiredLengthMilis;
    }

}
