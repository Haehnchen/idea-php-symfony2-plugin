package fr.adrienbrault.idea.symfony2plugin.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerEntry
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerInteger
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerNull
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.SymfonyVarDumperDataReader
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.profilerString

/** Reads and sanitizes the Symfony request collector without fixing its complete field schema. */
object SymfonyProfilerRequestConsumer {
    private val REQUEST_COLLECTOR_CLASSES = setOf(
        "Symfony\\Component\\HttpKernel\\DataCollector\\RequestDataCollector",
        "Symfony\\Bundle\\FrameworkBundle\\DataCollector\\RequestDataCollector",
    )

    fun read(profile: SymfonyProfilerProfile): SymfonyProfilerRequest {
        val collector = profile.collector("request")
            ?: error("Symfony profile does not contain the request collector")
        check(collector.className.utf8StringOrNull() in REQUEST_COLLECTOR_CLASSES) {
            "Symfony request collector has an unsupported class"
        }

        val rawData = collector["data"]
            ?: error("Symfony request collector does not contain its data value")
        val decoded = SymfonyVarDumperDataReader(profile.result).read(rawData) as? ProfilerArray
            ?: error("Symfony request collector data is not an array")

        // Sanitization happens before the value tree can reach any output formatter.
        val sanitized = SymfonyProfilerRequestSanitizer.sanitize(decoded)
        return SymfonyProfilerRequest(
            data = sanitized,
            summary = SymfonyProfilerRequestSummary(
                method = sanitized.stringValue("method"),
                path = sanitized.stringValue("path_info"),
                route = sanitized.stringValue("route"),
                statusCode = (sanitized["status_code"] as? ProfilerInteger)?.value
                    ?.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }
                    ?.toInt(),
                contentType = sanitized.stringValue("content_type"),
            ),
        )
    }
}

data class SymfonyProfilerRequest(
    val data: ProfilerArray,
    val summary: SymfonyProfilerRequestSummary,
)

data class SymfonyProfilerRequestSummary(
    val method: String?,
    val path: String?,
    val route: String?,
    val statusCode: Int?,
    val contentType: String?,
)

/** Keeps the request-specific security policies separate from the generic value decoder. */
internal object SymfonyProfilerRequestSanitizer {
    private const val REDACTED_BODY =
        "$PROFILER_REDACTED_TEXT (raw request body omitted; see request_request / request_query)"
    private const val REDACTED_CURL =
        "$PROFILER_REDACTED_TEXT (curl command omitted; reconstructs the raw body, URL query string and request headers)"

    private val SENSITIVE_ENV_PATTERNS = listOf(
        "SECRET",
        "KEY",
        "PASSWORD",
        "TOKEN",
        "BEARER",
        "AUTH",
        "CREDENTIAL",
        "PRIVATE",
        "COOKIE",
        "DSN",
    )
    private val SENSITIVE_SERVER_KEYS = setOf("REQUEST_URI", "QUERY_STRING")
    private val SENSITIVE_HEADER_PATTERNS = listOf(
        "authorization",
        "cookie",
        "auth",
        "token",
        "secret",
        "credential",
        "csrf",
        "xsrf",
        "api-key",
        "apikey",
        "session",
        "x-amz-security",
    )
    private val SENSITIVE_PARAM_PATTERNS = listOf(
        "PASSWORD",
        "PASSWD",
        "PASSPHRASE",
        "PWD",
        "SECRET",
        "TOKEN",
        "API_KEY",
        "APIKEY",
        "ACCESS_KEY",
        "SIGNING_KEY",
        "ENCRYPTION_KEY",
        "OAUTH",
        "CREDENTIAL",
        "PRIVATE",
        "BEARER",
        "CSRF",
        "XSRF",
        "OTP",
    )
    private val CREDENTIAL_URI = Regex("(?i)^[a-z][a-z0-9+.-]*://[^/\\s:@]+:[^/\\s@]+@")

