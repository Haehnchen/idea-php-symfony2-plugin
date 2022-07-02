package fr.adrienbrault.idea.symfony2plugin.tests.form;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.form.PhpLineMarkerProvider
 */
public class PhpLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/form/fixtures";
    }

    public void testThatFormCanNavigateToDataClass() {
        assertLineMarker(
            myFixture.configureByText(
                PhpFileType.INSTANCE,
                "<?php\n" +
                    "\n" +
                    "use Symfony\\Component\\Form\\AbstractType;\n" +
                    "use Symfony\\Component\\OptionsResolver\\OptionsResolver;\n" +
                    "\n" +
                    "class Form extends AbstractType\n" +
                    "{\n" +
                    "    public function configureOptions(OptionsResolver $resolver): void\n" +
                    "    {\n" +
                    "        $resolver->setDefaults([\n" +
                    "            'data_class' => \\Form\\DataClass\\Model::class,\n" +
                    "        ]);\n" +
                    "    }\n" +
                    "}"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to data class")
        );
    }

    public void testThatDataClassCanNavigateToForm() {
        assertLineMarker(
            myFixture.configureByText(
                PhpFileType.INSTANCE,
                "<?php\n" +
                    "namespace App;\n" +
                    "class FoobarDataClass {}\n"
            ),
            new LineMarker.ToolTipEqualsAssert("Navigate to form")
        );
    }
}
