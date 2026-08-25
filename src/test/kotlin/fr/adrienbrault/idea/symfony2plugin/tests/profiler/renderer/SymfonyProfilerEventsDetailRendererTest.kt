package fr.adrienbrault.idea.symfony2plugin.tests.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerEventDispatcher
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerEvents
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerEventsConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.SymfonyProfilerEventsDetailRenderer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SymfonyProfilerEventsDetailRendererTest {
    private val renderer = SymfonyProfilerEventsDetailRenderer

    @Test
    fun `overview contains counts without listener details`() {
        val text = renderer.formatOverview(readFixture())

        assertTrue("## Collector: events" in text)
        assertTrue("- Dispatchers: 2" in text)
        assertTrue("- Dispatched events: 5" in text)
        assertTrue("- Called listeners: 7" in text)
        assertFalse("### Dispatcher:" in text)
        assertFalse("RequestListener" in text)
    }

    @Test
    fun `details retain headings and render each event listener list as CSV`() {
        val text = renderer.formatDetails(readFixture())

        assertTrue("### Dispatcher: event_dispatcher" in text)
        assertTrue("### Dispatcher: domain_dispatcher" in text)
        assertEquals(2, Regex("#### kernel\\.request ").findAll(text).count())
        assertTrue("#### kernel.request (2 listeners)" in text)
        assertTrue("#### kernel.controller (1 listener)" in text)
        assertTrue("priority,listener" in text)
        assertTrue(
            "256,Example\\EventListener\\RequestListener::validate(RequestEvent \$event): void" in text,
        )
        assertTrue("-10,Example\\EventListener\\LateRequestListener::inspect" in text)
        assertTrue("ResponseEvent | continued \$event" in text)
        assertTrue(text.indexOf("### Dispatcher: event_dispatcher") < text.indexOf("### Dispatcher: domain_dispatcher"))
        assertFalse("Page:" in text)
    }

    @Test
    fun `details retain empty dispatchers`() {
        val text = renderer.formatDetails(
            SymfonyProfilerEvents(listOf(SymfonyProfilerEventDispatcher("empty_dispatcher", emptyList()))),
        )

        assertTrue("### Dispatcher: empty_dispatcher" in text)
        assertTrue("No dispatched events recorded." in text)
    }

    private fun readFixture() = SymfonyProfilerEventsConsumer.read(
        SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-events-symfony-6.3.gz")),
    )

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
