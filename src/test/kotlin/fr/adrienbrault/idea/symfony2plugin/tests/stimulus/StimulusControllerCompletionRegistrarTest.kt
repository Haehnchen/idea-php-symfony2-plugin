package fr.adrienbrault.idea.symfony2plugin.tests.stimulus

import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.stimulus.StimulusControllerCompletionRegistrar
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see StimulusControllerCompletionRegistrar
 */
class StimulusControllerCompletionRegistrarTest : SymfonyLightCodeInsightFixtureTestCase() {

    override fun getTestDataPath(): String =
        "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stimulus/fixtures"

    fun testCompletionForHtmlDataControllerAttribute() {
        myFixture.addFileToProject(
            "assets/controllers/hello_controller.js",
            """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """.trimIndent()
        )

        assertCompletionContains("test.html", """<div data-controller="<caret>">""", "hello")
    }

    fun testCompletionForTwigStimulusControllerFunction() {
        myFixture.addFileToProject(
            "assets/controllers/my_component_controller.ts",
            """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """.trimIndent()
        )

        assertCompletionContains(TwigFileType.INSTANCE, "{{ stimulus_controller('<caret>') }}", "my-component")
    }

    fun testCompletionForNestedController() {
        myFixture.addFileToProject(
            "assets/controllers/users/list_controller.js",
            """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """.trimIndent()
        )

        assertCompletionContains("test.html", """<div data-controller="<caret>">""", "users--list")
    }

    fun testCompletionForControllerWithDashNaming() {
        myFixture.addFileToProject(
            "assets/controllers/search-form_controller.js",
            """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """.trimIndent()
        )

        assertCompletionContains(TwigFileType.INSTANCE, "{{ stimulus_controller('<caret>') }}", "search-form")
    }

    fun testNavigationForHtmlDataControllerToJavaScript() {
        myFixture.addFileToProject(
            "assets/controllers/hello_controller.js",
            """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """.trimIndent()
        )

        assertNavigationMatch("test.html", """<div data-controller="hel<caret>lo">""")
    }

    fun testNavigationForTwigStimulusControllerToJavaScript() {
        myFixture.addFileToProject(
            "assets/controllers/my_component_controller.ts",
            """
            import { Controller } from '@hotwired/stimulus';

            export default class extends Controller {
            }
            """.trimIndent()
        )

        assertNavigationMatch(TwigFileType.INSTANCE, "{{ stimulus_controller('my-co<caret>mponent') }}")
    }

    fun testNavigationForHtmlDataControllerToControllersJson() {
        myFixture.addFileToProject(
            "assets/controllers.json",
            """
            {
              "controllers": {
                "@symfony/ux-chartjs": {
                  "chart": {
                    "enabled": true,
                    "fetch": "eager"
                  }
                }
              },
              "entrypoints": []
            }
            """.trimIndent()
        )

        assertNavigationMatch("test.html", """<div data-controller="symfony--ux-ch<caret>artjs--chart">""")
    }

    fun testNavigationForTwigStimulusControllerToControllersJson() {
        myFixture.addFileToProject(
            "assets/controllers.json",
            """
            {
              "controllers": {
                "@symfony/ux-dropzone": {
                  "dropzone": {
                    "enabled": true
                  }
                }
              },
              "entrypoints": []
            }
            """.trimIndent()
        )

        assertNavigationMatch(TwigFileType.INSTANCE, "{{ stimulus_controller('@symfony/ux-<caret>dropzone/dropzone') }}")
    }

    fun testCompletionForControllersJsonWithOriginalNamesInTwig() {
        myFixture.addFileToProject(
            "assets/controllers.json",
            """
            {
              "controllers": {
                "@symfony/ux-chartjs": {
                  "chart": {
                    "enabled": true
                  }
                }
              },
              "entrypoints": []
            }
            """.trimIndent()
        )

        assertCompletionContains(TwigFileType.INSTANCE, "{{ stimulus_controller('<caret>') }}", "@symfony/ux-chartjs/chart")
    }

    fun testCompletionForControllersJsonWithNormalizedNamesInHtml() {
        myFixture.addFileToProject(
            "assets/controllers.json",
            """
            {
              "controllers": {
                "@symfony/ux-chartjs": {
                  "chart": {
                    "enabled": true
                  }
                }
              },
              "entrypoints": []
            }
            """.trimIndent()
        )

        assertCompletionContains("test.html", """<div data-controller="<caret>">""", "symfony--ux-chartjs--chart")
    }
}
