package fr.adrienbrault.idea.symfony2plugin.tests.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTwigConsumer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SymfonyProfilerTwigConsumerTest {
    @Test
    fun `reads metrics unique templates and rendering tree`() {
        val actual = SymfonyProfilerTwigConsumer.read(
            SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-twig.gz")),
        )

        assertEquals(14.0, actual.renderTimeMs, 0.001)
        assertEquals(8, actual.templateCallCount)
        assertEquals(1, actual.blockCallCount)
        assertEquals(1, actual.macroCallCount)
        assertEquals(
            listOf(
                "catalog/detail.html.twig",
                "base.html.twig",
                "partials/navigation.html.twig",
                "components/card.html.twig",
                "emails/banner.html.twig",
                "@WebProfiler/Profiler/toolbar_js.html.twig",
                "@WebProfiler/Profiler/toolbar.html.twig",
            ),
            actual.renderedTemplates.map { it.name },
        )
        assertEquals(2, actual.renderedTemplates.single { it.name == "components/card.html.twig" }.renderCount)
        assertEquals("main", actual.root.name)
        assertEquals(
            listOf("catalog/detail.html.twig", "@WebProfiler/Profiler/toolbar_js.html.twig"),
            actual.root.children.map { it.template },
        )
        assertEquals(
            listOf("format_price", "base.html.twig"),
            actual.root.children.first().children.map { it.name },
        )
    }

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
