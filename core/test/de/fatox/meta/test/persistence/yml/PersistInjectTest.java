package de.fatox.meta.test.persistence.yml;

import com.badlogic.gdx.files.FileHandle;
import de.fatox.meta.api.ide.persist.Persist;
import de.fatox.meta.ide.persist.YamlPersistanceManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.test.MetaTest;
import org.junit.Assert;
import org.junit.Test;

public class PersistInjectTest extends MetaTest {

    @Persist(key = "test/")
    static class TestObject {
        @Persist(key = "coolValue", defaultValue = "5")
        private Integer intVal;
    }

    @Inject
    private YamlPersistanceManager persistanceManager;

    @Test
    public void testDefault() {
        Assert.assertNotNull(persistanceManager);

        TestObject object = new TestObject();
        FileHandle fileHandle = persistanceManager.injectObject(object, 0);
        Assert.assertEquals(5, (int) object.intVal);

        Assert.assertNotNull(fileHandle);
        Assert.assertTrue(fileHandle.exists());

        Assert.assertEquals(fileHandle.name(), "TestObject-0.yml");

        fileHandle.delete();
    }

    @Test
    public void testCustom() {
        Assert.assertNotNull(persistanceManager);

        TestObject object = new TestObject();
        object.intVal = 15;
        FileHandle fileHandle = persistanceManager.injectObject(object, 0);
        Assert.assertEquals(15, (int) object.intVal);

        Assert.assertNotNull(fileHandle);
        Assert.assertTrue(fileHandle.exists());

        Assert.assertEquals(fileHandle.name(), "TestObject-0.yml");

        fileHandle.delete();
    }
}
