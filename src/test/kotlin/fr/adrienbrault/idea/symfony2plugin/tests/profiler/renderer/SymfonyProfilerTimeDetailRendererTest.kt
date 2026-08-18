package fr.adrienbrault.idea.symfony2plugin.tests.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerMemory
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTime
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerTimeEvent
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.SymfonyProfilerTimeDetailRenderer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SymfonyProfilerTimeDetailRendererTest {
    private val renderer = SymfonyProfilerTimeDetailRenderer

    @Test
    fun `overview shows three slowest events`() {
        val text = renderer.renderOverview(readProfile())

        assertTrue("- Total duration: 132.34 ms" in text)
        assertTrue("- Initialization time: 12.34 ms" in text)
        assertTrue("### Memory" in text)
        assertTrue("- Memory peak: 6.00 MiB" in text)
        assertTrue("- PHP memory limit: 128.00 MiB" in text)
        assertTrue("- Stopwatch installed: yes" in text)
        assertTrue("- Events: 6" in text)
        assertTrue("- Threshold: 1.00 ms" in text)
        assertTrue("### Top 3 events by duration" in text)
        assertTrue("| controller | section | 10.00 | 95.00 | 70.00 | 8.00 |" in text)
        assertTrue("| view | template | 96.00 | 120.00 | 24.00 | 7.00 |" in text)
        assertFalse("| kernel.request |" in text)
    }

    @Test
    fun `details show all events ordered by duration without pagination`() {
        val text = renderer.renderDetails(readProfile(), 99)

        assertTrue("### Memory" in text)
        assertTrue("- Memory peak: 6.00 MiB" in text)
        assertTrue("- PHP memory limit: 128.00 MiB" in text)
        assertTrue("- Threshold: 1.00 ms" in text)
        assertTrue("### Events ordered by duration" in text)
        assertTrue("| kernel.request | event_listener | 0.00 | 8.75 | 8.75 | 4.00 |" in text)
        assertTrue("| at.threshold | event_listener | 9.00 | 10.00 | 1.00 | 4.00 |" in text)
        assertFalse("| below.threshold |" in text)
        assertTrue(text.indexOf("| controller |") < text.indexOf("| view |"))
        assertTrue(text.indexOf("| view |") < text.indexOf("| response.listener |"))
        assertTrue(text.indexOf("| response.listener |") < text.indexOf("| kernel.request |"))
        assertFalse("Page:" in text)
    }

    @Test
    fun `details explain when every event is below threshold`() {
        val time = SymfonyProfilerTime(
            durationMs = 1.0,
            initializationTimeMs = 0.0,
            memory = null,
            stopwatchInstalled = true,
            events = listOf(
                SymfonyProfilerTimeEvent(
                    name = "fast.event",
                    category = "event_listener",
                    startMs = 0.0,
                    endMs = 0.999,
                    durationMs = 0.999,
                    memoryBytes = 0,
                ),
            ),
        )

        val text = renderer.formatDetails(time)

        assertTrue("No timing events meet the 1.00 ms threshold." in text)
        assertFalse("| fast.event |" in text)
    }

    @Test
    fun `details do not limit events`() {
        val time = SymfonyProfilerTime(
            durationMs = 75.0,
            initializationTimeMs = 1.0,
            memory = null,
            stopwatchInstalled = true,
            events = (1..75).map { index ->
                SymfonyProfilerTimeEvent(
                    name = if (index == 75) "event | 75\ncontinued" else "event $index",
                    category = "section",
                    startMs = index.toDouble(),
                    endMs = index + 1.0,
                    durationMs = index.toDouble(),
                    memoryBytes = 0,
                )
            },
        )

        val text = renderer.formatDetails(time)

        assertTrue("event \\| 75 continued" in text)
        assertTrue("| event 1 |" in text)
        assertFalse("Page:" in text)
    }

    @Test
    fun `unlimited PHP memory is rendered without a byte value`() {
        val time = SymfonyProfilerTime(
            durationMs = 1.0,
            initializationTimeMs = 0.0,
            memory = SymfonyProfilerMemory(peakBytes = 1024 * 1024, limitBytes = -1),
            stopwatchInstalled = false,
            events = emptyList(),
        )

        val text = renderer.formatOverview(time)

        assertTrue("- PHP memory limit: unlimited" in text)
    }

    private fun readProfile() = SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-time.gz"))

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
