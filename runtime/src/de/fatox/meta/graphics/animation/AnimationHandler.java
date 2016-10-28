package de.fatox.meta.graphics.animation;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * #getCurrentFrame()
 * Returns a textureRegion from an animation according to it's settings and stateTime.
 * Animations can be played directly or queued to be played when the current one finishes.
 */
public class AnimationHandler {
    public Array<Animation> animQueue;
    private Animation currentAnimation;
    private boolean playing = false;
    private float stateTime = 0;

    /**
     * Plays the given animation instantly
     */
    public void playAnimation(Animation animation) {
        stateTime = 0;
        currentAnimation = animation;
        playing = true;
    }

    /**
     * Queues the given animation to be played after the current one finishes,
     * if it is not queued already.
     */
    public void queueAnimation(Animation animation) {
        if (animQueue == null) {
            animQueue = new Array<>(2);
        }
        if (animQueue.size == 0 || animQueue.peek() != animation) {
            animQueue.add(animation);
        }
    }

    /**
     * @return The to be rendered frame of the animation
     */
    public TextureRegion getCurrentFrame() {
        return currentAnimation.getKeyFrame(getStateTime());
    }


    /**
     * Internally updates the animation
     */
    public void update(float delta) {
        if (!playing) {
            return;
        }
        stateTime += delta;
        if (currentAnimation.isAnimationFinished(getStateTime()) && currentAnimation.getPlayMode() != PlayMode.LOOP) {
            if (animQueue != null && animQueue.size > 0) {
                currentAnimation = animQueue.pop();
                stateTime = 0;
            } else {
                stateTime = currentAnimation.getAnimationDuration();
                playing = false;
            }
        }
    }

    /**
     * Randomizes the timeState
     */
    public void randomizeState() {
        setStateTime(MathUtils.random(0, currentAnimation.getAnimationDuration()));
    }

    /**
     * Stops the current animation from playing.
     * It will still stay as the last animation and return the same frame.
     */
    public void stopAnimation() {
        playing = false;
    }

    public float getStateTime() {
        return stateTime;
    }

    public void setStateTime(float stateTime) {
        this.stateTime = stateTime;
    }

    public boolean isFinished() {
        return currentAnimation.isAnimationFinished(stateTime);
    }

    public Animation getCurrentAnimation() {
        return currentAnimation;
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean hasNoQueue() {
        return animQueue == null || animQueue.size == 0;
    }

    public boolean hasQueue() {
        return animQueue != null && animQueue.size > 0;
    }
}
