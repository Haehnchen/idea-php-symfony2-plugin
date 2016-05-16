package fr.adrienbrault.idea.symfony2plugin.tests.dic;

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
}
