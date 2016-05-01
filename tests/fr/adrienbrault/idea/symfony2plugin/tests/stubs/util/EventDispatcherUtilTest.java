package fr.adrienbrault.idea.symfony2plugin.tests.stubs.util;

import fr.adrienbrault.idea.symfony2plugin.stubs.util.EventDispatcherUtil;
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
}
