package fr.adrienbrault.idea.symfony2plugin.tests.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerProfile
import fr.adrienbrault.idea.symfony2plugin.profiler.consumer.SymfonyProfilerRequestConsumer
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SymfonyProfilerRequestConsumerTest {
    @Test
    fun `builds the stable summary and preserves unknown safe fields`() {
        val request = readRequest()

        assertEquals("POST", request.summary.method)
        assertEquals("/login", request.summary.path)
        assertEquals("app_login", request.summary.route)
        assertEquals(200, request.summary.statusCode)
        assertEquals("application/json", request.summary.contentType)
        assertEquals("neutral-value", request.data.array("future_context").text("feature"))
    }

    @Test
    fun `applies section-specific and fallback redaction before rendering`() {
        val data = readRequest().data

        assertEquals("2", data.array("request_query").text("page"))
        assertRedacted(data.array("request_query").text("api_key"))
        assertEquals("user@example.test", data.array("request_request").text("email"))
        assertRedacted(data.array("request_request").text("password"))
        assertRedacted(data.array("request_request").array("profile").text("otp"))

        assertEquals("corr-42", data.array("request_headers").text("x-correlation-id"))
        assertRedacted(data.array("request_headers").text("authorization"))
        assertRedacted(data.array("response_headers").text("set-cookie"))
        assertRedacted(data.array("request_cookies").text("theme"))
        assertRedacted(data.array("session_attributes").text("cart_id"))

        val server = data.array("request_server")
        assertEquals("test", server.text("APP_ENV"))
        assertRedacted(server.text("APP_SECRET"))
        assertRedacted(server.text("DATABASE_URL"))
        assertRedacted(server.text("REQUEST_URI"))
        assertRedacted(server.text("QUERY_STRING"))

        val dotenv = data.array("dotenv_vars")
        assertEquals("1", dotenv.text("FEATURE_FLAG"))
        assertRedacted(dotenv.text("MESSENGER_TRANSPORT_DSN"))
        assertRedacted(data.array("redirect").text("token"))
        assertRedacted(data.array("future_context").array("nested").text("api_token"))
    }

    @Test
    fun `redacts raw request content curl commands and private object secrets`() {
        val data = readRequest().data

        assertEquals(
            "***REDACTED*** (raw request body omitted; see request_request / request_query)",
            data.text("content"),
        )
        assertEquals(
            "***REDACTED*** (curl command omitted; reconstructs the raw body, URL query string and request headers)",
            data.text("curlCommand"),
        )

        val context = data.array("session_usages").array(0).array("context")
        assertRedacted(context.text("secretToken"))
    }

    private fun readRequest() = SymfonyProfilerRequestConsumer.read(
        SymfonyProfilerProfile.read(resourceFixture("symfony-profiler-request.gz")),
    )

    private fun ProfilerArray.array(key: String): ProfilerArray = this[key] as ProfilerArray

    private fun ProfilerArray.array(index: Long): ProfilerArray =
        entries.first { entry -> (entry.key as? PhpIntegerKey)?.value == index }.value as ProfilerArray

    private fun ProfilerArray.text(key: String): String? =
        (this[key] as? ProfilerString)?.utf8StringOrNull()

    private fun assertRedacted(value: String?) {
        assertEquals("***REDACTED***", value)
    }

    private fun resourceFixture(name: String): ByteArray = requireNotNull(
        javaClass.getResourceAsStream("/profiler/generated/$name"),
    ).use { it.readAllBytes() }
}
