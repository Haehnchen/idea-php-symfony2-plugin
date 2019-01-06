package fr.adrienbrault.idea.symfony2plugin.tests.dic.translation;

import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;

public class TranslationStringParserTest  extends Assert {

    @Test
    public void testParse() {

        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/translation/translations/catalogue.de.php");
        TranslationStringMap map =  new TranslationStringParser().parse(testFile);

        assertTrue(map.getDomainList().contains("FOSUserBundle"));
        assertTrue(map.getDomainList().contains("icu_domain+intl-icu"));
        assertFalse(map.getDomainList().contains("NotInList"));

        assertTrue(map.getDomainMap("FOSUserBundle").size() > 0);
        assertTrue(map.getDomainMap("icu_domain+intl-icu").size() > 0);
        assertNull(map.getDomainMap("NotInList"));

        assertTrue(map.getDomainMap("FOSUserBundle").contains("registration.email.message"));
        assertTrue(map.getDomainMap("icu_domain+intl-icu").contains("login.submit"));
    }

    @Test
    public void testParsePathMatcher() {
        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/translation/translations/catalogue.de.php");
        TranslationStringMap map = new TranslationStringParser().parsePathMatcher(testFile.getParentFile().getPath());
        assertTrue(map.getDomainMap("FOSUserBundle").contains("registration.email.message"));
    }
}
