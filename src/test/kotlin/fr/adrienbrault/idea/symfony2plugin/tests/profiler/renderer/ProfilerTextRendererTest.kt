package fr.adrienbrault.idea.symfony2plugin.tests.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBytes
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEntry
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.profilerString
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.ProfilerTextRenderer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProfilerTextRendererTest {
    @Test
    fun `timestamps use ISO-8601 T format with timezone`() {
        val timestamp = requireNotNull(ProfilerTextRenderer.formatTimestamp(1_723_557_600))

        assertTrue(Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:Z|[+-]\d{2}:\d{2})$""").matches(timestamp))
    }

    @Test
    fun `CSV rows sanitize controls and escape delimiters`() {
        assertEquals("line break,plain", ProfilerTextRenderer.csvRow("line\nbreak", "plain"))
        assertEquals(
            "\"value, \"\"quoted\"\"\",plain",
            ProfilerTextRenderer.csvRow("value, \"quoted\"", "plain"),
        )
    }

    @Test
    fun `sequential integer arrays render as lists`() {
        val value = mapValue(
            "items" to ProfilerArray(
                listOf(
                    ProfilerEntry(PhpIntegerKey(0), profilerString("first")),
                    ProfilerEntry(PhpIntegerKey(1), profilerString("second")),
                ),
            ),
        )

        assertEquals(
            listOf(
                "items:",
                "  - first",
                "  - second",
            ),
            ProfilerTextRenderer.render(value),
        )
    }

    @Test
    fun `list entries can contain associative key value structures`() {
        val value = mapValue(
            "items" to ProfilerArray(
                listOf(
                    ProfilerEntry(
                        PhpIntegerKey(0),
                        mapValue(
                            "name" to profilerString("first"),
                            "state" to profilerString("active"),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            listOf(
                "items:",
                "  -",
                "    name: first",
                "    state: active",
            ),
            ProfilerTextRenderer.render(value),
        )
    }

    @Test
    fun `mixed and non sequential arrays preserve their keys`() {
        val mixed = ProfilerArray(
            listOf(
                ProfilerEntry(PhpIntegerKey(1), profilerString("indexed")),
                ProfilerEntry(stringKey("name"), profilerString("named")),
            ),
        )

        assertEquals(
            listOf(
                "[1]: indexed",
                "name: named",
            ),
            ProfilerTextRenderer.render(mixed),
        )
    }

    private fun mapValue(vararg entries: Pair<String, fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue>) =
        ProfilerArray(entries.map { (key, value) -> ProfilerEntry(stringKey(key), value) })

    private fun stringKey(value: String) = PhpStringKey(PhpBytes(value.toByteArray(Charsets.UTF_8)))
}
