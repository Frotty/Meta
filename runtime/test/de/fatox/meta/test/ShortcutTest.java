package de.fatox.meta.test;

import de.fatox.meta.injection.Inject;
import de.fatox.meta.input.MetaInput;

public class ShortcutTest extends MetaTest {
    @Inject
    private MetaInput metaInput;


//    public static class TestShortcutClass {
//        public TestShortcutClass() {
//            Meta.inject(this);
//        }
//
//        @Hotkey(keycodes = {Input.Keys.CONTROL_LEFT, Input.Keys.D})
//        public void doSomeShit() {
//            System.out.println("okies");
//        }
//    }
//
//    @Test
//    public void testSimple() throws AWTException {
//        new TestShortcutClass();
//
//        Robot robot = new Robot();
//        robot.keyPress(KeyEvent.VK_CONTROL);
//        robot.keyPress(KeyEvent.VK_D);
//    }
}
