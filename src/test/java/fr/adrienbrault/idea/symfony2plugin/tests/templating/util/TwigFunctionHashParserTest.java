package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser.FunctionArgument;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

/**
 * @see TwigFunctionHashParser
 */
public class TwigFunctionHashParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testGetArgumentsReadsPositionalAndNamedArguments() {
        List<FunctionArgument> arguments = getArguments("{{ fn('Save', options: { a: 1 }, size = 'sm') }}");

        assertEquals(3, arguments.size());
        assertArgument(arguments.get(0), 0, null, "'");
        assertArgument(arguments.get(1), 1, "options", "{ a: 1 }");
        assertArgument(arguments.get(2), 2, "size", "'");
    }

    public void testGetArgumentsSkipsNamedArgumentWithoutValue() {
        List<FunctionArgument> arguments = getArguments("{{ fn(a:, b) }}");

        assertEquals(1, arguments.size());
        assertArgument(arguments.get(0), 1, null, "b");
    }

    public void testGetArgumentsSkipsTrailingNamedArgumentWithoutValue() {
        assertEmpty(getArguments("{{ fn(name:) }}"));
    }

    private List<FunctionArgument> getArguments(String content) {
        PsiFile file = myFixture.configureByText(TwigFileType.INSTANCE, content);
        PsiElement functionCall = PsiTreeUtil.findFirstParent(file.findElementAt(content.indexOf("fn")), element -> element.getNode().getElementType() == TwigElementTypes.FUNCTION_CALL);
        assertNotNull(functionCall);

        return TwigFunctionHashParser.getArguments(functionCall);
    }

    private static void assertArgument(FunctionArgument argument, int index, String name, String valueText) {
        assertEquals(index, argument.index());
        assertEquals(name, argument.name());
        assertEquals(valueText, argument.value().getText());
    }
}
