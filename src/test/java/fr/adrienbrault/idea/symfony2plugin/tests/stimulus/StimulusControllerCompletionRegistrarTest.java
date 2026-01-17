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
}
