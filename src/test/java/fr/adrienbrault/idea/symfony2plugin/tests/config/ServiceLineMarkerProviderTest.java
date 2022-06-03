package fr.adrienbrault.idea.symfony2plugin.tests.config;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.ServiceLineMarkerProvider
 */
public class ServiceLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("ServiceLineMarkerProvider.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("SymfonyPhpReferenceContributor.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/fixtures";
    }

    public void testDoctrineModelLineMarker() {

        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<doctrine-mapping>\n" +
            "    <document name=\"Foo\\Car\"/>\n" +
            "</doctrine-mapping>"
        );

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
                "namespace Foo{\n" +
                "    class Car{}\n" +
                "}"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to model"));
    }

    public void testThatDoctrineAnnotationMetadataNotProvidesSelfLineMarker() {
        assertLineMarkerIsEmpty(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
                "namespace Foo{\n" +
                "    class Bar{}\n" +
                "}"
        ));
    }

    public void testDoctrineRepositoryDefinitionLineMarker() {
        myFixture.configureByText(XmlFileType.INSTANCE, "" +
                "<doctrine-mapping>\n" +
                "    <document name=\"Foo\" repository-class=\"Entity\\Bar\"/>\n" +
                "</doctrine-mapping>"
        );

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Entity{\n" +
                "    class Bar{}\n" +
                "}"
        );

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
                "namespace Entity{\n" +
                "    class Bar{}\n" +
                "}"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to metadata"));
    }

    public void testXmlServiceLineMarker() {
        myFixture.configureByText(XmlFileType.INSTANCE,
            "<container>\n" +
            "  <services>\n" +
            "      <service class=\"Service\\Bar\" id=\"service_bar\"/>\n" +
            "  </services>\n" +
            "</container>"
        );

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
                "namespace Service{\n" +
                "    class Bar{}\n" +
                "}"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to definition"));

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
                "namespace Service{\n" +
                "    class Bar{}\n" +
                "}"
        ), new LineMarker.TargetAcceptsPattern("Navigate to definition", XmlPatterns.xmlTag().withName("service").withAttributeValue("id", "service_bar")));
    }

    public void testXmlServiceLineMarkerForClassName() {
        myFixture.configureByText(XmlFileType.INSTANCE,
            "<container>\n" +
                "  <services>\n" +
                "      <service id=\"Service\\Bar\"/>\n" +
                "  </services>\n" +
                "</container>"
        );

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
            "namespace Service{\n" +
            "    class Bar{}\n" +
            "}"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to definition"));

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
            "namespace Service{\n" +
            "    class Bar{}\n" +
            "}"
        ), new LineMarker.TargetAcceptsPattern("Navigate to definition", XmlPatterns.xmlTag().withName("service").withAttributeValue("id", "Service\\Bar")));
    }

    public void testYamlServiceLineMarker() {
        myFixture.configureByText(YAMLFileType.YML,
            "services:\n" +
                "  foo:\n" +
                "    class: Service\\YamlBar"
        );

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
            "namespace Service{\n" +
            "    class YamlBar{}\n" +
            "}"
        ), new LineMarker.TargetAcceptsPattern("Navigate to definition", PlatformPatterns.psiElement(YAMLKeyValue.class).with(new PatternCondition<YAMLKeyValue>("KeyText") {
            @Override
            public boolean accepts(@NotNull YAMLKeyValue yamlKeyValue, ProcessingContext processingContext) {
                return yamlKeyValue.getKeyText().equals("foo");
            }
        })));
    }

    public void testYamlServiceLineMarkerForClassName() {
        myFixture.configureByText(YAMLFileType.YML,
            "services:\n" +
                "  Service\\YamlBar: ~\n"
        );

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
            "namespace Service{\n" +
            "    class YamlBar{}\n" +
            "}"
        ), new LineMarker.TargetAcceptsPattern("Navigate to definition", PlatformPatterns.psiElement(YAMLKeyValue.class).with(new PatternCondition<YAMLKeyValue>("KeyText") {
            @Override
            public boolean accepts(@NotNull YAMLKeyValue yamlKeyValue, ProcessingContext processingContext) {
                return yamlKeyValue.getKeyText().equals("Service\\YamlBar");
            }
        })));
    }

    public void testConstraintAndValidateClassLineMarker() {
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("Validation.php"));

        assertLineMarker(myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Foo\\Validation {\n" +
                "    class Bar extends \\Symfony\\Component\\Validator\\Constraint {}\n" +
                "}"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to validator"));

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Foo\\Validation\n" +
                "{\n" +
                "    class Bar{}\n" +
                "}"
        );

        assertLineMarker(myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace Foo\\Validation {\n" +
                "    class BarValidator implements \\Symfony\\Component\\Validator\\ConstraintValidatorInterface {}\n" +
                "}"
        ), new LineMarker.ToolTipEqualsAssert("Navigate to constraint"));
    }

    public void testThatAutowireConstructorIsGivenALineMarker() {
        myFixture.configureByText(YAMLFileType.YML,
            "services:\n" +
                "  Service\\YamlBar: " +
                "       autowire: true\n\n"
        );

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
            "namespace Service {\n" +
            "    class YamlBar {\n" +
            "       function __construct() {}\n" +
            "   }\n" +
            "}"
        ), markerInfo -> markerInfo.getLineMarkerTooltip() != null && markerInfo.getLineMarkerTooltip().toLowerCase().contains("autowire"));

        myFixture.configureByText(YAMLFileType.YML,
            "services:\n" +
                "  _defaults:\n" +
                "    autowire: true\n" +
                "" +
                "  Service\\YamlBarDefault: ~\n"
        );

        assertLineMarker(PhpPsiElementFactory.createPsiFileFromText(getProject(), "<?php\n" +
            "namespace Service {\n" +
            "    class YamlBarDefault {\n" +
            "       function __construct() {}\n" +
            "   }\n" +
            "}"
        ), markerInfo -> markerInfo.getLineMarkerTooltip() != null && markerInfo.getLineMarkerTooltip().toLowerCase().contains("autowire"));
    }
}
