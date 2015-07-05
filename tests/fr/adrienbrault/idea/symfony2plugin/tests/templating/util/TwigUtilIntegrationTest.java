package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.openapi.vfs.VfsUtil;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

public class TwigUtilIntegrationTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        createDummyFiles(
            "app/Resources/TwigUtilIntegrationBundle/views/layout.html.twig",
            "app/Resources/TwigUtilIntegrationBundle/views/Foo/layout.html.twig",
            "app/Resources/TwigUtilIntegrationBundle/views/Foo/Bar/layout.html.twig"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTemplateNameByOverwrite
     */
    public void testTemplateOverwriteNameGeneration() {

        if(true == true) {
            return;
        }

        assertEquals(
            "TwigUtilIntegrationBundle:layout.html.twig",
            TwigUtil.getTemplateNameByOverwrite(getProject(), VfsUtil.findRelativeFile(getProject().getBaseDir(), "app/Resources/TwigUtilIntegrationBundle/views/layout.html.twig".split("/")))
        );

        assertEquals(
            "TwigUtilIntegrationBundle:Foo/layout.html.twig",
            TwigUtil.getTemplateNameByOverwrite(getProject(), VfsUtil.findRelativeFile(getProject().getBaseDir(), "app/Resources/TwigUtilIntegrationBundle/views/Foo/layout.html.twig".split("/")))
        );

        assertEquals(
            "TwigUtilIntegrationBundle:Foo/Bar/layout.html.twig",
            TwigUtil.getTemplateNameByOverwrite(getProject(), VfsUtil.findRelativeFile(getProject().getBaseDir(), "app/Resources/TwigUtilIntegrationBundle/views/Foo/Bar/layout.html.twig".split("/")))
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTemplateNameByOverwrite
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTemplateName
     */
    public void testTemplateOverwriteNavigation() {

        if(true == true) {
            return;
        }

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends '<caret>TwigUtilIntegrationBundle:layout.html.twig' %}", "/views/layout.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends '<caret>TwigUtilIntegrationBundle:Foo/layout.html.twig' %}", "/views/Foo/layout.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends '<caret>TwigUtilIntegrationBundle:Foo/Bar/layout.html.twig' %}", "/views/Foo/Bar/layout.html.twig");
    }

}
