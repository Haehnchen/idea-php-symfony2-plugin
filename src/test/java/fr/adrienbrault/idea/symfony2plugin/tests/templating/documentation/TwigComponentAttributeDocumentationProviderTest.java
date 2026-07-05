package fr.adrienbrault.idea.symfony2plugin.tests.templating.documentation;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.documentation.TwigComponentAttributeDocumentationProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.templating.documentation.TwigComponentAttributeDocumentationProvider
 */
public class TwigComponentAttributeDocumentationProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testDocumentationForTwigComponentAttribute() {
        myFixture.copyFileToProject("PropsAlert.php", "src/PropsAlert.php");
        myFixture.copyFileToProject("twig_component.yaml", "config/packages/twig_component.yaml");
        myFixture.copyFileToProject("ide-twig.json", "ide-twig.json");
        myFixture.addFileToProject("templates/components/PropsAlert.html.twig",
            "{# @prop variant 'default'|'destructive' The visual style variant. #}\n" +
            "{%- props variant = 'default' -%}\n" +
            "<div></div>"
        );

        myFixture.configureByText(TwigFileType.INSTANCE, "<twig:PropsAlert varia<caret>nt=\"\" />");
        int offset = myFixture.getCaretOffset();

        TwigComponentAttributeDocumentationProvider provider = new TwigComponentAttributeDocumentationProvider();
        PsiElement docElement = provider.getCustomDocumentationElement(
            myFixture.getEditor(), myFixture.getFile(), myFixture.getFile().findElementAt(offset), offset
        );

        assertNotNull("Expected a documentation element for a <twig:...> attribute", docElement);

        String rawDoc = provider.generateDoc(docElement, myFixture.getFile().findElementAt(offset));
        assertNotNull(rawDoc);

        // the doc is HTML with XML-escaped values (e.g. ' -> &#39;), unescape to assert on the source form
        String doc = StringUtil.unescapeXmlEntities(rawDoc);
        assertTrue("Expected the prop type", doc.contains("'default'|'destructive'"));
        assertTrue("Expected the default value from {% props %}", doc.contains("Default: <code>'default'</code>"));
        assertTrue("Expected the description", doc.contains("The visual style variant."));
    }

    public void testNonTwigAttributeIsNotDocumented() {
        myFixture.configureByText(TwigFileType.INSTANCE, "<div variant<caret>=\"\"></div>");
        int offset = myFixture.getCaretOffset();

        TwigComponentAttributeDocumentationProvider provider = new TwigComponentAttributeDocumentationProvider();
        assertNull(provider.getCustomDocumentationElement(
            myFixture.getEditor(), myFixture.getFile(), myFixture.getFile().findElementAt(offset), offset
        ));
    }
}
