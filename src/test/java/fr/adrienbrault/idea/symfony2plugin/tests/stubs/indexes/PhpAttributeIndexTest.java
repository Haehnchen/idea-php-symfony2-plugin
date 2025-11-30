package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

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

        // Key: Attribute FQN, Value: [class FQN, method name, filter name]
        assertIndexContainsKeyWithValue(PhpAttributeIndex.KEY, "\\Twig\\Attribute\\AsTwigFilter", value ->
            value.size() == 3 &&
            value.get(0).equals("App\\Twig\\AppExtension") &&
            value.get(1).equals("formatProductNumberFilter") &&
            value.get(2).equals("product_number_filter")
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

        // Key: Attribute FQN, Value: [class FQN, method name, function name]
        assertIndexContainsKeyWithValue(PhpAttributeIndex.KEY, "\\Twig\\Attribute\\AsTwigFunction", value ->
            value.size() == 3 &&
            value.get(0).equals("App\\Twig\\AppExtension") &&
            value.get(1).equals("formatProductNumberFunction") &&
            value.get(2).equals("product_number_function")
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

        // Key: Attribute FQN, Value: [class FQN, method name, test name]
        assertIndexContainsKeyWithValue(PhpAttributeIndex.KEY, "\\Twig\\Attribute\\AsTwigTest", value ->
            value.size() == 3 &&
            value.get(0).equals("App\\Twig\\AppExtension") &&
            value.get(1).equals("formatProductNumberTest") &&
            value.get(2).equals("product_number_test")
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

        // Key: Attribute FQN, Value: [class FQN]
        assertIndexContainsKeyWithValue(PhpAttributeIndex.KEY, "\\Symfony\\Component\\Console\\Attribute\\AsCommand", value ->
            value.size() == 1 &&
            value.get(0).equals("App\\Command\\CreateUserCommand")
        );
    }
}
