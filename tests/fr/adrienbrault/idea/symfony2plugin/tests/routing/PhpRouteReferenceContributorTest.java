package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteReferenceContributor
 */
public class PhpRouteReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {


    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("PhpRouteReferenceContributor.php"));
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testGenerateUrlProvidesNavigation() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
            "/** @var $f \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */\n" +
            "$f->generate('<caret>')"
        , "foo_bar");
    }
}
