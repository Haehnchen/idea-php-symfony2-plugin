package fr.adrienbrault.idea.symfony2plugin.tests.templating.usages;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.util.TextRange;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigTemplateUsageReference;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.ArrayList;
import java.util.Collection;
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
}
