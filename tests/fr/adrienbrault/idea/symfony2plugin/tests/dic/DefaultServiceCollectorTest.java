package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.DefaultServiceCollector;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.DefaultServiceCollector
 */
public class DefaultServiceCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {

    /**
     * @see DefaultServiceCollector#collectServices
     * @see DefaultServiceCollector#collectIds
     */
    public void testDefaultServiceAreInsideContainer() {
        assertTrue(ContainerCollectionResolver.hasServiceNames(getProject(), "service_container"));
        ContainerService servicContainer = ContainerCollectionResolver.getService(getProject(), "service_container");

        assertNotNull(servicContainer);
        assertEquals("Symfony\\Component\\DependencyInjection\\ContainerInterface", servicContainer.getClassName());
    }

    /**
     * @see DefaultServiceCollector#collectServices
     * @see DefaultServiceCollector#collectIds
     */
    public void testThatDeprecatesRequestIsOnlyAvailableInSupportedVersion() {
        assertFalse(ContainerCollectionResolver.hasServiceNames(getProject(), "request"));

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '2.5.3';\n" +
            "   }\n" +
            "}"
        );

        ContainerService serviceContainer = ContainerCollectionResolver.getService(getProject(), "request");

        assertNotNull(serviceContainer);
        assertEquals("Symfony\\Component\\HttpFoundation\\Request", serviceContainer.getClassName());

        assertTrue(ContainerCollectionResolver.hasServiceNames(getProject(), "request"));
    }
}
