package fr.adrienbrault.idea.symfony2plugin.tests.config.xml;

import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlServiceContainerAnnotator;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.xml.XmlServiceContainerAnnotator
 */
public class XmlServiceContainerAnnotatorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("annotator.php");
        myFixture.copyFileToProject("annotator.xml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    private AnnotationHolderImpl createAnnotationHolder(@NotNull String content) {
        PsiFile psiFile = myFixture.configureByText("services.xml", content);

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        AnnotationHolderImpl holder = new AnnotationHolderImpl(new AnnotationSession(psiFile));
        new XmlServiceContainerAnnotator().annotate(psiElement.getParent(), holder);

        return holder;
    }

    public void testConstructorInstance() {
        AnnotationHolderImpl holder = createAnnotationHolder(
            "<services>" +
            "     <service class=\"Args\\Foo\">\n" +
            "         <argument type=\"service\" id=\"args<caret>_bar\"/>\n" +
            "     </service>" +
            "</services>"
        );

        assertEquals("Expect instance of: Args\\Foo", holder.get(0).getMessage());
    }

    public void testCallInstance() {
        AnnotationHolderImpl holder = createAnnotationHolder(
            "<services>" +
                "        <service class=\"Args\\Foo\">\n" +
                "            <call method=\"setFoo\">\n" +
                "                <argument type=\"service\" id=\"args<caret>_bar\"/>\n" +
                "            </call>\n" +
                "        </service>" +
                "</services>"
        );

        assertEquals("Expect instance of: Args\\Foo", holder.get(0).getMessage());

        holder = createAnnotationHolder(
            "<services>" +
                "        <service class=\"Args\\Foo\">\n" +
                "            <call method=\"setFoo\">\n" +
                "                <argument type=\"service\" id=\"args_bar<caret>\"/>\n" +
                "            </call>\n" +
                "        </service>" +
                "</services>"
        );

        assertEquals("Expect instance of: Args\\Foo", holder.get(0).getMessage());

        holder = createAnnotationHolder(
            "<services>" +
                "     <service class=\"Args\\Foo\">\n" +
                "         <call method=\"setFoo\">\n" +
                "             <argument/>\n" +
                "             <argument/>\n" +
                "             <argument type=\"service\" id=\"args_bar<caret>\"/>\n" +
                "         </call>\n" +
                "     </service>" +
                "</services>"
        );

        assertEquals("Expect instance of: Args\\Foo", holder.get(0).getMessage());
    }
}
