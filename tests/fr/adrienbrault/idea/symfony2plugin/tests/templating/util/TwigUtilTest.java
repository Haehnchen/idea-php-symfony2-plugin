package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.junit.Assert;
import org.junit.Test;


public class TwigUtilTest extends Assert {

    @Test
    public void testGetFoldingTemplateName() {

        assertEquals("Foo:edit", TwigUtil.getFoldingTemplateName("FooBundle:edit.html.twig"));
        assertEquals("Foo:edit", TwigUtil.getFoldingTemplateName("FooBundle:edit.html.php"));
        assertEquals("Bundle:", TwigUtil.getFoldingTemplateName("Bundle:.html.twig"));
        assertEquals("edit", TwigUtil.getFoldingTemplateName("edit.html.twig"));
        assertNull(TwigUtil.getFoldingTemplateName("FooBundle:edit.foo.twig"));

    }

}
