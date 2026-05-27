package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.util.Pair;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
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
            "#}\n" +
            "{# @var foo_7 \\AppBundle\\Entity\\MeterValueDTO|\\AppBundle\\Entity\\OtherDTO #}\n" +
            "{# @var \\AppBundle\\Entity\\MeterValueDTO|\\AppBundle\\Entity\\OtherDTO foo_8 #}\n" +
            "{# @var foo_9 string|null #}\n" +
            "{# @var foo_10 \\AppBundle\\Entity\\MeterValueDTO[]|\\AppBundle\\Entity\\OtherDTO[] #}\n"
        );

        Map<String, String> fileVariableDocBlock = TwigTypeResolveUtil.findFileVariableDocBlock((TwigFile) fileFromText);

        assertEquals("\\AppBundle\\Entity\\MeterValueDTO", fileVariableDocBlock.get("foo_1"));
        assertEquals("\\AppBundle\\Entity\\MeterValueDTO[]", fileVariableDocBlock.get("foo_2"));
        assertEquals("\\AppBundle\\Entity\\MeterValueDTO", fileVariableDocBlock.get("foo_3"));
        assertEquals("\\AppBundle\\Entity\\MeterValueDTO[]", fileVariableDocBlock.get("foo_4"));

        assertEquals("\\AppBundle\\Entity\\MeterValueDTO", fileVariableDocBlock.get("foo_5"));
        assertEquals("\\AppBundle\\Entity\\MeterValueDTO", fileVariableDocBlock.get("foo_6"));
        assertEquals("\\AppBundle\\Entity\\MeterValueDTO|\\AppBundle\\Entity\\OtherDTO", fileVariableDocBlock.get("foo_7"));
        assertEquals("\\AppBundle\\Entity\\MeterValueDTO|\\AppBundle\\Entity\\OtherDTO", fileVariableDocBlock.get("foo_8"));
        assertEquals("string|null", fileVariableDocBlock.get("foo_9"));
        assertEquals("\\AppBundle\\Entity\\MeterValueDTO[]|\\AppBundle\\Entity\\OtherDTO[]", fileVariableDocBlock.get("foo_10"));
    }

    /**
     * @see TwigTypeResolveUtil#findFileVariableDocBlock
     */
    public void testFindFileTypeTag() {
        PsiFile fileFromText = PsiFileFactory.getInstance(getProject()).createFileFromText(TwigLanguage.INSTANCE, "" +
                "{% types {\n" +
                "    is_correct: 'bool',\n" +
                "    score: 'int',\n" +
                "    foobar_1: 'array<int, App\\\\User>',\n" +
                "    foobar_2?: '\\\\App\\\\User'," +
                "    foobar_3: '\\\\App\\\\User[]'," +
                "    foobar_4: '\\\\App\\\\User[]|\\User'," +
                "    foobar_8: '\\\\App\\\\User[]|\\\\App\\\\Admin[]'," +
                "    foobar_5: '',foobar_6: ''\r\n,\n\tfoobar_7:''\n\t\r," +
                "} %}" +
                "\n"
        );

        Map<String, String> fileVariableDocBlock = TwigTypeResolveUtil.findFileVariableDocBlock((TwigFile) fileFromText);
        assertEquals("bool", fileVariableDocBlock.get("is_correct"));
        assertEquals("int", fileVariableDocBlock.get("score"));
        assertNull(fileVariableDocBlock.get("foobar_5"));

        assertEquals("\\App\\User", fileVariableDocBlock.get("foobar_2"));
        assertEquals("\\App\\User[]", fileVariableDocBlock.get("foobar_3"));

        // maybe resolve this
        assertEquals("\\mixed", fileVariableDocBlock.get("foobar_1"));
        assertEquals("\\App\\User[]|\\User", fileVariableDocBlock.get("foobar_4"));
        assertEquals("\\App\\User[]|\\App\\Admin[]", fileVariableDocBlock.get("foobar_8"));

        assertNull(fileVariableDocBlock.get("foobar_6"));
        assertNull(fileVariableDocBlock.get("foobar_7"));
    }

    public void testReqExForInlineDocVariables() {
        assertMatches("@var foo_1 \\AppBundle\\Entity\\MeterValueDTO", TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE);
        assertMatches("@var \\AppBundle\\Entity\\MeterValueDTO foo_1", TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE);
        assertMatches("foo_1 \\AppBundle\\Entity\\MeterValueDTO", TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE);
        assertMatches("@var foo_1 \\AppBundle\\Entity\\MeterValueDTO|\\AppBundle\\Entity\\OtherDTO", TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE);
        assertMatches("@var \\AppBundle\\Entity\\MeterValueDTO|\\AppBundle\\Entity\\OtherDTO foo_1", TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE);
        assertMatches("foo_1 \\AppBundle\\Entity\\MeterValueDTO|\\AppBundle\\Entity\\OtherDTO", TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE);
        assertMatches("@var foo_1 \\AppBundle\\Entity\\MeterValueDTO[]|\\AppBundle\\Entity\\OtherDTO[]", TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE);
        assertNotMatches("@var foo_1 \\AppBundle\\Entity\\MeterValueDTO|", TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE);
    }

    public void testSplitTwigDocTypes() {
        Set<String> types = TwigTypeResolveUtil.splitTwigDocTypes("\\App\\Entity\\User[]|\\App\\Entity\\Admin|null");

        assertContainsElements(types, "\\App\\Entity\\User[]", "\\App\\Entity\\Admin", "null");
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

    public void testCollectScopeVariablesSplitsUnionTypes() {
        myFixture.configureByText(TwigFileType.INSTANCE,
            "{# @var user \\App\\Entity\\User|\\App\\Entity\\Admin|null #}\n" +
                "{{ <caret> }}"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        Map<String, PsiVariable> vars = TwigTypeResolveUtil.collectScopeVariables(psiElement);

        assertNotNull(vars.get("user"));
        assertContainsElements(vars.get("user").getTypes(), "\\App\\Entity\\User", "\\App\\Entity\\Admin", "null");
    }

    public void testCollectScopeVariablesSplitsArrayUnionTypes() {
        myFixture.configureByText(TwigFileType.INSTANCE,
            "{# @var users \\App\\Entity\\User[]|\\App\\Entity\\Admin[] #}\n" +
                "{{ <caret> }}"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        Map<String, PsiVariable> vars = TwigTypeResolveUtil.collectScopeVariables(psiElement);

        assertNotNull(vars.get("users"));
        assertContainsElements(vars.get("users").getTypes(), "\\App\\Entity\\User[]", "\\App\\Entity\\Admin[]");
    }

    public void testCollectPsiTypeNameElementsWithCurrent() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{{ root.children.ent<caret>ries }}");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        List<PsiElement> pathElements = TwigTypeResolveUtil.collectPsiTypeNameElementsWithCurrent(psiElement);

        assertEquals(Arrays.asList("root", "children", "entries"), pathElements.stream().map(PsiElement::getText).collect(Collectors.toList()));
        assertSame(psiElement, pathElements.get(2));
    }

    public void testCollectPsiTypeNameElementsWithCurrentSupportsMethodCallAsCurrentElement() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{{ root.children.ff<caret>f() }}");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        List<PsiElement> pathElements = TwigTypeResolveUtil.collectPsiTypeNameElementsWithCurrent(psiElement);

        assertEquals(Arrays.asList("root", "children", "fff"), pathElements.stream().map(PsiElement::getText).collect(Collectors.toList()));
        assertSame(psiElement, pathElements.get(2));
        assertFalse(TwigTypeResolveUtil.hasNextPsiTypeNameElement(psiElement));
    }

    public void testCollectPsiTypeNameElementsWithCurrentSupportsMethodCallBeforeCurrentElement() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{{ root.children.fff().b<caret>ar }}");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        List<PsiElement> pathElements = TwigTypeResolveUtil.collectPsiTypeNameElementsWithCurrent(psiElement);

        assertEquals(Arrays.asList("root", "children", "fff", "bar"), pathElements.stream().map(PsiElement::getText).collect(Collectors.toList()));
        assertSame(psiElement, pathElements.get(3));
    }

    public void testCollectPsiTypeNameElementsWithCurrentSupportsMultipleMethodCallsBeforeCurrentElement() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{{ root.getChildren().fff('value').b<caret>ar }}");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        List<PsiElement> pathElements = TwigTypeResolveUtil.collectPsiTypeNameElementsWithCurrent(psiElement);

        assertEquals(Arrays.asList("root", "getChildren", "fff", "bar"), pathElements.stream().map(PsiElement::getText).collect(Collectors.toList()));
        assertSame(psiElement, pathElements.get(3));
    }

    public void testHasNextPsiTypeNameElementSupportsMethodCallBeforeMethodCallElement() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{{ root.getCh<caret>ildren().fff('value').bar }}");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        assertTrue(TwigTypeResolveUtil.hasNextPsiTypeNameElement(psiElement));
    }

    public void testHasNextPsiTypeNameElementSupportsMethodCallBeforeNextElement() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{{ root.children.ff<caret>f('value').bar }}");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        assertTrue(TwigTypeResolveUtil.hasNextPsiTypeNameElement(psiElement));
    }

    public void testGetForTagScopeExtractsSingleVariablePathFromAst() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{% for entry in entries %}{{ entry }}{% endfor %}");

        Pair<String, java.util.List<String>> forTagScope = TwigTypeResolveUtil.getForTagScope(findForTag());

        assertNotNull(forTagScope);
        assertEquals("entry", forTagScope.getFirst());
        assertEquals(Arrays.asList("entries"), forTagScope.getSecond());
    }

    public void testGetForTagScopeExtractsNestedVariablePathFromAst() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{% for entry in root.children.entries %}{{ entry }}{% endfor %}");

        Pair<String, java.util.List<String>> forTagScope = TwigTypeResolveUtil.getForTagScope(findForTag());

        assertNotNull(forTagScope);
        assertEquals("entry", forTagScope.getFirst());
        assertEquals(Arrays.asList("root", "children", "entries"), forTagScope.getSecond());
    }

    public void testGetForTagScopeStopsNestedVariablePathBeforeFilter() {
        myFixture.configureByText(TwigFileType.INSTANCE, "{% for entry in root.children.entries | filter %}{{ entry }}{% endfor %}");

        Pair<String, java.util.List<String>> forTagScope = TwigTypeResolveUtil.getForTagScope(findForTag());

        assertNotNull(forTagScope);
        assertEquals("entry", forTagScope.getFirst());
        assertEquals(Arrays.asList("root", "children", "entries"), forTagScope.getSecond());
    }

    @NotNull
    private PsiElement findForTag() {
        for (TwigCompositeElement twigCompositeElement : PsiTreeUtil.findChildrenOfType(myFixture.getFile(), TwigCompositeElement.class)) {
            if (twigCompositeElement.getNode().getElementType() == TwigElementTypes.FOR_TAG) {
                return twigCompositeElement;
            }
        }

        fail("FOR_TAG not found");
        throw new AssertionError("unreachable");
    }

    private void assertMatches(@NotNull String content, @NotNull String... regularExpressions) {
        for (String regularExpression : regularExpressions) {
            if(content.matches(regularExpression)) {
                return;
            }
        }

        fail("invalid regular expression: " + content);
    }

    private void assertNotMatches(@NotNull String content, @NotNull String... regularExpressions) {
        for (String regularExpression : regularExpressions) {
            if(content.matches(regularExpression)) {
                fail("invalid regular expression match: " + content);
            }
        }
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
