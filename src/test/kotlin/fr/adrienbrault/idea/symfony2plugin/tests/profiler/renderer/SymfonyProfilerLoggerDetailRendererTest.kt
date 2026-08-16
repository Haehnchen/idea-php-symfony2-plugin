package fr.adrienbrault.idea.symfony2plugin.tests.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLog
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLogger
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLogSection
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.SymfonyProfilerLoggerDetailRenderer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SymfonyProfilerLoggerDetailRendererTest {
    private val renderer = SymfonyProfilerLoggerDetailRenderer

    @Test
    fun `overview shows only five prioritized important logs`() {
        val text = renderer.renderOverview(readProfile())

        assertTrue("- Log entries: 48" in text)
        assertTrue("### Important logs (12 entries)" in text)
        assertTrue("| Level | Time | Channel | Occurrences | Message |" in text)
        assertTrue("| Deprecations | 12:00:47.000 | deprecation | 3 | Latest deprecated feature used |" in text)
        assertTrue("| Emergency | 12:00:45.000 | app | 1 | System unavailable |" in text)
        assertTrue("| Errors | 12:00:42.000 | app | 1 | Latest recoverable error |" in text)
        assertFalse("Warnings" in text)
        assertFalse("Debug message" in text)
        assertTrue("_Truncated: showing 5 of 12 important log entries._" in text)
    }

    @Test
    fun `details show non-empty sections newest first and truncate per level`() {
        val text = renderer.renderDetails(readProfile(), 99)
        val headings = listOf(
            "### Deprecations (2 entries)",
            "### Emergency (1 entry)",
            "### Alert (1 entry)",
            "### Critical (1 entry)",
            "### Errors (2 entries)",
            "### Warnings (7 entries)",
            "### Silenced (1 entry)",
            "### Notice (1 entry)",
            "### Info (2 entries)",
            "### Debug (30 entries)",
        )
        headings.zipWithNext().forEach { (first, second) ->
            assertTrue(text.indexOf(first) < text.indexOf(second))
        }
        assertTrue(text.indexOf("Latest deprecated feature used") < text.indexOf("Deprecated feature used"))
        assertTrue("Warning message 07 \\| continued next line" in text)

        val debugSection = text.substringAfter("### Debug (30 entries)")
        assertTrue("Debug message 30" in debugSection)
        assertTrue("Debug message 06" in debugSection)
        assertFalse("Debug message 05" in debugSection)
        assertEquals(25, debugSection.lineSequence().count { it.startsWith("| 12:00:") })
        assertTrue("_Truncated: showing the newest 25 of 30 entries._" in debugSection)
        assertFalse("Page:" in text)
        assertFalse("fixture-secret-that-must-not-be-retained" in text)
    }

    @Test
    fun `details omit empty sections`() {
        val text = renderer.formatDetails(
            SymfonyProfilerLogger(
                listOf(
                    SymfonyProfilerLog(
                        section = SymfonyProfilerLogSection.INFO,
                        timestamp = "2026-08-16T12:00:00.000+00:00",
                        timestampEpochMillis = 1_787_227_200_000,
                        priority = 200,
                        channel = "app",
                        message = "Single informational message",
                        occurrences = 1,
                    ),
                ),
            ),
        )

        assertTrue("### Info (1 entry)" in text)
        assertFalse("### Errors" in text)
        assertFalse("### Warnings" in text)
        assertFalse("### Debug" in text)
    }

    private fun readProfile() = SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-logger.gz"))

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
