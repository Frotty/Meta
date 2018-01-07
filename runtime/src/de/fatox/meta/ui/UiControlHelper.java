package de.fatox.meta.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;
import de.fatox.meta.api.ui.UIManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Singleton;
import de.fatox.meta.input.KeyListener;
import de.fatox.meta.input.MetaInput;

import java.util.Iterator;

@Singleton
public class UiControlHelper {
    @Inject
    private MetaInput metaInput;
    @Inject
    private UIManager uiManager;

    private Actor selectedActor = null;
    private Color selectedColor = Color.WHITE.cpy();
    private boolean activated = true;

    public UiControlHelper() {
        Meta.inject(this);
        metaInput.registerGlobalKeyListener(Input.Keys.RIGHT, new KeyListener() {
            @Override
            public void onEvent() {
                if (activated) {
                    setSelectedActor(getNextX(false));
                }
            }
        });
        metaInput.registerGlobalKeyListener(Input.Keys.LEFT, new KeyListener() {
            @Override
            public void onEvent() {
                if (activated) {
                    setSelectedActor(getNextX(true));
                }
            }
        });
        metaInput.registerGlobalKeyListener(Input.Keys.DOWN, new KeyListener() {
            @Override
            public void onEvent() {
                if (activated) {
                    setSelectedActor(getNextY(false));
                }
            }
        });
        metaInput.registerGlobalKeyListener(Input.Keys.UP, new KeyListener() {
            @Override
            public void onEvent() {
                if (activated) {
                    setSelectedActor(getNextY(true));
                }
            }
        });
        metaInput.registerGlobalKeyListener(Input.Keys.ENTER, new KeyListener() {
            @Override
            public void onEvent() {
                if (activated) {
                    InputEvent inputEvent = new InputEvent();
                    inputEvent.setButton(Input.Buttons.LEFT);
                    inputEvent.setStageX(selectedActor.getX());
                    inputEvent.setStageY(selectedActor.getY());
                    inputEvent.setType(InputEvent.Type.touchDown);
                    inputEvent.setListenerActor(selectedActor);
                    selectedActor.getListeners().forEach((eventListener -> eventListener.handle(inputEvent)));
                    inputEvent.setType(InputEvent.Type.touchUp);
                    selectedActor.getListeners().forEach((eventListener -> eventListener.handle(inputEvent)));
                }
            }
        });
    }

    private void targetsInGroup(Group t) {
        for (Actor actor : t.getChildren()) {
            if (actor instanceof Button && !targets.contains(actor, true)) {
                targets.add(actor);
            } else if (actor instanceof Group) {
                targetsInGroup((Group) actor);
            }
        }
    }

    Array<Actor> targets = new Array<>();

    private Array<Actor> getPossibleTargets() {
        targets.clear();
        if (!selectedActor.isVisible()) {
            selectedActor = selectedActor.getStage().getActors().get(0);
        }
        Group parent = selectedActor instanceof Group ? (Group) selectedActor : selectedActor.getParent();
        while (parent != null) {
            targetsInGroup(parent);
            parent = parent.getParent();
        }
        return targets;
    }

    private Vector2 helper = new Vector2();

    private Actor getNextX(boolean left) {
        Array<Actor> possibleTargets = getPossibleTargets();

        possibleTargets.sort((a1, a2) -> {
                    float a1x = a1.localToStageCoordinates(helper).x;
                    float a2x = a2.localToStageCoordinates(helper).x;
                    return (int) (a2x - a1x);
                }
        );
        Iterator<Actor> iterator = possibleTargets.iterator();
        while (iterator.hasNext()) {
            Actor next = iterator.next();
            if (Math.abs(next.getY() - selectedActor.getY()) > (selectedActor.getHeight() * 2.75f)) {
                iterator.remove();
            }
        }
        int index = possibleTargets.indexOf(selectedActor, true);
        return getNext(left, possibleTargets, index);
    }

    private Actor getNextY(boolean up) {
        Array<Actor> possibleTargets = getPossibleTargets();

        possibleTargets.sort((a1, a2) -> {
                    float a1y = a1.localToStageCoordinates(helper).y;
                    float a2y = a2.localToStageCoordinates(helper).y;
                    return (int) (a2y - a1y);
                }
        );
        Iterator<Actor> iterator = possibleTargets.iterator();
        while (iterator.hasNext()) {
            Actor next = iterator.next();
            if (Math.abs(next.getX() - selectedActor.getX()) > (selectedActor.getWidth() * 1.25f)) {
                iterator.remove();
            }
        }
        int index = possibleTargets.indexOf(selectedActor, true);
        return getNext(up, possibleTargets, index);
    }

    private Actor getNext(boolean left, Array<Actor> possibleTargets, int index) {
        if (left) {
            if (index == 0) {
                return possibleTargets.get(possibleTargets.size - 1);
            } else {
                return possibleTargets.get(index - 1);
            }
        } else {
            if (index == possibleTargets.size - 1) {
                return possibleTargets.get(0);
            } else {
                return possibleTargets.get(index + 1);
            }
        }
    }

    public Actor getSelectedActor() {
        return selectedActor;
    }

    public void setSelectedActor(Actor selectedActor) {
        if (this.selectedActor != null) {
            this.selectedActor.setColor(selectedColor);
        }
        this.selectedActor = selectedActor;
        this.selectedColor.set(selectedActor.getColor());
        this.selectedActor.setColor(Color.LIGHT_GRAY);
    }

}
