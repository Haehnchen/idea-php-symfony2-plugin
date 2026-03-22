package fr.adrienbrault.idea.symfony2plugin.tests.translation.parser;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationPsiParserTest extends SymfonyLightCodeInsightFixtureTestCase {
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/translation/parser/fixtures";
    }

    public void testCompiledTranslationParser() {
        TranslationStringMap translationStringMap = TranslationStringMap.create(List.of(new File(getTestDataPath())));

        assertTrue(!translationStringMap.getDomainMap("security").isEmpty());

        assertTrue(translationStringMap.getDomainMap("validators").contains("This value should be false."));
        assertTrue(translationStringMap.getDomainMap("validators").contains("This value should be false. (1)"));

        assertNull(translationStringMap.getDomainMap("my_intl_icu_domain+intl-icu"));

        assertTrue(!translationStringMap.getDomainMap("my_intl_icu_domain").isEmpty());
        assertTrue(translationStringMap.getDomainMap("my_intl_icu_domain").contains("messages_intl_icu_key"));
    }

    public void testCompiledTranslationParserOnBackgroundThread() throws Exception {
        TranslationStringMap translationStringMap = CompletableFuture
            .supplyAsync(() -> TranslationStringMap.create(List.of(new File(getTestDataPath()))))
            .get();

        assertNotNull(translationStringMap.getDomainMap("validators"));
        assertTrue(translationStringMap.getDomainMap("validators").contains("This value should be false."));
    }
}
