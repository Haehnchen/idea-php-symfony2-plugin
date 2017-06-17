package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.Variable;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpElementsUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("PhpElementsUtil.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getMethodParameterTypeHint
     */
    public void testGetMethodParameterClassHint() {
        assertEquals("\\DateTime", PhpElementsUtil.getMethodParameterTypeHint(
            PhpPsiElementFactory.createMethod(getProject(), "function foo(\\DateTime $e) {}")
        ));

        assertEquals("\\Iterator", PhpElementsUtil.getMethodParameterTypeHint(
            PhpPsiElementFactory.createMethod(getProject(), "function foo(/* foo */ \\Iterator $a, \\DateTime $b")
        ));

        assertNull(PhpElementsUtil.getMethodParameterTypeHint(
            PhpPsiElementFactory.createMethod(getProject(), "function foo(/* foo */ $a, \\DateTime $b")
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getArrayKeyValueMap
     */
    public void testGetArrayKeyValueMap() {
        assertEquals("foo", PhpElementsUtil.getArrayKeyValueMap(PhpPsiElementFactory.createPhpPsiFromText(getProject(), ArrayCreationExpression.class, "$foo = ['foo' => 'foo'];")).get("foo"));
        assertEquals("foo", PhpElementsUtil.getArrayKeyValueMap(PhpPsiElementFactory.createPhpPsiFromText(getProject(), ArrayCreationExpression.class, "$foo = [1 => 'foo'];")).get("1"));

        assertSize(0, PhpElementsUtil.getArrayKeyValueMap(PhpPsiElementFactory.createPhpPsiFromText(getProject(), ArrayCreationExpression.class, "$foo = [null => 'foo'];")).keySet());
        assertSize(0, PhpElementsUtil.getArrayKeyValueMap(PhpPsiElementFactory.createPhpPsiFromText(getProject(), ArrayCreationExpression.class, "$foo = ['' => 'foo'];")).keySet());
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getFirstVariableTypeInScope(Variable)
     */
    public void testGetFirstVariableTypeInScope() {
        PsiElement psiElement = myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "$foo = new \\DateTime();\n" +
            "$d->dispatch('foo', $f<caret>oo)->;").findElementAt(myFixture.getCaretOffset()).getParent();

        assertEquals("\\DateTime", PhpElementsUtil.getFirstVariableTypeInScope((Variable) psiElement));

        psiElement = myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "function foo() {" +
            "  $foo = new \\DateTime();\n" +
            "  $d->dispatch('foo', $f<caret>oo);\n" +
            "}").findElementAt(myFixture.getCaretOffset()).getParent();

        assertEquals("\\DateTime", PhpElementsUtil.getFirstVariableTypeInScope((Variable) psiElement));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getFirstVariableTypeInScope(Variable)
     */
    public void testGetFirstVariableTypeInScopeNotFound() {
        PsiElement psiElement = myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "$foo = new \\DateTime();\n" +
            "function foo() {\n" +
            "  $d->dispatch('foo', $f<caret>oo);" +
            "}").findElementAt(myFixture.getCaretOffset()).getParent();

        assertNull(PhpElementsUtil.getFirstVariableTypeInScope((Variable) psiElement));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getClassInsideNamespaceScope
     */
    public void testGetClassInsideNamespaceScope() {
        assertNotNull(PhpElementsUtil.getClassInsideNamespaceScope(getProject(), "Foo\\Foo", "\\Foo\\Bar"));
        assertNotNull(PhpElementsUtil.getClassInsideNamespaceScope(getProject(), "Foo\\Foo", "Bar"));
        assertNotNull(PhpElementsUtil.getClassInsideNamespaceScope(getProject(), "\\Foo\\Foo", "Bar"));
        assertNotNull(PhpElementsUtil.getClassInsideNamespaceScope(getProject(), "\\Foo\\Foo", "Bar\\"));
        assertNotNull(PhpElementsUtil.getClassInsideNamespaceScope(getProject(), "\\Foo\\Foo\\", "Bar\\"));

        assertNull(PhpElementsUtil.getClassInsideNamespaceScope(getProject(), "Fooa", "Bar"));
        assertNull(PhpElementsUtil.getClassInsideNamespaceScope(getProject(), "Fooa\\Foo", "Bar"));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#isInstanceOf
     */
    public void testIsInstanceOf() {
        if("162.1121.34".equals(PluginManager.getPlugin(PluginId.getId("com.jetbrains.php")).getVersion())) {
            System.out.println("Skipping PhpElementsUtil.testIsInstanceOf for PhpStorm 2016.2 (162.1121.34) inconsistently behavior fixed in 2016.2.1");
            return;
        }

        myFixture.copyFileToProject("InstanceOf.php");

        Collection<String[]> providers = new ArrayList<String[]>() {{
            add(new String[] {"\\Instance\\Of\\Foo", "\\Instance\\Of\\Bar"});
            add(new String[] {"\\Instance\\Of\\Foo", "\\Instance\\Of\\Cool"});
            add(new String[] {"\\Instance\\Of\\Car", "\\Instance\\Of\\Bar"});
            add(new String[] {"\\Instance\\Of\\Car", "\\Instance\\Of\\Foo"});
            add(new String[] {"\\Instance\\Of\\Car", "\\Instance\\Of\\Cool"});

            // backslash
            add(new String[] {"Instance\\Of\\Car", "Instance\\Of\\Cool"});
            add(new String[] {"Instance\\Of\\Car", "\\Instance\\Of\\Cool"});
            add(new String[] {"\\Instance\\Of\\Car", "Instance\\Of\\Cool"});

            // dups
            add(new String[] {"\\Instance\\Of\\Car", "Instance\\Of\\Apple"});
            add(new String[] {"\\Instance\\Of\\Foo", "Instance\\Of\\Apple"});
        }};

        for (String[] provider : providers) {
            assertTrue(PhpElementsUtil.isInstanceOf(getProject(), provider[0], provider[1]));
            assertTrue(PhpElementsUtil.isInstanceOf(PhpElementsUtil.getClassInterface(getProject(), provider[0]), provider[1]));

            assertTrue(PhpElementsUtil.isInstanceOf(
                PhpElementsUtil.getClassInterface(getProject(), provider[0]),
                PhpElementsUtil.getClassInterface(getProject(), provider[1])
            ));
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#insertUseIfNecessary
     */
    public void testUseImports() {
        assertEquals("Bar", PhpElementsUtil.insertUseIfNecessary(PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
            "namespace Foo;\n" +
            "class Foo{}\n"
        ), "\\Foo\\Bar"));

        assertEquals("Bar", PhpElementsUtil.insertUseIfNecessary(PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
            "namespace Foo;\n" +
            "use Foo\\Bar;\n" +
            "class Foo{}\n"
        ), "\\Foo\\Bar"));

        assertEquals("Apple", PhpElementsUtil.insertUseIfNecessary(PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpClass.class, "<?php\n" +
            "namespace Bar\\Bar;\n" +
            "use Foo as Car;\n" +
            "class Foo{}\n"
        ), "Foo\\Cool\\Apple"));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getVariableReferencesInScope
     */
    public void testGetVariableReferencesInScopeForVariable() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "function foobar() {\n" +
            "  $var = new \\DateTime();" +
            "  $va<caret>r->format();" +
            "  $var->modify();" +
            "\n}"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        Collection<Variable> vars = PhpElementsUtil.getVariableReferencesInScope((Variable) psiElement.getParent());
        assertSize(2, vars);

        assertNotNull(ContainerUtil.find(vars, variable ->
            "$var = new \\DateTime()".equals(variable.getParent().getText()))
        );

        assertNotNull(ContainerUtil.find(vars, variable ->
            "$var->modify()".equals(variable.getParent().getText()))
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getVariableReferencesInScope
     */
    public void testGetVariableReferencesInScopeForVariableDeclaration() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "function foobar() {\n" +
            "  $v<caret>ar = new \\DateTime();" +
            "  $var->format();" +
            "  $var->modify();" +
            "\n}"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        Collection<Variable> vars = PhpElementsUtil.getVariableReferencesInScope((Variable) psiElement.getParent());
        assertSize(2, vars);

        assertNotNull(ContainerUtil.find(vars, variable ->
            "$var->format()".equals(variable.getParent().getText()))
        );

        assertNotNull(ContainerUtil.find(vars, variable ->
            "$var->modify()".equals(variable.getParent().getText()))
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getParameterListArrayValuePattern
     */
    public void testGetParameterListArrayValuePattern() {
        String[] strings = {
            "foo(['<caret>']",
            "foo(['<caret>' => 'foo']",
            "foo(['foo' => null, '<caret>' => null]"
        };

        for (String s : strings) {
            myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" + s);

            assertTrue(
                PhpElementsUtil.getParameterListArrayValuePattern().accepts(myFixture.getFile().findElementAt(myFixture.getCaretOffset()))
            );
        }

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "foobar(['foobar' => '<caret>'])"
        );

        assertFalse(
            PhpElementsUtil.getParameterListArrayValuePattern().accepts(myFixture.getFile().findElementAt(myFixture.getCaretOffset()))
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getStringValue
     */
    public void testGetStringValueForStringValue() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php $foo = 'foo<caret>bar';");
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        assertEquals("foobar", PhpElementsUtil.getStringValue(psiElement));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getStringValue
     */
    public void testGetStringValueForClassConstants() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php class Foobar {};\n $foo = Foobar::cl<caret>ass;");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        assertEquals("Foobar", PhpElementsUtil.getStringValue(psiElement));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getStringValue
     */
    public void testGetStringValueForFieldWithClassConstants() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php class Foobar { var $x = Foobar::class;\n function test() { $foo = $this-><caret>x; } };\n ");

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        assertEquals("Foobar", PhpElementsUtil.getStringValue(psiElement));
    }
}
