package fr.adrienbrault.idea.symfony2plugin.tests.config.utils;

import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil
 */
public class ConfigUtilTempTest extends SymfonyLightCodeInsightFixtureTestCase {
    /**
     * @see ConfigUtil#getConfigurations
     */
    public void testGetConfigurations() {
        createFileInProjectRoot("config/packages/twig.yml", "");
        createFileInProjectRoot("config/packages/twig/config.yaml", "");
        createFileInProjectRoot("app/config/config_dev.yml", "");

        assertEquals(3, ConfigUtil.getConfigurations(getProject(), "twig").size());
    }
}
