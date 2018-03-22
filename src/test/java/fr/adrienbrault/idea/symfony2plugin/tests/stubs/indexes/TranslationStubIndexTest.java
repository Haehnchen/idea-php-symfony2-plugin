package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ArrayListSet;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TranslationStubIndex
 */
public class TranslationStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("messages.fr.xlf");
        myFixture.copyFileToProject("messages_two.fr.xlf");
        myFixture.copyFileToProject("foo.fr.xliff");
        myFixture.copyFileToProject("fr.xlf");
        myFixture.copyFileToProject("fr.xlf","foo.bar.de.xlf");
        myFixture.copyFileToProject("fr.xlf",".de.xlf");
        myFixture.copyFileToProject("fr.xlf", "car_xlf_flex.fr.xlf");

        myFixture.copyFileToProject("apple.de.yml", "Resources/translations/apple.de.yml");
        myFixture.copyFileToProject("car.de.yml", "Resources/translations/car.de.yml");
        myFixture.copyFileToProject("car.de.yml", "translations/car_flex_yml.de.yml");
        myFixture.copyFileToProject("car.de.yml", "translations/car_flex_yaml.de.yaml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }

    public void testThatDomainFromFileIsExtracted() {
        assertIndexContains(TranslationStubIndex.KEY, "foo.bar");
        assertIndexNotContains(TranslationStubIndex.KEY, "");
    }

    public void testThatDomainAndTranslationsKeyOfXlfIsInIndex() {
        assertIndexContains(TranslationStubIndex.KEY, "messages");
        assertContainsElements(getDomainKeys("messages"), "Symfony is great");
    }

    public void testThatDomainAndTranslationsKeyOfXliffIsInIndex() {
        assertIndexContains(TranslationStubIndex.KEY, "foo");
        assertContainsElements(getDomainKeys("foo"), "Symfony is great xliff");
    }

    public void testThatDomainAndTranslationsKeyOfXliffv2IsInIndex() {
        assertIndexContains(TranslationStubIndex.KEY, "messages_two");
        assertContainsElements(getDomainKeys("messages_two"), "hello xliff v2");

        assertIndexContains(TranslationStubIndex.KEY, "messages_two");
        assertContainsElements(getDomainKeys("messages_two"), "hello xliff v2 group less");
    }

    public void testThatDomainAndTranslationsKeyOfYamlFileIsInIndex() {
        assertIndexContains(TranslationStubIndex.KEY, "apple");
        assertContainsElements(getDomainKeys("apple"), "yaml_weak.symfony.great");
        assertContainsElements(getDomainKeys("apple"), "yaml_weak.symfony.greater than");
        assertContainsElements(getDomainKeys("apple"), "yaml_weak.symfony.greater than equals");

        assertContainsElements(getDomainKeys("apple"), "yaml_weak.symfony.more.lines");
        assertContainsElements(getDomainKeys("apple"), "yaml_weak.symfony.more.lines_2");

        assertFalse(getDomainKeys("apple").contains("yaml_weak.symfony"));
        assertFalse(getDomainKeys("apple").contains("yaml_weak.symfony"));
        assertFalse(getDomainKeys("apple").contains("yaml_weak"));

        assertIndexContains(TranslationStubIndex.KEY, "car");
        assertContainsElements(getDomainKeys("car"), "foo_yaml.symfony.great");
    }

    public void testThatEmptyXliffDomainShouldNotIndexAsLanguageKey() {
        assertIndexNotContains(TranslationStubIndex.KEY, "fr");
    }

    public void testThatResnameXliffShouldBeUsedForKey() {
        assertContainsElements(getDomainKeys("messages"), "resname.symfony_is_great");
    }

    public void testThatTranslationsInRootForSymfonyFlexIsInIndex() {
        assertContainsElements(getDomainKeys("car_flex_yml"), "foo_yaml.symfony.great");
        assertContainsElements(getDomainKeys("car_flex_yaml"), "foo_yaml.symfony.great");
        assertContainsElements(getDomainKeys("car_xlf_flex"), "Symfony is great");
    }

    @NotNull
    private Set<String> getDomainKeys(@NotNull String domain) {
        Set<String> uniqueKeySet = new ArrayListSet<String>();

        for(Set<String> splits: FileBasedIndex.getInstance().getValues(TranslationStubIndex.KEY, domain, GlobalSearchScope.allScope(getProject()))) {
            ContainerUtil.addAll(uniqueKeySet, splits);
        }

        return uniqueKeySet;
    }
}
