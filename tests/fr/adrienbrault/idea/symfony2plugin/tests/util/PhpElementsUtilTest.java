package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpElementsUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getMethodParameterTypeHint
     */
    public void testGetMethodParameterClassHint() {
        assertEquals("\\DateTime", PhpElementsUtil.getMethodParameterTypeHint(
            PhpPsiElementFactory.createMethod(getProject(), "function foo(\\DateTime $e) {}")
        ));

        assertEquals("\\Iterator", PhpElementsUtil.getMethodParameterTypeHint(
            PhpPsiElementFactory.createMethod(getProject(), "function foo(/* foo */ \\Iterator $a, \\DateTime $b")
        ));

        assertNull(PhpElementsUtil.getMethodParameterTypeHint(
            PhpPsiElementFactory.createMethod(getProject(), "function foo(/* foo */ $a, \\DateTime $b")
        ));
    }

}
