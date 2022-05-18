package fr.adrienbrault.idea.symfony2plugin.tests.config.xml;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/xml/fixtures";
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

    /**
     * @see XmlHelper#getServiceDefinitionClass
     */
    public void testGetServiceDefinitionClass() {
        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<service class=\"Foo\\Bar\">\n" +
            "      <tag type=\"service\" method=\"ma<caret>iler\" />\n" +
            "</service>"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertEquals("Foo\\Bar", XmlHelper.getServiceDefinitionClass(psiElement));

        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<service id=\"Foo\\Bar\">\n" +
            "      <tag type=\"service\" method=\"ma<caret>iler\" />\n" +
            "</service>"
        );

        psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertEquals("Foo\\Bar", XmlHelper.getServiceDefinitionClass(psiElement));
    }

    /**
     * @see XmlHelper#getArgumentIndex
     */
    public void testGetArgumentIndex() {
        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<service class=\"Foo\\Bar\">\n" +
            "      <argum<caret>ent key=\"$foobar1\" />\n" +
            "</service>"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertEquals(1, XmlHelper.getArgumentIndex((XmlTag) psiElement.getParent()));
    }

    /**
     * @see XmlHelper#getArgumentIndex
     */
    public void testGetArgumentIndexOnIndex() {
        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<service class=\"Foo\\Bar\">\n" +
            "      <argum<caret>ent index=\"2\" />\n" +
            "</service>"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertEquals(2, XmlHelper.getArgumentIndex((XmlTag) psiElement.getParent()));

        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<service class=\"Foo\\Bar\">\n" +
            "      <argum<caret>ent index=\"foobar\" />\n" +
            "</service>"
        );

        psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertEquals(-1, XmlHelper.getArgumentIndex((XmlTag) psiElement.getParent()));
    }

    /**
     * @see XmlHelper#getArgumentIndex
     */
    public void testGetArgumentIndexCallOnNamedArgument() {
        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<service class=\"Foo\\Bar\">\n" +
            "   <call method=\"setBar\">\n" +
            "       <arg<caret>ument type=\"service\" key=\"$arg2\" id=\"args_bar\"/>\n" +
            "   </call>\n" +
            "</service>"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertEquals(1, XmlHelper.getArgumentIndex((XmlTag) psiElement.getParent()));
    }

    /**
     * @see XmlHelper#getArgumentIndex
     */
    public void testGetArgumentIndexOnArgumentCount() {
        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<service class=\"Foo\\Bar\">\n" +
            "      <argument/>\n" +
            "      <argument index=\"\"/>\n" +
            "      <argument key=\"\"/>\n" +
            "      <argum<caret>ent/>\n" +
            "</service>"
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertEquals(1, XmlHelper.getArgumentIndex((XmlTag) psiElement.getParent()));
    }

    /**
     * @see XmlHelper#getNamespaceResourcesClasses
     */
    public void testGetNamespaceResourcesClasses() {
        myFixture.copyFileToProject("XmlHelper.php", "src/XmlHelper.php");
        VirtualFile service = myFixture.copyFileToProject("services.xml", "src/services.xml");

        PsiFile file = PsiManager.getInstance(getProject()).findFile(service);

        Collection<XmlTag> xmlTags = PsiTreeUtil.collectElementsOfType(file, XmlTag.class);

        XmlTag xmlTag = xmlTags.stream().filter(xmlTag1 -> "prototype".equals(xmlTag1.getName())).findFirst().orElseThrow();
        Collection<String> namespaceResourcesClasses = XmlHelper.getNamespaceResourcesClasses(xmlTag).stream()
            .map(PhpNamedElement::getFQN)
            .collect(Collectors.toSet());

        assertContainsElements(namespaceResourcesClasses, "\\Foo\\Bar");
    }
}
