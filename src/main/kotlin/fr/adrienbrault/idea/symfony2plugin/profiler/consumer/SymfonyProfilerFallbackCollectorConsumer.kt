package fr.adrienbrault.idea.symfony2plugin.profiler.consumer

import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpIntegerKey
import fr.adrienbrault.idea.symfony2plugin.phpUnserializer.PhpStringKey
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerArray
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerString
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.ProfilerValue
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.SymfonyVarDumperDataReader
import fr.adrienbrault.idea.symfony2plugin.profiler.decoder.profilerString

internal const val PROFILER_REDACTED_TEXT = "***REDACTED***"

internal fun redactedProfilerValue(): ProfilerString = profilerString(PROFILER_REDACTED_TEXT)

/** Normalizes and sanitizes any profiler collector that does not have a specialized consumer. */
internal object SymfonyProfilerFallbackCollectorConsumer {
    fun read(profile: SymfonyProfilerProfile, collectorName: String): SymfonyProfilerFallbackCollector {
        val collector = profile.collector(collectorName)
            ?: error("Symfony profile does not contain the '$collectorName' collector")
        val decoded = collector["data"]?.let(SymfonyVarDumperDataReader(profile.result)::read)
            ?: ProfilerArray(emptyList())

        return SymfonyProfilerFallbackCollector(
            name = collectorName,
            data = ProfilerSecretRedactor.redact(decoded),
        )
    }
}

internal data class SymfonyProfilerFallbackCollector(
    val name: String,
    val data: ProfilerValue,
)

/** Applies a conservative key-based safety net before unknown collector data reaches MCP output. */
internal object ProfilerSecretRedactor {
    private val SENSITIVE_EXACT_NAMES = setOf(
        "auth",
        "key",
        "private",
        "pwd",
    )
    private val SENSITIVE_NAME_FRAGMENTS = listOf(
        "accesskey",
        "apikey",
        "authheader",
        "authorization",
        "authentication",
        "bearer",
        "cookie",
        "credential",
        "csrf",
        "dsn",
        "encryptionkey",
        "oauth",
        "otp",
        "passphrase",
        "passwd",
        "password",
        "privatekey",
        "secret",
        "session",
        "sessionid",
        "signingkey",
        "token",
        "xsrf",
    )
    private val CREDENTIAL_URI = Regex("(?i)^[a-z][a-z0-9+.-]*://[^/\\s:@]+:[^/\\s@]+@")

    fun redact(value: ProfilerValue): ProfilerValue = when (value) {
        is ProfilerArray -> ProfilerArray(
            value.entries.map { entry ->
                when (val key = entry.key) {
                    is PhpIntegerKey -> entry.copy(value = redact(entry.value))
                    is PhpStringKey -> {
                        val name = key.bytes.utf8StringOrNull()
                        if (name == null || isSensitiveName(name)) {
                            entry.copy(value = redactedProfilerValue())
                        } else {
                            entry.copy(value = redact(entry.value))
                        }
                    }
                }
            },
        )
        is ProfilerString -> if (value.containsCredentialUri()) redactedProfilerValue() else value
        else -> value
    }

    private fun isSensitiveName(name: String): Boolean {
        val normalized = buildString(name.length) {
            name.forEach { character ->
                if (character.isLetterOrDigit()) {
                    append(character.lowercaseChar())
                }
            }
        }
        return normalized in SENSITIVE_EXACT_NAMES || SENSITIVE_NAME_FRAGMENTS.any(normalized::contains)
    }

    private fun ProfilerString.containsCredentialUri(): Boolean =
        utf8StringOrNull()?.let(CREDENTIAL_URI::containsMatchIn) == true
}
