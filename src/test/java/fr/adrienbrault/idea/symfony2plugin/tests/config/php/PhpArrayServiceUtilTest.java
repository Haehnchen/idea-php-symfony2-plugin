package fr.adrienbrault.idea.symfony2plugin.tests.config.php;

import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.config.php.PhpArrayServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * @see PhpArrayServiceUtil#isServiceKey
 */
public class PhpArrayServiceUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testServiceKeyInsideAppConfig() {
        assertServiceKey(true,
            "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "return App::config([\n" +
            "    'services' => [\n" +
            "        'app.mail<caret>er' => null,\n" +
            "    ],\n" +
            "]);"
        );

        assertServiceKey(true,
    "namespace Symfony\\Component\\DependencyInjection\\Loader\\Configurator;\n" +
            "App::config([\n" +
            "    'services' => [\n" +
            "        'app.mail<caret>er' => null,\n" +
            "    ],\n" +
            "]);"
        );
    }

    public void testServiceKeyInsideReturnArray() {
        assertServiceKey(true,
            "return [\n" +
            "    'services' => [\n" +
            "        'app.mail<caret>er' => null,\n" +
            "    ],\n" +
            "];"
        );

        assertServiceKey(false,
            "$foo = [\n" +
            "    'services' => [\n" +
            "        'app.mail<caret>er' => null,\n" +
            "    ],\n" +
            "];"
        );
    }

    public void testServiceKeyInsideNonServicesArray() {
        assertServiceKey(false,
            "return [\n" +
            "    'parameters' => [\n" +
            "        'app.mail<caret>er' => null,\n" +
            "    ],\n" +
            "];"
        );
    }

    private void assertServiceKey(boolean expected, @NotNull String code) {
        PsiFile file = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" + code);

        int offset = myFixture.getCaretOffset();
        var element = file.findElementAt(offset);
        assertNotNull("No element at caret", element);

        StringLiteralExpression stringLiteral = PsiTreeUtil.getParentOfType(element, StringLiteralExpression.class);
        assertNotNull("No StringLiteralExpression at caret", stringLiteral);

        // The key is the StringLiteralExpression itself
        assertEquals(expected, PhpArrayServiceUtil.isServiceKey(stringLiteral));
    }
}
