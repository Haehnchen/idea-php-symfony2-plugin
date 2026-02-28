package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigLineMarkerProvider;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer
 */
public class TwigLineMarkerProviderRendererTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testRendersTwigComponentTagUsageWithStaticLabel() {
        PsiFile file = myFixture.configureByText(TwigFileType.INSTANCE, "<twig:Alert />");

        XmlTag xmlTag = null;
        for (PsiFile psiFile : file.getViewProvider().getAllFiles()) {
            xmlTag = PsiTreeUtil.findChildOfType(psiFile, XmlTag.class);
            if (xmlTag != null) {
                break;
            }
        }

        assertNotNull(xmlTag);

        TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer renderer = new TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer();
        assertEquals("Alert", renderer.getElementText(xmlTag));
    }

    public void testRendersComponentFunctionUsageWithStaticLabel() {
        PsiFile file = myFixture.configureByText(TwigFileType.INSTANCE, "{{ component('Card') }}");
        PsiElement stringElement = java.util.Arrays.stream(PsiTreeUtil.collectElements(file, TwigPattern.getComponentPattern()::accepts))
            .findFirst()
            .orElse(null);
        assertNotNull(stringElement);

        TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer renderer = new TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer();
        assertEquals("Card", renderer.getElementText(stringElement));
    }

    public void testRendersComponentTagBlockUsageWithStaticLabel() {
        PsiFile file = myFixture.configureByText(TwigFileType.INSTANCE, "{% component Banner with {type: 'success'} %}{% endcomponent %}");
        PsiElement componentElement = java.util.Arrays.stream(PsiTreeUtil.collectElements(file, psiElement ->
                psiElement.getNode() != null &&
                    psiElement.getNode().getElementType() == TwigTokenTypes.IDENTIFIER &&
                    "Banner".equals(psiElement.getText())
            ))
            .findFirst()
            .orElse(null);

        assertNotNull(componentElement);

        TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer renderer = new TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer();
        assertEquals("Banner", renderer.getElementText(componentElement));
    }

    public void testRendersNestedComponentNameWithTagSyntax() {
        PsiFile file = myFixture.configureByText(TwigFileType.INSTANCE, "<twig:Alert:Html:Foo_Bar_1 />");

        XmlTag xmlTag = null;
        for (PsiFile psiFile : file.getViewProvider().getAllFiles()) {
            xmlTag = PsiTreeUtil.findChildOfType(psiFile, XmlTag.class);
            if (xmlTag != null) {
                break;
            }
        }

        assertNotNull(xmlTag);

        TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer renderer = new TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer();
        assertEquals("Alert:Html:Foo_Bar_1", renderer.getElementText(xmlTag));
    }

    public void testRendersNestedComponentNameWithFunctionSyntax() {
        PsiFile file = myFixture.configureByText(TwigFileType.INSTANCE, "{{ component('Alert:Html:Foo_Bar_1') }}");
        PsiElement stringElement = java.util.Arrays.stream(PsiTreeUtil.collectElements(file, TwigPattern.getComponentPattern()::accepts))
            .findFirst()
            .orElse(null);
        assertNotNull(stringElement);

        TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer renderer = new TwigLineMarkerProvider.UxComponentTargetsPsiElementListCellRenderer();
        assertEquals("Alert:Html:Foo_Bar_1", renderer.getElementText(stringElement));
    }
}
