package fr.adrienbrault.idea.symfony2plugin.tests.completion.yaml;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.completion.yaml.YamlGotoCompletionRegistrar
 */
public class YamlGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("routes.xml");
        myFixture.copyFileToProject("services.xml");
        myFixture.copyFileToProject("YamlGotoCompletionRegistrar.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatRouteInsideRouteDefaultKeyCompletedAndNavigable() {
        assertCompletionContains(YAMLFileType.YML, "" +
            "root:\n" +
            "    path: /wp-admin\n" +
            "    defaults:\n" +
            "        route: '<caret>'\n",
            "foo_route"
        );

        assertNavigationMatch(YAMLFileType.YML, "" +
                "root:\n" +
                "    path: /wp-admin\n" +
                "    defaults:\n" +
                "        route: 'foo_<caret>route'\n"
        );
    }

    public void testThatDecoratesServiceTagProvidesReferences() {
        Collection<String[]> strings = new ArrayList<String[]>() {{
            add(new String[] {"<caret>", "foo.bar<caret>_factory"});
            add(new String[] {"'<caret>'", "'foo.bar<caret>_factory'"});
            add(new String[] {"\"<caret>\"", "\"foo.bar<caret>_factory\""});
        }};

        for (String[] s : strings) {
            assertCompletionContains(YAMLFileType.YML, "" +
                    "services:\n" +
                    "    foo:\n" +
                    "       class: Foo\\Foobar\n" +
                    "       decorates: " + s[0] + "\n",
                "foo.bar_factory"
            );

            assertNavigationMatch(YAMLFileType.YML, "" +
                    "services:\n" +
                    "    foo:\n" +
                    "       class: Foo\\Foobar\n" +
                    "       decorates: " + s[1] + "\n",
                PlatformPatterns.psiElement(PhpClass.class)
            );
        }
    }

    public void testThatDecoratesPrioritizeLookupElementOnInstance() {
        assertCompletionLookupContainsPresentableItem(YAMLFileType.YML, "" +
                "services:\n" +
                "    foo:\n" +
                "       class: Foo\\Foobar\n" +
                "       decorates: <caret>\n",
            lookupElement -> "foo".equals(lookupElement.getItemText()) && lookupElement.isItemTextBold() && lookupElement.isItemTextBold()
        );
    }
}
