package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.ide.highlighter.HtmlFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigHtmlLineMarkerProvider;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigHtmlLineMarkerProvider
 */
public class TwigHtmlLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.addFileToProject("AlertComponent.php", "<?php\n" +
            "namespace App\\\\Twig\\\\Components;\n" +
            "use Symfony\\\\UX\\\\TwigComponent\\\\Attribute\\\\AsTwigComponent;\n" +
            "#[AsTwigComponent('alert')]\n" +
            "class Alert {}\n").getVirtualFile());

        VirtualFile templateFile = myFixture.addFileToProject("templates/components/alert.html.twig", "{% block headline %}{% endblock %}").getVirtualFile();
        myFixture.configureFromExistingVirtualFile(templateFile);

        VirtualFile templatesDirectory = templateFile.getParent().getParent();
        Settings settings = Settings.getInstance(getProject());
        settings.twigNamespaces.clear();
        settings.twigNamespaces.add(new TwigNamespaceSetting(
            TwigUtil.MAIN,
            templatesDirectory.getPath(),
            true,
            TwigUtil.NamespaceType.ADD_PATH,
            true
        ));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/completion/fixtures";
    }

    public void testSupportsTwigExtensionOnly() {
        PsiFile twigFile = myFixture.configureByText("test.html.twig", "<div></div>");
        assertTrue(TwigHtmlLineMarkerProvider.isSupportedFile(twigFile));

        PsiFile htmlFile = myFixture.configureByText(HtmlFileType.INSTANCE, "<div></div>");
        assertFalse(TwigHtmlLineMarkerProvider.isSupportedFile(htmlFile));
    }

    public void testSkipsNonTwigHtmlFiles() {
        PsiFile htmlFile = myFixture.configureByText(
            HtmlFileType.INSTANCE,
            "<twig:Alert><twig:block name=\"headline\"></twig:block></twig:Alert>"
        );

        TwigHtmlLineMarkerProvider provider = new TwigHtmlLineMarkerProvider();
        Collection<LineMarkerInfo<?>> lineMarkerInfos = new ArrayList<>();
        provider.collectSlowLineMarkers(collectPsiElementsRecursive(htmlFile), lineMarkerInfos);

        assertTrue(lineMarkerInfos.isEmpty());
    }

    public void testAttachesLineMarkerForTwigComponentBlock() {
        PsiFile psiFile = myFixture.configureByText(
            HtmlFileType.INSTANCE,
            "<twig:alert><twig:block name=\"headline\"></twig:block></twig:alert>"
        );

        class TestProvider extends TwigHtmlLineMarkerProvider {
            @Override
            protected String resolveComponentName(@NotNull com.intellij.openapi.project.Project project, @NotNull String rawComponentName) {
                return rawComponentName;
            }

            @Override
            protected boolean hasComponentBlock(@NotNull com.intellij.openapi.project.Project project, @NotNull String componentName, @NotNull String blockName) {
                return "alert".equals(componentName) && "headline".equals(blockName);
            }

            @Override
            protected boolean isTwigBackedXmlTag(@NotNull com.intellij.psi.xml.XmlTag xmlTag) {
                return true;
            }
            @Nullable
            public LineMarkerInfo<?> create(@NotNull XmlAttributeValue value) {
                PsiElement token = value.getFirstChild() != null ? value.getFirstChild() : value;
                return attachTwigComponentBlockOverride(value, token);
            }
        }

        TestProvider provider = new TestProvider();

        XmlAttribute attribute = PsiTreeUtil.findChildrenOfType(psiFile, XmlAttribute.class)
            .stream()
            .filter(a -> "name".equals(a.getName()))
            .findFirst()
            .orElse(null);
        assertNotNull(attribute);

        XmlAttributeValue attributeValue = attribute.getValueElement();
        assertNotNull(attributeValue);

        LineMarkerInfo<?> lineMarkerInfo = provider.create(attributeValue);
        assertNotNull(lineMarkerInfo);
        assertEquals("Navigate to block", lineMarkerInfo.getLineMarkerTooltip());
    }

    @NotNull
    private static List<PsiElement> collectPsiElementsRecursive(@NotNull PsiFile psiFile) {
        List<PsiElement> elements = new ArrayList<>();

        psiFile.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                elements.add(element);
                super.visitElement(element);
            }
        });

        elements.add(psiFile);
        return elements;
    }
}
