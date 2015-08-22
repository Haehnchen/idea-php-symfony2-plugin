package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.PhpTemplateGlobalStringGoToDeclarationHandler
 */
public class PhpTemplateGlobalStringGoToDeclarationHandlerTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        createDummyFiles(
            "app/Resources/views/layout.html.twig"
        );
    }

    public void testTemplateGoTo() {
        assertNavigationContainsFile(PhpFileType.INSTANCE, "<?php '::layout.<caret>html.twig'", "layout.html.twig");
        assertNavigationContainsFile(PhpFileType.INSTANCE, "<?php \"::layout.<caret>html.twig\"", "layout.html.twig");
        assertNavigationContainsFile(PhpFileType.INSTANCE, "<?php 'layout.<caret>html.twig'", "layout.html.twig");
    }
}
