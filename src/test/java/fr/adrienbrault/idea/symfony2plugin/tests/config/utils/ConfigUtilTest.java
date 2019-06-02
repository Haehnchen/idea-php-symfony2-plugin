package fr.adrienbrault.idea.symfony2plugin.tests.config.utils;

import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ConfigUtil#getTreeSignatures
 */
public class ConfigUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ConfigUtilTest.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/utils/fixtures";
    }

    /**
     * @see ConfigUtil#getTreeSignatures
     */
    public void testGetTreeSignatures() {
        Map<String, Collection<String>> signatures = ConfigUtil.getTreeSignatures(getProject());

        assertContainsElements(signatures.get("foobar_root"), "\\Foo\\Bar\\MyConfiguration");
        assertContainsElements(signatures.get("foobar_root"), "\\Foo\\Bar\\MyNextConfiguration");
    }

    /**
     * @see ConfigUtil#getTreeSignatureTargets
     */
    public void testGetTreeSignatureTargetsWithClassFilter() {
        assertNotNull(ContainerUtil.find(ConfigUtil.getTreeSignatureTargets(getProject(), "foobar_root", Collections.singletonList("\\Foo\\Bar\\MyConfiguration")), psiElement ->
            psiElement instanceof StringLiteralExpression && ((StringLiteralExpression) psiElement).getContents().equals("foobar_root")
        ));
    }

    /**
     * @see ConfigUtil#getTreeSignatureTargets
     */
    public void testGetTreeSignatureTargetsWithKeyOnly() {
        assertNotNull(ContainerUtil.find(ConfigUtil.getTreeSignatureTargets(getProject(), "foobar_root"), psiElement ->
            psiElement instanceof StringLiteralExpression && ((StringLiteralExpression) psiElement).getContents().equals("foobar_root")
        ));
    }
}
