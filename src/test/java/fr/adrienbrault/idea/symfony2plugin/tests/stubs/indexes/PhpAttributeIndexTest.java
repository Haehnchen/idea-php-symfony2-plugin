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

    public void testFormAttributes() {
        myFixture.configureByText(PhpFileType.INSTANCE, """
            <?php
            namespace App;
            use Symfony\\Component\\Form\\Attribute\\AsFormType;
            use Symfony\\Component\\Form\\Attribute\\FormField;
            use Symfony\\Component\\Form\\Extension\\Core\\Type\\EmailType;

            #[AsFormType(options: ['label' => 'Account'])]
            class AccountData {
                #[FormField(EmailType::class, options: ['required' => true])]
                public ?string $email = null;

                #[FormField(name: 'displayName')]
                public ?string $internalName = null;

                #[FormField(null, [], 'positionalName')]
                public ?string $positional = null;

                #[FormField(name: null)]
                public ?string $defaultName = null;

                #[FormField(name: 'reorderedName', type: EmailType::class, options: [])]
                public ?string $reordered = null;

                public ?string $unannotated = null;
            }

            #[AsFormType]
            class EmptyData {}
            """);

        Collection<PhpAttributeIndex.AttributeTarget> classes = PhpAttributeIndexUtil.getAttributeData(
            getProject(), PhpAttributeIndex.PhpAttributeIndexer.AS_FORM_TYPE_ATTRIBUTE);
        assertSize(2, classes);
        assertTrue(classes.stream().allMatch(target -> target.scope() == PhpAttributeIndex.TargetScope.PHP_CLASS && target.memberName() == null));
        assertTrue(classes.stream().anyMatch(target -> target.classFqn().equals("App\\AccountData")));
        assertTrue(classes.stream().anyMatch(target -> target.classFqn().equals("App\\EmptyData")));

        Collection<PhpAttributeIndex.AttributeTarget> fields = PhpAttributeIndexUtil.getAttributeData(
            getProject(), PhpAttributeIndex.PhpAttributeIndexer.FORM_FIELD_ATTRIBUTE);
        assertSize(5, fields);
        PhpAttributeIndex.AttributeTarget email = fields.stream()
            .filter(target -> "email".equals(target.memberName()))
            .findFirst()
            .orElseThrow();
        assertEquals(PhpAttributeIndex.TargetScope.PROPERTY, email.scope());
        assertEquals("App\\AccountData", email.classFqn());
        assertEmpty(email.data());

        PhpAttributeIndex.AttributeTarget internalName = fields.stream()
            .filter(target -> "internalName".equals(target.memberName()))
            .findFirst()
            .orElseThrow();
        assertEquals(PhpAttributeIndex.TargetScope.PROPERTY, internalName.scope());
        assertEquals("App\\AccountData", internalName.classFqn());
        assertEmpty(internalName.data());

        PhpAttributeIndex.AttributeTarget positional = fields.stream()
            .filter(target -> "positional".equals(target.memberName()))
            .findFirst()
            .orElseThrow();
        assertEquals(PhpAttributeIndex.TargetScope.PROPERTY, positional.scope());
        assertEquals("App\\AccountData", positional.classFqn());
        assertEmpty(positional.data());

        PhpAttributeIndex.AttributeTarget defaultName = fields.stream()
            .filter(target -> "defaultName".equals(target.memberName()))
            .findFirst()
            .orElseThrow();
        assertEquals(PhpAttributeIndex.TargetScope.PROPERTY, defaultName.scope());
        assertEquals("App\\AccountData", defaultName.classFqn());
        assertEmpty(defaultName.data());

        PhpAttributeIndex.AttributeTarget reordered = fields.stream()
            .filter(target -> "reordered".equals(target.memberName()))
            .findFirst()
            .orElseThrow();
        assertEquals(PhpAttributeIndex.TargetScope.PROPERTY, reordered.scope());
        assertEquals("App\\AccountData", reordered.classFqn());
        assertEmpty(reordered.data());
    }

    public void testFormAttributesAreLimitedToTheirTargetScopes() {
        myFixture.configureByText(PhpFileType.INSTANCE, """
            <?php
            use Symfony\\Component\\Form\\Attribute\\AsFormType;
            use Symfony\\Component\\Form\\Attribute\\FormField;
            #[FormField]
            class InvalidData {
                #[AsFormType]
                public $property;
                #[FormField]
                public const FIELD = 'field';
                #[AsFormType, FormField]
                public function method() {}
            }
            """);
        assertEmpty(PhpAttributeIndexUtil.getAttributeData(getProject(), PhpAttributeIndex.PhpAttributeIndexer.AS_FORM_TYPE_ATTRIBUTE));
        assertEmpty(PhpAttributeIndexUtil.getAttributeData(getProject(), PhpAttributeIndex.PhpAttributeIndexer.FORM_FIELD_ATTRIBUTE));
    }

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
                target.memberName() == null
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
            "    #[AsCommand('app:user:delete')]\n" +
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
            "create".equals(target.memberName())
        ));
        assertTrue(targets.stream().anyMatch(target ->
            target.scope() == PhpAttributeIndex.TargetScope.METHOD &&
            target.classFqn().equals("App\\Command\\UserCommands") &&
            "delete".equals(target.memberName())
        ));
        assertFalse(targets.stream().anyMatch(target -> "privateCommand".equals(target.memberName())));
    }
}
