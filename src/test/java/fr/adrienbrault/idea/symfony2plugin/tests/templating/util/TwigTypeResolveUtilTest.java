package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigLanguage;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTypeResolveUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatTwigGetAttributeSupportShortcuts() {
        assertEquals("myFoobar", TwigTypeResolveUtil.getPropertyShortcutMethodName(createMethod("myFoobar")));
        assertEquals("foo", TwigTypeResolveUtil.getPropertyShortcutMethodName(createMethod("getFoo")));
        assertEquals("foo", TwigTypeResolveUtil.getPropertyShortcutMethodName(createMethod("hasFoo")));
        assertEquals("foo", TwigTypeResolveUtil.getPropertyShortcutMethodName(createMethod("isFoo")));
    }

    /**
     * @see TwigTypeResolveUtil#findFileVariableDocBlock
     */
    public void testFindFileVariableDocBlock() {
        PsiFile fileFromText = PsiFileFactory.getInstance(getProject()).createFileFromText(TwigLanguage.INSTANCE, "" +
            "{# @var foo_1 \\AppBundle\\Entity\\MeterValueDTO #}\n" +
            "{# @var foo_2 \\AppBundle\\Entity\\MeterValueDTO[] #}\n" +
            "{# @var \\AppBundle\\Entity\\MeterValueDTO foo_3 #}\n" +
            "{# @var \\AppBundle\\Entity\\MeterValueDTO[] foo_4 #}\n" +
            "" +
            "{#\n" +
            "@var \\AppBundle\\Entity\\MeterValueDTO foo_5\n" +
            "@var foo_6 \\AppBundle\\Entity\\MeterValueDTO\n" +
            "#}\n"
        );

        Map<String, String> fileVariableDocBlock = TwigTypeResolveUtil.findFileVariableDocBlock((TwigFile) fileFromText);

        assertEquals("\\AppBundle\\Entity\\MeterValueDTO", fileVariableDocBlock.get("foo_1"));
        assertEquals("\\AppBundle\\Entity\\MeterValueDTO[]", fileVariableDocBlock.get("foo_2"));
        assertEquals("\\AppBundle\\Entity\\MeterValueDTO", fileVariableDocBlock.get("foo_3"));
        assertEquals("\\AppBundle\\Entity\\MeterValueDTO[]", fileVariableDocBlock.get("foo_4"));

        assertEquals("\\AppBundle\\Entity\\MeterValueDTO", fileVariableDocBlock.get("foo_5"));
        assertEquals("\\AppBundle\\Entity\\MeterValueDTO", fileVariableDocBlock.get("foo_6"));
    }

    public void testReqExForInlineDocVariables() {
        assertMatches("@var foo_1 \\AppBundle\\Entity\\MeterValueDTO", TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE);
        assertMatches("@var \\AppBundle\\Entity\\MeterValueDTO foo_1", TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE);
        assertMatches("foo_1 \\AppBundle\\Entity\\MeterValueDTO", TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE);
    }

    /**
     * @see TwigTypeResolveUtil#collectScopeVariables
     */
    public void testCollectScopeVariables() {
        myFixture.configureByText(TwigFileType.INSTANCE,
            "{# @var b \\Foo\\Bar #}" +
                "{% block one %}\n" +
                "    {# @var a \\Foo\\Bar #}\n" +
                "\n" +
                "    {% block two %}\n" +
                "        {% block two %}\n" +
                "            {{ <caret> }}\n" +
                "        {% endblock %}\n" +
                "    {% endblock %}\n" +
                "{% endblock %}"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        Map<String, PsiVariable> stringPsiVariableMap = TwigTypeResolveUtil.collectScopeVariables(psiElement);

        assertContainsElements(stringPsiVariableMap.get("a").getTypes(), "\\Foo\\Bar");
        assertContainsElements(stringPsiVariableMap.get("b").getTypes(), "\\Foo\\Bar");
    }

    /**
     * @see TwigTypeResolveUtil#collectScopeVariables
     */
    public void testForeachMustProvideLoopVariable() {
        myFixture.configureByText(TwigFileType.INSTANCE,
            "{% for foo in foo %}" +
                "{{ <caret> }}" +
                "{% endfor %}"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        Map<String, PsiVariable> stringPsiVariableMap = TwigTypeResolveUtil.collectScopeVariables(psiElement);
        assertContainsElements(stringPsiVariableMap.keySet(), "loop");
    }

    /**
     * @see TwigTypeResolveUtil#resolveTwigMethodName
     */
    public void testForeachMustProvideLoopTypeVariable() {
        myFixture.configureByText(TwigFileType.INSTANCE,
            "{% for foo in foo %}" +
                "{{ loop.i<caret> }}" +
                "{% endfor %}"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Collection<String> beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(psiElement);

        Collection<TwigTypeContainer> stringPsiVariableMap = TwigTypeResolveUtil.resolveTwigMethodName(psiElement.getPrevSibling(), beforeLeaf);

        Set<String> types = stringPsiVariableMap.stream().map(TwigTypeContainer::getStringElement).collect(Collectors.toSet());
        assertContainsElements(types, "last", "index", "index0");
    }

    private void assertMatches(@NotNull String content, @NotNull String... regularExpressions) {
        for (String regularExpression : regularExpressions) {
            if(content.matches(regularExpression)) {
                return;
            }
        }

        fail("invalid regular expression: " + content);
    }

    @NotNull
    private Method createMethod(@NotNull String method) {
        return PhpPsiElementFactory.createFromText(
            getProject(),
            Method.class,
            "<?php interface F { function " + method + "(); }"
        );
    }
}
