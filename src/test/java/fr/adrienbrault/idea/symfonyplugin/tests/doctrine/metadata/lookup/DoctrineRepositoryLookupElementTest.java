package fr.adrienbrault.idea.symfonyplugin.tests.doctrine.metadata.lookup;

import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.lookup.DoctrineRepositoryLookupElement;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.lookup.DoctrineRepositoryLookupElement#create
 */
public class DoctrineRepositoryLookupElementTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/doctrine/metadata/lookup/fixtures";
    }

    public void testCreate() {
        DoctrineRepositoryLookupElement element = DoctrineRepositoryLookupElement.create(PhpElementsUtil.getClass(getProject(), "\\Foo\\Bar\\BarRepository"));
        LookupElementPresentation presentation = new LookupElementPresentation();
        element.renderElement(presentation);

        assertEquals("Foo\\Bar\\BarRepository", element.getLookupString());
        assertEquals(Symfony2Icons.DOCTRINE, presentation.getIcon());
        assertEquals("BarRepository", presentation.getItemText());
        assertEquals("Foo\\Bar\\BarRepository", presentation.getTypeText());
        assertTrue(presentation.isTypeGrayed());
    }
}
