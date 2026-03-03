package fr.adrienbrault.idea.symfony2plugin.tests.templating.usages;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.PsiManager;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.PsiElementUsageTarget;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigExtendsTag;
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigTemplateUsageTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateUsageTypeProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

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

    public void testClassifiesAllSupportedTemplateUsages() {
        addFixturesForAllUsageTypes();

        PsiFile targetFile = getTargetTwigFile("templates/base.html.twig");
        Collection<PsiReference> references = ReferencesSearch.search(targetFile, GlobalSearchScope.projectScope(getProject())).findAll();
        UsageTarget[] targets = {new TestUsageTarget(targetFile)};
        TwigTemplateUsageTypeProvider typeProvider = new TwigTemplateUsageTypeProvider();

        Map<String, String> expectedBySourceFile = new LinkedHashMap<>();
        expectedBySourceFile.put("templates/child_extends.html.twig", "extends");
        expectedBySourceFile.put("templates/include_tag.html.twig", "include");
        expectedBySourceFile.put("templates/include_array_tag.html.twig", "include");
        expectedBySourceFile.put("templates/include_function.html.twig", "include");
        expectedBySourceFile.put("templates/source_function.html.twig", "include");
        expectedBySourceFile.put("templates/embed_tag.html.twig", "embed");
        expectedBySourceFile.put("templates/import_tag.html.twig", "import");
        expectedBySourceFile.put("templates/from_tag.html.twig", "from");
        expectedBySourceFile.put("templates/form_theme_tag.html.twig", "form_theme");
        expectedBySourceFile.put("src/Controller/HomeController.php", "controller");

        for (Map.Entry<String, String> entry : expectedBySourceFile.entrySet()) {
            PsiReference reference = findReferenceBySourceFile(references, entry.getKey());
            assertNotNull("Expected reference from file: " + entry.getKey(), reference);

            UsageType usageType = typeProvider.getUsageType(reference.getElement(), targets);
            assertNotNull("Usage type should be detected for: " + entry.getKey(), usageType);
            assertEquals("Wrong usage type for " + entry.getKey(), entry.getValue(), usageType.toString());
        }

        // Also verify the extends STRING_TEXT node directly — this simulates what IntelliJ's built-in
        // Twig reference provider passes to the type provider (a leaf string, not the TwigExtendsTag).
        PsiElement extendsStringEl = getExtendsStringElement("templates/child_extends.html.twig");
        assertNotNull("Expected STRING_TEXT inside extends tag", extendsStringEl);
        UsageType extendsTypeViaPsi = typeProvider.getUsageType(extendsStringEl, targets);
        assertNotNull("extends STRING_TEXT must not be Unclassified", extendsTypeViaPsi);
        assertEquals("extends", extendsTypeViaPsi.toString());
    }

    public void testClassifiesConditionalTernaryExtendsAsExtends() {
        // {% extends condition ? 'a.html.twig' : 'b.html.twig' %} — both branches must be classified as "extends",
        // not fall through to "Unclassified".
        myFixture.addFileToProject("templates/base.html.twig", "base");
        myFixture.addFileToProject("templates/bundles/TwigBundle/Exception/error.html.twig", "error");
        myFixture.addFileToProject("templates/child_extends_conditional.html.twig",
            "{% extends is_granted('ROLE_MEMBER') ? 'base.html.twig' : 'bundles/TwigBundle/Exception/error.html.twig' %}"
        );

        TwigTemplateUsageTypeProvider typeProvider = new TwigTemplateUsageTypeProvider();

        // Verify via PSI directly — simulates what the built-in Twig reference provider passes
        // (a STRING_TEXT leaf, not the TwigExtendsTag wrapper).
        PsiFile conditionalFile = PsiManager.getInstance(getProject())
            .findFile(myFixture.findFileInTempDir("templates/child_extends_conditional.html.twig"));
        assertNotNull(conditionalFile);

        PsiFile baseTwigFile = getTargetTwigFile("templates/base.html.twig");
        UsageTarget[] baseTargets = {new TestUsageTarget(baseTwigFile)};

        PsiFile errorTwigFile = getTargetTwigFile("templates/bundles/TwigBundle/Exception/error.html.twig");
        UsageTarget[] errorTargets = {new TestUsageTarget(errorTwigFile)};

        // "then" branch: base.html.twig STRING_TEXT node
        PsiElement baseStringEl = findExtendsStringElement(conditionalFile, "base.html.twig");
        assertNotNull("Expected STRING_TEXT 'base.html.twig' inside conditional extends tag", baseStringEl);
        UsageType baseType = typeProvider.getUsageType(baseStringEl, baseTargets);
        assertNotNull("Conditional ternary extends must not be Unclassified for 'base.html.twig'", baseType);
        assertEquals("extends", baseType.toString());

        // "else" branch: bundles/TwigBundle/Exception/error.html.twig STRING_TEXT node
        PsiElement errorStringEl = findExtendsStringElement(conditionalFile, "bundles/TwigBundle/Exception/error.html.twig");
        assertNotNull("Expected STRING_TEXT 'bundles/.../error.html.twig' inside conditional extends tag", errorStringEl);
        UsageType errorType = typeProvider.getUsageType(errorStringEl, errorTargets);
        assertNotNull("Conditional ternary extends must not be Unclassified for 'error.html.twig'", errorType);
        assertEquals("extends", errorType.toString());
    }

    public void testClassifiesComponentUsagesForComponentTemplateTarget() {
        addFixturesForAllUsageTypes();

        PsiFile targetFile = getTargetTwigFile("templates/components/Alert.html.twig");
        Collection<PsiReference> references = ReferencesSearch.search(targetFile, GlobalSearchScope.projectScope(getProject())).findAll();
        UsageTarget[] targets = {new TestUsageTarget(targetFile)};
        TwigTemplateUsageTypeProvider typeProvider = new TwigTemplateUsageTypeProvider();

        Map<String, String> expectedBySourceFile = new LinkedHashMap<>();
        expectedBySourceFile.put("templates/component_usage_function.html.twig", "component");
        expectedBySourceFile.put("templates/component_usage_html_tag.html.twig", "component");
        expectedBySourceFile.put("templates/component_usage_html_tag_with_body.html.twig", "component");
        expectedBySourceFile.put("templates/component_usage_twig_tag.html.twig", "component");

        for (Map.Entry<String, String> entry : expectedBySourceFile.entrySet()) {
            PsiReference reference = findReferenceBySourceFile(references, entry.getKey());
            assertNotNull("Expected reference from file: " + entry.getKey(), reference);

            UsageType usageType = typeProvider.getUsageType(reference.getElement(), targets);
            assertNotNull("Usage type should be detected for: " + entry.getKey(), usageType);
            assertEquals("Wrong usage type for " + entry.getKey(), entry.getValue(), usageType.toString());
        }
    }

    private void addFixturesForAllUsageTypes() {
        myFixture.addFileToProject("templates/base.html.twig", "base");
        myFixture.addFileToProject("templates/child_extends.html.twig", "{% extends 'base.html.twig' %}");
        myFixture.addFileToProject("templates/include_tag.html.twig", "{% include 'base.html.twig' %}");
        myFixture.addFileToProject("templates/include_array_tag.html.twig", "{% include ['base.html.twig', 'fallback.html.twig'] %}");
        myFixture.addFileToProject("templates/include_function.html.twig", "{{ include('base.html.twig') }}");
        myFixture.addFileToProject("templates/source_function.html.twig", "{{ source('base.html.twig') }}");
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

    private PsiFile getTargetTwigFile(String templatePath) {
        PsiFile targetFile = PsiManager.getInstance(getProject()).findFile(myFixture.findFileInTempDir(templatePath));
        assertNotNull(targetFile);
        assertTrue(targetFile instanceof TwigFile);
        return targetFile;
    }

    /**
     * Returns the first STRING_TEXT leaf inside the TwigExtendsTag of the given file.
     * Simulates the element that IntelliJ's built-in Twig reference provider passes to the type provider.
     */
    private PsiElement getExtendsStringElement(String templatePath) {
        PsiFile file = PsiManager.getInstance(getProject()).findFile(myFixture.findFileInTempDir(templatePath));
        assertNotNull(file);
        TwigExtendsTag extendsTag = PsiTreeUtil.findChildOfType(file, TwigExtendsTag.class);
        if (extendsTag == null) return null;
        for (PsiElement leaf : PsiTreeUtil.collectElements(extendsTag, e ->
                e.getFirstChild() == null &&
                e.getNode() != null &&
                e.getNode().getElementType() == TwigTokenTypes.STRING_TEXT)) {
            return leaf;
        }
        return null;
    }

    /**
     * Returns the STRING_TEXT leaf with the given text inside the TwigExtendsTag of the given file.
     * Used to find a specific ternary branch template name string.
     */
    private PsiElement findExtendsStringElement(PsiFile file, String text) {
        TwigExtendsTag extendsTag = PsiTreeUtil.findChildOfType(file, TwigExtendsTag.class);
        if (extendsTag == null) return null;
        for (PsiElement leaf : PsiTreeUtil.collectElements(extendsTag, e ->
                e.getFirstChild() == null &&
                e.getNode() != null &&
                e.getNode().getElementType() == TwigTokenTypes.STRING_TEXT &&
                text.equals(e.getText()))) {
            return leaf;
        }
        return null;
    }

    private PsiReference findReferenceBySourceFile(Collection<PsiReference> references, String relativePath) {
        for (PsiReference reference : references) {
            PsiFile sourceFile = reference.getElement().getContainingFile();
            if (sourceFile.getVirtualFile() != null && sourceFile.getVirtualFile().getPath().endsWith(relativePath)) {
                return reference;
            }
        }

        return null;
    }

    private static class TestUsageTarget implements PsiElementUsageTarget {
        private final PsiElement element;

        private TestUsageTarget(PsiElement element) {
            this.element = element;
        }

        @Override
        public PsiElement getElement() {
            return element;
        }

        @Override
        public boolean isValid() {
            return element.isValid();
        }

        @Override
        public void findUsages() {
        }

        @Override
        public void findUsagesInEditor(FileEditor editor) {
        }

        @Override
        public void highlightUsages(PsiFile file, com.intellij.openapi.editor.Editor editor, boolean clearHighlights) {
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public VirtualFile[] getFiles() {
            return VirtualFile.EMPTY_ARRAY;
        }

        @Override
        public void update() {
        }

        @Override
        public String getName() {
            return element.getText();
        }

        @Override
        public ItemPresentation getPresentation() {
            return null;
        }

        @Override
        public void navigate(boolean requestFocus) {
        }

        @Override
        public boolean canNavigate() {
            return false;
        }

        @Override
        public boolean canNavigateToSource() {
            return false;
        }
    }
}
