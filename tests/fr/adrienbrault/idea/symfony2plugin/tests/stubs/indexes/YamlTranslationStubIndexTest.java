package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ArrayListSet;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.YamlTranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.YamlTranslationStubIndex
 */
public class YamlTranslationStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("messages.fr.xlf"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("foo.fr.xliff"));
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatDomainAndTranslationsKeyOfXlfIsInIndex() {
        assertIndexContains(YamlTranslationStubIndex.KEY, "messages");
        assertContainsElements(getDomainKeys("messages"), "Symfony is great");
    }

    public void testThatDomainAndTranslationsKeyOfXliffIsInIndex() {
        assertIndexContains(YamlTranslationStubIndex.KEY, "foo");
        assertContainsElements(getDomainKeys("foo"), "Symfony is great xliff");
    }

    @NotNull
    private Set<String> getDomainKeys(@NotNull String domain) {
        Set<String> uniqueKeySet = new ArrayListSet<String>();

        for(String[] splits: FileBasedIndex.getInstance().getValues(YamlTranslationStubIndex.KEY, domain, GlobalSearchScope.allScope(getProject()))) {
            ContainerUtil.addAll(uniqueKeySet, splits);
        }

        return uniqueKeySet;
    }
}
