package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementFactory;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

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
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#isValidTemplateString
     */
    public void testIsValidTemplateString() {
        assertFalse(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include \"foo/#{segment.typeKey}.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include \"foo/#{1 + 2}.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include ~ \"foo.html.twig\" ~ %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include \"foo.html.twig\" ~ %}", TwigElementTypes.INCLUDE_TAG)));
        assertFalse(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include ~ \"foo.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));

        assertTrue(TwigUtil.isValidTemplateString(createPsiElementAndFindString("{% include \"foo.html.twig\" %}", TwigElementTypes.INCLUDE_TAG)));
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
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getInjectedTwigElement
     */
    public void testGetTransDefaultDomainOnInjectedElement() {
        PsiFile psiFile = myFixture.configureByText("foo.html.twig", "" +
            "{% trans_default_domain \"foo\" %}\n" +
            "<a href=\"#\">FOO<caret>BAR</a>"
        );

        assertTrue(
            TwigUtil.getInjectedTwigElement(psiFile, myFixture.getCaretOffset()).getContainingFile().getFileType() == TwigFileType.INSTANCE
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getTransDefaultDomainOnScopeOrInjectedElement
     */
    public void testGetTransDefaultDomainOnScopeOrInjectedElement() {
        PsiFile psiFile = myFixture.configureByText("foo.html.twig", "" +
            "{% trans_default_domain \"foo\" %}\n" +
            "<a href=\"#\">FOO<caret>BAR</a>"
        );

        assertEquals("foo", TwigUtil.getTransDefaultDomainOnScopeOrInjectedElement(psiFile, myFixture.getCaretOffset()));

        psiFile = myFixture.configureByText("foo.html.twig", "" +
            "{% trans_default_domain \"foo\" %}\n" +
            "{% embed 'default/e.html.twig' %}\n" +
            "  {% trans_default_domain \"foobar\" %}\n" +
            "  <ht<caret>ml>\n" +
            "{% endembed %}\n"
        );

        assertEquals("foobar", TwigUtil.getTransDefaultDomainOnScopeOrInjectedElement(psiFile, myFixture.getCaretOffset()));
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil#getInjectedTwigElement
     */
    public void testGetTransDefaultDomainOnInjectedElementWithInvalidOversizesCaretOffset() {
        PsiFile psiFile = myFixture.configureByText("foo.html.twig", "<a href=\"#\">FOO<caret>BAR</a>");

        assertTrue(
            TwigUtil.getInjectedTwigElement(psiFile, 3).getContainingFile().getFileType() == TwigFileType.INSTANCE
        );

        assertNull(TwigUtil.getInjectedTwigElement(psiFile, 300000));
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

        PsiFile fileFromText = PsiFileFactory.getInstance(getProject()).createFileFromText(TwigLanguage.INSTANCE, "" +
            "{% form_theme form ':Foobar:fields.html.twig' %}" +
            "{% form_theme form.foobar \":Foobar:fields_foobar.html.twig\" %}" +
            "{% form_theme form.foobar with [\":Foobar:fields_foobar_1.html.twig\"] %}"
        );

        TwigUtil.visitTemplateIncludes((TwigFile) fileFromText, templateInclude ->
            includes.add(templateInclude.getTemplateName())
        );

        assertContainsElements(includes, ":Foobar:fields.html.twig", ":Foobar:fields_foobar.html.twig", ":Foobar:fields_foobar_1.html.twig");
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
