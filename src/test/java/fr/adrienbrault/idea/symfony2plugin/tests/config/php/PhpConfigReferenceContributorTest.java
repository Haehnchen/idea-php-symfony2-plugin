package fr.adrienbrault.idea.symfony2plugin.tests.config.php;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.php.PhpConfigReferenceContributor
 */
public class PhpConfigReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("tags.yml");
        myFixture.copyFileToProject("services.yml");
        myFixture.copyFileToProject("../../../dic/container/util/fixtures/services_array.php", "services_array.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/php/fixtures";
    }

    public void testTagReferences() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x \\Symfony\\Component\\DependencyInjection\\Definition */\n" +
                "$x->addTag('<caret>')",
            "foobar"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x \\Symfony\\Component\\DependencyInjection\\Definition */\n" +
                "$x->clearTag('<caret>')",
            "foobar"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "/** @var $x \\Symfony\\Component\\DependencyInjection\\Definition */\n" +
                "$x->hasTag('<caret>')",
            "foobar"
        );
    }

    public void testEventNameCompletionForAsEventListener() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "namespace App\\EventListener;\n" +
                "\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "#[AsEventListener(event: '<caret>')]\n" +
                "final class MyMultiListener implements \\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface\n" +
                "{\n" +
                "\n" +
                "}",
            "yaml_event_2"
        );
    }

    public void testServiceCompletionForPhpArrayConfigServices() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return App::config([\n" +
                "    'services' => [\n" +
                "        'foo' => [\n" +
                "            'arguments' => [service('<caret>')],\n" +
                "        ],\n" +
                "    ],\n" +
                "]);",
            "app.my_service"
        );
    }

    public void testServiceNavigationForPhpArrayConfigServices() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
                "return App::config([\n" +
                "    'services' => [\n" +
                "        'foo' => [\n" +
                "            'arguments' => [service('app.my<caret>_service')],\n" +
                "        ],\n" +
                "    ],\n" +
                "]);");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        StringLiteralExpression stringLiteralExpression = PsiTreeUtil.getParentOfType(psiElement, StringLiteralExpression.class);
        assertNotNull(stringLiteralExpression);

        boolean resolved = false;
        for (PsiReference reference : stringLiteralExpression.getReferences()) {
            if (PlatformPatterns.psiElement().accepts(reference.resolve())) {
                resolved = true;
                break;
            }
        }

        assertTrue(resolved);
    }
}
