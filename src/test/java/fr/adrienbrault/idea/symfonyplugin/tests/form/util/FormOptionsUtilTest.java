package fr.adrienbrault.idea.symfonyplugin.tests.form.util;

import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfonyplugin.form.dict.FormClass;
import fr.adrienbrault.idea.symfonyplugin.form.dict.FormClassEnum;
import fr.adrienbrault.idea.symfonyplugin.form.dict.FormOptionEnum;
import fr.adrienbrault.idea.symfonyplugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfonyplugin.form.visitor.FormOptionVisitor;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.form.util.FormOptionsUtil
 */
public class FormOptionsUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\Form\n" +
            "{\n" +
            "    interface FormTypeExtensionInterface\n" +
            "    {\n" +
            "        public function getExtendedType();\n" +
            "    }\n" +
            "\n" +
            "    interface FormTypeInterface\n" +
            "    {\n" +
            "        public function getName();\n" +
            "    }\n" +
            "}"
        );

        myFixture.copyFileToProject("FormOptionsUtil.php");
        myFixture.copyFileToProject("FormOptionsUtilKeys.php");

    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/form/util/fixtures";
    }

    /**
     * @see FormOptionsUtil#getExtendedTypeClasses
     */
    public void testGetExtendedTypeClassesAsStringValue() {
        FormClass foo = ContainerUtil.getFirstItem(FormOptionsUtil.getExtendedTypeClasses(getProject(), "foo_bar_my_type"));

        assertNotNull(foo);
        assertEquals(FormClassEnum.EXTENSION, foo.getType());
        assertEquals("\\Foo\\Bar\\MyType", foo.getPhpClass().getFQN());
    }

    /**
     * @see FormOptionsUtil#getExtendedTypeClasses
     */
    public void testGetExtendedTypeClassesAsClassConstant() {
        for (String s : new String[]{"Foo\\Bar\\MyType", "\\Foo\\Bar\\MyType"}) {
            FormClass myType = ContainerUtil.getFirstItem(FormOptionsUtil.getExtendedTypeClasses(getProject(), s));

            assertNotNull(myType);
            assertEquals(FormClassEnum.EXTENSION, myType.getType());
            assertEquals("\\Foo\\Bar\\BarType", myType.getPhpClass().getFQN());
        }
    }

    public void testClassOptionsVisitorWithExtensionAndParents() {

        final Set<String> options = new HashSet<String>();

        FormOptionsUtil.visitFormOptions(getProject(), "foo", (psiElement, option, formClass, optionEnum) ->
            options.add(option)
        );

        assertContainsElements(options, "MyType", "BarTypeParent", "BarTypeExtension");
    }

    public void testClassOptionsVisitorWithExtensionAndParentsWithClassConstant() {

        final Set<String> optionsClass = new HashSet<String>();

        FormOptionsUtil.visitFormOptions(getProject(), "Options\\Bar\\Foobar", new FormOptionVisitor() {
            @Override
            public void visit(@NotNull PsiElement psiElement, @NotNull String option, @NotNull FormClass formClass, @NotNull FormOptionEnum optionEnum) {
                optionsClass.add(option);
            }
        });

        assertContainsElements(optionsClass, "BarType");
    }

    /**
     * @see FormOptionsUtil#getFormExtensionKeys
     */
    public void testGetFormExtensionsKeysTargets() {
        StringLiteralExpression contents = PhpPsiElementFactory.createFromText(getProject(), StringLiteralExpression.class, "<?php 'BarType';");
        Collection<PsiElement> formExtensionsKeysTargets = FormOptionsUtil.getFormExtensionsKeysTargets(contents, "Options\\Bar\\Foobar");
        assertTrue(formExtensionsKeysTargets.size() > 0);
    }


    /**
     * @see FormOptionsUtil#getMethodReferenceStringParameter
     */
    public void testGetMethodReferenceStringParameter() {
        MethodReference methodReference = PhpPsiElementFactory.createFromText(getProject(), MethodReference.class, "<?php class Foobar { function foo() { $this->bar('test', 'my_value'); } };\n");
        assertContainsElements(FormOptionsUtil.getMethodReferenceStringParameter(methodReference, new String[] {"foo"}, "bar", "test"), "my_value");
    }
}
