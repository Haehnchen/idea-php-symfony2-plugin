package fr.adrienbrault.idea.symfonyplugin.tests.action;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfonyplugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/*
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceActionUtilTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatAutowireForServiceBlocksInspect() {
        XmlTag xmlTag = createServiceXmlTag(
            "    <services>\n" +
            "        <serv<caret>ice autowire=\"true\"/>\n" +
            "    </services>"
        );

        assertFalse(ServiceActionUtil.isValidXmlParameterInspectionService(xmlTag));
    }

    public void testThatAliasAttributeBlocksInspect() {
        XmlTag xmlTag = createServiceXmlTag(
            "<services><serv<caret>ice id=\"Foobar\" alias=\"Test\"/></services>"
        );

        assertFalse(ServiceActionUtil.isValidXmlParameterInspectionService(xmlTag));
    }

    public void testThatDefaultValueIsOverwriteInService() {
        XmlTag xmlTag = createServiceXmlTag(
            "    <services>\n" +
                "        <defaults autowire=\"true\" />\n" +
                "        <serv<caret>ice autowire=\"false\"/>\n" +
                "    </services>"
        );

        assertTrue(ServiceActionUtil.isValidXmlParameterInspectionService(xmlTag));
    }

    public void testThatAutowireForDefaultMustNotInspectService() {
        XmlTag xmlTag = createServiceXmlTag(
            "    <services>\n" +
            "        <defaults autowire=\"true\" />\n" +
            "        <serv<caret>ice/>\n" +
            "    </services>");

        assertFalse(ServiceActionUtil.isValidXmlParameterInspectionService(xmlTag));
    }

    @NotNull
    private XmlTag createServiceXmlTag(@NotNull String content) {
        myFixture.configureByText(XmlFileType.INSTANCE, content);

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        return (XmlTag) psiElement.getParent();
    }
}
