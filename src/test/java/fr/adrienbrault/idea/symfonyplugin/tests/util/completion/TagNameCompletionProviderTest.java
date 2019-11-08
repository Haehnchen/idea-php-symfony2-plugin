package fr.adrienbrault.idea.symfonyplugin.tests.util.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfonyplugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfonyplugin.util.completion.TagNameCompletionProvider;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfonyplugin.util.completion.TagNameCompletionProvider
 */
public class TagNameCompletionProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("services.xml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfonyplugin/tests/util/completion/fixtures";
    }

    public void testThatTagInServiceWithValidIdIsCompleted() {
        Collection<LookupElement> lookupElements = TagNameCompletionProvider.getTagLookupElements(getProject());

        assertNotNull(ContainerUtil.find(lookupElements, lookupElement ->
            "acme_mailer.transport".equals(lookupElement.getLookupString())
        ));
    }

    public void testThatTagsByFindTaggedServiceIdsOfPhpContainerBuilderIsCompleted() {
        Collection<LookupElement> lookupElements = TagNameCompletionProvider.getTagLookupElements(getProject());

        assertNotNull(ContainerUtil.find(lookupElements, lookupElement ->
            "my.acme_mailer.transport.tag".equals(lookupElement.getLookupString())
        ));
    }
}
