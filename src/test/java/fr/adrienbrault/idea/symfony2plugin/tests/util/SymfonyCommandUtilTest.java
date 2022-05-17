package fr.adrienbrault.idea.symfony2plugin.tests.util;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;

import java.util.Collection;

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
        Collection<SymfonyCommand> commands = SymfonyCommandUtil.getCommands(getProject());

        for (String s : new String[]{"foo", "property", "const", "app:create-user-1", "app:create-user-2", "app:create-user-3"}) {
            SymfonyCommand command = commands.stream()
                .filter(symfonyCommand -> s.equals(symfonyCommand.getName())).findFirst()
                .orElseThrow();

            assertNotNull(command);
            assertNotNull(command.getPhpClass());
        }
    }
}
