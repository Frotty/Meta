package de.fatox.meta.test;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import de.fatox.meta.Meta;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.input.Hotkey;
import de.fatox.meta.input.KeyListener;
import de.fatox.meta.input.MetaInput;
import org.junit.Assert;
import org.junit.Test;

import java.awt.*;
import java.awt.event.KeyEvent;

public class ShortcutTest extends MetaTest{
    @Inject
    private MetaInput metaInput;


    public static class TestShortcutClass {
        public TestShortcutClass() {
            Meta.inject(this);
        }

        @Hotkey(keycodes = {Input.Keys.CONTROL_LEFT, Input.Keys.D})
        public void doSomeShit() {
            System.out.println("okies");
        }
    }

    @Test
    public void testSimple() throws AWTException {
        new TestShortcutClass();
        IntMap<Array<KeyListener>> keyListenerMap = metaInput.getKeyListenerMap();
        Assert.assertTrue(keyListenerMap.containsKey(Input.Keys.CONTROL_LEFT));
        Assert.assertTrue(keyListenerMap.containsKey(Input.Keys.D));

        Robot robot = new Robot();
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_D);
    }
}
