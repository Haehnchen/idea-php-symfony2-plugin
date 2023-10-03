package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.Pair;
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
import com.jetbrains.twig.elements.*;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigMacro;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigMacroTagInterface;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.*;
import java.util.stream.Collectors;

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
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/util/fixtures";
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
                "{% include 'include.html.twig' %}" +
                "{% import 'import.html.twig' %}" +
                "{% from 'from.html.twig' import foobar %}" +
                "{% import 'import.html.twig' %}" +
                "{{ include('include_function.html.twig') }}" +
                "{{ source('source_function.html.twig') }}" +
                "{% embed 'embed.html.twig' %}" +
                "{% form_theme form.foobar \":Foobar:fields_foobar.html.twig\" %}" +
                "{% form_theme form.foobar with [\":Foobar:fields_foobar_1.html.twig\", \":Foobar:fields_foobar_2.html.twig\"] %}"
        );

        TwigUtil.visitTemplateIncludes((TwigFile) fileFromText, templateInclude ->
            includes.add(templateInclude.getTemplateName())
        );

        assertContainsElements(includes, "include.html.twig", "import.html.twig", "from.html.twig", "include_function.html.twig", "source_function.html.twig", "embed.html.twig");
        assertContainsElements(includes, ":Foobar:fields.html.twig", ":Foobar:fields_foobar.html.twig");
        assertContainsElements(includes, ":Foobar:fields_foobar_1.html.twig", ":Foobar:fields_foobar_2.html.twig");
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

        Collection<String> setDeclaration = TwigUtil.getSetDeclaration(psiFile);

        assertContainsElements(setDeclaration, "foobar");
        assertContainsElements(setDeclaration, "footag");
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

        Collection<String[]> dataProvider = new ArrayList<>() {{
            add(new String[]{"fo<caret>obarAction", "foobar"});
            add(new String[]{"fo<caret>obar", "foobar"});
            add(new String[]{"editAction", "edit"});
            add(new String[]{"ed<caret>it", "edit"});
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

            assertContainsElements(strings, "FooBundle:foobar:" + string[1] + ".html.twig");
            assertContainsElements(strings, "FooBundle:foobar:" + string[1] + ".json.twig");
            assertContainsElements(strings, "FooBundle:foobar:" + string[1] + ".xml.twig");
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

        assertContainsElements(strings, "FooBundle::foobar.html.twig");
        assertContainsElements(strings, "FooBundle::foobar.json.twig");
        assertContainsElements(strings, "FooBundle::foobar.xml.twig");
        assertContainsElements(strings, "FooBundle::foobar.text.twig");

        assertContainsElements(strings, "FooBundle::Foobar.html.twig");
        assertContainsElements(strings, "FooBundle::Foobar.json.twig");
        assertContainsElements(strings, "FooBundle::Foobar.xml.twig");
        assertContainsElements(strings, "FooBundle::Foobar.text.twig");
    }

    public void testGetControllerMethodShortcutForInvokeWithSnakeCase() {
        myFixture.copyFileToProject("controller_method.php");

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace FooBundle\\Controller;\n" +
            "class FooBarController\n" +
            "{\n" +
            "   public function __in<caret>voke() {}\n" +
            "" +
            "}\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        List<String> strings = Arrays.asList(TwigUtil.getControllerMethodShortcut((Method) psiElement.getParent()));

        assertContainsElements(strings, "FooBundle::foo_bar.html.twig");
        assertContainsElements(strings, "FooBundle::foo_bar.json.twig");
        assertContainsElements(strings, "FooBundle::foo_bar.xml.twig");
        assertContainsElements(strings, "FooBundle::foo_bar.text.twig");

        assertContainsElements(strings, "FooBundle::FooBar.html.twig");
        assertContainsElements(strings, "FooBundle::FooBar.json.twig");
        assertContainsElements(strings, "FooBundle::FooBar.xml.twig");
        assertContainsElements(strings, "FooBundle::FooBar.text.twig");
    }

    public void testGetControllerMethodShortcutForInvokeForGlobalNamespace() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace FoobarUnknownBundle\\Controller;\n" +
            "class FooBarController\n" +
            "{\n" +
            "   public function foo<caret>Action() {}\n" +
            "" +
            "}\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        List<String> strings = Arrays.asList(TwigUtil.getControllerMethodShortcut((Method) psiElement.getParent()));

        assertContainsElements(strings, "foo_bar/foo.html.twig");
        assertContainsElements(strings, "foo_bar/foo.json.twig");
        assertContainsElements(strings, "foo_bar/foo.xml.twig");
        assertContainsElements(strings, "foo_bar/foo.text.twig");

        assertContainsElements(strings, "FooBar/foo.html.twig");
        assertContainsElements(strings, "FooBar/foo.json.twig");
        assertContainsElements(strings, "FooBar/foo.xml.twig");
        assertContainsElements(strings, "FooBar/foo.text.twig");
    }

    public void testGetControllerMethodShortcutForInvokeForGlobalNamespaceInInvoke() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace FoobarUnknownBundle\\Controller;\n" +
            "class FooBarController\n" +
            "{\n" +
            "   public function __inv<caret>oke() {}\n" +
            "" +
            "}\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        List<String> strings = Arrays.asList(TwigUtil.getControllerMethodShortcut((Method) psiElement.getParent()));

        assertContainsElements(strings, "foo_bar.html.twig");
        assertContainsElements(strings, "foo_bar.json.twig");
        assertContainsElements(strings, "foo_bar.xml.twig");
        assertContainsElements(strings, "foo_bar.text.twig");

        assertContainsElements(strings, "FooBar.html.twig");
        assertContainsElements(strings, "FooBar.json.twig");
        assertContainsElements(strings, "FooBar.xml.twig");
        assertContainsElements(strings, "FooBar.text.twig");
    }

    public void testGetControllerMethodShortcutForInvokeForGlobalNamespaceInInvokeWIthPath() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Fixture\\Controller\\MyAdmin;\n" +
            "class OutOfBundleController\n" +
            "{\n" +
            "   public function index<caret>Action() {}\n" +
            "" +
            "}\n"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        List<String> strings = Arrays.asList(TwigUtil.getControllerMethodShortcut((Method) psiElement.getParent()));

        assertContainsElements(strings, "my_admin/out_of_bundle/index.html.twig");
        assertContainsElements(strings, "MyAdmin/OutOfBundle/index.html.twig");
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
        assertContainsElements(elementMap.keySet(), "inline_foo_2");

        // if
        assertContainsElements(elementMap.keySet(), "if_foo");
        assertContainsElements(elementMap.keySet(), "if_foo_set_foo");
        assertContainsElements(elementMap.keySet(), "if_foo_or");

        // print
        assertContainsElements(elementMap.keySet(), "print_foo");
        assertContainsElements(elementMap.keySet(), "print_foo2");
        assertContainsElements(elementMap.keySet(), "my_block_print_foo");
        assertContainsElements(elementMap.keySet(), "my_block_print_foo_html");
        assertContainsElements(elementMap.keySet(), "set_foo_inner_print_foo");

        // for
        assertContainsElements(elementMap.keySet(), "for_bar");

        assertFalse(elementMap.keySet().contains("print_foo_method"));
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

    public void testGetTemplateAnnotationFiles() {
        PhpDocTag phpPsiFromText = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpDocTag.class, "/** @Template(\"foo.html.twig\") */");
        assertEquals("foo.html.twig", TwigUtil.getTemplateAnnotationFiles(phpPsiFromText).getFirst());

        phpPsiFromText = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpDocTag.class, "/** @Template(template=\"foo.html.twig\") */");
        assertEquals("foo.html.twig", TwigUtil.getTemplateAnnotationFiles(phpPsiFromText).getFirst());

        phpPsiFromText = PhpPsiElementFactory.createPhpPsiFromText(getProject(), PhpDocTag.class, "/** @Template(template=\"foo\\foo.html.twig\") */");
        assertEquals("foo/foo.html.twig", TwigUtil.getTemplateAnnotationFiles(phpPsiFromText).getFirst());
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

    public void testSingleExtends() {
        assertEquals("::base.html.twig", buildExtendsTagList("{% extends '::base.html.twig' %}").iterator().next());
        assertEquals("::base.html.twig", buildExtendsTagList("{% extends \"::base.html.twig\" %}").iterator().next());
        assertEquals("::base.html.twig", buildExtendsTagList("{%extends \"::base.html.twig\"%}").iterator().next());
    }

    public void testSingleExtendsInvalid() {
        assertSize(0, buildExtendsTagList("{% extends ~ '::base.html.twig' %}"));
        assertSize(0, buildExtendsTagList("{% extends foo ~ '::base.html.twig' %}"));
    }

    public void testSingleBlank() {
        assertSize(0, buildExtendsTagList("{% extends '' %}"));
        assertSize(0, buildExtendsTagList("{% extends \"\" %}"));
    }

    public void testConditional() {
        Collection<String> twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? \"base_ajax.html\" : \"base.html\" %}");
        assertTrue(twigExtendsValues.contains("base_ajax.html"));
        assertTrue(twigExtendsValues.contains("base.html"));

        twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? 'base_ajax.html' : 'base.html' %}");
        assertTrue(twigExtendsValues.contains("base_ajax.html"));
        assertTrue(twigExtendsValues.contains("base.html"));

        twigExtendsValues = buildExtendsTagList("{%extends request.ajax?'base_ajax.html':'base.html'%}");
        assertTrue(twigExtendsValues.contains("base_ajax.html"));
        assertTrue(twigExtendsValues.contains("base.html"));
    }

    public void testConditionalBlank() {
        assertSize(0, buildExtendsTagList("{% extends ? '' : '' %}"));
        assertSize(0, buildExtendsTagList("{% extends ? \"\" : \"\" %}"));
    }

    public void testConditionalInvalidGiveBestPossibleStrings() {
        Collection<String> twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? foo ~ \"base_ajax.html\" : \"base.html\" %}");
        assertFalse(twigExtendsValues.contains("base_ajax.html"));
        assertTrue(twigExtendsValues.contains("base.html"));

        twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? \"base_ajax.html\" ~ test : \"base.html\" %}");
        assertFalse(twigExtendsValues.contains("base_ajax.html"));
        assertTrue(twigExtendsValues.contains("base.html"));

        twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? foo ~ \"base_ajax.html\" : ~ \"base.html\" %}");
        assertFalse(twigExtendsValues.contains("base_ajax.html"));
        assertFalse(twigExtendsValues.contains("base.html"));

        twigExtendsValues = buildExtendsTagList("{% extends request.ajax ? \"base_ajax.html\" : ~ \"base.html\" %}");
        assertTrue(twigExtendsValues.contains("base_ajax.html"));
        assertFalse(twigExtendsValues.contains("base.html"));
    }

    /**
     * @see TwigUtil#getBlocksInFile
     */
    public void testBlocksInFileCollector() {
        assertEquals("foo", buildBlocks("{% block foo %}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{% block \"foo\" %}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{% block 'foo' %}").iterator().next().getName());

        assertEquals("foo", buildBlocks("{%- block foo -%}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{%- block \"foo\" -%}").iterator().next().getName());
        assertEquals("foo", buildBlocks("{%- block 'foo' -%}").iterator().next().getName());

        assertNotNull(buildBlocks("{%- block 'foo' -%}").iterator().next().getTarget());

        assertEquals("foobar_block", buildBlocks("{{ block('foobar_block') }}").iterator().next().getName());
        assertEquals("foobar_block", buildBlocks("{{ block(\"foobar_block\") }}").iterator().next().getName());
    }

    /**
     * @see TwigUtil#getIncludeTagStrings
     */
    public void testIncludeTagStrings() {

        assertEqual(getIncludeTemplates("{% include 'foo.html.twig' %}"), "foo.html.twig");
        assertSize(0, getIncludeTemplates("{% include '' %}"));
        assertEqual(getIncludeTemplates("{% include ['foo.html.twig'] %}"), "foo.html.twig");
        assertEqual(getIncludeTemplates("{% include ['foo.html.twig',''] %}"), "foo.html.twig");

        List<String> collect = getIncludeTemplates("{% include ['foo.html.twig', 'foo_1.html.twig', , ~ 'foo_2.html.twig', 'foo_3.html.twig' ~, 'foo' ~ 'foo_4.html.twig', 'end.html.twig'] %}")
            .stream()
            .sorted()
            .collect(Collectors.toList());

        assertEqual(collect, "end.html.twig", "foo.html.twig", "foo_1.html.twig");
    }

    /**
     * @see TwigUtil#getIncludeTagStrings
     */
    public void testIncludeTagStringsTernary() {
        assertEqual(getIncludeTemplates("{% include ajax ? 'include_statement_0.html.twig' : \"include_statement_1.html.twig\" %}"), "include_statement_0.html.twig", "include_statement_1.html.twig");
        assertEqual(getIncludeTemplates("{% include ajax ? 'include_statem' ~ 'aa' ~ 'ent_0.html.twig' : 'include_statement_1.html.twig' %}"), "include_statement_1.html.twig");
        assertEqual(getIncludeTemplates("{% include ajax ? 'include_statement_0.html.twig' : ~ 'include_statement_1.html.twig' %}"), "include_statement_0.html.twig");
        assertSize(0, getIncludeTemplates("{% include ajax ? 'include_statement_0.html.twig' ~ : ~ 'include_statement_1.html.twig' %}"));
    }

    /**
     * @see TwigUtil#getIncludeTagStrings
     */
    public void testIncludeTagNonAllowedTags() {
        assertSize(0, getIncludeTemplates("{% from 'foo.html.twig' %}", TwigElementTypes.IMPORT_TAG));
        assertSize(0, getIncludeTemplates("{% import 'foo.html.twig' %}", TwigElementTypes.IMPORT_TAG));
    }

    /**
     * @see TwigUtil#getTwigPathFromYamlConfig
     */
    public void testGetTwigPathFromYamlConfig() {
        String content = "twig:\n" +
            "    paths:\n" +
            "        \"%kernel.root_dir%/../src/views\": core\n" +
            "        \"%kernel.root_dir%/../src/views2\": 'core2'\n" +
            "        \"%kernel.root_dir%/../src/views3\": \"core3\"\n" +
            "        \"%kernel.root_dir%/../src/views4\": ~\n" +
            "        \"%kernel.root_dir%/../src/views5\": \n" +
            "        \"%kernel.root_dir%/..//src/views\": core8\n" +
            "        \"%kernel.root_dir%/..\\src/views\": core9\n" +
            "        \"%kernel.root_dir%/..\\src/views10\": '!core10'\n" +
            "        \"%kernel.root_dir%/..\\src/views11\": !core11\n"
            ;

        YAMLFile fileFromText = (YAMLFile) PsiFileFactory.getInstance(getProject())
            .createFileFromText("DUMMY__." + YAMLFileType.YML.getDefaultExtension(), YAMLFileType.YML, content, System.currentTimeMillis(), false);

        Collection<Pair<String, String>> map = TwigUtil.getTwigPathFromYamlConfig(fileFromText);

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core") && pair.getSecond().equals("%kernel.root_dir%/../src/views")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core2") && pair.getSecond().equals("%kernel.root_dir%/../src/views2")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core3") && pair.getSecond().equals("%kernel.root_dir%/../src/views3")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("") && pair.getSecond().equals("%kernel.root_dir%/../src/views4")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("") && pair.getSecond().equals("%kernel.root_dir%/../src/views5")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core8") && pair.getSecond().equals("%kernel.root_dir%/../src/views")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core9") && pair.getSecond().equals("%kernel.root_dir%/../src/views")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core10") && pair.getSecond().equals("%kernel.root_dir%/../src/views10")
        ));

        assertNotNull(ContainerUtil.find(map, pair ->
            pair.getFirst().equals("core11") && pair.getSecond().equals("%kernel.root_dir%/../src/views11")
        ));
    }
    /**
     * @see TwigUtil#getTwigGlobalsFromYamlConfig
     */
    public void testGetTwigGlobalsFromYamlConfig() {
        String content = "twig:\n" +
            "    globals:\n" +
            "       ga_tracking: '%ga_tracking%'\n" +
            "       user_management: '@AppBundle\\Service\\UserManagement'\n"
            ;

        YAMLFile yamlFile = (YAMLFile) PsiFileFactory.getInstance(getProject())
            .createFileFromText("DUMMY__." + YAMLFileType.YML.getDefaultExtension(), YAMLFileType.YML, content, System.currentTimeMillis(), false);

        Map<String, String> globals = TwigUtil.getTwigGlobalsFromYamlConfig(yamlFile);

        assertEquals("%ga_tracking%", globals.get("ga_tracking"));
        assertEquals("@AppBundle\\Service\\UserManagement", globals.get("user_management"));
    }

    /**
     * @see TwigUtil#getUniqueTwigTemplatesList
     */
    public void testGetUniqueTwigTemplatesList() {
        assertSize(1, TwigUtil.getUniqueTwigTemplatesList(Arrays.asList(
            new TwigPath("path/", "path"),
            new TwigPath("path\\", "path")
        )));

        assertSize(1, TwigUtil.getUniqueTwigTemplatesList(Arrays.asList(
            new TwigPath("path", "path"),
            new TwigPath("path", "path")
        )));

        assertSize(2, TwigUtil.getUniqueTwigTemplatesList(Arrays.asList(
            new TwigPath("path/a", "path"),
            new TwigPath("foobar", "path")
        )));

        assertSize(2, TwigUtil.getUniqueTwigTemplatesList(Arrays.asList(
            new TwigPath("path", "path", TwigUtil.NamespaceType.BUNDLE),
            new TwigPath("path", "path")
        )));
    }

    /**
     * @see TwigUtil#getMatchingRouteNameOnParameter
     */
    public void testGetMatchingRouteNameOnParameter() {
        assertEquals(
            "foo",
            TwigUtil.getMatchingRouteNameOnParameter(findElementAt(TwigFileType.INSTANCE, "{{ path('foo', {'f<caret>o'}) }}"))
        );

        assertEquals(
            "foo",
            TwigUtil.getMatchingRouteNameOnParameter(findElementAt(TwigFileType.INSTANCE, "{{ path('foo', {'f': 'f', 'f': 'f', 'f<caret>a': 'f'}) }}"))
        );

        assertNull(
            TwigUtil.getMatchingRouteNameOnParameter(findElementAt(TwigFileType.INSTANCE, "{{ path('foo' ~ 'foo', {'f<caret>o'}) }}"))
        );
    }

    /**
     * @see TwigUtil#normalizeTemplateName
     */
    public void testNormalizeTemplateName() {
        assertEquals("BarBundle:Foo/steps:step_finish.html.twig", TwigUtil.normalizeTemplateName("BarBundle:Foo:steps/step_finish.html.twig"));
        assertEquals("BarBundle:Foo/steps:step_finish.html.twig", TwigUtil.normalizeTemplateName("BarBundle:Foo/steps:step_finish.html.twig"));
        assertEquals("BarBundle:Foo/steps:step_finish.html.twig", TwigUtil.normalizeTemplateName("BarBundle::Foo/steps/step_finish.html.twig"));
        assertEquals("@BarBundle/Foo/steps:step_finish.html.twig", TwigUtil.normalizeTemplateName("@BarBundle/Foo/steps:step_finish.html.twig"));
        assertEquals("step_finish.html.twig", TwigUtil.normalizeTemplateName("step_finish.html.twig"));

        assertEquals("BarBundle:Foo/steps:step_finish.html.twig", TwigUtil.normalizeTemplateName("BarBundle:Foo:steps\\step_finish.html.twig"));
        assertEquals("@BarBundle/Foo/steps:step_finish.html.twig", TwigUtil.normalizeTemplateName("@BarBundle\\Foo/steps:step_finish.html.twig"));
        assertEquals("@BarBundle/Foo/steps:step_finish.html.twig", TwigUtil.normalizeTemplateName("@!BarBundle\\Foo/steps:step_finish.html.twig"));
    }

    /**
     * @see TwigUtil#getDomainFromTranslationTag
     */
    public void testGetDomainFromTranslationTag() {
        assertEquals("app", TwigUtil.getDomainFromTranslationTag(
            (TwigCompositeElement) TwigElementFactory.createPsiElement(getProject(), "{% trans with {'%name%': 'Fabien'} from \"app\" %}", TwigElementTypes.TAG)
        ));

        assertEquals("app", TwigUtil.getDomainFromTranslationTag(
            (TwigCompositeElement) TwigElementFactory.createPsiElement(getProject(), "{% transchoice count with {'%name%': 'Fabien'} from 'app' %}", TwigElementTypes.TAG)
        ));
    }

    public void testGetMacros() {
        PsiFile psiFile = myFixture.configureByText(TwigFileType.INSTANCE, "" +
            "{% macro input(name, value, type, size) %}{% endmacro %}\n" +
            "{% macro foobar %}{% endmacro %}\n" +
            "{% if foobar %}{% macro foobar_if %}{% endmacro %}{% endif %}"
        );

        Collection<TwigMacroTagInterface> macros = TwigUtil.getMacros(psiFile);

        assertEquals(
            "(name, value, type, size)",
            macros.stream().filter(twigMacroTag -> "input".equals(twigMacroTag.name())).findFirst().get().parameters()
        );

        assertNull(
            macros.stream().filter(twigMacroTag -> "foobar".equals(twigMacroTag.name())).findFirst().get().parameters()
        );

        assertNull(
            macros.stream().filter(twigMacroTag -> "foobar_if".equals(twigMacroTag.name())).findFirst().get().parameters()
        );
    }

    public void testGetBlocksForFile() {
        PsiFile psiFile = myFixture.configureByText("foo.html.twig", "{% block name %}{% endblock %}");
        PsiFile psiFile2 = myFixture.configureByText("foo_2.html.twig", "{% block foobar %}{% endblock %}");
        myFixture.configureByText("foo_3.html.twig", "{% block foobar_2 %}{% endblock %}");

        Collection<String> blocks = new HashSet<>();
        TwigUtil.getBlockNamesForFiles(getProject(), Collections.singletonList(psiFile.getVirtualFile())).values()
            .forEach(blocks::addAll);

        assertContainsElements(blocks, "name");
        assertDoesntContain(blocks, "foobar");

        Collection<String> blocks2 = new HashSet<>();
        TwigUtil.getBlockNamesForFiles(getProject(), Arrays.asList(psiFile.getVirtualFile(), psiFile2.getVirtualFile())).values()
            .forEach(blocks2::addAll);

        assertContainsElements(blocks2, "name");
        assertContainsElements(blocks2, "foobar");
        assertDoesntContain(blocks, "foobar_2");
    }

    public void testGetTwigFunctionParameterIdentifierPsi() {
        PsiElement psiElement = TwigElementFactory.createPsiElement(getProject(), "{{ form(form.name) }}", TwigElementTypes.FUNCTION_CALL);
        PsiElement twigFunctionParameter = TwigUtil.getTwigFunctionParameterIdentifierPsi(psiElement);

        assertEquals("form", twigFunctionParameter.getText());
        assertEquals(TwigTokenTypes.IDENTIFIER, twigFunctionParameter.getNode().getElementType());

        psiElement = TwigElementFactory.createPsiElement(getProject(), "{{ form_start(\n     form, {attr: {'novalidate': 'novalidate'}}) }}", TwigElementTypes.FUNCTION_CALL);
        twigFunctionParameter = TwigUtil.getTwigFunctionParameterIdentifierPsi(psiElement);

        assertEquals("form", twigFunctionParameter.getText());
        assertEquals(TwigTokenTypes.IDENTIFIER, twigFunctionParameter.getNode().getElementType());
    }

    public void testGetBlockLookupElements() {
        PsiFile psiFile = myFixture.configureByText("foo.html.twig", "{% block name %}{% endblock %}");
        PsiFile psiFile2 = myFixture.configureByText("foo_2.html.twig", "{% block foobar %}{% endblock %}");
        myFixture.configureByText("foo_3.html.twig", "{% block foobar_2 %}{% endblock %}");

        Collection<LookupElement> lookupElements = TwigUtil.getBlockLookupElements(getProject(), Arrays.asList(psiFile.getVirtualFile(), psiFile2.getVirtualFile()));
        assertSize(2, lookupElements);

        assertNotNull(lookupElements.stream().filter(lookupElement -> "name".equals(lookupElement.getLookupString())).findFirst().orElse(null));
        assertNotNull(lookupElements.stream().filter(lookupElement -> "foobar".equals(lookupElement.getLookupString())).findFirst().orElse(null));
    }

    public void testGetBlockExtendsScopeInsideEmbed() {
        PsiElement insideEmbed = TwigElementFactory.createPsiElement(getProject(), "" +
            "{% block body %}\n" +
            "    {% embed \"test.html.twig\" %}\n" +
            "        {% block my_foobar %}" +
            "           {% macro foo %}{% endmacro %}\n" +
            "        {% endblock %}\n" +
            "    {% endembed %}\n" +
            "{% endblock body %}\n", TwigElementTypes.MACRO_TAG);

        TwigCompositeElement blockExtendsScope = (TwigCompositeElement) TwigUtil.getBlockExtendsScope(insideEmbed);
        assertTrue(blockExtendsScope.getText().startsWith("{% embed"));
    }

    public void testGetBlockExtendsScopeInsideComponent() {
        PsiElement insideEmbed = TwigElementFactory.createPsiElement(getProject(), "" +
            "{% block body %}\n" +
            "    {% component DataTable with {headers: ['key', 'value'], data: [[1, 2], [3, 4]]} %}\n" +
            "        {% block my_foobar %}" +
            "           {% macro foo %}{% endmacro %}\n" +
            "        {% endblock %}\n" +
            "    {% endcomponent %}\n" +
            "{% endblock body %}\n", TwigElementTypes.MACRO_TAG);

        TwigCompositeElement blockExtendsScope = (TwigCompositeElement) TwigUtil.getBlockExtendsScope(insideEmbed);
        assertTrue(blockExtendsScope.getText().startsWith("{% component"));
    }

    public void testFindScopedFile() {
        PsiElement insideEmbed = TwigElementFactory.createPsiElement(getProject(), "" +
            "{% block body %}\n" +
            "    {% component DataTable with {headers: ['key', 'value'], data: [[1, 2], [3, 4]]} %}\n" +
            "        {% block my_foobar %}" +
            "           {% macro foo %}{% endmacro %}\n" +
            "        {% endblock %}\n" +
            "    {% endcomponent %}\n" +
            "{% endblock body %}\n", TwigElementTypes.MACRO_TAG);

        TwigUtil.findScopedFile(insideEmbed);
    }

    private void assertEqual(Collection<String> c, String... values) {
        if(!StringUtils.join(c, ",").equals(StringUtils.join(Arrays.asList(values), ","))) {
            fail(String.format("Fail that '%s' is equal '%s'", StringUtils.join(c, ","), StringUtils.join(Arrays.asList(values), ",")));
        }
    }

    private Collection<String> buildExtendsTagList(String string) {
        return TwigUtil.getTwigExtendsTagTemplates((TwigExtendsTag) TwigElementFactory.createPsiElement(getProject(), string, TwigElementTypes.EXTENDS_TAG));
    }

    private Collection<TwigBlock> buildBlocks(String content) {
        return TwigUtil.getBlocksInFile((TwigFile) PsiFileFactory.getInstance(getProject()).createFileFromText("DUMMY__." + TwigFileType.INSTANCE.getDefaultExtension(), TwigFileType.INSTANCE, content, System.currentTimeMillis(), false));
    }

    private Collection<String> getIncludeTemplates(@NotNull String content) {
        return getIncludeTemplates(content, TwigElementTypes.INCLUDE_TAG);
    }

    private Collection<String> getIncludeTemplates(@NotNull String content, @NotNull final IElementType type) {
        return TwigUtil.getIncludeTagStrings((TwigTagWithFileReference) TwigElementFactory.createPsiElement(getProject(), content, type));
    }
}
