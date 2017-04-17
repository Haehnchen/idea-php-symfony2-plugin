package fr.adrienbrault.idea.symfony2plugin.tests.config.xml;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Parameter;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper
 */
public class XmlHelperTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("XmlHelper.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see XmlHelper#visitServiceCallArgumentMethodIndex
     */
    public void testVisitServiceCallArgument() {
        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<service class=\"Foo\\Bar\">\n" +
            "   <call method=\"setBar\">\n" +
            "      <argument/>\n" +
            "      <argument type=\"service\" id=\"ma<caret>iler\" />\n" +
            "   </call>\n" +
            "</service>"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PsiElement parent = psiElement.getParent();

        Collection<String> results = new ArrayList<>();

        XmlHelper.visitServiceCallArgument((XmlAttributeValue) parent, visitor ->
            results.add(visitor.getClassName() + ":" + visitor.getMethod() + ":" + visitor.getParameterIndex())
        );

        assertContainsElements(results, "Foo\\Bar:setBar:1");
    }

    /**
     * @see XmlHelper#visitServiceCallArgumentMethodIndex
     */
    public void testVisitServiceCallArgumentParameter() {
        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<service class=\"Foo\\Bar\">\n" +
            "   <call method=\"setBar\">\n" +
            "      <argument/>\n" +
            "      <argument type=\"service\" id=\"ma<caret>iler\" />\n" +
            "   </call>\n" +
            "</service>"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PsiElement parent = psiElement.getParent();

        Collection<Parameter> results = new ArrayList<>();

        XmlHelper.visitServiceCallArgumentMethodIndex((XmlAttributeValue) parent, results::add);

        assertNotNull(
            ContainerUtil.find(results, parameter -> "arg2".equals(parameter.getName()))
        );
    }
}
