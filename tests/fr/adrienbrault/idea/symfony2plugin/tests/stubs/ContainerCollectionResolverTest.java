package fr.adrienbrault.idea.symfony2plugin.tests.stubs;

import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
 */
public class ContainerCollectionResolverTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText(YAMLFileType.YML, "" +
            "parameters:\n" +
            "    bar: foo\n" +
            "\n" +
            "services:\n" +
            "    foo:\n" +
            "        class: DateTime\n"
        );
    }

    public void testCaseInsensitiveService() {
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "foo").getClassName());
        assertEquals("DateTime", ContainerCollectionResolver.getService(getProject(), "Foo").getClassName());
        assertTrue(ContainerCollectionResolver.hasServiceNames(getProject(), "foo"));
        assertTrue(ContainerCollectionResolver.hasServiceNames(getProject(), "Foo"));
    }

    public void testCaseInsensitiveParameter() {
        assertTrue(ContainerCollectionResolver.getParameterNames(getProject()).contains("Bar"));
        assertTrue(ContainerCollectionResolver.getParameterNames(getProject()).contains("bar"));
    }
}
