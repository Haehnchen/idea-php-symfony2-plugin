package fr.adrienbrault.idea.symfony2plugin.tests.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerEventsConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SymfonyProfilerEventsConsumerTest {
    @Test
    fun `reads multiple dispatchers and preserves consecutive event lists`() {
        val actual = readFixture("symfony-profiler-events-symfony-6.3.gz")

        assertEquals(2, actual.dispatchers.size)
        assertEquals(5, actual.dispatchedEventCount)
        assertEquals(7, actual.calledListenerCount)

        val main = actual.dispatchers[0]
        assertEquals("event_dispatcher", main.name)
        assertEquals(
            listOf("kernel.request", "kernel.controller", "kernel.request", "kernel.response"),
            main.events.map { it.name },
        )
        assertEquals(listOf(256, 32), main.events[0].listeners.map { it.priority })
        assertEquals(
            "Example\\EventListener\\RequestListener::validate(RequestEvent \$event): void",
            main.events[0].listeners[0].listener,
        )
        assertEquals(-10, main.events[2].listeners.single().priority)

        val domain = actual.dispatchers[1]
        assertEquals("domain_dispatcher", domain.name)
        assertEquals(listOf("example.order.created"), domain.events.map { it.name })
        assertEquals(2, domain.events.single().listeners.size)
    }

    @Test
    fun `reads Symfony 6_2 events data without dispatcher nesting`() {
        val actual = readFixture("symfony-profiler-events-symfony-6.2.gz")

        assertEquals(1, actual.dispatchers.size)
        assertEquals("event_dispatcher", actual.dispatchers.single().name)
        assertEquals(listOf("kernel.request", "kernel.controller"), actual.dispatchers.single().events.map { it.name })
        assertEquals(3, actual.calledListenerCount)
    }

    private fun readFixture(name: String) = SymfonyProfilerEventsConsumer.read(
        SymfonyProfilerProfile.read(resourceFixture(name)),
    )

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
