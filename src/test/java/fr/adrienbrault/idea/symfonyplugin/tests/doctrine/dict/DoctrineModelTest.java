package fr.adrienbrault.idea.symfonyplugin.tests.doctrine.dict;

import fr.adrienbrault.idea.symfonyplugin.doctrine.dict.DoctrineModel;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineModelTest extends Assert {
    @Test
    public void testObjectEqualsCheck() {
        assertTrue(new DoctrineModel("foobar").equals(new DoctrineModel("foobar")));
        assertEquals(new DoctrineModel("foobar").hashCode(), new DoctrineModel("foobar").hashCode());

        assertFalse(new DoctrineModel("foobar").equals(new DoctrineModel("foobar", "foo")));
        assertNotEquals(new DoctrineModel("foobar").hashCode(), new DoctrineModel("foobar", "foo").hashCode());

        assertTrue(new DoctrineModel("foobar", "apple").equals(new DoctrineModel("foobar", "apple")));
        assertEquals(new DoctrineModel("foobar", "apple").hashCode(), new DoctrineModel("foobar", "apple").hashCode());

        assertFalse(new DoctrineModel("foobar", null).equals(new DoctrineModel("foobar", "")));
        assertNotEquals(new DoctrineModel("foobar", null).hashCode(), new DoctrineModel("foobar", "").hashCode());
        assertNotEquals(new DoctrineModel("foobar", "").hashCode(), new DoctrineModel("foobar", null).hashCode());
        assertNotEquals(new DoctrineModel("foobar", "").hashCode(), new DoctrineModel("foobar").hashCode());
    }
}
