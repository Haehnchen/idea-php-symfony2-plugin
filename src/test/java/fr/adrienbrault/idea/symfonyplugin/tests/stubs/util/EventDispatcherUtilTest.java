package fr.adrienbrault.idea.symfonyplugin.tests.stubs.util;

import fr.adrienbrault.idea.symfonyplugin.stubs.util.EventDispatcherUtil;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventDispatcherUtilTest extends Assert {

    @Test
    public void testTextInstanceExtraction() {
        String[] strings = {
            "The event listener method receives a Symfony\\Component\\Form\\FormEvent instance.",
            "The event listener method receives a \\Symfony\\Component\\Form\\FormEvent instance.",
            "The event listener method receives an Symfony\\Component\\Form\\FormEvent instance.",
            "The event listener method receive an Symfony\\Component\\Form\\FormEvent instance.",
            " method receive f Symfony\\Component\\Form\\FormEvent instance.",
            " method receive   f   Symfony\\Component\\Form\\FormEvent instance.",
            " method receive   hhh   Symfony\\Component\\Form\\FormEvent instance.",
            " method receive   hhh   Symfony\\Component\\Form\\FormEvent",
            "The event listener method\n  * receives a Symfony\\Component\\Form\\FormEvent\n * instance."
        };

        for (String string : strings) {
            assertEquals(
                "Symfony\\Component\\Form\\FormEvent",
                EventDispatcherUtil.extractEventClassInstance(string)
            );
        }
    }
    @Test
    public void testTextInstanceExtractionForUnderline() {
        assertEquals("Symfony\\Compo_nent\\Form\\FormEvent", EventDispatcherUtil.extractEventClassInstance(
            "The event listener method receives a Symfony\\Compo_nent\\Form\\FormEvent instance."
        ));
    }

    @Test
    public void testTextInstanceExtractionForLongStringShouldBeNull() {
        assertNull(EventDispatcherUtil.extractEventClassInstance(
            "The event listener method receives a " + StringUtils.repeat("a", 500) + " instance."
        ));
    }
}
