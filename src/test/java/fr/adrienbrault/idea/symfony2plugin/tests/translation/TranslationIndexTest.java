package fr.adrienbrault.idea.symfony2plugin.tests.translation;

import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TranslationIndex#getTranslationMap
 */
public class TranslationIndexTest extends SymfonyTempCodeInsightFixtureTestCase {

    // Uses FQN directly so no class definition stub is needed for PHP type resolution
    private static final String CATALOGUE_CONTENT = "<?php\n" +
        "$catalogue = new \\Symfony\\Component\\Translation\\MessageCatalogue('en', [\n" +
        "    'messages' => [\n" +
        "        'hello.world' => 'Hello World',\n" +
        "        'foo.bar' => 'Foo Bar',\n" +
        "    ],\n" +
        "    'validators' => [\n" +
        "        'not.blank' => 'This value should not be blank.',\n" +
        "    ],\n" +
        "    'my_domain+intl-icu' => [\n" +
        "        'intl.key' => 'ICU message',\n" +
        "    ],\n" +
        "]);\n";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createFile("translations/catalogue.en.php", CATALOGUE_CONTENT);
        Settings.getInstance(getProject()).pathToTranslation = "translations";
    }

    public void testGetTranslationMapReturnsDomains() {
        TranslationStringMap map = TranslationIndex.getTranslationMap(getProject());
        assertContainsElements(map.getDomainList(), "messages", "validators");
    }

    public void testGetTranslationMapReturnsKeysForDomain() {
        TranslationStringMap map = TranslationIndex.getTranslationMap(getProject());
        assertNotNull(map.getDomainMap("messages"));
        assertContainsElements(map.getDomainMap("messages"), "hello.world", "foo.bar");
    }

    public void testGetTranslationMapStripsIntlIcuSuffix() {
        TranslationStringMap map = TranslationIndex.getTranslationMap(getProject());
        assertNull(map.getDomainMap("my_domain+intl-icu"));
        assertNotNull(map.getDomainMap("my_domain"));
        assertContainsElements(map.getDomainMap("my_domain"), "intl.key");
    }
}
