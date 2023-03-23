package fr.adrienbrault.idea.symfony2plugin.tests.dic.command;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.dic.command.SymfonyCommandTestRunLineMarkerProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see SymfonyCommandTestRunLineMarkerProvider
 */
public class SymfonyCommandTestRunLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/command/fixtures";
    }

    public void testCommandNameFromDefaultNameProperty() {
        PhpClass phpClass = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "class FoobarCommand extends \\Symfony\\Component\\Console\\Command\\Command {\n" +
            "    protected static $defaultName = 'app:create-user';\n" +
            "}"
        );

        assertEquals(List.of("app:create-user"), SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass));

        PhpClass phpClass1 = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "class FoobarCommand extends \\Symfony\\Component\\Console\\Command\\Command {\n" +
            "    protected static $defaultName = 'app:create-user|foobar|car';\n" +
            "}"
        );

        assertEquals(List.of("app:create-user", "foobar", "car"), SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass1));
    }

    public void testCommandNameFromDefaultPhpProperty() {
        PhpClass phpClass = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand(\n" +
            "    name: 'app:create-user',\n" +
            "    description: 'Creates a new user.',\n" +
            "    hidden: false,\n" +
            "    aliases: ['app:add-user', 'foo']\n" +
            ")]\n" +
            "class FoobarCommand extends \\Symfony\\Component\\Console\\Command\\Command {\n" +
            "}"
        );

        assertEquals(List.of("app:create-user", "app:add-user", "foo"), SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass));
    }

    public void testCommandNameFromDefaultPhpPropertyAsDefault() {
        PhpClass phpClass = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "#[AsCommand('app:create-user')]\n" +
            "class FoobarCommand extends \\Symfony\\Component\\Console\\Command\\Command {\n" +
            "}"
        );

        assertEquals(List.of("app:create-user"), SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass));
    }

    public void testCommandNameFromSetName() {
        PhpClass phpClass = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "class FoobarCommand extends \\Symfony\\Component\\Console\\Command\\Command\n" +
            "{\n" +
            "    public function configure()\n" +
            "    {\n" +
            "        $this->setName('set-const-command');\n" +
            "    }\n" +
            "}"
        );

        assertEquals(List.of("set-const-command"), SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass));
    }
}
