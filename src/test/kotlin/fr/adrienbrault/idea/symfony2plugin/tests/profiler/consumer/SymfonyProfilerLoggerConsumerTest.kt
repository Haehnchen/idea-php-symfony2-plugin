package fr.adrienbrault.idea.symfony2plugin.tests.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLogSection
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerLoggerConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SymfonyProfilerLoggerConsumerTest {
    @Test
    fun `groups logs and orders newest entries first`() {
        val actual = SymfonyProfilerLoggerConsumer.read(
            SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-logger.gz")),
        )

        assertEquals(48, actual.logs.size)
        assertEquals(SymfonyProfilerLogSection.SILENCED, actual.logs.first().section)
        assertEquals(
            mapOf(
                SymfonyProfilerLogSection.DEPRECATION to 2,
                SymfonyProfilerLogSection.EMERGENCY to 1,
                SymfonyProfilerLogSection.ALERT to 1,
                SymfonyProfilerLogSection.CRITICAL to 1,
                SymfonyProfilerLogSection.ERROR to 2,
                SymfonyProfilerLogSection.WARNING to 7,
                SymfonyProfilerLogSection.SILENCED to 1,
                SymfonyProfilerLogSection.NOTICE to 1,
                SymfonyProfilerLogSection.INFO to 2,
                SymfonyProfilerLogSection.DEBUG to 30,
            ),
            actual.logs.groupingBy { it.section }.eachCount(),
        )
        val debug = actual.logs.filter { it.section == SymfonyProfilerLogSection.DEBUG }
        assertEquals("Debug message 30", debug.first().message)
        assertEquals("Debug message 01", debug.last().message)
        assertEquals(
            3,
            actual.logs.first { it.message == "Latest deprecated feature used" }.occurrences,
        )
        assertFalse("fixture-secret-that-must-not-be-retained" in actual.toString())
    }

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
