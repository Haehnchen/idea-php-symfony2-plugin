package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.jetbrains.php.PhpIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpTypeProviderUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    /**
     * @see PhpTypeProviderUtil#getResolvedParameter
     */
    public void testGetResolvedParameter() {
        assertEquals("\\Class\\Foo", PhpTypeProviderUtil.getResolvedParameter(PhpIndex.getInstance(getProject()), "#K#C\\Class\\Foo."));
        assertEquals("\\Class\\Foo", PhpTypeProviderUtil.getResolvedParameter(PhpIndex.getInstance(getProject()), "#K#C\\Class\\Foo.class"));
    }
}
