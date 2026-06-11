package fr.adrienbrault.idea.symfony2plugin.tests.templating.usages;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.util.TextRange;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigComponentDefinition;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigComponentProvider;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigComponentProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigTemplateUsageReference;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.tests.templating.TestTwigFileUsage;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateReferencesSearchExecutorTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        myFixture.addFileToProject("config/packages/twig_component.yaml",
            "twig_component:\n" +
                "    defaults:\n" +
                "        App\\\\Twig\\\\Components\\\\: 'components/'\n"
        );
        myFixture.addFileToProject("ide-twig.json",
            "{\"namespaces\":[{\"namespace\":\"\",\"path\":\"templates\"}]}"
        );
    }

    public void testFindsAllSupportedTemplateUsages() {
        addFixturesForAllUsageTypes();

        Collection<TwigTemplateUsageReference> references = getTwigUsageReferences("templates/base.html.twig");

        assertContainsSourceFile(references, "templates/child_extends.html.twig");
        assertContainsSourceFile(references, "templates/include_tag.html.twig");
        assertContainsSourceFile(references, "templates/include_array_tag.html.twig");
        assertContainsSourceFile(references, "templates/include_function.html.twig");
        assertContainsSourceFile(references, "templates/source_function.html.twig");
        assertContainsSourceFile(references, "templates/block_function.html.twig");
        assertContainsSourceFile(references, "templates/embed_tag.html.twig");
        assertContainsSourceFile(references, "templates/import_tag.html.twig");
        assertContainsSourceFile(references, "templates/from_tag.html.twig");
        assertContainsSourceFile(references, "templates/form_theme_tag.html.twig");
        assertContainsSourceFile(references, "src/Controller/HomeController.php");
    }

    public void testFindsComponentUsagesForComponentTemplateTarget() {
        addFixturesForAllUsageTypes();

        Collection<TwigTemplateUsageReference> references = getTwigUsageReferences("templates/components/Alert.html.twig");

        assertContainsSourceFile(references, "templates/component_usage_function.html.twig");
        assertContainsSourceFile(references, "templates/component_usage_html_tag.html.twig");
        assertContainsSourceFile(references, "templates/component_usage_html_tag_with_body.html.twig");
        assertContainsSourceFile(references, "templates/component_usage_twig_tag.html.twig");
    }

    public void testFindsComponentUsagesForProviderBackedTemplateWithoutTwigTemplateName() {
        PsiFile templateFile = myFixture.addFileToProject(
            "external/package/templates/components/Button/Primary.html.twig",
            "<button></button>"
        );

        UxUtil.TWIG_COMPONENT_PROVIDERS.getPoint().registerExtension(
            new TestTwigComponentProvider(new TwigComponentDefinition(
                "ExternalPackage:Button:Primary",
                templateFile.getVirtualFile(),
                null
            )),
            getTestRootDisposable()
        );

        myFixture.addFileToProject("templates/provider_component_usage.html.twig", "{{ component('ExternalPackage:Button:Primary') }}");
        myFixture.addFileToProject("templates/provider_component_tag_usage.html.twig", "<twig:ExternalPackage:Button:Primary />");

        Collection<TwigTemplateUsageReference> references = getTwigUsageReferences(
            "external/package/templates/components/Button/Primary.html.twig"
        );

        assertContainsSourceFile(references, "templates/provider_component_usage.html.twig");
        assertContainsSourceFile(references, "templates/provider_component_tag_usage.html.twig");
    }

    public void testFindsCustomTwigFileUsageExtensionUsages() {
        TwigUtil.TWIG_FILE_USAGE_EXTENSIONS.getPoint().registerExtension(new TestTwigFileUsage(), getTestRootDisposable());
        addCustomUsageFixtures();

        Collection<TwigTemplateUsageReference> references = getTwigUsageReferences("templates/base.html.twig");

        assertContainsSourceFile(references, "templates/custom_extends.html.twig");
        assertContainsSourceFile(references, "templates/custom_include.html.twig");
        assertContainsSourceFile(references, "templates/custom_embed.html.twig");
        assertContainsSourceFile(references, "templates/custom_import.html.twig");
        assertContainsSourceFile(references, "templates/custom_from.html.twig");
        assertContainsSourceFile(references, "templates/custom_source.html.twig");
    }

    public void testFindsCustomExtensionUsagesForEveryTemplateAliasInSameSourceFile() {
        TwigUtil.TWIG_FILE_USAGE_EXTENSIONS.getPoint().registerExtension(new TestTwigFileUsage(), getTestRootDisposable());
        Settings.getInstance(getProject()).twigNamespaces.add(new TwigNamespaceSetting("Alias", "templates", true, TwigUtil.NamespaceType.ADD_PATH, true));

        myFixture.addFileToProject("templates/base.html.twig", "base");
        myFixture.addFileToProject("templates/custom_multi_alias.html.twig",
            "{% custom_include 'base.html.twig' %}\n" +
            "{% custom_include '@Alias/base.html.twig' %}"
        );

        Collection<TwigTemplateUsageReference> references = getTwigUsageReferences("templates/base.html.twig");

        assertEquals(2, countReferencesBySourceFile(references, "templates/custom_multi_alias.html.twig"));
    }

    public void testComponentHtmlTagReferenceRangeIsOnlyTagName() {
        addFixturesForAllUsageTypes();

        Collection<TwigTemplateUsageReference> references = getTwigUsageReferences("templates/components/Alert.html.twig");
        TwigTemplateUsageReference reference = findReferenceBySourceFile(references, "templates/component_usage_html_tag_with_body.html.twig");
        assertNotNull(reference);

        TextRange range = reference.getRangeInElement();
        String selected = range.substring(reference.getElement().getText());

        assertFalse("Must not select full tag including body", range.equals(new TextRange(0, reference.getElement().getTextLength())));
        assertEquals("twig:Alert", selected);
    }

    private void addFixturesForAllUsageTypes() {
        myFixture.addFileToProject("templates/base.html.twig", "base");
        myFixture.addFileToProject("templates/child_extends.html.twig", "{% extends 'base.html.twig' %}");
        myFixture.addFileToProject("templates/include_tag.html.twig", "{% include 'base.html.twig' %}");
        myFixture.addFileToProject("templates/include_array_tag.html.twig", "{% include ['base.html.twig', 'fallback.html.twig'] %}");
        myFixture.addFileToProject("templates/include_function.html.twig", "{{ include('base.html.twig') }}");
        myFixture.addFileToProject("templates/source_function.html.twig", "{{ source('base.html.twig') }}");
        myFixture.addFileToProject("templates/block_function.html.twig", "{{ block('title', 'base.html.twig') }}");
        myFixture.addFileToProject("templates/embed_tag.html.twig", "{% embed 'base.html.twig' %}{% endembed %}");
        myFixture.addFileToProject("templates/import_tag.html.twig", "{% import 'base.html.twig' as macros %}");
        myFixture.addFileToProject("templates/from_tag.html.twig", "{% from 'base.html.twig' import foo as bar %}");
        myFixture.addFileToProject("templates/form_theme_tag.html.twig", "{% form_theme form with 'base.html.twig' %}");

        myFixture.addFileToProject("src/Controller/HomeController.php",
            "<?php\nnamespace App\\Controller;\n" +
                "class HomeController {\n" +
                "    public function index() { $this->render('base.html.twig'); }\n" +
                "}\n"
        );

        myFixture.addFileToProject("src/Twig/Components/Alert.php",
            "<?php\nnamespace App\\Twig\\Components;\n" +
                "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
                "#[AsTwigComponent('Alert')]\n" +
                "class Alert {}\n"
        );
        myFixture.addFileToProject("templates/components/Alert.html.twig", "<div>Alert</div>");
        myFixture.addFileToProject("templates/component_usage_function.html.twig", "{{ component('Alert') }}");
        myFixture.addFileToProject("templates/component_usage_html_tag.html.twig", "<twig:Alert />");
        myFixture.addFileToProject("templates/component_usage_html_tag_with_body.html.twig", "<twig:Alert><div>inner</div></twig:Alert>");
        myFixture.addFileToProject("templates/component_usage_twig_tag.html.twig", "{% component Alert %}{% endcomponent %}");
    }

    private void addCustomUsageFixtures() {
        myFixture.addFileToProject("templates/base.html.twig", "base");
        myFixture.addFileToProject("templates/custom_extends.html.twig", "{% custom_extends 'base.html.twig' %}");
        myFixture.addFileToProject("templates/custom_include.html.twig", "{% custom_include 'base.html.twig' %}");
        myFixture.addFileToProject("templates/custom_embed.html.twig", "{% custom_embed 'base.html.twig' %}{% end_custom_embed %}");
        myFixture.addFileToProject("templates/custom_import.html.twig", "{% custom_import 'base.html.twig' %}");
        myFixture.addFileToProject("templates/custom_from.html.twig", "{% custom_from 'base.html.twig' %}");
        myFixture.addFileToProject("templates/custom_source.html.twig", "{% custom_source 'base.html.twig' %}");
    }

    private Collection<TwigTemplateUsageReference> getTwigUsageReferences(String templatePath) {
        PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(myFixture.findFileInTempDir(templatePath));
        assertNotNull(psiFile);
        assertTrue(psiFile instanceof TwigFile);

        Collection<PsiReference> references = ReferencesSearch.search(psiFile, GlobalSearchScope.projectScope(getProject())).findAll();

        List<TwigTemplateUsageReference> usageReferences = new ArrayList<>();
        for (PsiReference reference : references) {
            if (reference instanceof TwigTemplateUsageReference usageReference) {
                usageReferences.add(usageReference);
            }
        }

        return usageReferences;
    }

    private void assertContainsSourceFile(Collection<TwigTemplateUsageReference> references, String relativePath) {
        TwigTemplateUsageReference sourceFile = findReferenceBySourceFile(references, relativePath);
        if (sourceFile != null) {
            return;
        }

        fail("Expected reference from file: " + relativePath);
    }

    private TwigTemplateUsageReference findReferenceBySourceFile(Collection<TwigTemplateUsageReference> references, String relativePath) {
        for (TwigTemplateUsageReference reference : references) {
            PsiFile sourceFile = reference.getElement().getContainingFile();
            if (sourceFile.getVirtualFile() != null && sourceFile.getVirtualFile().getPath().endsWith(relativePath)) {
                return reference;
            }
        }

        return null;
    }

    private long countReferencesBySourceFile(Collection<TwigTemplateUsageReference> references, String relativePath) {
        return references.stream()
            .filter(reference -> {
                PsiFile sourceFile = reference.getElement().getContainingFile();
                return sourceFile.getVirtualFile() != null && sourceFile.getVirtualFile().getPath().endsWith(relativePath);
            })
            .count();
    }

    private static class TestTwigComponentProvider implements TwigComponentProvider {
        private final TwigComponentDefinition definition;

        private TestTwigComponentProvider(TwigComponentDefinition definition) {
            this.definition = definition;
        }

        @Override
        public Collection<TwigComponentDefinition> getComponents(TwigComponentProviderParameter parameter) {
            return Collections.singletonList(this.definition);
        }
    }
}
