package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementFactory;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigMacro;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigSet;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class TwigUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        createDummyFiles(
            "app/Resources/TwigUtilIntegrationBundle/views/layout.html.twig",
            "app/Resources/TwigUtilIntegrationBundle/views/Foo/layout.html.twig",
            "app/Resources/TwigUtilIntegrationBundle/views/Foo/Bar/layout.html.twig"
        );
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTemplateNameByOverwrite
     */
    public void testTemplateOverwriteNameGeneration() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertEquals(
            "TwigUtilIntegrationBundle:layout.html.twig",
            TwigUtil.getTemplateNameByOverwrite(getProject(), VfsUtil.findRelativeFile(getProject().getBaseDir(), "app/Resources/TwigUtilIntegrationBundle/views/layout.html.twig".split("/")))
        );

        assertEquals(
            "TwigUtilIntegrationBundle:Foo/layout.html.twig",
            TwigUtil.getTemplateNameByOverwrite(getProject(), VfsUtil.findRelativeFile(getProject().getBaseDir(), "app/Resources/TwigUtilIntegrationBundle/views/Foo/layout.html.twig".split("/")))
        );

        assertEquals(
            "TwigUtilIntegrationBundle:Foo/Bar/layout.html.twig",
            TwigUtil.getTemplateNameByOverwrite(getProject(), VfsUtil.findRelativeFile(getProject().getBaseDir(), "app/Resources/TwigUtilIntegrationBundle/views/Foo/Bar/layout.html.twig".split("/")))
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTemplateNameByOverwrite
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTemplateName
     */
    public void testTemplateOverwriteNavigation() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends '<caret>TwigUtilIntegrationBundle:layout.html.twig' %}", "/views/layout.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends '<caret>TwigUtilIntegrationBundle:Foo/layout.html.twig' %}", "/views/Foo/layout.html.twig");
        assertNavigationContainsFile(TwigFileType.INSTANCE, "{% extends '<caret>TwigUtilIntegrationBundle:Foo/Bar/layout.html.twig' %}", "/views/Foo/Bar/layout.html.twig");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#isValidStringWithoutInterpolatedOrConcat
     */
    public void testIsValidTemplateString() {
        assertFalse(TwigUtil.isValidStringWithoutInterpolatedOrConcat(createPsiElementAndFindString("{% include \"foo/#{segment.typeKey}.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidStringWithoutInterpolatedOrConcat(createPsiElementAndFindString("{% include \"foo/#{1 + 2}.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidStringWithoutInterpolatedOrConcat(createPsiElementAndFindString("{% include ~ \"foo.html.twig\" ~ %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidStringWithoutInterpolatedOrConcat(createPsiElementAndFindString("{% include \"foo.html.twig\" ~ %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidStringWithoutInterpolatedOrConcat(createPsiElementAndFindString("{% include ~ \"foo.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));

        assertTrue(TwigUtil.isValidStringWithoutInterpolatedOrConcat(createPsiElementAndFindString("{% include \"foo.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));
    }
    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getDomainTrans
     */
    public void testGetDomainTrans() {
        String[] blocks = {
            "{{ '<caret>'|transchoice(3, {}, 'foo') }}",
            "{{ '<caret>'|transchoice(3, [], 'foo') }}",
            "{{ '<caret>'|trans({}, 'foo') }}",
            "{{ '<caret>'   |   trans(    {}    ,   'foo'    ) }}",
            "{{ '<caret>'|trans([], 'foo') }}",
            "{{ '<caret>'|trans(, 'foo') }}",
            "{{ '<caret>'|trans(null, 'foo') }}",
            "{{ '<caret>'|trans({'foo': 'foo', 'foo'}, 'foo') }}",
            "{{ '<caret>' | transchoice(count, {'%var%': value}, 'foo') }}",
            "{{ '<caret>' | transchoice(c, {'%var%': value}, 'foo') }}",
            "{{ '<caret>' | transchoice(, {'%var%': value}, 'foo') }}",
            "{{ '<caret>' | transchoice(foo.bar, {'%var%': value}, 'foo') }}",
            "{{ '<caret>' | transchoice(foo.bar ~ bar, {'%var%': value}, 'foo') }}",
            "{{ '<caret>' | transchoice(foo.bar + bar, {'%var%': value}, 'foo') }}",
            "{{ '<caret>' | transchoice(foo.bar - bar, {'%var%': value}, 'foo') }}",
            "{{ '<caret>'|trans({'%some%': \"button.reserve\"|trans,}, 'foo') }}",
            "{{ 'bar'|trans({'%some%': \"<caret>\"|trans({}, 'foo'),}) }}",
        };

        for (String s : blocks) {
            myFixture.configureByText(TwigFileType.INSTANCE, s);

            PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
            assertNotNull(element);

            assertEquals("foo", TwigUtil.getDomainTrans(element));
        }
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getCreateAbleTemplatePaths
     */
    public void testGetCreateAbleTemplatePaths() {
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.copyFileToProject("dummy.html.twig", "res/dummy.html.twig");
        myFixture.copyFileToProject("dummy.html.twig", "res/foo/dummy.html.twig");

        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "@foo/bar.html.twig"), "src/res/bar.html.twig");
        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "bar.html.twig"), "src/res/bar.html.twig");

        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "FooBundle:Bar:dummy.html.twig"), "src/res/Bar/dummy.html.twig");
        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "FooBundle:Bar\\Foo:dummy.html.twig"), "src/res/Bar/Foo/dummy.html.twig");
        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "FooBundle:Bar:Foo\\dummy.html.twig"), "src/res/Bar/Foo/dummy.html.twig");

        assertContainsElements(TwigUtil.getCreateAbleTemplatePaths(getProject(), "@FooBundle/Bar/dummy.html.twig"), "src/res/Bar/dummy.html.twig");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTransDefaultDomainOnScope
     */
    public void testGetTwigFileTransDefaultDomainForFileScope() {
        PsiFile psiFile = myFixture.configureByText("foo.html.twig", "{% trans_default_domain \"foo\" %}{{ <caret> }}");
        PsiElement psiElement = psiFile.findElementAt(myFixture.getCaretOffset());

        assertNotNull(psiElement);
        assertEquals("foo", TwigUtil.getTransDefaultDomainOnScope(psiElement));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTransDefaultDomainOnScope
     */
    public void testGetTwigFileTransDefaultDomainForEmbedScope() {
        PsiFile psiFile = myFixture.configureByText("foo.html.twig", "" +
            "{% trans_default_domain \"foo\" %}\n" +
            "{% embed 'default/e.html.twig' %}\n" +
            "  {% trans_default_domain \"foobar\" %}\n" +
            "  {{ <caret> }}\n" +
            "{% endembed %}\n"
        );

        PsiElement psiElement = psiFile.findElementAt(myFixture.getCaretOffset());

        assertNotNull(psiElement);
        assertEquals("foobar", TwigUtil.getTransDefaultDomainOnScope(psiElement));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTransDefaultDomainOnScope
     */
    public void testGetTwigFileTransDefaultDomainForEmbedScopeInEmbedTag() {
        PsiFile psiFile = myFixture.configureByText("foo.html.twig", "" +
            "{% trans_default_domain \"foo\" %}\n" +
            "{% embed 'default/e.html.twig' with { foo: '<caret>'|trans } %}\n" +
            "  {% trans_default_domain \"foobar\" %}\n" +
            "{% endembed %}\n"
        );

        PsiElement psiElement = psiFile.findElementAt(myFixture.getCaretOffset());

        assertNotNull(psiElement);
        assertEquals("foo", TwigUtil.getTransDefaultDomainOnScope(psiElement));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTransDefaultDomainOnScope
     */
    public void testGetTwigFileTransDefaultDomainForEmbedScopeInEmbedTagWithNotMatch() {
        PsiFile psiFile = myFixture.configureByText("foo.html.twig", "" +
            "{% embed 'default/e.html.twig' with {foo: '<caret>'|trans } %}\n" +
            "  {% trans_default_domain \"foobar\" %}\n" +
            "{% endembed %}\n"
        );

        PsiElement psiElement = psiFile.findElementAt(myFixture.getCaretOffset());

        assertNotNull(psiElement);
        assertNull(TwigUtil.getTransDefaultDomainOnScope(psiElement));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getElementOnTwigViewProvider
     */
    public void testGetTransDefaultDomainOnInjectedElement() {
        myFixture.configureByText("foo.html.twig", "" +
            "{% trans_default_domain \"foo\" %}\n" +
            "<a href=\"#\">FOO<caret>BAR</a>"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        assertTrue(
            TwigUtil.getElementOnTwigViewProvider(psiElement).getContainingFile().getFileType() == TwigFileType.INSTANCE
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTransDefaultDomainOnScopeOrInjectedElement
     */
    public void testGetTransDefaultDomainOnScopeOrInjectedElement() {
        myFixture.configureByText("foo.html.twig", "" +
            "{% trans_default_domain \"foo\" %}\n" +
            "<a href=\"#\">FOO<caret>BAR</a>"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertEquals("foo", TwigUtil.getTransDefaultDomainOnScopeOrInjectedElement(psiElement));

        myFixture.configureByText("foo.html.twig", "" +
            "{% trans_default_domain \"foo\" %}\n" +
            "{% embed 'default/e.html.twig' %}\n" +
            "  {% trans_default_domain \"foobar\" %}\n" +
            "  <ht<caret>ml>\n" +
            "{% endembed %}\n"
        );

        psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertEquals("foobar", TwigUtil.getTransDefaultDomainOnScopeOrInjectedElement(psiElement));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getElementOnTwigViewProvider
     */
    public void testGetTransDefaultDomainOnInjectedElementWithInvalidOversizesCaretOffset() {
        myFixture.configureByText("foo.html.twig", "<a href=\"#\">FOO<caret>BAR</a>");
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        assertTrue(
            TwigUtil.getElementOnTwigViewProvider(psiElement).getContainingFile().getFileType() == TwigFileType.INSTANCE
        );

        myFixture.configureByText("test.php", "<?php<caret> ");
        psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        assertNull(TwigUtil.getElementOnTwigViewProvider(psiElement));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTwigFileMethodUsageOnIndex
     */
    public void testGetTwigFileMethodUsageOnIndex() {
        myFixture.copyFileToProject("GetTwigFileMethodUsageOnIndex.php");
        Set<Function> methods = TwigUtil.getTwigFileMethodUsageOnIndex(getProject(), Collections.singletonList("car.html.twig"));

        assertNotNull(ContainerUtil.find(methods, method -> method.getFQN().equals("\\Template\\Bar\\MyTemplate.fooAction")));
        assertNotNull(ContainerUtil.find(methods, method -> method.getFQN().equals("\\foo")));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getFoldingTemplateName
     */
    public void testGetFoldingTemplateName() {
        assertEquals("Foo:edit", TwigUtil.getFoldingTemplateName("FooBundle:edit.html.twig"));
        assertEquals("Foo:edit", TwigUtil.getFoldingTemplateName("FooBundle:edit.html.php"));
        assertEquals("Bundle:", TwigUtil.getFoldingTemplateName("Bundle:.html.twig"));
        assertEquals("edit", TwigUtil.getFoldingTemplateName("edit.html.twig"));
        assertNull(TwigUtil.getFoldingTemplateName("FooBundle:edit.foo.twig"));
        assertNull(TwigUtil.getFoldingTemplateName(""));
        assertNull(TwigUtil.getFoldingTemplateName(null));
    }


    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#visitTemplateIncludes
     */
    public void testVisitTemplateIncludes() {
        Collection<String> includes = new ArrayList<>();

        PsiFile fileFromText = PsiFileFactory.getInstance(getProject()).createFileFromText(TwigLanguage.INSTANCE,
            "{% form_theme form ':Foobar:fields.html.twig' %}" +
            "{% form_theme form.foobar \":Foobar:fields_foobar.html.twig\" %}" +
            "{% form_theme form.foobar with [\":Foobar:fields_foobar_1.html.twig\"] %}"
        );

        TwigUtil.visitTemplateIncludes((TwigFile) fileFromText, templateInclude ->
            includes.add(templateInclude.getTemplateName())
        );

        assertContainsElements(includes, ":Foobar:fields.html.twig", ":Foobar:fields_foobar.html.twig", ":Foobar:fields_foobar_1.html.twig");
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getImportedMacros
     */
    public void testGetImportedMacros() {
        PsiFile psiFile = PsiFileFactory.getInstance(getProject()).createFileFromText(TwigLanguage.INSTANCE,
            "{% macro foobar %}{% endmacro %}\n" +
            "{% from _self import foobar as input, foobar %}\n" +
            "{% from 'foobar.html.twig' import foobar_twig %}\n" +
            "{% from \"foobar2.html.twig\" import foobar_twig_2 %}"
        );

        Collection<TwigMacro> importedMacros = TwigUtil.getImportedMacros(psiFile);

        assertTrue(importedMacros.stream().anyMatch(twigMacro ->
            "input".equals(twigMacro.getName()) && "foobar".equals(twigMacro.getOriginalName()))
        );

        assertTrue(importedMacros.stream().anyMatch(twigMacro ->
            "foobar".equals(twigMacro.getName()) && "_self".equals(twigMacro.getTemplate()))
        );

        assertTrue(importedMacros.stream().anyMatch(twigMacro ->
            "foobar_twig".equals(twigMacro.getName()) && "foobar.html.twig".equals(twigMacro.getTemplate()))
        );

        assertTrue(importedMacros.stream().anyMatch(twigMacro ->
            "foobar_twig_2".equals(twigMacro.getName()) && "foobar2.html.twig".equals(twigMacro.getTemplate()))
        );
    }

    public void testGetImportedMacrosTargets() {
        PsiFile psiFile = PsiFileFactory.getInstance(getProject()).createFileFromText(TwigLanguage.INSTANCE,
            "{% macro foobar %}{% endmacro %}\n" +
            "{% from _self import foobar as input, foobar %}\n"
        );

        assertTrue(TwigUtil.getImportedMacros(psiFile, "foobar").stream().anyMatch(psiElement ->
            psiElement.getNode().getElementType() == TwigElementTypes.MACRO_TAG
        ));

        assertTrue(TwigUtil.getImportedMacros(psiFile, "input").stream().anyMatch(psiElement ->
            psiElement.getNode().getElementType() == TwigElementTypes.MACRO_TAG
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getImportedMacrosNamespaces
     */
    public void testGetImportedMacrosNamespaces() {
        PsiFile psiFile = PsiFileFactory.getInstance(getProject()).createFileFromText(TwigLanguage.INSTANCE,
            "{% import _self as macros %}\n" +
            "{% macro foobar %}{% endmacro %}\n"
        );

        assertTrue(
            TwigUtil.getImportedMacrosNamespaces(psiFile).stream().anyMatch(twigMacro -> "macros.foobar".equals(twigMacro.getName()))
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getSetDeclaration
     */
    public void testGetSetDeclaration() {
        PsiFile psiFile = PsiFileFactory.getInstance(getProject()).createFileFromText(TwigLanguage.INSTANCE,
            "{% set foobar = 'foo' %}\n" +
            "{% set footag %}{% endset %}\n"
        );

        Collection<TwigSet> setDeclaration = TwigUtil.getSetDeclaration(psiFile);

        assertTrue(
            setDeclaration.stream().anyMatch(twigMacro -> "foobar".equals(twigMacro.getName()))
        );

        assertTrue(
            setDeclaration.stream().anyMatch(twigMacro -> "footag".equals(twigMacro.getName()))
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getImportedMacrosNamespaces
     */
    public void testGetImportedMacrosNamespacesTargets() {
        PsiFile psiFile = PsiFileFactory.getInstance(getProject()).createFileFromText(TwigLanguage.INSTANCE,
            "{% macro my_foobar %}{% endmacro %}\n" +
            "{% import _self as foobar %}\n"
        );

        assertTrue(TwigUtil.getImportedMacrosNamespaces(psiFile, "foobar.my_foobar").stream().anyMatch(
            psiElement -> psiElement.getNode().getElementType() == TwigElementTypes.MACRO_TAG
        ));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTwigMacroNameAndParameter
     */
    public void testGetTwigMacroNameAndParameter() {
        PsiElement psiElement = TwigElementFactory.createPsiElement(
            getProject(),
            "{% macro foo(foobar, foo, bar) %}{% endmacro %}",
            TwigElementTypes.MACRO_TAG
        );

        Pair<String, String> parameter = TwigUtil.getTwigMacroNameAndParameter(psiElement);

        assertNotNull(parameter);
        assertEquals("foo", parameter.getFirst());
        assertEquals("(foobar, foo, bar)", parameter.getSecond());
    }

    public void testGetControllerMethodShortcut() {
        myFixture.copyFileToProject("controller_method.php");

        Collection<String[]> dataProvider = new ArrayList<String[]>() {{
            add(new String[] {"fo<caret>obarAction", "foobar"});
            add(new String[] {"fo<caret>obar", "foobar"});
            add(new String[] {"editAction", "edit"});
            add(new String[] {"ed<caret>it", "edit"});
        }};

        for (String[] string : dataProvider) {
            myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace FooBundle\\Controller;\n" +
                "class FoobarController\n" +
                "{\n" +
                "   public function " + string[0] + "() {}\n" +
                "" +
                "}\n"
            );

            PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

            List<String> strings = Arrays.asList(TwigUtil.getControllerMethodShortcut((Method) psiElement.getParent()));

            assertContainsElements(strings, "FooBundle:Foobar:" + string[1] + ".html.twig");
            assertContainsElements(strings, "FooBundle:Foobar:" + string[1] + ".json.twig");
            assertContainsElements(strings, "FooBundle:Foobar:" + string[1] + ".xml.twig");
        }
    }

    public void testGetControllerMethodShortcutForInvoke() {
        myFixture.copyFileToProject("controller_method.php");

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace FooBundle\\Controller;\n" +
            "class FoobarController\n" +
            "{\n" +
            "   public function __in<caret>voke() {}\n" +
            "" +
            "}\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        List<String> strings = Arrays.asList(TwigUtil.getControllerMethodShortcut((Method) psiElement.getParent()));

        assertContainsElements(strings, "FooBundle::Foobar.html.twig");
        assertContainsElements(strings, "FooBundle::Foobar.json.twig");
        assertContainsElements(strings, "FooBundle::Foobar.xml.twig");
    }

    public void testFindTwigFileController() {
        myFixture.copyFileToProject("bundle.php");

        VirtualFile bar = myFixture.copyFileToProject("dummy.html.twig", "Resources/views/Foobar/Bar.html.twig");
        assertEquals(
            "barAction",
            TwigUtil.findTwigFileController((TwigFile) PsiManager.getInstance(getProject()).findFile(bar)).iterator().next().getName()
        );

        VirtualFile json = myFixture.copyFileToProject("dummy.html.twig", "Resources/views/Foobar/Bar.json.twig");
        assertEquals(
            "barAction",
            TwigUtil.findTwigFileController((TwigFile) PsiManager.getInstance(getProject()).findFile(json)).iterator().next().getName()
        );

        VirtualFile foobar = myFixture.copyFileToProject("dummy.html.twig", "Resources/views/Foobar.html.twig");
        assertEquals(
            "__invoke",
            TwigUtil.findTwigFileController((TwigFile) PsiManager.getInstance(getProject()).findFile(foobar)).iterator().next().getName()
        );
    }

    public void testGetTwigFileDomainScope() {
        myFixture.configureByText(
            "t.html.twig",
            "{% trans_default_domain 'foobar' %} {{ 'f'|trans({}, 'bar') }} {{ 'f'|trans({}, 'bar') }} {{ 'f'|trans({}, 'bar2') }}"
        );

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        TwigUtil.DomainScope twigFileDomainScope = TwigUtil.getTwigFileDomainScope(element);

        assertEquals("foobar", twigFileDomainScope.getDefaultDomain());
        assertEquals("bar", twigFileDomainScope.getDomain());
    }

    public void testVisitTemplateVariables() {
        PsiFile psiFile = myFixture.configureByFile("variables.html.twig");

        Map<String, PsiElement> elementMap = new HashMap<>();

        TwigUtil.visitTemplateVariables((TwigFile) psiFile, pair ->
            elementMap.put(pair.getFirst(), pair.getSecond())
        );

        // set declaration
        assertContainsElements(elementMap.keySet(), "set_foo");
        assertContainsElements(elementMap.keySet(), "my_block_set_foo");

        // file doc
        assertContainsElements(elementMap.keySet(), "inline_foo");

        // if
        assertContainsElements(elementMap.keySet(), "if_foo");
        assertContainsElements(elementMap.keySet(), "if_foo_set_foo");
        assertContainsElements(elementMap.keySet(), "if_foo_or");

        // print
        assertContainsElements(elementMap.keySet(), "print_foo");
        assertContainsElements(elementMap.keySet(), "my_block_print_foo");
        assertContainsElements(elementMap.keySet(), "my_block_print_foo_html");
        assertContainsElements(elementMap.keySet(), "set_foo_inner_print_foo");

        // for
        assertContainsElements(elementMap.keySet(), "for_bar");
    }

    public void testGetTemplateAnnotationFilesWithSiblingMethod() {
        PhpDocTag phpDocTag = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpDocTag.class, "<?php\n" +
            "class Foo\n" +
            "{\n" +
            "   /**" +
            "   * @Template(\"foo.html.twig\")" +
            "   */" +
            "   function fooAction()\n" +
            "   {\n" +
            "   }\n" +
            "}\n"
        );

        assertContainsElements(TwigUtil.getTemplateAnnotationFilesWithSiblingMethod(phpDocTag).keySet(), "foo.html.twig");
    }

    private PsiElement createPsiElementAndFindString(@NotNull String content, @NotNull IElementType type) {
        PsiElement psiElement = TwigElementFactory.createPsiElement(getProject(), content, type);
        if(psiElement == null) {
            fail();
        }

        final PsiElement[] string = {null};
        psiElement.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (string[0] == null && element.getNode().getElementType() == TwigTokenTypes.STRING_TEXT) {
                    string[0] = element;
                }
                super.visitElement(element);
            }
        });

        return string[0];
    }

}
