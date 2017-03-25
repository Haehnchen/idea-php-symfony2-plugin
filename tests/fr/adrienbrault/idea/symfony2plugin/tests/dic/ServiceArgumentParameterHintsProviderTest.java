package fr.adrienbrault.idea.symfony2plugin.tests.dic;

import com.intellij.codeInsight.hints.InlayInfo;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceArgumentParameterHintsProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see ServiceArgumentParameterHintsProvider
 */
public class ServiceArgumentParameterHintsProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ServiceArgumentParameterHintsProvider.php");
        myFixture.copyFileToProject("ServiceArgumentParameterHintsProvider.xml");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testXmlParameterTypeHintForIdAttribute() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service class=\"Foobar\\MyFoobar\">\n" +
            "           <argument type=\"service\" id=\"a<caret>a\">\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    public void testXmlParameterTypeHintForXmlText() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service class=\"Foobar\\MyFoobar\">\n" +
            "           <argument>%a<caret>a%</argument>\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    public void testXmlParameterTypeHintWithoutTypeHintMustFallbackToParameterName() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service class=\"Foobar\\MyFoobar\">\n" +
            "           <argument>a</argument>\n" +
            "           <argument>%a<caret>a%</argument>\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "foobar".equals(inlayInfo.getText())));
    }

    public void testXmlParameterTypeForNestedArgumentsMustNotProvideHint() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service class=\"Foobar\\MyFoobar\">\n" +
            "           <argument type=\"collection\">\n" +
            "               <caret>\n" +
            "               <argument/>" +
            "           </argument>\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertSize(0, parameterHints);
    }

    public void testXmlParameterTypeForId() {
        List<InlayInfo> parameterHints = getInlayInfo("" +
            "<container>\n" +
            "    <services>\n" +
            "        <service id=\"my_<caret>foo\" alias=\"foo_alias_hint\"/>\n" +
            "    </services>\n" +
            "</container>\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "MyFoobar".equals(inlayInfo.getText())));
    }
    public void testYamlParameterTypeForId() {
        List<InlayInfo> parameterHints = getInlayInfo(YAMLFileType.YML,"" +
            "services:\n" +
            "    foobar:\n" +
            "        class: Foobar\\MyFoobar\n" +
            "        arguments: [@fo<caret>obar]\n"
        );

        assertNotNull(ContainerUtil.find(parameterHints, inlayInfo -> "FooInterface".equals(inlayInfo.getText())));
    }

    @NotNull
    private List<InlayInfo> getInlayInfo(@NotNull FileType fileType, @NotNull String content) {
        myFixture.configureByText(fileType, content);

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertNotNull(psiElement);

        PsiElement parent = psiElement.getParent();

        return new ServiceArgumentParameterHintsProvider().getParameterHints(parent);
    }

    @NotNull
    private List<InlayInfo> getInlayInfo(@NotNull String content) {
        return getInlayInfo(XmlFileType.INSTANCE, content);
    }
}
