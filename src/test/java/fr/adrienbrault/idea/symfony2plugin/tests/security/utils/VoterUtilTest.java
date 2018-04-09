package fr.adrienbrault.idea.symfony2plugin.tests.security.utils;

import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.ClassConstantReference;
import fr.adrienbrault.idea.symfony2plugin.security.utils.VoterUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.security.utils.VoterUtil
 */
public class VoterUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("security.yml");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/security/utils/fixtures";
    }

    /**
     * @see VoterUtil#visitAttribute
     */
    public void testVoterAttributeForPhpFile() {
        VoterUtil.StringPairConsumer consumer = new VoterUtil.StringPairConsumer();
        VoterUtil.visitAttribute(getProject(), consumer);

        assertContainsElements(consumer.getValues(), "FOOBAR_IF_1", "FOOBAR_IF_2", "FOOBAR_IF_3", "FOOBAR_IF_4");

        assertContainsElements(consumer.getValues(), "FOOBAR_ARRAY_1", "FOOBAR_ARRAY_2");
        assertContainsElements(consumer.getValues(), "FOOBAR_ARRAY_3", "FOOBAR_ARRAY_4");

        assertContainsElements(consumer.getValues(), "FOOBAR_CASE_1", "FOOBAR_CASE_2");

        assertContainsElements(consumer.getValues(), "FOOBAR_ATTRIBUTES_IN_CONST_1", "FOOBAR_ATTRIBUTES_IN_PROPERTY_1");
    }

    /**
     * @see VoterUtil#visitAttribute
     */
    public void testVoterAttributeForPhpFileInForeach() {
        VoterUtil.StringPairConsumer consumer = new VoterUtil.StringPairConsumer();
        VoterUtil.visitAttribute(getProject(), consumer);

        assertContainsElements(consumer.getValues(), "FOOBAR_EACH_1", "FOOBAR_ATTRIBUTES_IN_ARRAY");
    }

    public void testVoterAttributeForPhpFileWithTarget() {
        VoterUtil.TargetPairConsumer consumer = new VoterUtil.TargetPairConsumer("FOOBAR_IF_1");
        VoterUtil.visitAttribute(getProject(), consumer);

        assertNotNull(ContainerUtil.filter(consumer.getValues(), psiElement ->
            psiElement instanceof ClassConstantReference)
        );
    }

    /**
     * @see VoterUtil#visitAttribute
     */
    public void testVoterAttributeForYamlSecurityFile() {
        VoterUtil.StringPairConsumer consumer = new VoterUtil.StringPairConsumer();
        VoterUtil.visitAttribute(getProject(), consumer);

        Set<String> values = consumer.getValues();
        assertContainsElements(values, "YAML_ROLE_ADMIN", "YAML_ROLE_ALLOWED_TO_SWITCH", "YAML_ROLE_SUPER_ADMIN");

        assertContainsElements(values, "YAML_ROLE_USER_FOOBAR", "YAML_ROLE_USER_FOOBAR_1");
        assertContainsElements(values, "YAML_ROLE_FOOBAR_ARRAY_1", "YAML_ROLE_FOOBAR_ARRAY_2");
    }
}
