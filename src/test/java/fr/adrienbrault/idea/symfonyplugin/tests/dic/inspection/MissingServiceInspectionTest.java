package fr.adrienbrault.idea.symfonyplugin.tests.dic.inspection;

import fr.adrienbrault.idea.symfonyplugin.dic.inspection.MissingServiceInspection;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.dic.inspection.MissingServiceInspection
 */
public class MissingServiceInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("services.xml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/dic/inspection/fixtures";
    }

    public void testThatPhpServiceInterfaceForGetMethodIsInspected() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "/** @var $x \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$x->get('fo<caret>obar')",
            MissingServiceInspection.INSPECTION_MESSAGE
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "/** @var $x \\Symfony\\Component\\DependencyInjection\\ContainerInterface */\n" +
                "$x->get('app.ma<caret>iler')",
            MissingServiceInspection.INSPECTION_MESSAGE
        );
    }

    public void testThatYamlServiceInterfaceForGetMethodIsInspected() {
        assertLocalInspectionContains("services.yml", "services:\n   @args<caret>_unknown", "Missing Service");
        assertLocalInspectionContains("services.yml", "services:\n   @Args<caret>_unknown", "Missing Service");

        assertLocalInspectionNotContains("services.yml", "services:\n   @App.ma<caret>iler", "Missing Service");
        assertLocalInspectionNotContains("services.yml", "services:\n   @app.ma<caret>iler", "Missing Service");

        assertLocalInspectionNotContains("services.yml", "services:\n   @@args<caret>_unknown", "Missing Service");
        assertLocalInspectionNotContains("services.yml", "services:\n   @=args<caret>_unknown", "Missing Service");
    }
}
