package fr.adrienbrault.idea.symfony2plugin.tests.javascript;


import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class JavascriptCompletionNavigationContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("routes.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/javascript/fixtures";
    }

    public void testCompletionForUrlsInsideJavascriptAndTypescript() {
        assertCompletionContains("test.ts", "" +
                "import axios from 'axios';\n" +
                "const instance = axios.create({\n" +
                "  baseURL: '<caret>'\n" +
                "});",
            "/test/{foo}/car/foobar", "foo_controller_invoke"
        );

        assertCompletionContains("test.js", "" +
                "import axios from 'axios';\n" +
                "const instance = axios.create({\n" +
                "  baseURL: '<caret>'\n" +
                "});",
            "/test/{foo}/car/foobar", "foo_controller_invoke"
        );

        assertCompletionContains("test.ts", "" +
                "import axios from 'axios';\n" +
                "axios({url: '<caret>'});",
            "/test/{foo}/car/foobar", "foo_controller_invoke"
        );

        assertCompletionContains("test.ts", "" +
                "import axios from 'axios';\n" +
                "axios('<caret>');",
            "/test/{foo}/car/foobar", "foo_controller_invoke"
        );

        assertCompletionContains("test.ts", "" +
                "new Request('<caret>');\n",
            "/test/{foo}/car/foobar", "foo_controller_invoke"
        );

        assertCompletionContains("test.ts", "" +
                "const foo = {url: '<caret>'};\n",
            "/test/{foo}/car/foobar", "foo_controller_invoke"
        );

        assertCompletionContains("test.ts", "" +
                "fetch('<caret>');\n",
            "/test/{foo}/car/foobar", "foo_controller_invoke"
        );

        assertCompletionResultEquals("test.ts", "" +
                "fetch('foo_controller_invo<caret>');",
            "fetch('/test/{foo}/car/foobar');"
        );
    }

    public void testNavigationForUrlsInsideJavascriptAndTypescript() {
        assertNavigationMatch("test.ts",
            "fetch('/test/foobar/car/foob<caret>ar');",
            PlatformPatterns.psiElement(Method.class)
        );
    }
}
