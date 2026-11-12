package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PsiElementUtilsTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testGetParameterOrNamedArgumentPrefersIndexedParameter() {
        NewExpression newExpression = createNewExpression("<?php new Foo('first', ['positional'], aliases: ['named']);");

        PsiElement aliases = PsiElementUtils.getParameterOrNamedArgument(newExpression.getParameters(), "aliases", 1);

        assertTrue(aliases instanceof ArrayCreationExpression);
        assertContainsElements(PhpElementsUtil.getArrayValuesAsString((ArrayCreationExpression) aliases), "positional");
    }

    public void testGetParameterOrNamedArgumentFallsBackToNamedArgument() {
        NewExpression newExpression = createNewExpression("<?php new Foo('first', aliases: ['named']);");

        PsiElement aliases = PsiElementUtils.getParameterOrNamedArgument(newExpression.getParameters(), "aliases", 6);

        assertTrue(aliases instanceof ArrayCreationExpression);
        assertContainsElements(PhpElementsUtil.getArrayValuesAsString((ArrayCreationExpression) aliases), "named");
    }

    public void testNamedArgumentCanBeReordered() {
        NewExpression expression = createNewExpression("<?php new Foo(aliases: ['named'], other: 'last');");

        PsiElement aliases = PsiElementUtils.getParameterOrNamedArgument(expression.getParameters(), "aliases", 1);

        assertSame(expression.getParameters()[0], aliases);
    }

    public void testIsCurrentParameterMatchesReorderedNamedArgument() {
        NewExpression expression = createNewExpression("<?php new Foo(options: [], type: 'integer');");

        assertTrue(PsiElementUtils.isCurrentParameter(expression.getParameters()[1], "type", 0));
    }

    public void testIsCurrentParameterMatchesPositionalArgument() {
        NewExpression expression = createNewExpression("<?php new Foo('field', 'integer');");

        assertTrue(PsiElementUtils.isCurrentParameter(expression.getParameters()[1], "type", 1));
    }

    private NewExpression createNewExpression(String code) {
        myFixture.configureByText(PhpFileType.INSTANCE, code);

        return Objects.requireNonNull(PsiTreeUtil.findChildOfType(myFixture.getFile(), NewExpression.class));
    }
}
