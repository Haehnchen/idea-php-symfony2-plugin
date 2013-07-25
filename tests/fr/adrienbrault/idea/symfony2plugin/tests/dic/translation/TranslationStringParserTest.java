package fr.adrienbrault.idea.symfony2plugin.tests.dic.translation;

import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;

public class TranslationStringParserTest  extends Assert {

    @Test
    public void testParse() {

        File testFile = new File(this.getClass().getResource("translations/catalogue.de.php").getFile());
        TranslationStringMap map =  new TranslationStringParser().parse(testFile);

        assertEquals("registration.email.message", map.getStringMap().get("FOSUserBundle:registration.email.message"));
        assertEquals("layout.logout", map.getStringMap().get("FOSUserBundle:layout.logout"));
        assertEquals("button.next", map.getStringMap().get("CraueFormFlowBundle:button.next"));
        assertEquals("foo.bar", map.getStringMap().get("validators:foo.bar"));
        assertEquals("foo.baz", map.getStringMap().get("validators:foo.baz"));
        assertEquals("foo.escape", map.getStringMap().get("validators:foo.escape"));

        assertTrue(map.getDomainList().contains("FOSUserBundle"));
        assertFalse(map.getDomainList().contains("NotInList"));

        assertTrue(map.getDomainMap("FOSUserBundle").size() > 0);
        assertEquals(0, map.getDomainMap("NotInList").size());
    }

    @Test
    public void testParsePathMatcher() {
        File testFile = new File(this.getClass().getResource("translations/catalogue.de.php").getFile());
        TranslationStringMap map = new TranslationStringParser().parsePathMatcher(testFile.getParentFile().getPath());
        assertEquals("registration.email.message", map.getStringMap().get("FOSUserBundle:registration.email.message"));
    }
}
