package de.espend.idea.php.drupal.tests.registrar;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import de.espend.idea.php.drupal.tests.DrupalLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see de.espend.idea.php.drupal.registrar.ControllerCompletion
 */
public class PhpRouteParameterCompletionTest extends DrupalLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("foo.routing.yml");
        myFixture.copyFileToProject("routing.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/de/espend/idea/php/drupal/tests/registrar/fixtures";
    }

    public void testRouteParameterForClassConstructorCompletion() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Drupal\\Core\\Url('foo_bar', ['<caret>'])",
            "foobar", "foobar2"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Drupal\\Core\\Url('foo_bar', ['<caret>' => ''])",
            "foobar", "foobar2"
        );
    }

    public void testRouteParameterForClassConstructorNavigation() {
        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "new \\Drupal\\Core\\Url('foo_bar', ['foo<caret>bar'])",
            PlatformPatterns.psiElement()
        );
    }
}
