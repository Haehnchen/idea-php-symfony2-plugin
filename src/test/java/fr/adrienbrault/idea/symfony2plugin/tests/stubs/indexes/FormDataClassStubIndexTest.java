package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FormDataClassStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FormDataClassStubIndex
 */
public class FormDataClassStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("FormDataClassStubIndex.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }

    public void testDataClassIndexContainsFormTypeFqn() {
        assertIndexContainsKeyWithValue(FormDataClassStubIndex.KEY, "\\App\\FooDataClass1", value -> value.contains("\\App\\AutoFarmType"));
    }

    public void testTemplateIncludeIndexer() {
        assertIndexContains(FormDataClassStubIndex.KEY, "\\App\\FooDataClass1", "\\App\\FooDataClass2", "\\App\\FooDataClass3");
    }

    public void testAsFormTypeAttributeImportedThroughNamespaceAlias() {
        myFixture.addFileToProject("AliasedAttributeType.php", "<?php\n" +
            "namespace App;\n" +
            "use Symfony\\Component\\Form\\Attribute as Form;\n" +
            "#[Form\\AsFormType]\n" +
            "class AliasedAttributeType {}\n"
        );

        assertIndexContainsKeyWithValue(FormDataClassStubIndex.KEY, "\\App\\AliasedAttributeType", value -> value.contains("\\App\\AliasedAttributeType"));
    }
}
