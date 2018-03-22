package fr.adrienbrault.idea.symfony2plugin.tests.config.xml;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
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
        myFixture.copyFileToProject("XmlReferenceContributor.env");
        myFixture.copyFileToProject("services.xml");
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

    public void testThatFactoryClassAttributeProvidesReference() {
        assertReferenceMatchOnParent(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service>\n" +
                "            <factory class=\"Foo\\<caret>Bar\" method=\"create\"/>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            PlatformPatterns.psiElement(PhpClass.class).withName("Bar")
        );
    }

    public void testThatFactoryMethodAttributeProvidesReferenceForClass() {
        assertReferenceMatchOnParent(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service>\n" +
                "            <factory class=\"Foo\\Bar\" method=\"cr<caret>eate\"/>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            PlatformPatterns.psiElement(Method.class).withName("create")
        );
    }

    public void testThatFactoryMethodAttributeProvidesReferenceForService() {
        assertReferenceMatchOnParent(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service>\n" +
                "            <factory service=\"foo.bar_factory\" method=\"cr<caret>eate\"/>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            PlatformPatterns.psiElement(Method.class).withName("create")
        );
    }

    public void testThatArgumentConstantProvidesReferences() {
        assertReferenceMatch(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service>\n" +
                "            <argument type=\"constant\">Foo\\Bar::FO<caret>O</argument>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            PlatformPatterns.psiElement(Field.class).withName("FOO")
        );

        assertReferenceMatch(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service>\n" +
                "            <argument type=\"constant\">CONS<caret>T_FOO</argument>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            PlatformPatterns.psiElement()
        );
    }

    public void testEnvironmentParameter() {
        assertReferenceMatch(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service>\n" +
                "            <argument>%env(FOOB<caret>AR_ENV)%</argument>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            PlatformPatterns.psiElement()
        );

        assertReferenceMatch(XmlFileType.INSTANCE, "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service>\n" +
                "            <argument>%env(int:FOOB<caret>AR_ENV)%</argument>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            PlatformPatterns.psiElement()
        );
    }

    public void testServiceIdAsClassReferences() {
        assertReferenceMatchOnParent("test.xml", "" +
                "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service id=\"Foo\\<caret>Bar\"/>\n" +
                "    </services>\n" +
                "</container>\n",
            PlatformPatterns.psiElement(PhpClass.class).withName("Bar")
        );
    }
}
