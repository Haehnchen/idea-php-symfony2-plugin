package fr.adrienbrault.idea.symfony2plugin.tests.config.xml;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.xml.XmlReferenceContributor
 */
public class XmlReferenceContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("XmlReferenceContributor.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatAutowiringTypeReferenceToPhpClass() {
        assertReferenceMatch(XmlFileType.INSTANCE, "" +
            "<?xml version=\"1.0\"?>\n" +
            "<container>\n" +
            "    <services>\n" +
            "        <service>\n" +
            "            <autowiring-type>Foo<caret>\\Bar</autowiring-type>\n" +
            "        </service>\n" +
            "    </services>\n" +
            "</container>\n",
            PlatformPatterns.psiElement(PhpClass.class).withName("Bar")
        );
    }
}
