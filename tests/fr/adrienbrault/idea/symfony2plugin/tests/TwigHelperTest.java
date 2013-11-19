package fr.adrienbrault.idea.symfony2plugin.tests;

import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import org.junit.Assert;
import org.junit.Test;

public class TwigHelperTest extends Assert {

    @Test
    public void testNormalizeTemplateName() {
        assertEquals("BarBundle:Foo/steps:step_finish.html.twig", TwigHelper.normalizeTemplateName("BarBundle:Foo:steps/step_finish.html.twig"));
        assertEquals("BarBundle:Foo/steps:step_finish.html.twig", TwigHelper.normalizeTemplateName("BarBundle:Foo/steps:step_finish.html.twig"));
        assertEquals("BarBundle:Foo/steps:step_finish.html.twig", TwigHelper.normalizeTemplateName("BarBundle::Foo/steps/step_finish.html.twig"));
        assertEquals("@BarBundle/Foo/steps:step_finish.html.twig", TwigHelper.normalizeTemplateName("@BarBundle/Foo/steps:step_finish.html.twig"));
        assertEquals("step_finish.html.twig", TwigHelper.normalizeTemplateName("step_finish.html.twig"));
    }
}
