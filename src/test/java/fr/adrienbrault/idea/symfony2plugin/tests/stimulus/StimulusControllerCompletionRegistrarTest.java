package fr.adrienbrault.idea.symfony2plugin.tests.stimulus;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.stimulus.StimulusControllerCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see StimulusControllerCompletionRegistrar
 */
public class StimulusControllerCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stimulus/fixtures";
    }

    public void testCompletionForHtmlDataControllerAttribute() {
        // Create a simple Stimulus controller
        myFixture.addFileToProject("assets/controllers/hello_controller.js", """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """
        );

        assertCompletionContains("test.html", "<div data-controller=\"<caret>\">", "hello");
    }

    public void testCompletionForTwigStimulusControllerFunction() {
        // Create a simple Stimulus controller
        myFixture.addFileToProject("assets/controllers/my_component_controller.ts", """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """
        );

        assertCompletionContains(TwigFileType.INSTANCE, "{{ stimulus_controller('<caret>') }}", "my-component");
    }

    public void testCompletionForNestedController() {
        // Create nested Stimulus controllers
        myFixture.addFileToProject("assets/controllers/users/list_controller.js", """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """
        );

        assertCompletionContains("test.html", "<div data-controller=\"<caret>\">", "users--list");
    }

    public void testCompletionForControllerWithDashNaming() {
        // Create controller with dash naming
        myFixture.addFileToProject("assets/controllers/search-form_controller.js", """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """
        );

        assertCompletionContains(TwigFileType.INSTANCE, "{{ stimulus_controller('<caret>') }}", "search-form");
    }

    public void testNavigationForHtmlDataControllerToJavaScript() {
        // Create a Stimulus controller
        myFixture.addFileToProject("assets/controllers/hello_controller.js", """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """
        );

        assertNavigationMatch("test.html", "<div data-controller=\"hel<caret>lo\">");
    }

    public void testNavigationForTwigStimulusControllerToJavaScript() {
        // Create a Stimulus controller
        myFixture.addFileToProject("assets/controllers/my_component_controller.ts", """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """
        );

        assertNavigationMatch(TwigFileType.INSTANCE, "{{ stimulus_controller('my-co<caret>mponent') }}");
    }

    public void testNavigationForHtmlDataControllerToControllersJson() {
        // Create controllers.json
        myFixture.addFileToProject("assets/controllers.json",
            "{\n" +
            "  \"controllers\": {\n" +
            "    \"@symfony/ux-chartjs\": {\n" +
            "      \"chart\": {\n" +
            "        \"enabled\": true,\n" +
            "        \"fetch\": \"eager\"\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"entrypoints\": []\n" +
            "}\n"
        );

        assertNavigationMatch("test.html", "<div data-controller=\"symfony--ux-ch<caret>artjs--chart\">");
    }

    public void testNavigationForTwigStimulusControllerToControllersJson() {
        // Create controllers.json
        myFixture.addFileToProject("assets/controllers.json",
            "{\n" +
            "  \"controllers\": {\n" +
            "    \"@symfony/ux-dropzone\": {\n" +
            "      \"dropzone\": {\n" +
            "        \"enabled\": true\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"entrypoints\": []\n" +
            "}\n"
        );

        // Test navigation with the original name (as used in Twig)
        assertNavigationMatch(TwigFileType.INSTANCE, "{{ stimulus_controller('@symfony/ux-<caret>dropzone/dropzone') }}");
    }

    public void testCompletionForControllersJsonWithOriginalNamesInTwig() {
        // Create controllers.json
        myFixture.addFileToProject("assets/controllers.json",
            "{\n" +
            "  \"controllers\": {\n" +
            "    \"@symfony/ux-chartjs\": {\n" +
            "      \"chart\": {\n" +
            "        \"enabled\": true\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"entrypoints\": []\n" +
            "}\n"
        );

        // Twig should show the original name
        assertCompletionContains(TwigFileType.INSTANCE, "{{ stimulus_controller('<caret>') }}", "@symfony/ux-chartjs/chart");
    }

    public void testCompletionForControllersJsonWithNormalizedNamesInHtml() {
        // Create controllers.json
        myFixture.addFileToProject("assets/controllers.json",
            "{\n" +
            "  \"controllers\": {\n" +
            "    \"@symfony/ux-chartjs\": {\n" +
            "      \"chart\": {\n" +
            "        \"enabled\": true\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"entrypoints\": []\n" +
            "}\n"
        );

        // HTML should show normalized name
        assertCompletionContains("test.html", "<div data-controller=\"<caret>\">", "symfony--ux-chartjs--chart");
    }
}
