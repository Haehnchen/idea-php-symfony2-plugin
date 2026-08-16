package fr.adrienbrault.idea.symfony2plugin.tests.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerFallbackCollectorConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SymfonyProfilerFallbackCollectorConsumerTest {
    @Test
    fun `fallback expands var dumper data before redacting known secrets`() {
        val profile = SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-request.gz"))

        val fallback = SymfonyProfilerFallbackCollectorConsumer.read(profile, "request")
        val data = fallback.data as ProfilerArray

        assertEquals("POST", data.text("method"))
        assertNotNull(data["future_context"])
        assertEquals("***REDACTED***", data.array("request_query").text("api_key"))
    }

    private fun ProfilerArray.array(key: String): ProfilerArray = this[key] as ProfilerArray

    private fun ProfilerArray.text(key: String): String =
        (this[key] as ProfilerString).utf8StringOrNull() ?: error("Expected UTF-8 profiler string")

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
