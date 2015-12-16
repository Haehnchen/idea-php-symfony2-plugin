package fr.adrienbrault.idea.symfony2plugin.tests.form.util;

import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.impl.ClassConstantReferenceImpl;
import com.jetbrains.php.lang.psi.elements.impl.PhpTypedElementImpl;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeClass;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

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
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
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
        assertEquals(formTypeClasses.get("foo_type").getPhpClass().getFQN(), "\\Form\\FormType\\Foo");

        assertNotNull(formTypeClasses.get("foo_bar"));
        assertEquals(formTypeClasses.get("foo_bar").getPhpClass().getFQN(), "\\Form\\FormType\\FooBar");
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

        assertEquals("bar", FormUtil.getFormParentOfPhpClass(phpClass));

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

        assertEquals("My\\Bar\\Foo", FormUtil.getFormParentOfPhpClass(phpClass));

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

        assertEquals("My\\Bar\\Bar", FormUtil.getFormParentOfPhpClass(phpClass));
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
        Collection<String[]> providers = new ArrayList<String[]>() {{
            add(new String[] {"foo", "Foo"});
            add(new String[] {"foo", "FooType"});
            add(new String[] {"foo_bar", "FooBar"});
            add(new String[] {"foo_bar", "fooBar"});
            add(new String[] {"foo", "footype"});
            add(new String[] {"type", "type"});
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
}
