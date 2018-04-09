package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("SymfonyCommandUtilTest.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures";
    }

    /**
     * @see SymfonyCommandUtil#getCommands
     */
    public void testGetCommands() {
        for (String s : new String[]{"foo", "property", "const"}) {
            SymfonyCommand command = ContainerUtil.find(SymfonyCommandUtil.getCommands(getProject()), new SymfonyCommandCondition(s));
            assertNotNull(command);
            assertNotNull(command.getName().equals(s));
            assertNotNull(command.getPsiElement());
        }

        assertNull(ContainerUtil.find(SymfonyCommandUtil.getCommands(getProject()), new SymfonyCommandCondition("unknown")));
    }

    private static class SymfonyCommandCondition implements Condition<SymfonyCommand> {
        private final String name;

        public SymfonyCommandCondition(String name) {
            this.name = name;
        }

        @Override
        public boolean value(SymfonyCommand symfonyCommand) {
            return symfonyCommand.getName().equals(name);
        }
    }
}
