package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.DispatcherEvent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.EventAnnotationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.EventAnnotationStubIndex
 */
public class EventAnnotationStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("EventAnnotationStubIndex.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }

    public void testInstanceExtractionOfDocComment() {
        assertIndexContains(EventAnnotationStubIndex.KEY, "form.pre_bind");

        DispatcherEvent event = ContainerUtil.getFirstItem(FileBasedIndex.getInstance().getValues(
            EventAnnotationStubIndex.KEY,
            "form.pre_bind",
            GlobalSearchScope.allScope(getProject()))
        );

        assertEquals("Symfony\\Component\\Form\\FormEvents.PRE_SUBMIT", event.getFqn());
        assertEquals("Symfony\\Component\\Form\\FormEvent", event.getInstance());
    }

    public void testInstanceExtractionOfDocTag() {
        assertIndexContains(EventAnnotationStubIndex.KEY, "form.post_bind");

        DispatcherEvent event = ContainerUtil.getFirstItem(FileBasedIndex.getInstance().getValues(
            EventAnnotationStubIndex.KEY,
            "form.post_bind",
            GlobalSearchScope.allScope(getProject()))
        );

        assertEquals("Symfony\\Component\\Form\\FormEvents.POST_SUBMIT", event.getFqn());
        assertEquals("Foo\\Bar", event.getInstance());
    }
}
