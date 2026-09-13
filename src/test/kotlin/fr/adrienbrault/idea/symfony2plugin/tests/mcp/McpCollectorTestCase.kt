package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

abstract class McpCollectorTestCase : SymfonyLightCodeInsightFixtureTestCase() {
    protected fun loadDoctrineFixtures() {
        myFixture.copyFileToProject("doctrine/fixtures/entity_helper.php", "src/Doctrine/entity_helper.php")
        myFixture.copyFileToProject("doctrine/fixtures/doctrine.orm.yml", "config/doctrine/doctrine.orm.yml")
    }

    protected fun loadFormFixtures() {
        myFixture.copyFileToProject("form/util/fixtures/classes.php", "src/Form/classes.php")
        myFixture.copyFileToProject("form/util/fixtures/FormOptionsUtil.php", "src/Form/FormOptionsUtil.php")
        myFixture.copyFileToProject("form/util/fixtures/FormOptionsUtilKeys.php", "src/Form/FormOptionsUtilKeys.php")
        myFixture.addFileToProject(
            "src/Form/FormInterfaces.php",
            "<?php\n" +
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
        )
    }

    protected fun loadRouteFixtures() {
        myFixture.copyFileToProject("routing/fixtures/RouteHelper.php", "src/Controller/RouteHelper.php")
        myFixture.copyFileToProject("routing/fixtures/RouteHelper.services.yml", "config/RouteHelper.services.yml")
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests"
    }

    protected fun assertUsesRealLineBreaks(csv: String) {
        assertTrue("CSV should contain at least one real line break:\n$csv", csv.contains('\n'))
        assertFalse("CSV should not contain literal \\\\n separators:\n$csv", csv.contains("\\n"))
    }
}
