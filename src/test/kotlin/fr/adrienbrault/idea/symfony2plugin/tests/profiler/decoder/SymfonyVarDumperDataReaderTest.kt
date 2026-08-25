package fr.adrienbrault.idea.symfony2plugin.tests.profiler.decoder

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.SymfonyVarDumperDataReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SymfonyVarDumperDataReaderTest {
    @Test
    fun `expands position tables while preserving unknown fields and normalizing property keys`() {
        val profile = SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-request.gz"))
        val collector = requireNotNull(profile.collector("request"))
        val decoded = SymfonyVarDumperDataReader(profile.result).read(collector["data"]) as ProfilerArray

        assertEquals("POST", (decoded["method"] as ProfilerString).utf8StringOrNull())
        assertNotNull(decoded["future_context"])

        val context = decoded.array("session_usages").array(0).array("context")
        val propertyNames = context.entries.mapNotNull { entry ->
            (entry.key as? PhpStringKey)?.bytes?.utf8StringOrNull()
        }
        assertEquals(listOf("secretToken", "label", "metadata"), propertyNames)
    }

    private fun ProfilerArray.array(key: String): ProfilerArray = this[key] as ProfilerArray

    private fun ProfilerArray.array(index: Long): ProfilerArray =
        entries.first { entry ->
            (entry.key as? fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey)?.value == index
        }.value as ProfilerArray

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
