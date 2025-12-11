package fr.adrienbrault.idea.symfony2plugin.tests.completion.command;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.completion.command.PhpCommandGotoCompletionRegistrar
 */
public class PhpCommandGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("FooCommand.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/completion/command/fixtures";
    }

    public void testCommandArguments() {
        String[] argsLookup = {"arg1", "arg2", "arg3" ,"argDef"};
        assertAtTextCompletionContains("GETARGUMENT", argsLookup);
        assertAtTextCompletionContains("HASARGUMENT", argsLookup);
    }

    public void testCommandOptions() {
        String[] optsLookup = {"opt1", "opt2", "opt3" ,"optDef", "optSingleDef"};
        assertAtTextCompletionContains("GETOPTION", optsLookup);
        assertAtTextCompletionContains("HASOPTION", optsLookup);
    }

}
