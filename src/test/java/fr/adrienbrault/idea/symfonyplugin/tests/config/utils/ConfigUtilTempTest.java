package fr.adrienbrault.idea.symfony2plugin.tests.config.utils;

import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil
 */
public class ConfigUtilTempTest extends SymfonyTempCodeInsightFixtureTestCase {
    /**
     * @see ConfigUtil#getConfigurations
     */
    public void testGetConfigurations() {
        createFile("config/packages/twig.yml");
        createFile("config/packages/twig/config.yaml");
        createFile("app/config/config_dev.yml");

        assertEquals(3, ConfigUtil.getConfigurations(getProject(), "twig").size());
    }
}
