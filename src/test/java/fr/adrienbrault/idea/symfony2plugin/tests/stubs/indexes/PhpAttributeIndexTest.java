package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex
 */
public class PhpAttributeIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testBasicIndexFunctionality() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig;\n" +
            "\n" +
            "use Twig\\Attribute\\AsTwigFilter;\n" +
            "\n" +
            "class AppExtension\n" +
            "{\n" +
            "    #[AsTwigFilter('product_number_filter')]\n" +
            "    public function formatProductNumberFilter(string $number): string\n" +
            "    {\n" +
            "    }\n" +
            "}\n"
        );

        // Just check if the key exists
        assertIndexContains(PhpAttributeIndex.KEY, "\\Twig\\Attribute\\AsTwigFilter");
    }

    public void testIndexKeysExist() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig;\n" +
            "\n" +
            "use Twig\\Attribute\\AsTwigFilter;\n" +
            "use Twig\\Attribute\\AsTwigFunction;\n" +
            "use Twig\\Attribute\\AsTwigTest;\n" +
            "\n" +
            "class AppExtension\n" +
            "{\n" +
            "    #[AsTwigFilter('product_number_filter')]\n" +
            "    public function formatProductNumberFilter(string $number): string\n" +
            "    {\n" +
            "    }\n" +
            "\n" +
            "    #[AsTwigFunction('product_number_function')]\n" +
            "    public function formatProductNumberFunction(string $number): string\n" +
            "    {\n" +
            "    }\n" +
            "\n" +
            "    #[AsTwigTest('product_number_test')]\n" +
            "    public function formatProductNumberTest(string $number): string\n" +
            "    {\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "namespace App\\Command;\n" +
            "\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "use Symfony\\Component\\Console\\Command\\Command;\n" +
            "\n" +
            "#[AsCommand(\n" +
            "    name: 'app:create-user',\n" +
            "    aliases: ['app:create-admin']\n" +
            ")]\n" +
            "class CreateUserCommand extends Command\n" +
            "{\n" +
            "}\n"
        );

        assertIndexContains(PhpAttributeIndex.KEY,
            "\\Twig\\Attribute\\AsTwigFilter",
            "\\Twig\\Attribute\\AsTwigFunction",
            "\\Twig\\Attribute\\AsTwigTest",
            "\\Symfony\\Component\\Console\\Attribute\\AsCommand"
        );
    }

    public void testThatTwigFilterAttributeIsInIndex() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig;\n" +
            "\n" +
            "use Twig\\Attribute\\AsTwigFilter;\n" +
            "\n" +
            "class AppExtension\n" +
            "{\n" +
            "    #[AsTwigFilter('product_number_filter')]\n" +
            "    public function formatProductNumberFilter(string $number): string\n" +
            "    {\n" +
            "    }\n" +
            "}\n"
        );

        // Key: Attribute FQN, Value: typed method target
        assertIndexContainsKeyWithValue(PhpAttributeIndex.KEY, "\\Twig\\Attribute\\AsTwigFilter", value ->
            value.stream().anyMatch(target ->
                target.scope() == PhpAttributeIndex.TargetScope.METHOD &&
                target.classFqn().equals("App\\Twig\\AppExtension") &&
                "formatProductNumberFilter".equals(target.memberName()) &&
                target.data().equals(java.util.List.of("product_number_filter"))
            )
        );
    }

    public void testThatTwigFunctionAttributeIsInIndex() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig;\n" +
            "\n" +
            "use Twig\\Attribute\\AsTwigFunction;\n" +
            "\n" +
            "class AppExtension\n" +
            "{\n" +
            "    #[AsTwigFunction('product_number_function')]\n" +
            "    public function formatProductNumberFunction(string $number): string\n" +
            "    {\n" +
            "    }\n" +
            "}\n"
        );

        // Key: Attribute FQN, Value: typed method target
        assertIndexContainsKeyWithValue(PhpAttributeIndex.KEY, "\\Twig\\Attribute\\AsTwigFunction", value ->
            value.stream().anyMatch(target ->
                target.scope() == PhpAttributeIndex.TargetScope.METHOD &&
                target.classFqn().equals("App\\Twig\\AppExtension") &&
                "formatProductNumberFunction".equals(target.memberName()) &&
                target.data().equals(java.util.List.of("product_number_function"))
            )
        );
    }

    public void testThatTwigTestAttributeIsInIndex() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Twig;\n" +
            "\n" +
            "use Twig\\Attribute\\AsTwigTest;\n" +
            "\n" +
            "class AppExtension\n" +
            "{\n" +
            "    #[AsTwigTest('product_number_test')]\n" +
            "    public function formatProductNumberTest(string $number): string\n" +
            "    {\n" +
            "    }\n" +
            "}\n"
        );

        // Key: Attribute FQN, Value: typed method target
        assertIndexContainsKeyWithValue(PhpAttributeIndex.KEY, "\\Twig\\Attribute\\AsTwigTest", value ->
            value.stream().anyMatch(target ->
                target.scope() == PhpAttributeIndex.TargetScope.METHOD &&
                target.classFqn().equals("App\\Twig\\AppExtension") &&
                "formatProductNumberTest".equals(target.memberName()) &&
                target.data().equals(java.util.List.of("product_number_test"))
            )
        );
    }

    public void testThatAsCommandOnClassIsInIndex() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Command;\n" +
            "\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "use Symfony\\Component\\Console\\Command\\Command;\n" +
            "\n" +
            "#[AsCommand(\n" +
            "    name: 'app:create-user',\n" +
            "    aliases: ['app:create-admin']\n" +
            ")]\n" +
            "class CreateUserCommand extends Command\n" +
            "{\n" +
            "}\n"
        );

        // Key: Attribute FQN, Value: typed class target
        assertIndexContainsKeyWithValue(PhpAttributeIndex.KEY, "\\Symfony\\Component\\Console\\Attribute\\AsCommand", value ->
            value.stream().anyMatch(target ->
                target.scope() == PhpAttributeIndex.TargetScope.PHP_CLASS &&
                target.classFqn().equals("App\\Command\\CreateUserCommand") &&
                target.memberName() == null &&
                target.data().equals(java.util.List.of("app:create-user", "app:create-admin"))
            )
        );
    }

    public void testThatExcludeOnClassIsInIndex() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Service;\n" +
            "\n" +
            "use Symfony\\Component\\DependencyInjection\\Attribute\\Exclude;\n" +
            "\n" +
            "#[Exclude]\n" +
            "class ExcludedService\n" +
            "{\n" +
            "}\n"
        );

        // Key: Attribute FQN, Value: typed class target
        assertIndexContainsKeyWithValue(PhpAttributeIndex.KEY, "\\Symfony\\Component\\DependencyInjection\\Attribute\\Exclude", value ->
            value.stream().anyMatch(target ->
                target.scope() == PhpAttributeIndex.TargetScope.PHP_CLASS &&
                target.classFqn().equals("App\\Service\\ExcludedService") &&
                target.memberName() == null
            )
        );
    }

    public void testThatMultipleMethodCommandsInOneFileAreInIndex() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Command;\n" +
            "\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "\n" +
            "class UserCommands\n" +
            "{\n" +
            "    #[AsCommand('app:user:create')]\n" +
            "    public function create(): int { return 0; }\n" +
            "\n" +
            "    #[AsCommand(name: 'app:user:delete', aliases: ['app:user:remove'])]\n" +
            "    public function delete(): int { return 0; }\n" +
            "\n" +
            "    #[AsCommand('app:user:private')]\n" +
            "    private function privateCommand(): int { return 0; }\n" +
            "}\n"
        );

        Collection<PhpAttributeIndex.AttributeTarget> targets = PhpAttributeIndexUtil.getAttributeData(getProject(), "\\Symfony\\Component\\Console\\Attribute\\AsCommand");

        assertTrue(targets.stream().anyMatch(target ->
            target.scope() == PhpAttributeIndex.TargetScope.METHOD &&
            target.classFqn().equals("App\\Command\\UserCommands") &&
            "create".equals(target.memberName()) &&
            target.data().equals(java.util.List.of("app:user:create"))
        ));
        assertTrue(targets.stream().anyMatch(target ->
            target.scope() == PhpAttributeIndex.TargetScope.METHOD &&
            target.classFqn().equals("App\\Command\\UserCommands") &&
            "delete".equals(target.memberName()) &&
            target.data().equals(java.util.List.of("app:user:delete", "app:user:remove"))
        ));
        assertFalse(targets.stream().anyMatch(target -> "privateCommand".equals(target.memberName())));
    }
}
