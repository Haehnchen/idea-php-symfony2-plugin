package fr.adrienbrault.idea.symfony2plugin.tests.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerMemory
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SymfonyProfilerTimeConsumerTest {
    @Test
    fun `reads performance summary and events sorted by duration`() {
        val actual = SymfonyProfilerTimeConsumer.read(
            SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-time.gz")),
        )

        assertEquals(132.34, actual.durationMs)
        assertEquals(12.34, actual.initializationTimeMs)
        assertEquals(
            SymfonyProfilerMemory(
                peakBytes = 6L * 1024 * 1024,
                limitBytes = 128L * 1024 * 1024,
            ),
            actual.memory,
        )
        assertTrue(actual.stopwatchInstalled)
        assertEquals(
            listOf("controller", "view", "response.listener", "kernel.request", "at.threshold", "below.threshold"),
            actual.events.map { it.name },
        )
        assertFalse(actual.events.any { it.name == "__section__" })
        assertEquals(0.999, actual.events.last().durationMs, 0.000_001)
        assertEquals(
            SymfonyProfilerTimeEvent(
                name = "controller",
                category = "section",
                startMs = 10.0,
                endMs = 95.0,
                durationMs = 70.0,
                memoryBytes = 8L * 1024 * 1024,
            ),
            actual.events.first(),
        )
    }

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
