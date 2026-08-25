package fr.adrienbrault.idea.symfony2plugin.tests.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class SymfonyProfilerProfileTest {
    @Test
    fun `accepts GZIP profiles larger than the generic unserializer limit`() {
        val serialized = largeSerializedProfile()
        val compressed = gzip(serialized)

        assertTrue(serialized.size > 5 * 1024 * 1024)
        assertTrue(compressed.size < 5 * 1024 * 1024)
        assertEquals(listOf("custom"), SymfonyProfilerProfile.read(compressed).collectorNames)
    }

    private fun largeSerializedProfile(): ByteArray {
        val chunk = "a".repeat(64 * 1024)
        return buildString {
            append("a:2:{")
            append("s:4:\"data\";a:1:{s:6:\"custom\";O:6:\"Custom\":0:{}}")
            append("s:7:\"padding\";a:96:{")
            repeat(96) { index ->
                append("i:$index;s:${chunk.length}:\"")
                append(chunk)
                append("\";")
            }
            append("}}")
        }.toByteArray(Charsets.UTF_8)
    }

    private fun gzip(input: ByteArray): ByteArray = ByteArrayOutputStream().use { output ->
        GZIPOutputStream(output).use { gzip -> gzip.write(input) }
        output.toByteArray()
    }
}
