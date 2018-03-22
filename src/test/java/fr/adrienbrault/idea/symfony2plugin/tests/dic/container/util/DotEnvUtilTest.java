package fr.adrienbrault.idea.symfony2plugin.tests.dic.container.util;

import com.intellij.psi.PsiFile;
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
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/container/util/fixtures";
    }

    public void testGetEnvironmentVariables() {
        assertContainsElements(DotEnvUtil.getEnvironmentVariables(getProject()), "foobar", "DEBUG_WEB", "DEBUG_SERVICES", "DOCKERFILE_FOO");
    }

    public void testGetEnvironmentVariableTargets() {
        assertEquals(1, DotEnvUtil.getEnvironmentVariableTargets(getProject(), "foobar")
            .stream()
            .filter(psiElement -> psiElement instanceof PsiFile && "env.env".equals(((PsiFile) psiElement).getName()))
            .count()
        );
    }

    public void testGetEnvironmentVariableTargetsForParameter() {
        assertEquals(1, DotEnvUtil.getEnvironmentVariableTargetsForParameter(getProject(), "%env(int:foobar)%")
            .stream()
            .filter(psiElement -> psiElement instanceof PsiFile && "env.env".equals(((PsiFile) psiElement).getName()))
            .count()
        );

        assertEquals(1, DotEnvUtil.getEnvironmentVariableTargetsForParameter(getProject(), "%env(foobar)%")
            .stream()
            .filter(psiElement -> psiElement instanceof PsiFile && "env.env".equals(((PsiFile) psiElement).getName()))
            .count()
        );
    }
}
