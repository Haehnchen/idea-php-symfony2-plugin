package fr.adrienbrault.idea.symfony2plugin.tests.completion;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.completion.PhpAttributeScopeValidator;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * Tests for PhpAttributeScopeValidator utility class.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.completion.PhpAttributeScopeValidator
 */
public class PhpAttributeScopeValidatorTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    @Override
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/completion/fixtures";
    }

    // ===============================
    // getField() tests
    // ===============================

    public void testGetFieldReturnsFieldWhenCaretBeforeModifierList() {
        // #<caret>
        // private $test;
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "class MyClass {\n" +
                "    #<caret>\n" +
                "    private $test;\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Field field = PhpAttributeScopeValidator.getField(element);

        assertNotNull("Should find field after caret", field);
        assertEquals("test", field.getName());
    }

    public void testGetFieldReturnsFieldWhenCaretBetweenAttributeLists() {
        // #[ORM\Column]
        // #<caret>
        // #[ORM\Column]
        // private $test;
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "class MyClass {\n" +
                "    #[Attr1]\n" +
                "    #<caret>\n" +
                "    #[Attr2]\n" +
                "    private $test;\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Field field = PhpAttributeScopeValidator.getField(element);

        assertNotNull("Should find field when caret is between attribute lists", field);
        assertEquals("test", field.getName());
    }

    public void testGetFieldReturnsFieldWhenCaretAfterFirstAttributeBeforeSecond() {
        // #[Attr1]
        // #<caret>
        // #[Attr2]
        // #[Attr3]
        // private string $name;
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "class MyClass {\n" +
                "    #[Attr1]\n" +
                "    #<caret>\n" +
                "    #[Attr2]\n" +
                "    #[Attr3]\n" +
                "    private string $name;\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Field field = PhpAttributeScopeValidator.getField(element);

        assertNotNull("Should find field when caret is between attribute lists", field);
        assertEquals("name", field.getName());
    }

    public void testGetFieldReturnsFieldWithTypedProperty() {
        // #<caret>
        // private string $typedField;
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "class MyClass {\n" +
                "    #<caret>\n" +
                "    private string $typedField;\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Field field = PhpAttributeScopeValidator.getField(element);

        assertNotNull("Should find typed field", field);
        assertEquals("typedField", field.getName());
    }

    public void testGetFieldReturnsNullWhenCaretBeforeMethod() {
        // #<caret>
        // public function test() {}
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "class MyClass {\n" +
                "    #<caret>\n" +
                "    public function test() {}\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Field field = PhpAttributeScopeValidator.getField(element);

        assertNull("Should return null when next element is a method, not a field", field);
    }

    public void testGetFieldReturnsNullWhenCaretBeforeClass() {
        // #<caret>
        // class MyClass {}
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "#<caret>\n" +
                "class MyClass {\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Field field = PhpAttributeScopeValidator.getField(element);

        assertNull("Should return null when next element is a class", field);
    }

    // ===============================
    // getMethod() tests
    // ===============================

    public void testGetMethodReturnsMethodWhenCaretBeforeMethod() {
        // #<caret>
        // public function test() {}
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "class MyClass {\n" +
                "    #<caret>\n" +
                "    public function test() {}\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Method method = PhpAttributeScopeValidator.getMethod(element);

        assertNotNull("Should find method after caret", method);
        assertEquals("test", method.getName());
    }

    public void testGetMethodReturnsNullWhenCaretBeforeField() {
        // #<caret>
        // private $field;
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "class MyClass {\n" +
                "    #<caret>\n" +
                "    private $field;\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        Method method = PhpAttributeScopeValidator.getMethod(element);

        assertNull("Should return null when next element is a field", method);
    }

    // ===============================
    // getPhpClass() tests
    // ===============================

    public void testGetPhpClassReturnsClassWhenCaretBeforeClass() {
        // #<caret>
        // class MyClass {}
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "#<caret>\n" +
                "class MyClass {\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PhpClass phpClass = PhpAttributeScopeValidator.getPhpClass(element);

        assertNotNull("Should find class after caret", phpClass);
        assertEquals("MyClass", phpClass.getName());
    }

    public void testGetPhpClassReturnsClassWhenCaretBetweenAttributeLists() {
        // #[Entity]
        // #<caret>
        // #[Table]
        // class MyClass {}
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "#[Attr1]\n" +
                "#<caret>\n" +
                "#[Attr2]\n" +
                "class MyClass {\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PhpClass phpClass = PhpAttributeScopeValidator.getPhpClass(element);

        assertNotNull("Should find class when caret is between attribute lists", phpClass);
        assertEquals("MyClass", phpClass.getName());
    }

    public void testGetPhpClassReturnsNullWhenCaretBeforeMethod() {
        // #<caret>
        // public function test() {}
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "class MyClass {\n" +
                "    #<caret>\n" +
                "    public function test() {}\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PhpClass phpClass = PhpAttributeScopeValidator.getPhpClass(element);

        assertNull("Should return null when next element is a method", phpClass);
    }

    // ===============================
    // getValidAttributeScope() tests
    // ===============================

    public void testGetValidAttributeScopeReturnsMethodFirst() {
        // When caret is before a method, getValidAttributeScope should return the method
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "class MyClass {\n" +
                "    #<caret>\n" +
                "    public function test() {}\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        var result = PhpAttributeScopeValidator.getValidAttributeScope(element);

        assertNotNull("Should find valid attribute scope", result);
        assertInstanceOf(result, Method.class);
        assertEquals("test", result.getName());
    }

    public void testGetValidAttributeScopeReturnsClass() {
        // When caret is before a class, getValidAttributeScope should return the class
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "#<caret>\n" +
                "class MyClass {\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        var result = PhpAttributeScopeValidator.getValidAttributeScope(element);

        assertNotNull("Should find valid attribute scope", result);
        assertInstanceOf(result, PhpClass.class);
        assertEquals("MyClass", result.getName());
    }

    public void testGetValidAttributeScopeReturnsField() {
        // When caret is before a field, getValidAttributeScope should return the field
        myFixture.configureByText(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "class MyClass {\n" +
                "    #<caret>\n" +
                "    private $field;\n" +
                "}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        var result = PhpAttributeScopeValidator.getValidAttributeScope(element);

        assertNotNull("Should find valid attribute scope", result);
        assertInstanceOf(result, Field.class);
        assertEquals("field", result.getName());
    }

    // ===============================
    // Completion integration tests for field attribute scope
    // ===============================

    public void testDoctrineFieldCompletionBetweenAttributeLists() {
        // Test that Doctrine field attributes appear when caret is between attribute lists
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "use Doctrine\\ORM\\Mapping as ORM;\n\n" +
                "#[ORM\\Entity]\n" +
                "class MyEntity {\n" +
                "    #[ORM\\Id]\n" +
                "    #<caret>\n" +
                "    #[ORM\\GeneratedValue]\n" +
                "    private int $id;\n" +
                "}",
            "#[Column]"
        );
    }

    public void testDoctrineFieldCompletionAfterFirstAttributeBeforeSecond() {
        // Test that Doctrine field attributes appear when caret is after first attribute, before second
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "use Doctrine\\ORM\\Mapping as ORM;\n\n" +
                "#[ORM\\Entity]\n" +
                "class MyEntity {\n" +
                "    #[ORM\\Id]\n" +
                "    #<caret>\n" +
                "    #[ORM\\Column]\n" +
                "    private string $name;\n" +
                "}",
            "#[GeneratedValue]"
        );
    }

    public void testTwigComponentPropertyCompletionBetweenAttributeLists() {
        // Test that Twig component property attributes appear when caret is between attribute lists
        assertCompletionContains(PhpFileType.INSTANCE,
            "<?php\n\n" +
                "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n\n" +
                "#[AsTwigComponent]\n" +
                "class Button {\n" +
                "    #[SomeAttr]\n" +
                "    #<caret>\n" +
                "    #[AnotherAttr]\n" +
                "    public string $label;\n" +
                "}",
            "#[ExposeInTemplate]"
        );
    }
}
