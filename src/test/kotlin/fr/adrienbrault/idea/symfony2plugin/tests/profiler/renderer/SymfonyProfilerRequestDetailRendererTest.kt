package fr.adrienbrault.idea.symfony2plugin.tests.profiler.renderer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpBytes
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequest
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequestSummary
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEntry
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.profilerString
import fr.adrienbrault.idea.symfony2plugin.profiler.renderer.SymfonyProfilerRequestDetailRenderer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class SymfonyProfilerRequestDetailRendererTest {
    private val renderer = SymfonyProfilerRequestDetailRenderer

    @Test
    fun `details render sanitized project-neutral plain text`() {
        val text = renderer.renderDetails(readProfile(), 1)

        assertTrue("Collector: request" in text)
        assertTrue("Path: /login" in text)
        assertTrue("Route: app_login" in text)
        assertTrue("request_query:" in text)
        assertTrue("api_key: ***REDACTED***" in text)
        assertTrue("email: user@example.test" in text)
        assertTrue("feature: neutral-value" in text)
        assertTrue("secretToken: ***REDACTED***" in text)
        assertTrue("label: neutral-context" in text)
        assertFalse("\\0ProfilerFixture\\TraceContext\\0" in text)
        assertFalse("\\0*\\0" in text)
        assertFalse("Page:" in text)
        assertTrue("\nData\n\nmethod: POST\n" in text)
        assertFalse("\n  method: POST\n" in text)

        listOf(
            "query-secret",
            "request-secret",
            "authorization-secret",
            "database-secret",
            "session-secret",
            "body-secret",
            "curl-secret",
            "object-secret",
            "future-secret",
        ).forEach { secret -> assertFalse(secret in text) }
        assertFalse(text.lineSequence().any { line ->
            line.startsWith("#") || line.startsWith("- ") || line.startsWith("|") || line.startsWith("```")
        })
        assertFalse(text.trimStart().startsWith("{"))
    }

    @Test
    fun `overview adds only compact request metadata`() {
        val text = renderer.renderOverview(readProfile())

        assertTrue("- Path: /login" in text)
        assertTrue("- Route: app_login" in text)
        assertTrue("- Content type: application/json" in text)
        assertFalse("request_query" in text)
        assertFalse("Summary" in text)
        assertFalse("Data" in text)
    }

    @Test
    fun `details paginate generic value lines around the token target`() {
        val compactRequest = SymfonyProfilerRequest(
            data = ProfilerArray(
                (1..205).map { index ->
                    ProfilerEntry(
                        PhpStringKey(PhpBytes("field_$index".toByteArray(StandardCharsets.UTF_8))),
                        ProfilerInteger(index.toLong()),
                    )
                },
            ),
            summary = SymfonyProfilerRequestSummary("GET", "/example", "app_example", 200, "text/html"),
        )

        val compactDetails = renderer.formatDetails(compactRequest, 1)
        assertFalse("Page:" in compactDetails)
        assertTrue("field_205: 205" in compactDetails)

        val largeRequest = SymfonyProfilerRequest(
            data = ProfilerArray(
                (1..16).map { index ->
                    ProfilerEntry(
                        PhpStringKey(PhpBytes("field_$index".toByteArray(StandardCharsets.UTF_8))),
                        profilerString("x".repeat(1_100)),
                    )
                },
            ),
            summary = compactRequest.summary,
        )

        val firstPage = renderer.formatDetails(largeRequest, 1)
        assertTrue("Page: 1 of 2" in firstPage)
        assertTrue("field_15:" in firstPage)
        assertFalse("field_16:" in firstPage)

        val secondPage = renderer.formatDetails(largeRequest, 2)
        assertTrue("Page: 2 of 2" in secondPage)
        assertTrue("field_16:" in secondPage)
        assertFalse("field_15:" in secondPage)
    }

    private fun readProfile() = SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-request.gz"))

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
