package fr.adrienbrault.idea.symfony2plugin.tests.completion.command;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.completion.command.PhpCommandGotoCompletionRegistrar
 */
public class PhpCommandGotoCompletionRegistrar extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("FooCommand.php"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testTwigExtensionFilterCompletion() {

        String[] strings = {"arg1", "arg2", "arg3" /*,"argDef", "optDef", "optSingleDef" */};

        assertAtTextCompletionContains("<getArgument>", strings);
        assertAtTextCompletionContains("<hasArgument>", strings);
        assertAtTextCompletionContains("<getOption>", strings);
        assertAtTextCompletionContains("<hasOption>", strings);
    }


}
