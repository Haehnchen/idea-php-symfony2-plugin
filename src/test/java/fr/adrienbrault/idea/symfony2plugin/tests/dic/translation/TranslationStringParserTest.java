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

        assertTrue(map.getDomainMap("FOSUserBundle").contains("registration.email.message"));

        assertTrue(map.getDomainList().contains("FOSUserBundle"));
        assertFalse(map.getDomainList().contains("NotInList"));

        assertTrue(map.getDomainMap("FOSUserBundle").size() > 0);
        assertNull(map.getDomainMap("NotInList"));
    }

    @Test
    public void testParsePathMatcher() {
        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/translation/translations/catalogue.de.php");
        TranslationStringMap map = new TranslationStringParser().parsePathMatcher(testFile.getParentFile().getPath());
        assertTrue(map.getDomainMap("FOSUserBundle").contains("registration.email.message"));
    }
}
