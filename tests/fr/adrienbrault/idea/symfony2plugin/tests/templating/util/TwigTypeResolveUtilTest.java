package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    private Method createMethod(@NotNull String method) {
        return PhpPsiElementFactory.createFromText(
            getProject(),
            Method.class,
            "<?php interface F { function " + method + "(); }"
        );
    }
}
