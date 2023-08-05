package fr.adrienbrault.idea.symfony2plugin.tests.form.util;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.impl.ClassConstantReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.PhpTypedElementImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeClass;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil
 */
public class FormUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/util/fixtures";
    }

    @SuppressWarnings({"ConstantConditions"})
    public void testGetFormTypeClassOnParameter() {
        assertEquals("\\Form\\FormType\\Foo", FormUtil.getFormTypeClassOnParameter(
            PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpTypedElementImpl.class, "<?php new \\Form\\FormType\\Foo();")
        ).getFQN());

        assertEquals("\\Form\\FormType\\Foo", FormUtil.getFormTypeClassOnParameter(
            PhpPsiElementFactory.createPhpPsiFromText(getProject(), StringLiteralExpressionImpl.class, "<?php '\\Form\\FormType\\Foo'")
        ).getFQN());

        assertEquals("\\Form\\FormType\\Foo", FormUtil.getFormTypeClassOnParameter(
            PhpPsiElementFactory.createPhpPsiFromText(getProject(), StringLiteralExpressionImpl.class, "<?php 'Form\\FormType\\Foo'")
        ).getFQN());

        assertEquals("\\Form\\FormType\\Foo", FormUtil.getFormTypeClassOnParameter(
            PhpPsiElementFactory.createPhpPsiFromText(getProject(), ClassConstantReferenceImpl.class, "<?php Form\\FormType\\Foo::class")
        ).getFQN());
    }

    @SuppressWarnings({"ConstantConditions"})
    public void testGetFormTypeClasses() {
        Map<String, FormTypeClass> formTypeClasses = FormUtil.getFormTypeClasses(getProject());
        assertNotNull(formTypeClasses.get("foo_type"));
        assertEquals(formTypeClasses.get("foo_type").getPhpClass(getProject()).getFQN(), "\\Form\\FormType\\Foo");

        assertNotNull(formTypeClasses.get("foo_bar"));
        assertEquals(formTypeClasses.get("foo_bar").getPhpClass(getProject()).getFQN(), "\\Form\\FormType\\FooBar");
    }

    public void testGetFormAliases() {
        PhpClass phpClass = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
                "class Foo implements \\Symfony\\Component\\Form\\FormTypeInterface {\n" +
                "  public function getName()" +
                "  {\n" +
                "    return 'bar';\n" +
                "    return 'foo';\n" +
                "  }\n" +
                "}"
        );

        assertContainsElements(Arrays.asList("bar", "foo"), FormUtil.getFormAliases(phpClass));
    }

    public void testGetFormAliasesPhpClassNotImplementsInterfaceAndShouldBeEmpty() {
        PhpClass phpClass = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
                "class Foo {\n" +
                "  public function getName()" +
                "  {\n" +
                "    return 'bar';\n" +
                "  }\n" +
                "}"
        );

        assertSize(0, FormUtil.getFormAliases(phpClass));
    }

    public void testGetFormParentOfPhpClass() {
        PhpClass phpClass = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
                "class Foo {\n" +
                "  public function getParent()" +
                "  {\n" +
                "    return 'bar';\n" +
                "  }\n" +
                "}"
        );

        assertContainsElements(FormUtil.getFormParentOfPhpClass(phpClass), "bar");

        phpClass = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
                "namespace My\\Bar {\n" +
                "  class Foo {\n" +
                "    public function getParent()" +
                "    {\n" +
                "      return __NAMESPACE__ . '\\Foo';\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertContainsElements(FormUtil.getFormParentOfPhpClass(phpClass), "My\\Bar\\Foo");

        phpClass = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
                "namespace My\\Bar {\n" +
                "  class Bar() {}\n" +
                "  class Foo {\n" +
                "    public function getParent()" +
                "    {\n" +
                "      return Bar::class;\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );

        assertContainsElements(FormUtil.getFormParentOfPhpClass(phpClass), "My\\Bar\\Bar");

        phpClass = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
            "namespace My\\Bar {\n" +
            "  class Bar() {}\n" +
            "  class Foo {\n" +
            "    public function getParent()" +
            "    {\n" +
            "      return true ? Bar::class : 'foobar';\n" +
            "    }\n" +
            "  }\n" +
            "}"
        );

        assertContainsElements(FormUtil.getFormParentOfPhpClass(phpClass), "My\\Bar\\Bar", "foobar");
    }

    public void testGetFormNameOfPhpClass() {

        // Symfony < 2.8
        assertEquals("datetime", FormUtil.getFormNameOfPhpClass(PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
                "namespace My\\Bar {\n" +
                "  class Foo {\n" +
                "    public function getName()\n" +
                "    {\n" +
                "        return 'datetime';\n" +
                "    }\n" +
                "  }\n" +
                "}"
        )));

        // Symfony 2.8+
        // getBlockPrefix for bc
        assertEquals("datetime", FormUtil.getFormNameOfPhpClass(PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
                "namespace My\\Bar {\n" +
                "  class Foo {\n" +
                "    public function getName()\n" +
                "    {\n" +
                "        return $this->getBlockPrefix();\n" +
                "    }\n" +
                "    public function getBlockPrefix()\n" +
                "    {\n" +
                "        return 'datetime';\n" +
                "    }\n" +
                "  }\n" +
                "}"
        )));

        // invalid method logic
        assertNull(FormUtil.getFormNameOfPhpClass(PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
                "namespace My\\Bar {\n" +
                "  class Foo {\n" +
                "    public function getName()\n" +
                "    {\n" +
                "        return $this->foo();\n" +
                "    }\n" +
                "    public function foo()\n" +
                "    {\n" +
                "        return 'datetime';\n" +
                "    }\n" +
                "  }\n" +
                "}"
        )));

        // Symfony 2.8
        // class name as type but stripped Type
        Collection<String[]> providers = new ArrayList<>() {{
            add(new String[]{"foo", "Foo"});
            add(new String[]{"foo", "FooType"});
            add(new String[]{"foo_bar", "FooBar"});
            add(new String[]{"foo_bar", "fooBar"});
            add(new String[]{"foo", "footype"});
            add(new String[]{"type", "type"});
        }};

        for (String[] provider : providers) {
            assertEquals(provider[0], FormUtil.getFormNameOfPhpClass(PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
                    "namespace My\\Bar {\n" +
                    "  class Foo {\n" +
                    "    public function getBlockPrefix()\n" +
                    "    {\n" +
                    "        return 'datetime';\n" +
                    "    }\n" +
                    "}\n" +
                    String.format("  class %s extends Foo {", provider[1]) +
                    "    public function getName()\n" +
                    "    {\n" +
                    "        return $this->getBlockPrefix();\n" +
                    "    }\n" +
                    "   }\n" +
                    "}"
            )));
        }

        // Symfony 3
        // class name if no "getName" method found
        assertEquals("My\\Bar\\Foo", FormUtil.getFormNameOfPhpClass(PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
                "namespace My\\Bar {\n" +
                "  class Foo {}\n" +
                "}"
        )));
    }

    public void testGetFormExtendedType() {
        PhpClass phpClass = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "class Foobar\n" +
            "{\n" +
            "   public function getExtendedType()\n" +
            "   {\n" +
            "       return true ? 'foobar' : Foobar::class;" +
            "   }\n" +
            "}\n"
        );

        assertContainsElements(FormUtil.getFormExtendedType(phpClass), "foobar", "Foobar");

        phpClass = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "namespace Car {\n" +
            "   class Foo {}\n" +
            "}" +
            "" +
            "namespace Bar {\n" +
            "   use Car\n" +
            "   \n" +
            "   class Foobar\n" +
            "   {\n" +
            "       public function getExtendedType()\n" +
            "       {\n" +
            "           return Foo::class;" +
            "      }\n" +
            "   }\n" +
            "}"
        );

        assertContainsElements(FormUtil.getFormExtendedType(phpClass), "Bar\\Foo");
    }

    public void testGetFormExtendedTypesAsArray() {
        PhpClass phpClass = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
                "class Foobar\n" +
                "{\n" +
                "   public function getExtendedTypes()\n" +
                "   {\n" +
                "       return [Foobar::class, 'test'];\n" +
                "   }\n" +
                "}\n"
        );

        assertContainsElements(FormUtil.getFormExtendedType(phpClass), "Foobar", "test");
    }

    public void testGetFormExtendedTypesAsYield() {
        PhpClass phpClass = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
                "class Foobar\n" +
                "{\n" +
                "   public function getExtendedTypes()\n" +
                "   {\n" +
                "       yield Foobar::class;\n" +
                "       yield 'test';\n'" +
                "   }\n" +
                "}\n"
        );

        assertContainsElements(FormUtil.getFormExtendedType(phpClass), "Foobar", "test");
    }

    public void testThatFormFieldsOfBuildFormForGetFormBuilderTypes() {
        Method method = PhpPsiElementFactory.createFromText(getProject(), Method.class, "<?php\n" +
            "class Foobar\n" +
            "{\n" +
            "   public public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options)\n" +
            "   {\n" +
            "       $builder\n" +
            "            ->add('task', TextType::class)\n" +
            "            ->add('dueDate', DateType::class)\n" +
            "            ->add('save', SubmitType::class)\n" +
            "        ;" +
            "   }\n" +
            "}\n"
        );

        Set<String> collect = Arrays.stream(FormUtil.getFormBuilderTypes(method))
            .map(methodReference -> ((StringLiteralExpression) Objects.requireNonNull(methodReference.getParameter(0))).getContents())
            .collect(Collectors.toSet());

        assertContainsElements(collect, "task");
        assertContainsElements(collect, "dueDate");
        assertContainsElements(collect, "save");
    }

    public void testThatFormFieldsOfBuildFormForGetFormBuilderTypesForCodeFlow() {
        Method method = PhpPsiElementFactory.createFromText(getProject(), Method.class, "<?php\n" +
            "class Foobar\n" +
            "{\n" +
            "   public function foo(array $options, \\Symfony\\Component\\Form\\FormBuilderInterface $builder2)\n" +
            "   {\n" +
            "       $builder2->add('builder2');\n" +
            "       $builder->addEventListener('foo', function (\\Symfony\\Component\\Form\\FormEvent $event) {\n" +
            "           $form = $event->getForm();\n" +
            "           $form->add('email2');\n" +
            "       });" +
            "   }\n" +
            "   public public function buildForm(\\Symfony\\Component\\Form\\FormBuilderInterface $builder, array $options)\n" +
            "   {\n" +
            "       $builder->addEventListener('foo', function (\\Symfony\\Component\\Form\\FormEvent $event) {\n" +
            "           $form = $event->getForm();\n" +
            "           $form->add('email');\n" +
            "       });" +
            "       $builder->add('task', TextType::class);\n" +
            "       $this->foo([], $builder);\n" +
            "   }\n" +
            "}\n"
        );

        Set<String> collect = Arrays.stream(FormUtil.getFormBuilderTypes(method))
            .map(methodReference -> ((StringLiteralExpression) Objects.requireNonNull(methodReference.getParameter(0))).getContents())
            .collect(Collectors.toSet());

        assertContainsElements(collect, "email");
        assertContainsElements(collect, "task");

        assertContainsElements(collect, "email2");
        assertContainsElements(collect, "builder2");
    }
}