    fun sanitize(data: ProfilerArray): ProfilerArray = data.transformEntries { name, value ->
        when (name) {
            "request_cookies", "response_cookies", "session_attributes" -> redactImmediateValues(value)
            "request_headers", "response_headers" -> sanitizeHeaders(value)
            "request_server" -> sanitizeServer(value)
            "dotenv_vars" -> sanitizeEnvironment(value)
            "request_request", "request_query", "request_attributes" -> redactSensitiveParams(value)
            "content" -> if (value.hasContent()) profilerString(REDACTED_BODY) else value
            "curlCommand" -> if (value != ProfilerNull) profilerString(REDACTED_CURL) else value
            else -> redactSensitiveParams(value)
        }
    }

    private fun redactImmediateValues(value: ProfilerValue): ProfilerValue = when (value) {
        is ProfilerArray -> ProfilerArray(value.entries.map { entry -> entry.copy(value = redactedProfilerValue()) })
        else -> redactedProfilerValue()
    }

    private fun sanitizeHeaders(value: ProfilerValue): ProfilerValue = value.transformArray { entry ->
        val name = entry.stringKeyOrNull()
        if (name == null || SENSITIVE_HEADER_PATTERNS.any { name.contains(it, ignoreCase = true) }) {
            entry.copy(value = redactedProfilerValue())
        } else {
            entry
        }
    }

    private fun sanitizeEnvironment(value: ProfilerValue): ProfilerValue = value.transformArray { entry ->
        val name = entry.stringKeyOrNull()
        if (name == null || isSensitiveEnvironmentName(name) || entry.value.containsCredentialUri()) {
            entry.copy(value = redactedProfilerValue())
        } else {
            entry.copy(value = redactSensitiveParams(entry.value))
        }
    }

    private fun sanitizeServer(value: ProfilerValue): ProfilerValue {
        val environment = sanitizeEnvironment(value)
        return environment.transformArray { entry ->
            val name = entry.stringKeyOrNull()
            if (name != null && name.uppercase() in SENSITIVE_SERVER_KEYS && entry.value.hasContent()) {
                entry.copy(value = redactedProfilerValue())
            } else {
                entry
            }
        }
    }

    private fun redactSensitiveParams(value: ProfilerValue): ProfilerValue = when (value) {
        is ProfilerArray -> ProfilerArray(
            value.entries.map { entry ->
                when (entry.key) {
                    is PhpIntegerKey -> entry.copy(value = redactSensitiveParams(entry.value))
                    is PhpStringKey -> {
                        val name = entry.stringKeyOrNull()
                        if (name == null || isSensitiveParameterName(name)) {
                            entry.copy(value = redactedProfilerValue())
                        } else {
                            entry.copy(value = redactSensitiveParams(entry.value))
                        }
                    }
                }
            },
        )
        else -> value
    }

    private fun isSensitiveEnvironmentName(name: String): Boolean {
        val uppercase = name.uppercase()
        return SENSITIVE_ENV_PATTERNS.any(uppercase::contains)
    }

    private fun isSensitiveParameterName(name: String): Boolean {
        val uppercase = name.uppercase()
        return SENSITIVE_PARAM_PATTERNS.any(uppercase::contains)
    }

    private fun ProfilerValue.containsCredentialUri(): Boolean =
        (this as? ProfilerString)?.utf8StringOrNull()?.let(CREDENTIAL_URI::containsMatchIn) == true

    private fun ProfilerValue.hasContent(): Boolean = when (this) {
        is ProfilerString -> bytes.size > 0
        ProfilerNull -> false
        else -> true
    }

    private fun ProfilerValue.transformArray(transform: (ProfilerEntry) -> ProfilerEntry): ProfilerValue =
        if (this is ProfilerArray) ProfilerArray(entries.map(transform)) else this

    private fun ProfilerArray.transformEntries(transform: (String?, ProfilerValue) -> ProfilerValue): ProfilerArray =
        ProfilerArray(entries.map { entry -> entry.copy(value = transform(entry.stringKeyOrNull(), entry.value)) })

    private fun ProfilerEntry.stringKeyOrNull(): String? =
        (key as? PhpStringKey)?.bytes?.utf8StringOrNull()

}

private fun ProfilerArray.stringValue(key: String): String? =
    (this[key] as? ProfilerString)?.utf8StringOrNull()
