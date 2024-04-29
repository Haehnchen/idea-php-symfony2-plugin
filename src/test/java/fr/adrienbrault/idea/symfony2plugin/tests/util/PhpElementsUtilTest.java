package fr.adrienbrault.idea.symfony2plugin.tests.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpElementsUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("PhpElementsUtil.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/util/fixtures";
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getMethodParameterTypeHints
     */
    public void testGetMethodParameterClassHint() {
        assertEquals(Collections.singletonList("\\DateTime"), PhpElementsUtil.getMethodParameterTypeHints(
            PhpPsiElementFactory.createMethod(getProject(), "function foo(\\DateTime $e) {}")
        ));

        assertEquals(Arrays.asList("\\DateTime", "\\DateInterval"), PhpElementsUtil.getMethodParameterTypeHints(
            PhpPsiElementFactory.createMethod(getProject(), "function foo(\\DateTime|\\DateInterval $e) {}")
        ));

        assertEquals(Collections.singletonList("\\Iterator"), PhpElementsUtil.getMethodParameterTypeHints(
            PhpPsiElementFactory.createMethod(getProject(), "function foo(/* foo */ \\Iterator $a, \\DateTime $b")
        ));

        assertEmpty(PhpElementsUtil.getMethodParameterTypeHints(
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
        myFixture.copyFileToProject("InstanceOf.php");

        Collection<String[]> providers = new ArrayList<>() {{
            add(new String[]{"\\Instance\\Of\\Foo", "\\Instance\\Of\\Bar"});
            add(new String[]{"\\Instance\\Of\\Foo", "\\Instance\\Of\\Cool"});
            add(new String[]{"\\Instance\\Of\\Foo", "\\Instance\\Of\\Apple"});
            add(new String[]{"\\Instance\\Of\\Car", "\\Instance\\Of\\Bar"});
            add(new String[]{"\\Instance\\Of\\Car", "\\Instance\\Of\\Foo"});
            add(new String[]{"\\Instance\\Of\\Car", "\\Instance\\Of\\Cool"});
            add(new String[]{"\\Instance\\Of\\Car", "\\Instance\\Of\\Car"});

            // backslash
            add(new String[]{"Instance\\Of\\Car", "Instance\\Of\\Cool"});
            add(new String[]{"Instance\\Of\\Car", "\\Instance\\Of\\Cool"});
            add(new String[]{"\\Instance\\Of\\Car", "Instance\\Of\\Cool"});

            add(new String[]{"\\Instance\\Of\\Car", "Instance\\Of\\Apple"});
            add(new String[]{"\\Instance\\Of\\Foo", "Instance\\Of\\Apple"});
            add(new String[]{"\\Instance\\Of\\Apple", "Instance\\Of\\Apple"});
        }};

        for (String[] provider : providers) {
            String errorMessage = "'%s' not instance of '%s'".formatted(provider[0], provider[1]);

            assertTrue(errorMessage, PhpElementsUtil.isInstanceOf(getProject(), provider[0], provider[1]));
            assertTrue(errorMessage, PhpElementsUtil.isInstanceOf(PhpElementsUtil.getClassInterface(getProject(), provider[0]), provider[1]));

            assertTrue(errorMessage, PhpElementsUtil.isInstanceOf(
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

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#getConstructorArgumentByName
     */
    public void testGetConstructorArgumentByName() {
        PhpClass phpClass = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php class Foobar { function __construct($foo, $foobar) {} };\n ");

        assertEquals(0, PhpElementsUtil.getConstructorArgumentByName(phpClass, "foo"));
        assertEquals(1, PhpElementsUtil.getConstructorArgumentByName(phpClass, "foobar"));
        assertEquals(-1, PhpElementsUtil.getConstructorArgumentByName(phpClass, "UNKNOWN"));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil#isMethodInstanceOf
     */
    public void testIsMethodInstanceOf() {
        assertTrue(PhpElementsUtil.isMethodInstanceOf(
            PhpElementsUtil.getClassMethod(getProject(), "FooBar\\Bar", "getBar"),
            new MethodMatcher.CallToSignature("FooBar\\Foo", "getBar"))
        );

        assertTrue(PhpElementsUtil.isMethodInstanceOf(
            PhpElementsUtil.getClassMethod(getProject(), "FooBar\\Car", "getBar"),
            new MethodMatcher.CallToSignature("FooBar\\Foo", "getBar"))
        );
    }

    /**
     * @see PhpElementsUtil.StringResolver#findStringValues
     */
    public void testThatPhpThatStringValueCanBeResolvedViaChainResolve() {
        assertContainsElements(PhpElementsUtil.StringResolver.findStringValues(PhpPsiElementFactory.createPhpPsiFromText(getProject(), StringLiteralExpression.class, "<?php foo('test.html');")), "test.html");
        assertContainsElements(PhpElementsUtil.StringResolver.findStringValues(PhpPsiElementFactory.createPhpPsiFromText(getProject(), BinaryExpression.class, "<?php foo($var ?? 'test.html');")), "test.html");
        assertContainsElements(PhpElementsUtil.StringResolver.findStringValues(PhpPsiElementFactory.createPhpPsiFromText(getProject(), ParameterList.class, "<?php $var = 'test.html'; foo($var);").getFirstPsiChild()), "test.html");
        assertContainsElements(PhpElementsUtil.StringResolver.findStringValues(PhpPsiElementFactory.createPhpPsiFromText(getProject(), TernaryExpression.class, "<?php $var = 'test.html'; $x = true == true ? $var : 'test2.html';")), "test.html", "test2.html");
    }

    public void testGetImplementedMethodsForRecursiveClassHierarchy() {
        myFixture.addFileToProject("First.php", "<?php class First extends Second { public function method() {} }");
        myFixture.addFileToProject("Second.php", "<?php class Second extends First { public function method() {} }");

        var firstClass = PhpIndex.getInstance(getProject()).getClassByName("First");
        var secondClass = PhpIndex.getInstance(getProject()).getClassByName("Second");

        var actualResult = PhpElementsUtil.getImplementedMethods(
            secondClass.findOwnMethodByName("method")
        );

        assertSize(2, actualResult);
        assertEquals(secondClass.findOwnMethodByName("method"), actualResult[0]);
        assertEquals(firstClass.findOwnMethodByName("method"), actualResult[1]);
    }

    public void testGetMethodParameterListStringPattern() {
        StringLiteralExpression psiElement = (StringLiteralExpression) myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "function foobar() {\n" +
            "  $var = new \\DateTime();\n" +
            "  $var->format('te<caret>st');\n" +
            "}").findElementAt(myFixture.getCaretOffset()).getParent();

        assertTrue(PhpElementsUtil.getMethodParameterListStringPattern().accepts(psiElement));

        StringLiteralExpression psiElement2 = (StringLiteralExpression) myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "function foobar() {\n" +
            "  $var = new \\DateTime();\n" +
            "  $var->format(test: 'te<caret>st');\n" +
            "}").findElementAt(myFixture.getCaretOffset()).getParent();

        assertTrue(PhpElementsUtil.getMethodParameterListStringPattern().accepts(psiElement2));

        StringLiteralExpression psiElement3 = (StringLiteralExpression) myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "function foobar() {\n" +
            "  $var = new \\DateTime();\n" +
            "  $var->format($x, $z, null, 'te<caret>st');\n" +
            "}").findElementAt(myFixture.getCaretOffset()).getParent();

        assertTrue(PhpElementsUtil.getMethodParameterListStringPattern().accepts(psiElement3));
    }
    public void testGetMethodReturnAsStrings() {
        PsiFile psiFile = myFixture.configureByFile("PhpElementsUtilReturn.php");
        PhpClass phpClass = PsiTreeUtil.collectElementsOfType(psiFile, PhpClass.class).iterator().next();

        assertContainsElements(PhpElementsUtil.getMethodReturnAsStrings(phpClass, "foo1"), "foo1_1", "foo1_2", "foo1_3", "foo1_4_const", "Foo");
        assertContainsElements(PhpElementsUtil.getMethodReturnAsStrings(phpClass, "foo2"), "foo2_1", "foo2_2", "foo2_3", "foo2_4", "foo2_x");
        assertContainsElements(PhpElementsUtil.getMethodReturnAsStrings(phpClass, "foo3"), "foo3_1", "foo3_2", "foo3_3");
    }

    public void testGetMethodWithFirstStringOrNamedArgumentPattern() {
        StringLiteralExpression psiElement = (StringLiteralExpression) myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "function foobar() {\n" +
            "  $var = new \\DateTime();\n" +
            "  $var->format('te<caret>st');\n" +
            "}").findElementAt(myFixture.getCaretOffset()).getParent();

        assertTrue(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern().accepts(psiElement));

        StringLiteralExpression psiElement2 = (StringLiteralExpression) myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "function foobar() {\n" +
            "  $var = new \\DateTime();\n" +
            "  $var->format(test: 'te<caret>st');\n" +
            "}").findElementAt(myFixture.getCaretOffset()).getParent();

        assertTrue(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern().accepts(psiElement2));

        StringLiteralExpression psiElement3 = (StringLiteralExpression) myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "function foobar() {\n" +
            "  $var = new \\DateTime();\n" +
            "  $var->format(null, 'te<caret>st');\n" +
            "}").findElementAt(myFixture.getCaretOffset()).getParent();

        assertFalse(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern().accepts(psiElement3));

        StringLiteralExpression psiElement4 = (StringLiteralExpression) myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "function foobar() {\n" +
            "  $var = new \\DateTime();\n" +
            "  $var->format(, 'te<caret>st');\n" +
            "}").findElementAt(myFixture.getCaretOffset()).getParent();

        assertFalse(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern().accepts(psiElement4));

        StringLiteralExpression psiElement5 = (StringLiteralExpression) myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "function foobar() {\n" +
            "  format('te<caret>st');\n" +
            "}").findElementAt(myFixture.getCaretOffset()).getParent();

        assertTrue(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern().accepts(psiElement5));
    }
}
