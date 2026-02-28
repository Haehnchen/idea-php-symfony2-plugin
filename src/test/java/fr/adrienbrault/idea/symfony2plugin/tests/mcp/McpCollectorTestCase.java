package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

abstract class McpCollectorTestCase extends SymfonyLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("util/fixtures/SymfonyCommandUtilTest.php", "src/Command/SymfonyCommandUtilTest.php");
        myFixture.copyFileToProject("doctrine/fixtures/entity_helper.php", "src/Doctrine/entity_helper.php");
        myFixture.copyFileToProject("doctrine/fixtures/doctrine.orm.yml", "config/doctrine/doctrine.orm.yml");
        myFixture.copyFileToProject("stubs/fixtures/classes.php", "src/Service/classes.php");
        myFixture.copyFileToProject("stubs/fixtures/services.yml", "config/services.yml");
        myFixture.copyFileToProject("stubs/fixtures/services.xml", "config/services.xml");

        myFixture.copyFileToProject("form/util/fixtures/classes.php", "src/Form/classes.php");
        myFixture.copyFileToProject("form/util/fixtures/FormOptionsUtil.php", "src/Form/FormOptionsUtil.php");
        myFixture.copyFileToProject("form/util/fixtures/FormOptionsUtilKeys.php", "src/Form/FormOptionsUtilKeys.php");
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\Form\n" +
            "{\n" +
            "    interface FormTypeExtensionInterface\n" +
            "    {\n" +
            "        public function getExtendedType();\n" +
            "    }\n" +
            "\n" +
            "    interface FormTypeInterface\n" +
            "    {\n" +
            "        public function getName();\n" +
            "    }\n" +
            "}"
        );

        myFixture.copyFileToProject("routing/fixtures/RouteHelper.php", "src/Controller/RouteHelper.php");
        myFixture.copyFileToProject("routing/fixtures/RouteHelper.services.yml", "config/RouteHelper.services.yml");
        myFixture.copyFileToProject("templating/util/fixtures/twig_extensions.php", "src/Twig/twig_extensions.php");
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests";
    }
}
