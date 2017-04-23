package fr.adrienbrault.idea.symfony2plugin.tests.dic.container.util;

import fr.adrienbrault.idea.symfony2plugin.dic.container.util.DotEnvUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DotEnvUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("env.env");
        myFixture.copyFileToProject("docker-compose.yml");
        myFixture.copyFileToProject("Dockerfile");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testGetEnvironmentVariables() {
        assertContainsElements(DotEnvUtil.getEnvironmentVariables(getProject()), "foobar", "DEBUG_WEB", "DEBUG_SERVICES", "DOCKERFILE_FOO");
    }

    public void testGetEnvironmentVariableTargets() {
        assertTrue(DotEnvUtil.getEnvironmentVariableTargets(getProject(), "foobar").size() > 0);
    }
}
